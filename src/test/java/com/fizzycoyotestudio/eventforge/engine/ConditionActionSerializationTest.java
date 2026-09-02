package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ConditionActionSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void comparisonConditionRoundTrips() throws Exception {
        Condition original = new ComparisonCondition("zombies", Operator.GREATER_THAN, 10.0);

        String json = mapper.writeValueAsString(original);
        Condition restored = mapper.readValue(json, Condition.class);

        assertThat(restored).isInstanceOf(ComparisonCondition.class);
        assertThat(json).contains("\"type\":\"COMPARISON\"");

        GameState state = new GameState();
        state.set("zombies", 12.0);
        assertThat(restored.evaluate(state)).isTrue();
    }

    @Test
    void nestedAndConditionRoundTrips() throws Exception {
        Condition original = new AndCondition(List.of(
                new ComparisonCondition("food", Operator.GREATER_THAN, 20.0),
                new ComparisonCondition("survivors", Operator.GREATER_THAN_OR_EQUAL, 3.0)
        ));

        String json = mapper.writeValueAsString(original);
        Condition restored = mapper.readValue(json, Condition.class);

        assertThat(restored).isInstanceOf(AndCondition.class);

        GameState state = new GameState();
        state.set("food", 42.0);
        state.set("survivors", 6.0);
        assertThat(restored.evaluate(state)).isTrue();
    }

    @Test
    void modifyResourceActionRoundTrips() throws Exception {
        GameAction original = new ModifyResourceAction("ammo", -2.0);

        String json = mapper.writeValueAsString(original);
        GameAction restored = mapper.readValue(json, GameAction.class);

        assertThat(restored).isInstanceOf(ModifyResourceAction.class);
        assertThat(json).contains("\"type\":\"MODIFY_RESOURCE\"");

        GameState state = new GameState();
        state.set("ammo", 13.0);
        restored.execute(state);
        assertThat(state.get("ammo")).isEqualTo(11.0);
    }
}
