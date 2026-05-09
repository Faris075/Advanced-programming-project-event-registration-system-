package com.evently.web;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Exposes the current request URI to all Thymeleaf templates as ${currentUri}.
 *
 * This replaces direct use of #request.requestURI, which was removed in
 * Thymeleaf 3.1 for security reasons.
 */
@ControllerAdvice
public class RequestUriAdvice {

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
