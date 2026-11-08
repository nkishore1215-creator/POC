package com.example.cartrecovery.service;

import com.example.cartrecovery.dto.BulkSmsResponse;
import com.example.cartrecovery.dto.BulkSmsResponse.RowFailure;
import com.example.cartrecovery.exception.SmsNotConfiguredException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class BulkSmsService {
    private final ObjectProvider<SmsSender> smsSender;

    public BulkSmsService(ObjectProvider<SmsSender> smsSender) {
        this.smsSender = smsSender;
    }

    /**
     * Reads the first worksheet, with an optional phone/mobile header in column A.
     * Numbers must include their country code; text cells preserve leading plus signs.
     * Sent counts provider acceptance, not confirmed delivery. Blank and duplicate
     * numbers are skipped. Row numbers in failures are one-based Excel row numbers.
     */
    public BulkSmsResponse processExcel(MultipartFile file, String message) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("An Excel file is required.");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("SMS message must not be empty.");
        }

        List<Recipient> recipients = readRecipients(file);
        SmsSender sender = smsSender.getIfAvailable();
        if (sender == null) {
            throw new SmsNotConfiguredException();
        }

        int sent = 0;
        int skipped = 0;
        Set<String> seen = new HashSet<>();
        List<RowFailure> failures = new ArrayList<>();
        for (Recipient recipient : recipients) {
            String number = recipient.number().replaceAll("[\\s()\\-]", "");
            if (number.isBlank()) {
                skipped++;
                continue;
            }
            if (!number.matches("\\+?[1-9][0-9]{6,14}")) {
                failures.add(new RowFailure(recipient.row(), "Invalid phone number; include country code."));
                continue;
            }
            if (!number.startsWith("+")) {
                number = "+" + number;
            }
            if (!seen.add(number)) {
                skipped++;
                continue;
            }
            try {
                sender.send(number, message);
                sent++;
            } catch (RuntimeException exception) {
                failures.add(new RowFailure(recipient.row(), "SMS provider rejected or could not accept the message."));
            }
        }
        return new BulkSmsResponse(recipients.size(), sent, failures.size(), skipped, failures);
    }

    private List<Recipient> readRecipients(MultipartFile file) {
        List<Recipient> recipients = new ArrayList<>();
        try (InputStream input = file.getInputStream(); Workbook workbook = WorkbookFactory.create(input)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw new IllegalArgumentException("Excel file must contain a worksheet.");
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            for (Row row : sheet) {
                String number = formatter.formatCellValue(row.getCell(0)).trim();
                String header = number.toLowerCase(Locale.ROOT).replaceAll("[\\s_]", "");
                if (row.getRowNum() == sheet.getFirstRowNum()
                        && Set.of("phone", "phonenumber", "mobile", "mobilenumber").contains(header)) {
                    continue;
                }
                recipients.add(new Recipient(row.getRowNum() + 1, number));
            }
        } catch (IOException | RuntimeException exception) {
            throw new IllegalArgumentException("Unable to read Excel file. Upload a valid, unencrypted .xls or .xlsx file.", exception);
        }
        if (recipients.stream().allMatch(recipient -> recipient.number().isBlank())) {
            throw new IllegalArgumentException("Excel file contains no phone numbers in column A.");
        }
        return recipients;
    }

    private record Recipient(int row, String number) {}
}
