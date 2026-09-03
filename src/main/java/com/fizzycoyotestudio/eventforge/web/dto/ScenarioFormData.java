package com.fizzycoyotestudio.eventforge.web.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ScenarioFormData {
    private String name;
    private String description;

    private List<StateVariableForm> initialState = new ArrayList<>();

    /**
     * Every scenario needs at least one event to begin with. We collect
     * just the bare essentials here (id/name/description) — condition,
     * actions and choices for this event (and any further events) are
     * added afterwards via the existing EventBuilderController flow.
     */
    private String startEventId;
    private String startEventName;
    private String startEventDescription;
}