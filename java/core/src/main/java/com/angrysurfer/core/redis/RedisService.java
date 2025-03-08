package com.angrysurfer.core.redis;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.config.TableState;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.model.midi.Instrument;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Getter
public class RedisService implements CommandListener {
    private static final Logger logger = Logger.getLogger(RedisService.class.getName());
    private static RedisService instance;
    private final JedisPool jedisPool;
    private final ObjectMapper objectMapper;
    private final CommandBus commandBus = CommandBus.getInstance();

    // Helper classes as implementation details
    private final PlayerHelper playerHelper;
    private final RuleHelper ruleHelper;
    private final SongHelper songHelper;
    private final PatternHelper patternHelper;
    private final StepHelper stepHelper;
    private final InstrumentHelper instrumentHelper;
    private final SessionHelper sessionHelper;
    private final UserConfigHelper userConfigHelper;
    // private final RedisConfigHelper configHelper;

    private RedisService() {
        this.jedisPool = initJedisPool();
        this.objectMapper = initObjectMapper();

        // Initialize helpers
        this.sessionHelper = new SessionHelper(jedisPool, objectMapper);
        this.playerHelper = new PlayerHelper(jedisPool, objectMapper);
        this.ruleHelper = new RuleHelper(jedisPool, objectMapper);
        this.patternHelper = new PatternHelper(jedisPool, objectMapper);
        this.stepHelper = new StepHelper(jedisPool, objectMapper);
        this.songHelper = new SongHelper(jedisPool, objectMapper);
        this.instrumentHelper = new InstrumentHelper(jedisPool, objectMapper);
        this.userConfigHelper = new UserConfigHelper(jedisPool, objectMapper);
        // this.configHelper = new RedisConfigHelper(jedisPool, objectMapper);

        commandBus.register(this);
    }

    private JedisPool initJedisPool() {
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(10);
        poolConfig.setMaxIdle(5);
        poolConfig.setMinIdle(1);
        return new JedisPool(poolConfig, "localhost", 6379);
    }

