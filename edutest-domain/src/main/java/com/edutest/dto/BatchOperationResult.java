package com.edutest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of batch operation on multiple users.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchOperationResult {

    private int successCount;

    private int failedCount;

    @Builder.Default
    private List<String> errors = new ArrayList<>();

    public void addError(Long userId, String errorMessage) {
        errors.add(String.format("User ID %d: %s", userId, errorMessage));
        failedCount++;
    }

    public void incrementSuccess() {
        successCount++;
    }
}
