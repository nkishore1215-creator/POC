package com.example.cartrecovery.controller;

import com.example.cartrecovery.dto.CustomerRequest;
import com.example.cartrecovery.dto.CustomerResponse;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(
            @Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.createCustomer(toEntity(request));
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(customer));
    }

    @GetMapping("/{customerId}")
    public CustomerResponse getCustomer(@PathVariable Long customerId) {
        return toResponse(customerService.getCustomerById(customerId));
    }

    @GetMapping
    public List<CustomerResponse> getCustomers(
            @RequestParam(required = false) String email) {
        if (email != null && !email.isBlank()) {
            return List.of(toResponse(
                    customerService.getCustomerByEmail(email)
            ));
        }

        return customerService.getAllCustomers()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @PutMapping("/{customerId}")
    public CustomerResponse updateCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody CustomerRequest request) {
        Customer customer = customerService.updateCustomer(
                customerId,
                toEntity(request)
        );
        return toResponse(customer);
    }

    @DeleteMapping("/{customerId}")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
        customerService.deleteCustomer(customerId);
        return ResponseEntity.noContent().build();
    }

    private Customer toEntity(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setEmailOptIn(request.isEmailOptIn());
        customer.setPhone(request.getPhone());
        customer.setPassword(request.getPassword());
        return customer;
    }

    private CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.isEmailOptIn(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                customer.getPassword()

        );
    }
}
