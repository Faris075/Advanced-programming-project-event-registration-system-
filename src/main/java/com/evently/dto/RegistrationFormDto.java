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

//  public class RegristrationFormDto{

//  Private String Name;

//  Private String Email;

 
// pulic RegistrationFormDto{

//     this.Name = "";
//     this.Email = "";


// }


// public String getName(){
//     return Name;
// }

// public String getEmail(){
//     return Email;
// }
// public void setName(String name){
//     this.Name = name;
// }
// public void setEmail(String email){
//     this.Email = email;
// }



//  }



@Getter
@Setter
public class RegistrationFormDto {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    private String email;
}