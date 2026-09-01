package com.fizzycoyotestudio.eventforge.engine;

/**
 * A condition determines whether an event, action, or choice is allowed
 * to happen, based on the current GameState.
 *
 * Implementations should be side-effect free: evaluate() must never
 * mutate the GameState.
 */
public interface Condition {

    boolean evaluate(GameState state);

    /** A condition that is always true — useful as a default/no-op. */
    static Condition alwaysTrue() {
        return state -> true;
    }
}
