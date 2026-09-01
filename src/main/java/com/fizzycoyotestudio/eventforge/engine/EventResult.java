package com.fizzycoyotestudio.eventforge.engine;

import lombok.Getter;

import java.util.List;

/**
 * Outcome of running an Event (or a Choice within it) through the
 * EventEngine: whether it actually fired, the resulting GameState,
 * which choices are now offered to the player (if any), and what the
 * next event id is (if any).
 */
@Getter
public final class EventResult {

    private final boolean triggered;
    private final GameState resultingState;
    private final List<Choice> offeredChoices;
    private final String nextEventId;

    private EventResult(boolean triggered, GameState resultingState,
                        List<Choice> offeredChoices, String nextEventId) {
        this.triggered = triggered;
        this.resultingState = resultingState;
        this.offeredChoices = offeredChoices;
        this.nextEventId = nextEventId;
    }

    public static EventResult blocked(GameState state) {
        return new EventResult(false, state, List.of(), null);
    }

    public static EventResult awaitingChoice(GameState state, List<Choice> choices) {
        return new EventResult(true, state, choices, null);
    }

    public static EventResult resolved(GameState state, String nextEventId) {
        return new EventResult(true, state, List.of(), nextEventId);
    }

    public boolean isAwaitingChoice() {
        return !offeredChoices.isEmpty();
    }

    public boolean hasNextEvent() {
        return nextEventId != null;
    }
}
