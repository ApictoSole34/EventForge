package com.fizzycoyotestudio.eventforge.engine;

import java.util.Objects;

/**
 * Central service responsible for executing events against a GameState.
 *
 * Flow:
 *   1. execute(event, state)      -> checks condition, applies the event's
 *                                     own actions, then either offers
 *                                     Choices to the player or resolves
 *                                     straight to a next event.
 *   2. resolveChoice(choice, state) -> applies the chosen Choice's actions
 *                                     and resolves to its next event.
 *
 * The engine is intentionally game-agnostic: it knows nothing about
 * zombies, shelters, or any specific domain. It only knows how to
 * evaluate Conditions and execute GameActions against a generic
 * GameState.
 */
public class EventEngine {

    /**
     * Attempts to trigger an event. If the event's condition fails, the
     * state is returned unchanged and the result is marked as not
     * triggered. Otherwise the event's own actions are applied (in
     * declaration order, since later actions may depend on earlier ones),
     * and the engine either offers the event's choices or resolves
     * directly to its next event.
     */
    public EventResult execute(Event event, GameState state) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(state, "state must not be null");

        if (!event.canTrigger(state)) {
            return EventResult.blocked(state);
        }

        event.getActions().forEach(action -> action.execute(state));

        if (event.hasChoices()) {
            return EventResult.awaitingChoice(state, event.availableChoices(state));
        }

        return EventResult.resolved(state, event.getNextEventId());
    }

    /**
     * Resolves a player's Choice within an already-triggered event:
     * applies the choice's actions and returns the next event id.
     *
     * Does not re-check the choice's own condition — callers are
     * expected to only pass choices obtained from
     * EventResult#getOfferedChoices(), which are already filtered.
     */
    public EventResult resolveChoice(Choice choice, GameState state) {
        Objects.requireNonNull(choice, "choice must not be null");
        Objects.requireNonNull(state, "state must not be null");

        choice.applyActions(state);
        return EventResult.resolved(state, choice.getNextEventId());
    }
}
