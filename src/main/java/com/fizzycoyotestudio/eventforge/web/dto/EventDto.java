package com.fizzycoyotestudio.eventforge.web.dto;


import com.fizzycoyotestudio.eventforge.engine.Condition;
import com.fizzycoyotestudio.eventforge.engine.GameAction;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Reuses the domain Condition/GameAction types directly (they already
 * carry their own @JsonTypeInfo polymorphism) rather than re-declaring
 * mirror DTO hierarchies for them. What we DON'T expose over REST is the
 * JPA entities — that boundary is what actually matters here.
 */
public record EventDto(
        @NotBlank String id,
        @NotBlank String name,
        String description,
        Condition condition,
        List<GameAction> actions,
        @Valid List<ChoiceDto> choices,
        String nextEventId
) {
    public EventDto {
        if (condition == null) condition = Condition.alwaysTrue();
        if (actions == null) actions = List.of();
        if (choices == null) choices = List.of();
    }
}
