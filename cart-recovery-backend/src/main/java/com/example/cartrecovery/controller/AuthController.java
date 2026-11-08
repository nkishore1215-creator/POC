package com.example.cartrecovery.controller;


import com.example.cartrecovery.dto.LoginRequest;
import com.example.cartrecovery.entity.Customer;
import com.example.cartrecovery.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final CustomerRepository customerRepository;

    public AuthController(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }
@PostMapping("/login")
  public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

   Optional<Customer> customerOptional=customerRepository.findByEmail(loginRequest.getEmail());

   if(customerOptional.isEmpty()){
       return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid email or password");
   }
   Customer customer=customerOptional.get();
    if (!customer.getPassword().equals(loginRequest.getPassword())) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Incorrect password");}
      Map<String, Object> response = new HashMap<>();
    response.put("email", customer.getEmail());
   response.put("lastname", customer.getLastName());
    response.put("firstname", customer.getFirstName());
    response.put("id", customer.getCustomerId());
    response.put("message", "Login Successful");

    return ResponseEntity.ok(response);
  }





}
