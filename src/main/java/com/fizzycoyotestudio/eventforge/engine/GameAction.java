package com.fizzycoyotestudio.eventforge.engine;

/**
 * An action mutates the GameState. Unlike Condition, actions ARE allowed
 * (expected) to have side effects on the state passed in.
 */
public interface GameAction {

    void execute(GameState state);
}
