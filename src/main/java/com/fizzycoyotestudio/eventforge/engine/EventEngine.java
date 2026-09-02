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
     *
     * <p><b>Design note on ordering:</b> the event's own {@code actions}
     * are applied immediately, <i>before</i> any choices are offered to
     * the player. This is intentional — it models things like a Zombie
     * Attack automatically costing ammo/morale the instant it happens,
     * independent of whatever the player decides to do next (Fight/Run).
     * If a specific event should show its description first and only
     * apply consequences after the player picks a choice, put those
     * consequences on the {@link Choice} itself (via {@code
     * Choice#getActions()}) rather than on the {@code Event}, and leave
     * the event's own {@code actions} empty.
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
     * <p>Callers are expected to only pass choices obtained from a
     * fresh {@link EventResult#getOfferedChoices()} call. As a safety
     * net against stale choices — e.g. a client that cached the choice
     * list and calls this well after the GameState has since changed —
     * the choice's condition is re-checked here. If it no longer holds,
     * this throws rather than silently applying actions that shouldn't
     * be available anymore.
     *
     * @throws IllegalStateException if {@code choice}'s condition no
     *         longer holds against the current {@code state}
     */
    public EventResult resolveChoice(Choice choice, GameState state) {
        Objects.requireNonNull(choice, "choice must not be null");
        Objects.requireNonNull(state, "state must not be null");

        if (!choice.isAvailable(state)) {
            throw new IllegalStateException(
                    "Choice '" + choice.getId() + "' is no longer available: its condition "
                            + "does not hold against the current GameState. The caller likely "
                            + "resolved a stale choice obtained before the state changed.");
        }

        choice.applyActions(state);
        return EventResult.resolved(state, choice.getNextEventId());
    }
}