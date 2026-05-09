package com.evently.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

import jakarta.persistence.EntityNotFoundException;

/**
 * Global exception handler.
 *
 * Maps well-known application exceptions to the correct error views.
 * Deliberately does NOT catch Spring framework/MVC internal exceptions
 * (BindException, MethodArgumentNotValidException, etc.) so they are
 * handled by Spring's own infrastructure.
 *
 * OWNER: Faris
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    @ExceptionHandler({EventNotFoundException.class, EntityNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(Exception ex, Model model) {
        log.warn("Not found: {}", ex.getMessage());
        model.addAttribute("message", ex.getMessage());
        return "error/404";
    }

    @ExceptionHandler(DuplicateRegistrationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handleDuplicateReg(DuplicateRegistrationException ex, Model model) {
        log.info("Duplicate registration attempt: {}", ex.getMessage());
        model.addAttribute("message", "You are already registered for this event.");
        return "error/duplicate";
    }

    /**
     * Catch-all for unhandled RuntimeExceptions thrown by application code.
     * Intentionally limited to RuntimeException — Spring MVC / Thymeleaf
     * internal exceptions (which extend Exception but not RuntimeException)
     * are left for Spring's DefaultHandlerExceptionResolver.
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneric(RuntimeException ex, Model model) {
        log.error("Unexpected error", ex);
        model.addAttribute("message", "An unexpected error occurred. Please try again.");
        return "error/500";
    }
}
