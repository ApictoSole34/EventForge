package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.persistence.GameSessionPersistenceService;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/game")
public class GamePlayController {

    private final GameSessionPersistenceService gameService;
    private final ScenarioPersistenceService scenarioService;

    public GamePlayController(GameSessionPersistenceService gameService, ScenarioPersistenceService scenarioService) {
        this.gameService = gameService;
        this.scenarioService = scenarioService;
    }

    @GetMapping("/zombie-shelter")
    public String startZombieShelter() {
        var scenario = scenarioService.findAll().stream()
                .filter(s -> "Zombie Shelter".equals(s.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No 'Zombie Shelter' scenario found. Load it first (DataLoader with 'local' profile, or via /api/scenarios)."));

        UUID sessionId = gameService.startSession(scenario.id());
        return "redirect:/game/session/" + sessionId;
    }

    @GetMapping("/session/{sessionId}")
    public String viewSession(@PathVariable UUID sessionId, Model model) {
        var view = gameService.getSession(sessionId);
        model.addAttribute("gameSession", view);
        model.addAttribute("sessionId", sessionId.toString());
        return "game-play";
    }

    @PostMapping("/session/{sessionId}/trigger")
    public String trigger(@PathVariable UUID sessionId) {
        gameService.triggerCurrentEvent(sessionId);
        return "redirect:/game/session/" + sessionId;
    }

    @PostMapping("/session/{sessionId}/choice")
    public String choose(@PathVariable UUID sessionId, @RequestParam String choiceId) {
        gameService.choose(sessionId, choiceId);
        return "redirect:/game/session/" + sessionId;
    }
}
