package com.fizzycoyotestudio.eventforge.engine;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * A lookup table of Events by id. Used by GameSession to resolve
 * "next event" transitions. Deliberately dumb and game-agnostic — it
 * knows nothing about how the events were built (hardcoded, JSON,
 * database — Phase 3+).
 */
public final class EventRegistry {

    private final Map<String, Event> eventsById;

    public EventRegistry(Map<String, Event> eventsById) {
        this.eventsById = Map.copyOf(Objects.requireNonNull(eventsById));
    }

    public Event getOrThrow(String eventId) {
        Event event = eventsById.get(eventId);
        if (event == null) {
            throw new IllegalArgumentException("No event registered with id '" + eventId + "'");
        }
        return event;
    }

    public boolean contains(String eventId) {
        return eventsById.containsKey(eventId);
    }

    public Collection<Event> getAll() {
        return eventsById.values();
    }
}
