package com.fizzycoyotestudio.eventforge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import org.springframework.stereotype.Component;

import java.util.Map;

/** GameState has no polymorphism, so this is much simpler than ConditionActionJsonMapper. */
@Component
public class GameStateJsonMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    public String write(GameState state) {
        try {
            return mapper.writeValueAsString(state.asMap());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize GameState", e);
        }
    }

    public GameState read(String json) {
        try {
            Map<String, Double> map = mapper.readValue(json, new TypeReference<Map<String, Double>>() {});
            return new GameState(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize GameState: " + json, e);
        }
    }
}