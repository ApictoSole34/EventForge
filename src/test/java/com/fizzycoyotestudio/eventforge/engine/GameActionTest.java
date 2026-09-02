package com.fizzycoyotestudio.eventforge.engine;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GameActionTest {

    @Test
    void modifyResourceSubtractsAmount() {
        GameState state = new GameState();
        state.set("ammo", 13.0);

        GameAction action = new ModifyResourceAction("ammo", -2.0);
        action.execute(state);

        assertThat(state.get("ammo")).isEqualTo(11.0);
    }

    @Test
    void modifyResourceAddsAmount() {
        GameState state = new GameState();
        state.set("survivors", 6.0);

        GameAction action = new ModifyResourceAction("survivors", 1.0);
        action.execute(state);

        assertThat(state.get("survivors")).isEqualTo(7.0);
    }

    @Test
    void setResourceOverwritesValue() {
        GameState state = new GameState();
        state.set("morale", 67.0);

        GameAction action = new SetResourceAction("morale", 0.0);
        action.execute(state);

        assertThat(state.get("morale")).isEqualTo(0.0);
    }

    @Test
    void actionsAppliedInDeclarationOrder() {
        GameState state = new GameState();
        state.set("morale", 50.0);

        GameAction first = new ModifyResourceAction("morale", 20.0);   // -> 70
        GameAction second = new ModifyResourceAction("morale", -100.0); // -> -30

        first.execute(state);
        second.execute(state);

        assertThat(state.get("morale")).isEqualTo(-30.0);
    }
}
