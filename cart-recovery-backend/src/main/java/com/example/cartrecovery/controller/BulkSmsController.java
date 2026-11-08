package com.example.cartrecovery.controller;

import com.example.cartrecovery.dto.BulkSmsResponse;
import com.example.cartrecovery.service.BulkSmsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sms")
public class BulkSmsController {

    private final BulkSmsService bulkSmsService;

    public BulkSmsController(BulkSmsService bulkSmsService) {
        this.bulkSmsService = bulkSmsService;
    }

    @PostMapping("/bulk-send")
    public ResponseEntity<BulkSmsResponse> sendBulkSms(
            @RequestParam("file") MultipartFile file,
            @RequestParam("message") String message) {

        BulkSmsResponse response =
                bulkSmsService.processExcel(file, message);

        return ResponseEntity.ok(response);
    }
}
