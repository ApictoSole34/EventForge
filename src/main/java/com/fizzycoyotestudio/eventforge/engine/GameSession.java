package com.fizzycoyotestudio.eventforge.engine;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Orchestrates a single play session: holds the GameState, the current
 * Event, and drives transitions between events via the EventEngine and
 * an EventRegistry.
 *
 * This class is intentionally game-agnostic — it works the same way
 * whether the events came from Zombie Shelter, a fantasy dungeon, or
 * any other game built on EventForge.
 *
 * <p><b>Ticks &amp; cooldowns:</b> GameSession is where the "tick"
 * concept actually lives. Every time an event fires (its condition
 * passed and its actions ran — i.e. {@code EventResult.isTriggered()}),
 * the tick counter advances by one and that event's id is stamped with
 * the tick it fired on. When resolving a weighted nextEventPool
 * (Event or Choice), a candidate's eligibility (registry contains it,
 * its own condition currently holds, and it isn't still on cooldown) is
 * delegated to {@link EligibilityChecker} — the same check
 * {@code GameSessionPersistenceService} uses for the DB-backed flow.
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

    @Getter
    private int currentTick = 0;

    private final Map<String, Integer> lastTriggeredTick = new HashMap<>();

    public GameSession(EventEngine engine, EventRegistry registry, GameState state, String startEventId) {
        this(engine, registry, state, startEventId, 0, Map.of());
    }

    /** Full-state constructor — used when resuming a persisted session that already has cooldown/tick history. */
    public GameSession(EventEngine engine, EventRegistry registry, GameState state, String startEventId,
                       int currentTick, Map<String, Integer> lastTriggeredTick) {
        this.engine = Objects.requireNonNull(engine);
        this.registry = Objects.requireNonNull(registry);
        this.state = Objects.requireNonNull(state);
        this.currentEvent = registry.getOrThrow(startEventId);
        this.currentTick = currentTick;
        this.lastTriggeredTick.putAll(lastTriggeredTick);
    }

    /** Read-only view of "which event last fired on which tick", e.g. for persistence or debugging. */
    public Map<String, Integer> getLastTriggeredTick() {
        return Collections.unmodifiableMap(lastTriggeredTick);
    }

    /** Triggers the current event: applies its actions, and either offers choices or advances automatically. */
    public EventResult triggerCurrentEvent() {
        if (terminal) {
            throw new IllegalStateException("Session is terminal; no further events.");
        }
        EventResult result = engine.execute(currentEvent, state, this::isEligibleCandidate);
        if (!result.isTriggered()) {
            pendingChoices = List.of();
            return result;
        }

        recordTrigger(currentEvent.getId());

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

        EventResult result = engine.resolveChoice(choice, state, this::isEligibleCandidate);
        pendingChoices = List.of();
        if (result.hasNextEvent()) {
            advanceTo(result.getNextEventId());
        } else {
            terminal = true;
        }
        return result;
    }

    private void recordTrigger(String eventId) {
        currentTick++;
        lastTriggeredTick.put(eventId, currentTick);
    }

    private boolean isEligibleCandidate(String eventId) {
        return EligibilityChecker.isEligible(registry, state, lastTriggeredTick, currentTick, eventId);
    }

    private void advanceTo(String nextEventId) {
        if (nextEventId != null && registry.contains(nextEventId)) {
            this.currentEvent = registry.getOrThrow(nextEventId);
        } else {
            this.terminal = true;
        }
    }
}