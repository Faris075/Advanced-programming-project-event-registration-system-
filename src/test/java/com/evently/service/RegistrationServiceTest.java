package com.evently.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.evently.dto.RegistrationFormDto;
import com.evently.exception.DuplicateRegistrationException;
import com.evently.model.Attendee;
import com.evently.model.Event;
import com.evently.model.EventStatus;
import com.evently.model.PaymentStatus;
import com.evently.model.Registration;
import com.evently.model.RegistrationStatus;
import com.evently.repository.AttendeeRepository;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock
    EventRepository eventRepository;

    @Mock
    AttendeeRepository attendeeRepository;

    @Mock
    RegistrationRepository registrationRepository;

    @InjectMocks
    RegistrationService registrationService;

    @Test
    void register_confirmsWhenCapacityAvailable() {
        Event event = Event.builder()
                .id(1L)
                .capacity(100)
                .status(EventStatus.PUBLISHED)
                .build();
        Attendee attendee = Attendee.builder()
                .id(1L)
                .name("Test")
                .email("test@test.com")
                .build();
        RegistrationFormDto form = new RegistrationFormDto();
        form.setName("Test");
        form.setEmail("test@test.com");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(attendeeRepository.findByEmail("test@test.com")).thenReturn(Optional.of(attendee));
        when(registrationRepository.findByEventIdAndAttendeeId(1L, 1L)).thenReturn(Optional.empty());
        when(eventRepository.countConfirmedRegistrations(1L)).thenReturn(2L);
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Registration result = registrationService.register(1L, form);

        assertEquals(RegistrationStatus.CONFIRMED, result.getStatus());
        assertNull(result.getWaitlistPosition());
    }

    @Test
    void register_waitlistsWhenAtCapacity() {
        Event event = Event.builder()
                .id(1L)
                .capacity(1)
                .status(EventStatus.PUBLISHED)
                .build();
        Attendee attendee = Attendee.builder()
                .id(1L)
                .name("Test")
                .email("test@test.com")
                .build();
        RegistrationFormDto form = new RegistrationFormDto();
        form.setName("Test");
        form.setEmail("test@test.com");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(attendeeRepository.findByEmail("test@test.com")).thenReturn(Optional.of(attendee));
        when(registrationRepository.findByEventIdAndAttendeeId(1L, 1L)).thenReturn(Optional.empty());
        when(eventRepository.countConfirmedRegistrations(1L)).thenReturn(1L);
        when(registrationRepository.countWaitlisted(1L)).thenReturn(0L);
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Registration result = registrationService.register(1L, form);

        assertEquals(RegistrationStatus.WAITLISTED, result.getStatus());
        assertEquals(1, result.getWaitlistPosition());
    }

    @Test
    void register_throwsDuplicateRegistrationException() {
        Event event = Event.builder()
                .id(1L)
                .capacity(10)
                .status(EventStatus.PUBLISHED)
                .build();
        Attendee attendee = Attendee.builder()
                .id(1L)
                .name("Test")
                .email("test@test.com")
                .build();
        RegistrationFormDto form = new RegistrationFormDto();
        form.setName("Test");
        form.setEmail("test@test.com");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(attendeeRepository.findByEmail("test@test.com")).thenReturn(Optional.of(attendee));
        when(registrationRepository.findByEventIdAndAttendeeId(1L, 1L)).thenReturn(Optional.of(Registration.builder().build()));

        assertThrows(DuplicateRegistrationException.class, () -> registrationService.register(1L, form));
    }

    @Test
    void register_throwsWhenEventNotPublished() {
        Event event = Event.builder()
                .id(1L)
                .capacity(10)
                .status(EventStatus.CANCELLED)
                .build();
        RegistrationFormDto form = new RegistrationFormDto();
        form.setName("Test");
        form.setEmail("test@test.com");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        assertThrows(IllegalStateException.class, () -> registrationService.register(1L, form));
    }

    @Test
    void promoteNextFromWaitlist_promotesFirstWaitlistedAttendee() {
        Attendee attendee1 = Attendee.builder().id(1L).name("Alice").email("alice@test.com").build();
        Attendee attendee2 = Attendee.builder().id(2L).name("Bob").email("bob@test.com").build();
        Event event = Event.builder().id(1L).capacity(1).status(EventStatus.PUBLISHED).build();
        Registration confirmed = Registration.builder()
                .id(10L)
                .event(event)
                .attendee(attendee1)
                .status(RegistrationStatus.CONFIRMED)
                .build();
        Registration waitlisted = Registration.builder()
                .id(11L)
                .event(event)
                .attendee(attendee2)
                .status(RegistrationStatus.WAITLISTED)
                .waitlistPosition(1)
                .build();

        when(registrationRepository.findById(10L)).thenReturn(Optional.of(confirmed));
        when(registrationRepository.findWaitlistedByEventIdOrdered(1L)).thenReturn(List.of(waitlisted));
        when(registrationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        registrationService.cancel(10L);

        assertEquals(RegistrationStatus.CONFIRMED, waitlisted.getStatus());
        assertNull(waitlisted.getWaitlistPosition());
    }
}
