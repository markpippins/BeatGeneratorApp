package com.angrysurfer.core.redis;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.swing.*;

@Getter
@Setter
public class InstrumentHelper {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;

    public InstrumentHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;

        // Configure ObjectMapper to ignore unknown properties
        // This is critical when the model evolves but old data exists
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        logger.info("InstrumentHelper initialized with ObjectMapper configured to ignore unknown fields");
    }

    public List<InstrumentWrapper> findAllInstruments() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys("instrument:*").stream()
                    .map(key -> findInstrumentById(Long.parseLong(key.split(":")[1])))
                    .filter(i -> i != null)
                    .collect(Collectors.toList());
        }
    }

    public InstrumentWrapper findInstrumentById(Long id) {
        if (id == null) {
            logger.warn("Attempted to find instrument with null ID");
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = "instrument:" + id;
            String json = jedis.get(key);

            if (json == null) {
                logger.debug("No instrument found with ID: {}", id);
                return null;
            }

            logger.debug("Retrieved JSON for instrument {}: {}", id, json);
            return objectMapper.readValue(json, InstrumentWrapper.class);
        } catch (Exception e) {
            logger.error("Error finding instrument with ID {}: {}", id, e.getMessage());
            // For debugging purposes, print the full stack trace
            e.printStackTrace();
            return null;
        }
    }

    public void saveInstrument(InstrumentWrapper instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (instrument.getId() == null) {
                instrument.setId(jedis.incr("seq:instrument"));
            }
            String json = objectMapper.writeValueAsString(instrument);
            jedis.set("instrument:" + instrument.getId(), json);
        } catch (Exception e) {
            logger.error("Error saving instrument: " + e.getMessage());
            throw new RuntimeException("Failed to save instrument", e);
        }
    }

    public void deleteInstrument(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("instrument:" + id);
        } catch (Exception e) {
            logger.error("Error deleting instrument: " + e.getMessage());
            throw new RuntimeException("Failed to delete instrument", e);
        }
    }
}
// /**
// * Main method to test InstrumentHelper functionality
// */
// public static void main(String[] args) {
// // Setup logging
// System.out.println("=== InstrumentHelper Test ===");

// // Initialize Redis connection and ObjectMapper
// JedisPool jedisPool = null;
// try {
// // Create Redis connection pool
// System.out.println("Connecting to Redis...");
// jedisPool = new JedisPool("localhost", 6379);
// ObjectMapper objectMapper = new ObjectMapper();

// // Create InstrumentHelper instance
// InstrumentHelper helper = new InstrumentHelper(jedisPool, objectMapper);

// // Test 1: Save a new instrument
// System.out.println("\n--- Test 1: Save a new instrument ---");
// InstrumentWrapper newInstrument = new InstrumentWrapper();
// newInstrument.setName("Test Instrument " + System.currentTimeMillis());
// newInstrument.setDeviceName("Test Device");
// newInstrument.setChannel(0);
// newInstrument.setLowestNote(36);
// newInstrument.setHighestNote(84);
// newInstrument.setAvailable(true);

// System.out.println("Saving new instrument: " + newInstrument.getName());
// helper.saveInstrument(newInstrument);
// System.out.println("Saved with ID: " + newInstrument.getId());

// // Test 2: Retrieve the instrument by ID
// System.out.println("\n--- Test 2: Retrieve instrument by ID ---");
// Long savedId = newInstrument.getId();
// InstrumentWrapper retrieved = helper.findInstrumentById(savedId);

// if (retrieved != null) {
// System.out.println("Retrieved instrument: " + retrieved.getName() + " (ID: "
// + retrieved.getId() + ")");
// System.out.println("Device: " + retrieved.getDeviceName());
// System.out.println("Channel: " + retrieved.getChannel());
// System.out.println("Available: " + retrieved.getAvailable());
// } else {
// JOptionPane.showMessageDialog(null, "ERROR: Failed to retrieve instrument
// with ID " + savedId);
// }

// // Test 3: Update the instrument
// System.out.println("\n--- Test 3: Update the instrument ---");
// if (retrieved != null) {
// String oldName = retrieved.getName();
// String newName = "Updated " + oldName;
// retrieved.setName(newName);

// System.out.println("Updating name from '" + oldName + "' to '" + newName +
// "'");
// helper.saveInstrument(retrieved);

// // Verify the update
// InstrumentWrapper afterUpdate = helper.findInstrumentById(savedId);
// if (afterUpdate != null && newName.equals(afterUpdate.getName())) {
// System.out.println("Update successful! Name changed to: " +
// afterUpdate.getName());
// } else {
// JOptionPane.showMessageDialog(null, "ERROR: Update failed or verification
// failed");
// if (afterUpdate != null) {
// System.out.println(" Retrieved name: " + afterUpdate.getName() + " (expected:
// " + newName + ")");
// }
// }
// }

// // Test 4: Find all instruments
// System.out.println("\n--- Test 4: Find all instruments ---");
// List<InstrumentWrapper> allInstruments = helper.findAllInstruments();
// System.out.println("Found " + allInstruments.size() + " instruments:");

// for (InstrumentWrapper instrument : allInstruments) {
// System.out.println(" - " + instrument.getId() + ": " + instrument.getName() +
// " (" + instrument.getDeviceName() + ", Channel: " +
// (instrument.getChannel() != null ? instrument.getChannel() + 1 : "N/A") +
// ")");
// }

// // Test 5: Delete the test instrument
// System.out.println("\n--- Test 5: Delete the test instrument ---");
// System.out.println("Deleting instrument with ID: " + savedId);
// helper.deleteInstrument(savedId);

// // Verify deletion
// InstrumentWrapper afterDelete = helper.findInstrumentById(savedId);
// if (afterDelete == null) {
// System.out.println("Deletion successful! Instrument with ID " + savedId + "
// no longer exists.");
// } else {
// JOptionPane.showMessageDialog(null, "ERROR: Deletion failed. Instrument still
// exists: " + afterDelete.getName());
// }

// // Test 6: Edge cases
// System.out.println("\n--- Test 6: Edge cases ---");
// // Try to find a non-existent instrument
// Long nonExistentId = 999999L;
// InstrumentWrapper nonExistent = helper.findInstrumentById(nonExistentId);
// if (nonExistent == null) {
// System.out.println("Correctly returned null for non-existent ID " +
// nonExistentId);
// } else {
// JOptionPane.showMessageDialog(null, "ERROR: Unexpectedly found instrument
// with ID " + nonExistentId);
// }

// System.out.println("\n=== Tests completed ===");

// } catch (Exception e) {
// System.err.println("ERROR: Test failed with exception:");
// e.printStackTrace();
// } finally {
// // Clean up resources
// if (jedisPool != null) {
// jedisPool.close();
// }
// }
// }
