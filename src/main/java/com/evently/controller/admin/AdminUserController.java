package com.evently.controller.admin;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.evently.model.User;
import com.evently.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Admin management of user accounts.
 *
 * OWNER: Alei
 *
 * Endpoints:
 *   - GET  /admin/users              → paginated list of all users
 *   - GET  /admin/users/{id}         → show user details + their registrations
 *   - POST /admin/users/{id}/promote → promote user to admin (super admin only)
 *   - POST /admin/users/{id}/demote  → demote admin to user (super admin only)
 *   - POST /admin/users/{id}/reset-password → generate temp password for admin to share
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Get the currently logged-in user from the database.
     */
    private User getCurrentUser() {
        String email = (String) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Authenticated user not found"));
    }

    @GetMapping
    public String listUsers(@RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            Model model) {
        Page<User> users = userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        model.addAttribute("users", users);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", users.getTotalPages());
        return "admin/users/index";
    }

    @GetMapping("/{id}")
    public String showUser(@PathVariable Long id,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        
        var userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }

        User user = userOpt.get();
        model.addAttribute("user", user);
        return "admin/users/show";
    }

    @PostMapping("/{id}/promote")
    public String promoteUser(@PathVariable Long id,
                              RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        
        User currentUser = getCurrentUser();
        var userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }

        User user = userOpt.get();

        // Prevent self-promotion
        if (user.getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error",
                    "You cannot promote yourself.");
            return "redirect:/admin/users/" + id;
        }

        // Already admin
        if (user.isAdmin()) {
            redirectAttributes.addFlashAttribute("info",
                    "User is already an admin.");
            return "redirect:/admin/users/" + id;
        }

        user.setAdmin(true);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success",
                "User promoted to admin.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/demote")
    public String demoteUser(@PathVariable Long id,
                             RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        
        User currentUser = getCurrentUser();
        var userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }

        User user = userOpt.get();

        // Prevent self-demotion
        if (user.getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error",
                    "You cannot demote yourself.");
            return "redirect:/admin/users/" + id;
        }

        // Not admin
        if (!user.isAdmin()) {
            redirectAttributes.addFlashAttribute("info",
                    "User is already a regular user.");
            return "redirect:/admin/users/" + id;
        }

        user.setAdmin(false);
        userRepository.save(user);

        redirectAttributes.addFlashAttribute("success",
                "User demoted to regular user.");
        return "redirect:/admin/users/" + id;
    }

    @PostMapping("/{id}/reset-password")
    public String resetPassword(@PathVariable Long id,
                                RedirectAttributes redirectAttributes) {
        if (id == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }
        
        var userOpt = userRepository.findById(id);

        if (userOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/users";
        }

        User user = userOpt.get();

        // Generate a random 10-character password
        String tempPassword = UUID.randomUUID().toString().substring(0, 10);
        String encodedPassword = passwordEncoder.encode(tempPassword);

        user.setPassword(encodedPassword);
        userRepository.save(user);

        // Add the plaintext temp password to flash so admin can see it once
        redirectAttributes.addFlashAttribute("tempPassword", tempPassword);
        redirectAttributes.addFlashAttribute("success",
                "Password reset. Temporary password: " + tempPassword +
                        " (shown only once — share securely with user)");

        return "redirect:/admin/users/" + id;
    }
}
