package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * A single comparison between a GameState variable and a constant value.
 *
 * Example: new ComparisonCondition("zombies", Operator.GREATER_THAN, 10)
 * represents the rule "zombies > 10".
 */
public final class ComparisonCondition implements Condition {

    private final String variable;
    private final Operator operator;
    private final double value;

    @JsonCreator
    public ComparisonCondition(@JsonProperty("variable") String variable,
                               @JsonProperty("operator") Operator operator,
                               @JsonProperty("value") double value) {
        this.variable = Objects.requireNonNull(variable, "variable must not be null");
        this.operator = Objects.requireNonNull(operator, "operator must not be null");
        this.value = value;
    }

    @Override
    public boolean evaluate(GameState state) {
        double actual = state.get(variable);
        return operator.apply(actual, value);
    }

    public String getVariable() {
        return variable;
    }

    public Operator getOperator() {
        return operator;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return variable + " " + operator + " " + value;
    }
}
