package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameSessionPersistenceServiceTest {

    @Mock
    private GameSessionRepository repository;
    @Mock
    private ScenarioPersistenceService scenarioService;

    private GameSessionPersistenceService service;
    private GameSessionEntity entity;
    private final UUID sessionId = UUID.randomUUID();
    private final UUID scenarioId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GameSessionPersistenceService(repository, scenarioService, new GameStateJsonMapper());

        entity = new GameSessionEntity();
        entity.setId(sessionId);
        entity.setScenarioId(scenarioId);
        entity.setCurrentEventBusinessId("zombie-attack");
        entity.setStateJson("{\"zombies\":12.0,\"ammo\":13.0}");
        entity.setTriggered(false);
        entity.setTerminal(false);

        lenient().when(repository.findById(sessionId)).thenReturn(Optional.of(entity));

        Event zombieAttack = Event.builder()
                .id("zombie-attack")
                .name("Zombie Attack")
                .condition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 0))
                .actions(List.of(new ModifyResourceAction("ammo", -2)))
                .nextEventId("loot")
                .build();
        Event loot = Event.builder().id("loot").name("Loot").build();

        EventRegistry registry = new EventRegistry(Map.of("zombie-attack", zombieAttack, "loot", loot));
        var loadedScenario = new ScenarioPersistenceService.LoadedScenario(
                scenarioId, "Zombie Shelter", "desc", "zombie-attack", new GameState(), registry);
        lenient().when(scenarioService.load(scenarioId)).thenReturn(loadedScenario);
    }

    @Test
    void triggerCurrentEvent_appliesActionsAndAdvancesWhenNoChoices() {
        GameSessionPersistenceService.GameSessionView view = service.triggerCurrentEvent(sessionId);

        assertThat(view.state().get("ammo")).isEqualTo(11.0);
        assertThat(view.eventId()).isEqualTo("loot");
        assertThat(view.terminal()).isFalse();
    }

    @Test
    void triggerCurrentEvent_isIdempotent_doesNotReapplyActionsIfCalledTwice() {
        service.triggerCurrentEvent(sessionId);
        entity.setTriggered(true);
        entity.setCurrentEventBusinessId("zombie-attack");

        GameSessionPersistenceService.GameSessionView view = service.triggerCurrentEvent(sessionId);
        assertThat(view.state().get("ammo")).isEqualTo(11.0);
    }

    @Test
    void triggerCurrentEvent_marksTerminalWhenConditionFails() {
        entity.setStateJson("{\"zombies\":0.0,\"ammo\":13.0}");

        GameSessionPersistenceService.GameSessionView view = service.triggerCurrentEvent(sessionId);

        assertThat(view.terminal()).isTrue();
        assertThat(view.state().get("ammo")).isEqualTo(13.0);
    }

    @Test
    void choose_throwsIllegalState_whenCurrentEventNotYetTriggered() {
        assertThatThrownBy(() -> service.choose(sessionId, "some-choice"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No choice is currently pending");
    }
}