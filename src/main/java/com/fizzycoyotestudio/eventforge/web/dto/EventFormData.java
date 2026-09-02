package com.fizzycoyotestudio.eventforge.web.dto;


import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EventFormData {
    private String id;
    private String name;
    private String description;

    private String conditionVariable;
    private String conditionOperator;
    private Double conditionValue;

    private List<ActionRowForm> actions = new ArrayList<>();
    private List<ChoiceFormData> choices = new ArrayList<>();

    private String nextEventId;
}
