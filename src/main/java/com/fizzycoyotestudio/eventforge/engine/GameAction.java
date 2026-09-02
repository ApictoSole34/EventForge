package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * An action mutates the GameState. Unlike Condition, actions ARE allowed
 * (expected) to have side effects on the state passed in.
 *
 * <p>See {@link Condition} for the rationale behind the JSON type
 * annotations below. New GameAction implementations must be added to
 * {@code @JsonSubTypes} or they will fail to deserialize once Phase 3
 * adds JSON-backed event definitions.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ModifyResourceAction.class, name = "MODIFY_RESOURCE"),
        @JsonSubTypes.Type(value = SetResourceAction.class, name = "SET_RESOURCE")
})
public interface GameAction {

    void execute(GameState state);
}

