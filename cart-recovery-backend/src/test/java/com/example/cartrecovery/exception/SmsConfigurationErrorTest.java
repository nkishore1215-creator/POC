package com.example.cartrecovery.exception;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SmsConfigurationErrorTest {
    @Test
    void missingProviderReturnsServiceUnavailableInsteadOfConflict() throws Exception {
        var mvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/test-sms"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SMS_NOT_CONFIGURED"))
                .andExpect(jsonPath("$.status").value(503));
    }

    @RestController
    static class TestController {
        @GetMapping("/test-sms")
        public void send() {
            throw new SmsNotConfiguredException();
        }
    }
}
