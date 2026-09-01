package com.fizzycoyotestudio.eventforge.engine;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Represents the current state of a game world as a generic, dynamic
 * set of numeric variables (e.g. "food", "zombies", "morale").
 *
 * Deliberately NOT a fixed DTO with hardcoded fields like `food`, `water`,
 * `zombies` — this is what lets EventForge stay game-agnostic. A fantasy
 * dungeon game can use completely different variable names without any
 * change to the engine itself.
 */
public class GameState {

    private final Map<String, Double> variables = new HashMap<>();

    public GameState() {
    }

    public GameState(Map<String, Double> initialVariables) {
        this.variables.putAll(initialVariables);
    }

    public double get(String key) {
        return variables.getOrDefault(key, 0.0);
    }

    public void set(String key, double value) {
        variables.put(key, value);
    }

    public void modify(String key, double delta) {
        set(key, get(key) + delta);
    }

    public boolean has(String key) {
        return variables.containsKey(key);
    }

    /** Read-only view, e.g. for logging or serialization. */
    public Map<String, Double> asMap() {
        return Collections.unmodifiableMap(variables);
    }

    /** Creates an independent copy — useful for dry-running events. */
    public GameState copy() {
        return new GameState(this.variables);
    }

    @Override
    public String toString() {
        return "GameState" + variables;
    }
}
