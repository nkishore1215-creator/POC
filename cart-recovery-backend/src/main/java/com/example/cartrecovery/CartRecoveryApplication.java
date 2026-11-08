package com.example.cartrecovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class CartRecoveryApplication {

    public static void main(String[] args) {
        SpringApplication.run(CartRecoveryApplication.class, args);
    }

}
