package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class EventEngineTest {

    private final EventEngine engine = new EventEngine();

    @Test
    void eventIsBlockedWhenConditionFails() {
        GameState state = new GameState();
        state.set("zombies", 0.0);

        Event zombieAttack = Event.builder()
                .id("zombie-attack")
                .name("Zombie Attack")
                .condition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 0.0))
                .actions(List.of(
                        new ModifyResourceAction("ammo", -2.0),
                        new ModifyResourceAction("morale", -5.0)
                ))
                .nextEventId("zombie-attack-result")
                .build();

        EventResult result = engine.execute(zombieAttack, state);

        assertThat(result.isTriggered()).isFalse();
        assertThat(state.get("ammo")).isEqualTo(0.0);
        assertThat(state.get("morale")).isEqualTo(0.0);
    }

    @Test
    void eventWithoutChoicesAppliesActionsAndResolvesDirectly() {
        GameState state = new GameState();
        state.set("zombies", 12.0);
        state.set("ammo", 13.0);
        state.set("morale", 67.0);

        Event zombieAttack = Event.builder()
                .id("zombie-attack")
                .name("Zombie Attack")
                .condition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 0.0))
                .actions(List.of(
                        new ModifyResourceAction("ammo", -2.0),
                        new ModifyResourceAction("morale", -5.0)
                ))
                .nextEventId("zombie-attack-result")
                .build();

        EventResult result = engine.execute(zombieAttack, state);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.isAwaitingChoice()).isFalse();
        assertThat(result.getNextEventId()).isEqualTo("zombie-attack-result");
        assertThat(state.get("ammo")).isEqualTo(11.0);
        assertThat(state.get("morale")).isEqualTo(62.0);
    }

    @Test
    void eventWithChoicesOffersOnlyAvailableOnes() {
        GameState state = new GameState();
        state.set("survivors", 6.0);

        Choice letHimIn = Choice.builder()
                .id("let-him-in")
                .label("Let him in")
                .actions(List.of(
                        new ModifyResourceAction("survivors", 1.0),
                        new ModifyResourceAction("morale", 5.0)
                ))
                .nextEventId("stranger-joins")
                .build();

        Choice refuse = Choice.builder()
                .id("refuse")
                .label("Refuse")
                .actions(List.of(new ModifyResourceAction("morale", -3.0)))
                .nextEventId("night-falls")
                .build();

        // Only available once survivors dropped very low, to prove filtering works.
        Choice desperateMeasure = Choice.builder()
                .id("desperate")
                .label("Sacrifice someone")
                .condition(new ComparisonCondition("survivors", Operator.LESS_THAN, 2.0))
                .nextEventId("tragedy")
                .build();

        Event strangerAtTheGate = Event.builder()
                .id("stranger-at-the-gate")
                .name("Stranger at the Gate")
                .condition(new ComparisonCondition("survivors", Operator.GREATER_THAN_OR_EQUAL, 3.0))
                .choices(List.of(letHimIn, refuse, desperateMeasure))
                .build();

        EventResult result = engine.execute(strangerAtTheGate, state);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.isAwaitingChoice()).isTrue();
        assertThat(result.getOfferedChoices())
                .extracting(Choice::getId)
                .containsExactly("let-him-in", "refuse");
    }

    @Test
    void resolvingChoiceAppliesItsActionsAndReturnsItsNextEvent() {
        GameState state = new GameState();
        state.set("survivors", 6.0);
        state.set("morale", 67.0);

        Choice letHimIn = Choice.builder()
                .id("let-him-in")
                .label("Let him in")
                .actions(List.of(
                        new ModifyResourceAction("survivors", 1.0),
                        new ModifyResourceAction("morale", 5.0)
                ))
                .nextEventId("stranger-joins")
                .build();

        EventResult result = engine.resolveChoice(letHimIn, state);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getNextEventId()).isEqualTo("stranger-joins");
        assertThat(state.get("survivors")).isEqualTo(7.0);
        assertThat(state.get("morale")).isEqualTo(72.0);
    }

    @Test
    void resolvingAStaleChoiceThrowsWhenConditionNoLongerHolds() {
        GameState state = new GameState();
        state.set("survivors", 1.0); // too low now

        Choice desperateMeasure = Choice.builder()
                .id("desperate")
                .label("Sacrifice someone")
                .condition(new ComparisonCondition("survivors", Operator.LESS_THAN, 2.0))
                .nextEventId("tragedy")
                .build();

        assertThat(desperateMeasure.isAvailable(state)).isTrue();

        state.set("survivors", 10.0);

        assertThatThrownBy(() -> engine.resolveChoice(desperateMeasure, state))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("desperate");
    }
}
