package com.angrysurfer.core.redis;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.*;

@Getter
@Setter
class DrumSequenceDataHelper {
    private static final Logger logger = LoggerFactory.getLogger(DrumSequenceDataHelper.class.getName());
    // Constants
    private static final int DRUM_PAD_COUNT = 16;
    private static final int MAX_STEPS = 64;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public DrumSequenceDataHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    /**
     * Find a drum sequence by ID
     */
    public DrumSequenceData findDrumSequenceById(Long id) throws JsonProcessingException {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("drumseq:" + id);
            if (json != null) {
                DrumSequenceData data = objectMapper.readValue(json, DrumSequenceData.class);
                logger.info("Loaded drum sequence {}", id);
                return data;
            }
            return null;
        } catch (JsonProcessingException e) {
            logger.error(e.getMessage());
            throw e;
        }
    }

    /**
     * Save a drum sequence
     */
    public void saveDrumSequence(DrumSequencer sequencer) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Create a data transfer object
            DrumSequenceData data = sequencer.getSequenceData();
            logger.debug(data.toString());
            // Set or generate ID
            if (data.getId() <= 0) {
                data.setId(jedis.incr("seq:drumsequence"));
            }

            // Copy instrument data for each drum
            for (int i = 0; i < SequencerConstants.DRUM_PAD_COUNT; i++) {
                Player player = sequencer.getPlayer(i);
                if (player != null && player.getInstrument() != null) {
                    InstrumentWrapper instrument = player.getInstrument();
                    data.getInstrumentIds()[i] = instrument.getId();
                    data.getSoundbankNames()[i] = instrument.getSoundBank();
                    data.getPresets()[i] = instrument.getPreset();
                    data.getBankIndices()[i] = instrument.getBankIndex();
                    data.getDeviceNames()[i] = instrument.getDeviceName();
                    data.getInstrumentNames()[i] = instrument.getName();
                }
                if (player != null)
                    data.getRootNotes()[i] = player.getRootNote();
            }


            // Save to Redis
            String json = objectMapper.writeValueAsString(data);
            jedis.set("drumseq:" + data.getId(), json);

            // Also store in the hash for faster lookup
            jedis.hset("drum-sequences", String.valueOf(data.getId()), json);

            logger.info("Saved drum sequence {}", data.getId());

            // Notify listeners
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_SAVED, this, data.getId());

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

            // Initialize root notes array with standard GM drum mapping
            int[] rootNotes = new int[DRUM_PAD_COUNT];
            for (int i = 0; i < DRUM_PAD_COUNT; i++) {
                rootNotes[i] = SequencerConstants.MIDI_DRUM_NOTE_OFFSET + i;
            }
            data.setRootNotes(rootNotes);

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

    /**
     * Get the minimum drum sequence ID
     *
     * @return The minimum ID, or null if no sequences exist
     */
    public Long getMinimumDrumSequenceId() {
        try (Jedis jedis = jedisPool.getResource()) {

            // Get all sequence keys
            Map<String, String> sequences = jedis.hgetAll("drum-sequences");

            if (sequences == null || sequences.isEmpty()) {
                logger.info("No drum sequences found in Redis");
                return null;
            }

            // Filter out non-numeric keys and find minimum ID
            return sequences.keySet().stream()
                    .filter(key -> key != null && !key.equals("null") && key.matches("\\d+")) // Ensure the key is a
                    // valid number
                    .map(key -> {
                        try {
                            return Long.parseLong(key);
                        } catch (NumberFormatException e) {
                            // This shouldn't happen due to the regex filter, but just in case
                            logger.warn("Failed to parse sequence ID '{}' as Long", key);
                            return null;
                        }
                    })
                    .filter(id -> id != null) // Filter out any nulls from failed parsing
                    .min(Long::compareTo)
                    .orElse(null); // Return null if no valid IDs found
        } catch (Exception e) {
            logger.error("Error getting minimum drum sequence ID: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get the maximum drum sequence ID
     *
     * @return The maximum ID, or null if no sequences exist
     */
    public Long getMaximumDrumSequenceId() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Get all sequence keys
            Map<String, String> sequences = jedis.hgetAll("drum-sequences");

            if (sequences == null || sequences.isEmpty()) {
                logger.info("No drum sequences found in Redis");
                return null;
            }

            // Filter out non-numeric keys and find maximum ID
            return sequences.keySet().stream()
                    .filter(key -> key != null && !key.equals("null") && key.matches("\\d+")) // Ensure the key is a
                    // valid number
                    .map(key -> {
                        try {
                            return Long.parseLong(key);
                        } catch (NumberFormatException e) {
                            // This shouldn't happen due to the regex filter, but just in case
                            logger.warn("Failed to parse sequence ID '{}' as Long", key);
                            return null;
                        }
                    })
                    .filter(id -> id != null) // Filter out any nulls from failed parsing
                    .max(Long::compareTo)
                    .orElse(null); // Return null if no valid IDs found
        } catch (Exception e) {
            logger.error("Error getting maximum drum sequence ID: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Delete a drum sequence by ID
     *
     * @param id The ID of the drum sequence to delete
     * @return true if successfully deleted, false otherwise
     */
    public boolean deleteDrumSequence(Long id) {
        if (id == null) {
            logger.warn("Cannot delete drum sequence with null ID");
            return false;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "drumseq:" + id;

            // Check if the sequence exists
            if (!jedis.exists(key)) {
                logger.warn("Drum sequence with ID {} not found", id);
                return false;
            }

            // Delete the sequence
            Long result = jedis.del(key);

            // Also remove from the hash if it exists there
            String hashKey = id.toString();
            if (jedis.hexists("drum-sequences", hashKey)) {
                jedis.hdel("drum-sequences", hashKey);
            }

            boolean success = result != null && result > 0;
            if (success) {
                logger.info("Successfully deleted drum sequence with ID {}", id);

                // Notify listeners
                CommandBus.getInstance().publish(Commands.DRUM_SEQUENCE_DELETED, this, id);
            } else {
                logger.warn("Failed to delete drum sequence with ID {}", id);
            }

            return success;
        } catch (Exception e) {
            logger.error("Error deleting drum sequence with ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete all drum sequences
     *
     * @return The number of sequences successfully deleted
     */
    public int deleteAllDrumSequences() {
        try (Jedis jedis = jedisPool.getResource()) {
            // Get all sequence keys
            Set<String> keys = jedis.keys("drumseq:*");
            if (keys == null || keys.isEmpty()) {
                logger.info("No drum sequences to delete");
                return 0;
            }

            int deletedCount = 0;
            for (String key : keys) {
                try {
                    // Extract the ID from the key
                    String idStr = key.split(":")[1];
                    Long id = Long.parseLong(idStr);

                    // Use existing delete method to ensure proper cleanup
                    if (deleteDrumSequence(id)) {
                        deletedCount++;
                    }
                } catch (Exception e) {
                    logger.error("Error deleting drum sequence with key {}: {}", key, e.getMessage(), e);
                }
            }

            // Also clear the hash if it exists
            jedis.del("drum-sequences");

            logger.info("Successfully deleted {} drum sequences", deletedCount);

            // Notify listeners that all sequences were deleted
            CommandBus.getInstance().publish(Commands.DRUM_SEQUENCES_ALL_DELETED, this, deletedCount);

            return deletedCount;
        } catch (Exception e) {
            logger.error("Error deleting all drum sequences: {}", e.getMessage(), e);
            return 0;
        }
    }
}