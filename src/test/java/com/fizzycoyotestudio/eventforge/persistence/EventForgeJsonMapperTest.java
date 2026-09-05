package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EventForgeJsonMapperTest {

    private final EventForgeJsonMapper json = new EventForgeJsonMapper();

    @Test
    void nullConditionJsonReadsAsAlwaysTrue() {
        assertThat(json.readCondition(null)).isInstanceOf(AlwaysTrueCondition.class);
    }

    @Test
    void comparisonConditionRoundTrips() {
        Condition original = new ComparisonCondition("zombies", Operator.GREATER_THAN, 10.0);

        Condition roundTripped = json.readCondition(json.writeCondition(original));

        assertThat(roundTripped).isInstanceOf(ComparisonCondition.class);
        ComparisonCondition cc = (ComparisonCondition) roundTripped;
        assertThat(cc.getVariable()).isEqualTo("zombies");
        assertThat(cc.getOperator()).isEqualTo(Operator.GREATER_THAN);
        assertThat(cc.getValue()).isEqualTo(10.0);
    }

    @Test
    void alwaysTrueConditionRoundTrips() {
        String written = json.writeCondition(Condition.alwaysTrue());
        assertThat(json.readCondition(written)).isInstanceOf(AlwaysTrueCondition.class);
    }

    @Test
    void notConditionRoundTrips() {
        Condition original = new NotCondition(new ComparisonCondition("zombies", Operator.EQUAL, 0));
        Condition roundTripped = json.readCondition(json.writeCondition(original));

        assertThat(roundTripped).isInstanceOf(NotCondition.class);
        assertThat(((NotCondition) roundTripped).getCondition()).isInstanceOf(ComparisonCondition.class);
    }

    @Test
    void andOrConditionsRoundTripWithNestedChildren() {
        Condition nested = new OrCondition(List.of(
                new AndCondition(List.of(
                        new ComparisonCondition("a", Operator.EQUAL, 1),
                        new ComparisonCondition("b", Operator.EQUAL, 2)
                )),
                new ComparisonCondition("c", Operator.EQUAL, 3)
        ));

        Condition roundTripped = json.readCondition(json.writeCondition(nested));

        assertThat(roundTripped).isInstanceOf(OrCondition.class);
        OrCondition or = (OrCondition) roundTripped;
        assertThat(or.getConditions()).hasSize(2);
        assertThat(or.getConditions().get(0)).isInstanceOf(AndCondition.class);
    }

    @Test
    void nullActionsJsonReadsAsEmptyList() {
        assertThat(json.readActions(null)).isEmpty();
    }

    @Test
    void actionsRoundTripPreservingPolymorphicType() {
        List<GameAction> original = List.of(
                new ModifyResourceAction("ammo", -2.0),
                new SetResourceAction("morale", 0.0)
        );

        List<GameAction> roundTripped = json.readActions(json.writeActions(original));

        assertThat(roundTripped).hasSize(2);
        assertThat(roundTripped.get(0)).isInstanceOf(ModifyResourceAction.class);
        assertThat(roundTripped.get(1)).isInstanceOf(SetResourceAction.class);
    }

    @Test
    void nullPoolJsonReadsAsEmptyList() {
        assertThat(json.readPool(null)).isEmpty();
    }

    @Test
    void poolRoundTripsWeightsAndIds() {
        List<WeightedTransition> original = List.of(new WeightedTransition("loot", 2.5));

        List<WeightedTransition> roundTripped = json.readPool(json.writePool(original));

        assertThat(roundTripped).hasSize(1);
        assertThat(roundTripped.get(0).getEventId()).isEqualTo("loot");
        assertThat(roundTripped.get(0).getWeight()).isEqualTo(2.5);
    }

    @Test
    void gameStateRoundTripsAllVariables() {
        GameState original = new GameState(Map.of("food", 42.0, "zombies", 12.0));

        GameState roundTripped = json.readState(json.writeState(original));

        assertThat(roundTripped.get("food")).isEqualTo(42.0);
        assertThat(roundTripped.get("zombies")).isEqualTo(12.0);
    }

    @Test
    void cooldownMapRoundTripsEventIdToTick() {
        Map<String, Integer> original = Map.of("zombie-attack", 3, "loot", 7);

        Map<String, Integer> roundTripped = json.readCooldowns(json.writeCooldowns(original));

        assertThat(roundTripped).isEqualTo(original);
    }

    @Test
    void nullCooldownJsonReadsAsEmptyMap() {
        assertThat(json.readCooldowns(null)).isEmpty();
    }
}
