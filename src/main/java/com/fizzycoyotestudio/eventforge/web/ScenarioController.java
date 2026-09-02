package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import com.fizzycoyotestudio.eventforge.web.dto.EventDto;
import com.fizzycoyotestudio.eventforge.web.dto.ScenarioCreateRequest;
import com.fizzycoyotestudio.eventforge.web.dto.ScenarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {

    private final ScenarioPersistenceService service;
    private final EventDtoMapper mapper;

    public ScenarioController(ScenarioPersistenceService service, EventDtoMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<ScenarioResponse> create(@Valid @RequestBody ScenarioCreateRequest request) {
        List<Event> events = request.events().stream().map(mapper::toDomain).toList();
        GameState initialState = new GameState(request.initialState());
        UUID id = service.save(request.name(), request.description(), request.startEventId(), initialState, events);

        ScenarioResponse response = toResponse(id, request.name(), request.description(),
                request.startEventId(), initialState.asMap(), events);

        return ResponseEntity.created(URI.create("/api/scenarios/" + id)).body(response);
    }

    @GetMapping("/{id}")
    public ScenarioResponse get(@PathVariable UUID id) {
        ScenarioPersistenceService.LoadedScenario loaded = service.load(id);
        List<Event> events = List.copyOf(loaded.registry().getAll());
        return toResponse(id, loaded.name(), loaded.description(), loaded.startEventId(),
                loaded.initialState().asMap(), events);
    }

    private ScenarioResponse toResponse(UUID id, String name, String description, String startEventId,
                                        Map<String, Double> initialState, List<Event> events) {
        List<EventDto> eventDtos = events.stream().map(mapper::toDto).toList();
        return new ScenarioResponse(id, name, description, startEventId, initialState, eventDtos);
    }
}
