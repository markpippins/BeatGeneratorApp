package com.angrysurfer.core.redis;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.event.MelodicSequencerEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.*;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.util.MelodicSequenceDataDeserializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class MelodicSequenceDataHelper {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceDataHelper.class);
    // Constants
    private static final int MAX_STEPS = 16;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public MelodicSequenceDataHelper(JedisPool jedisPool) {
        this.jedisPool = jedisPool;
        this.objectMapper = new ObjectMapper();
        configureObjectMapper();  // Add this line
    }

    private void configureObjectMapper() {
        // Register custom deserializer for MelodicSequenceData
        SimpleModule module = new SimpleModule();
        module.addDeserializer(MelodicSequenceData.class, new MelodicSequenceDataDeserializer());
        this.objectMapper.registerModule(module);

        // Enable more tolerant deserialization
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);
    }

    /**
     * Find a melodic sequence by ID and sequencer ID
     *
     * @param id          The sequence ID
     * @param sequencerId The sequencer instance ID
     * @return The melodic sequence data or null if not found
     */
    public MelodicSequenceData findMelodicSequenceById(Long id, Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = null;
            String source = "unknown";

            // Try first with the newer key format
            json = jedis.get("melodicseq:" + sequencerId + ":" + id);
            if (json != null) source = "melodicseq key";

            // If not found, try the older key format
            if (json == null) {
                json = jedis.get("melseq:" + sequencerId + ":" + id);
                if (json != null) source = "melseq key";
            }

            // If still not found, try looking up in the hash for faster lookup
            if (json == null) {
                json = jedis.hget("melodic-sequences:" + sequencerId, String.valueOf(id));
                if (json != null) source = "hash table";
            }

            if (json != null) {
                // Log a portion of the JSON to debug
                logger.debug("Found melodic sequence from {}, JSON excerpt: {}",
                        source, json.length() > 100 ? json.substring(0, 100) + "..." : json);

                try {
                    MelodicSequenceData data = objectMapper.readValue(json, MelodicSequenceData.class);
                    logger.info("Loaded melodic sequence {} for sequencer {} with instrument: {} ({})",
                            id, sequencerId,
                            data.getInstrumentName(),
                            data.getPreset() != null ? "Preset " + data.getPreset() : "No preset");
                    return data;
                } catch (Exception e) {
                    logger.error("Error deserializing melodic sequence: " + e.getMessage(), e);
                    // Return null instead of throwing to make the API more robust
                    return null;
                }
            }

            // If no sequence was found, return null instead of throwing an exception
            logger.warn("No melodic sequence found for id {} and sequencer {}", id, sequencerId);
            return null;
        } catch (Exception e) {
            logger.error("Error finding melodic sequence: " + e.getMessage(), e);
            // Consider returning null instead of throwing to make the API more robust
            return null;
        }
    }

    /**
     * Apply loaded data to a MelodicSequencer including instrument settings
     */
    public void applyToSequencer(MelodicSequenceData data, MelodicSequencer sequencer) {
        if (data == null || sequencer == null) {
            logger.warn("Cannot apply null data or sequencer");
            return;
        }

        try {
            // Set basic sequence ID
            // sequencer.setId(data.getId());

            // Apply pattern length
            sequencer.getSequenceData().setPatternLength(data.getPatternLength());

            // Apply direction
            sequencer.getSequenceData().setDirection(data.getDirection());

            // Apply timing division
            sequencer.getSequenceData().setTimingDivision(data.getTimingDivision());

            // Apply looping flag
            sequencer.setLooping(data.isLooping());

            // Apply octave shift
            sequencer.getSequenceData().setOctaveShift(data.getOctaveShift());

            // Apply quantization settings
            sequencer.getSequenceData().setQuantizeEnabled(data.isQuantizeEnabled());
            // sequencer.setRootNote(data.getRootNote());
            sequencer.getSequenceData().setScale(data.getScale());

            // Apply steps data
            sequencer.getSequenceData().clearPattern();

            // Apply active steps
            List<Boolean> activeSteps = data.getActiveSteps();
            if (activeSteps != null) {
                for (int i = 0; i < Math.min(activeSteps.size(), MAX_STEPS); i++) {
                    if (activeSteps.get(i)) {
                        // Get corresponding note, velocity, and gate values
                        int noteValue = (i < data.getNoteValues().size()) ? data.getNoteValues().get(i) : 60;
                        int velocityValue = (i < data.getVelocityValues().size()) ? data.getVelocityValues().get(i)
                                : 100;
                        int gateValue = (i < data.getGateValues().size()) ? data.getGateValues().get(i) : 50;

                        // Set step data
                        sequencer.setStepData(i, true, noteValue, velocityValue, gateValue);
                    }
                }
            }

            // Apply harmonic tilt values
            List<Integer> tiltValues = data.getHarmonicTiltValues();
            if (tiltValues != null && !tiltValues.isEmpty()) {
                logger.info("Applying {} harmonic tilt values: {}", tiltValues.size(), tiltValues);
                sequencer.setHarmonicTiltValues(tiltValues);
            } else {
                logger.warn("No harmonic tilt values found in sequence data");

                // Initialize with defaults if missing
                int[] defaultTiltValues = new int[sequencer.getSequenceData().getPatternLength()];
                sequencer.getSequenceData().setHarmonicTiltValues(defaultTiltValues);
                logger.info("Initialized default tilt values");
            }

            // Apply instrument settings to the player with better error handling
            if (sequencer.getPlayer() != null) {
                Player player = sequencer.getPlayer();

                if (player.getInstrument() != null) {
                    InstrumentWrapper instrument = player.getInstrument();

                    // Log the before state
                    logger.info("Before applying - Instrument settings: soundbank='{}', bank={}, preset={}",
                            instrument.getSoundbankName(), instrument.getBankIndex(), instrument.getPreset());

                    // Log what we're loading from saved data
                    logger.info("Loaded values from data: soundbank='{}', bank={}, preset={}",
                            data.getSoundbankName(), data.getBankIndex(), data.getPreset());

                    // Only apply if values are present and valid
                    boolean settingsChanged = false;

                    if (data.getSoundbankName() != null && !data.getSoundbankName().isEmpty()) {
                        instrument.setSoundbankName(data.getSoundbankName());
                        settingsChanged = true;
                    }

                    if (data.getBankIndex() != null && data.getBankIndex() >= 0) {
                        instrument.setBankIndex(data.getBankIndex());
                        settingsChanged = true;
                    }

                    if (data.getPreset() != null && data.getPreset() >= 0) {
                        instrument.setPreset(data.getPreset());
                        settingsChanged = true;
                    }

                    // Device name and other identifiers
                    if (data.getDeviceName() != null && !data.getDeviceName().isEmpty()) {
                        instrument.setDeviceName(data.getDeviceName());
                    }

                    if (data.getInstrumentName() != null && !data.getInstrumentName().isEmpty()) {
                        instrument.setName(data.getInstrumentName());
                    }

                    logger.info("After applying - Instrument settings: soundbank='{}', bank={}, preset={}",
                            instrument.getSoundbankName(), instrument.getBankIndex(), instrument.getPreset());

                    // Only send MIDI program changes if settings actually changed
                    if (settingsChanged) {
                        try {
                            logger.info("Applying instrument preset changes to MIDI device");
                            PlayerManager.getInstance().applyInstrumentPreset(player);

                            // Verify the changes were applied
                            logger.info("Verified final settings: soundbank='{}', bank={}, preset={}",
                                    player.getInstrument().getSoundbankName(),
                                    player.getInstrument().getBankIndex(),
                                    player.getInstrument().getPreset());

                        } catch (Exception e) {
                            logger.error("Error applying program change: {}", e.getMessage(), e);
                        }
                    } else {
                        logger.info("No instrument settings changes needed");
                    }
                } else {
                    logger.warn("Player has no instrument to apply settings to");
                }
            } else {
                logger.warn("No player available to apply instrument settings");
            }

            // Notify that pattern has updated
            CommandBus.getInstance().publish(
                    Commands.MELODIC_SEQUENCE_LOADED,
                    this,
                    new MelodicSequencerEvent(sequencer.getId(), data.getId()));

        } catch (Exception e) {
            logger.error("Error applying sequence data: " + e.getMessage(), e);
        }
    }

    /**
     * Save a melodic sequence
     */
    public void saveMelodicSequence(MelodicSequencer sequencer) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Create a data transfer object
            MelodicSequenceData data = sequencer.getSequenceData();

            // Set or generate ID
            if (data.getId() <= 0) {
                data.setId(jedis.incr("seq:melodicsequence"));
            }

            // Save instrument settings with improved logging
            Player player = sequencer.getPlayer();
            if (player != null && player.getInstrument() != null) {
                InstrumentWrapper instrument = player.getInstrument();

                // Store all instrument information
                data.setSoundbankName(instrument.getSoundbankName());
                data.setPreset(instrument.getPreset());
                data.setBankIndex(instrument.getBankIndex());
                data.setDeviceName(instrument.getDeviceName());
                data.setInstrumentId(instrument.getId());
                data.setInstrumentName(instrument.getName());

                // Detailed logging to confirm values are set
                logger.info("Saving instrument settings: soundbank='{}', bank={}, preset={}, device='{}', name='{}'",
                        data.getSoundbankName(),
                        data.getBankIndex(),
                        data.getPreset(),
                        data.getDeviceName(),
                        data.getInstrumentName());
            } else {
                logger.warn("No instrument available to save for sequence {}", data.getId());
            }

            // Save to Redis
            String json = objectMapper.writeValueAsString(data);

            // Debug log the JSON to see if instrument data is included
            logger.debug("Saving melodic sequence JSON (excerpt): {}",
                    json.length() > 300 ? json.substring(0, 300) + "..." : json);

            // Save to both storage formats
            jedis.set("melodicseq:" + sequencer.getId() + ":" + data.getId(), json);
            jedis.hset("melodic-sequences:" + sequencer.getId(), String.valueOf(data.getId()), json);

            logger.info("Saved melodic sequence {} for sequencer {} with instrument settings",
                    data.getId(), sequencer.getId());

            // Notify listeners
            CommandBus.getInstance().publish(Commands.MELODIC_SEQUENCE_SAVED, this,
                    Map.of("sequencerId", sequencer.getId(), "sequenceId", data.getId()));

        } catch (Exception e) {
            logger.error("Error saving melodic sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to save melodic sequence", e);
        }
    }

    /**
     * Get all melodic sequence IDs for a specific sequencer
     */
    public List<Long> getAllMelodicSequenceIds(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            List<Long> ids = new ArrayList<>();
            for (String key : keys) {
                try {
                    ids.add(Long.parseLong(key.split(":")[2]));
                } catch (NumberFormatException e) {
                    logger.error("Invalid melodic sequence key: " + key);
                }
            }
            return ids;
        }
    }

    /**
     * Get the minimum melodic sequence ID
     */
    public Long getMinimumMelodicSequenceId(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[2]))
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Get the maximum melodic sequence ID
     */
    public Long getMaximumMelodicSequenceId(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[2]))
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Delete a melodic sequence
     */
    public void deleteMelodicSequence(Integer sequencerId, Long melSequenceId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("melseq:" + sequencerId + ":" + melSequenceId);
            logger.info("Deleted melodic sequence {} for sequencer {}", melSequenceId, sequencerId);

            // Notify listeners
            CommandBus.getInstance().publish(
                    Commands.MELODIC_SEQUENCE_REMOVED,
                    this,
                    new MelodicSequencerEvent(sequencerId, melSequenceId));
        } catch (Exception e) {
            logger.error("Error deleting melodic sequence {}: {}", melSequenceId, e.getMessage());
            throw new RuntimeException("Failed to delete melodic sequence", e);
        }
    }

    /**
     * Create a new empty melodic sequence
     */
    public MelodicSequenceData newMelodicSequence(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Creating new melodic sequence for sequencer {}", sequencerId);
            MelodicSequenceData data = new MelodicSequenceData();
            data.setId(jedis.incr("seq:melsequence:" + sequencerId));
            data.setSequencerId(sequencerId);

            // Set default values
            data.setPatternLength(16);
            data.setDirection(Direction.FORWARD);
            data.setTimingDivision(TimingDivision.NORMAL);
            data.setLooping(true);
            data.setOctaveShift(0);

            // Default quantization settings
            data.setQuantizeEnabled(true);
            data.setRootNote(60);
            data.setScale(Scale.SCALE_CHROMATIC);

            // Initialize pattern data with arrays and SET THEM on the data object
            boolean[] activeSteps = new boolean[16];
            int[] noteValues = new int[16];
            int[] velocityValues = new int[16];
            int[] gateValues = new int[16];
            int[] harmonicTiltValues = new int[16]; // Create tilt values array

            // Initialize values
            for (int i = 0; i < 16; i++) {
                activeSteps[i] = false;
                noteValues[i] = 60 + (i % 12);
                velocityValues[i] = 100;
                gateValues[i] = 50;
                harmonicTiltValues[i] = 0; // Default to no tilt
            }

            // SET ALL arrays on the data object
            data.setActiveSteps(activeSteps);
            data.setNoteValues(noteValues);
            data.setVelocityValues(velocityValues);
            data.setGateValues(gateValues);
            data.setHarmonicTiltValues(harmonicTiltValues); // Don't forget this line!

            // Save to Redis
            String json = objectMapper.writeValueAsString(data);
            jedis.set("melseq:" + sequencerId + ":" + data.getId(), json);

            logger.info("Created new melodic sequence with ID: {} for sequencer {}", data.getId(), sequencerId);
            return data;
        } catch (Exception e) {
            logger.error("Error creating new melodic sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create new melodic sequence", e);
        }
    }

    /**
     * Get the previous melodic sequence ID
     */
    public Long getPreviousMelodicSequenceId(Integer sequencerId, Long currentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[2]))
                    .filter(id -> id < currentId)
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    /**
     * Get the next melodic sequence ID
     */
    public Long getNextMelodicSequenceId(Integer sequencerId, Long currentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[2]))
                    .filter(id -> id > currentId)
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

}