package com.evently.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by RegistrationService when an attendee tries to register
 * for an event they are already registered for (confirmed or waitlisted).
 *
 * GlobalControllerAdvice redirects the user back to the event page with an error flash.
 *
 * OWNER: Faris
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateRegistrationException extends RuntimeException {

    public DuplicateRegistrationException(Long eventId, String email) {
        super("Attendee " + email + " is already registered for event id=" + eventId);
    }
}
