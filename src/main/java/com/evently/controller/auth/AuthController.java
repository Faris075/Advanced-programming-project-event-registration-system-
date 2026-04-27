package com.evently.controller.auth;

import com.evently.dto.UserRegistrationDto;
import com.evently.repository.UserRepository;
import com.evently.model.User;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Handles user registration and login page rendering.
 *
 * (Spring Security handles the actual POST /auth/login authentication — this
 *  controller only needs to serve the GET login page.)
 *
 * OWNER: Alei
 *
 * TODO (Alei):
 *   1. POST /auth/register — validate DTO, check passwords match, check email uniqueness,
 *      hash password with BCrypt, save User, redirect to /auth/security-setup.
 *   2. Ensure all error messages use Thymeleaf th:errors binding.
 */
@Controller
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository  userRepository;
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
     * TODO (Alei): Process the registration form.
     *
     * Steps:
     *   1. Return to form if validation errors exist.
     *   2. Check passwords match — add field error if not.
     *   3. Check email is not already taken — add field error if so.
     *   4. Encode password with passwordEncoder.encode().
     *   5. Build and save a new User entity.
     *   6. Redirect to /auth/security-setup with a flash message.
     */
    @PostMapping("/register")
    public String register(@Valid UserRegistrationDto dto,
                           BindingResult result,
                           RedirectAttributes redirectAttributes) {
        // TODO (Alei): implement
        return "auth/register";
    }
}
