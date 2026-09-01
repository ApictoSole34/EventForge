package com.fizzycoyotestudio.eventforge.engine;

import java.util.Objects;

/** Composite condition: negates a single child condition. */
public final class NotCondition implements Condition {

    private final Condition condition;

    public NotCondition(Condition condition) {
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
