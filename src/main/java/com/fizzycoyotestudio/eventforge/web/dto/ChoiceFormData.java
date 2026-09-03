package com.fizzycoyotestudio.eventforge.web.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChoiceFormData {
    private String id;
    private String label;
    private String description;

    private String conditionVariable;
    private String conditionOperator;
    private Double conditionValue;

    private List<ActionRowForm> actions = new ArrayList<>();
    private String nextEventId;

    /** Weighted candidates for random transition; takes precedence over nextEventId when non-empty. */
    private List<PoolEntryForm> nextEventPool = new ArrayList<>();
}
