package com.angrysurfer.beats.diagnostic;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.swing.JOptionPane;

import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.redis.InstrumentHelper;
import com.angrysurfer.core.redis.PlayerHelper;
import com.angrysurfer.core.redis.SessionHelper;
import com.angrysurfer.core.redis.UserConfigHelper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class RedisDiagnosticsHelper {

    public static String testName = "Redis Diagnostic Test";
    public static String testDescription = "This class is used to run diagnostics on the Redis database and its helpers.";
    /**
     * Main method to test InstrumentHelper functionality
     */
    public static void main(String[] args) {
        
        runUserConfigDiagnostics();
        runInstrumentDiagnostics();
        runPlayerDiagnostics();
        runSessionDiagnostics();
    }

    public static void showError(String testName, String message) {
        JOptionPane.showMessageDialog(null, testName + "\n\nERROR: " + message);
    }

    /**
     * Main method to test UserConfigHelper functionality
     */
    public static void runUserConfigDiagnostics() {
        // Setup logging
        testName = "UserConfigHelper Test";
        System.out.println("=== UserConfigHelper Test ===");

        // Initialize Redis connection and ObjectMapper
        JedisPool jedisPool = null;
        try {
            // Create Redis connection pool
            System.out.println("Connecting to Redis...");
            jedisPool = new JedisPool("localhost", 6379);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

            // Create UserConfigHelper instance
            UserConfigHelper configHelper = new UserConfigHelper(jedisPool, objectMapper);

            // Test 1: Create a test configuration
            System.out.println("\n--- Test 1: Create test configuration ---");
            UserConfig testConfig = new UserConfig();
            testConfig.setName("Test Config " + System.currentTimeMillis());
            testConfig.setInstruments(new ArrayList<>());
            testConfig.setConfigs(new HashSet<>());
            testConfig.setLastUpdated(new Date());

            // Add a test instrument
            InstrumentWrapper testInstrument = new InstrumentWrapper();
            testInstrument.setId(1L);
            testInstrument.setName("Test Instrument");
            testInstrument.setDeviceName("Test Device");
            testInstrument.setChannel(0);
            testInstrument.setAvailable(true);
            testConfig.getInstruments().add(testInstrument);

            System.out.println("Created test config: " + testConfig.getName());
            System.out.println("With instrument: " + testInstrument.getName());

            // Test 2: Save to Redis
            System.out.println("\n--- Test 2: Save to Redis ---");
            configHelper.saveConfig(testConfig);
            System.out.println("Saved config to Redis");

            // Test 3: Load from Redis
            System.out.println("\n--- Test 3: Load from Redis ---");
            UserConfig loadedFromRedis = configHelper.loadConfigFromRedis();
            if (loadedFromRedis != null) {
                System.out.println("Successfully loaded config from Redis: " + loadedFromRedis.getName());
                System.out.println("Instrument count: " + loadedFromRedis.getInstruments().size());
                if (!loadedFromRedis.getInstruments().isEmpty()) {
                    InstrumentWrapper loadedInstrument = loadedFromRedis.getInstruments().get(0);
                    System.out.println("Loaded instrument: " + loadedInstrument.getName());
                }
            } else {
                showError(testName, "ERROR: Failed to load config from Redis");
            }

            // Test 4: Save to JSON file
            System.out.println("\n--- Test 4: Save to JSON file ---");
            String testFilePath = System.getProperty("user.home") + "/test_config.json";
            boolean saved = configHelper.saveConfigToJSON(testConfig, testFilePath);
            System.out.println("Saved to JSON file: " + (saved ? "SUCCESS" : "FAILED"));
            System.out.println("File location: " + testFilePath);

            // Test 5: Load from JSON file
            System.out.println("\n--- Test 5: Load from JSON file ---");
            UserConfig loadedFromFile = configHelper.loadConfigFromJSON(testFilePath);
            if (loadedFromFile != null) {
                System.out.println("Successfully loaded config from file: " + loadedFromFile.getName());
                System.out.println("Instrument count: " + loadedFromFile.getInstruments().size());
                if (!loadedFromFile.getInstruments().isEmpty()) {
                    InstrumentWrapper loadedInstrument = loadedFromFile.getInstruments().get(0);
                    System.out.println("Loaded instrument: " + loadedInstrument.getName());
                }
            } else {
                showError(testName, "ERROR: Failed to load config from file");
            }

            // Test 6: Modify and save updated config
            System.out.println("\n--- Test 6: Update configuration ---");
            if (loadedFromRedis != null) {
                // Modify config
                String oldName = loadedFromRedis.getName();
                String newName = oldName + " (Updated)";
                loadedFromRedis.setName(newName);

                // Add another instrument
                InstrumentWrapper newInstrument = new InstrumentWrapper();
                newInstrument.setId(2L);
                newInstrument.setName("Second Test Instrument");
                newInstrument.setDeviceName("Another Device");
                newInstrument.setChannel(1);
                newInstrument.setAvailable(true);
                loadedFromRedis.getInstruments().add(newInstrument);

                System.out.println("Updated config name from '" + oldName + "' to '" + newName + "'");
                System.out.println("Added instrument: " + newInstrument.getName());

                // Save to both Redis and JSON
                configHelper.saveConfig(loadedFromRedis);
                configHelper.saveConfigToJSON(loadedFromRedis, testFilePath);
                System.out.println("Saved updated config to Redis and JSON");
            }

            // Test 7: Verify updates in both storage systems
            System.out.println("\n--- Test 7: Verify updates ---");
            UserConfig redisUpdated = configHelper.loadConfigFromRedis();
            UserConfig fileUpdated = configHelper.loadConfigFromJSON(testFilePath);

            if (redisUpdated != null) {
                System.out.println("Redis - Updated name: " + redisUpdated.getName());
                System.out.println("Redis - Instrument count: " + redisUpdated.getInstruments().size());
            }

            if (fileUpdated != null) {
                System.out.println("File - Updated name: " + fileUpdated.getName());
                System.out.println("File - Instrument count: " + fileUpdated.getInstruments().size());
            }

            // Test 8: Test validation and migration
            System.out.println("\n--- Test 8: Test validation and migration ---");
            UserConfig incompleteConfig = new UserConfig();
            incompleteConfig.setName("Incomplete Config");
            // Deliberately don't set other fields

            // Save and load to test validation
            configHelper.saveConfigToJSON(incompleteConfig, testFilePath + ".incomplete");
            UserConfig validatedConfig = configHelper.loadConfigFromJSON(testFilePath + ".incomplete");

            if (validatedConfig != null) {
                System.out.println("Validated config instrument list is null: " +
                        (validatedConfig.getInstruments() == null));
                System.out.println("Validated config configs set is null: " +
                        (validatedConfig.getConfigs() == null));
                System.out.println("Validated config has lastUpdated: " +
                        (validatedConfig.getLastUpdated() != null));
            }

            // Test 9: Clean up
            System.out.println("\n--- Test 9: Clean up ---");
            // Delete the test files
            File testFile = new File(testFilePath);
            File incompleteFile = new File(testFilePath + ".incomplete");

            boolean testFileDeleted = testFile.delete();
            boolean incompleteFileDeleted = incompleteFile.delete();

            System.out.println("Deleted test file: " + (testFileDeleted ? "SUCCESS" : "FAILED"));
            System.out.println("Deleted incomplete file: " + (incompleteFileDeleted ? "SUCCESS" : "FAILED"));

            // Optional: clear the Redis key
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.del("userconfig");
                System.out.println("Cleared Redis userconfig key");
            }

            System.out.println("\n=== Tests completed ===");

        } catch (Exception e) {
            System.err.println("ERROR: Test failed with exception:");
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (jedisPool != null) {
                jedisPool.close();
            }
        }
    }
    public static void runPlayerDiagnostics() {
        // Setup logging
        testName = "PlayerHelper Test";
        System.out.println("=== PlayerHelper Test ===");

        // Initialize Redis connection and ObjectMapper
        JedisPool jedisPool = null;
        try {
            // Create Redis connection pool
            System.out.println("Connecting to Redis...");
            jedisPool = new JedisPool("localhost", 6379);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            // Create helpers
            PlayerHelper playerHelper = new PlayerHelper(jedisPool, objectMapper);
            InstrumentHelper instrumentHelper = new InstrumentHelper(jedisPool, objectMapper);
            SessionHelper sessionHelper = new SessionHelper(jedisPool, objectMapper);

            // Test 1: Create a test session
            System.out.println("\n--- Test 1: Create a test session ---");
            Session testSession = new Session();
            testSession.setName("Test Session " + System.currentTimeMillis());
            testSession.setPlayers(new HashSet<>());
            sessionHelper.saveSession(testSession);
            System.out.println("Created session: " + testSession.getName() + " (ID: " + testSession.getId() + ")");

            // Test 2: Create test players (Note and Strike)
            System.out.println("\n--- Test 2: Create test players ---");
            // Note player
            Note notePlayer = new Note("Test Note Player", testSession, null, 60, null);
            notePlayer.setId(playerHelper.getNextPlayerId());
            notePlayer.setRules(new HashSet<>());
            notePlayer.setMinVelocity(60);
            notePlayer.setMaxVelocity(100);
            notePlayer.setLevel(80);
            notePlayer.setChannel(0);
            playerHelper.savePlayer(notePlayer);
            System.out.println("Created Note player: " + notePlayer.getName() + " (ID: " + notePlayer.getId() + ")");

            // Strike player
            Strike strikePlayer = new Strike("Test Strike Player", testSession, null, 36, null);
            strikePlayer.setId(playerHelper.getNextPlayerId());
            strikePlayer.setRules(new HashSet<>());
            strikePlayer.setChannel(9); // Typically channel 10 (index 9) for drum sounds
            playerHelper.savePlayer(strikePlayer);
            System.out.println(
                    "Created Strike player: " + strikePlayer.getName() + " (ID: " + strikePlayer.getId() + ")");

            // Test 3: Create test instruments
            System.out.println("\n--- Test 3: Create test instruments ---");
            // For Note player
            InstrumentWrapper melodicInstrument = new InstrumentWrapper();
            melodicInstrument.setName("Test Piano " + System.currentTimeMillis());
            melodicInstrument.setDeviceName("Test Device");
            melodicInstrument.setChannel(0); // Same as Note player's channel
            melodicInstrument.setLowestNote(36);
            melodicInstrument.setHighestNote(96);
            melodicInstrument.setAvailable(true);
            instrumentHelper.saveInstrument(melodicInstrument);
            System.out.println("Created melodic instrument: " + melodicInstrument.getName() +
                    " (ID: " + melodicInstrument.getId() + ")");

            // For Strike player
            InstrumentWrapper percussionInstrument = new InstrumentWrapper();
            percussionInstrument.setName("Test Drum Kit " + System.currentTimeMillis());
            percussionInstrument.setDeviceName("Test Device");
            percussionInstrument.setChannel(9); // Drum channel
            percussionInstrument.setLowestNote(36);
            percussionInstrument.setHighestNote(60);
            percussionInstrument.setAvailable(true);
            instrumentHelper.saveInstrument(percussionInstrument);
            System.out.println("Created percussion instrument: " + percussionInstrument.getName() +
                    " (ID: " + percussionInstrument.getId() + ")");

            // Test 4: Attach instruments to players
            System.out.println("\n--- Test 4: Attach instruments to players ---");
            // Assign to Note player
            notePlayer.setInstrumentId(melodicInstrument.getId());
            playerHelper.savePlayer(notePlayer);
            System.out.println("Assigned instrument " + melodicInstrument.getName() +
                    " to player " + notePlayer.getName());

            // Assign to Strike player
            strikePlayer.setInstrumentId(percussionInstrument.getId());
            playerHelper.savePlayer(strikePlayer);
            System.out.println("Assigned instrument " + percussionInstrument.getName() +
                    " to player " + strikePlayer.getName());

            // Test 5: Retrieve players and verify instrument relationships
            System.out.println("\n--- Test 5: Retrieve players and verify instrument relationships ---");
            Player retrievedNotePlayer = playerHelper.findPlayerById(notePlayer.getId(), "Note");
            Player retrievedStrikePlayer = playerHelper.findPlayerById(strikePlayer.getId(), "Strike");

            if (retrievedNotePlayer != null) {
                System.out.println("Retrieved Note player: " + retrievedNotePlayer.getName());
                if (retrievedNotePlayer.getInstrumentId() != null &&
                        retrievedNotePlayer.getInstrumentId().equals(melodicInstrument.getId())) {
                    System.out.println("SUCCESS: Note player correctly linked to instrument " +
                            melodicInstrument.getId());
                } else {
                    JOptionPane.showMessageDialog(null, "ERROR: Note player has wrong instrument ID: " +
                            retrievedNotePlayer.getInstrumentId());
                }
            } else {
                JOptionPane.showMessageDialog(null, "ERROR: Failed to retrieve Note player");
            }

            if (retrievedStrikePlayer != null) {
                System.out.println("Retrieved Strike player: " + retrievedStrikePlayer.getName());
                if (retrievedStrikePlayer.getInstrumentId() != null &&
                        retrievedStrikePlayer.getInstrumentId().equals(percussionInstrument.getId())) {
                    System.out.println("SUCCESS: Strike player correctly linked to instrument " +
                            percussionInstrument.getId());
                } else {
                    JOptionPane.showMessageDialog(null, "ERROR: Strike player has wrong instrument ID: " +
                            retrievedStrikePlayer.getInstrumentId());
                }
            } else {
                JOptionPane.showMessageDialog(null, "ERROR: Failed to retrieve Strike player");
            }

            // Test 6: Create new instances of players with same IDs and load them
            System.out.println("\n--- Test 6: Create new player instances with same IDs and load them ---");
            Note newNoteInstance = new Note();
            newNoteInstance.setId(notePlayer.getId());
            newNoteInstance = (Note) playerHelper.findPlayerById(newNoteInstance.getId(), "Note");

            Strike newStrikeInstance = new Strike();
            newStrikeInstance.setId(strikePlayer.getId());
            newStrikeInstance = (Strike) playerHelper.findPlayerById(newStrikeInstance.getId(), "Strike");

            // Verify instrument relationships persisted
            if (newNoteInstance != null) {
                System.out.println("Loaded new Note player instance: " + newNoteInstance.getName());
                if (newNoteInstance.getInstrumentId() != null &&
                        newNoteInstance.getInstrumentId().equals(melodicInstrument.getId())) {
                    System.out.println("SUCCESS: New Note instance correctly linked to instrument " +
                            melodicInstrument.getId());

                    // Verify instrument can be loaded
                    InstrumentWrapper loadedInstrument = instrumentHelper.findInstrumentById(
                            newNoteInstance.getInstrumentId());
                    if (loadedInstrument != null) {
                        System.out.println("SUCCESS: Loaded instrument: " + loadedInstrument.getName());
                    } else {
                        JOptionPane.showMessageDialog(null, "ERROR: Could not load instrument");
                    }
                } else {
                    JOptionPane.showMessageDialog(null, "ERROR: New Note instance has wrong instrument ID: " +
                            newNoteInstance.getInstrumentId());
                }
            } else {
                JOptionPane.showMessageDialog(null, "ERROR: Failed to load new Note instance");
            }

            if (newStrikeInstance != null) {
                System.out.println("Loaded new Strike player instance: " + newStrikeInstance.getName());
                if (newStrikeInstance.getInstrumentId() != null &&
                        newStrikeInstance.getInstrumentId().equals(percussionInstrument.getId())) {
                    System.out.println("SUCCESS: New Strike instance correctly linked to instrument " +
                            percussionInstrument.getId());
                } else {
                    JOptionPane.showMessageDialog(null, "ERROR: New Strike instance has wrong instrument ID: " +
                            newStrikeInstance.getInstrumentId());
                }
            } else {
                JOptionPane.showMessageDialog(null, "ERROR: Failed to load new Strike instance");
            }

            // Test 7: Test player helper methods (newNote and newStrike)
            System.out.println("\n--- Test 7: Test helper factory methods ---");
            Player factoryNote = playerHelper.newNote();
            System.out.println("Created note via factory method: " + factoryNote.getName() +
                    " (ID: " + factoryNote.getId() + ")");

            Player factoryStrike = playerHelper.newStrike();
            System.out.println("Created strike via factory method: " + factoryStrike.getName() +
                    " (ID: " + factoryStrike.getId() + ")");

            // Test 8: Clean up
            System.out.println("\n--- Test 8: Clean up ---");
            playerHelper.deletePlayer(notePlayer);
            playerHelper.deletePlayer(strikePlayer);
            playerHelper.deletePlayer(factoryNote);
            playerHelper.deletePlayer(factoryStrike);
            instrumentHelper.deleteInstrument(melodicInstrument.getId());
            instrumentHelper.deleteInstrument(percussionInstrument.getId());
            sessionHelper.deleteSession(testSession.getId());
            System.out.println("Cleaned up all test data");

            System.out.println("\n=== Tests completed ===");

        } catch (Exception e) {
            System.err.println("ERROR: Test failed with exception:");
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (jedisPool != null) {
                jedisPool.close();
            }
        }
    }

    public static void runSessionDiagnostics() {
        // Setup logging
        testName = "SessionHelper Test";
        System.out.println("=== SessionHelper Test ===");

        // Initialize Redis connection and ObjectMapper
        JedisPool jedisPool = null;
        try {
            // Create Redis connection pool
            System.out.println("Connecting to Redis...");
            jedisPool = new JedisPool("localhost", 6379);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

            // Create helpers
            SessionHelper sessionHelper = new SessionHelper(jedisPool, objectMapper);
            PlayerHelper playerHelper = sessionHelper.getPlayerHelper();
            InstrumentHelper instrumentHelper = new InstrumentHelper(jedisPool, objectMapper);

            // Test 1: Create sessions
            System.out.println("\n--- Test 1: Create sessions ---");
            Session session1 = sessionHelper.newSession();
            session1.setName("Test Session 1");
            sessionHelper.saveSession(session1);
            System.out.println("Created session 1: " + session1.getId() + " - " + session1.getName());

            Session session2 = sessionHelper.newSession();
            session2.setName("Test Session 2");
            sessionHelper.saveSession(session2);
            System.out.println("Created session 2: " + session2.getId() + " - " + session2.getName());

            // Test 2: Create instruments
            System.out.println("\n--- Test 2: Create instruments ---");
            // Melodic instrument
            InstrumentWrapper melodicInstrument = new InstrumentWrapper();
            melodicInstrument.setName("Test Piano " + System.currentTimeMillis());
            melodicInstrument.setDeviceName("Test Device");
            melodicInstrument.setChannel(0);
            melodicInstrument.setLowestNote(36);
            melodicInstrument.setHighestNote(96);
            melodicInstrument.setAvailable(true);
            instrumentHelper.saveInstrument(melodicInstrument);
            System.out.println("Created melodic instrument: " + melodicInstrument.getName() +
                    " (ID: " + melodicInstrument.getId() + ")");

            // Percussion instrument
            InstrumentWrapper percussionInstrument = new InstrumentWrapper();
            percussionInstrument.setName("Test Drum Kit " + System.currentTimeMillis());
            percussionInstrument.setDeviceName("Test Device");
            percussionInstrument.setChannel(9); // Drum channel
            percussionInstrument.setLowestNote(36);
            percussionInstrument.setHighestNote(60);
            percussionInstrument.setAvailable(true);
            instrumentHelper.saveInstrument(percussionInstrument);
            System.out.println("Created percussion instrument: " + percussionInstrument.getName() +
                    " (ID: " + percussionInstrument.getId() + ")");

            // Test 3: Create players and add them to sessions
            System.out.println("\n--- Test 3: Create players and add to sessions ---");
            // Create a Note player for session 1
            Note notePlayer = new Note("Test Note Player", session1, null, 60, null);
            notePlayer.setId(playerHelper.getNextPlayerId());
            notePlayer.setRules(new HashSet<>());
            notePlayer.setMinVelocity(60);
            notePlayer.setMaxVelocity(100);
            notePlayer.setLevel(80);
            notePlayer.setChannel(0);
            notePlayer.setInstrumentId(melodicInstrument.getId()); // Assign instrument
            playerHelper.savePlayer(notePlayer);
            System.out.println("Created Note player: " + notePlayer.getName() +
                    " (ID: " + notePlayer.getId() + ") with instrument: " +
                    melodicInstrument.getName());

            // Create a Strike player for session 2
            Strike strikePlayer = new Strike("Test Strike Player", session2, null, 36, null);
            strikePlayer.setId(playerHelper.getNextPlayerId());
            strikePlayer.setRules(new HashSet<>());
            strikePlayer.setChannel(9);
            strikePlayer.setInstrumentId(percussionInstrument.getId()); // Assign instrument
            playerHelper.savePlayer(strikePlayer);
            System.out.println("Created Strike player: " + strikePlayer.getName() +
                    " (ID: " + strikePlayer.getId() + ") with instrument: " +
                    percussionInstrument.getName());

            // Add players to sessions
            sessionHelper.addPlayerToSession(session1, notePlayer);
            sessionHelper.addPlayerToSession(session2, strikePlayer);

            // Test 4: Session retrieval and validation of relationships
            System.out.println("\n--- Test 4: Test session retrieval with relationships ---");
            // Load session 1 and verify player and instrument
            Session loadedSession1 = sessionHelper.findSessionById(session1.getId());
            if (loadedSession1 != null) {
                System.out.println("Loaded session 1: " + loadedSession1.getName() +
                        " (ID: " + loadedSession1.getId() + ")");
                System.out.println("Session 1 has " + loadedSession1.getPlayers().size() + " players");

                for (Player player : loadedSession1.getPlayers()) {
                    System.out.println("  Player: " + player.getName() + " (ID: " + player.getId() + ")");
                    if (player.getInstrumentId() != null) {
                        InstrumentWrapper instrument = instrumentHelper.findInstrumentById(player.getInstrumentId());
                        if (instrument != null) {
                            System.out.println("    Instrument: " + instrument.getName() +
                                    " (ID: " + instrument.getId() + ")");
                        } else {
                            System.out.println("    ERROR: Could not find player's instrument with ID: " +
                                    player.getInstrumentId());
                        }
                    }
                }
            } else {
                showError(testName, "ERROR: Failed to load session 1");
            }

            // Load session 2 and verify player and instrument
            Session loadedSession2 = sessionHelper.findSessionById(session2.getId());
            if (loadedSession2 != null) {
                System.out.println("Loaded session 2: " + loadedSession2.getName() +
                        " (ID: " + loadedSession2.getId() + ")");
                System.out.println("Session 2 has " + loadedSession2.getPlayers().size() + " players");

                for (Player player : loadedSession2.getPlayers()) {
                    System.out.println("  Player: " + player.getName() + " (ID: " + player.getId() + ")");
                    if (player.getInstrumentId() != null) {
                        InstrumentWrapper instrument = instrumentHelper.findInstrumentById(player.getInstrumentId());
                        if (instrument != null) {
                            System.out.println("    Instrument: " + instrument.getName() +
                                    " (ID: " + instrument.getId() + ")");
                        } else {
                            System.out.println("    ERROR: Could not find player's instrument with ID: " +
                                    player.getInstrumentId());
                        }
                    }
                }
            } else {
                showError(testName, "ERROR: Failed to load session 2");
            }

            // Test 5: Test session navigation methods
            System.out.println("\n--- Test 5: Test session navigation ---");
            Long minSessionId = sessionHelper.getMinimumSessionId();
            Long maxSessionId = sessionHelper.getMaximumSessionId();
            System.out.println("Minimum session ID: " + minSessionId);
            System.out.println("Maximum session ID: " + maxSessionId);

            Long nextId = sessionHelper.getNextSessionId(session1);
            Long prevId = sessionHelper.getPreviousSessionId(session2);
            System.out.println("Next session after session 1: " + nextId);
            System.out.println("Previous session before session 2: " + prevId);

            // Test 6: Find session by player
            System.out.println("\n--- Test 6: Find session by player ---");
            Session foundSessionForPlayer = sessionHelper.findSessionForPlayer(notePlayer);
            if (foundSessionForPlayer != null && foundSessionForPlayer.getId().equals(session1.getId())) {
                System.out.println("SUCCESS: Correctly found session 1 for Note player");
            } else {
                showError(testName, "ERROR: Failed to find correct session for Note player");
            }

            // Test 7: Test getAllSessionIds
            System.out.println("\n--- Test 7: Get all session IDs ---");
            List<Long> allIds = sessionHelper.getAllSessionIds();
            System.out.println("All session IDs (" + allIds.size() + "): " + allIds);

            // Test 8: Test sessionExists
            System.out.println("\n--- Test 8: Test session existence ---");
            boolean session1Exists = sessionHelper.sessionExists(session1.getId());
            boolean invalidSessionExists = sessionHelper.sessionExists(999999L);
            System.out.println("Session 1 exists: " + session1Exists);
            System.out.println("Invalid session exists: " + invalidSessionExists);

            // Test 9: Find first valid session
            System.out.println("\n--- Test 9: Find first valid session ---");
            Session firstValid = sessionHelper.findFirstValidSession();
            if (firstValid != null) {
                System.out.println("First valid session: " + firstValid.getId() + " - " + firstValid.getName());
            } else {
                System.out.println("No valid sessions found");
            }

            // Test 10: Update a session
            System.out.println("\n--- Test 10: Update session ---");
            Session updateSession = sessionHelper.findSessionById(session1.getId());
            String oldName = updateSession.getName();
            String newName = oldName + " (Updated)";
            updateSession.setName(newName);
            sessionHelper.saveSession(updateSession);

            // Verify update
            Session afterUpdate = sessionHelper.findSessionById(session1.getId());
            if (afterUpdate != null && afterUpdate.getName().equals(newName)) {
                System.out.println("SUCCESS: Session name updated from '" + oldName +
                        "' to '" + afterUpdate.getName() + "'");
            } else {
                showError(testName, "ERROR: Session update failed");
            }

            // Test 11: Clear invalid sessions (for this test, mark session 2 as invalid and
            // clear)
            System.out.println("\n--- Test 11: Clear invalid sessions ---");
            try {
                // Make session2 invalid for testing
                Session invalidSession = sessionHelper.findSessionById(session2.getId());
                System.out.println("Session before marking invalid: " + invalidSession.getName());
                
                // Inspect the Session class first to find the correct field
                System.out.println("Available fields in Session class:");
                Field[] fields = Session.class.getDeclaredFields();
                for (Field field : fields) {
                    System.out.println(" - " + field.getName() + " (type: " + field.getType().getName() + ")");
                }
                
                // Try the most likely field names, in order of likelihood
                String[] possibleFieldNames = {"valid", "isValid", "_valid", "validity"};
                Field validField = null;
                
                for (String fieldName : possibleFieldNames) {
                    try {
                        validField = Session.class.getDeclaredField(fieldName);
                        System.out.println("Found field: " + fieldName);
                        break;
                    } catch (NoSuchFieldException e) {
                        System.out.println("Field not found: " + fieldName);
                    }
                }
                
                if (validField != null) {
                    // Use the found field
                    validField.setAccessible(true);
                    validField.set(invalidSession, Boolean.FALSE);
                    System.out.println("Set validity field to false");
                } else {
                    // Alternative approach - use a method that marks sessions as invalid if available
                    System.out.println("Attempting to call setValid(false) method");
                    try {
                        Method setValidMethod = Session.class.getMethod("setValid", boolean.class);
                        setValidMethod.invoke(invalidSession, Boolean.FALSE);
                        System.out.println("Called setValid(false) method");
                    } catch (Exception e) {
                        System.out.println("Failed to call setValid: " + e.getMessage());
                        
                        // Last resort - add a custom property to mark as invalid
                        System.out.println("Adding custom 'invalid' property to session");
                        invalidSession.setName(invalidSession.getName() + " [INVALID]");
                    }
                }
                
                // Save the modified session
                sessionHelper.saveSession(invalidSession);
                System.out.println("Saved modified session");
                
                // Print session details to verify modification
                Session checkedSession = sessionHelper.findSessionById(session2.getId());
                System.out.println("Session after modification: " + checkedSession.getName());
                System.out.println("Session isValid() returns: " + checkedSession.isValid());
                
                // Clear invalid sessions
                System.out.println("Clearing invalid sessions...");
                sessionHelper.clearInvalidSessions();
                
                // Verify session2 was deleted
                boolean session2ExistsAfterClear = sessionHelper.sessionExists(session2.getId());
                System.out.println("Session 2 exists after clearing invalid sessions: " + 
                        session2ExistsAfterClear);
            } catch (Exception e) {
                showError(testName, "ERROR during invalid session test: " + e.getMessage());
                e.printStackTrace();
            }

            // Test 12: Clean up test session 1
            System.out.println("\n--- Test 12: Clean up ---");
            sessionHelper.deleteSession(session1.getId());
            // Session 2 should already be deleted from previous test
            instrumentHelper.deleteInstrument(melodicInstrument.getId());
            instrumentHelper.deleteInstrument(percussionInstrument.getId());

            boolean cleanupSuccess = !sessionHelper.sessionExists(session1.getId()) &&
                    !sessionHelper.sessionExists(session2.getId());
            System.out.println("Cleanup " + (cleanupSuccess ? "successful" : "failed"));

            System.out.println("\n=== Tests completed ===");

        } catch (Exception e) {
            System.err.println("ERROR: Test failed with exception:");
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (jedisPool != null) {
                jedisPool.close();
            }
        }
    }

    public static void runInstrumentDiagnostics() {
        // Setup logging
        testName = "InstrumentHelper Test";
        System.out.println("=== InstrumentHelper Test ===");

        // Initialize Redis connection and ObjectMapper
        JedisPool jedisPool = null;
        try {
            // Create Redis connection pool
            System.out.println("Connecting to Redis...");
            jedisPool = new JedisPool("localhost", 6379);
            ObjectMapper objectMapper = new ObjectMapper();

            // Create InstrumentHelper instance
            InstrumentHelper helper = new InstrumentHelper(jedisPool, objectMapper);

            // Test 1: Save a new instrument
            System.out.println("\n--- Test 1: Save a new instrument ---");
            InstrumentWrapper newInstrument = new InstrumentWrapper();
            newInstrument.setName("Test Instrument " + System.currentTimeMillis());
            newInstrument.setDeviceName("Test Device");
            newInstrument.setChannel(0);
            newInstrument.setLowestNote(36);
            newInstrument.setHighestNote(84);
            newInstrument.setAvailable(true);

            System.out.println("Saving new instrument: " + newInstrument.getName());
            helper.saveInstrument(newInstrument);
            System.out.println("Saved with ID: " + newInstrument.getId());

            // Test 2: Retrieve the instrument by ID
            System.out.println("\n--- Test 2: Retrieve instrument by ID ---");
            Long savedId = newInstrument.getId();
            InstrumentWrapper retrieved = helper.findInstrumentById(savedId);

            if (retrieved != null) {
                System.out.println("Retrieved instrument: " + retrieved.getName() + " (ID: " + retrieved.getId() + ")");
                System.out.println("Device: " + retrieved.getDeviceName());
                System.out.println("Channel: " + retrieved.getChannel());
                System.out.println("Available: " + retrieved.getAvailable());
            } else {
                showError(testName, "ERROR: Failed to retrieve instrument with ID " + savedId);
            }

            // Test 3: Update the instrument
            System.out.println("\n--- Test 3: Update the instrument ---");
            if (retrieved != null) {
                String oldName = retrieved.getName();
                String newName = "Updated " + oldName;
                retrieved.setName(newName);

                System.out.println("Updating name from '" + oldName + "' to '" + newName + "'");
                helper.saveInstrument(retrieved);

                // Verify the update
                InstrumentWrapper afterUpdate = helper.findInstrumentById(savedId);
                if (afterUpdate != null && newName.equals(afterUpdate.getName())) {
                    System.out.println("Update successful! Name changed to: " + afterUpdate.getName());
                } else {
                    showError(testName, "ERROR: Update failed or verification failed");
                    if (afterUpdate != null) {
                        System.out.println("  Retrieved name: " + afterUpdate.getName() + " (expected: " + newName + ")");
                    }
                }
            }

            // Test 4: Find all instruments
            System.out.println("\n--- Test 4: Find all instruments ---");
            List<InstrumentWrapper> allInstruments = helper.findAllInstruments();
            System.out.println("Found " + allInstruments.size() + " instruments:");

            for (InstrumentWrapper instrument : allInstruments) {
                System.out.println(" - " + instrument.getId() + ": " + instrument.getName() +
                        " (" + instrument.getDeviceName() + ", Channel: " +
                        (instrument.getChannel() != null ? instrument.getChannel() + 1 : "N/A") + ")");
            }

            // Test 5: Delete the test instrument
            System.out.println("\n--- Test 5: Delete the test instrument ---");
            System.out.println("Deleting instrument with ID: " + savedId);
            helper.deleteInstrument(savedId);

            // Verify deletion
            InstrumentWrapper afterDelete = helper.findInstrumentById(savedId);
            if (afterDelete == null) {
                System.out.println("Deletion successful! Instrument with ID " + savedId + " no longer exists.");
            } else {
                showError(testName, "ERROR: Deletion failed. Instrument still exists: " + afterDelete.getName());
            }

            // Test 6: Edge cases
            System.out.println("\n--- Test 6: Edge cases ---");
            // Try to find a non-existent instrument
            Long nonExistentId = 999999L;
            InstrumentWrapper nonExistent = helper.findInstrumentById(nonExistentId);
            if (nonExistent == null) {
                System.out.println("Correctly returned null for non-existent ID " + nonExistentId);
            } else {
                showError(testName, "ERROR: Unexpectedly found instrument with ID " + nonExistentId);
            }

            System.out.println("\n=== Tests completed ===");

        } catch (Exception e) {
            System.err.println("ERROR: Test failed with exception:");
            e.printStackTrace();
        } finally {
            // Clean up resources
            if (jedisPool != null) {
                jedisPool.close();
            }
        }
    }
    
}
