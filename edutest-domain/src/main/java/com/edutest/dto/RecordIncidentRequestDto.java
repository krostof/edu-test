package com.edutest.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecordIncidentRequestDto {
    private String type;
    private String metadata;
}
