package com.edutest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveAssignmentRequest {
    @NotNull
    private Integer newOrderNumber;
}
