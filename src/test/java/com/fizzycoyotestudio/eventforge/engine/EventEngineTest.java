package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EventEngineTest {

    private final EventEngine engine = new EventEngine(new Random(99));

    @Test
    void blockedWhenConditionFails() {
        Event event = Event.builder().id("e").name("e")
                .condition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 0))
                .actions(List.of(new ModifyResourceAction("ammo", -100)))
                .build();

        GameState state = new GameState();
        state.set("zombies", 0);
        state.set("ammo", 10);

        EventResult result = engine.execute(event, state);

        assertThat(result.isTriggered()).isFalse();
        assertThat(state.get("ammo")).isEqualTo(10);
    }

    @Test
    void ownActionsAppliedBeforeChoicesAreOffered() {
        Event event = Event.builder().id("zombie-attack").name("Zombie Attack")
                .actions(List.of(new ModifyResourceAction("ammo", -2)))
                .choices(List.of(Choice.builder().id("push-back").label("Push Back").build()))
                .build();

        GameState state = new GameState();
        state.set("ammo", 10);

        EventResult result = engine.execute(event, state);

        assertThat(state.get("ammo")).isEqualTo(8);
        assertThat(result.isAwaitingChoice()).isTrue();
        assertThat(result.getOfferedChoices()).extracting(Choice::getId).containsExactly("push-back");
    }

    @Test
    void resolvesDirectlyWhenThereAreNoChoices() {
        Event event = Event.builder().id("loot").name("Loot").nextEventId("day-summary").build();

        EventResult result = engine.execute(event, new GameState());

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.isAwaitingChoice()).isFalse();
        assertThat(result.hasNextEvent()).isTrue();
        assertThat(result.getNextEventId()).isEqualTo("day-summary");
    }

    @Test
    void resolvesAutomaticallyWhenNoChoiceIsCurrentlyAvailable() {
        Choice gated = Choice.builder().id("only-choice").label("Only")
                .condition(new ComparisonCondition("survivors", Operator.GREATER_THAN, 100))
                .build();
        Event event = Event.builder().id("e").name("e")
                .choices(List.of(gated))
                .nextEventId("day-summary")
                .build();

        EventResult result = engine.execute(event, new GameState());

        assertThat(result.isAwaitingChoice()).isFalse();
        assertThat(result.getNextEventId()).isEqualTo("day-summary");
    }

    @Test
    void resolvesViaWeightedPoolWhenEligibleCandidateExists() {
        Event event = Event.builder().id("e").name("e")
                .nextEventPool(List.of(new WeightedTransition("loot", 1.0)))
                .nextEventId("day-summary")
                .build();

        EventResult result = engine.execute(event, new GameState(), id -> true);

        assertThat(result.getNextEventId()).isEqualTo("loot");
    }

    @Test
    void resolveChoiceAppliesActionsAndReturnsNextEvent() {
        Choice choice = Choice.builder().id("push-back").label("Push Back")
                .actions(List.of(new ModifyResourceAction("zombies", -5)))
                .nextEventId("loot")
                .build();

        GameState state = new GameState();
        state.set("zombies", 12);

        EventResult result = engine.resolveChoice(choice, state);

        assertThat(state.get("zombies")).isEqualTo(7);
        assertThat(result.getNextEventId()).isEqualTo("loot");
    }

    @Test
    void resolveChoiceThrowsWhenChoiceIsStaleAgainstCurrentState() {
        Choice choice = Choice.builder().id("retreat").label("Retreat")
                .condition(new ComparisonCondition("morale", Operator.GREATER_THAN, 50))
                .build();

        GameState state = new GameState();
        state.set("morale", 10);

        assertThatThrownBy(() -> engine.resolveChoice(choice, state))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("retreat");
    }

    @Test
    void blockedResultCarriesUnchangedStateReference() {
        Event event = Event.builder().id("e").name("e")
                .condition(new ComparisonCondition("x", Operator.EQUAL, 1))
                .build();
        GameState state = new GameState();

        EventResult result = engine.execute(event, state);

        assertThat(result.getResultingState()).isSameAs(state);
        assertThat(result.hasNextEvent()).isFalse();
    }
}
