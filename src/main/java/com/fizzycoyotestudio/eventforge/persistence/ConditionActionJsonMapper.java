package com.fizzycoyotestudio.eventforge.persistence;

import com.fizzycoyotestudio.eventforge.engine.Condition;
import com.fizzycoyotestudio.eventforge.engine.GameAction;
import com.fizzycoyotestudio.eventforge.engine.WeightedTransition;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@Component
public class ConditionActionJsonMapper {

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

    /** WeightedTransition isn't polymorphic, so this is a plain bean list — no @JsonTypeInfo involved. */
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
}
