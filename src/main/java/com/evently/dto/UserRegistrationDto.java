package com.evently.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Form-backing bean for the user registration form (/auth/register).
 *
 * Bean Validation annotations provide server-side field validation.
 * Cross-field check (password == passwordConfirm) is done in AuthController
 * before calling the service.
 *
 * OWNER: Alei
 */
@Getter
@Setter
public class UserRegistrationDto {

    @NotBlank(message = "Name is required.")
    @Size(max = 100, message = "Name must be 100 characters or fewer.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Email(message = "Please enter a valid email address.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must be at least 8 characters.")
    private String password;

    @NotBlank(message = "Please confirm your password.")
    private String passwordConfirm;
}
