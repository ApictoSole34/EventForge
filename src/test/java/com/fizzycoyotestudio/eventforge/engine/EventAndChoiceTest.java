package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

class EventAndChoiceTest {

    private final Random random = new Random(7);

    @Test
    void eventWithNoPoolFallsBackToNextEventId() {
        Event event = Event.builder().id("e").name("e").nextEventId("day-summary").build();

        assertThat(event.resolveNextEventId(random, id -> true)).isEqualTo("day-summary");
    }

    @Test
    void eventWithEmptyPoolFallsBackToNextEventId() {
        Event event = Event.builder().id("e").name("e")
                .nextEventPool(List.of())
                .nextEventId("day-summary")
                .build();

        assertThat(event.resolveNextEventId(random, id -> true)).isEqualTo("day-summary");
    }

    @Test
    void eventWithPoolButNoEligibleCandidatesFallsBackToNextEventId() {
        Event event = Event.builder().id("e").name("e")
                .nextEventPool(List.of(new WeightedTransition("loot", 1.0)))
                .nextEventId("day-summary")
                .build();

        assertThat(event.resolveNextEventId(random, id -> false)).isEqualTo("day-summary");
    }

    @Test
    void eventWithEligiblePoolCandidatePreferredOverNextEventId() {
        Event event = Event.builder().id("e").name("e")
                .nextEventPool(List.of(new WeightedTransition("loot", 1.0)))
                .nextEventId("day-summary")
                .build();

        assertThat(event.resolveNextEventId(random, id -> true)).isEqualTo("loot");
    }

    @Test
    void eventWithPoolAndNoFallbackReturnsNullWhenNoCandidateEligible() {
        Event event = Event.builder().id("e").name("e")
                .nextEventPool(List.of(new WeightedTransition("loot", 1.0)))
                .build();

        assertThat(event.resolveNextEventId(random, id -> false)).isNull();
    }

    @Test
    void hasChoicesReflectsChoiceList() {
        Event withChoices = Event.builder().id("e").name("e")
                .choices(List.of(Choice.builder().id("c").label("c").build()))
                .build();
        Event withoutChoices = Event.builder().id("e2").name("e2").build();

        assertThat(withChoices.hasChoices()).isTrue();
        assertThat(withoutChoices.hasChoices()).isFalse();
    }

    @Test
    void availableChoicesFiltersOutChoicesWhoseConditionFails() {
        Choice always = Choice.builder().id("push-back").label("Push Back").build();
        Choice gated = Choice.builder().id("retreat").label("Retreat")
                .condition(new ComparisonCondition("morale", Operator.GREATER_THAN, 50))
                .build();

        Event event = Event.builder().id("e").name("e").choices(List.of(always, gated)).build();

        GameState lowMorale = new GameState();
        lowMorale.set("morale", 10);

        assertThat(event.availableChoices(lowMorale)).containsExactly(always);
    }

    @Test
    void choiceResolveNextEventIdSharesFallbackSemanticsWithEvent() {
        Choice choice = Choice.builder().id("c").label("c")
                .nextEventPool(List.of(new WeightedTransition("loot", 1.0)))
                .nextEventId("day-summary")
                .build();

        assertThat(choice.resolveNextEventId(random, id -> false)).isEqualTo("day-summary");
        assertThat(choice.resolveNextEventId(random, id -> true)).isEqualTo("loot");
    }

    @Test
    void choiceIsAvailableDelegatesToCondition() {
        Choice choice = Choice.builder().id("c").label("c")
                .condition(new ComparisonCondition("survivors", Operator.GREATER_THAN_OR_EQUAL, 3))
                .build();

        GameState state = new GameState();
        state.set("survivors", 2);
        assertThat(choice.isAvailable(state)).isFalse();

        state.set("survivors", 3);
        assertThat(choice.isAvailable(state)).isTrue();
    }

    @Test
    void choiceApplyActionsExecutesInOrder() {
        Choice choice = Choice.builder().id("c").label("c")
                .actions(List.of(
                        new SetResourceAction("morale", 0),
                        new ModifyResourceAction("morale", 10)
                ))
                .build();

        GameState state = new GameState();
        state.set("morale", 999);
        choice.applyActions(state);

        assertThat(state.get("morale")).isEqualTo(10.0);
    }
}
