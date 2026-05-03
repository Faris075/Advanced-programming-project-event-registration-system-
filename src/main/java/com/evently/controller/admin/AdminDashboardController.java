package com.evently.controller.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.evently.model.EventStatus;
import com.evently.model.RegistrationStatus;
import com.evently.repository.EventRepository;
import com.evently.repository.RegistrationRepository;
import com.evently.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("totalEvents", eventRepository.count());
        model.addAttribute("publishedEvents", eventRepository.countByStatus(EventStatus.PUBLISHED));
        model.addAttribute("totalRegistrations", registrationRepository.countAll());
        model.addAttribute("confirmedRegistrations", registrationRepository.countByStatus(RegistrationStatus.CONFIRMED));
        model.addAttribute("waitlistedRegistrations", registrationRepository.countByStatus(RegistrationStatus.WAITLISTED));
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("recentEvents", eventRepository.findTop5ByOrderByCreatedAtDesc());
        return "admin/dashboard";
    }
}
