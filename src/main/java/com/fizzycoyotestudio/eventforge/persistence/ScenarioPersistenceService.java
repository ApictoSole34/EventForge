package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.engine.EventRegistry;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScenarioPersistenceService {

    private final ScenarioRepository repository;
    private final EventPersistenceMapper mapper;
    private final GameStateJsonMapper stateJsonMapper;
    private final GameSessionRepository gameSessionRepository;

    public ScenarioPersistenceService(ScenarioRepository repository, EventPersistenceMapper mapper,
                                      GameStateJsonMapper stateJsonMapper, GameSessionRepository gameSessionRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.stateJsonMapper = stateJsonMapper;
        this.gameSessionRepository = gameSessionRepository;
    }

    @Transactional
    public UUID save(String name, String description, String startEventId,
                     GameState initialState, Collection<Event> events) {
        ScenarioEntity scenario = new ScenarioEntity();
        scenario.setName(name);
        scenario.setDescription(description);
        scenario.setStartEventBusinessId(startEventId);
        scenario.setInitialStateJson(stateJsonMapper.write(initialState));

        events.forEach(event -> scenario.getEvents().add(mapper.toEntity(event, scenario)));

        return repository.save(scenario).getId();
    }

    @Transactional
    public void saveEvent(UUID scenarioId, Event event) {
        ScenarioEntity scenario = repository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("No scenario with id " + scenarioId));

        boolean replaced = scenario.getEvents().removeIf(e -> e.getBusinessId().equals(event.getId()));
        if (replaced) {
            repository.saveAndFlush(scenario);
        }

        scenario.getEvents().add(mapper.toEntity(event, scenario));
        repository.save(scenario);
    }

    @Transactional
    public void deleteEvent(UUID scenarioId, String eventId) {
        ScenarioEntity scenario = repository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("No scenario with id " + scenarioId));
        scenario.getEvents().removeIf(e -> e.getBusinessId().equals(eventId));
        repository.save(scenario);
    }

    /**
     * Deletes the scenario and all its events/choices (cascaded via
     * ScenarioEntity's mapping). Any game sessions still pointing at
     * this scenario (scenarioId isn't a DB foreign key, by design —
     * see EventEntity's note on nextEventBusinessId) would otherwise
     * become permanently unplayable orphans, so we clean those up too.
     */
    @Transactional
    public void deleteScenario(UUID scenarioId) {
        ScenarioEntity scenario = repository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("No scenario with id " + scenarioId));
        repository.delete(scenario);
        gameSessionRepository.deleteByScenarioId(scenarioId);
    }

    @Transactional(readOnly = true)
    public LoadedScenario load(UUID scenarioId) {
        ScenarioEntity entity = repository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("No scenario with id " + scenarioId));

        Map<String, Event> eventsById = entity.getEvents().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toMap(Event::getId, e -> e));

        return new LoadedScenario(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getStartEventBusinessId(),
                stateJsonMapper.read(entity.getInitialStateJson()),
                new EventRegistry(eventsById)
        );
    }

    @Transactional(readOnly = true)
    public List<LoadedScenario> findAll() {
        return repository.findAll().stream()
                .map(entity -> {
                    Map<String, Event> eventsById = entity.getEvents().stream()
                            .map(mapper::toDomain)
                            .collect(Collectors.toMap(Event::getId, e -> e));
                    return new LoadedScenario(
                            entity.getId(),
                            entity.getName(),
                            entity.getDescription(),
                            entity.getStartEventBusinessId(),
                            stateJsonMapper.read(entity.getInitialStateJson()),
                            new EventRegistry(eventsById)
                    );
                })
                .toList();
    }

    public record LoadedScenario(UUID id, String name, String description, String startEventId,
                                 GameState initialState, EventRegistry registry) {}
}