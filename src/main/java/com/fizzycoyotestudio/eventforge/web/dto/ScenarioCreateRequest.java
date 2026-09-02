package com.fizzycoyotestudio.eventforge.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.Map;

public record ScenarioCreateRequest(
        @NotBlank String name,
        String description,
        @NotBlank String startEventId,
        Map<String, Double> initialState,
        @NotEmpty @Valid List<EventDto> events
) {
    public ScenarioCreateRequest {
        if (initialState == null) initialState = Map.of();
    }
}