    private ObjectMapper initObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return mapper;
    }

    // Singleton access
    public static RedisService getInstance() {
        if (instance == null) {
            synchronized (RedisService.class) {
                if (instance == null) {
                    instance = new RedisService();
                    logger.info("RedisService singleton instance created");
                }
            }
        }
        return instance;
    }

    // Facade methods delegating to helpers

    // Session operations
    public Session findSessionById(Long id) {
        return sessionHelper.findSessionById(id);
    }

    public void saveSession(Session session) {
        sessionHelper.saveSession(session);
    }

    public Long getMinimumSessionId() {
        return sessionHelper.getMinimumSessionId();
    }

    public Long getMaximumSessionId() {
        return sessionHelper.getMaximumSessionId();
    }

    public Long getPreviousSessionId(Session session) {
        return sessionHelper.getPreviousSessionId(session);
    }

    public Long getNextSessionId(Session session) {
        return sessionHelper.getNextSessionId(session);
    }

    public List<Long> getAllSessionIds() {
        return sessionHelper.getAllSessionIds();
    }

    public void deleteSession(Long sessionId) {
        sessionHelper.deleteSession(sessionId);
    }

    public Session newSession() {
        return sessionHelper.newSession();
    }

    public Session findSessionForPlayer(Player player) {
        return sessionHelper.findSessionForPlayer(player);
    }

    public void clearInvalidSessions() {
        sessionHelper.clearInvalidSessions();
    }

    public boolean sessionExists(Long sessionId) {
        return sessionHelper.sessionExists(sessionId);
    }

    public Session findFirstValidSession() {
        return sessionHelper.findFirstValidSession();
    }

    // Player operations
    public Player findPlayerById(Long id) {
        return playerHelper.findPlayerById(id, Player.class.getSimpleName().toLowerCase());
    }

    public void savePlayer(Player player) {
        playerHelper.savePlayer(player);
    }

    public Long getNextPlayerId() {
        return playerHelper.getNextPlayerId();
    }

    // Rule operations
    public Rule findRuleById(Long id) {
        return ruleHelper.findRuleById(id);
    }

    public void saveRule(Rule rule) {
        ruleHelper.saveRule(rule);
    }

    public Long getNextRuleId() {
        return ruleHelper.getNextRuleId();
    }

    // Song operations
    public Song findSongById(Long id) {
        return songHelper.findSongById(id);
    }

    public Song saveSong(Song song) {
        return songHelper.saveSong(song);
    }

    public Long getMinimumSongId() {
        return songHelper.getMinimumSongId();
    }

    public Long getMaximumSongId() {
        return songHelper.getMaximumSongId();
    }

    public Long getNextSongId(long currentId) {
        return songHelper.getNextSongId(currentId);
    }

    public Long getPreviousSongId(long currentId) {
        return songHelper.getPreviousSongId(currentId);
    }

    public void deleteSong(Long songId) {
        songHelper.deleteSong(songId);
    }

    // Pattern operations
    public Pattern findPatternById(Long id) {
        return patternHelper.findPatternById(id);
    }

    public Pattern savePattern(Pattern pattern) {
        patternHelper.savePattern(pattern);
        return pattern;
    }

    // Step operations
    public Step findStepById(Long id) {
        return stepHelper.findStepById(id);
    }

    public Step saveStep(Step step) {
        return stepHelper.saveStep(step);
    }

    @Override
    public void onAction(Command action) {

        switch (action.getCommand()) {
            case Commands.CLEAR_DATABASE:
                clearDatabase();
                break;
            // case Commands.LOAD_CONFIG:
            // String configPath = (String) action.getPayload();
            // UserConfig config = loadConfigFromJSON(configPath);
            // commandBus.publish(Commands.USER_CONFIG_LOADED, this, config);
            // break;
            default:
                logger.warning("Unknown command: " + action.getCommand());
        }
    }

    public void clearDatabase() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.flushDB();
            logger.info("Database cleared");
            Session session = sessionHelper.newSession();
            commandBus.publish(Commands.SESSION_LOADED, this, session);
        }
    }

    public boolean isDatabaseEmpty() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys("*").isEmpty();
        }
    }

    // Replace direct implementations with delegation to instrumentHelper
    public List<Instrument> findAllInstruments() {
        return instrumentHelper.findAllInstruments();
    }

    public Instrument findInstrumentById(Long id) {
        return instrumentHelper.findInstrumentById(id);
    }

    public void saveInstrument(Instrument instrument) {
        instrumentHelper.saveInstrument(instrument);
    }

    public void deleteInstrument(Instrument instrument) {
        if (instrument != null && instrument.getId() != null) {
            instrumentHelper.deleteInstrument(instrument.getId());
        }
    }

    public TableState loadTableState(String table) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("tablestate-" + table);
            if (json != null) {
                TableState state = objectMapper.readValue(json, TableState.class);
                logger.info("Loaded table state with column order: " +
                        (state.getColumnOrder() != null ? String.join(", ", state.getColumnOrder())
                                : "null"));
                return state;
            }
            logger.info("No existing table state found, creating new one");
        } catch (Exception e) {
            logger.severe("Error loading table state: " + e.getMessage());
        }
        return new TableState();
    }

    public void saveTableState(TableState state, String table) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(state);
            jedis.set("tablestate-" + table, json);
            logger.info("Saved table state with column order: " +
                    (state.getColumnOrder() != null ? String.join(", ", state.getColumnOrder()) : "null"));
        } catch (Exception e) {
            logger.severe("Error saving frame state: " + e.getMessage());
        }
    }

    public FrameState loadFrameState(String window) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("framestate-" + window);
            if (json != null) {
                FrameState state = objectMapper.readValue(json, FrameState.class);
                logger.info("Loaded frame state with column order: " +
                        (state.getColumnOrder() != null ? String.join(", ", state.getColumnOrder())
                                : "null"));
                return state;
            }
            logger.info("No existing frame state found, creating new one");
        } catch (Exception e) {
            logger.severe("Error loading frame state: " + e.getMessage());
        }
        return new FrameState();
    }

    public void saveFrameState(FrameState state, String window) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(state);
            jedis.set("framestate-" + window, json);
            logger.info("Saved frame state with column order: " +
                    (state.getColumnOrder() != null ? String.join(", ", state.getColumnOrder()) : "null"));
        } catch (Exception e) {
            logger.severe("Error saving frame state: " + e.getMessage());
        }
    }

    // Replace direct implementations with delegation to userConfigHelper
    public UserConfig loadConfigFromJSON(String configPath) {
        return userConfigHelper.loadConfigFromJSON(configPath);
    }

    public void saveConfig(UserConfig config) {
        userConfigHelper.saveConfig(config);
    }

    // Player methods
    public Set<Player> findPlayersForSession(Long sessionId) {
        return playerHelper.findPlayersForSession(sessionId, Player.class.getSimpleName().toLowerCase());
    }

    public Player newPlayer() {
        Player player = new Strike();
        player.setId(playerHelper.getNextPlayerId());
        player.setRules(new HashSet<>()); // Ensure rules are initialized
        playerHelper.savePlayer(player);
        return player;
    }

    public void deletePlayer(Player player) {
        playerHelper.deletePlayer(player);
    }

    // Rule methods
    public Rule newRule() {
        return ruleHelper.createNewRule();
    }

    public Set<Rule> findRulesForPlayer(Long playerId) {
        return ruleHelper.findRulesForPlayer(playerId);
    }

    public boolean isValidNewRule(Player player, Rule newRule) {
        return ruleHelper.isValidNewRule(player, newRule);
    }

    public void addRuleToPlayer(Player player, Rule rule) {
        ruleHelper.addRuleToPlayer(player, rule);
    }

    public void removeRuleFromPlayer(Player player, Rule rule) {
        ruleHelper.removeRuleFromPlayer(player, rule);
    }

    public void deleteRule(Long ruleId) {
        ruleHelper.deleteRule(ruleId);
    }

    public Player findPlayerForRule(Rule rule) {
        return ruleHelper.findPlayerForRule(rule);
    }

    public Set<Step> findStepsByPatternId(Long patternId) {
        return stepHelper.findStepsByPatternId(patternId);
    }

    public void deletePattern(Pattern pattern) {
        patternHelper.deletePattern(pattern.getId());
    }

    public Set<Pattern> findPatternsBySongId(Long songId) {
        return patternHelper.findPatternsForSong(songId);
    }

    public void addPlayerToSession(Session session, Player player) {
        playerHelper.addPlayerToSession(session, player);
        // Save the session after updating its players
        saveSession(session);
    }
}
