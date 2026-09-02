package com.fizzycoyotestudio.eventforge.persistence;


import com.fizzycoyotestudio.eventforge.engine.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Drives GameSessions over HTTP. Each request reconstructs the engine
 * state fresh from the DB rather than reusing a long-lived, in-memory
 * GameSession (engine package) — that class caches "pendingChoices"
 * between trigger() and choose(), which cannot survive across separate
 * HTTP requests. Instead, available choices are re-derived directly
 * from the current Event + GameState on every call. This is safe
 * because Condition#evaluate has no side effects, and it avoids ever
 * re-applying an event's own actions more than once (which triggering
 * the same event twice via GameSession.triggerCurrentEvent() would risk).
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
        ScenarioPersistenceService.LoadedScenario scenario = scenarioService.load(scenarioId);

        GameSessionEntity entity = new GameSessionEntity();
        entity.setScenarioId(scenarioId);
        entity.setCurrentEventBusinessId(scenario.startEventId());
        entity.setStateJson(stateJsonMapper.write(scenario.initialState()));

        return repository.save(entity).getId();
    }

    @Transactional(readOnly = true)
    public GameSessionView getSession(UUID sessionId) {
        GameSessionEntity entity = getOrThrow(sessionId);
        ScenarioPersistenceService.LoadedScenario scenario = scenarioService.load(entity.getScenarioId());
        GameState state = stateJsonMapper.read(entity.getStateJson());
        Event current = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());

        return new GameSessionView(entity.getId(), current.getId(), current.getName(),
                current.getDescription(), List.of(), state.asMap());
    }

    /** Triggers the session's current event: applies its actions, offers choices or advances. */
    @Transactional
    public GameSessionView triggerCurrentEvent(UUID sessionId) {
        GameSessionEntity entity = getOrThrow(sessionId);
        ScenarioPersistenceService.LoadedScenario scenario = scenarioService.load(entity.getScenarioId());
        GameState state = stateJsonMapper.read(entity.getStateJson());
        Event current = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());

        EventResult result = engine.execute(current, state);

        if (result.isTriggered() && !result.isAwaitingChoice() && result.hasNextEvent()) {
            entity.setCurrentEventBusinessId(result.getNextEventId());
        }
        entity.setStateJson(stateJsonMapper.write(state));
        repository.save(entity);

        Event currentEvent = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());

        List<ChoiceView> choices = result.isAwaitingChoice()
                ? result.getOfferedChoices().stream().map(c -> new ChoiceView(c.getId(), c.getLabel())).toList()
                : List.of();

        return new GameSessionView(entity.getId(), currentEvent.getId(), currentEvent.getName(),
                currentEvent.getDescription(), choices, state.asMap());
    }

    /** Resolves a player's choice on the session's current event and advances it. */
    @Transactional
    public GameSessionView choose(UUID sessionId, String choiceId) {
        GameSessionEntity entity = getOrThrow(sessionId);
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
        }
        entity.setStateJson(stateJsonMapper.write(state));
        repository.save(entity);

        Event newCurrent = scenario.registry().getOrThrow(entity.getCurrentEventBusinessId());
        return new GameSessionView(entity.getId(), newCurrent.getId(), newCurrent.getName(),
                newCurrent.getDescription(), List.of(), state.asMap());
    }

    private GameSessionEntity getOrThrow(UUID sessionId) {
        return repository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("No game session with id " + sessionId));
    }

    public record ChoiceView(String id, String label) {}

    public record GameSessionView(UUID sessionId, String eventId, String eventName, String eventDescription,
                                  List<ChoiceView> choices, Map<String, Double> state) {}
}
