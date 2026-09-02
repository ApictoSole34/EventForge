package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.persistence.GameSessionPersistenceService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameSessionPersistenceService service;

    public GameController(GameSessionPersistenceService service) {
        this.service = service;
    }

    @PostMapping("/start")
    public ResponseEntity<StartResponse> start(@Valid @RequestBody StartRequest request) {
        UUID sessionId = service.startSession(request.scenarioId());
        return ResponseEntity.created(URI.create("/api/game/" + sessionId))
                .body(new StartResponse(sessionId));
    }

    @GetMapping("/{id}")
    public GameSessionPersistenceService.GameSessionView get(@PathVariable UUID id) {
        return service.getSession(id);
    }

    @PostMapping("/{id}/event")
    public GameSessionPersistenceService.GameSessionView triggerEvent(@PathVariable UUID id) {
        return service.triggerCurrentEvent(id);
    }

    @PostMapping("/{id}/choice")
    public GameSessionPersistenceService.GameSessionView choose(@PathVariable UUID id,
                                                                @Valid @RequestBody ChoiceRequest request) {
        return service.choose(id, request.choiceId());
    }

    public record StartRequest(@NotNull UUID scenarioId) {}
    public record StartResponse(UUID sessionId) {}
    public record ChoiceRequest(@NotBlank String choiceId) {}
}
