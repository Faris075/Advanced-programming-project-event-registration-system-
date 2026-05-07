package com.evently.controller.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.dto.UserRegistrationDto;
import com.evently.repository.UserRepository;

import jakarta.validation.Valid;

/**
 * Handles user registration and login page rendering.
 *
 * (Spring Security handles the actual POST /auth/login authentication — this
 *  controller only needs to serve the GET login page.)
 *
 * OWNER: Alei
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    @SuppressWarnings("unused")
    private final UserRepository  userRepository;
    @SuppressWarnings("unused")
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository  = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Show the login page. Spring Security handles POST /auth/login automatically. */
    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /** Show the registration form. */
    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registrationDto", new UserRegistrationDto());
        return "auth/register";
    }

    /**
     * Process the registration form.
     *
     * Steps:
     *   1. Return to form if validation errors exist.
     *   2. Check passwords match — add field error if not.
     *   3. Check email is not already taken — add field error if so.
     *   4. Encode password with passwordEncoder.encode().
     *   5. Build and save a new User entity.
     *   6. Redirect to /auth/login with a flash message.
     */
    @PostMapping("/register")
    public String register(@Valid UserRegistrationDto dto,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        // Return to form if validation errors exist.
        if (result.hasErrors()) {
            return "auth/register";
        }

        // Check passwords match
        if (!dto.getPassword().equals(dto.getPasswordConfirm())) {
            result.rejectValue("passwordConfirm", "password.mismatch",
                    "Passwords do not match");
            return "auth/register";
        }

        // Check email is not already taken
        if (userRepository.existsByEmail(dto.getEmail())) {
            result.rejectValue("email", "email.taken",
                    "An account with this email already exists");
            return "auth/register";
        }

        // Encode password and create new User entity
        String encodedPassword = passwordEncoder.encode(dto.getPassword());
        com.evently.model.User user = com.evently.model.User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .password(encodedPassword)
                .isAdmin(false)
                .isSuperAdmin(false)
                .currencyPreference("USD")
                .build();

        if (user != null) {
            userRepository.save(user);
        }
        redirectAttributes.addFlashAttribute("success",
                "Registration successful! Please log in.");
        return "redirect:/auth/login";
    }
}
