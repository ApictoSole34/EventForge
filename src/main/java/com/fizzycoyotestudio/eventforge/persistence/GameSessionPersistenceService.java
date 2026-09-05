package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Drives GameSessions over HTTP. Each request reconstructs the engine
 * state fresh from the DB rather than reusing a long-lived, in-memory
 * GameSession (engine package) — see the class-level note that used to
 * live here for the full rationale (pendingChoices can't survive across
 * requests). For the same reason this class does NOT use the engine's
 * GameSession class at all — it re-implements the (small) amount of
 * tick/cooldown bookkeeping GameSession does, directly against
 * GameSessionEntity, since it needs that bookkeeping to survive a
 * request boundary via persisted columns rather than live in memory.
 *
 * <p>JSON (de)serialization of GameState and the cooldown map is
 * delegated to {@link EventForgeJsonMapper} — the single consolidated
 * mapper also used by {@link EventPersistenceMapper} and
 * {@link ScenarioPersistenceService}, replacing what used to be two
 * separate single-purpose mappers ({@code GameStateJsonMapper},
 * {@code CooldownJsonMapper}) here.
 *
 * Three persisted flags/fields make the session's state fully derivable
 * from a plain GET (no action needs to run just to render a page):
 *   - triggered: has the current event's own actions already been
 *     applied / has it already been revealed to the player?
 *   - terminal: has this path reached a dead end (no further event to
 *     move to)?
 *   - currentTick / cooldownJson: how many events have fired so far in
 *     this session, and when each one last fired — used to filter
 *     weighted nextEventPool candidates that are still on cooldown.
 * triggered and terminal reset to false whenever the session advances
 * to a new event; currentTick/cooldownJson only ever move forward.
 */
@Service
public class GameSessionPersistenceService {

    private final GameSessionRepository repository;
    private final ScenarioPersistenceService scenarioService;
    private final EventForgeJsonMapper json;
    private final EventEngine engine = new EventEngine();

    public GameSessionPersistenceService(GameSessionRepository repository,
                                         ScenarioPersistenceService scenarioService,
                                         EventForgeJsonMapper json) {
        this.repository = repository;
        this.scenarioService = scenarioService;
        this.json = json;
    }

    @Transactional
    public UUID startSession(UUID scenarioId) {
        return startSession(scenarioId, null);
    }

    /** playerId is null for sessions started via the REST API (no web cookie identity involved). */
    @Transactional
    public UUID startSession(UUID scenarioId, UUID playerId) {
        ScenarioPersistenceService.LoadedScenario scenario = scenarioService.load(scenarioId);

        GameSessionEntity entity = new GameSessionEntity();
        entity.setScenarioId(scenarioId);
        entity.setPlayerId(playerId);
        entity.setCurrentEventBusinessId(scenario.startEventId());
        entity.setStateJson(json.writeState(scenario.initialState()));
        entity.setTriggered(false);
        entity.setTerminal(false);
        entity.setCurrentTick(0);
        entity.setCooldownJson(json.writeCooldowns(Map.of()));

        return repository.save(entity).getId();
    }

    /** Returns an in-progress session id for this player+scenario, if one exists, so "Play" resumes instead of restarting. */
    @Transactional(readOnly = true)
    public Optional<UUID> findResumableSession(UUID playerId, UUID scenarioId) {
        if (playerId == null) {
            return Optional.empty();
        }
        return repository.findFirstByPlayerIdAndScenarioIdAndTerminalFalseOrderByUpdatedAtDesc(playerId, scenarioId)
                .map(GameSessionEntity::getId);
    }

