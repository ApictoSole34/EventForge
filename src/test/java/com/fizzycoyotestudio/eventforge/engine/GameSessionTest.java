package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GameSessionTest {

    @Test
    void triggeringAnAutomaticEventAdvancesToItsNextEvent() {
        Event start = Event.builder().id("start").name("start").nextEventId("end").build();
        Event end = Event.builder().id("end").name("end").build();
        EventRegistry registry = new EventRegistry(Map.of("start", start, "end", end));

        GameSession session = new GameSession(new EventEngine(), registry, new GameState(), "start");
        session.triggerCurrentEvent();

        assertThat(session.getCurrentEvent().getId()).isEqualTo("end");
        assertThat(session.getCurrentTick()).isEqualTo(1);
        assertThat(session.isTerminal()).isFalse();
    }

    @Test
    void triggeringATerminalEventEndsTheSession() {
        Event end = Event.builder().id("end").name("end").build();
        EventRegistry registry = new EventRegistry(Map.of("end", end));

        GameSession session = new GameSession(new EventEngine(), registry, new GameState(), "end");
        session.triggerCurrentEvent();

        assertThat(session.isTerminal()).isTrue();
    }

    @Test
    void conditionFailureDoesNotAdvanceOrIncrementTick() {
        Event gated = Event.builder().id("gated").name("gated")
                .condition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 100))
                .nextEventId("end")
                .build();
        Event end = Event.builder().id("end").name("end").build();
        EventRegistry registry = new EventRegistry(Map.of("gated", gated, "end", end));

        GameSession session = new GameSession(new EventEngine(), registry, new GameState(), "gated");
        EventResult result = session.triggerCurrentEvent();

        assertThat(result.isTriggered()).isFalse();
        assertThat(session.getCurrentEvent().getId()).isEqualTo("gated");
        assertThat(session.getCurrentTick()).isZero();
        assertThat(session.isTerminal()).isFalse();
    }

    @Test
    void choosingAnOfferedChoiceAdvancesToItsNextEvent() {
        Event attack = Event.builder().id("attack").name("attack")
                .choices(List.of(
                        Choice.builder().id("push-back").label("Push Back").nextEventId("loot").build(),
                        Choice.builder().id("retreat").label("Retreat").nextEventId("night").build()
                ))
                .build();
        Event loot = Event.builder().id("loot").name("loot").build();
        Event night = Event.builder().id("night").name("night").build();
        EventRegistry registry = new EventRegistry(Map.of("attack", attack, "loot", loot, "night", night));

        GameSession session = new GameSession(new EventEngine(), registry, new GameState(), "attack");
        session.triggerCurrentEvent();

        assertThat(session.getPendingChoices()).extracting(Choice::getId)
                .containsExactlyInAnyOrder("push-back", "retreat");

        session.choose("push-back");

        assertThat(session.getCurrentEvent().getId()).isEqualTo("loot");
        assertThat(session.getPendingChoices()).isEmpty();
    }

    @Test
    void choosingAnUnknownChoiceThrows() {
        Event attack = Event.builder().id("attack").name("attack")
                .choices(List.of(Choice.builder().id("push-back").label("Push Back").build()))
                .build();
        EventRegistry registry = new EventRegistry(Map.of("attack", attack));

        GameSession session = new GameSession(new EventEngine(), registry, new GameState(), "attack");
        session.triggerCurrentEvent();

        assertThatThrownBy(() -> session.choose("does-not-exist"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void triggeringAfterSessionIsTerminalThrows() {
        Event end = Event.builder().id("end").name("end").build();
        EventRegistry registry = new EventRegistry(Map.of("end", end));

        GameSession session = new GameSession(new EventEngine(), registry, new GameState(), "end");
        session.triggerCurrentEvent();

        assertThatThrownBy(session::triggerCurrentEvent)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cooldownExcludesRecentlyFiredCandidateFromWeightedPoolEvenWithOnlyOneOtherCandidate() {
        Event a = Event.builder().id("a").name("a")
                .nextEventPool(List.of(new WeightedTransition("b", 1.0)))
                .build();
        Event b = Event.builder().id("b").name("b")
                .cooldownTicks(2)
                .nextEventId("a")
                .build();
        EventRegistry registry = new EventRegistry(Map.of("a", a, "b", b));

        GameSession session = new GameSession(new EventEngine(), registry, new GameState(), "a");

        session.triggerCurrentEvent();
        assertThat(session.getCurrentEvent().getId()).isEqualTo("b");

        session.triggerCurrentEvent();
        assertThat(session.getCurrentEvent().getId()).isEqualTo("a");

        session.triggerCurrentEvent();
        assertThat(session.isTerminal()).isTrue();
        assertThat(session.getCurrentTick()).isEqualTo(3);
    }

    @Test
    void lastTriggeredTickViewIsUnmodifiable() {
        Event end = Event.builder().id("end").name("end").build();
        EventRegistry registry = new EventRegistry(Map.of("end", end));
        GameSession session = new GameSession(new EventEngine(), registry, new GameState(), "end");

        assertThatThrownBy(() -> session.getLastTriggeredTick().put("x", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
