package com.angrysurfer.core.redis;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Step;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class StepHelper {
    private static final Logger logger = Logger.getLogger(StepHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public StepHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    public Step findStepById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("step:" + id);
            return json != null ? objectMapper.readValue(json, Step.class) : null;
        } catch (Exception e) {
            logger.severe("Error finding step: " + e.getMessage());
            return null;
        }
    }

    public Set<Step> findStepsForPattern(Long patternId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Step> steps = new HashSet<>();
            String stepsKey = "pattern:" + patternId + ":steps";
            Set<String> stepIds = jedis.smembers(stepsKey);
            
            for (String id : stepIds) {
                Step step = findStepById(Long.valueOf(id));
                if (step != null) {
                    steps.add(step);
                }
            }
            return steps;
        }
    }

    public Step saveStep(Step step) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (step.getId() == null) {
                step.setId(jedis.incr("seq:step"));
            }

            // Temporarily remove circular references
            Pattern pattern = step.getPattern();
            step.setPattern(null);

            // Save the step
            String json = objectMapper.writeValueAsString(step);
            jedis.set("step:" + step.getId(), json);

            // Update pattern-step relationship
            if (pattern != null) {
                String stepsKey = "pattern:" + pattern.getId() + ":steps";
                jedis.sadd(stepsKey, step.getId().toString());
            }

            // Restore references
            step.setPattern(pattern);
            return step;
        } catch (Exception e) {
            logger.severe("Error saving step: " + e.getMessage());
            throw new RuntimeException("Failed to save step", e);
        }
    }

    public void deleteStep(Long stepId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Step step = findStepById(stepId);
            if (step != null) {
                // Remove from pattern's step set
                if (step.getPattern() != null) {
                    String stepsKey = "pattern:" + step.getPattern().getId() + ":steps";
                    jedis.srem(stepsKey, stepId.toString());
                }
                // Delete the step
                jedis.del("step:" + stepId);
            }
        } catch (Exception e) {
            logger.severe("Error deleting step: " + e.getMessage());
            throw new RuntimeException("Failed to delete step", e);
        }
    }

    public Set<Step> findStepsByPatternId(Long patternId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Step> steps = new HashSet<>();
            String stepsKey = "pattern:" + patternId + ":steps";
            Set<String> stepIds = jedis.smembers(stepsKey);
            for (String id : stepIds) {
                Step step = findStepById(Long.valueOf(id));
                if (step != null) {
                    steps.add(step);
                }
            }
            return steps;
        }
    }
}