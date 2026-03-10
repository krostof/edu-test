package com.edutest.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignGroupRequest {
    @NotNull
    private Long groupId;
}
