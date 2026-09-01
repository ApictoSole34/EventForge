package com.fizzycoyotestudio.eventforge.engine;

import java.util.List;
import java.util.Objects;

/**
 * Composite condition: true only if ALL child conditions are true.
 * Empty list of conditions evaluates to true (vacuous truth), matching
 * standard AND semantics.
 */
public final class AndCondition implements Condition {

    private final List<Condition> conditions;

    public AndCondition(List<Condition> conditions) {
        this.conditions = List.copyOf(Objects.requireNonNull(conditions));
    }

    @Override
    public boolean evaluate(GameState state) {
        return conditions.stream().allMatch(c -> c.evaluate(state));
    }

    public List<Condition> getConditions() {
        return conditions;
    }
}