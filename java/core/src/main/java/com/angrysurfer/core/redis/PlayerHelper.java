package com.angrysurfer.core.redis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.service.SessionManager;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
public class PlayerHelper {
    private static final Logger logger = LoggerFactory.getLogger(PlayerHelper.class.getName());

    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final RuleHelper ruleHelper;

    public PlayerHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        this.ruleHelper = new RuleHelper(jedisPool, objectMapper);
    }

    private String getPlayerKey(String className, Long id) {
        return String.format("player:%s:%d", className.toLowerCase(), id);
    }

    public Player findPlayerById(Long id, String className) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Normalize the class name (capitalize first letter for consistency)
            String normalizedClassName = className;
            if (className != null && !className.isEmpty()) {
                normalizedClassName = className.substring(0, 1).toUpperCase() + className.substring(1).toLowerCase();
            }

            String json = jedis.get(getPlayerKey(normalizedClassName, id));
            if (json != null) {
                // Dynamically determine class based on normalized className parameter
                Class<? extends Player> playerClass;
                switch (normalizedClassName) {
                    case "Note":
                        playerClass = Note.class;
                        break;
                    case "Strike":
                        playerClass = Strike.class;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported player class: " + className);
                }

                // Use the correct class for deserialization
                Player player = objectMapper.readValue(json, playerClass);

                // Load rules for this player
                Set<String> ruleIds = jedis.smembers("player:" + id + ":rules");
                if (!ruleIds.isEmpty()) {
                    Set<Rule> rules = new HashSet<>();
                    for (String ruleId : ruleIds) {
                        String ruleJson = jedis.get("rule:" + ruleId);
                        if (ruleJson != null) {
                            Rule rule = objectMapper.readValue(ruleJson, Rule.class);
                            rules.add(rule);
                        }
                    }
                    player.setRules(rules);
                } else {
                    player.setRules(new HashSet<>());
                }

                logger.info(String.format("Loaded player %d with %d rules", id, player.getRules().size()));
                return player;
            }
            return null;
        } catch (Exception e) {
            logger.error("Error finding player: " + e.getMessage(), e);
            throw new RuntimeException("Failed to find player", e);
        }
    }

    public Set<Player> findPlayersForSession(Long sessionId, String className) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Player> players = new HashSet<>();
            String playersKey = String.format("session:%d:players:%s", sessionId, className);
            Set<String> playerIds = jedis.smembers(playersKey);

            for (String id : playerIds) {
                Player player = findPlayerById(Long.valueOf(id), className);
                if (player != null) {
                    players.add(player);
                }
            }
            return players;
        }
    }

    public Long nextPlayerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:player");
        } catch (Exception e) {
            logger.error("Error getting next player ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next player ID", e);
        }
    }

    static final int CACHE_SIZE = 50;

    public Long[] getCachedPlayerIds() {
        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < CACHE_SIZE; i++)
            ids.add(nextPlayerId());

        return ids.toArray(new Long[ids.size()]);

    }
    // public Long[] getPlayerIdsForSession() {
    // try (Jedis jedis = jedisPool.getResource()) {
    // Set<String> keys = jedis.keys("player:*");
    // Long[] ids = new Long[keys.size()];
    // int i = 0;
    // for (String key : keys) {
    // ids[i++] = Long.valueOf(key.split(":")[2]);
    // }
    // return ids;
    // } catch (Exception e) {
    // logger.error("Error getting player IDs: " + e.getMessage());
    // throw new RuntimeException("Failed to get player IDs", e);
    // }
    // }

    public void savePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (player.getId() == null) {
                player.setId(jedis.incr("seq:player"));
            }

            // Save rules first and maintain relationships
            String rulesKey = String.format("player:%d:rules", player.getId());
            jedis.del(rulesKey); // Clear existing rules

            if (player.getRules() != null) {
                for (Rule rule : player.getRules()) {
                    if (rule.getId() == null) {
                        rule.setId(jedis.incr("seq:rule"));
                    }
                    // Save rule
                    String ruleJson = objectMapper.writeValueAsString(rule);
                    jedis.set("rule:" + rule.getId(), ruleJson);
                    // Add to player's rules set
                    jedis.sadd(rulesKey, rule.getId().toString());
                }
            }

            // Store references before removing
            Set<Rule> rules = new HashSet<>(player.getRules() != null ? player.getRules() : new HashSet<>());
            Session session = player.getSession();

            // Temporarily remove circular references
            player.setSession(null);
            player.setRules(null);

            // Save the player
            String json = objectMapper.writeValueAsString(player);
            String playerKey = getPlayerKey(player.getPlayerClassName(), player.getId());
            jedis.set(playerKey, json);

            // Restore references
            player.setSession(session);
            player.setRules(rules);

            logger.info(String.format("Saved player %d with %d rules", player.getId(), rules.size()));
        } catch (Exception e) {
            logger.error("Error saving player: " + e.getMessage());
            throw new RuntimeException("Failed to save player", e);
        }
    }

    public void deletePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            // Remove player's rules
            if (player.getRules() != null) {
                player.getRules().forEach(rule -> ruleHelper.deleteRule(rule.getId()));
            }

            // Remove player from session's player set
            if (player.getSession() != null) {
                String playersKey = String.format("session:%d:players:%s",
                        player.getSession().getId(),
                        player.getPlayerClassName());
                jedis.srem(playersKey, player.getId().toString());
            }

            // Delete the player
            String key = getPlayerKey(player.getPlayerClassName(), player.getId());
            jedis.del(key);
        } catch (Exception e) {
            logger.error("Error deleting player: " + e.getMessage());
            throw new RuntimeException("Failed to delete player", e);
        }
    }

    public Long getNextPlayerId() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.incr("seq:player");
        } catch (Exception e) {
            logger.error("Error getting next player ID: " + e.getMessage());
            throw new RuntimeException("Failed to get next player ID", e);
        }
    }

    public void addPlayerToSession(Session session, Player player) {
        logger.info("Adding player " + player.getId() + " to session " + session.getId());

        try {
            // Set up relationships
            player.setSession(session);
            if (session.getPlayers() == null) {
                session.setPlayers(new HashSet<>());
            }
            session.getPlayers().add(player);

            // Save both entities
            savePlayer(player);

            logger.info("Successfully added player " + player.getId() +
                    " (" + player.getName() + ") to session " + session.getId());
        } catch (Exception e) {
            logger.error("Error adding player to session: " + e.getMessage());
            throw new RuntimeException("Failed to add player to session", e);
        }
    }

    public Player newNote() {
        Player player = new Note("Note", SessionManager.getInstance().getActiveSession(), null, 60, null);

        player.setId(getNextPlayerId());
        player.setRules(new HashSet<>()); // Ensure rules are initialized
        player.setMinVelocity(60);
        player.setMaxVelocity(127);
        player.setLevel(100);
        savePlayer(player);
        return player;

    }

    public Player newStrike() {
        Player player = new Strike("Strike", SessionManager.getInstance().getActiveSession(), null, 36, null);

        player.setId(getNextPlayerId());
        player.setRules(new HashSet<>()); // Ensure rules are initialized
        savePlayer(player);
        return player;
    }
}
    /**
     * Main method to test PlayerHelper functionality
     */
    // public static void main(String[] args) {
    //     // Setup logging
    //     System.out.println("=== PlayerHelper Test ===");

    //     // Initialize Redis connection and ObjectMapper
    //     JedisPool jedisPool = null;
    //     try {
    //         // Create Redis connection pool
    //         System.out.println("Connecting to Redis...");
    //         jedisPool = new JedisPool("localhost", 6379);
    //         ObjectMapper objectMapper = new ObjectMapper();
    //         objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    //         // Create helpers
    //         PlayerHelper playerHelper = new PlayerHelper(jedisPool, objectMapper);
    //         InstrumentHelper instrumentHelper = new InstrumentHelper(jedisPool, objectMapper);
    //         SessionHelper sessionHelper = new SessionHelper(jedisPool, objectMapper);

    //         // Test 1: Create a test session
    //         System.out.println("\n--- Test 1: Create a test session ---");
    //         Session testSession = new Session();
    //         testSession.setName("Test Session " + System.currentTimeMillis());
    //         testSession.setPlayers(new HashSet<>());
    //         sessionHelper.saveSession(testSession);
    //         System.out.println("Created session: " + testSession.getName() + " (ID: " + testSession.getId() + ")");

    //         // Test 2: Create test players (Note and Strike)
    //         System.out.println("\n--- Test 2: Create test players ---");
    //         // Note player
    //         Note notePlayer = new Note("Test Note Player", testSession, null, 60, null);
    //         notePlayer.setId(playerHelper.getNextPlayerId());
    //         notePlayer.setRules(new HashSet<>());
    //         notePlayer.setMinVelocity(60);
    //         notePlayer.setMaxVelocity(100);
    //         notePlayer.setLevel(80);
    //         notePlayer.setChannel(0);
    //         playerHelper.savePlayer(notePlayer);
    //         System.out.println("Created Note player: " + notePlayer.getName() + " (ID: " + notePlayer.getId() + ")");

    //         // Strike player
    //         Strike strikePlayer = new Strike("Test Strike Player", testSession, null, 36, null);
    //         strikePlayer.setId(playerHelper.getNextPlayerId());
    //         strikePlayer.setRules(new HashSet<>());
    //         strikePlayer.setChannel(9); // Typically channel 10 (index 9) for drum sounds
    //         playerHelper.savePlayer(strikePlayer);
    //         System.out.println(
    //                 "Created Strike player: " + strikePlayer.getName() + " (ID: " + strikePlayer.getId() + ")");

    //         // Test 3: Create test instruments
    //         System.out.println("\n--- Test 3: Create test instruments ---");
    //         // For Note player
    //         InstrumentWrapper melodicInstrument = new InstrumentWrapper();
    //         melodicInstrument.setName("Test Piano " + System.currentTimeMillis());
    //         melodicInstrument.setDeviceName("Test Device");
    //         melodicInstrument.setChannel(0); // Same as Note player's channel
    //         melodicInstrument.setLowestNote(36);
    //         melodicInstrument.setHighestNote(96);
    //         melodicInstrument.setAvailable(true);
    //         instrumentHelper.saveInstrument(melodicInstrument);
    //         System.out.println("Created melodic instrument: " + melodicInstrument.getName() +
    //                 " (ID: " + melodicInstrument.getId() + ")");

    //         // For Strike player
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

    //         // Test 4: Attach instruments to players
    //         System.out.println("\n--- Test 4: Attach instruments to players ---");
    //         // Assign to Note player
    //         notePlayer.setInstrumentId(melodicInstrument.getId());
    //         playerHelper.savePlayer(notePlayer);
    //         System.out.println("Assigned instrument " + melodicInstrument.getName() +
    //                 " to player " + notePlayer.getName());

    //         // Assign to Strike player
    //         strikePlayer.setInstrumentId(percussionInstrument.getId());
    //         playerHelper.savePlayer(strikePlayer);
    //         System.out.println("Assigned instrument " + percussionInstrument.getName() +
    //                 " to player " + strikePlayer.getName());

    //         // Test 5: Retrieve players and verify instrument relationships
    //         System.out.println("\n--- Test 5: Retrieve players and verify instrument relationships ---");
    //         Player retrievedNotePlayer = playerHelper.findPlayerById(notePlayer.getId(), "Note");
    //         Player retrievedStrikePlayer = playerHelper.findPlayerById(strikePlayer.getId(), "Strike");

    //         if (retrievedNotePlayer != null) {
    //             System.out.println("Retrieved Note player: " + retrievedNotePlayer.getName());
    //             if (retrievedNotePlayer.getInstrumentId() != null &&
    //                     retrievedNotePlayer.getInstrumentId().equals(melodicInstrument.getId())) {
    //                 System.out.println("SUCCESS: Note player correctly linked to instrument " +
    //                         melodicInstrument.getId());
    //             } else {
    //                 JOptionPane.showMessageDialog(null, "ERROR: Note player has wrong instrument ID: " +
    //                         retrievedNotePlayer.getInstrumentId());
    //             }
    //         } else {
    //             JOptionPane.showMessageDialog(null, "ERROR: Failed to retrieve Note player");
    //         }

    //         if (retrievedStrikePlayer != null) {
    //             System.out.println("Retrieved Strike player: " + retrievedStrikePlayer.getName());
    //             if (retrievedStrikePlayer.getInstrumentId() != null &&
    //                     retrievedStrikePlayer.getInstrumentId().equals(percussionInstrument.getId())) {
    //                 System.out.println("SUCCESS: Strike player correctly linked to instrument " +
    //                         percussionInstrument.getId());
    //             } else {
    //                 JOptionPane.showMessageDialog(null, "ERROR: Strike player has wrong instrument ID: " +
    //                         retrievedStrikePlayer.getInstrumentId());
    //             }
    //         } else {
    //             JOptionPane.showMessageDialog(null, "ERROR: Failed to retrieve Strike player");
    //         }

    //         // Test 6: Create new instances of players with same IDs and load them
    //         System.out.println("\n--- Test 6: Create new player instances with same IDs and load them ---");
    //         Note newNoteInstance = new Note();
    //         newNoteInstance.setId(notePlayer.getId());
    //         newNoteInstance = (Note) playerHelper.findPlayerById(newNoteInstance.getId(), "Note");

    //         Strike newStrikeInstance = new Strike();
    //         newStrikeInstance.setId(strikePlayer.getId());
    //         newStrikeInstance = (Strike) playerHelper.findPlayerById(newStrikeInstance.getId(), "Strike");

    //         // Verify instrument relationships persisted
    //         if (newNoteInstance != null) {
    //             System.out.println("Loaded new Note player instance: " + newNoteInstance.getName());
    //             if (newNoteInstance.getInstrumentId() != null &&
    //                     newNoteInstance.getInstrumentId().equals(melodicInstrument.getId())) {
    //                 System.out.println("SUCCESS: New Note instance correctly linked to instrument " +
    //                         melodicInstrument.getId());

    //                 // Verify instrument can be loaded
    //                 InstrumentWrapper loadedInstrument = instrumentHelper.findInstrumentById(
    //                         newNoteInstance.getInstrumentId());
    //                 if (loadedInstrument != null) {
    //                     System.out.println("SUCCESS: Loaded instrument: " + loadedInstrument.getName());
    //                 } else {
    //                     JOptionPane.showMessageDialog(null, "ERROR: Could not load instrument");
    //                 }
    //             } else {
    //                 JOptionPane.showMessageDialog(null, "ERROR: New Note instance has wrong instrument ID: " +
    //                         newNoteInstance.getInstrumentId());
    //             }
    //         } else {
    //             JOptionPane.showMessageDialog(null, "ERROR: Failed to load new Note instance");
    //         }

    //         if (newStrikeInstance != null) {
    //             System.out.println("Loaded new Strike player instance: " + newStrikeInstance.getName());
    //             if (newStrikeInstance.getInstrumentId() != null &&
    //                     newStrikeInstance.getInstrumentId().equals(percussionInstrument.getId())) {
    //                 System.out.println("SUCCESS: New Strike instance correctly linked to instrument " +
    //                         percussionInstrument.getId());
    //             } else {
    //                 JOptionPane.showMessageDialog(null, "ERROR: New Strike instance has wrong instrument ID: " +
    //                         newStrikeInstance.getInstrumentId());
    //             }
    //         } else {
    //             JOptionPane.showMessageDialog(null, "ERROR: Failed to load new Strike instance");
    //         }

    //         // Test 7: Test player helper methods (newNote and newStrike)
    //         System.out.println("\n--- Test 7: Test helper factory methods ---");
    //         Player factoryNote = playerHelper.newNote();
    //         System.out.println("Created note via factory method: " + factoryNote.getName() +
    //                 " (ID: " + factoryNote.getId() + ")");

    //         Player factoryStrike = playerHelper.newStrike();
    //         System.out.println("Created strike via factory method: " + factoryStrike.getName() +
    //                 " (ID: " + factoryStrike.getId() + ")");

    //         // Test 8: Clean up
    //         System.out.println("\n--- Test 8: Clean up ---");
    //         playerHelper.deletePlayer(notePlayer);
    //         playerHelper.deletePlayer(strikePlayer);
    //         playerHelper.deletePlayer(factoryNote);
    //         playerHelper.deletePlayer(factoryStrike);
    //         instrumentHelper.deleteInstrument(melodicInstrument.getId());
    //         instrumentHelper.deleteInstrument(percussionInstrument.getId());
    //         sessionHelper.deleteSession(testSession.getId());
    //         System.out.println("Cleaned up all test data");

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
