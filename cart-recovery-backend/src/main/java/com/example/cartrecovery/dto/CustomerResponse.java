package com.example.cartrecovery.dto;

import java.time.LocalDateTime;

public class CustomerResponse {

    private Long customerId;
    private String firstName;
    private String lastName;
    private String email;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    private String password;

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    private String phone;
    private boolean emailOptIn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CustomerResponse() {
    }

    public CustomerResponse(
            Long customerId,
            String firstName,
            String lastName,
            String email,
            String phone,
            boolean emailOptIn,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
             String password) {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email= email;
        this.phone = phone;
        this.emailOptIn = emailOptIn;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.password = password;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
