package com.fizzycoyotestudio.eventforge.engine;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

/**
 * The most common action: adds (or subtracts, with a negative amount)
 * a fixed value to a GameState variable.
 *
 * Example: new ModifyResourceAction("ammo", -2) represents "ammo -= 2".
 */
public final class ModifyResourceAction implements GameAction {

    private final String variable;
    private final double amount;

    @JsonCreator
    public ModifyResourceAction(@JsonProperty("variable") String variable,
                                @JsonProperty("amount") double amount) {
        this.variable = Objects.requireNonNull(variable, "variable must not be null");
        this.amount = amount;
    }

    @Override
    public void execute(GameState state) {
        state.modify(variable, amount);
    }

    public String getVariable() {
        return variable;
    }

    public double getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return variable + (amount >= 0 ? " += " : " -= ") + Math.abs(amount);
    }
}
