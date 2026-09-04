package com.fizzycoyotestudio.eventforge.web.dto;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EventFormData {
    private String id;
    private String name;
    private String description;

    private List<ConditionRowForm> conditions = new ArrayList<>();
    private String conditionCombinator = "AND";
    private boolean complexCondition;

    private List<ActionRowForm> actions = new ArrayList<>();
    private List<ChoiceFormData> choices = new ArrayList<>();

    private String nextEventId;

    /** How many ticks must pass after this event fires before it's eligible again as a random-pool candidate elsewhere. */
    private Integer cooldownTicks;

    /** Weighted candidates for automatic random transition; takes precedence over nextEventId when non-empty. */
    private List<PoolEntryForm> nextEventPool = new ArrayList<>();
}
