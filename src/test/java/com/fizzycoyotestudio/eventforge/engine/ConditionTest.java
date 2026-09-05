package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConditionTest {

    private GameState stateWith(double zombies) {
        GameState state = new GameState();
        state.set("zombies", zombies);
        return state;
    }

    @ParameterizedTest
    @CsvSource({
            "GREATER_THAN, 5, 10, true",
            "GREATER_THAN, 5, 5, false",
            "GREATER_THAN, 5, 3, false",
            "GREATER_THAN_OR_EQUAL, 5, 5, true",
            "GREATER_THAN_OR_EQUAL, 5, 4, false",
            "LESS_THAN, 5, 3, true",
            "LESS_THAN, 5, 5, false",
            "LESS_THAN_OR_EQUAL, 5, 5, true",
            "EQUAL, 5, 5, true",
            "EQUAL, 5, 6, false",
            "NOT_EQUAL, 5, 6, true",
            "NOT_EQUAL, 5, 5, false",
    })
    void comparisonConditionEvaluatesEachOperator(Operator operator, double threshold, double actual, boolean expected) {
        ComparisonCondition condition = new ComparisonCondition("zombies", operator, threshold);
        assertThat(condition.evaluate(stateWith(actual))).isEqualTo(expected);
    }

    @Test
    void comparisonConditionTreatsMissingVariableAsZero() {
        ComparisonCondition condition = new ComparisonCondition("ghost-var", Operator.EQUAL, 0);
        assertThat(condition.evaluate(new GameState())).isTrue();
    }

    @Test
    void alwaysTrueConditionIsAlwaysTrue() {
        assertThat(Condition.alwaysTrue().evaluate(new GameState())).isTrue();
        assertThat(new AlwaysTrueCondition().evaluate(stateWith(-1))).isTrue();
    }

    @Test
    void notConditionNegatesChild() {
        Condition child = new ComparisonCondition("zombies", Operator.GREATER_THAN, 10);
        NotCondition notCondition = new NotCondition(child);

        assertThat(notCondition.evaluate(stateWith(20))).isFalse();
        assertThat(notCondition.evaluate(stateWith(5))).isTrue();
    }

    @Test
    void andConditionRequiresAllChildrenTrue() {
        Condition zombiesHigh = new ComparisonCondition("zombies", Operator.GREATER_THAN, 10);
        Condition zombiesLow = new ComparisonCondition("zombies", Operator.LESS_THAN, 5);
        AndCondition and = new AndCondition(List.of(zombiesHigh, zombiesLow));

        assertThat(and.evaluate(stateWith(20))).isFalse();
    }

    @Test
    void andConditionTrueWhenAllChildrenTrue() {
        Condition c1 = new ComparisonCondition("zombies", Operator.GREATER_THAN, 10);
        Condition c2 = new ComparisonCondition("zombies", Operator.LESS_THAN, 30);
        AndCondition and = new AndCondition(List.of(c1, c2));

        assertThat(and.evaluate(stateWith(20))).isTrue();
    }

    @Test
    void andConditionOfEmptyListIsVacuouslyTrue() {
        AndCondition and = new AndCondition(List.of());
        assertThat(and.evaluate(new GameState())).isTrue();
    }

    @Test
    void orConditionTrueIfAnyChildTrue() {
        Condition zombiesHigh = new ComparisonCondition("zombies", Operator.GREATER_THAN, 100);
        Condition zombiesLow = new ComparisonCondition("zombies", Operator.LESS_THAN, 5);
        OrCondition or = new OrCondition(List.of(zombiesHigh, zombiesLow));

        assertThat(or.evaluate(stateWith(2))).isTrue();
        assertThat(or.evaluate(stateWith(50))).isFalse();
    }

    @Test
    void orConditionOfEmptyListIsFalse() {
        OrCondition or = new OrCondition(List.of());
        assertThat(or.evaluate(new GameState())).isFalse();
    }

    @Test
    void conditionsCanBeNestedArbitrarily() {
        Condition inRange = new AndCondition(List.of(
                new ComparisonCondition("zombies", Operator.GREATER_THAN, 10),
                new ComparisonCondition("zombies", Operator.LESS_THAN, 30)
        ));
        Condition none = new NotCondition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 0));
        Condition nested = new OrCondition(List.of(inRange, none));

        assertThat(nested.evaluate(stateWith(20))).isTrue();
        assertThat(nested.evaluate(stateWith(0))).isTrue();
        assertThat(nested.evaluate(stateWith(50))).isFalse();
    }
}
