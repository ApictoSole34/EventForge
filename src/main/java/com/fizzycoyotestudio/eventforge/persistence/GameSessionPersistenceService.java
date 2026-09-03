package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Drives GameSessions over HTTP. Each request reconstructs the engine
 * state fresh from the DB rather than reusing a long-lived, in-memory
 * GameSession (engine package) — see the class-level note that used to
 * live here for the full rationale (pendingChoices can't survive across
 * requests).
 *
 * Two persisted flags make the session's state fully derivable from a
 * plain GET (no action needs to run just to render a page):
 *   - triggered: has the current event's own actions already been
 *     applied / has it already been revealed to the player?
 *   - terminal: has this path reached a dead end (no further event to
 *     move to)?
 * Both reset to false whenever the session advances to a new event.
 */
@Service
public class GameSessionPersistenceService {

    private final GameSessionRepository repository;
    private final ScenarioPersistenceService scenarioService;
    private final GameStateJsonMapper stateJsonMapper;
    private final EventEngine engine = new EventEngine();

    public GameSessionPersistenceService(GameSessionRepository repository,
                                         ScenarioPersistenceService scenarioService,
                                         GameStateJsonMapper stateJsonMapper) {
        this.repository = repository;
        this.scenarioService = scenarioService;
        this.stateJsonMapper = stateJsonMapper;
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
        entity.setStateJson(stateJsonMapper.write(scenario.initialState()));
        entity.setTriggered(false);
        entity.setTerminal(false);

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
        GameState state = stateJsonMapper.read(entity.getStateJson());
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
                state.asMap()
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
        GameState state = stateJsonMapper.read(entity.getStateJson());
        Event current = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());

        EventResult result = engine.execute(current, state);

        if (!result.isTriggered()) {
            entity.setTerminal(true);
        } else {
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

        entity.setStateJson(stateJsonMapper.write(state));
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
        GameState state = stateJsonMapper.read(entity.getStateJson());
        Event current = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());

        Choice choice = current.getChoices().stream()
                .filter(c -> c.getId().equals(choiceId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Choice '" + choiceId + "' does not exist on event '" + current.getId() + "'"));

        EventResult result = engine.resolveChoice(choice, state);

        if (result.hasNextEvent()) {
            entity.setCurrentEventBusinessId(result.getNextEventId());
            entity.setTriggered(false);
        } else {
            entity.setTerminal(true);
        }

        entity.setStateJson(stateJsonMapper.write(state));
        repository.save(entity);
        return getSession(sessionId);
    }

    private GameSessionEntity getOrThrow(UUID sessionId) {
        return repository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("No game session with id " + sessionId));
    }

    public record ChoiceView(String id, String label) {}
    public record GameSessionView(UUID sessionId, UUID scenarioId, String scenarioName, String eventId, String eventName, String eventDescription,
                                  Boolean triggered, Boolean terminal,
                                  List<ChoiceView> choices, Map<String, Double> state) {}

    public record GameSummaryView(UUID sessionId, UUID scenarioId, String scenarioName, String currentEventName,
                                  boolean terminal, String lastPlayedAt) {}
}