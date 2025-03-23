package com.angrysurfer.core.redis;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class PatternHelper {
    private static final Logger logger = LoggerFactory.getLogger(PatternHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final StepHelper stepHelper;

    public PatternHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        this.stepHelper = new StepHelper(jedisPool, objectMapper);
    }

    public Pattern findPatternById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("pattern:" + id);
            if (json != null) {
                Pattern pattern = objectMapper.readValue(json, Pattern.class);
                Set<Step> steps = stepHelper.findStepsForPattern(id);
                pattern.setSteps(steps);
                steps.forEach(s -> s.setPattern(pattern));
                return pattern;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error finding pattern: " + e.getMessage());
            return null;
        }
    }

    public Set<Pattern> findPatternsForSong(Long songId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Pattern> patterns = new HashSet<>();
            String patternsKey = "song:" + songId + ":patterns";
            Set<String> patternIds = jedis.smembers(patternsKey);
            
            for (String id : patternIds) {
                Pattern pattern = findPatternById(Long.valueOf(id));
                if (pattern != null) {
                    patterns.add(pattern);
                }
            }
            return patterns;
        }
    }

    public void savePattern(Pattern pattern) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (pattern.getId() == null) {
                pattern.setId(jedis.incr("seq:pattern"));
            }

            // Save all steps first
            if (pattern.getSteps() != null) {
                pattern.getSteps().forEach(step -> stepHelper.saveStep(step));
            }

            // Temporarily remove circular references
            Set<Step> steps = pattern.getSteps();
            Song song = pattern.getSong();
            pattern.setSteps(null);
            pattern.setSong(null);

            // Save the pattern
            String json = objectMapper.writeValueAsString(pattern);
            jedis.set("pattern:" + pattern.getId(), json);

            // Update song-pattern relationship
            if (song != null) {
                String patternsKey = "song:" + song.getId() + ":patterns";
                jedis.sadd(patternsKey, pattern.getId().toString());
            }

            // Restore references
            pattern.setSteps(steps);
            pattern.setSong(song);
        } catch (Exception e) {
            logger.error("Error saving pattern: " + e.getMessage());
            throw new RuntimeException("Failed to save pattern", e);
        }
    }

    public void deletePattern(Long patternId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Pattern pattern = findPatternById(patternId);
            if (pattern != null) {
                // Delete all steps
                if (pattern.getSteps() != null) {
                    pattern.getSteps().forEach(step -> 
                        stepHelper.deleteStep(step.getId()));
                }
                // Remove from song's pattern set
                if (pattern.getSong() != null) {
                    String patternsKey = "song:" + pattern.getSong().getId() + ":patterns";
                    jedis.srem(patternsKey, patternId.toString());
                }
                // Delete the pattern
                jedis.del("pattern:" + patternId);
            }
        } catch (Exception e) {
            logger.error("Error deleting pattern: " + e.getMessage());
            throw new RuntimeException("Failed to delete pattern", e);
        }
    }
}