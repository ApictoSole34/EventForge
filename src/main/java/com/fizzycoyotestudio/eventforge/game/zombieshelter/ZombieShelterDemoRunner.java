package com.fizzycoyotestudio.eventforge.game.zombieshelter;

import com.fizzycoyotestudio.eventforge.engine.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Scanner;

/**
 * Interactive console playthrough of Zombie Shelter, useful for manually
 * sanity-checking the engine end-to-end without a UI. Only runs under
 * the "demo" Spring profile so it never fires during normal app startup
 * or `mvn test`.
 *
 * Run with: mvn spring-boot:run -Dspring-boot.run.profiles=demo
 */
@Component
@Profile("demo")
public class ZombieShelterDemoRunner implements CommandLineRunner {

    @Override
    public void run(String... args) {
        EventEngine engine = new EventEngine();
        EventRegistry registry = ZombieShelterScenario.buildRegistry();
        GameState state = ZombieShelterScenario.initialState();
        GameSession session = new GameSession(engine, registry, state, "zombie-attack");

        Scanner scanner = new Scanner(System.in);

        System.out.println("=== ZOMBIE SHELTER ===");
        printState(state);

        playChainUntilEnd(session, scanner);

        System.out.println("\n--- A new event approaches ---");
        session = new GameSession(engine, registry, state, "stranger-at-the-gate");
        playChainUntilEnd(session, scanner);

        System.out.println("\n=== FINAL STATE ===");
        printState(state);
    }

    private void playChainUntilEnd(GameSession session, Scanner scanner) {
        Event previousEvent = null;
        while (true) {
            Event current = session.getCurrentEvent();
            EventResult result = session.triggerCurrentEvent();

            if (!result.isTriggered()) {
                System.out.println("\n[" + current.getName() + "] condition not met — nothing happens.");
                return;
            }

            System.out.println("\n[" + current.getName() + "] " + current.getDescription());

            if (result.isAwaitingChoice()) {
                List<Choice> choices = result.getOfferedChoices();
                for (int i = 0; i < choices.size(); i++) {
                    System.out.println("  " + (i + 1) + ") " + choices.get(i).getLabel());
                }
                System.out.print("> ");
                int pick = Integer.parseInt(scanner.nextLine().trim()) - 1;
                session.choose(choices.get(pick).getId());
                continue;
            }

            if (previousEvent == current && !result.hasNextEvent()) {
                return;
            }
            previousEvent = current;

            if (!result.hasNextEvent()) {
                return;
            }
        }
    }

    private void printState(GameState state) {
        state.asMap().forEach((key, value) -> System.out.println("  " + key + ": " + value));
    }
}
