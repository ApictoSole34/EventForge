package com.fizzycoyotestudio.eventforge.engine;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.function.Predicate;

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
 *
 * <p><b>Weighted random pools / cooldowns:</b> the engine itself has no
 * concept of an EventRegistry, a tick counter, or "which events are on
 * cooldown" — that bookkeeping lives with whoever drives a session
 * ({@code GameSession} in-memory, {@code GameSessionPersistenceService}
 * for the DB-backed REST flow), since only they have both the registry
 * and the per-session cooldown state. What the engine DOES own is
 * {@code Random}, so that selection is deterministic/seedable for
 * tests. Callers pass in an {@code eligibleForRandomPool} predicate
 * (candidate event id -> is it currently pickable) which the engine
 * applies when an Event/Choice has a non-empty nextEventPool. The
 * simpler two-argument overloads keep old call sites (e.g. the demo
 * runner, unit tests) working unchanged — they treat every candidate as
 * always-eligible, which is a no-op unless you've actually configured a
 * nextEventPool.
 */
public class EventEngine {

    private static final Predicate<String> ALWAYS_ELIGIBLE = id -> true;

    private final Random random;

    public EventEngine() {
        this(new Random());
    }

    /** Seeded constructor — mainly for deterministic tests of weighted-pool selection. */
    public EventEngine(Random random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    /** Convenience overload: no weighted pools in play, or caller doesn't care about eligibility filtering. */
    public EventResult execute(Event event, GameState state) {
        return execute(event, state, ALWAYS_ELIGIBLE);
    }

    /**
     * Attempts to trigger an event. If the event's condition fails, the
     * state is returned unchanged and the result is marked as not
     * triggered. Otherwise, the event's own actions are applied (in
     * declaration order, since later actions may depend on earlier ones),
     * and the engine either offers the event's choices or resolves
     * directly to its next event (via {@link Event#resolveNextEventId},
     * which picks from {@code nextEventPool} when one is configured,
     * restricted to candidates {@code eligibleForRandomPool} accepts).
     *
     * <p><b>Design note on ordering:</b> the event's own {@code actions}
     * are applied immediately, <i>before</i> any choices are offered to
     * the player. This is intentional — it models things like a Zombie
     * Attack automatically costing ammo/morale the instant it happens,
     * independent of whatever the player decides to do next (Fight/Run).
     * If a specific event should show its description first and only
     * apply consequences of the player picks a choice, put those
     * consequences on the {@link Choice} itself (via {@code
     * Choice#getActions()}) rather than on the {@code Event}, and leave
     * the event's own {@code actions} empty.
     */
    public EventResult execute(Event event, GameState state, Predicate<String> eligibleForRandomPool) {
        Objects.requireNonNull(event, "event must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(eligibleForRandomPool, "eligibleForRandomPool must not be null");

        if (!event.canTrigger(state)) {
            return EventResult.blocked(state);
        }

        event.getActions().forEach(action -> action.execute(state));

        if (event.hasChoices()) {
            List<Choice> available = event.availableChoices(state);
            if (available.isEmpty()) {
                return EventResult.resolved(state, event.resolveNextEventId(random, eligibleForRandomPool));
            }
            return EventResult.awaitingChoice(state, available);
        }

        return EventResult.resolved(state, event.resolveNextEventId(random, eligibleForRandomPool));
    }

    /** Convenience overload: no weighted pools in play, or caller doesn't care about eligibility filtering. */
    public EventResult resolveChoice(Choice choice, GameState state) {
        return resolveChoice(choice, state, ALWAYS_ELIGIBLE);
    }

    /**
     * Resolves a player's Choice within an already-triggered event:
     * applies the choice's actions and returns the next event id (via
     * {@link Choice#resolveNextEventId}, same weighted-pool semantics as
     * {@link Event}).
     *
     * <p>Callers are expected to only pass choices obtained from a
     * fresh {@code EventResult#getOfferedChoices()} call. As a safety
     * net against stale choices — e.g. a client that cached the choice
     * list and calls this well after the GameState has since changed —
     * the choice's condition is re-checked here. If it no longer holds,
     * this throws rather than silently applying actions that shouldn't
     * be available anymore.
     *
     * @throws IllegalStateException if {@code choice}'s condition no
     *         longer holds against the current {@code state}
     */
    public EventResult resolveChoice(Choice choice, GameState state, Predicate<String> eligibleForRandomPool) {
        Objects.requireNonNull(choice, "choice must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(eligibleForRandomPool, "eligibleForRandomPool must not be null");

        if (!choice.isAvailable(state)) {
            throw new IllegalStateException(
                    "Choice '" + choice.getId() + "' is no longer available: its condition "
                            + "does not hold against the current GameState. The caller likely "
                            + "resolved a stale choice obtained before the state changed.");
        }

        choice.applyActions(state);
        return EventResult.resolved(state, choice.resolveNextEventId(random, eligibleForRandomPool));
    }
}
