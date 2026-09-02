package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * Sets a GameState variable to an exact value, overwriting whatever it
 * was before. Useful for things like "morale = 0" rather than relative
 * changes.
 */
public final class SetResourceAction implements GameAction {

    private final String variable;
    private final double value;

    @JsonCreator
    public SetResourceAction(@JsonProperty("variable") String variable,
                             @JsonProperty("value") double value) {
        this.variable = Objects.requireNonNull(variable, "variable must not be null");
        this.value = value;
    }

    @Override
    public void execute(GameState state) {
        state.set(variable, value);
    }

    public String getVariable() {
        return variable;
    }

    public double getValue() {
        return value;
    }
}
