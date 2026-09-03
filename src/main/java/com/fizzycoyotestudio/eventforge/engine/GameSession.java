package com.fizzycoyotestudio.eventforge.engine;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

/**
 * Orchestrates a single play session: holds the GameState, the current
 * Event, and drives transitions between events via the EventEngine and
 * an EventRegistry.
 *
 * This class is intentionally game-agnostic — it works the same way
 * whether the events came from Zombie Shelter, a fantasy dungeon, or
 * any other game built on EventForge.
 */
public final class GameSession {

    private final EventEngine engine;
    private final EventRegistry registry;
    @Getter
    private final GameState state;

    @Getter
    private Event currentEvent;

    @Getter
    private List<Choice> pendingChoices = List.of();

    @Getter
    private boolean terminal = false;

    public GameSession(EventEngine engine, EventRegistry registry, GameState state, String startEventId) {
        this.engine = Objects.requireNonNull(engine);
        this.registry = Objects.requireNonNull(registry);
        this.state = Objects.requireNonNull(state);
        this.currentEvent = registry.getOrThrow(startEventId);
    }

    /** Triggers the current event: applies its actions, and either offers choices or advances automatically. */
    public EventResult triggerCurrentEvent() {
        if (terminal) {
            throw new IllegalStateException("Session is terminal; no further events.");
        }
        EventResult result = engine.execute(currentEvent, state);
        if (!result.isTriggered()) {
            pendingChoices = List.of();
            return result;
        }
        if (result.isAwaitingChoice()) {
            pendingChoices = result.getOfferedChoices();
        } else {
            pendingChoices = List.of();
            if (result.hasNextEvent()) {
                advanceTo(result.getNextEventId());
            } else {
                terminal = true;
            }
        }
        return result;
    }

    /** Resolves one of the currently offered choices by id and advances the session. */
    public EventResult choose(String choiceId) {
        Choice choice = pendingChoices.stream()
                .filter(c -> c.getId().equals(choiceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Choice '" + choiceId + "' is not currently offered"));

        EventResult result = engine.resolveChoice(choice, state);
        pendingChoices = List.of();
        if (result.hasNextEvent()) {
            advanceTo(result.getNextEventId());
        } else {
            terminal = true;
        }
        return result;
    }

    private void advanceTo(String nextEventId) {
        if (nextEventId != null && registry.contains(nextEventId)) {
            this.currentEvent = registry.getOrThrow(nextEventId);
        } else {
            this.terminal = true;
        }
    }
}