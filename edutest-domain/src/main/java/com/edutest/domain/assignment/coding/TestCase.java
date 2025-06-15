package com.edutest.domain.assignment.coding;


import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase{

    private CodingAssignment assignment;

    private String inputData;

    private String expectedOutput;

    @Builder.Default
    private Boolean isPublic = false;

    private String description;

    @Builder.Default
    private Integer weight = 1;

    public boolean matches(String actualOutput) {
        if (expectedOutput == null && actualOutput == null) {
            return true;
        }
        if (expectedOutput == null || actualOutput == null) {
            return false;
        }

        String normalizedExpected = expectedOutput.trim();
        String normalizedActual = actualOutput.trim();

        return normalizedExpected.equals(normalizedActual);
    }
}
