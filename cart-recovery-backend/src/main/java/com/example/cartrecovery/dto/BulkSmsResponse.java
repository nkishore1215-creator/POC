package com.example.cartrecovery.dto;

import java.util.List;

public record BulkSmsResponse(int total, int sent, int failed, int skipped,
                              List<RowFailure> failures) {
    public BulkSmsResponse {
        failures = List.copyOf(failures);
    }

    public record RowFailure(int row, String reason) {}
}
