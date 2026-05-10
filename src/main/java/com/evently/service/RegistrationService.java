package com.evently.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.evently.dto.RegistrationFormDto;
import com.evently.exception.DuplicateRegistrationException;
import com.evently.exception.EventNotFoundException;
import com.evently.model.Attendee;
import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.model.PaymentStatus;
import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;
import com.evently.repository.AttendeeRepository;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Core booking logic: find-or-create attendee, capacity check, waitlist
 * management.
 *
 * OWNER: Mohamed Ahmed
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class RegistrationService {

    private final EventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;

    /**
     * Registers an attendee for an event. 
     * Creates an Attendee record if one with the given email does not exist.
     * Places the registration as CONFIRMED or WAITLISTED based on remaining capacity.
     */
    @Transactional
   public Registration register(Long eventId, RegistrationFormDto form) {

    Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException(eventId));

    if (event.getStatus() != EventStatus.PUBLISHED) {
        throw new IllegalStateException("Event is not open for registration.");
    }

    // Block registration when the event has already started or is less than 2 hours away.
    if (event.getDateTime().isBefore(LocalDateTime.now().plusHours(2))) {
        throw new IllegalStateException("Registration is closed. The event starts in less than 2 hours or has already passed.");
    }

    // Normalize email
    String normalizedEmail = form.getEmail().trim().toLowerCase();

    // Find or create attendee (SAFE VERSION)
    Attendee attendee = attendeeRepository.findByEmail(normalizedEmail)
            .orElseGet(() -> attendeeRepository.save(
                    Attendee.builder()
                            .name(form.getName())
                            .email(normalizedEmail)
                            .phone(form.getPhone())
                            .company(form.getCompany())
                            .build()
            ));

    // Duplicate check — only reject if there is an active (non-cancelled) registration.
    // A previously cancelled registration allows the user to register again.
        if (registrationRepository.findActiveByEventIdAndAttendeeId(eventId, attendee.getId()).isPresent()) {
            throw new DuplicateRegistrationException(eventId, attendee.getEmail());
        }

        long confirmedCount = eventRepository.countConfirmedRegistrations(eventId);
        Registration.RegistrationBuilder builder = Registration.builder()
                .event(event)
                .attendee(attendee)
                .registrationDate(LocalDateTime.now())
                .paymentStatus(PaymentStatus.PENDING)
                .isAdminOverride(false);

        if (confirmedCount >= event.getCapacity()) {
            long waitlistSize = registrationRepository.countWaitlisted(eventId);
            builder.status(RegistrationStatus.WAITLISTED)
                   .waitlistPosition((int) waitlistSize + 1);
        } else {
            builder.status(RegistrationStatus.CONFIRMED);
        }

        Registration saved = registrationRepository.save(builder.build());
        if (saved.getStatus() == RegistrationStatus.CONFIRMED) {
            emailService.sendConfirmationEmail(saved);
        } else {
            emailService.sendWaitlistEmail(saved);
        }
        return saved;
    }

    /**
     * Cancels a registration and promotes the first waitlisted attendee if
     * the cancelled registration was CONFIRMED.
     */
    @Transactional
    public void cancel(Long registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found: " + registrationId));

        log.info("Cancelling registration {}: current status={}", registrationId, reg.getStatus());

        if (reg.getStatus() == RegistrationStatus.CANCELLED) {
            log.info("Registration {} already cancelled — no-op", registrationId);
            return; // idempotent
        }

        RegistrationStatus previousStatus = reg.getStatus();
        reg.setStatus(RegistrationStatus.CANCELLED);
        reg.setWaitlistPosition(null);
        registrationRepository.save(reg);
        log.info("Registration {} status set to CANCELLED (was {})", registrationId, previousStatus);

        // A freed confirmed slot triggers waitlist promotion.
        if (previousStatus == RegistrationStatus.CONFIRMED) {
            promoteWaitlist(reg.getEvent().getId());
        }
    }

    /** Promotes the head of the waitlist and re-sequences remaining positions. */
    private void promoteWaitlist(Long eventId) {
        List<Registration> waitlisted = registrationRepository.findWaitlistedByEventIdOrdered(eventId);
        if (waitlisted.isEmpty())
            return;

        Registration head = waitlisted.get(0);
        head.setStatus(RegistrationStatus.CONFIRMED);
        head.setWaitlistPosition(null);
        registrationRepository.save(head);
        emailService.sendWaitlistPromotionEmail(head);

        // Re-number remaining waitlist entries so positions stay sequential.
        for (int i = 1; i < waitlisted.size(); i++) {
            waitlisted.get(i).setWaitlistPosition(i);
        }
        if (waitlisted.size() > 1) {
            registrationRepository.saveAll(waitlisted.subList(1, waitlisted.size()));
        }
    }
}
