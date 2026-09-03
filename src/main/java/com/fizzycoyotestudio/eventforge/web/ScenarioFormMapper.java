package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import com.fizzycoyotestudio.eventforge.web.dto.ScenarioFormData;
import com.fizzycoyotestudio.eventforge.web.dto.StateVariableForm;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ScenarioFormMapper {

    public GameState toInitialState(List<StateVariableForm> rows) {
        Map<String, Double> map = new HashMap<>();
        for (StateVariableForm row : rows) {
            if (StringUtils.hasText(row.getVariable()) && row.getValue() != null) {
                map.put(row.getVariable().trim(), row.getValue());
            }
        }
        return new GameState(map);
    }

    /**
     * Builds the bare start event for a brand-new scenario: no condition
     * (defaults to always-true), no actions, no choices, no next event
     * (terminal). The author fleshes it out afterwards via the event
     * builder/edit screens.
     */
    public Event toStartEvent(ScenarioFormData form) {
        return Event.builder()
                .id(form.getStartEventId())
                .name(form.getStartEventName())
                .description(form.getStartEventDescription())
                .build();
    }
}