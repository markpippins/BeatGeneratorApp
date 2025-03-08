package com.angrysurfer.core.redis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Getter
@Setter
class SessionHelper {
    private static final Logger logger = Logger.getLogger(SessionHelper.class.getName());
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final PlayerHelper playerHelper;
    private final CommandBus commandBus = CommandBus.getInstance();

    public SessionHelper(JedisPool jedisPool, ObjectMapper objectMapper) {
        this.jedisPool = jedisPool;
        this.objectMapper = objectMapper;
        this.playerHelper = new PlayerHelper(jedisPool, objectMapper);
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

                // Load players for this session
                Set<String> playerKeys = jedis.smembers("session:" + id + ":players:strike");
                if (!playerKeys.isEmpty()) {
                    playerKeys.forEach(playerId -> {
                        Player player = playerHelper.findPlayerById(Long.parseLong(playerId), "strike");
                        if (player != null) {
                            player.setSession(session);
                            session.getPlayers().add(player);
                        }
                    });
                }

                logger.info(String.format("Loaded session %d with %d players", id, session.getPlayers().size()));
                return session;
            }
            return null;
        } catch (Exception e) {
            logger.severe("Error finding session: " + e.getMessage());
            throw new RuntimeException("Failed to find session", e);
        }
    }

    public void saveSession(Session session) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (session.getId() == null) {
                session.setId(jedis.incr("seq:session"));
            }

            // Save player relationships
            String playerSetKey = "session:" + session.getId() + ":players:strike";
            jedis.del(playerSetKey); // Clear existing relationships

            if (session.getPlayers() != null) {
                session.getPlayers().forEach(player -> {
                    jedis.sadd(playerSetKey, player.getId().toString());
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
            logger.severe("Error saving session: " + e.getMessage());
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
                    logger.warning("Invalid session key: " + key);
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
            logger.severe("Error deleting session " + sessionId + ": " + e.getMessage());
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
            logger.severe("Error creating new session: " + e.getMessage());
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

    // Add method to find session for a player

    public Session findSessionForPlayer(Player player) {
        if (player == null || player.getId() == null) {
            return null;
        }
        
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> sessionKeys = jedis.keys("session:*");
            for (String sessionKey : sessionKeys) {
                if (!sessionKey.contains(":players")) {
                    String sessionId = sessionKey.split(":")[1];
                    String playersKey = "session:" + sessionId + ":players:strike";
                    
                    if (jedis.sismember(playersKey, player.getId().toString())) {
                        return findSessionById(Long.valueOf(sessionId));
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error finding session for player: " + e.getMessage());
        }
        return null;
    }
}