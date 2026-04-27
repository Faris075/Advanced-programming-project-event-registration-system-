package com.evently.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the application home/landing page.
 *
 * OWNER: Mohamed Ehab
 */
@Controller
public class HomeController {

    /** Redirect root to the public events listing. */
    @GetMapping("/")
    public String home() {
        return "redirect:/events";
    }
}
