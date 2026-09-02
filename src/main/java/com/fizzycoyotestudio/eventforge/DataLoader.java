package com.fizzycoyotestudio.eventforge;


import com.fizzycoyotestudio.eventforge.game.zombieshelter.ZombieShelterScenario;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class DataLoader implements CommandLineRunner {

    private final ScenarioPersistenceService scenarioService;

    public DataLoader(ScenarioPersistenceService scenarioService) {
        this.scenarioService = scenarioService;
    }

    @Override
    public void run(String... args) {
        var scenarios = scenarioService.findAll();
        if (scenarios.isEmpty()) {
            var registry = ZombieShelterScenario.buildRegistry();
            var initialState = ZombieShelterScenario.initialState();
            scenarioService.save(
                    "Zombie Shelter",
                    "Survive the zombie apocalypse",
                    "zombie-attack",
                    initialState,
                    registry.getAll()
            );
            System.out.println("✅ Loaded Zombie Shelter scenario into database.");
        } else {
            System.out.println("ℹ️ Scenarios already exist – skipping data load.");
        }
    }
}