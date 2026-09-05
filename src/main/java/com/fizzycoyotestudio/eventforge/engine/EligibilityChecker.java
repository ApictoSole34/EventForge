package com.fizzycoyotestudio.eventforge.engine;

import java.util.Map;
import java.util.Objects;

/**
 * Shared "is this candidate eligible for a weighted nextEventPool pick"
 * check, used identically by {@code GameSession} (in-memory play) and
 * {@code GameSessionPersistenceService} (DB-backed REST/web play).
 *
 * <p>Previously this logic was copy-pasted in both places (one reading
 * from a field, the other from a parameter map) — a prime candidate for
 * the two copies quietly drifting apart the next time cooldown rules
 * change. Both callers now just supply their own registry/state/tick
 * bookkeeping and get identical eligibility semantics for free.
 *
 * <p>A candidate is eligible if:
 * <ul>
 *   <li>it exists in the registry,</li>
 *   <li>its own condition currently holds against {@code state}, and</li>
 *   <li>it isn't still within its {@code cooldownTicks} window since it
 *       last fired in this session.</li>
 * </ul>
 */
public final class EligibilityChecker {

    private EligibilityChecker() {
    }

    /**
     * @param registry          where the candidate event is looked up
     * @param state              current GameState, used to re-check the candidate's own condition
     * @param lastTriggeredTick  eventId -> the tick it last fired on, within this session
     * @param currentTick        the session's current tick counter
     * @param candidateId        the event id being considered as a pool candidate
     */
    public static boolean isEligible(EventRegistry registry, GameState state,
                                     Map<String, Integer> lastTriggeredTick,
                                     int currentTick, String candidateId) {
        Objects.requireNonNull(registry, "registry must not be null");
        Objects.requireNonNull(state, "state must not be null");
        Objects.requireNonNull(lastTriggeredTick, "lastTriggeredTick must not be null");
        Objects.requireNonNull(candidateId, "candidateId must not be null");

        if (!registry.contains(candidateId)) {
            return false;
        }
        Event candidate = registry.getOrThrow(candidateId);
        if (!candidate.canTrigger(state)) {
            return false;
        }

        int cooldown = candidate.getCooldownTicks();
        if (cooldown <= 0) {
            return true;
        }
        Integer lastFired = lastTriggeredTick.get(candidateId);
        if (lastFired == null) {
            return true;
        }
        return (currentTick - lastFired) >= cooldown;
    }
}