package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A condition determines whether an event, action, or choice is allowed
 * to happen, based on the current GameState.
 *
 * Implementations should be side-effect free: evaluate() must never
 * mutate the GameState.
 *
 * <p>The {@code @JsonTypeInfo}/{@code @JsonSubTypes} annotations below
 * define the polymorphic JSON shape ahead of time (Phase 3 will add the
 * actual persistence/parsing code that reads events from JSON/DB). A
 * serialized condition looks like:
 * <pre>{@code {"type": "COMPARISON", "variable": "zombies", "operator": "GREATER_THAN", "value": 10} }</pre>
 * New Condition implementations must be added to the {@code @JsonSubTypes}
 * list below or they will fail to deserialize.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ComparisonCondition.class, name = "COMPARISON"),
        @JsonSubTypes.Type(value = AndCondition.class, name = "AND"),
        @JsonSubTypes.Type(value = OrCondition.class, name = "OR"),
        @JsonSubTypes.Type(value = NotCondition.class, name = "NOT"),
        @JsonSubTypes.Type(value = AlwaysTrueCondition.class, name = "ALWAYS_TRUE")
})
public interface Condition {

    boolean evaluate(GameState state);

    /** A condition that is always true — useful as a default/no-op. */
    static Condition alwaysTrue() {
        return new AlwaysTrueCondition();
    }
}
