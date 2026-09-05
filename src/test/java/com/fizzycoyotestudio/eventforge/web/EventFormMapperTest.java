package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.*;
import com.fizzycoyotestudio.eventforge.web.dto.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventFormMapperTest {

    private final EventFormMapper mapper = new EventFormMapper();

    private EventFormData baseForm() {
        EventFormData form = new EventFormData();
        form.setId("zombie-attack");
        form.setName("Zombie Attack");
        form.setDescription("desc");
        return form;
    }

    @Test
    void mapsBasicFieldsToDomain() {
        Event event = mapper.toDomain(baseForm());
        assertThat(event.getId()).isEqualTo("zombie-attack");
        assertThat(event.getName()).isEqualTo("Zombie Attack");
        assertThat(event.getDescription()).isEqualTo("desc");
        assertThat(event.getCondition()).isInstanceOf(AlwaysTrueCondition.class);
        assertThat(event.getCooldownTicks()).isZero();
    }

    @Test
    void blankNextEventIdBecomesNullInDomain() {
        EventFormData form = baseForm();
        form.setNextEventId("   ");
        assertThat(mapper.toDomain(form).getNextEventId()).isNull();
    }

    @Test
    void nullCooldownDefaultsToZero() {
        EventFormData form = baseForm();
        form.setCooldownTicks(null);
        assertThat(mapper.toDomain(form).getCooldownTicks()).isZero();
    }

    @Test
    void singleConditionRowMapsToComparisonCondition() {
        EventFormData form = baseForm();
        ConditionRowForm row = new ConditionRowForm();
        row.setVariable("zombies");
        row.setOperator("GREATER_THAN");
        row.setValue(5.0);
        form.setConditions(new ArrayList<>(List.of(row)));

        Condition condition = mapper.toDomain(form).getCondition();

        assertThat(condition).isInstanceOf(ComparisonCondition.class);
        ComparisonCondition cc = (ComparisonCondition) condition;
        assertThat(cc.getVariable()).isEqualTo("zombies");
        assertThat(cc.getOperator()).isEqualTo(Operator.GREATER_THAN);
        assertThat(cc.getValue()).isEqualTo(5.0);
    }

    @Test
    void negatedConditionRowMapsToNotCondition() {
        EventFormData form = baseForm();
        ConditionRowForm row = new ConditionRowForm();
        row.setVariable("zombies");
        row.setOperator("EQUAL");
        row.setValue(0.0);
        row.setNegate(true);
        form.setConditions(new ArrayList<>(List.of(row)));

        Condition condition = mapper.toDomain(form).getCondition();

        assertThat(condition).isInstanceOf(NotCondition.class);
        assertThat(((NotCondition) condition).getCondition()).isInstanceOf(ComparisonCondition.class);
    }

    @Test
    void multipleRowsDefaultToAndCombinator() {
        EventFormData form = baseForm();
        form.setConditions(new ArrayList<>(List.of(row("zombies", "GREATER_THAN", 0.0), row("morale", "LESS_THAN", 100.0))));

        Condition condition = mapper.toDomain(form).getCondition();
        assertThat(condition).isInstanceOf(AndCondition.class);
        assertThat(((AndCondition) condition).getConditions()).hasSize(2);
    }

    @Test
    void multipleRowsWithOrCombinatorMapsToOrCondition() {
        EventFormData form = baseForm();
        form.setConditionCombinator("OR");
        form.setConditions(new ArrayList<>(List.of(row("zombies", "GREATER_THAN", 0.0), row("morale", "LESS_THAN", 100.0))));

        Condition condition = mapper.toDomain(form).getCondition();
        assertThat(condition).isInstanceOf(OrCondition.class);
    }

    @Test
    void incompleteConditionRowIsSilentlyDropped() {
        EventFormData form = baseForm();
        ConditionRowForm incomplete = new ConditionRowForm();
        incomplete.setVariable("zombies");
        form.setConditions(new ArrayList<>(List.of(incomplete)));

        assertThat(mapper.toDomain(form).getCondition()).isInstanceOf(AlwaysTrueCondition.class);
    }

    @Test
    void actionRowsMapToCorrectActionTypes() {
        EventFormData form = baseForm();
        ActionRowForm modify = new ActionRowForm();
        modify.setActionType("MODIFY_RESOURCE");
        modify.setVariable("ammo");
        modify.setAmount(-2.0);
        ActionRowForm set = new ActionRowForm();
        set.setActionType("SET_RESOURCE");
        set.setVariable("morale");
        set.setAmount(0.0);
        form.setActions(new ArrayList<>(List.of(modify, set)));

        List<GameAction> actions = mapper.toDomain(form).getActions();

        assertThat(actions).hasSize(2);
        assertThat(actions.get(0)).isInstanceOf(ModifyResourceAction.class);
        assertThat(actions.get(1)).isInstanceOf(SetResourceAction.class);
    }

    @Test
    void actionRowMissingVariableOrAmountIsDropped() {
        EventFormData form = baseForm();
        ActionRowForm incomplete = new ActionRowForm();
        incomplete.setVariable("ammo");
        form.setActions(new ArrayList<>(List.of(incomplete)));

        assertThat(mapper.toDomain(form).getActions()).isEmpty();
    }

    @Test
    void poolRowsFilterOutInvalidEntries() {
        EventFormData form = baseForm();
        PoolEntryForm valid = new PoolEntryForm();
        valid.setEventId("loot");
        valid.setWeight(2.0);
        PoolEntryForm invalidWeight = new PoolEntryForm();
        invalidWeight.setEventId("night");
        invalidWeight.setWeight(0.0);
        PoolEntryForm missingId = new PoolEntryForm();
        missingId.setWeight(1.0);
        form.setNextEventPool(new ArrayList<>(List.of(valid, invalidWeight, missingId)));

        List<WeightedTransition> pool = mapper.toDomain(form).getNextEventPool();

        assertThat(pool).hasSize(1);
        assertThat(pool.get(0).getEventId()).isEqualTo("loot");
        assertThat(pool.get(0).getWeight()).isEqualTo(2.0);
    }

    @Test
    void choicesMapWithOwnConditionActionsAndPool() {
        EventFormData form = baseForm();
        ChoiceFormData choice = new ChoiceFormData();
        choice.setId("push-back");
        choice.setLabel("Push Back");
        choice.setConditions(new ArrayList<>(List.of(row("zombies", "GREATER_THAN", 0.0))));
        ActionRowForm action = new ActionRowForm();
        action.setVariable("zombies");
        action.setAmount(-5.0);
        choice.setActions(new ArrayList<>(List.of(action)));
        choice.setNextEventId("loot");
        form.setChoices(new ArrayList<>(List.of(choice)));

        Choice domainChoice = mapper.toDomain(form).getChoices().get(0);

        assertThat(domainChoice.getId()).isEqualTo("push-back");
        assertThat(domainChoice.getCondition()).isInstanceOf(ComparisonCondition.class);
        assertThat(domainChoice.getActions()).hasSize(1);
        assertThat(domainChoice.getNextEventId()).isEqualTo("loot");
    }

    @Test
    void toFormDataRoundTripsAlwaysTrueConditionAsEmptyRows() {
        Event event = Event.builder().id("e").name("e").build();
        EventFormData form = mapper.toFormData(event);

        assertThat(form.getConditions()).isEmpty();
        assertThat(form.isComplexCondition()).isFalse();
    }

    @Test
    void toFormDataRoundTripsSimpleAndConditionIntoRows() {
        Event event = Event.builder().id("e").name("e")
                .condition(new AndCondition(List.of(
                        new ComparisonCondition("zombies", Operator.GREATER_THAN, 0),
                        new NotCondition(new ComparisonCondition("morale", Operator.EQUAL, 0))
                )))
                .build();

        EventFormData form = mapper.toFormData(event);

        assertThat(form.isComplexCondition()).isFalse();
        assertThat(form.getConditionCombinator()).isEqualTo("AND");
        assertThat(form.getConditions()).hasSize(2);
        assertThat(form.getConditions().get(1).isNegate()).isTrue();
    }

    @Test
    void toFormDataFlagsDeeplyNestedConditionAsComplexAndClearsRows() {
        Condition nested = new OrCondition(List.of(
                new AndCondition(List.of(
                        new ComparisonCondition("a", Operator.EQUAL, 1),
                        new ComparisonCondition("b", Operator.EQUAL, 2)
                )),
                new ComparisonCondition("c", Operator.EQUAL, 3)
        ));
        Event event = Event.builder().id("e").name("e").condition(nested).build();

        EventFormData form = mapper.toFormData(event);

        assertThat(form.isComplexCondition()).isTrue();
        assertThat(form.getConditions()).isEmpty();
    }

    @Test
    void toFormDataRoundTripsActionsAndPool() {
        Event event = Event.builder().id("e").name("e")
                .actions(List.of(new ModifyResourceAction("ammo", -2), new SetResourceAction("morale", 0)))
                .nextEventPool(List.of(new WeightedTransition("loot", 2.0)))
                .build();

        EventFormData form = mapper.toFormData(event);

        assertThat(form.getActions()).hasSize(2);
        assertThat(form.getActions().get(0).getActionType()).isEqualTo("MODIFY_RESOURCE");
        assertThat(form.getActions().get(1).getActionType()).isEqualTo("SET_RESOURCE");
        assertThat(form.getNextEventPool()).hasSize(1);
        assertThat(form.getNextEventPool().get(0).getEventId()).isEqualTo("loot");
    }

    private ConditionRowForm row(String variable, String operator, double value) {
        ConditionRowForm row = new ConditionRowForm();
        row.setVariable(variable);
        row.setOperator(operator);
        row.setValue(value);
        return row;
    }
}
