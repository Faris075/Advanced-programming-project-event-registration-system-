package com.evently.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown by RegistrationService when a requested event does not exist or is not PUBLISHED.
 * GlobalControllerAdvice maps this to a 404 error page.
 *
 * OWNER: Faris
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class EventNotFoundException extends RuntimeException {

    public EventNotFoundException(Long eventId) {
        super("Event not found: id=" + eventId);
    }

    public EventNotFoundException(String message) {
        super(message);
    }
}
