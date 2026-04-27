# Mohamed Ahmed — Task Sheet
## Project: Evently Event Registration System

**Your branch:** `feature/ahmed-registration`
**Merge target:** `develop`
**Depends on:** Faris (entities + repositories with pessimistic lock query), Alei (SecurityConfig — needed to protect `/dashboard`), Ehab (`events/show.html` registration form + `RegistrationFormDto` fields)

---

## Your Role
You own the most complex feature: the registration and waitlist system. When a user clicks "Register Now" on an event, your code handles everything — checking capacity, assigning waitlist positions, processing mock payment, sending confirmation emails, and promoting waitlisted attendees when a spot opens up. You also own the user dashboard that shows a person's registration history.

---

## Prerequisites
- Pull `develop` after Faris + Alei + Ehab are merged
- Verify the `RegistrationRepository` has the `findByIdWithLock` method with `@Lock(LockModeType.PESSIMISTIC_WRITE)` — this is critical for your concurrency handling
- Verify `EventRepository` has `countConfirmedRegistrations(@Param("eventId") Long eventId)`

---

## Step-by-Step Tasks

### Step 1 — RegistrationFormDto
Create or confirm `src/main/java/com/evently/dto/RegistrationFormDto.java`:

```java
package com.evently.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegistrationFormDto {
    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;

    @Pattern(regexp = "^[+0-9 \\-()]{7,20}$|^$",
             message = "Please enter a valid phone number")
    private String phone;
}
```

### Step 2 — PaymentDto
Create or confirm `src/main/java/com/evently/dto/PaymentDto.java`:

```java
package com.evently.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PaymentDto {
    @NotBlank
    @Pattern(regexp = "\\d{16}", message = "Card number must be 16 digits")
    private String cardNumber;

    @NotBlank(message = "Cardholder name is required")
    private String cardHolderName;

    @Min(1) @Max(12)
    private int expiryMonth;

    @Min(2024) @Max(2040)
    private int expiryYear;

    @NotBlank
    @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3 or 4 digits")
    private String cvv;
}
```

> **IMPORTANT:** Never save any payment data to the database. Never log card numbers. The mock payment is only validated in memory and then discarded.

### Step 3 — EmailService
Create `src/main/java/com/evently/service/EmailService.java`:

```java
package com.evently.service;

import com.evently.model.Event;
import com.evently.model.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendConfirmationEmail(Registration registration) {
        try {
            Event event = registration.getEvent();
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(registration.getAttendee().getEmail());
            msg.setSubject("Registration Confirmed — " + event.getTitle());
            msg.setText(String.format(
                "Hi %s,\n\nYour registration for \"%s\" on %s is confirmed.\n\nSee you there!\n— Evently",
                registration.getAttendee().getName(),
                event.getTitle(),
                event.getDateTime()
            ));
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send confirmation email for registration {}: {}",
                      registration.getId(), e.getMessage());
        }
    }

    @Async
    public void sendWaitlistEmail(Registration registration) {
        try {
            Event event = registration.getEvent();
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(registration.getAttendee().getEmail());
            msg.setSubject("You're on the Waitlist — " + event.getTitle());
            msg.setText(String.format(
                "Hi %s,\n\nThe event \"%s\" is currently full, but you are #%d on the waitlist." +
                " We will email you if a spot opens up.\n\n— Evently",
                registration.getAttendee().getName(),
                event.getTitle(),
                registration.getWaitlistPosition()
            ));
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send waitlist email for registration {}: {}",
                      registration.getId(), e.getMessage());
        }
    }

    @Async
    public void sendWaitlistPromotionEmail(Registration registration) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(registration.getAttendee().getEmail());
            msg.setSubject("Great news — You got a spot! — " + registration.getEvent().getTitle());
            msg.setText(String.format(
                "Hi %s,\n\nA spot opened up for \"%s\" and you have been promoted from the waitlist. " +
                "Your registration is now confirmed!\n\n— Evently",
                registration.getAttendee().getName(),
                registration.getEvent().getTitle()
            ));
            mailSender.send(msg);
        } catch (Exception e) {
            log.error("Failed to send promotion email for registration {}: {}",
                      registration.getId(), e.getMessage());
        }
    }
}
```

