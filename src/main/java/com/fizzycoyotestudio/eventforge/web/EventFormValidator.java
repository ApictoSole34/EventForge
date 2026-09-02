package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.web.dto.ChoiceFormData;
import com.fizzycoyotestudio.eventforge.web.dto.EventFormData;
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

        validateNextEventId(form.getNextEventId(), form.getId(), existingEventIds, "Next Event", errors);

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

            validateNextEventId(choice.getNextEventId(), form.getId(), existingEventIds, label + " next event", errors);
        }

        return errors;
    }

    private void validateNextEventId(String nextEventId, String ownId, List<String> existingEventIds,
                                     String fieldLabel, List<String> errors) {
        if (!StringUtils.hasText(nextEventId)) return;
        boolean pointsToSelf = nextEventId.equals(ownId);
        if (!pointsToSelf && !existingEventIds.contains(nextEventId)) {
            errors.add(fieldLabel + " '" + nextEventId + "' does not exist in this scenario.");
        }
    }
}
