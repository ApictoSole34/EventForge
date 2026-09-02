package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/** Composite condition: negates a single child condition. */
public final class NotCondition implements Condition {

    private final Condition condition;

    @JsonCreator
    public NotCondition(@JsonProperty("condition") Condition condition) {
        this.condition = Objects.requireNonNull(condition);
    }

    @Override
    public boolean evaluate(GameState state) {
        return !condition.evaluate(state);
    }

    public Condition getCondition() {
        return condition;
    }
}
