package com.edutest.webserver.api.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignGroupRequest {
    @NotNull
    private Long groupId;
}
