package com.ecommerce.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

@Data
public class ImportResultDTO {
    private int successCount;
    private int failCount;
    private int totalCount;
    private List<ImportError> errors = new ArrayList<>();

    public void addError(int row, String reason) {
        ImportError error = new ImportError();
        error.setRow(row);
        error.setReason(reason);
        this.errors.add(error);
    }

    @Data
    public static class ImportError {
        private int row;
        private String reason;
    }
}
