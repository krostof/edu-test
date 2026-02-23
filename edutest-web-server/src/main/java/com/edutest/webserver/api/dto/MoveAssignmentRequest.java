package com.edutest.webserver.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MoveAssignmentRequest {
    @NotNull
    private Integer newOrderNumber;
}
