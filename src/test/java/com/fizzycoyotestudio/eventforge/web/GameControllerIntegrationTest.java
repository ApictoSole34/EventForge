package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.game.zombieshelter.ZombieShelterScenario;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@ActiveProfiles("test")
class GameControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ScenarioPersistenceService scenarioService;

    @Test
    void fullPlaythroughThroughRestApi() throws Exception {
        UUID scenarioId = scenarioService.save(
                "Zombie Shelter", "desc", "zombie-attack",
                ZombieShelterScenario.initialState(),
                ZombieShelterScenario.buildRegistry().getAll()
        );

        String startResponse = mockMvc.perform(post("/api/game/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scenarioId\": \"" + scenarioId + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String sessionId = JsonPath.read(startResponse, "$.sessionId");

        mockMvc.perform(post("/api/game/" + sessionId + "/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.ammo").value(11.0))
                .andExpect(jsonPath("$.eventId").value("zombie-attack-result"));

        mockMvc.perform(post("/api/game/" + sessionId + "/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.choices[0].id").value("push-back"));

        mockMvc.perform(post("/api/game/" + sessionId + "/choice")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"choiceId\": \"push-back\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.zombies").value(7.0))
                .andExpect(jsonPath("$.eventId").value("loot"));

        mockMvc.perform(post("/api/game/" + sessionId + "/event"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.food").value(52.0));
    }
}
