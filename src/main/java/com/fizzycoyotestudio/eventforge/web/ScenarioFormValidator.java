package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.web.dto.ScenarioFormData;
import com.fizzycoyotestudio.eventforge.web.dto.StateVariableForm;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class ScenarioFormValidator {

    public List<String> validate(ScenarioFormData form) {
        List<String> errors = new ArrayList<>();

        if (!StringUtils.hasText(form.getName())) {
            errors.add("Scenario name is required.");
        }

        if (!StringUtils.hasText(form.getStartEventId())) {
            errors.add("Start event id is required.");
        } else if (!form.getStartEventId().matches("[a-zA-Z0-9_-]+")) {
            errors.add("Start event id may only contain letters, numbers, hyphens and underscores.");
        }

        if (!StringUtils.hasText(form.getStartEventName())) {
            errors.add("Start event name is required.");
        }

        Set<String> seenVariables = new HashSet<>();
        List<StateVariableForm> state = form.getInitialState();
        for (int i = 0; i < state.size(); i++) {
            StateVariableForm row = state.get(i);
            boolean hasVar = StringUtils.hasText(row.getVariable());
            boolean hasVal = row.getValue() != null;

            if (!hasVar && !hasVal) {
                continue; // fully empty row (e.g. an "Add Variable" the user didn't fill in) — ignore silently
            }
            if (!hasVar) {
                errors.add("Initial state row #" + (i + 1) + ": variable name is required.");
            } else if (!seenVariables.add(row.getVariable().trim())) {
                errors.add("Initial state row #" + (i + 1) + ": duplicate variable '" + row.getVariable() + "'.");
            }
            if (!hasVal) {
                errors.add("Initial state row #" + (i + 1) + ": value is required.");
            }
        }

        return errors;
    }
}