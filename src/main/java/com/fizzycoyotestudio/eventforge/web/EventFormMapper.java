package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.*;
import com.fizzycoyotestudio.eventforge.web.dto.*;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Component
public class EventFormMapper {

    public Event toDomain(EventFormData form) {
        return Event.builder()
                .id(form.getId())
                .name(form.getName())
                .description(form.getDescription())
                .condition(toCondition(form.getConditions(), form.getConditionCombinator()))
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
                .condition(toCondition(form.getConditions(), form.getConditionCombinator()))
                .actions(toActions(form.getActions()))
                .nextEventId(blankToNull(form.getNextEventId()))
                .nextEventPool(toPool(form.getNextEventPool()))
                .build();
    }

    public EventFormData toFormData(Event event) {
        EventFormData form = new EventFormData();
        form.setId(event.getId());
        form.setName(event.getName());
        form.setDescription(event.getDescription());
        fillCondition(event.getCondition(), form::setConditions, form::setConditionCombinator, form::setComplexCondition);
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
        fillCondition(choice.getCondition(), form::setConditions, form::setConditionCombinator, form::setComplexCondition);
        form.setActions(toActionRows(choice.getActions()));
        form.setNextEventId(choice.getNextEventId());
        form.setNextEventPool(toPoolRows(choice.getNextEventPool()));
        return form;
    }

    private Condition toCondition(List<ConditionRowForm> rows, String combinator) {
        List<Condition> conditions = rows.stream()
                .filter(r -> StringUtils.hasText(r.getVariable()) && StringUtils.hasText(r.getOperator()) && r.getValue() != null)
                .<Condition>map(r -> {
                    Condition c = new ComparisonCondition(r.getVariable(), Operator.valueOf(r.getOperator()), r.getValue());
                    return r.isNegate() ? new NotCondition(c) : c;
                })
                .toList();

        if (conditions.isEmpty()) {
            return Condition.alwaysTrue();
        }
        if (conditions.size() == 1) {
            return conditions.get(0);
        }
        return "OR".equals(combinator) ? new OrCondition(conditions) : new AndCondition(conditions);
    }

    /**
     * Reconstructs form rows from a saved Condition. Handles:
     * <ul>
     *   <li>ALWAYS_TRUE → empty rows</li>
     *   <li>single COMPARISON → one row</li>
     *   <li>single NOT(COMPARISON) → one row with negate=true</li>
     *   <li>AND/OR where all children are COMPARISON or NOT(COMPARISON) → one row per child</li>
     * </ul>
     * Anything deeper (e.g. OR inside an AND) cannot be represented in this simple builder —
     * rows are left empty and complexCondition = true, so the UI can warn the author
     * rather than silently overwriting the original condition on save.
     */
    private void fillCondition(Condition condition, Consumer<List<ConditionRowForm>> setRows,
                               Consumer<String> setCombinator, Consumer<Boolean> setComplex) {
        setCombinator.accept("AND");
        setComplex.accept(false);

        if (condition instanceof AlwaysTrueCondition) {
            setRows.accept(new ArrayList<>());
            return;
        }
        if (condition instanceof ComparisonCondition c) {
            setRows.accept(new ArrayList<>(List.of(toRow(c, false))));
            return;
        }
        if (condition instanceof NotCondition n && n.getCondition() instanceof ComparisonCondition c) {
            setRows.accept(new ArrayList<>(List.of(toRow(c, true))));
            return;
        }
        if (condition instanceof AndCondition || condition instanceof OrCondition) {
            List<Condition> children = condition instanceof AndCondition a
                    ? a.getConditions() : ((OrCondition) condition).getConditions();
            List<ConditionRowForm> rows = new ArrayList<>();
            boolean allSimple = true;
            for (Condition child : children) {
                if (child instanceof ComparisonCondition c) {
                    rows.add(toRow(c, false));
                } else if (child instanceof NotCondition n && n.getCondition() instanceof ComparisonCondition c) {
                    rows.add(toRow(c, true));
                } else {
                    allSimple = false;
                    break;
                }
            }
            if (allSimple) {
                setRows.accept(rows);
                setCombinator.accept(condition instanceof OrCondition ? "OR" : "AND");
            } else {
                setRows.accept(new ArrayList<>());
                setComplex.accept(true);
            }
            return;
        }

        setRows.accept(new ArrayList<>());
        setComplex.accept(true);
    }

    private ConditionRowForm toRow(ComparisonCondition c, boolean negate) {
        ConditionRowForm row = new ConditionRowForm();
        row.setVariable(c.getVariable());
        row.setOperator(c.getOperator().name());
        row.setValue(c.getValue());
        row.setNegate(negate);
        return row;
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