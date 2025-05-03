package com.angrysurfer.core.redis;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class SessionHelper {
    private static final Logger logger = LoggerFactory.getLogger(SessionHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final PlayerHelper playerHelper;
    private final CommandBus commandBus = CommandBus.getInstance();

    public SessionHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;

        // Configure ObjectMapper to handle empty beans and ignore unknown properties
        this.objectMapper = objectMapper;
        this.objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        this.playerHelper = new PlayerHelper(jedisPool, objectMapper);

        logger.info("SessionHelper initialized with configured ObjectMapper");
    }

    public Session findSessionById(Long id) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("session:" + id);
            if (json != null) {
                Session session = objectMapper.readValue(json, Session.class);

                // Initialize players set if null
                if (session.getPlayers() == null) {
                    session.setPlayers(new HashSet<>());
                }

                // Check for different player types
                String[] playerTypes = {"Strike", "Note"};
                
                for (String playerType : playerTypes) {
                    // Load players for this session (using a consistent key format)
                    String playerSetKey = "session:" + id + ":players:" + playerType.toLowerCase();
                    Set<String> playerIds = jedis.smembers(playerSetKey);
                    
                    if (!playerIds.isEmpty()) {
                        logger.info("Found {} {} players for session {}", playerIds.size(), playerType, id);
                        
                        for (String playerId : playerIds) {
                            Player player = playerHelper.findPlayerById(Long.parseLong(playerId), playerType);
                            if (player != null) {
                                player.setSession(session);
                                session.getPlayers().add(player);
                            }
                        }
                    }
                }

                logger.info(String.format("Loaded session %d with %d players", id, session.getPlayers().size()));
                return session;
            }
            return null;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error finding session: " + e.getMessage());
            throw new RuntimeException("Failed to find session", e);
        }
    }

    public void saveSession(Session session) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (session.getId() == null) {
                session.setId(jedis.incr("seq:session"));
            }

            // Clear existing player relationships for all types
            String strikePlayerSetKey = "session:" + session.getId() + ":players:strike";
            String notePlayerSetKey = "session:" + session.getId() + ":players:note";
            jedis.del(strikePlayerSetKey);
            jedis.del(notePlayerSetKey);

            // Save player relationships by type
            if (session.getPlayers() != null) {
                session.getPlayers().forEach(player -> {
                    String className = player.getClass().getSimpleName().toLowerCase();
                    String playerSetKey = "session:" + session.getId() + ":players:" + className;
                    jedis.sadd(playerSetKey, player.getId().toString());
                    
                    logger.debug("Added player {} of type {} to session {}", 
                        player.getId(), className, session.getId());
                });
            }

            // Temporarily remove circular references
            Set<Player> players = session.getPlayers();
            session.setPlayers(null);

            // Save session
            String json = objectMapper.writeValueAsString(session);
            jedis.set("session:" + session.getId(), json);

            // Restore references
            session.setPlayers(players);

            logger.info(String.format("Saved session %d with %d players",
                    session.getId(),
                    players != null ? players.size() : 0));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error saving session: " + e.getMessage());
            throw new RuntimeException("Failed to save session", e);
        }
    }

    public List<Long> getAllSessionIds() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            List<Long> ids = new ArrayList<>();
            for (String key : keys) {
                try {
                    ids.add(Long.parseLong(key.split(":")[1]));
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(null, "Invalid session key: " + key);
                }
            }
            return ids;
        }
    }

    public Long getMinimumSessionId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[1]))
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    public Long getMaximumSessionId() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            return keys.stream()
                    .map(key -> Long.parseLong(key.split(":")[1]))
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    public void deleteSession(Long sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Session session = findSessionById(sessionId);
            if (session == null)
                return;

            // Delete all players in the session
            if (session.getPlayers() != null) {
                session.getPlayers().forEach(player -> playerHelper.deletePlayer(player));
            }

            // Delete the session itself
            jedis.del("session:" + sessionId);

            // Notify via command bus
            commandBus.publish(Commands.SESSION_DELETED, this, sessionId);
            logger.info("Successfully deleted session " + sessionId + " and all related entities");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error deleting session " + sessionId + ": " + e.getMessage());
            throw new RuntimeException("Failed to delete session", e);
        }
    }

    public Session newSession() {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Creating new session");
            Session session = new Session();
            session.setId(jedis.incr("seq:session"));

            // Set default values
            session.setTempoInBPM(120F);
            session.setTicksPerBeat(24);
            session.setBeatsPerBar(4);
            session.setBars(4);
            session.setParts(16);

            // Initialize players set
            session.setPlayers(new HashSet<>());

            // Save the new session
            saveSession(session);
            logger.info("Created new session with ID: " + session.getId());
            return session;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error creating new session: " + e.getMessage());
            throw new RuntimeException("Failed to create new session", e);
        }
    }

    public Long getPreviousSessionId(Session session) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id < session.getId())
                    .max(Long::compareTo)
                    .orElse(null);
        }
    }

    public Long getNextSessionId(Session session) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            return keys.stream()
                    .map(k -> Long.parseLong(k.split(":")[1]))
                    .filter(id -> id > session.getId())
                    .min(Long::compareTo)
                    .orElse(null);
        }
    }

    public void clearInvalidSessions() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            for (String key : keys) {
                Session session = findSessionById(Long.parseLong(key.split(":")[1]));
                if (session != null && !session.isValid()) {
                    deleteSession(session.getId());
                }
            }
        }
    }

    public boolean sessionExists(Long sessionId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists("session:" + sessionId);
        }
    }

    public Session findFirstValidSession() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("session:*");
            for (String key : keys) {
                Session session = findSessionById(Long.parseLong(key.split(":")[1]));
                if (session != null && session.isValid()) {
                    return session;
                }
            }
            return null;
        }
    }

    /**
     * Find the session containing a specific player
     */
    public Session findSessionForPlayer(Player player) {
        if (player == null || player.getId() == null) {
            return null;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            // Determine player type from player class
            String playerType = player.getClass().getSimpleName().toLowerCase();
            logger.debug("Finding session for player {} of type {}", player.getId(), playerType);
            
            Set<String> sessionKeys = jedis.keys("session:*");
            for (String sessionKey : sessionKeys) {
                if (!sessionKey.contains(":players")) {
                    String sessionId = sessionKey.split(":")[1];
                    String playersKey = "session:" + sessionId + ":players:" + playerType;
                    
                    if (jedis.sismember(playersKey, player.getId().toString())) {
                        logger.info("Found session {} for player {} of type {}", 
                                   sessionId, player.getId(), playerType);
                        return findSessionById(Long.valueOf(sessionId));
                    }
                }
            }
            
            logger.warn("No session found for player {} of type {}", player.getId(), playerType);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error finding session for player: " + e.getMessage());
        }
        return null;
    }


    /**
     * Helper method to add a player to a session
     */
    public void addPlayerToSession(Session session, Player player) {
        if (session == null || player == null) {
            logger.warn("Cannot add player to session: null reference");
            return;
        }

        try {
            // Set up relationships
            player.setSession(session);
            if (session.getPlayers() == null) {
                session.setPlayers(new HashSet<>());
            }
            session.getPlayers().add(player);

            // Save both entities
            playerHelper.savePlayer(player);
            saveSession(session);

            logger.info("Successfully added player " + player.getId() +
                    " (" + player.getName() + ") to session " + session.getId());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error adding player to session: " + e.getMessage());
            throw new RuntimeException("Failed to add player to session", e);
        }
    }

}

    //  public static void main(String[] args) {
    //     // Setup logging
    //     System.out.println("=== SessionHelper Test ===");

    //     // Initialize Redis connection and ObjectMapper
    //     JedisPool jedisPool = null;
    //     try {
    //         // Create Redis connection pool
    //         System.out.println("Connecting to Redis...");
    //         jedisPool = new JedisPool("localhost", 6379);
    //         ObjectMapper objectMapper = new ObjectMapper();
    //         objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    //         objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    //         // Create helpers
    //         SessionHelper sessionHelper = new SessionHelper(jedisPool, objectMapper);
    //         PlayerHelper playerHelper = sessionHelper.getPlayerHelper();
    //         InstrumentHelper instrumentHelper = new InstrumentHelper(jedisPool, objectMapper);

    //         // Test 1: Create sessions
    //         System.out.println("\n--- Test 1: Create sessions ---");
    //         Session session1 = sessionHelper.newSession();
    //         session1.setName("Test Session 1");
    //         sessionHelper.saveSession(session1);
    //         System.out.println("Created session 1: " + session1.getId() + " - " + session1.getName());

    //         Session session2 = sessionHelper.newSession();
    //         session2.setName("Test Session 2");
    //         sessionHelper.saveSession(session2);
    //         System.out.println("Created session 2: " + session2.getId() + " - " + session2.getName());

    //         // Test 2: Create instruments
    //         System.out.println("\n--- Test 2: Create instruments ---");
    //         // Melodic instrument
    //         InstrumentWrapper melodicInstrument = new InstrumentWrapper();
    //         melodicInstrument.setName("Test Piano " + System.currentTimeMillis());
    //         melodicInstrument.setDeviceName("Test Device");
    //         melodicInstrument.setChannel(0);
    //         melodicInstrument.setLowestNote(36);
    //         melodicInstrument.setHighestNote(96);
    //         melodicInstrument.setAvailable(true);
    //         instrumentHelper.saveInstrument(melodicInstrument);
    //         System.out.println("Created melodic instrument: " + melodicInstrument.getName() +
    //                 " (ID: " + melodicInstrument.getId() + ")");

    //         // Percussion instrument
    //         InstrumentWrapper percussionInstrument = new InstrumentWrapper();
    //         percussionInstrument.setName("Test Drum Kit " + System.currentTimeMillis());
    //         percussionInstrument.setDeviceName("Test Device");
    //         percussionInstrument.setChannel(9); // Drum channel
    //         percussionInstrument.setLowestNote(36);
    //         percussionInstrument.setHighestNote(60);
    //         percussionInstrument.setAvailable(true);
    //         instrumentHelper.saveInstrument(percussionInstrument);
    //         System.out.println("Created percussion instrument: " + percussionInstrument.getName() +
    //                 " (ID: " + percussionInstrument.getId() + ")");

    //         // Test 3: Create players and add them to sessions
    //         System.out.println("\n--- Test 3: Create players and add to sessions ---");
    //         // Create a Note player for session 1
    //         Note notePlayer = new Note("Test Note Player", session1, null, 60, null);
    //         notePlayer.setId(playerHelper.getNextPlayerId());
    //         notePlayer.setRules(new HashSet<>());
    //         notePlayer.setMinVelocity(60);
    //         notePlayer.setMaxVelocity(100);
    //         notePlayer.setLevel(80);
    //         notePlayer.setChannel(0);
    //         notePlayer.setInstrumentId(melodicInstrument.getId()); // Assign instrument
    //         playerHelper.savePlayer(notePlayer);
    //         System.out.println("Created Note player: " + notePlayer.getName() +
    //                 " (ID: " + notePlayer.getId() + ") with instrument: " +
    //                 melodicInstrument.getName());

    //         // Create a Strike player for session 2
    //         Strike strikePlayer = new Strike("Test Strike Player", session2, null, 36, null);
    //         strikePlayer.setId(playerHelper.getNextPlayerId());
    //         strikePlayer.setRules(new HashSet<>());
    //         strikePlayer.setChannel(9);
    //         strikePlayer.setInstrumentId(percussionInstrument.getId()); // Assign instrument
    //         playerHelper.savePlayer(strikePlayer);
    //         System.out.println("Created Strike player: " + strikePlayer.getName() +
    //                 " (ID: " + strikePlayer.getId() + ") with instrument: " +
    //                 percussionInstrument.getName());

    //         // Add players to sessions
    //         sessionHelper.addPlayerToSession(session1, notePlayer);
    //         sessionHelper.addPlayerToSession(session2, strikePlayer);

    //         // Test 4: Session retrieval and validation of relationships
    //         System.out.println("\n--- Test 4: Test session retrieval with relationships ---");
    //         // Load session 1 and verify player and instrument
    //         Session loadedSession1 = sessionHelper.findSessionById(session1.getId());
    //         if (loadedSession1 != null) {
    //             System.out.println("Loaded session 1: " + loadedSession1.getName() +
    //                     " (ID: " + loadedSession1.getId() + ")");
    //             System.out.println("Session 1 has " + loadedSession1.getPlayers().size() + " players");

    //             for (Player player : loadedSession1.getPlayers()) {
    //                 System.out.println("  Player: " + player.getName() + " (ID: " + player.getId() + ")");
    //                 if (player.getInstrumentId() != null) {
    //                     InstrumentWrapper instrument = instrumentHelper.findInstrumentById(player.getInstrumentId());
    //                     if (instrument != null) {
    //                         System.out.println("    Instrument: " + instrument.getName() +
    //                                 " (ID: " + instrument.getId() + ")");
    //                     } else {
    //                         System.out.println("    ERROR: Could not find player's instrument with ID: " +
    //                                 player.getInstrumentId());
    //                     }
    //                 }
    //             }
    //         } else {
    //             JOptionPane.showMessageDialog(null,  "ERROR: Failed to load session 1");
    //         }

    //         // Load session 2 and verify player and instrument
    //         Session loadedSession2 = sessionHelper.findSessionById(session2.getId());
    //         if (loadedSession2 != null) {
    //             System.out.println("Loaded session 2: " + loadedSession2.getName() +
    //                     " (ID: " + loadedSession2.getId() + ")");
    //             System.out.println("Session 2 has " + loadedSession2.getPlayers().size() + " players");

    //             for (Player player : loadedSession2.getPlayers()) {
    //                 System.out.println("  Player: " + player.getName() + " (ID: " + player.getId() + ")");
    //                 if (player.getInstrumentId() != null) {
    //                     InstrumentWrapper instrument = instrumentHelper.findInstrumentById(player.getInstrumentId());
    //                     if (instrument != null) {
    //                         System.out.println("    Instrument: " + instrument.getName() +
    //                                 " (ID: " + instrument.getId() + ")");
    //                     } else {
    //                         System.out.println("    ERROR: Could not find player's instrument with ID: " +
    //                                 player.getInstrumentId());
    //                     }
    //                 }
    //             }
    //         } else {
    //             JOptionPane.showMessageDialog(null,  "ERROR: Failed to load session 2");
    //         }

    //         // Test 5: Test session navigation methods
    //         System.out.println("\n--- Test 5: Test session navigation ---");
    //         Long minSessionId = sessionHelper.getMinimumSessionId();
    //         Long maxSessionId = sessionHelper.getMaximumSessionId();
    //         System.out.println("Minimum session ID: " + minSessionId);
    //         System.out.println("Maximum session ID: " + maxSessionId);

    //         Long nextId = sessionHelper.getNextSessionId(session1);
    //         Long prevId = sessionHelper.getPreviousSessionId(session2);
    //         System.out.println("Next session after session 1: " + nextId);
    //         System.out.println("Previous session before session 2: " + prevId);

    //         // Test 6: Find session by player
    //         System.out.println("\n--- Test 6: Find session by player ---");
    //         Session foundSessionForPlayer = sessionHelper.findSessionForPlayer(notePlayer);
    //         if (foundSessionForPlayer != null && foundSessionForPlayer.getId().equals(session1.getId())) {
    //             System.out.println("SUCCESS: Correctly found session 1 for Note player");
    //         } else {
    //             JOptionPane.showMessageDialog(null,  "ERROR: Failed to find correct session for Note player");
    //         }

    //         // Test 7: Test getAllSessionIds
    //         System.out.println("\n--- Test 7: Get all session IDs ---");
    //         List<Long> allIds = sessionHelper.getAllSessionIds();
    //         System.out.println("All session IDs (" + allIds.size() + "): " + allIds);

    //         // Test 8: Test sessionExists
    //         System.out.println("\n--- Test 8: Test session existence ---");
    //         boolean session1Exists = sessionHelper.sessionExists(session1.getId());
    //         boolean invalidSessionExists = sessionHelper.sessionExists(999999L);
    //         System.out.println("Session 1 exists: " + session1Exists);
    //         System.out.println("Invalid session exists: " + invalidSessionExists);

    //         // Test 9: Find first valid session
    //         System.out.println("\n--- Test 9: Find first valid session ---");
    //         Session firstValid = sessionHelper.findFirstValidSession();
    //         if (firstValid != null) {
    //             System.out.println("First valid session: " + firstValid.getId() + " - " + firstValid.getName());
    //         } else {
    //             System.out.println("No valid sessions found");
    //         }

    //         // Test 10: Update a session
    //         System.out.println("\n--- Test 10: Update session ---");
    //         Session updateSession = sessionHelper.findSessionById(session1.getId());
    //         String oldName = updateSession.getName();
    //         String newName = oldName + " (Updated)";
    //         updateSession.setName(newName);
    //         sessionHelper.saveSession(updateSession);

    //         // Verify update
    //         Session afterUpdate = sessionHelper.findSessionById(session1.getId());
    //         if (afterUpdate != null && afterUpdate.getName().equals(newName)) {
    //             System.out.println("SUCCESS: Session name updated from '" + oldName +
    //                     "' to '" + afterUpdate.getName() + "'");
    //         } else {
    //             JOptionPane.showMessageDialog(null,  "ERROR: Session update failed");
    //         }

    //         // Test 11: Clear invalid sessions (for this test, mark session 2 as invalid and
    //         // clear)
    //         System.out.println("\n--- Test 11: Clear invalid sessions ---");
    //         try {
    //             // Make session2 invalid for testing
    //             Session invalidSession = sessionHelper.findSessionById(session2.getId());
    //             System.out.println("Session before marking invalid: " + invalidSession.getName());
                
    //             // Inspect the Session class first to find the correct field
    //             System.out.println("Available fields in Session class:");
    //             Field[] fields = Session.class.getDeclaredFields();
    //             for (Field field : fields) {
    //                 System.out.println(" - " + field.getName() + " (type: " + field.getType().getName() + ")");
    //             }
                
    //             // Try the most likely field names, in order of likelihood
    //             String[] possibleFieldNames = {"valid", "isValid", "_valid", "validity"};
    //             Field validField = null;
                
    //             for (String fieldName : possibleFieldNames) {
    //                 try {
    //                     validField = Session.class.getDeclaredField(fieldName);
    //                     System.out.println("Found field: " + fieldName);
    //                     break;
    //                 } catch (NoSuchFieldException e) {
    //                     System.out.println("Field not found: " + fieldName);
    //                 }
    //             }
                
    //             if (validField != null) {
    //                 // Use the found field
    //                 validField.setAccessible(true);
    //                 validField.set(invalidSession, Boolean.FALSE);
    //                 System.out.println("Set validity field to false");
    //             } else {
    //                 // Alternative approach - use a method that marks sessions as invalid if available
    //                 System.out.println("Attempting to call setValid(false) method");
    //                 try {
    //                     Method setValidMethod = Session.class.getMethod("setValid", boolean.class);
    //                     setValidMethod.invoke(invalidSession, Boolean.FALSE);
    //                     System.out.println("Called setValid(false) method");
    //                 } catch (Exception e) {
    //                     System.out.println("Failed to call setValid: " + e.getMessage());
                        
    //                     // Last resort - add a custom property to mark as invalid
    //                     System.out.println("Adding custom 'invalid' property to session");
    //                     invalidSession.setName(invalidSession.getName() + " [INVALID]");
    //                 }
    //             }
                
    //             // Save the modified session
    //             sessionHelper.saveSession(invalidSession);
    //             System.out.println("Saved modified session");
                
    //             // Print session details to verify modification
    //             Session checkedSession = sessionHelper.findSessionById(session2.getId());
    //             System.out.println("Session after modification: " + checkedSession.getName());
    //             System.out.println("Session isValid() returns: " + checkedSession.isValid());
                
    //             // Clear invalid sessions
    //             System.out.println("Clearing invalid sessions...");
    //             sessionHelper.clearInvalidSessions();
                
    //             // Verify session2 was deleted
    //             boolean session2ExistsAfterClear = sessionHelper.sessionExists(session2.getId());
    //             System.out.println("Session 2 exists after clearing invalid sessions: " + 
    //                     session2ExistsAfterClear);
    //         } catch (Exception e) {
    //             JOptionPane.showMessageDialog(null,  "ERROR during invalid session test: " + e.getMessage());
    //             e.printStackTrace();
    //         }

    //         // Test 12: Clean up test session 1
    //         System.out.println("\n--- Test 12: Clean up ---");
    //         sessionHelper.deleteSession(session1.getId());
    //         // Session 2 should already be deleted from previous test
    //         instrumentHelper.deleteInstrument(melodicInstrument.getId());
    //         instrumentHelper.deleteInstrument(percussionInstrument.getId());

    //         boolean cleanupSuccess = !sessionHelper.sessionExists(session1.getId()) &&
    //                 !sessionHelper.sessionExists(session2.getId());
    //         System.out.println("Cleanup " + (cleanupSuccess ? "successful" : "failed"));

    //         System.out.println("\n=== Tests completed ===");

    //     } catch (Exception e) {
    //         System.err.println("ERROR: Test failed with exception:");
    //         e.printStackTrace();
    //     } finally {
    //         // Clean up resources
    //         if (jedisPool != null) {
    //             jedisPool.close();
    //         }
    //     }
    // }