For local dev, add these to `application.properties`:
```properties
spring.mail.host=sandbox.smtp.mailtrap.io
spring.mail.port=2525
spring.mail.username=${MAIL_USERNAME:}
spring.mail.password=${MAIL_PASSWORD:}
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

Use [Mailtrap.io](https://mailtrap.io) free plan: sign up, get credentials, put them in your local env vars. Emails will not actually send — they appear in Mailtrap's inbox.

### Step 4 — RegistrationService (core waitlist logic)
Create `src/main/java/com/evently/service/RegistrationService.java`.

This is the heart of the system. All operations must be inside `@Transactional`.

```java
package com.evently.service;

import com.evently.dto.RegistrationFormDto;
import com.evently.exception.DuplicateRegistrationException;
import com.evently.exception.EventNotFoundException;
import com.evently.model.*;
import com.evently.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final EventRepository eventRepository;
    private final AttendeeRepository attendeeRepository;
    private final RegistrationRepository registrationRepository;
    private final EmailService emailService;

    @Transactional
    public Registration register(Long eventId, RegistrationFormDto form) {
        // 1. Load event — PESSIMISTIC lock to prevent concurrent over-booking
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new EventNotFoundException("Event not found: " + eventId));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new IllegalStateException("Event is not open for registration");
        }

        // 2. Find or create attendee
        Attendee attendee = attendeeRepository.findByEmail(form.getEmail())
            .orElseGet(() -> attendeeRepository.save(
                Attendee.builder()
                    .name(form.getName())
                    .email(form.getEmail())
                    .phone(form.getPhone())
                    .build()
            ));

        // 3. Check for duplicate registration
        if (registrationRepository.findByEventIdAndAttendeeId(eventId, attendee.getId()).isPresent()) {
            throw new DuplicateRegistrationException("You are already registered for this event");
        }

        // 4. Check capacity (use pessimistic-locked count)
        long confirmed = registrationRepository.countConfirmedRegistrations(eventId);
        boolean isFull = confirmed >= event.getCapacity();

        // 5. Build registration
        Registration registration = Registration.builder()
            .event(event)
            .attendee(attendee)
            .status(isFull ? RegistrationStatus.WAITLISTED : RegistrationStatus.CONFIRMED)
            .paymentStatus(isFull ? PaymentStatus.PENDING : PaymentStatus.PENDING)
            .waitlistPosition(isFull ? (int)(registrationRepository.countWaitlisted(eventId) + 1) : null)
            .isAdminOverride(false)
            .build();

        Registration saved = registrationRepository.save(registration);

        // 6. Send async email
        if (isFull) {
            emailService.sendWaitlistEmail(saved);
        } else {
            emailService.sendConfirmationEmail(saved);
        }

        return saved;
    }

    /**
     * Called after a registration is cancelled.
     * Promotes the first person on the waitlist to CONFIRMED.
     */
    @Transactional
    public void promoteNextFromWaitlist(Long eventId) {
        List<Registration> waitlisted = registrationRepository
            .findWaitlistedByEventIdOrdered(eventId);
        if (waitlisted.isEmpty()) return;

        Registration next = registrationRepository
            .findByIdWithLock(waitlisted.get(0).getId())
            .orElse(null);
        if (next == null) return;

        next.setStatus(RegistrationStatus.CONFIRMED);
        next.setWaitlistPosition(null);
        registrationRepository.save(next);
        emailService.sendWaitlistPromotionEmail(next);

        // Re-number remaining waitlist
        List<Registration> remaining = registrationRepository
            .findWaitlistedByEventIdOrdered(eventId);
        for (int i = 0; i < remaining.size(); i++) {
            remaining.get(i).setWaitlistPosition(i + 1);
        }
        registrationRepository.saveAll(remaining);
    }

    @Transactional
    public void cancelRegistration(Long registrationId) {
        Registration reg = registrationRepository.findById(registrationId)
            .orElseThrow(() -> new EventNotFoundException("Registration not found"));
        if (reg.getStatus() == RegistrationStatus.CANCELLED) return;

        boolean wasConfirmed = reg.getStatus() == RegistrationStatus.CONFIRMED;
        reg.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(reg);

        if (wasConfirmed) {
            promoteNextFromWaitlist(reg.getEvent().getId());
        }
    }
}
```

### Step 5 — RegistrationController (3-step flow)
Create `src/main/java/com/evently/controller/RegistrationController.java`.

The registration is a 3-step session-based flow:

**Step 1 — POST `/register/{id}` (form submission):**
- Bind `@Valid RegistrationFormDto`
- If validation errors → redirect back to `/events/{id}` with error flash
- Call `registrationService.register(id, form)`
- Store `registration.getId()` in session (`session.setAttribute("pendingRegistrationId", ...)`)
- If confirmed and event is paid: redirect to `/register/{id}/payment`
- If confirmed and event is free: redirect to `/register/{id}/confirmed`
- If waitlisted: redirect to `/register/{id}/waitlisted`

**Step 2 — GET `/register/{id}/payment`:**
- Guard: check session has `pendingRegistrationId`, else redirect to `/events/{id}`
- Add empty `PaymentDto` to model
- Return `"register/payment"` template

**POST `/register/{id}/payment`:**
- Validate `PaymentDto`
- Simulate payment: just flip `paymentStatus = PAID` on the registration
- Clear `pendingRegistrationId` from session
- Redirect to `/register/{id}/confirmed`

**Step 3 — GET `/register/{id}/confirmed`:**
- Load registration by ID
- Return `"register/confirmed"` template

**GET `/register/{id}/waitlisted`:**
- Load registration, return `"register/waitlisted"` template

**POST `/register/{id}/cancel`:**
- Must be authenticated (add `@PreAuthorize("isAuthenticated()")` or use `HttpSession` guard)
- Call `registrationService.cancelRegistration(id)`
- Redirect to `/dashboard?cancelled`

### Step 6 — DashboardController
Create `src/main/java/com/evently/controller/DashboardController.java`:

```java
@GetMapping("/dashboard")
public String dashboard(Authentication authentication, Model model) {
    String email = authentication.getName();
    // Find attendee by email (not user — registrations are linked to attendees)
    Optional<Attendee> attendee = attendeeRepository.findByEmail(email);
    if (attendee.isEmpty()) {
        model.addAttribute("registrations", List.of());
        return "dashboard";
    }
    List<Registration> registrations = registrationRepository
        .findByAttendeeIdOrderByCreatedAtDesc(attendee.get().getId());
    model.addAttribute("registrations", registrations);
    return "dashboard";
}
```

This controller is protected — Spring Security requires authentication for `/dashboard` (set in `SecurityConfig`).

### Step 7 — Thymeleaf templates
Create these in `src/main/resources/templates/`:

| Template | Purpose |
|----------|---------|
| `register/payment.html` | Mock payment form (card number, name, expiry, CVV). Bold warning: "This is a demo — do not enter real card details." |
| `register/confirmed.html` | Success page: "Registration Confirmed!" with event details and "Back to Events" link |
| `register/waitlisted.html` | Waitlisted page: "You are #X on the waitlist" with event name |
| `dashboard.html` | Table of user's registrations with status badges. Cancel button for CONFIRMED registrations where event is in the future. |

---

## Files You Own

| File | Package / Path |
|------|---------------|
| `RegistrationFormDto.java` | `com.evently.dto` |
| `PaymentDto.java` | `com.evently.dto` |
| `EmailService.java` | `com.evently.service` |
| `RegistrationService.java` | `com.evently.service` |
| `RegistrationController.java` | `com.evently.controller` |
| `DashboardController.java` | `com.evently.controller` |
| `register/payment.html` | `templates/register/` |
| `register/confirmed.html` | `templates/register/` |
| `register/waitlisted.html` | `templates/register/` |
| `dashboard.html` | `templates/` |

---

## Concurrency Note
The waitlist uses a database-level pessimistic lock (`SELECT ... FOR UPDATE`) via `findByIdWithLock()`. This prevents two concurrent requests from both seeing "1 spot left" and both confirming — only one transaction wins. Keep all registration state changes inside `@Transactional` methods in `RegistrationService`.

## Payment Security Note
The `PaymentDto` is **never persisted**. Values are validated in memory and then discarded. Do not log `cardNumber`, `cvv`, or `expiryYear`. Do not expose `PaymentDto` in any response body.
