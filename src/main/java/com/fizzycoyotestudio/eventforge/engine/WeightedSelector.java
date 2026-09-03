package com.fizzycoyotestudio.eventforge.engine;

import java.util.List;
import java.util.Random;

/**
 * Shared weighted-random-pick logic used by both Event and Choice when
 * resolving a next-event transition from a nextEventPool. Package-private:
 * this is an implementation detail, not part of the public engine API.
 */
final class WeightedSelector {

    private WeightedSelector() {
    }

    /**
     * Picks one candidate from {@code candidates}, weighted by
     * {@link WeightedTransition#getWeight()}. Caller is responsible for
     * having already filtered the list down to eligible candidates and
     * ensuring it's non-empty.
     */
    static String pick(List<WeightedTransition> candidates, Random random) {
        double totalWeight = candidates.stream().mapToDouble(WeightedTransition::getWeight).sum();
        double roll = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (WeightedTransition candidate : candidates) {
            cumulative += candidate.getWeight();
            if (roll < cumulative) {
                return candidate.getEventId();
            }
        }
        return candidates.get(candidates.size() - 1).getEventId();
    }
}
