package com.evently.service;

import com.evently.model.Attendee;
import com.evently.model.Event;
import com.evently.model.PaymentStatus;
import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;
import com.evently.repository.AttendeeRepository;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRegistrationService {

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final EmailService emailService;

    /**
     * Get all registrations for an event (any status).
     */
    public List<Registration> getByEvent(Long eventId) {
        return registrationRepository.findByEventId(eventId);
    }

    /**
     * Get registrations filtered by status.
     */
    public List<Registration> getByEventAndStatus(Long eventId, RegistrationStatus status) {
        return registrationRepository.findByEventIdAndStatus(eventId, status);
    }

    /**
     * Get all registrations for an event with paging.
     */
    public org.springframework.data.domain.Page<Registration> getPage(Long eventId,
            RegistrationStatus status,
            org.springframework.data.domain.Pageable pageable) {
        if (eventId != null && status != null) {
            return registrationRepository.findByEventIdAndStatus(eventId, status, pageable);
        }
        if (eventId != null) {
            return registrationRepository.findByEventId(eventId, pageable);
        }
        if (status != null) {
            return registrationRepository.findByStatus(status, pageable);
        }
        return registrationRepository.findAll(pageable);
    }

    /**
     * Admin force-add: bypass capacity limit and add attendee as CONFIRMED.
     * Sets isAdminOverride = true on the registration.
     */
    @Transactional
    public Registration forceAdd(Long eventId, String attendeeEmail) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        Attendee attendee = attendeeRepository.findByEmail(attendeeEmail.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Attendee not found: " + attendeeEmail));

        if (registrationRepository.findByEventIdAndAttendeeId(eventId, attendee.getId()).isPresent()) {
            throw new IllegalStateException("Attendee is already registered for this event");
        }

        Registration registration = Registration.builder()
                .event(event)
                .attendee(attendee)
                .status(RegistrationStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.PAID)
                .isAdminOverride(true)
                .build();

        Registration saved = registrationRepository.save(registration);
        emailService.sendConfirmationEmail(saved);
        return saved;
    }

    /**
     * Cancel a registration as admin; promotes waitlist if applicable.
     */
    @Transactional
    public void cancelRegistration(Long registrationId, RegistrationService registrationService) {
        registrationService.cancel(registrationId);
    }

    /**
     * Generate a CSV string of all registrations for an event.
     */
    public String exportCsv(Long eventId) {
        List<Registration> regs = registrationRepository.findByEventId(eventId);
        StringBuilder sb = new StringBuilder("id,name,email,status,payment_status,registered_at\n");
        for (Registration r : regs) {
            sb.append(r.getId()).append(",")
                    .append(r.getAttendee().getName()).append(",")
                    .append(r.getAttendee().getEmail()).append(",")
                    .append(r.getStatus()).append(",")
                    .append(r.getPaymentStatus()).append(",")
                    .append(r.getRegistrationDate()).append("\n");
        }
        return sb.toString();
    }
}
