package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.Choice;
import com.fizzycoyotestudio.eventforge.engine.Event;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventPersistenceMapper {

    private final ConditionActionJsonMapper json;

    public EventPersistenceMapper(ConditionActionJsonMapper json) {
        this.json = json;
    }

    public EventEntity toEntity(Event event, ScenarioEntity scenario) {
        EventEntity entity = new EventEntity();
        entity.setBusinessId(event.getId());
        entity.setName(event.getName());
        entity.setDescription(event.getDescription());
        entity.setScenario(scenario);
        entity.setConditionJson(json.writeCondition(event.getCondition()));
        entity.setActionsJson(json.writeActions(event.getActions()));
        entity.setNextEventBusinessId(event.getNextEventId());
        entity.setCooldownTicks(event.getCooldownTicks());
        entity.setNextEventPoolJson(json.writePool(event.getNextEventPool()));

        List<ChoiceEntity> choiceEntities = event.getChoices().stream()
                .map(choice -> toEntity(choice, entity))
                .toList();
        entity.getChoices().addAll(choiceEntities);

        return entity;
    }

    public ChoiceEntity toEntity(Choice choice, EventEntity event) {
        ChoiceEntity entity = new ChoiceEntity();
        entity.setBusinessId(choice.getId());
        entity.setLabel(choice.getLabel());
        entity.setDescription(choice.getDescription());
        entity.setConditionJson(json.writeCondition(choice.getCondition()));
        entity.setActionsJson(json.writeActions(choice.getActions()));
        entity.setNextEventBusinessId(choice.getNextEventId());
        entity.setNextEventPoolJson(json.writePool(choice.getNextEventPool()));
        entity.setEvent(event);
        return entity;
    }

    public Event toDomain(EventEntity entity) {
        return Event.builder()
                .id(entity.getBusinessId())
                .name(entity.getName())
                .description(entity.getDescription())
                .condition(json.readCondition(entity.getConditionJson()))
                .actions(json.readActions(entity.getActionsJson()))
                .choices(entity.getChoices().stream().map(this::toDomain).toList())
                .nextEventId(entity.getNextEventBusinessId())
                .cooldownTicks(entity.getCooldownTicks())
                .nextEventPool(json.readPool(entity.getNextEventPoolJson()))
                .build();
    }

    public Choice toDomain(ChoiceEntity entity) {
        return Choice.builder()
                .id(entity.getBusinessId())
                .label(entity.getLabel())
                .description(entity.getDescription())
                .condition(json.readCondition(entity.getConditionJson()))
                .actions(json.readActions(entity.getActionsJson()))
                .nextEventId(entity.getNextEventBusinessId())
                .nextEventPool(json.readPool(entity.getNextEventPoolJson()))
                .build();
    }
}
