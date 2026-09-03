package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.EventRegistry;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventBuilderController.class)
@Import({EventFormMapper.class, EventFormValidator.class})
class EventBuilderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScenarioPersistenceService scenarioService;

    @Test
    void createWithMissingIdReturnsFormWithErrors() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        var loaded = new ScenarioPersistenceService.LoadedScenario(
                scenarioId, "Test Scenario", "desc", "start", new GameState(), new EventRegistry(Map.of()));
        when(scenarioService.load(scenarioId)).thenReturn(loaded);

        mockMvc.perform(post("/scenarios/" + scenarioId + "/events")
                        .param("name", "Some Event")) // id intentionally missing
                .andExpect(status().isOk())
                .andExpect(view().name("event-form"))
                .andExpect(model().attributeExists("errors"));

        verify(scenarioService, never()).saveEvent(any(), any());
    }

    @Test
    void createWithValidDataSavesAndRedirects() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        var loaded = new ScenarioPersistenceService.LoadedScenario(
                scenarioId, "Test Scenario", "desc", "start", new GameState(), new EventRegistry(Map.of()));
        when(scenarioService.load(scenarioId)).thenReturn(loaded);

        mockMvc.perform(post("/scenarios/" + scenarioId + "/events")
                        .param("id", "loot")
                        .param("name", "Loot"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/scenarios/" + scenarioId));

        verify(scenarioService).saveEvent(eq(scenarioId), any());
    }

    @Test
    void createWithNonexistentNextEventIdReturnsFormWithErrors() throws Exception {
        UUID scenarioId = UUID.randomUUID();
        var loaded = new ScenarioPersistenceService.LoadedScenario(
                scenarioId, "Test Scenario", "desc", "start", new GameState(), new EventRegistry(Map.of()));
        when(scenarioService.load(scenarioId)).thenReturn(loaded);

        mockMvc.perform(post("/scenarios/" + scenarioId + "/events")
                        .param("id", "loot")
                        .param("name", "Loot")
                        .param("nextEventId", "ghost-event"))
                .andExpect(status().isOk())
                .andExpect(view().name("event-form"))
                .andExpect(model().attribute("errors", org.hamcrest.Matchers.hasItem(
                        org.hamcrest.Matchers.containsString("does not exist"))));

        verify(scenarioService, never()).saveEvent(any(), any());
    }
}
