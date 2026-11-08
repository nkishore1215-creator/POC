package com.example.cartrecovery.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class CustomerRequest {

    @NotBlank(message = "First name is required.")
    private String firstName;

    @NotBlank(message = "Last name is required.")
    private String lastName;

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    private String email;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    @NotBlank(message = "Password is required")
    private String password;
    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @NotBlank(message = "Phone Number is required")
    private String phone;

    private boolean emailOptIn;

    public CustomerRequest() {
    }

    public CustomerRequest(
            String firstName,
            String lastName,
            String email,
            String phone,
            boolean emailOptIn) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.phone = phone;
        this.emailOptIn = emailOptIn;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEmailOptIn() {
        return emailOptIn;
    }

    public void setEmailOptIn(boolean emailOptIn) {
        this.emailOptIn = emailOptIn;
    }
}
