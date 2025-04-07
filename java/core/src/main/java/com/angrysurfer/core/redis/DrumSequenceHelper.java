package com.angrysurfer.core.redis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
class DrumSequenceHelper {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequenceHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final CommandBus commandBus = CommandBus.getInstance();
    
    // Constants
    private static final int DRUM_PAD_COUNT = 16;
    private static final int MAX_STEPS = 64;

    public DrumSequenceHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    /**
     * Find a drum sequence by ID
     */
    public DrumSequenceData findDrumSequenceById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("drumseq:" + id);
            if (json != null) {
                DrumSequenceData data = objectMapper.readValue(json, DrumSequenceData.class);
                logger.info("Loaded drum sequence {}", id);
                return data;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error finding drum sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to find drum sequence", e);
        }
    }
    
    /**
     * Apply loaded data to a DrumSequencer
     */
    public void applyToSequencer(DrumSequenceData data, DrumSequencer sequencer) {
        if (data == null || sequencer == null) {
            logger.warn("Cannot apply null data or sequencer");
            return;
        }
        
        try {
            // Set basic sequence ID
            sequencer.setDrumSequenceId(data.getId());
            
            // Apply pattern lengths
            if (data.getPatternLengths() != null) {
                for (int i = 0; i < Math.min(data.getPatternLengths().length, DRUM_PAD_COUNT); i++) {
                    sequencer.setPatternLength(i, data.getPatternLengths()[i]);
                }
            }
            
            // Apply directions
            if (data.getDirections() != null) {
                for (int i = 0; i < Math.min(data.getDirections().length, DRUM_PAD_COUNT); i++) {
                    sequencer.setDirection(i, data.getDirections()[i]);
                }
            }
            
            // Apply timing divisions
            if (data.getTimingDivisions() != null) {
                for (int i = 0; i < Math.min(data.getTimingDivisions().length, DRUM_PAD_COUNT); i++) {
                    sequencer.setTimingDivision(i, data.getTimingDivisions()[i]);
                }
            }
            
            // Apply looping flags
            if (data.getLoopingFlags() != null) {
                for (int i = 0; i < Math.min(data.getLoopingFlags().length, DRUM_PAD_COUNT); i++) {
                    sequencer.setLooping(i, data.getLoopingFlags()[i]);
                }
            }
            
            // Apply velocities
            if (data.getVelocities() != null) {
                for (int i = 0; i < Math.min(data.getVelocities().length, DRUM_PAD_COUNT); i++) {
                    sequencer.setVelocity(i, data.getVelocities()[i]);
                }
            }
            
            // Apply patterns
            if (data.getPatterns() != null) {
                for (int i = 0; i < Math.min(data.getPatterns().length, DRUM_PAD_COUNT); i++) {
                    for (int j = 0; j < Math.min(data.getPatterns()[i].length, MAX_STEPS); j++) {
                        if (data.getPatterns()[i][j]) {
                            // Make sure it's toggled to ON state if true in stored data
                            if (!sequencer.isStepActive(i, j)) {
                                sequencer.toggleStep(i, j);
                            }
                        } else {
                            // Make sure it's toggled to OFF state if false in stored data
                            if (sequencer.isStepActive(i, j)) {
                                sequencer.toggleStep(i, j);
                            }
                        }
                    }
                }
            }
            
            // Notify that pattern has updated
            commandBus.publish(Commands.PATTERN_UPDATED, this, sequencer.getDrumSequenceId());
            
        } catch (Exception e) {
            logger.error("Error applying drum sequence data to sequencer: " + e.getMessage(), e);
        }
    }

    /**
     * Save a drum sequence
     */
    public void saveDrumSequence(DrumSequencer sequencer) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Create a data transfer object
            DrumSequenceData data = new DrumSequenceData();
            
            // Set or generate ID
            if (sequencer.getDrumSequenceId() == 0) {
                data.setId(jedis.incr("seq:drumsequence"));
                sequencer.setDrumSequenceId(data.getId());
            } else {
                data.setId(sequencer.getDrumSequenceId());
            }
            
            // Copy pattern data
            data.setPatternLengths(Arrays.copyOf(sequencer.getPatternLengths(), DRUM_PAD_COUNT));
            data.setDirections(Arrays.copyOf(sequencer.getDirections(), DRUM_PAD_COUNT));
            data.setTimingDivisions(Arrays.copyOf(sequencer.getTimingDivisions(), DRUM_PAD_COUNT));
            data.setLoopingFlags(Arrays.copyOf(sequencer.getLoopingFlags(), DRUM_PAD_COUNT));
            data.setVelocities(Arrays.copyOf(sequencer.getVelocities(), DRUM_PAD_COUNT));
            data.setOriginalVelocities(Arrays.copyOf(sequencer.getOriginalVelocities(), DRUM_PAD_COUNT));
            
            // Copy step patterns (all drums, all steps)
            boolean[][] patterns = new boolean[DRUM_PAD_COUNT][MAX_STEPS];
            for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                for (int j = 0; j < MAX_STEPS; j++) {
                    patterns[i][j] = sequencer.isStepActive(i, j);
                }
            }
            data.setPatterns(patterns);
            
            // Save to Redis
            String json = objectMapper.writeValueAsString(data);
            jedis.set("drumseq:" + data.getId(), json);
            
            logger.info("Saved drum sequence {}", data.getId());
            
            // Notify listeners
            commandBus.publish(Commands.DRUM_SEQUENCE_SAVED, this, data.getId());
            
        } catch (Exception e) {
            logger.error("Error saving drum sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to save drum sequence", e);
        }
    }

    /**
     * Get all drum sequence IDs
     */
    public List<Long> getAllDrumSequenceIds() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("drumseq:*");
            List<Long> ids = new ArrayList<>();
            for (String key : keys) {
                try {
                    ids.add(Long.parseLong(key.split(":")[1]));
                } catch (NumberFormatException e) {
                    logger.error("Invalid drum sequence key: " + key);
                }
            }
            return ids;
        }
    }

    /**
     * Get the minimum drum sequence ID
     */
    public Long getMinimumDrumSequenceId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("drumseq:*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[1]))
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Get the maximum drum sequence ID
     */
    public Long getMaximumDrumSequenceId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("drumseq:*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[1]))
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Delete a drum sequence
     */
    public void deleteDrumSequence(Long drumSequenceId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("drumseq:" + drumSequenceId);
            logger.info("Deleted drum sequence {}", drumSequenceId);
            
            // Notify listeners
            commandBus.publish(Commands.PATTERN_REMOVED, this, drumSequenceId);
        } catch (Exception e) {
            logger.error("Error deleting drum sequence {}: {}", drumSequenceId, e.getMessage());
            throw new RuntimeException("Failed to delete drum sequence", e);
        }
    }

    /**
     * Create a new empty drum sequence
     */
    public DrumSequenceData newDrumSequence() {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Creating new drum sequence");
            DrumSequenceData data = new DrumSequenceData();
            data.setId(jedis.incr("seq:drumsequence"));
            
            // Set default values
            int[] patternLengths = new int[DRUM_PAD_COUNT];
            Arrays.fill(patternLengths, 16); // Default to 16 steps
            data.setPatternLengths(patternLengths);
            
            Direction[] directions = new Direction[DRUM_PAD_COUNT];
            Arrays.fill(directions, Direction.FORWARD); // Default to forward
            data.setDirections(directions);
            
            TimingDivision[] timings = new TimingDivision[DRUM_PAD_COUNT];
            Arrays.fill(timings, TimingDivision.NORMAL); // Default to normal
            data.setTimingDivisions(timings);
            
            boolean[] looping = new boolean[DRUM_PAD_COUNT];
            Arrays.fill(looping, true); // Default to looping
            data.setLoopingFlags(looping);
            
            int[] velocities = new int[DRUM_PAD_COUNT];
            Arrays.fill(velocities, 100); // Default velocity
            data.setVelocities(velocities);
            data.setOriginalVelocities(Arrays.copyOf(velocities, DRUM_PAD_COUNT));
            
            // Initialize patterns (all false)
            data.setPatterns(new boolean[DRUM_PAD_COUNT][MAX_STEPS]);
            
            // Save to Redis
            String json = objectMapper.writeValueAsString(data);
            jedis.set("drumseq:" + data.getId(), json);
            
            logger.info("Created new drum sequence with ID: {}", data.getId());
            return data;
        } catch (Exception e) {
            logger.error("Error creating new drum sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create new drum sequence", e);
        }
    }

    /**
     * Get the previous drum sequence ID
     */
    public Long getPreviousDrumSequenceId(Long currentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("drumseq:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id < currentId)
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Get the next drum sequence ID
     */
    public Long getNextDrumSequenceId(Long currentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("drumseq:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id > currentId)
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }
}