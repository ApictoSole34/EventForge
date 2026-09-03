package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.web.dto.ChoiceFormData;
import com.fizzycoyotestudio.eventforge.web.dto.EventFormData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventFormValidatorTest {

    private final EventFormValidator validator = new EventFormValidator();

    @Test
    void validEventProducesNoErrors() {
        EventFormData form = new EventFormData();
        form.setId("loot");
        form.setName("Loot");
        form.setNextEventId("day-summary");

        List<String> errors = validator.validate(form, false, List.of("day-summary"));

        assertThat(errors).isEmpty();
    }

    @Test
    void blankIdIsRejected() {
        EventFormData form = new EventFormData();
        form.setName("Something");

        List<String> errors = validator.validate(form, false, List.of());

        assertThat(errors).anyMatch(e -> e.contains("Event ID is required"));
    }

    @Test
    void idWithInvalidCharactersIsRejected() {
        EventFormData form = new EventFormData();
        form.setId("zombie attack!");
        form.setName("Zombie Attack");

        List<String> errors = validator.validate(form, false, List.of());

        assertThat(errors).anyMatch(e -> e.contains("may only contain"));
    }

    @Test
    void duplicateIdRejectedOnCreateButAllowedOnEdit() {
        EventFormData form = new EventFormData();
        form.setId("zombie-attack");
        form.setName("Zombie Attack");

        List<String> createErrors = validator.validate(form, false, List.of("zombie-attack"));
        List<String> editErrors = validator.validate(form, true, List.of("zombie-attack"));

        assertThat(createErrors).anyMatch(e -> e.contains("already exists"));
        assertThat(editErrors).isEmpty();
    }

    @Test
    void nextEventIdPointingToNonexistentEventIsRejected() {
        EventFormData form = new EventFormData();
        form.setId("loot");
        form.setName("Loot");
        form.setNextEventId("does-not-exist");

        List<String> errors = validator.validate(form, false, List.of("zombie-attack"));

        assertThat(errors).anyMatch(e -> e.contains("does not exist"));
    }

    @Test
    void nextEventIdPointingToSelfIsAllowed() {
        EventFormData form = new EventFormData();
        form.setId("wait");
        form.setName("Wait");
        form.setNextEventId("wait");

        List<String> errors = validator.validate(form, true, List.of("wait"));

        assertThat(errors).isEmpty();
    }

    @Test
    void duplicateChoiceIdsWithinSameEventAreRejected() {
        EventFormData form = new EventFormData();
        form.setId("gate");
        form.setName("Stranger at the Gate");

        ChoiceFormData choice1 = new ChoiceFormData();
        choice1.setId("let-in");
        choice1.setLabel("Let him in");

        ChoiceFormData choice2 = new ChoiceFormData();
        choice2.setId("let-in"); // duplicate
        choice2.setLabel("Also let him in");

        form.setChoices(List.of(choice1, choice2));

        List<String> errors = validator.validate(form, false, List.of());

        assertThat(errors).anyMatch(e -> e.contains("duplicate choice id"));
    }

    @Test
    void choiceMissingLabelIsRejected() {
        EventFormData form = new EventFormData();
        form.setId("gate");
        form.setName("Gate");

        ChoiceFormData choice = new ChoiceFormData();
        choice.setId("refuse");

        form.setChoices(List.of(choice));

        List<String> errors = validator.validate(form, false, List.of());

        assertThat(errors).anyMatch(e -> e.contains("label is required"));
    }
}
