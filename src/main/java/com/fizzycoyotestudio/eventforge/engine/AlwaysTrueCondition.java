package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * A condition that is always true — the default for Events/Choices that
 * don't gate on anything. Implemented as a concrete, registered type
 * (rather than a lambda) so it can round-trip through JSON: a lambda has
 * no stable, serializable type id.
 */
public final class AlwaysTrueCondition implements Condition {

    @JsonCreator
    public AlwaysTrueCondition() {
    }

    @Override
    public boolean evaluate(GameState state) {
        return true;
    }
}
