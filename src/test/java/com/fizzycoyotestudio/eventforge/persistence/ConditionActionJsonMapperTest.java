package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain unit test (no Spring context needed) proving that
 * ConditionActionJsonMapper round-trips a HETEROGENEOUS list of actions —
 * this is exactly the case that silently lost its "type" discriminator
 * before the writerFor(TypeReference) fix.
 */
class ConditionActionJsonMapperTest {

    private final ConditionActionJsonMapper mapper = new ConditionActionJsonMapper();

    @Test
    void actionsListRoundTripsWithTypeDiscriminatorPreserved() {
        List<GameAction> original = List.of(
                new ModifyResourceAction("ammo", -2),
                new SetResourceAction("morale", 0)
        );

        String json = mapper.writeActions(original);
        assertThat(json).contains("\"type\":\"MODIFY_RESOURCE\"");
        assertThat(json).contains("\"type\":\"SET_RESOURCE\"");

        List<GameAction> restored = mapper.readActions(json);

        assertThat(restored).hasSize(2);
        assertThat(restored.get(0)).isInstanceOf(ModifyResourceAction.class);
        assertThat(restored.get(1)).isInstanceOf(SetResourceAction.class);

        GameState state = new GameState();
        state.set("ammo", 13);
        state.set("morale", 67);
        restored.forEach(action -> action.execute(state));

        assertThat(state.get("ammo")).isEqualTo(11);
        assertThat(state.get("morale")).isEqualTo(0);
    }
}