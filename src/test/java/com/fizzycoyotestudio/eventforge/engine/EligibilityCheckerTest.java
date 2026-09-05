package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EligibilityCheckerTest {

    private EventRegistry registryWith(Event... events) {
        Map<String, Event> byId = new java.util.HashMap<>();
        for (Event e : events) byId.put(e.getId(), e);
        return new EventRegistry(byId);
    }

    @Test
    void candidateNotInRegistryIsNotEligible() {
        EventRegistry registry = registryWith();
        boolean eligible = EligibilityChecker.isEligible(registry, new GameState(), Map.of(), 5, "ghost");
        assertThat(eligible).isFalse();
    }

    @Test
    void candidateWhoseOwnConditionFailsIsNotEligible() {
        Event candidate = Event.builder().id("loot")
                .condition(new ComparisonCondition("zombies", Operator.EQUAL, 0))
                .build();
        EventRegistry registry = registryWith(candidate);

        GameState state = new GameState();
        state.set("zombies", 5);

        assertThat(EligibilityChecker.isEligible(registry, state, Map.of(), 5, "loot")).isFalse();
    }

    @Test
    void candidateWithNoCooldownAndNeverFiredIsEligible() {
        Event candidate = Event.builder().id("loot").cooldownTicks(0).build();
        EventRegistry registry = registryWith(candidate);

        assertThat(EligibilityChecker.isEligible(registry, new GameState(), Map.of(), 5, "loot")).isTrue();
    }

    @Test
    void candidateNeverTriggeredBeforeIsEligibleEvenWithCooldownConfigured() {
        Event candidate = Event.builder().id("loot").cooldownTicks(3).build();
        EventRegistry registry = registryWith(candidate);

        assertThat(EligibilityChecker.isEligible(registry, new GameState(), Map.of(), 5, "loot")).isTrue();
    }

    @Test
    void candidateStillWithinCooldownWindowIsNotEligible() {
        Event candidate = Event.builder().id("loot").cooldownTicks(3).build();
        EventRegistry registry = registryWith(candidate);

        Map<String, Integer> lastFired = Map.of("loot", 4);

        assertThat(EligibilityChecker.isEligible(registry, new GameState(), lastFired, 6, "loot")).isFalse();
    }

    @Test
    void candidateExactlyAtCooldownBoundaryIsEligible() {
        Event candidate = Event.builder().id("loot").cooldownTicks(3).build();
        EventRegistry registry = registryWith(candidate);

        Map<String, Integer> lastFired = Map.of("loot", 3);

        assertThat(EligibilityChecker.isEligible(registry, new GameState(), lastFired, 6, "loot")).isTrue();
    }

    @Test
    void candidatePastCooldownWindowIsEligible() {
        Event candidate = Event.builder().id("loot").cooldownTicks(3).build();
        EventRegistry registry = registryWith(candidate);

        Map<String, Integer> lastFired = Map.of("loot", 1);

        assertThat(EligibilityChecker.isEligible(registry, new GameState(), lastFired, 10, "loot")).isTrue();
    }
}
