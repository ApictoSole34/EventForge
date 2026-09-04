package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.web.dto.ChoiceFormData;
import com.fizzycoyotestudio.eventforge.web.dto.ConditionRowForm;
import com.fizzycoyotestudio.eventforge.web.dto.EventFormData;
import com.fizzycoyotestudio.eventforge.web.dto.PoolEntryForm;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class EventFormValidator {

    public List<String> validate(EventFormData form, boolean isEdit, List<String> existingEventIds) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(form.getId())) {
            errors.add("Event ID is required.");
        } else if (!form.getId().matches("[a-zA-Z0-9_-]+")) {
            errors.add("Event ID may only contain letters, numbers, hyphens and underscores.");
        } else if (!isEdit && existingEventIds.contains(form.getId())) {
            errors.add("An event with id '" + form.getId() + "' already exists in this scenario.");
        }

        if (!StringUtils.hasText(form.getName())) {
            errors.add("Name is required.");
        }

        if (form.getCooldownTicks() != null && form.getCooldownTicks() < 0) {
            errors.add("Cooldown must be zero or greater.");
        }

        validateConditions(form.getConditions(), "Condition", errors);
        validateNextEventId(form.getNextEventId(), form.getId(), existingEventIds, "Next Event", errors);
        validatePool(form.getNextEventPool(), form.getId(), existingEventIds, "Random next-event pool", errors);

        Set<String> seenChoiceIds = new HashSet<>();
        List<ChoiceFormData> choices = form.getChoices();
        for (int i = 0; i < choices.size(); i++) {
            ChoiceFormData choice = choices.get(i);
            String label = "Choice #" + (i + 1);

            if (!StringUtils.hasText(choice.getId())) {
                errors.add(label + ": id is required.");
            } else if (!seenChoiceIds.add(choice.getId())) {
                errors.add(label + ": duplicate choice id '" + choice.getId() + "' within this event.");
            }

            if (!StringUtils.hasText(choice.getLabel())) {
                errors.add(label + ": label is required.");
            }

            validateConditions(choice.getConditions(), label + " condition", errors);
            validateNextEventId(choice.getNextEventId(), form.getId(), existingEventIds, label + " next event", errors);
            validatePool(choice.getNextEventPool(), form.getId(), existingEventIds, label + " random next-event pool", errors);
        }

        return errors;
    }

    /**
     * Validates condition rows. Empty rows (e.g. an "Add Condition" the user didn't fill in) are ignored silently.
     */
    private void validateConditions(List<ConditionRowForm> rows, String fieldLabel, List<String> errors) {
        for (int i = 0; i < rows.size(); i++) {
            ConditionRowForm row = rows.get(i);
            boolean hasVar = StringUtils.hasText(row.getVariable());
            boolean hasOp = StringUtils.hasText(row.getOperator());
            boolean hasVal = row.getValue() != null;

            if (!hasVar && !hasOp && !hasVal) continue;

            if (!hasVar) errors.add(fieldLabel + " row #" + (i + 1) + ": variable is required.");
            if (!hasOp) errors.add(fieldLabel + " row #" + (i + 1) + ": operator is required.");
            if (!hasVal) errors.add(fieldLabel + " row #" + (i + 1) + ": value is required.");
        }
    }

    private void validateNextEventId(String nextEventId, String ownId, List<String> existingEventIds,
                                     String fieldLabel, List<String> errors) {
        if (!StringUtils.hasText(nextEventId)) return;
        boolean pointsToSelf = nextEventId.equals(ownId);
        if (!pointsToSelf && !existingEventIds.contains(nextEventId)) {
            errors.add(fieldLabel + " '" + nextEventId + "' does not exist in this scenario.");
        }
    }

    /**
     * Validates weighted pool entries. Blank/fully-empty rows are ignored silently.
     */
    private void validatePool(List<PoolEntryForm> pool, String ownId, List<String> existingEventIds,
                              String fieldLabel, List<String> errors) {
        for (int i = 0; i < pool.size(); i++) {
            PoolEntryForm row = pool.get(i);
            boolean hasId = StringUtils.hasText(row.getEventId());
            boolean hasWeight = row.getWeight() != null;

            if (!hasId && !hasWeight) {
                continue;
            }

            if (!hasId) {
                errors.add(fieldLabel + " row #" + (i + 1) + ": event id is required.");
            } else {
                boolean pointsToSelf = row.getEventId().equals(ownId);
                if (!pointsToSelf && !existingEventIds.contains(row.getEventId())) {
                    errors.add(fieldLabel + " row #" + (i + 1) + ": '" + row.getEventId() + "' does not exist in this scenario.");
                }
            }

            if (!hasWeight) {
                errors.add(fieldLabel + " row #" + (i + 1) + ": weight is required.");
            } else if (row.getWeight() <= 0) {
                errors.add(fieldLabel + " row #" + (i + 1) + ": weight must be greater than zero.");
            }
        }
    }
}