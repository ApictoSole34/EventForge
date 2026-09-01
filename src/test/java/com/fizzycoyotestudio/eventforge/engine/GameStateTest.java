package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameStateTest {

    @Test
    void returnsZeroForUnknownVariable() {
        GameState state = new GameState();
        assertThat(state.get("food")).isEqualTo(0.0);
    }

    @Test
    void setAndGetRoundTrip() {
        GameState state = new GameState();
        state.set("food", 42.0);
        assertThat(state.get("food")).isEqualTo(42.0);
    }

    @Test
    void modifyAppliesDelta() {
        GameState state = new GameState();
        state.set("ammo", 13.0);
        state.modify("ammo", -2.0);
        assertThat(state.get("ammo")).isEqualTo(11.0);
    }

    @Test
    void copyIsIndependent() {
        GameState original = new GameState();
        original.set("morale", 67.0);

        GameState copy = original.copy();
        copy.set("morale", 0.0);

        assertThat(original.get("morale")).isEqualTo(67.0);
        assertThat(copy.get("morale")).isEqualTo(0.0);
    }
}
