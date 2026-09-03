package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.persistence.GameSessionPersistenceService;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Drives web (non-API) gameplay. There's no login system, so we
 * identify "the same visitor" via a small "ef_player" cookie holding a
 * random UUID, set on first visit. Game sessions are stamped with that
 * playerId, which lets us resume in-progress sessions and list "My
 * Games" without requiring real authentication. Swapping this for a
 * logged-in user id later is a drop-in replacement — everything
 * downstream just needs a UUID.
 */
@Controller
@RequestMapping("/game")
public class GamePlayController {

    private static final String PLAYER_COOKIE = "ef_player";
    private static final int PLAYER_COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // 1 year

    private final GameSessionPersistenceService gameService;
    private final ScenarioPersistenceService scenarioService;

    public GamePlayController(GameSessionPersistenceService gameService, ScenarioPersistenceService scenarioService) {
        this.gameService = gameService;
        this.scenarioService = scenarioService;
    }

    /** Generic entry point: resumes an in-progress session for this scenario, or starts a new one. */
    @GetMapping("/start/{scenarioId}")
    public String start(@PathVariable UUID scenarioId,
                        @CookieValue(value = PLAYER_COOKIE, required = false) String playerCookie,
                        HttpServletResponse response) {
        scenarioService.load(scenarioId);

        UUID playerId = resolvePlayerId(playerCookie, response);

        UUID sessionId = gameService.findResumableSession(playerId, scenarioId)
                .orElseGet(() -> gameService.startSession(scenarioId, playerId));

        return "redirect:/game/session/" + sessionId;
    }

    /** Kept for the nav bar's quick-play link; resolves the demo scenario and delegates. */
    @GetMapping("/zombie-shelter")
    public String startZombieShelter(@CookieValue(value = PLAYER_COOKIE, required = false) String playerCookie,
                                     HttpServletResponse response) {
        var scenario = scenarioService.findAll().stream()
                .filter(s -> "Zombie Shelter".equals(s.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No 'Zombie Shelter' scenario found. Load it first (DataLoader with 'local' profile, or via /api/scenarios)."));

        return start(scenario.id(), playerCookie, response);
    }

    @GetMapping("/my-games")
    public String myGames(@CookieValue(value = PLAYER_COOKIE, required = false) String playerCookie,
                          HttpServletResponse response, Model model) {
        UUID playerId = resolvePlayerId(playerCookie, response);
        model.addAttribute("games", gameService.findMyGames(playerId));
        return "my-games";
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

    @PostMapping("/session/{sessionId}/delete")
    public String deleteSession(@PathVariable UUID sessionId,
                                @CookieValue(value = PLAYER_COOKIE, required = false) String playerCookie) {
        gameService.deleteSession(sessionId, parsePlayerId(playerCookie));
        return "redirect:/game/my-games";
    }

    private UUID resolvePlayerId(String cookieValue, HttpServletResponse response) {
        UUID playerId = parsePlayerId(cookieValue);
        if (playerId == null) {
            playerId = UUID.randomUUID();
            Cookie cookie = new Cookie(PLAYER_COOKIE, playerId.toString());
            cookie.setPath("/");
            cookie.setMaxAge(PLAYER_COOKIE_MAX_AGE);
            response.addCookie(cookie);
        }
        return playerId;
    }

    private UUID parsePlayerId(String cookieValue) {
        if (!StringUtils.hasText(cookieValue)) {
            return null;
        }
        try {
            return UUID.fromString(cookieValue);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}