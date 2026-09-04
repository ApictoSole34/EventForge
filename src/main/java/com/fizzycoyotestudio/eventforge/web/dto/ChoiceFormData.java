package com.fizzycoyotestudio.eventforge.web.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChoiceFormData {
    private String id;
    private String label;
    private String description;

    private List<ConditionRowForm> conditions = new ArrayList<>();
    private String conditionCombinator = "AND";
    private boolean complexCondition;

    private List<ActionRowForm> actions = new ArrayList<>();
    private String nextEventId;

    /** Weighted candidates for random transition; takes precedence over nextEventId when non-empty. */
    private List<PoolEntryForm> nextEventPool = new ArrayList<>();
}
