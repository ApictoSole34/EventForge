package com.fizzycoyotestudio.eventforge.engine;


import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents a single event in the game (e.g. "Zombie Attack",
 * "Stranger at the Gate"). An Event may:
 *   - have a condition that gates whether it can trigger at all
 *   - apply its own actions automatically (no player input), and/or
 *   - offer the player a list of Choices instead
 *   - transition to a next event, either directly (nextEventId) or
 *     via whichever Choice the player picked (Choice.nextEventId
 *     takes precedence when present)
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

    /** Default transition when there are no choices (or a choice doesn't override it). */
    private final String nextEventId;

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
}