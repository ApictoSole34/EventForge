// web/EventApiController.java
package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import com.fizzycoyotestudio.eventforge.web.dto.EventDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Full JSON CRUD for a single Event within a scenario, reusing the same
 * EventDto/EventDtoMapper the REST API and dashboard already use. Exists
 * so the graph editor (scenario-graph.html) can be a first‑class editor —
 * not just a viewer/reconnector — without duplicating field‑by‑field
 * PATCH endpoints for every attribute. Nothing here bypasses the domain
 * model: an EventDto can only carry Condition/GameAction subtypes the
 * engine already knows about (see Condition/GameAction @JsonSubTypes).
 */
@RestController
@RequestMapping("/api/scenarios/{scenarioId}/events")
public class EventApiController {

    private final ScenarioPersistenceService scenarioService;
    private final EventDtoMapper mapper;

    public EventApiController(ScenarioPersistenceService scenarioService, EventDtoMapper mapper) {
        this.scenarioService = scenarioService;
        this.mapper = mapper;
    }

    @GetMapping("/{eventId}")
    public EventDto get(@PathVariable UUID scenarioId, @PathVariable String eventId) {
        var scenario = scenarioService.load(scenarioId);
        return mapper.toDto(scenario.registry().getOrThrow(eventId));
    }

    /** Creates a brand‑new event (used by "double‑click empty canvas"). */
    @PostMapping
    public ResponseEntity<EventDto> create(@PathVariable UUID scenarioId, @Valid @RequestBody EventDto dto) {
        var scenario = scenarioService.load(scenarioId);
        if (scenario.registry().contains(dto.id())) {
            throw new IllegalArgumentException("An event with id '" + dto.id() + "' already exists in this scenario.");
        }
        Event event = mapper.toDomain(dto);
        scenarioService.saveEvent(scenarioId, event);
        return ResponseEntity.ok(mapper.toDto(event));
    }

    /** Full replace of an existing event — condition, actions, choices, nextEventId, cooldown, pool, all of it. */
    @PutMapping("/{eventId}")
    public EventDto update(@PathVariable UUID scenarioId, @PathVariable String eventId, @Valid @RequestBody EventDto dto) {
        if (!eventId.equals(dto.id())) {
            throw new IllegalArgumentException("Path eventId '" + eventId + "' does not match body id '" + dto.id() + "'.");
        }
        scenarioService.load(scenarioId).registry().getOrThrow(eventId);
        Event event = mapper.toDomain(dto);
        scenarioService.saveEvent(scenarioId, event);
        return mapper.toDto(event);
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(@PathVariable UUID scenarioId, @PathVariable String eventId) {
        var scenario = scenarioService.load(scenarioId);
        if (eventId.equals(scenario.startEventId())) {
            throw new IllegalArgumentException("Cannot delete the start event of a scenario.");
        }
        scenarioService.deleteEvent(scenarioId, eventId);
        return ResponseEntity.ok().build();
    }
}