package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameActionTest {

    @Test
    void modifyResourceActionAddsPositiveAmount() {
        GameState state = new GameState();
        state.set("food", 10.0);

        new ModifyResourceAction("food", 5.0).execute(state);

        assertThat(state.get("food")).isEqualTo(15.0);
    }

    @Test
    void modifyResourceActionSubtractsNegativeAmount() {
        GameState state = new GameState();
        state.set("ammo", 10.0);

        new ModifyResourceAction("ammo", -3.0).execute(state);

        assertThat(state.get("ammo")).isEqualTo(7.0);
    }

    @Test
    void modifyResourceActionOnMissingVariableStartsFromZero() {
        GameState state = new GameState();

        new ModifyResourceAction("morale", 5.0).execute(state);

        assertThat(state.get("morale")).isEqualTo(5.0);
    }

    @Test
    void setResourceActionOverwritesExistingValue() {
        GameState state = new GameState();
        state.set("morale", 50.0);

        new SetResourceAction("morale", 0.0).execute(state);

        assertThat(state.get("morale")).isEqualTo(0.0);
    }

    @Test
    void setResourceActionCreatesNewVariable() {
        GameState state = new GameState();

        new SetResourceAction("newVar", 3.0).execute(state);

        assertThat(state.get("newVar")).isEqualTo(3.0);
        assertThat(state.has("newVar")).isTrue();
    }
}
