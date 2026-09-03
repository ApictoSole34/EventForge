package com.fizzycoyotestudio.eventforge.engine;


import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

/**
 * Represents a single event in the game (e.g. "Zombie Attack",
 * "Stranger at the Gate"). An Event may:
 *   - have a condition that gates whether it can trigger at all
 *   - apply its own actions automatically (no player input), and/or
 *   - offer the player a list of Choices instead
 *   - transition to a next event, either directly (nextEventId), via
 *     whichever Choice the player picked (Choice.nextEventId takes
 *     precedence when present), or randomly from a weighted pool of
 *     candidates (nextEventPool) when one is configured
 *
 * <p><b>Cooldowns:</b> {@code cooldownTicks} is a property of THIS
 * event as a candidate — it says "once I've fired, don't let me fire
 * again for N ticks". A tick is incremented by whoever drives the
 * engine (see {@code GameSession} / {@code GameSessionPersistenceService})
 * every time any event actually triggers; the engine itself has no
 * notion of time beyond that counter. cooldownTicks only has an effect
 * when this event appears as a candidate inside some OTHER event's or
 * choice's nextEventPool — it does not gate this event's own
 * condition-based {@link #canTrigger} check. Cooldown filtering
 * happens purely at selection time, via the eligibility predicate the
 * caller supplies to {@link #resolveNextEventId}.
 *
 * <p><b>Weighted random pool:</b> if {@code nextEventPool} is
 * non-empty, it takes precedence over the plain {@code nextEventId}
 * for automatic (non-choice) transitions: one candidate is picked at
 * random, weighted by {@link WeightedTransition#getWeight()}, from
 * among only the candidates the eligibility predicate accepts
 * (typically: candidate exists in the registry, its own condition
 * holds, and it isn't on cooldown). If the pool is empty, or none of
 * its candidates are currently eligible, this falls back to
 * {@code nextEventId} (which may itself be null, ending the chain).
 */
@Getter
@Builder
public final class Event {

    private final String id;
    private final String name;
    private final String description;

    @Builder.Default
    private final Condition condition = Condition.alwaysTrue();

    @Builder.Default
    private final List<GameAction> actions = List.of();

    @Builder.Default
    private final List<Choice> choices = List.of();

    /**
     * Default transition when there are no choices (or a choice doesn't
     * override it), and/or the fallback used when nextEventPool has no
     * currently-eligible candidate.
     */
    private final String nextEventId;

    /**
     * Weighted candidates for random automatic transition. Takes
     * precedence over {@code nextEventId} when non-empty AND at least
     * one candidate is eligible at selection time.
     */
    @Builder.Default
    private final List<WeightedTransition> nextEventPool = List.of();

    /**
     * How many ticks must pass after this event last fired before it can
     * fire again as a random-pool candidate elsewhere. 0 = no cooldown.
     */
    @Builder.Default
    private final int cooldownTicks = 0;

    public boolean canTrigger(GameState state) {
        return condition.evaluate(state);
    }

    public boolean hasChoices() {
        return choices != null && !choices.isEmpty();
    }

    /** Choices currently available to the player, given the state (filters out ones whose condition fails). */
    public List<Choice> availableChoices(GameState state) {
        return choices.stream().filter(c -> c.isAvailable(state)).toList();
    }

    /**
     * Resolves this event's automatic next-event id: picks weighted-randomly
     * from {@code nextEventPool} (restricted to candidates
     * {@code eligibleForRandomPool} accepts), falling back to
     * {@code nextEventId} if the pool is empty or has no currently-eligible
     * candidate.
     */
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