    /** All of this player's sessions (in-progress and finished), most recently active first. */
    @Transactional(readOnly = true)
    public List<GameSummaryView> findMyGames(UUID playerId) {
        if (playerId == null) {
            return List.of();
        }
        return repository.findByPlayerIdOrderByUpdatedAtDesc(playerId).stream()
                .map(this::toSummaryOrNull)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Only deletes if the session actually belongs to this player — a basic ownership guard without full auth. */
    @Transactional
    public void deleteSession(UUID sessionId, UUID playerId) {
        GameSessionEntity entity = getOrThrow(sessionId);
        if (playerId == null || !playerId.equals(entity.getPlayerId())) {
            throw new IllegalArgumentException("No game session with id " + sessionId);
        }
        repository.delete(entity);
    }

    private static final DateTimeFormatter LAST_PLAYED_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    /** Skips (rather than blows up on) sessions whose scenario has since been deleted. */
    private GameSummaryView toSummaryOrNull(GameSessionEntity entity) {
        try {
            ScenarioPersistenceService.LoadedScenario scenario = scenarioService.load(entity.getScenarioId());
            Event current = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());
            String lastPlayedAt = entity.getUpdatedAt() != null ? LAST_PLAYED_FORMAT.format(entity.getUpdatedAt()) : "";
            return new GameSummaryView(
                    entity.getId(),
                    entity.getScenarioId(),
                    scenario.name(),
                    current.getName(),
                    entity.isTerminal(),
                    lastPlayedAt
            );
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** Pure read: derives the full view from persisted state, applies nothing. */
    @Transactional(readOnly = true)
    public GameSessionView getSession(UUID sessionId) {
        GameSessionEntity entity = getOrThrow(sessionId);
        ScenarioPersistenceService.LoadedScenario scenario = scenarioService.load(entity.getScenarioId());
        GameState state = json.readState(entity.getStateJson());
        Event current = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());

        List<ChoiceView> choices = (entity.isTriggered() && !entity.isTerminal() && current.hasChoices())
                ? current.availableChoices(state).stream()
                .map(c -> new ChoiceView(c.getId(), c.getLabel()))
                .toList()
                : List.of();

        return new GameSessionView(
                entity.getId(),
                entity.getScenarioId(),
                scenario.name(),
                current.getId(),
                current.getName(),
                current.getDescription(),
                entity.isTriggered(),
                entity.isTerminal(),
                choices,
                state.asMap(),
                entity.getCurrentTick()
        );
    }

    /** Applies the current event's own actions exactly once. Safe to call repeatedly — no-op if already triggered. */
    @Transactional
    public GameSessionView triggerCurrentEvent(UUID sessionId) {
        GameSessionEntity entity = getOrThrow(sessionId);
        if (entity.isTerminal()) {
            return getSession(sessionId);
        }
        if (entity.isTriggered()) {
            return getSession(sessionId);
        }

        ScenarioPersistenceService.LoadedScenario scenario = scenarioService.load(entity.getScenarioId());
        GameState state = json.readState(entity.getStateJson());
        Event current = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());
        Map<String, Integer> lastTriggeredTick = json.readCooldowns(entity.getCooldownJson());
        int tick = entity.getCurrentTick();

        Predicate<String> eligible = candidateId ->
                isEligible(scenario.registry(), state, lastTriggeredTick, tick, candidateId);

        EventResult result = engine.execute(current, state, eligible);

        if (!result.isTriggered()) {
            entity.setTerminal(true);
        } else {
            int newTick = tick + 1;
            Map<String, Integer> updatedCooldowns = new HashMap<>(lastTriggeredTick);
            updatedCooldowns.put(current.getId(), newTick);
            entity.setCurrentTick(newTick);
            entity.setCooldownJson(json.writeCooldowns(updatedCooldowns));

            entity.setTriggered(true);
            if (!result.isAwaitingChoice()) {
                if (result.hasNextEvent()) {
                    entity.setCurrentEventBusinessId(result.getNextEventId());
                    entity.setTriggered(false);
                } else {
                    entity.setTerminal(true);
                }
            }
        }

        entity.setStateJson(json.writeState(state));
        repository.save(entity);
        return getSession(sessionId);
    }

    /** Resolves a player's choice. Only valid once the current event has been triggered. */
    @Transactional
    public GameSessionView choose(UUID sessionId, String choiceId) {
        GameSessionEntity entity = getOrThrow(sessionId);
        if (!entity.isTriggered() || entity.isTerminal()) {
            throw new IllegalStateException("No choice is currently pending for this session.");
        }

        ScenarioPersistenceService.LoadedScenario scenario = scenarioService.load(entity.getScenarioId());
        GameState state = json.readState(entity.getStateJson());
        Event current = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());
        Map<String, Integer> lastTriggeredTick = json.readCooldowns(entity.getCooldownJson());
        int tick = entity.getCurrentTick();

        Choice choice = current.getChoices().stream()
                .filter(c -> c.getId().equals(choiceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Choice '" + choiceId + "' does not exist on event '" + current.getId() + "'"));

        Predicate<String> eligible = candidateId ->
                isEligible(scenario.registry(), state, lastTriggeredTick, tick, candidateId);

        EventResult result = engine.resolveChoice(choice, state, eligible);

        if (result.hasNextEvent()) {
            entity.setCurrentEventBusinessId(result.getNextEventId());
            entity.setTriggered(false);
        } else {
            entity.setTerminal(true);
        }

        entity.setStateJson(json.writeState(state));
        repository.save(entity);
        return getSession(sessionId);
    }

    /** A candidate from a weighted pool is eligible if it exists, its own condition currently holds, and it isn't on cooldown. */
    private boolean isEligible(EventRegistry registry, GameState state, Map<String, Integer> lastTriggeredTick,
                               int currentTick, String candidateId) {
        if (!registry.contains(candidateId)) {
            return false;
        }
        Event candidate = registry.getOrThrow(candidateId);
        if (!candidate.canTrigger(state)) {
            return false;
        }
        int cooldown = candidate.getCooldownTicks();
        if (cooldown > 0) {
            Integer lastFired = lastTriggeredTick.get(candidateId);
            if (lastFired != null && (currentTick - lastFired) < cooldown) {
                return false;
            }
        }
        return true;
    }

    private GameSessionEntity getOrThrow(UUID sessionId) {
        return repository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("No game session with id " + sessionId));
    }

    public record ChoiceView(String id, String label) {}
    public record GameSessionView(UUID sessionId, UUID scenarioId, String scenarioName, String eventId, String eventName, String eventDescription,
                                  Boolean triggered, Boolean terminal,
                                  List<ChoiceView> choices, Map<String, Double> state, int currentTick) {}

    public record GameSummaryView(UUID sessionId, UUID scenarioId, String scenarioName, String currentEventName,
                                  boolean terminal, String lastPlayedAt) {}
}
