package com.evently.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Manages the authenticated user's profile.
 *
 * OWNER: Alei
 *
 * TODO (Alei): Implement all handler methods below.
 *   - GET  /profile            → show profile page (current user's name, email, currency)
 *   - POST /profile/update     → update name and/or email (check uniqueness if email changed)
 *   - POST /profile/password   → verify current password, save new BCrypt hash
 *   - POST /profile/currency   → update preferred currency code
 *   - POST /profile/delete     → verify password confirmation, delete account, invalidate session
 *
 * Use SecurityContextHolder to get the authenticated user's email, then load the User entity.
 */
@Controller
@RequestMapping("/profile")
public class ProfileController {

    // TODO (Alei): inject UserRepository and PasswordEncoder via constructor

    @GetMapping
    public String profilePage() {
        // TODO
        return "profile/index";
    }

    @PostMapping("/update")
    public String updateProfile() {
        // TODO
        return "redirect:/profile";
    }

    @PostMapping("/password")
    public String changePassword() {
        // TODO
        return "redirect:/profile";
    }

    @PostMapping("/currency")
    public String updateCurrency() {
        // TODO
        return "redirect:/profile";
    }

    @PostMapping("/delete")
    public String deleteAccount() {
        // TODO: verify password, delete user, invalidate session, redirect to /
        return "redirect:/";
    }
}
