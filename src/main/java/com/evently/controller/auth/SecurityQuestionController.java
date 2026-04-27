package com.evently.controller.auth;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Multi-step password recovery using the user's personal security question.
 *
 * Flow:
 *   Step 1 — GET  /auth/forgot-password         → enter email
 *   Step 1 — POST /auth/forgot-password         → look up user, store email in session
 *   Step 2 — GET  /auth/forgot-password/question → show user's security question
 *   Step 2 — POST /auth/forgot-password/question → verify BCrypt answer
 *   Step 3 — GET  /auth/forgot-password/reset    → new password form
 *   Step 3 — POST /auth/forgot-password/reset    → save new password, clear session, redirect to login
 *
 * SECURITY NOTE:
 *   Each step must validate that the session holds the expected state from
 *   the previous step.  A user navigating directly to step 3 must be
 *   redirected to step 1 — never let them skip steps.
 *
 *   Use the same error message for "email not found" and "question not set"
 *   to prevent user enumeration attacks.
 *
 * OWNER: Alei
 *
 * TODO (Alei): Implement all six handler methods below.
 */
@Controller
@RequestMapping("/auth/forgot-password")
public class SecurityQuestionController {

    // TODO (Alei): inject UserRepository and PasswordEncoder via constructor

    @GetMapping
    public String forgotPasswordStep1() {
        // TODO: show email entry form
        return "auth/forgot-password";
    }

    @PostMapping
    public String forgotPasswordStep1Submit() {
        // TODO: look up user by email, store session attribute "recoveryEmail"
        //       redirect to /auth/forgot-password/question
        //       use same error message whether email exists or not (anti-enumeration)
        return "redirect:/auth/forgot-password/question";
    }

    @GetMapping("/question")
    public String forgotPasswordStep2() {
        // TODO: guard – redirect to step 1 if session "recoveryEmail" is missing
        //       load user, put security question in model
        return "auth/forgot-password-question";
    }

    @PostMapping("/question")
    public String forgotPasswordStep2Submit() {
        // TODO: compare submitted answer to stored BCrypt hash with passwordEncoder.matches()
        //       on success: store "recoveryUserId" in session, redirect to step 3
        //       on failure: show error on the same page
        return "redirect:/auth/forgot-password/reset";
    }

    @GetMapping("/reset")
    public String forgotPasswordStep3() {
        // TODO: guard – redirect to step 1 if session "recoveryUserId" is missing
        return "auth/forgot-password-reset";
    }

    @PostMapping("/reset")
    public String forgotPasswordStep3Submit() {
        // TODO: validate new password + confirm, hash with BCrypt, save, clear session attrs
        //       redirect to /auth/login with success flash
        return "redirect:/auth/login";
    }
}
