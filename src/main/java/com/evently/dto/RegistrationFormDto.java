package com.evently.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Form-backing bean for the event registration form (Step 1 of the booking flow).
 *
 * OWNER: Mohamed Ahmed
 */
@Getter
@Setter
public class RegistrationFormDto {

    @NotBlank(message = "Name is required.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    /** Optional phone – validated as a basic international-friendly pattern if provided. */
    @Pattern(regexp = "^[+]?[0-9 \\-().]{7,20}$", message = "Please enter a valid phone number.")
    private String phone;
}
