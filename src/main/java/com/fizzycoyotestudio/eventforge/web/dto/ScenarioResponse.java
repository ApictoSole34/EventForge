package com.fizzycoyotestudio.eventforge.web.dto;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ScenarioResponse(
        UUID id,
        String name,
        String description,
        String startEventId,
        Map<String, Double> initialState,
        List<EventDto> events
) {
}
