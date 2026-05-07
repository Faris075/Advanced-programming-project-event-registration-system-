package com.evently.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Form-backing bean for the mock payment form (Step 2 of the booking flow).
 *
 * SECURITY NOTE:
 *   This is a mock payment form only.  These fields are validated for
 *   plausible format but NO card data is ever stored in the database.
 *   The DTO is discarded immediately after the registration is committed.
 *
 * OWNER: Mohamed Ahmed
 */

public class PaymentDto {

    @NotBlank(message = "Card number is required.")
    @Pattern(regexp = "\\d{16}", message = "Card number must be exactly 16 digits.")
    private String cardNumber;

    @NotBlank(message = "Cardholder name is required.")
    private String cardHolderName;

    @NotBlank(message = "Expiry month is required.")
    @Pattern(regexp = "^(0[1-9]|1[0-2])$", message = "Expiry month must be 01–12.")
    private String expiryMonth;

    @NotBlank(message = "Expiry year is required.")
    @Pattern(regexp = "^\\d{4}$", message = "Expiry year must be a 4-digit year.")
    private String expiryYear;

    @NotBlank(message = "CVV is required.")
    @Size(min = 3, max = 4, message = "CVV must be 3 or 4 digits.")
    @Pattern(regexp = "\\d{3,4}", message = "CVV must be numeric.")
    private String cvv;

    public PaymentDto() {}
    

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public String getCardHolderName() {
        return cardHolderName;
    }

    public void setCardHolderName(String cardHolderName) {
        this.cardHolderName = cardHolderName;
    }

    public String getExpiryMonth() {
        return expiryMonth;
    }

    public void setExpiryMonth(String expiryMonth) {
        this.expiryMonth = expiryMonth;
    }

    public String getExpiryYear() {
        return expiryYear;
    }

    public void setExpiryYear(String expiryYear) {
        this.expiryYear = expiryYear;
    }

    public String getCvv() {
        return cvv;
    }

    public void setCvv(String cvv) {
        this.cvv = cvv;
    }
}






