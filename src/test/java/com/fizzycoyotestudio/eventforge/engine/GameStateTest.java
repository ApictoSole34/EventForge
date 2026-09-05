package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateTest {

    @Test
    void unknownVariableDefaultsToZero() {
        GameState state = new GameState();
        assertThat(state.get("zombies")).isZero();
        assertThat(state.has("zombies")).isFalse();
    }

    @Test
    void setThenGetReturnsStoredValue() {
        GameState state = new GameState();
        state.set("food", 42.0);
        assertThat(state.get("food")).isEqualTo(42.0);
        assertThat(state.has("food")).isTrue();
    }

    @Test
    void modifyAddsDeltaToExistingValue() {
        GameState state = new GameState(Map.of("ammo", 10.0));
        state.modify("ammo", -3);
        assertThat(state.get("ammo")).isEqualTo(7.0);
    }

    @Test
    void modifyOnUnknownVariableStartsFromZero() {
        GameState state = new GameState();
        state.modify("morale", 5);
        assertThat(state.get("morale")).isEqualTo(5.0);
    }

    @Test
    void asMapIsUnmodifiable() {
        GameState state = new GameState(Map.of("day", 1.0));
        assertThatThrownByModification(state);
    }

    private void assertThatThrownByModification(GameState state) {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> state.asMap().put("day", 2.0))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void copyIsIndependentFromOriginal() {
        GameState original = new GameState(Map.of("survivors", 6.0));
        GameState copy = original.copy();
        copy.set("survivors", 99.0);

        assertThat(original.get("survivors")).isEqualTo(6.0);
        assertThat(copy.get("survivors")).isEqualTo(99.0);
    }

    @Test
    void constructorCopiesInitialMapRatherThanAliasingIt() {
        Map<String, Double> source = new java.util.HashMap<>(Map.of("water", 10.0));
        GameState state = new GameState(source);
        source.put("water", 999.0);

        assertThat(state.get("water")).isEqualTo(10.0);
    }
}
