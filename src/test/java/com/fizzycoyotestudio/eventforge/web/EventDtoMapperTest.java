package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.*;
import com.fizzycoyotestudio.eventforge.web.dto.ChoiceDto;
import com.fizzycoyotestudio.eventforge.web.dto.EventDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventDtoMapperTest {

    private final EventDtoMapper mapper = new EventDtoMapper();

    @Test
    void eventRoundTripsThroughDtoUnchanged() {
        Event original = Event.builder()
                .id("zombie-attack")
                .name("Zombie Attack")
                .description("desc")
                .condition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 0))
                .actions(List.of(new ModifyResourceAction("ammo", -2)))
                .choices(List.of(Choice.builder().id("push-back").label("Push Back").nextEventId("loot").build()))
                .nextEventId("zombie-attack-result")
                .cooldownTicks(3)
                .nextEventPool(List.of(new WeightedTransition("loot", 2.0)))
                .build();

        EventDto dto = mapper.toDto(original);
        Event roundTripped = mapper.toDomain(dto);

        assertThat(roundTripped.getId()).isEqualTo(original.getId());
        assertThat(roundTripped.getName()).isEqualTo(original.getName());
        assertThat(roundTripped.getDescription()).isEqualTo(original.getDescription());
        assertThat(roundTripped.getCondition()).isInstanceOf(ComparisonCondition.class);
        assertThat(roundTripped.getActions()).hasSize(1);
        assertThat(roundTripped.getChoices()).hasSize(1);
        assertThat(roundTripped.getNextEventId()).isEqualTo("zombie-attack-result");
        assertThat(roundTripped.getCooldownTicks()).isEqualTo(3);
        assertThat(roundTripped.getNextEventPool()).extracting(WeightedTransition::getEventId).containsExactly("loot");
    }

    @Test
    void choiceRoundTripsThroughDtoUnchanged() {
        Choice original = Choice.builder()
                .id("push-back")
                .label("Push Back")
                .description("Fight back")
                .condition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 0))
                .actions(List.of(new ModifyResourceAction("zombies", -5)))
                .nextEventId("loot")
                .nextEventPool(List.of(new WeightedTransition("loot", 1.0)))
                .build();

        ChoiceDto dto = mapper.toDto(original);
        Choice roundTripped = mapper.toDomain(dto);

        assertThat(roundTripped.getId()).isEqualTo("push-back");
        assertThat(roundTripped.getLabel()).isEqualTo("Push Back");
        assertThat(roundTripped.getCondition()).isInstanceOf(ComparisonCondition.class);
        assertThat(roundTripped.getActions()).hasSize(1);
        assertThat(roundTripped.getNextEventId()).isEqualTo("loot");
        assertThat(roundTripped.getNextEventPool()).hasSize(1);
    }

    @Test
    void eventDtoDefaultsFillMissingCollectionsRatherThanNull() {
        EventDto dto = new EventDto("id", "name", null, null, null, null, null, 0, null);

        assertThat(dto.condition()).isInstanceOf(AlwaysTrueCondition.class);
        assertThat(dto.actions()).isEmpty();
        assertThat(dto.choices()).isEmpty();
        assertThat(dto.nextEventPool()).isEmpty();
    }
}
