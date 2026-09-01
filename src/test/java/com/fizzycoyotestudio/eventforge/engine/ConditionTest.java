package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionTest {

    @Test
    void comparisonGreaterThan() {
        GameState state = new GameState();
        state.set("zombies", 12.0);

        Condition condition = new ComparisonCondition("zombies", Operator.GREATER_THAN, 10.0);

        assertThat(condition.evaluate(state)).isTrue();
    }

    @Test
    void comparisonFailsWhenBelowThreshold() {
        GameState state = new GameState();
        state.set("zombies", 5.0);

        Condition condition = new ComparisonCondition("zombies", Operator.GREATER_THAN, 10.0);

        assertThat(condition.evaluate(state)).isFalse();
    }

    @Test
    void andConditionRequiresAllToBeTrue() {
        GameState state = new GameState();
        state.set("food", 42.0);
        state.set("survivors", 6.0);

        Condition condition = new AndCondition(List.of(
                new ComparisonCondition("food", Operator.GREATER_THAN, 20.0),
                new ComparisonCondition("survivors", Operator.GREATER_THAN_OR_EQUAL, 3.0)
        ));

        assertThat(condition.evaluate(state)).isTrue();
    }

    @Test
    void andConditionFailsIfOneIsFalse() {
        GameState state = new GameState();
        state.set("food", 10.0);
        state.set("survivors", 6.0);

        Condition condition = new AndCondition(List.of(
                new ComparisonCondition("food", Operator.GREATER_THAN, 20.0),
                new ComparisonCondition("survivors", Operator.GREATER_THAN_OR_EQUAL, 3.0)
        ));

        assertThat(condition.evaluate(state)).isFalse();
    }

    @Test
    void orConditionTrueIfAtLeastOneMatches() {
        GameState state = new GameState();
        state.set("morale", 5.0);
        state.set("zombies", 0.0);

        Condition condition = new OrCondition(List.of(
                new ComparisonCondition("morale", Operator.LESS_THAN_OR_EQUAL, 15.0),
                new ComparisonCondition("zombies", Operator.GREATER_THAN, 10.0)
        ));

        assertThat(condition.evaluate(state)).isTrue();
    }

    @Test
    void notConditionNegatesResult() {
        GameState state = new GameState();
        state.set("zombies", 0.0);

        Condition condition = new NotCondition(
                new ComparisonCondition("zombies", Operator.GREATER_THAN, 0.0)
        );

        assertThat(condition.evaluate(state)).isTrue();
    }
}
