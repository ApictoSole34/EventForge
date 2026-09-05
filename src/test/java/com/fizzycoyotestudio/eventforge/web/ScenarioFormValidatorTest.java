package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.web.dto.ScenarioFormData;
import com.fizzycoyotestudio.eventforge.web.dto.StateVariableForm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioFormValidatorTest {

    private final ScenarioFormValidator validator = new ScenarioFormValidator();

    private ScenarioFormData validForm() {
        ScenarioFormData form = new ScenarioFormData();
        form.setName("Zombie Shelter");
        form.setStartEventId("zombie-attack");
        form.setStartEventName("Zombie Attack");
        return form;
    }

    @Test
    void validFormProducesNoErrors() {
        assertThat(validator.validate(validForm())).isEmpty();
    }

    @Test
    void blankNameIsRejected() {
        ScenarioFormData form = validForm();
        form.setName(" ");
        assertThat(validator.validate(form)).anyMatch(e -> e.contains("Scenario name is required"));
    }

    @Test
    void blankStartEventIdIsRejected() {
        ScenarioFormData form = validForm();
        form.setStartEventId("");
        assertThat(validator.validate(form)).anyMatch(e -> e.contains("Start event id is required"));
    }

    @Test
    void startEventIdWithInvalidCharactersIsRejected() {
        ScenarioFormData form = validForm();
        form.setStartEventId("zombie attack!");
        assertThat(validator.validate(form))
                .anyMatch(e -> e.contains("letters, numbers, hyphens and underscores"));
    }

    @Test
    void blankStartEventNameIsRejected() {
        ScenarioFormData form = validForm();
        form.setStartEventName(" ");
        assertThat(validator.validate(form)).anyMatch(e -> e.contains("Start event name is required"));
    }

    @Test
    void fullyEmptyInitialStateRowIsSilentlyIgnored() {
        ScenarioFormData form = validForm();
        form.setInitialState(new ArrayList<>(List.of(new StateVariableForm())));
        assertThat(validator.validate(form)).isEmpty();
    }

    @Test
    void initialStateRowMissingVariableIsRejected() {
        ScenarioFormData form = validForm();
        StateVariableForm row = new StateVariableForm();
        row.setValue(10.0);
        form.setInitialState(new ArrayList<>(List.of(row)));

        assertThat(validator.validate(form)).anyMatch(e -> e.contains("variable name is required"));
    }

    @Test
    void initialStateRowMissingValueIsRejected() {
        ScenarioFormData form = validForm();
        StateVariableForm row = new StateVariableForm();
        row.setVariable("food");
        form.setInitialState(new ArrayList<>(List.of(row)));

        assertThat(validator.validate(form)).anyMatch(e -> e.contains("value is required"));
    }

    @Test
    void duplicateInitialStateVariablesAreRejected() {
        ScenarioFormData form = validForm();
        StateVariableForm food1 = new StateVariableForm();
        food1.setVariable("food");
        food1.setValue(10.0);
        StateVariableForm food2 = new StateVariableForm();
        food2.setVariable("food");
        food2.setValue(20.0);
        form.setInitialState(new ArrayList<>(List.of(food1, food2)));

        assertThat(validator.validate(form)).anyMatch(e -> e.contains("duplicate variable 'food'"));
    }

    @Test
    void distinctInitialStateVariablesAreAllowed() {
        ScenarioFormData form = validForm();
        StateVariableForm food = new StateVariableForm();
        food.setVariable("food");
        food.setValue(10.0);
        StateVariableForm water = new StateVariableForm();
        water.setVariable("water");
        water.setValue(5.0);
        form.setInitialState(new ArrayList<>(List.of(food, water)));

        assertThat(validator.validate(form)).isEmpty();
    }
}
