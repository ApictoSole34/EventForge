package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightedTransitionTest {

    @Test
    void rejectsZeroWeight() {
        assertThatThrownBy(() -> new WeightedTransition("loot", 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("weight must be > 0");
    }

    @Test
    void rejectsNegativeWeight() {
        assertThatThrownBy(() -> new WeightedTransition("loot", -2.0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullEventId() {
        assertThatThrownBy(() -> new WeightedTransition(null, 1.0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void acceptsPositiveWeight() {
        WeightedTransition transition = new WeightedTransition("loot", 2.5);
        assertThat(transition.getEventId()).isEqualTo("loot");
        assertThat(transition.getWeight()).isEqualTo(2.5);
    }

    @Test
    void selectionOnlyEverPicksFromTheGivenPool() {
        Event event = Event.builder()
                .id("e")
                .name("e")
                .nextEventPool(java.util.List.of(
                        new WeightedTransition("a", 1.0),
                        new WeightedTransition("b", 1.0)
                ))
                .build();

        java.util.Random random = new java.util.Random(42);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; i < 200; i++) {
            seen.add(event.resolveNextEventId(random, id -> true));
        }

        assertThat(seen).containsExactlyInAnyOrder("a", "b");
    }

    @Test
    void higherWeightIsPickedMoreOftenOverManyTrials() {
        Event event = Event.builder()
                .id("e")
                .name("e")
                .nextEventPool(java.util.List.of(
                        new WeightedTransition("common", 9.0),
                        new WeightedTransition("rare", 1.0)
                ))
                .build();

        java.util.Random random = new java.util.Random(1234);
        int commonCount = 0;
        int trials = 2000;
        for (int i = 0; i < trials; i++) {
            if ("common".equals(event.resolveNextEventId(random, id -> true))) {
                commonCount++;
            }
        }

        assertThat(commonCount).isGreaterThan((int) (trials * 0.75));
    }
}
