package com.fizzycoyotestudio.eventforge.web.dto;

import java.util.List;
import java.util.UUID;

public record ScenarioResponse(
        UUID id,
        String name,
        String description,
        String startEventId,
        List<EventDto> events
) {
}
