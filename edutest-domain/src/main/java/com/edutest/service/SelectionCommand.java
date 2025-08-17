package com.edutest.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelectionCommand {
    private Long answerId;
    private List<Long> optionIdsToSelect;
    private List<Long> optionIdsToDeselect;
    private boolean clearAll;
}
