package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ScenarioController.class)
@Import(EventDtoMapper.class)
class ScenarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScenarioPersistenceService service;

    @Test
    void createScenario_returns201WithLocationAndBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.save(anyString(), any(), anyString(), any(), anyList())).thenReturn(id);

        String requestJson = """
            {
              "name": "Test Scenario",
              "description": "desc",
              "startEventId": "start",
              "initialState": {"food": 42},
              "events": [
                {
                  "id": "start",
                  "name": "Start Event",
                  "cooldownTicks": 0,
                  "nextEventPool": [],
                  "actions": [
                    {"type": "MODIFY_RESOURCE", "variable": "food", "amount": -1}
                  ],
                  "choices": []
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/scenarios/" + id))
                .andExpect(jsonPath("$.name").value("Test Scenario"))
                .andExpect(jsonPath("$.events[0].actions[0].variable").value("food"));
    }

    @Test
    void createScenario_missingName_returns400() throws Exception {
        String requestJson = """
                {
                  "startEventId": "start",
                  "events": [
                    {
                      "id": "start",
                      "name": "x",
                      "cooldownTicks": 0,
                      "nextEventPool": [],
                      "actions": [],
                      "choices": []
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/scenarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getScenario_unknownId_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.load(id)).thenThrow(new IllegalArgumentException("No scenario with id " + id));

        mockMvc.perform(get("/api/scenarios/" + id))
                .andExpect(status().isNotFound());
    }
}