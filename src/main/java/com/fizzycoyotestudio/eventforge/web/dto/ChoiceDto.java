package com.fizzycoyotestudio.eventforge.web.dto;

import com.fizzycoyotestudio.eventforge.engine.Condition;
import com.fizzycoyotestudio.eventforge.engine.GameAction;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ChoiceDto(
        @NotBlank String id,
        @NotBlank String label,
        String description,
        Condition condition,
        List<GameAction> actions,
        String nextEventId
) {
    public ChoiceDto {
        if (condition == null) condition = Condition.alwaysTrue();
        if (actions == null) actions = List.of();
    }
}
