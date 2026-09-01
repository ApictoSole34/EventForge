package com.fizzycoyotestudio.eventforge.engine;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Represents a single choice a player can make within an Event, e.g.
 * "Let him in" / "Refuse" / "Search him".
 *
 * Each choice has its own conditions (whether it's even offered),
 * its own actions (what happens if picked), and its own next-event
 * transition — independent of the parent Event's own actions/next.
 */
@Getter
@Builder
public final class Choice {

    private final String id;
    private final String label;
    private final String description;

    @Builder.Default
    private final Condition condition = Condition.alwaysTrue();

    @Builder.Default
    private final List<GameAction> actions = List.of();

    /** Id of the event to transition to after this choice is resolved. May be null (end of chain). */
    private final String nextEventId;

    public boolean isAvailable(GameState state) {
        return condition.evaluate(state);
    }

    public void applyActions(GameState state) {
        actions.forEach(action -> action.execute(state));
    }
}
