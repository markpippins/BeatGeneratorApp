package com.angrysurfer.core.redis;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Helper class for managing melodic sequences
 */
@Getter
@Setter
class MelodicSequencerHelper {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencerHelper.class);
    
    // Replace RedisService with direct access to required components
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    
    /**
     * Create a new helper with required dependencies
     * 
     * @param jedisPool Pool for Redis connections
     * @param objectMapper For JSON serialization
     */
    public MelodicSequencerHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Load a sequence into the sequencer
     */
    public MelodicSequenceData findMelodicSequenceById(Long id, Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "melseq:" + sequencerId + ":" + id;
            String json = jedis.get(key);
            
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
     * Apply loaded data to a MelodicSequencer
     */
    public void applyToSequencer(MelodicSequenceData data, MelodicSequencer sequencer) {
        if (data == null || sequencer == null) {
            logger.warn("Cannot apply null data or sequencer");
            return;
        }
        
        try {
            // Implementation details for applying data to sequencer
            sequencer.setMelodicSequenceId(data.getId());
            
            // Apply other properties from data to sequencer
            // ...
            
            logger.info("Applied melodic sequence data to sequencer");
        } catch (Exception e) {
            logger.error("Error applying melodic sequence data: " + e.getMessage(), e);
        }
    }
    
    /**
     * Save a melodic sequence
     */
    public void saveMelodicSequence(MelodicSequencer sequencer) {
        if (sequencer == null) {
            logger.warn("Cannot save null sequencer");
            return;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            MelodicSequenceData data = new MelodicSequenceData();
            
            // Set or generate ID
            if (sequencer.getMelodicSequenceId() == null || sequencer.getMelodicSequenceId() == 0) {
                data.setId(jedis.incr("seq:melodicsequence:" + sequencer.getId()));
                sequencer.setMelodicSequenceId(data.getId());
            } else {
                data.setId(sequencer.getMelodicSequenceId());
            }
            
            // Set sequencer ID
            data.setSequencerId(sequencer.getId());
            
            // Copy sequencer data to data object
            // ...
            
            // Save to Redis
            String key = "melseq:" + sequencer.getId() + ":" + data.getId();
            String json = objectMapper.writeValueAsString(data);
            jedis.set(key, json);
            
            logger.info("Saved melodic sequence {} for sequencer {}", 
                    data.getId(), sequencer.getId());
        } catch (Exception e) {
            logger.error("Error saving melodic sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to save melodic sequence", e);
        }
    }
    
    /**
     * Create a new melodic sequence
     */
    public MelodicSequenceData newMelodicSequence(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            MelodicSequenceData data = new MelodicSequenceData();
            data.setId(jedis.incr("seq:melodicsequence:" + sequencerId));
            data.setSequencerId(sequencerId);
            
            // Initialize with default values
            // ...
            
            // Save to Redis
            String key = "melseq:" + sequencerId + ":" + data.getId();
            String json = objectMapper.writeValueAsString(data);
            jedis.set(key, json);
            
            logger.info("Created new melodic sequence {} for sequencer {}", 
                    data.getId(), sequencerId);
            return data;
        } catch (Exception e) {
            logger.error("Error creating new melodic sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to create new melodic sequence", e);
        }
    }
    
    /**
     * Get all melodic sequence IDs for a sequencer
     */
    public List<Long> getAllMelodicSequenceIds(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[2]))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting melodic sequence IDs: " + e.getMessage(), e);
            return List.of();
        }
    }
    
    /**
     * Get minimum melodic sequence ID
     */
    public Long getMinimumMelodicSequenceId(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[2]))
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }
    
    /**
     * Get maximum melodic sequence ID
     */
    public Long getMaximumMelodicSequenceId(Integer sequencerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("melseq:" + sequencerId + ":*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[2]))
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
     * Delete a melodic sequence
     *
     * @param sequencerId The ID of the sequencer that owns the sequence
     * @param id The ID of the melodic sequence to delete
     */
    public void deleteMelodicSequence(Integer sequencerId, Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = "melseq:" + sequencerId + ":" + id;
            jedis.del(key);
            logger.info("Deleted melodic sequence {} for sequencer {}", id, sequencerId);
        } catch (Exception e) {
            logger.error("Error deleting melodic sequence: " + e.getMessage(), e);
            throw new RuntimeException("Failed to delete melodic sequence", e);
        }
    }
}