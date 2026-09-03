package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.Choice;
import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.web.dto.ChoiceDto;
import com.fizzycoyotestudio.eventforge.web.dto.EventDto;
import org.springframework.stereotype.Component;

@Component
public class EventDtoMapper {

    public Event toDomain(EventDto dto) {
        return Event.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description())
                .condition(dto.condition())
                .actions(dto.actions())
                .choices(dto.choices().stream().map(this::toDomain).toList())
                .nextEventId(dto.nextEventId())
                .cooldownTicks(dto.cooldownTicks())
                .nextEventPool(dto.nextEventPool())
                .build();
    }

    public Choice toDomain(ChoiceDto dto) {
        return Choice.builder()
                .id(dto.id())
                .label(dto.label())
                .description(dto.description())
                .condition(dto.condition())
                .actions(dto.actions())
                .nextEventId(dto.nextEventId())
                .nextEventPool(dto.nextEventPool())
                .build();
    }

    public EventDto toDto(Event event) {
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getCondition(),
                event.getActions(),
                event.getChoices().stream().map(this::toDto).toList(),
                event.getNextEventId(),
                event.getCooldownTicks(),
                event.getNextEventPool()
        );
    }

    public ChoiceDto toDto(Choice choice) {
        return new ChoiceDto(
                choice.getId(),
                choice.getLabel(),
                choice.getDescription(),
                choice.getCondition(),
                choice.getActions(),
                choice.getNextEventId(),
                choice.getNextEventPool()
        );
    }
}
