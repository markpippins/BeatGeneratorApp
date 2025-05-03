package com.angrysurfer.core.redis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.PlayerManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Direction;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingDivision;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.swing.*;

@Getter
@Setter
public class MelodicSequenceDataHelper {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequenceDataHelper.class);
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final CommandBus commandBus = CommandBus.getInstance();

    // Constants
    private static final int MAX_STEPS = 16;

    public MelodicSequenceDataHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }

    /**
     * Find a melodic sequence by ID and sequencer ID
     * 
     * @param id          The sequence ID
     * @param sequencerId The sequencer instance ID
     * @return The melodic sequence data
     */
    public MelodicSequenceData findMelodicSequenceById(Long id, Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("melseq:" + sequencerId + ":" + id);
            if (json != null) {
                MelodicSequenceData data = objectMapper.readValue(json, MelodicSequenceData.class);
                logger.info("Loaded melodic sequence {} for sequencer {}", id, sequencerId);
                return data;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error finding melodic sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to find melodic sequence", e);
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
            sequencer.setPatternLength(data.getPatternLength());

            // Apply direction
            sequencer.setDirection(data.getDirection());

            // Apply timing division
            sequencer.setTimingDivision(data.getTimingDivision());

            // Apply looping flag
            sequencer.setLooping(data.isLooping());

            // Apply octave shift
            sequencer.setOctaveShift(data.getOctaveShift());

            // Apply quantization settings
            sequencer.setQuantizeEnabled(data.isQuantizeEnabled());
            // sequencer.setRootNote(data.getRootNote());
            sequencer.setScale(data.getScale());

            // Apply steps data
            sequencer.clearPattern();

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

            // NEW: Apply instrument settings to the player
            if (sequencer.getPlayer() != null && sequencer.getPlayer().getInstrument() != null) {
                Player player = sequencer.getPlayer();
                InstrumentWrapper instrument = player.getInstrument();
                
                // Only apply if values are present in the loaded data
                if (data.getSoundbankName() != null) {
                    instrument.setSoundbankName(data.getSoundbankName());
                }
                
                if (data.getBankIndex() != null) {
                    instrument.setBankIndex(data.getBankIndex());
                }
                
                if (data.getPreset() != null) {
                    player.getInstrument().setPreset(data.getPreset());
                }
                
                logger.info("Applied instrument settings: soundbank={}, bank={}, preset={}",
                    instrument.getSoundbankName(), instrument.getBankIndex(), instrument.getPreset());
                
                // Send MIDI program changes
                try {
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                    logger.info("Applied program change to instrument");
                } catch (Exception e) {
                    logger.error("Error applying program change: {}", e.getMessage(), e);
                }
            }

            // Notify that pattern has updated
            commandBus.publish(
                    Commands.MELODIC_SEQUENCE_LOADED,
                    this,
                    new MelodicSequencerEvent(sequencer.getId(), data.getId()));

        } catch (Exception e) {
            logger.error("Error applying sequence data: " + e.getMessage(), e);
        }
    }

    /**
     * Save a melodic sequence including instrument settings
     */
    public void saveMelodicSequence(MelodicSequencer sequencer) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Create a data transfer object
            MelodicSequenceData data = new MelodicSequenceData();

            // Set or generate ID
            if (sequencer.getId() == 0) {
                data.setId(jedis.incr("seq:melsequence:" + sequencer.getId()));
            } else {
                data.setSequencerId(sequencer.getId());
            }

            // Store the sequencer ID this belongs to
            data.setSequencerId(sequencer.getId());

            // Copy pattern parameters
            data.setPatternLength(sequencer.getPatternLength());
            data.setDirection(sequencer.getDirection());
            data.setTimingDivision(sequencer.getTimingDivision());
            data.setLooping(sequencer.isLooping());
            data.setOctaveShift(sequencer.getOctaveShift());

            // NEW: Save instrument settings from player
            if (sequencer.getPlayer() != null && sequencer.getPlayer().getInstrument() != null) {
                InstrumentWrapper instrument = sequencer.getPlayer().getInstrument();
                // Save soundbank, bank, and preset settings
                data.setSoundbankName(instrument.getSoundbankName());
                data.setBankIndex(instrument.getBankIndex());
                data.setPreset(instrument.getPreset());
                
                logger.info("Saving instrument settings: soundbank={}, bank={}, preset={}", 
                    data.getSoundbankName(), data.getBankIndex(), data.getPreset());
            }

            // Copy quantization settings
            data.setQuantizeEnabled(sequencer.isQuantizeEnabled());
            data.setScale(sequencer.getScale());

            // Copy pattern data (steps, notes, velocities, gates)
            // Convert List<Boolean> to boolean[]
            List<Boolean> activeStepsList = sequencer.getActiveSteps();
            boolean[] activeStepsArray = new boolean[activeStepsList.size()];
            for (int i = 0; i < activeStepsList.size(); i++) {
                activeStepsArray[i] = activeStepsList.get(i);
            }
            data.setActiveSteps(activeStepsArray);

            // Convert List<Integer> to int[]
            List<Integer> noteValuesList = sequencer.getNoteValues();
            int[] noteValuesArray = new int[noteValuesList.size()];
            for (int i = 0; i < noteValuesList.size(); i++) {
                noteValuesArray[i] = noteValuesList.get(i);
            }
            data.setNoteValues(noteValuesArray);

            // Convert List<Integer> to int[]
            List<Integer> velocityValuesList = sequencer.getVelocityValues();
            int[] velocityValuesArray = new int[velocityValuesList.size()];
            for (int i = 0; i < velocityValuesList.size(); i++) {
                velocityValuesArray[i] = velocityValuesList.get(i);
            }
            data.setVelocityValues(velocityValuesArray);

            // Convert List<Integer> to int[]
            List<Integer> gateValuesList = sequencer.getGateValues();
            int[] gateValuesArray = new int[gateValuesList.size()];
            for (int i = 0; i < gateValuesList.size(); i++) {
                gateValuesArray[i] = gateValuesList.get(i);
            }
            data.setGateValues(gateValuesArray);

            // Copy probability and nudge values if available
            if (sequencer.getProbabilityValues() != null) {
                List<Integer> probValuesList = sequencer.getProbabilityValues();
                int[] probValuesArray = new int[probValuesList.size()];
                for (int i = 0; i < probValuesList.size(); i++) {
                    probValuesArray[i] = probValuesList.get(i);
                }
                data.setProbabilityValues(probValuesArray);
            }

            if (sequencer.getNudgeValues() != null) {
                List<Integer> nudgeValuesList = sequencer.getNudgeValues();
                int[] nudgeValuesArray = new int[nudgeValuesList.size()];
                for (int i = 0; i < nudgeValuesList.size(); i++) {
                    nudgeValuesArray[i] = nudgeValuesList.get(i);
                }
                data.setNudgeValues(nudgeValuesArray);
            }

            // Copy harmonic tilt values
            if (sequencer.getHarmonicTiltValues() != null) {
                List<Integer> tiltValuesList = sequencer.getHarmonicTiltValues();
                int[] tiltValuesArray = new int[tiltValuesList.size()];
                for (int i = 0; i < tiltValuesList.size(); i++) {
                    tiltValuesArray[i] = tiltValuesList.get(i);
                }
                data.setHarmonicTiltValues(tiltValuesArray);
            }

            // Save to Redis
            String json = objectMapper.writeValueAsString(data);
            jedis.set("melseq:" + sequencer.getId() + ":" + data.getId(), json);

            logger.info("Saved melodic sequence {} for sequencer {}", data.getId(), sequencer.getId());

            // Notify listeners
            commandBus.publish(
                    Commands.MELODIC_SEQUENCE_SAVED,
                    this,
                    new MelodicSequencerEvent(sequencer.getId(), sequencer.getSequenceData().getId()));

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
            commandBus.publish(
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
            data.setRootNote("C");
            data.setScale("Major");

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

    /**
     * Class to hold sequencer ID and sequence ID for events
     */
    public static class MelodicSequencerEvent {
        private final Integer sequencerId;
        private final Long sequenceId;

        public MelodicSequencerEvent(Integer sequencerId, Long sequenceId) {
            this.sequencerId = sequencerId;
            this.sequenceId = sequenceId;
        }

        public Integer getSequencerId() {
            return sequencerId;
        }

        public Long getSequenceId() {
            return sequenceId;
        }
    }

    static void showError(String testName, String message) {
        JOptionPane.showMessageDialog(null, testName + "\n\nERROR: " + message);
    }

}