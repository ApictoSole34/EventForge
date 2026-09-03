package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.persistence.GameSessionPersistenceService;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Drives web (non-API) gameplay. Sessions aren't tied to a logged-in
 * user yet — as an interim measure (before a full "My Games" /
 * multi-session feature lands) we remember the visitor's most recent
 * active session per scenario in a cookie, so re-clicking "Play"
 * resumes instead of silently starting over.
 */
@Controller
@RequestMapping("/game")
public class GamePlayController {

    private static final String SESSIONS_COOKIE = "ef_sessions";

    private final GameSessionPersistenceService gameService;
    private final ScenarioPersistenceService scenarioService;

    public GamePlayController(GameSessionPersistenceService gameService, ScenarioPersistenceService scenarioService) {
        this.gameService = gameService;
        this.scenarioService = scenarioService;
    }

    /** Generic entry point: start (or resume) a session for any scenario. */
    @GetMapping("/start/{scenarioId}")
    public String start(@PathVariable UUID scenarioId,
                        @CookieValue(value = SESSIONS_COOKIE, required = false) String sessionsCookie,
                        HttpServletResponse response) {
        scenarioService.load(scenarioId);

        Map<UUID, UUID> sessions = parseSessions(sessionsCookie);
        UUID existing = sessions.get(scenarioId);

        UUID sessionId = (existing != null && sessionStillValid(existing)) ? existing : null;
        if (sessionId == null) {
            sessionId = gameService.startSession(scenarioId);
            sessions.put(scenarioId, sessionId);
            response.addCookie(buildCookie(sessions));
        }

        return "redirect:/game/session/" + sessionId;
    }

    /** Kept for the nav bar's quick-play link; now just resolves the demo scenario and delegates. */
    @GetMapping("/zombie-shelter")
    public String startZombieShelter(@CookieValue(value = SESSIONS_COOKIE, required = false) String sessionsCookie,
                                     HttpServletResponse response) {
        var scenario = scenarioService.findAll().stream()
                .filter(s -> "Zombie Shelter".equals(s.name()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No 'Zombie Shelter' scenario found. Load it first (DataLoader with 'local' profile, or via /api/scenarios)."));

        return start(scenario.id(), sessionsCookie, response);
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

    private boolean sessionStillValid(UUID sessionId) {
        try {
            gameService.getSession(sessionId);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Map<UUID, UUID> parseSessions(String cookieValue) {
        Map<UUID, UUID> map = new LinkedHashMap<>();
        if (StringUtils.hasText(cookieValue)) {
            for (String pair : cookieValue.split("~")) {
                String[] parts = pair.split(":");
                if (parts.length == 2) {
                    try {
                        map.put(UUID.fromString(parts[0]), UUID.fromString(parts[1]));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
        return map;
    }

    private Cookie buildCookie(Map<UUID, UUID> sessions) {
        String value = sessions.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining("~"));
        Cookie cookie = new Cookie(SESSIONS_COOKIE, value);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30);
        return cookie;
    }
}