package com.fizzycoyotestudio.eventforge.game;

import com.fizzycoyotestudio.eventforge.engine.*;
import com.fizzycoyotestudio.eventforge.game.zombieshelter.ZombieShelterScenario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end test proving the engine + GameSession + Zombie Shelter
 * event definitions work together for a real playthrough, without any
 * console input or Spring context needed.
 */
class ZombieShelterScenarioTest {

    private final EventEngine engine = new EventEngine();

    @Test
    void zombieAttackChain_pushBack_leadsToLootAndDaySummary() {
        GameState state = ZombieShelterScenario.initialState();
        EventRegistry registry = ZombieShelterScenario.buildRegistry();
        GameSession session = new GameSession(engine, registry, state, "zombie-attack");

        EventResult attackResult = session.triggerCurrentEvent();
        assertThat(attackResult.isTriggered()).isTrue();
        assertThat(state.get("ammo")).isEqualTo(11);
        assertThat(state.get("morale")).isEqualTo(62);
        assertThat(session.getCurrentEvent().getId()).isEqualTo("zombie-attack-result");

        EventResult resultEvent = session.triggerCurrentEvent();
        assertThat(resultEvent.isAwaitingChoice()).isTrue();
        assertThat(resultEvent.getOfferedChoices())
                .extracting(Choice::getId)
                .containsExactly("push-back", "retreat");

        session.choose("push-back");
        assertThat(state.get("zombies")).isEqualTo(7);
        assertThat(session.getCurrentEvent().getId()).isEqualTo("loot");

        EventResult lootResult = session.triggerCurrentEvent();
        assertThat(lootResult.isTriggered()).isTrue();
        assertThat(state.get("food")).isEqualTo(52);
        assertThat(state.get("ammo")).isEqualTo(14);
        assertThat(session.getCurrentEvent().getId()).isEqualTo("day-summary");

        EventResult summaryResult = session.triggerCurrentEvent();
        assertThat(summaryResult.isTriggered()).isTrue();
        assertThat(summaryResult.hasNextEvent()).isFalse();
        assertThat(state.get("day")).isEqualTo(18);
    }

    @Test
    void strangerAtTheGate_letHimIn_increasesSurvivorsAndMorale() {
        GameState state = ZombieShelterScenario.initialState();
        EventRegistry registry = ZombieShelterScenario.buildRegistry();
        GameSession session = new GameSession(engine, registry, state, "stranger-at-the-gate");

        EventResult gateResult = session.triggerCurrentEvent();
        assertThat(gateResult.isAwaitingChoice()).isTrue();
        assertThat(gateResult.getOfferedChoices())
                .extracting(Choice::getId)
                .containsExactly("let-him-in", "refuse", "search-him");

        session.choose("let-him-in");
        assertThat(state.get("survivors")).isEqualTo(7);
        assertThat(state.get("morale")).isEqualTo(72);
        assertThat(session.getCurrentEvent().getId()).isEqualTo("stranger-joins");

        EventResult joinsResult = session.triggerCurrentEvent();
        assertThat(joinsResult.isTriggered()).isTrue();
        assertThat(session.getCurrentEvent().getId()).isEqualTo("day-summary");
    }

    @Test
    void strangerAtTheGate_isNotOfferedWhenTooFewSurvivors() {
        GameState state = ZombieShelterScenario.initialState();
        state.set("survivors", 2);
        EventRegistry registry = ZombieShelterScenario.buildRegistry();
        GameSession session = new GameSession(engine, registry, state, "stranger-at-the-gate");

        EventResult result = session.triggerCurrentEvent();

        assertThat(result.isTriggered()).isFalse();
    }
}
