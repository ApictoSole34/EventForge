package com.fizzycoyotestudio.eventforge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Serializes the per-session "which event last fired on which tick" map
 * used for cooldown filtering (see GameSession#getLastTriggeredTick).
 * Kept separate from GameStateJsonMapper on purpose: different value
 * type (String -> Integer, not String -> Double) and a different
 * concern — engine bookkeeping, not player-visible game state.
 */
@Component
public class CooldownJsonMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    public String write(Map<String, Integer> lastTriggeredTick) {
        try {
            return mapper.writeValueAsString(lastTriggeredTick);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cooldown map", e);
        }
    }

    public Map<String, Integer> read(String json) {
        if (json == null) {
            return Map.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<Map<String, Integer>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize cooldown map: " + json, e);
        }
    }
}
