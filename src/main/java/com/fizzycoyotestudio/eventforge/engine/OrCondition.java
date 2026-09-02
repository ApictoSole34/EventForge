package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Objects;

/**
 * Composite condition: true if AT LEAST ONE child condition is true.
 * Empty list of conditions evaluates to false, matching standard OR
 * semantics.
 */
public final class OrCondition implements Condition {

    private final List<Condition> conditions;

    @JsonCreator
    public OrCondition(@JsonProperty("conditions") List<Condition> conditions) {
        this.conditions = List.copyOf(Objects.requireNonNull(conditions));
    }

    @Override
    public boolean evaluate(GameState state) {
        return conditions.stream().anyMatch(c -> c.evaluate(state));
    }

    public List<Condition> getConditions() {
        return conditions;
    }
}
