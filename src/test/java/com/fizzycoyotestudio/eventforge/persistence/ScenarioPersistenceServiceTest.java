package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.*;
import com.fizzycoyotestudio.eventforge.game.zombieshelter.ZombieShelterScenario;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@ActiveProfiles("test")
class ScenarioPersistenceServiceTest {

    @Autowired
    private ScenarioPersistenceService service;

    @Test
    void savingAndLoadingZombieShelterScenarioRoundTrips() {
        EventRegistry originalRegistry = ZombieShelterScenario.buildRegistry();

        UUID id = service.save(
                "Zombie Shelter Demo",
                "The default zombie shelter scenario",
                "zombie-attack",
                originalRegistry.getAll()
        );

        ScenarioPersistenceService.LoadedScenario loaded = service.load(id);

        assertThat(loaded.name()).isEqualTo("Zombie Shelter Demo");
        assertThat(loaded.startEventId()).isEqualTo("zombie-attack");

        EventEngine engine = new EventEngine();
        GameState state = ZombieShelterScenario.initialState();
        GameSession session = new GameSession(engine, loaded.registry(), state, loaded.startEventId());

        session.triggerCurrentEvent();
        assertThat(state.get("ammo")).isEqualTo(11);

        session.triggerCurrentEvent();
        session.choose("push-back");
        assertThat(state.get("zombies")).isEqualTo(7);
    }
}