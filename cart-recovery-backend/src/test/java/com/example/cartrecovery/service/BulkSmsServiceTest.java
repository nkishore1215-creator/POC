package com.example.cartrecovery.service;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BulkSmsServiceTest {
    private final SmsSender sender = mock(SmsSender.class);

    private BulkSmsService service(boolean configured) {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        if (configured) {
            beans.addBean("smsSender", sender);
        }
        return new BulkSmsService(beans.getBeanProvider(SmsSender.class));
    }

    @Test
    void reportsInvalidRowsDuplicatesAndProviderFailures() throws Exception {
        doThrow(new IllegalStateException("Provider failure"))
                .when(sender).send("+14155550102", "Hello");

        var response = service(true).processExcel(excel("phone", "+1 (415) 555-0101",
                "14155550101", "invalid", "", "+14155550102", "+14155550103"), "Hello");

        assertEquals(6, response.total());
        assertEquals(2, response.sent());
        assertEquals(2, response.failed());
        assertEquals(2, response.skipped());
        assertEquals(4, response.failures().get(0).row());
        assertEquals(6, response.failures().get(1).row());
        verify(sender).send("+14155550101", "Hello");
        verify(sender).send("+14155550102", "Hello");
        verify(sender).send("+14155550103", "Hello");
        verifyNoMoreInteractions(sender);
    }

    @Test
    void rejectsMissingProviderAndInvalidInput() throws Exception {
        assertThrows(IllegalStateException.class,
                () -> service(false).processExcel(excel("+14155550101"), "Hello"));
        assertThrows(IllegalArgumentException.class,
                () -> service(true).processExcel(excel("phone"), "Hello"));
        assertThrows(IllegalArgumentException.class,
                () -> service(true).processExcel(excel("+14155550101"), " "));
        assertThrows(IllegalArgumentException.class,
                () -> service(true).processExcel(new MockMultipartFile("file", new byte[]{1, 2, 3}), "Hello"));
        verifyNoInteractions(sender);
    }

    private MockMultipartFile excel(String... numbers) throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            for (int i = 0; i < numbers.length; i++) {
                sheet.createRow(i).createCell(0).setCellValue(numbers[i]);
            }
            workbook.write(output);
            return new MockMultipartFile("file", "phones.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
        }
    }
}
