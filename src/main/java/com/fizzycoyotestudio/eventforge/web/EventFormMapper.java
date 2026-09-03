package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.*;
import com.fizzycoyotestudio.eventforge.web.dto.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Consumer;

@Component
public class EventFormMapper {

    public Event toDomain(EventFormData form) {
        return Event.builder()
                .id(form.getId())
                .name(form.getName())
                .description(form.getDescription())
                .condition(toCondition(form.getConditionVariable(), form.getConditionOperator(), form.getConditionValue()))
                .actions(toActions(form.getActions()))
                .choices(form.getChoices().stream().map(this::toDomain).toList())
                .nextEventId(blankToNull(form.getNextEventId()))
                .cooldownTicks(form.getCooldownTicks() != null ? form.getCooldownTicks() : 0)
                .nextEventPool(toPool(form.getNextEventPool()))
                .build();
    }

    public Choice toDomain(ChoiceFormData form) {
        return Choice.builder()
                .id(form.getId())
                .label(form.getLabel())
                .description(form.getDescription())
                .condition(toCondition(form.getConditionVariable(), form.getConditionOperator(), form.getConditionValue()))
                .actions(toActions(form.getActions()))
                .nextEventId(blankToNull(form.getNextEventId()))
                .nextEventPool(toPool(form.getNextEventPool()))
                .build();
    }

    /** Pre-fills a form for editing. AND/OR/NOT conditions aren't representable here — left blank (author them via the API/JSON, as noted in the UI). */
    public EventFormData toFormData(Event event) {
        EventFormData form = new EventFormData();
        form.setId(event.getId());
        form.setName(event.getName());
        form.setDescription(event.getDescription());
        fillCondition(event.getCondition(), form::setConditionVariable, form::setConditionOperator, form::setConditionValue);
        form.setActions(toActionRows(event.getActions()));
        form.setChoices(event.getChoices().stream().map(this::toFormData).toList());
        form.setNextEventId(event.getNextEventId());
        form.setCooldownTicks(event.getCooldownTicks());
        form.setNextEventPool(toPoolRows(event.getNextEventPool()));
        return form;
    }

    public ChoiceFormData toFormData(Choice choice) {
        ChoiceFormData form = new ChoiceFormData();
        form.setId(choice.getId());
        form.setLabel(choice.getLabel());
        form.setDescription(choice.getDescription());
        fillCondition(choice.getCondition(), form::setConditionVariable, form::setConditionOperator, form::setConditionValue);
        form.setActions(toActionRows(choice.getActions()));
        form.setNextEventId(choice.getNextEventId());
        form.setNextEventPool(toPoolRows(choice.getNextEventPool()));
        return form;
    }

    private Condition toCondition(String variable, String operator, Double value) {
        if (!StringUtils.hasText(variable) || !StringUtils.hasText(operator) || value == null) {
            return Condition.alwaysTrue();
        }
        return new ComparisonCondition(variable, Operator.valueOf(operator), value);
    }

    private void fillCondition(Condition condition, Consumer<String> setVar, Consumer<String> setOp, Consumer<Double> setVal) {
        if (condition instanceof ComparisonCondition c) {
            setVar.accept(c.getVariable());
            setOp.accept(c.getOperator().name());
            setVal.accept(c.getValue());
        }
    }

    private List<GameAction> toActions(List<ActionRowForm> rows) {
        return rows.stream()
                .filter(r -> StringUtils.hasText(r.getVariable()) && r.getAmount() != null)
                .map(r -> (GameAction) ("SET_RESOURCE".equals(r.getActionType())
                        ? new SetResourceAction(r.getVariable(), r.getAmount())
                        : new ModifyResourceAction(r.getVariable(), r.getAmount())))
                .toList();
    }

    private List<ActionRowForm> toActionRows(List<GameAction> actions) {
        return actions.stream().map(a -> {
            ActionRowForm row = new ActionRowForm();
            if (a instanceof ModifyResourceAction m) {
                row.setActionType("MODIFY_RESOURCE");
                row.setVariable(m.getVariable());
                row.setAmount(m.getAmount());
            } else if (a instanceof SetResourceAction s) {
                row.setActionType("SET_RESOURCE");
                row.setVariable(s.getVariable());
                row.setAmount(s.getValue());
            }
            return row;
        }).toList();
    }

    /** Blank/incomplete rows (e.g. an "Add Candidate" the user didn't fill in) are silently dropped, matching the actions-row convention above. */
    private List<WeightedTransition> toPool(List<PoolEntryForm> rows) {
        return rows.stream()
                .filter(r -> StringUtils.hasText(r.getEventId()) && r.getWeight() != null && r.getWeight() > 0)
                .map(r -> new WeightedTransition(r.getEventId(), r.getWeight()))
                .toList();
    }

    private List<PoolEntryForm> toPoolRows(List<WeightedTransition> pool) {
        return pool.stream().map(t -> {
            PoolEntryForm row = new PoolEntryForm();
            row.setEventId(t.getEventId());
            row.setWeight(t.getWeight());
            return row;
        }).toList();
    }

    private String blankToNull(String s) {
        return StringUtils.hasText(s) ? s : null;
    }
}
