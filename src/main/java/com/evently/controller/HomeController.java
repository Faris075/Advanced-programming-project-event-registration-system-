package com.evently.controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the application home/landing page.
 *
 * OWNER: Mohamed Ehab
 */
@Controller
public class HomeController {

    /**
     * Unauthenticated visitors are sent to the login page.
     * Authenticated users are sent to the events listing.
     */
    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/events";
        }
        return "redirect:/auth/login";
    }
}
