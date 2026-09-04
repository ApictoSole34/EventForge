package com.fizzycoyotestudio.eventforge.engine;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Represents a single choice a player can make within an Event, e.g.
 * "Let him in" / "Refuse" / "Search him".
 *
 * Each choice has its own conditions (whether it's even offered),
 * its own actions (what happens if picked), and its own next-event
 * transition — independent of the parent Event's own actions/next.
 * Like Event, a choice may also transition to a weighted-random
 * candidate from {@code nextEventPool} instead of a fixed
 * {@code nextEventId} — see {@link Event}'s class doc for the shared
 * semantics (fallback behaviour, eligibility). Choices themselves don't
 * carry a cooldown — cooldown is a property of the candidate EVENTS in
 * the pool, not of the choice offering them.
 */
@Getter
@Builder(toBuilder = true)
public final class Choice {

    private final String id;
    private final String label;
    private final String description;

    @Builder.Default
    private final Condition condition = Condition.alwaysTrue();

    @Builder.Default
    private final List<GameAction> actions = List.of();

    /** Id of the event to transition to after this choice is resolved. May be null (end of chain), or overridden by nextEventPool. */
    private final String nextEventId;

    /** Weighted candidates for random transition. Takes precedence over nextEventId when non-empty AND at least one candidate is eligible. */
    @Builder.Default
    private final List<WeightedTransition> nextEventPool = List.of();

    public boolean isAvailable(GameState state) {
        return condition.evaluate(state);
    }

    public void applyActions(GameState state) {
        actions.forEach(action -> action.execute(state));
    }

    /** See {@link Event#resolveNextEventId} — identical semantics. */
    public String resolveNextEventId(Random random, Predicate<String> eligibleForRandomPool) {
        if (nextEventPool == null || nextEventPool.isEmpty()) {
            return nextEventId;
        }
        List<WeightedTransition> eligible = nextEventPool.stream()
                .filter(t -> eligibleForRandomPool.test(t.getEventId()))
                .toList();
        if (eligible.isEmpty()) {
            return nextEventId;
        }
        return WeightedSelector.pick(eligible, random);
    }
}