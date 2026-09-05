package com.fizzycoyotestudio.eventforge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fizzycoyotestudio.eventforge.engine.Condition;
import com.fizzycoyotestudio.eventforge.engine.GameAction;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import com.fizzycoyotestudio.eventforge.engine.WeightedTransition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Single Jackson entry point for everything EventForge persists as
 * JSON: polymorphic Condition/GameAction trees, WeightedTransition
 * pools, GameState variable maps, and the per-session cooldown map.
 *
 * <p>Replaces three separate mappers (ConditionActionJsonMapper,
 * GameStateJsonMapper, CooldownJsonMapper) that each carried their own
 * {@code ObjectMapper} instance for no real benefit — none of these
 * value types need different Jackson configuration from one another,
 * so splitting them only meant three places to keep in sync (e.g. if
 * a module/feature ever needs registering on the mapper).
 */
@Component
public class EventForgeJsonMapper {

    private final ObjectMapper mapper = new ObjectMapper();

    public String writeCondition(Condition condition) {
        try {
            return mapper.writerFor(new TypeReference<Condition>() {}).writeValueAsString(condition);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Condition", e);
        }
    }

    public Condition readCondition(String json) {
        if (json == null) {
            return Condition.alwaysTrue();
        }
        try {
            return mapper.readValue(json, Condition.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize Condition: " + json, e);
        }
    }

    public String writeActions(List<GameAction> actions) {
        try {
            return mapper.writerFor(new TypeReference<List<GameAction>>() {}).writeValueAsString(actions);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize actions", e);
        }
    }

    public List<GameAction> readActions(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<GameAction>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize actions: " + json, e);
        }
    }

    public String writePool(List<WeightedTransition> pool) {
        try {
            return mapper.writerFor(new TypeReference<List<WeightedTransition>>() {}).writeValueAsString(pool);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize next-event pool", e);
        }
    }

    public List<WeightedTransition> readPool(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return mapper.readValue(json, new TypeReference<List<WeightedTransition>>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize next-event pool: " + json, e);
        }
    }

    public String writeState(GameState state) {
        try {
            return mapper.writeValueAsString(state.asMap());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize GameState", e);
        }
    }

    public GameState readState(String json) {
        try {
            Map<String, Double> map = mapper.readValue(json, new TypeReference<Map<String, Double>>() {});
            return new GameState(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize GameState: " + json, e);
        }
    }

    /** Per-session "which event last fired on which tick" map, used for cooldown filtering. */
    public String writeCooldowns(Map<String, Integer> lastTriggeredTick) {
        try {
            return mapper.writeValueAsString(lastTriggeredTick);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize cooldown map", e);
        }
    }

    public Map<String, Integer> readCooldowns(String json) {
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