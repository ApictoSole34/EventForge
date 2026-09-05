package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * One candidate in a weighted random pool of next-event transitions
 * (see the nextEventPool field in {@link Event} and {@link Choice}).
 *
 * Higher weight = proportionally more likely to be picked, relative to
 * the OTHER ELIGIBLE candidates in the same pool at selection time.
 * Candidates that fail their own condition, don't exist in the
 * registry, or are currently on cooldown are excluded before weighting
 * is applied — see {@code Event#resolveNextEventId}.
 *
 * Not polymorphic (no @JsonTypeInfo needed) — it's a plain value type,
 * serialized as a regular Jackson bean.
 */
public final class WeightedTransition {

    private final String eventId;
    private final double weight;

    @JsonCreator
    public WeightedTransition(@JsonProperty("eventId") String eventId,
                              @JsonProperty("weight") double weight) {
        this.eventId = Objects.requireNonNull(eventId, "eventId must not be null");
        if (weight <= 0) {
            throw new IllegalArgumentException(
                    "weight must be > 0, got " + weight + " for eventId '" + eventId + "'");
        }
        this.weight = weight;
    }

    public String getEventId() {
        return eventId;
    }

    public double getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        return eventId + " (weight " + weight + ")";
    }
}
