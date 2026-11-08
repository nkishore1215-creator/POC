package com.example.cartrecovery.service;

import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.exception.CustomerNotFoundException;
import com.example.cartrecovery.repository.CustomerRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    public Customer createCustomer(Customer customer) {

        if (customerRepository.existsByEmail(customer.getEmail())) {
            throw new IllegalArgumentException(
                    "Customer already exists with email: " + customer.getEmail()
            );
        }

        if (customer.getCreatedAt() == null) {
            customer.setCreatedAt(LocalDateTime.now());
        }

        return customerRepository.save(customer);
    }

    public Customer getCustomerById(Long customerId) {

        return customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }

    public Customer getCustomerByEmail(String email) {

        return customerRepository.findByEmail(email)
                .orElseThrow(() -> new CustomerNotFoundException(email));
    }

    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    public Customer updateCustomer(
            Long customerId,
            Customer updatedCustomer) {

        Customer existingCustomer = getCustomerById(customerId);

        if (!existingCustomer.getEmail().equals(updatedCustomer.getEmail())
                && customerRepository.existsByEmail(updatedCustomer.getEmail())) {

            throw new IllegalArgumentException(
                    "Another customer already exists with email: "
                            + updatedCustomer.getEmail()
            );
        }

        existingCustomer.setFirstName(updatedCustomer.getFirstName());
        existingCustomer.setLastName(updatedCustomer.getLastName());
        existingCustomer.setEmail(updatedCustomer.getEmail());
        existingCustomer.setPhone(updatedCustomer.getPhone());
        existingCustomer.setEmailOptIn(updatedCustomer.isEmailOptIn());
        existingCustomer.setUpdatedAt(LocalDateTime.now());

        return customerRepository.save(existingCustomer);
    }

    public void deleteCustomer(Long customerId) {

        Customer customer = getCustomerById(customerId);
        customerRepository.delete(customer);
    }
}
