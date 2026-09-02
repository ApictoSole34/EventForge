package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.engine.EventRegistry;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ScenarioPersistenceService {

    private final ScenarioRepository repository;
    private final EventPersistenceMapper mapper;
    private final GameStateJsonMapper stateJsonMapper;

    public ScenarioPersistenceService(ScenarioRepository repository, EventPersistenceMapper mapper,
                                      GameStateJsonMapper stateJsonMapper) {
        this.repository = repository;
        this.mapper = mapper;
        this.stateJsonMapper = stateJsonMapper;
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

    public record LoadedScenario(UUID id, String name, String description, String startEventId,
                                 GameState initialState, EventRegistry registry) {}
}