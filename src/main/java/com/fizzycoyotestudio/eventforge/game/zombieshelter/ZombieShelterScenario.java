package com.fizzycoyotestudio.eventforge.game.zombieshelter;

import com.fizzycoyotestudio.eventforge.engine.*;

import java.util.List;
import java.util.Map;

/**
 * Concrete event definitions and initial state for the Zombie Shelter
 * game. This is the ONLY class in the codebase that knows about
 * zombies, survivors, or shelters — everything it uses (Event,
 * Condition, GameAction, Choice, EventRegistry) comes from the
 * game-agnostic `engine` package.
 *
 * Chain built here (matches the spec's example scenarios):
 *
 *   zombie-attack --(auto)--> zombie-attack-result --(choice)--> loot | night-falls
 *   stranger-at-the-gate --(choice)--> stranger-joins | night-falls | search-result
 *   loot / stranger-joins / search-result / night-falls --(auto)--> day-summary
 *
 * Hardcoded here for Phase 2 (prototype). Phase 3 will move this into
 * PostgreSQL/JSON so events can be authored without recompiling.
 */
public final class ZombieShelterScenario {

    private ZombieShelterScenario() {
    }

    public static GameState initialState() {
        GameState state = new GameState();
        state.set("day", 17);
        state.set("survivors", 6);
        state.set("food", 42);
        state.set("water", 31);
        state.set("ammo", 13);
        state.set("morale", 67);
        state.set("zombies", 12);
        state.set("shelterHealth", 84);
        return state;
    }

    public static EventRegistry buildRegistry() {
        Event zombieAttack = Event.builder()
                .id("zombie-attack")
                .name("Zombie Attack")
                .description("A group of zombies reached the shelter.")
                .condition(new ComparisonCondition("zombies", Operator.GREATER_THAN, 0))
                .actions(List.of(
                        new ModifyResourceAction("ammo", -2),
                        new ModifyResourceAction("morale", -5)
                ))
                .nextEventId("zombie-attack-result")
                .build();

        Event zombieAttackResult = Event.builder()
                .id("zombie-attack-result")
                .name("Zombie Attack Result")
                .description("The shelter must respond to the attack.")
                .choices(List.of(
                        Choice.builder()
                                .id("push-back")
                                .label("Push Back")
                                .actions(List.of(new ModifyResourceAction("zombies", -5)))
                                .nextEventId("loot")
                                .build(),
                        Choice.builder()
                                .id("retreat")
                                .label("Retreat")
                                .actions(List.of(
                                        new ModifyResourceAction("shelterHealth", -10),
                                        new ModifyResourceAction("morale", -5)
                                ))
                                .nextEventId("night-falls")
                                .build()
                ))
                .build();

        Event loot = Event.builder()
                .id("loot")
                .name("Loot")
                .description("The fallen zombies left something behind.")
                .actions(List.of(
                        new ModifyResourceAction("food", 10),
                        new ModifyResourceAction("ammo", 3)
                ))
                .nextEventId("day-summary")
                .build();

        Event strangerAtTheGate = Event.builder()
                .id("stranger-at-the-gate")
                .name("Stranger at the Gate")
                .description("A stranger asks for shelter.")
                .condition(new ComparisonCondition("survivors", Operator.GREATER_THAN_OR_EQUAL, 3))
                .choices(List.of(
                        Choice.builder()
                                .id("let-him-in")
                                .label("Let him in")
                                .actions(List.of(
                                        new ModifyResourceAction("survivors", 1),
                                        new ModifyResourceAction("morale", 5)
                                ))
                                .nextEventId("stranger-joins")
                                .build(),
                        Choice.builder()
                                .id("refuse")
                                .label("Refuse")
                                .actions(List.of(new ModifyResourceAction("morale", -3)))
                                .nextEventId("night-falls")
                                .build(),
                        Choice.builder()
                                .id("search-him")
                                .label("Search him")
                                .nextEventId("search-result")
                                .build()
                ))
                .build();

        Event strangerJoins = Event.builder()
                .id("stranger-joins")
                .name("Stranger Joins")
                .description("The stranger is now part of the shelter.")
                .nextEventId("day-summary")
                .build();

        Event searchResult = Event.builder()
                .id("search-result")
                .name("Search Result")
                .description("You find a spare magazine in his bag.")
                .actions(List.of(new ModifyResourceAction("ammo", 1)))
                .nextEventId("day-summary")
                .build();

        Event nightFalls = Event.builder()
                .id("night-falls")
                .name("Night Falls")
                .description("The shelter settles in for the night.")
                .nextEventId("day-summary")
                .build();

        Event daySummary = Event.builder()
                .id("day-summary")
                .name("Day Summary")
                .description("Another day survived.")
                .actions(List.of(new ModifyResourceAction("day", 1)))
                .build();

        return new EventRegistry(Map.ofEntries(
                Map.entry(zombieAttack.getId(), zombieAttack),
                Map.entry(zombieAttackResult.getId(), zombieAttackResult),
                Map.entry(loot.getId(), loot),
                Map.entry(strangerAtTheGate.getId(), strangerAtTheGate),
                Map.entry(strangerJoins.getId(), strangerJoins),
                Map.entry(searchResult.getId(), searchResult),
                Map.entry(nightFalls.getId(), nightFalls),
                Map.entry(daySummary.getId(), daySummary)
        ));
    }
}
