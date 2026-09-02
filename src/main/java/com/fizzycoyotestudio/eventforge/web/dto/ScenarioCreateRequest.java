package com.fizzycoyotestudio.eventforge.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ScenarioCreateRequest(
        @NotBlank String name,
        String description,
        @NotBlank String startEventId,
        @NotEmpty @Valid List<EventDto> events
) {
}
