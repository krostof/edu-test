package com.edutest.domain.assignment.coding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class ExecutionLimitCheck {
    private boolean timeExceeded;
    private boolean memoryExceeded;
    private boolean withinLimits;
    private long executionTime;
    private int memoryUsed;
    private Integer timeLimit;
    private Integer memoryLimit;

    public String getStatus() {
        if (withinLimits) return "WITHIN_LIMITS";
        if (timeExceeded && memoryExceeded) return "TIME_AND_MEMORY_EXCEEDED";
        if (timeExceeded) return "TIME_LIMIT_EXCEEDED";
        if (memoryExceeded) return "MEMORY_LIMIT_EXCEEDED";
        return "UNKNOWN";
    }
}
