package com.evently.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


/**
 * Form-backing bean for the event registration form (Step 1 of the booking flow).
 *
 * OWNER: Mohamed Ahmed
 */

public class RegistrationFormDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address.")
    private String email;

    public RegistrationFormDto(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

}



