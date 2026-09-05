package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import com.fizzycoyotestudio.eventforge.web.dto.ScenarioFormData;
import com.fizzycoyotestudio.eventforge.web.dto.StateVariableForm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioFormMapperTest {

    private final ScenarioFormMapper mapper = new ScenarioFormMapper();

    @Test
    void toInitialStateMapsCompleteRows() {
        StateVariableForm food = new StateVariableForm();
        food.setVariable("food");
        food.setValue(42.0);
        StateVariableForm water = new StateVariableForm();
        water.setVariable("water");
        water.setValue(31.0);

        GameState state = mapper.toInitialState(List.of(food, water));

        assertThat(state.get("food")).isEqualTo(42.0);
        assertThat(state.get("water")).isEqualTo(31.0);
    }

    @Test
    void toInitialStateTrimsVariableNames() {
        StateVariableForm row = new StateVariableForm();
        row.setVariable("  food  ");
        row.setValue(1.0);

        GameState state = mapper.toInitialState(List.of(row));

        assertThat(state.has("food")).isTrue();
        assertThat(state.has("  food  ")).isFalse();
    }

    @Test
    void toInitialStateSkipsIncompleteRows() {
        StateVariableForm noValue = new StateVariableForm();
        noValue.setVariable("food");
        StateVariableForm noVariable = new StateVariableForm();
        noVariable.setValue(5.0);

        GameState state = mapper.toInitialState(new ArrayList<>(List.of(noValue, noVariable)));

        assertThat(state.asMap()).isEmpty();
    }

    @Test
    void toStartEventBuildsBareEventWithNoConditionActionsOrChoices() {
        ScenarioFormData form = new ScenarioFormData();
        form.setStartEventId("zombie-attack");
        form.setStartEventName("Zombie Attack");
        form.setStartEventDescription("A horde approaches");

        Event event = mapper.toStartEvent(form);

        assertThat(event.getId()).isEqualTo("zombie-attack");
        assertThat(event.getName()).isEqualTo("Zombie Attack");
        assertThat(event.getDescription()).isEqualTo("A horde approaches");
        assertThat(event.getActions()).isEmpty();
        assertThat(event.hasChoices()).isFalse();
        assertThat(event.getNextEventId()).isNull();
        assertThat(event.canTrigger(new GameState())).isTrue();
    }
}
