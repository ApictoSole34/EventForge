package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.web.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventFormValidatorTest {

    private final EventFormValidator validator = new EventFormValidator();

    private EventFormData validForm() {
        EventFormData form = new EventFormData();
        form.setId("zombie-attack");
        form.setName("Zombie Attack");
        return form;
    }

    @Test
    void validFormProducesNoErrors() {
        List<String> errors = validator.validate(validForm(), false, List.of());
        assertThat(errors).isEmpty();
    }

    @Test
    void blankIdIsRejected() {
        EventFormData form = validForm();
        form.setId("  ");
        assertThat(validator.validate(form, false, List.of()))
                .anyMatch(e -> e.contains("Event ID is required"));
    }

    @Test
    void idWithIllegalCharactersIsRejected() {
        EventFormData form = validForm();
        form.setId("zombie attack!");
        assertThat(validator.validate(form, false, List.of()))
                .anyMatch(e -> e.contains("letters, numbers, hyphens and underscores"));
    }

    @Test
    void idsWithHyphensAndUnderscoresAreAllowed() {
        EventFormData form = validForm();
        form.setId("zombie-attack_2");
        assertThat(validator.validate(form, false, List.of())).isEmpty();
    }

    @Test
    void duplicateIdRejectedOnlyWhenCreating() {
        EventFormData form = validForm();
        form.setId("loot");

        assertThat(validator.validate(form, false, List.of("loot")))
                .anyMatch(e -> e.contains("already exists"));
        assertThat(validator.validate(form, true, List.of("loot"))).isEmpty();
    }

    @Test
    void blankNameIsRejected() {
        EventFormData form = validForm();
        form.setName(" ");
        assertThat(validator.validate(form, false, List.of()))
                .anyMatch(e -> e.contains("Name is required"));
    }

    @Test
    void negativeCooldownIsRejected() {
        EventFormData form = validForm();
        form.setCooldownTicks(-1);
        assertThat(validator.validate(form, false, List.of()))
                .anyMatch(e -> e.contains("Cooldown must be zero or greater"));
    }

    @Test
    void zeroCooldownIsAllowed() {
        EventFormData form = validForm();
        form.setCooldownTicks(0);
        assertThat(validator.validate(form, false, List.of())).isEmpty();
    }

    @Test
    void fullyEmptyConditionRowIsSilentlyIgnored() {
        EventFormData form = validForm();
        ConditionRowForm empty = new ConditionRowForm();
        form.setConditions(new ArrayList<>(List.of(empty)));

        assertThat(validator.validate(form, false, List.of())).isEmpty();
    }

    @Test
    void partiallyFilledConditionRowReportsEachMissingField() {
        EventFormData form = validForm();
        ConditionRowForm partial = new ConditionRowForm();
        partial.setVariable("zombies");
        form.setConditions(new ArrayList<>(List.of(partial)));

        List<String> errors = validator.validate(form, false, List.of());
        assertThat(errors).anyMatch(e -> e.contains("operator is required"));
        assertThat(errors).anyMatch(e -> e.contains("value is required"));
        assertThat(errors).noneMatch(e -> e.contains("variable is required"));
    }

    @Test
    void nextEventPointingToNonexistentEventIsRejected() {
        EventFormData form = validForm();
        form.setNextEventId("does-not-exist");

        assertThat(validator.validate(form, false, List.of("loot")))
                .anyMatch(e -> e.contains("Next Event 'does-not-exist' does not exist"));
    }

    @Test
    void nextEventPointingToSelfIsAllowed() {
        EventFormData form = validForm();
        form.setNextEventId("zombie-attack");

        assertThat(validator.validate(form, true, List.of())).isEmpty();
    }

    @Test
    void nextEventPointingToKnownExistingEventIsAllowed() {
        EventFormData form = validForm();
        form.setNextEventId("loot");

        assertThat(validator.validate(form, false, List.of("loot"))).isEmpty();
    }

    @Test
    void blankNextEventIdIsAllowedAsTerminal() {
        EventFormData form = validForm();
        form.setNextEventId(null);

        assertThat(validator.validate(form, false, List.of())).isEmpty();
    }

    @Test
    void fullyEmptyPoolRowIsSilentlyIgnored() {
        EventFormData form = validForm();
        form.setNextEventPool(new ArrayList<>(List.of(new PoolEntryForm())));

        assertThat(validator.validate(form, false, List.of())).isEmpty();
    }

    @Test
    void poolRowMissingWeightIsRejected() {
        EventFormData form = validForm();
        PoolEntryForm row = new PoolEntryForm();
        row.setEventId("loot");
        form.setNextEventPool(new ArrayList<>(List.of(row)));

        assertThat(validator.validate(form, false, List.of("loot")))
                .anyMatch(e -> e.contains("weight is required"));
    }

    @Test
    void poolRowWithZeroOrNegativeWeightIsRejected() {
        EventFormData form = validForm();
        PoolEntryForm row = new PoolEntryForm();
        row.setEventId("loot");
        row.setWeight(0.0);
        form.setNextEventPool(new ArrayList<>(List.of(row)));

        assertThat(validator.validate(form, false, List.of("loot")))
                .anyMatch(e -> e.contains("weight must be greater than zero"));
    }

    @Test
    void poolRowPointingToUnknownEventIsRejected() {
        EventFormData form = validForm();
        PoolEntryForm row = new PoolEntryForm();
        row.setEventId("ghost");
        row.setWeight(1.0);
        form.setNextEventPool(new ArrayList<>(List.of(row)));

        assertThat(validator.validate(form, false, List.of("loot")))
                .anyMatch(e -> e.contains("'ghost' does not exist"));
    }

    @Test
    void choiceMissingIdOrLabelIsRejected() {
        EventFormData form = validForm();
        ChoiceFormData choice = new ChoiceFormData();
        form.setChoices(new ArrayList<>(List.of(choice)));

        List<String> errors = validator.validate(form, false, List.of());
        assertThat(errors).anyMatch(e -> e.contains("Choice #1: id is required"));
        assertThat(errors).anyMatch(e -> e.contains("Choice #1: label is required"));
    }

    @Test
    void duplicateChoiceIdsWithinSameEventAreRejected() {
        EventFormData form = validForm();
        ChoiceFormData c1 = new ChoiceFormData();
        c1.setId("push-back");
        c1.setLabel("Push Back");
        ChoiceFormData c2 = new ChoiceFormData();
        c2.setId("push-back");
        c2.setLabel("Push Back Again");
        form.setChoices(new ArrayList<>(List.of(c1, c2)));

        assertThat(validator.validate(form, false, List.of()))
                .anyMatch(e -> e.contains("duplicate choice id 'push-back'"));
    }

    @Test
    void validChoiceWithNextEventProducesNoErrors() {
        EventFormData form = validForm();
        ChoiceFormData choice = new ChoiceFormData();
        choice.setId("push-back");
        choice.setLabel("Push Back");
        choice.setNextEventId("loot");
        form.setChoices(new ArrayList<>(List.of(choice)));

        assertThat(validator.validate(form, false, List.of("loot"))).isEmpty();
    }
}
