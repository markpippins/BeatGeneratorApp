package com.angrysurfer.core.redis;

import java.util.List;
import java.util.Set;
import java.util.Arrays;

import com.angrysurfer.core.sequencer.*;
import com.angrysurfer.core.util.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.config.TableState;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.event.PatternSwitchEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.SessionDeserializer;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;

import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Getter
public class RedisService implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
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
    private final DrumSequenceHelper drumSequenceHelper;
    private final MelodicSequenceDataHelper melodicSequencerHelper;
    // private final RedisConfigHelper configHelper;

    private RedisService() {
        this.jedisPool = initJedisPool();
        this.objectMapper = createObjectMapper();

        // Initialize helpers
        this.sessionHelper = new SessionHelper(jedisPool, objectMapper);
        this.playerHelper = new PlayerHelper(jedisPool, objectMapper);
        this.ruleHelper = new RuleHelper(jedisPool, objectMapper);
        this.patternHelper = new PatternHelper(jedisPool, objectMapper);
        this.stepHelper = new StepHelper(jedisPool, objectMapper);
        this.songHelper = new SongHelper(jedisPool, objectMapper);
        this.instrumentHelper = new InstrumentHelper(jedisPool, objectMapper);
        this.userConfigHelper = new UserConfigHelper(jedisPool, objectMapper);
        this.drumSequenceHelper = new DrumSequenceHelper(jedisPool, objectMapper);
        this.melodicSequencerHelper = new MelodicSequenceDataHelper(jedisPool, objectMapper);
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

    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Configure mapper
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        // Register module with custom deserializer
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Session.class, new SessionDeserializer());
        mapper.registerModule(module);

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

    /**
     * Get the next sequential session ID after the provided session
     */
    public Long getNextSessionId(Session session) {
        if (session == null) {
            logger.warn("Cannot get next session ID: current session is null");
            return null;
        }

        Long currentId = session.getId();
        List<Long> allIds = getAllSessionIds();
        logger.info("Finding next ID after {} among {} session IDs", currentId, allIds != null ? allIds.size() : 0);

        if (allIds == null || allIds.isEmpty()) {
            return null;
        }

        // Sort IDs in ascending order
        allIds.sort(Long::compareTo);

        // Log all IDs for debugging
        logger.debug("All session IDs (sorted): {}", allIds);

        // Find first ID greater than current
        for (Long id : allIds) {
            if (id > currentId) {
                logger.info("Found next ID: {} > {}", id, currentId);
                return id;
            }
        }

        logger.info("No session ID greater than {} found", currentId);
        return null;
    }

    /**
     * Get the previous sequential session ID before the provided session
     */
    public Long getPreviousSessionId(Session session) {
        if (session == null) {
            logger.warn("Cannot get previous session ID: current session is null");
            return null;
        }

        Long currentId = session.getId();
        List<Long> allIds = getAllSessionIds();
        logger.info("Finding previous ID before {} among {} session IDs", currentId,
                allIds != null ? allIds.size() : 0);

        if (allIds == null || allIds.isEmpty()) {
            return null;
        }

        // Sort IDs in ascending order
        allIds.sort(Long::compareTo);

        // Log all IDs for debugging
        logger.debug("All session IDs (sorted): {}", allIds);

        // Find last ID less than current
        Long prevId = null;
        for (Long id : allIds) {
            if (id >= currentId) {
                break;
            }
            prevId = id;
        }

        logger.info("Found previous ID: {} < {}", prevId, currentId);
        return prevId;
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
    /**
     * Save player to Redis
     * This handles instrument references and session updates
     */
    public void savePlayer(Player player) {
        try {
            if (player == null) {
                logger.warn("Cannot save null player");
                return;
            }

            logger.debug("Saving player ID: {} with name: {}", player.getId(), player.getName());

            // Save the instrument first if it exists
            if (player.getInstrument() != null) {
                saveInstrument(player.getInstrument());

                // Ensure the player's instrumentId is set correctly
                player.setInstrumentId(player.getInstrument().getId());

                logger.debug("Associated instrument ID: {} with name: {}",
                        player.getInstrumentId(), player.getInstrument().getName());
            }

            // Save the player with all its rules
            playerHelper.savePlayer(player);

            // If the player belongs to a session, update the session as well
            Session session = sessionHelper.findSessionForPlayer(player);
            if (session != null) {
                // Sets don't have index-based access, so we need to:
                // 1. Remove the existing player with the same ID
                session.getPlayers().removeIf(p -> p.getId().equals(player.getId()));

                // 2. Add the updated player
                session.getPlayers().add(player);

                // Save the updated session
                sessionHelper.saveSession(session);
            }
        } catch (Exception e) {
            logger.error("Error saving player: {}", e.getMessage(), e);
            // Use ErrorHandler instead of throwing exceptions directly
            ErrorHandler.logError("RedisService", "Failed to save player", e);
        }
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
            default:
                logger.warn("Unknown command: {}", action.getCommand());
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
    public List<InstrumentWrapper> findAllInstruments() {
        return instrumentHelper.findAllInstruments();
    }

    public InstrumentWrapper findInstrumentById(Long id) {
        return instrumentHelper.findInstrumentById(id);
    }

    public void saveInstrument(InstrumentWrapper instrument) {
        instrumentHelper.saveInstrument(instrument);
    }

    public void deleteInstrument(InstrumentWrapper instrument) {
        if (instrument != null && instrument.getId() != null) {
            instrumentHelper.deleteInstrument(instrument.getId());
        }
    }

    public TableState loadTableState(String table) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("tablestate-" + table);
            if (json != null) {
                TableState state = objectMapper.readValue(json, TableState.class);
                logger.info("Loaded table state with column order: {}",
                        (state.getColumnOrder() != null ? String.join(", ", state.getColumnOrder()) : "null"));
                return state;
            }
            logger.info("No existing table state found, creating new one");
        } catch (Exception e) {
            logger.error("Error loading table state: {}", e.getMessage());
        }
        return new TableState();
    }

    public void saveTableState(TableState state, String table) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(state);
            jedis.set("tablestate-" + table, json);
            logger.info("Saved table state with column order: {}",
                    (state.getColumnOrder() != null ? String.join(", ", state.getColumnOrder()) : "null"));
        } catch (Exception e) {
            logger.error("Error saving frame state: {}", e.getMessage());
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
            logger.error("Error loading frame state: {}", e.getMessage());
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
            logger.error("Error saving frame state: {}", e.getMessage());
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

    public Player newNote() {
        return playerHelper.newNote();
    }

    public Player newStrike() {
        return playerHelper.newStrike();
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

    // Drum sequence methods
    public DrumSequenceData findDrumSequenceById(Long id) {
        return drumSequenceHelper.findDrumSequenceById(id);
    }

    public void applyDrumSequenceToSequencer(DrumSequenceData data, DrumSequencer sequencer) {
        drumSequenceHelper.applyToSequencer(data, sequencer);
    }

    public void saveDrumSequence(DrumSequencer sequencer) {
        drumSequenceHelper.saveDrumSequence(sequencer);
    }

    public List<Long> getAllDrumSequenceIds() {
        return drumSequenceHelper.getAllDrumSequenceIds();
    }

    public Long getMinimumDrumSequenceId() {
        return drumSequenceHelper.getMinimumDrumSequenceId();
    }

    public Long getMaximumDrumSequenceId() {
        return drumSequenceHelper.getMaximumDrumSequenceId();
    }

    public DrumSequenceData newDrumSequence() {
        return drumSequenceHelper.newDrumSequence();
    }

    public void deleteDrumSequence(Long id) {
        drumSequenceHelper.deleteDrumSequence(id);
    }

    public Long getNextDrumSequenceId(Long currentId) {
        return drumSequenceHelper.getNextDrumSequenceId(currentId);
    }

    public Long getPreviousDrumSequenceId(Long currentId) {
        return drumSequenceHelper.getPreviousDrumSequenceId(currentId);
    }

    // Melodic sequence methods
    public MelodicSequenceData newMelodicSequence() {
        // Default to sequencer ID 0 when no ID is specified
        return melodicSequencerHelper.newMelodicSequence(0);
    }

    /**
     * Apply a melodic sequence to a sequencer
     * 
     * @param data      The melodic sequence data to apply
     * @param sequencer The sequencer to apply the data to
     */
    public void applyMelodicSequenceToSequencer(MelodicSequenceData data, MelodicSequencer sequencer) {
        if (data == null || sequencer == null) {
            logger.error("Cannot apply null sequence or sequencer");
            return;
        }

        try {
            // Store current playback and step state before applying new data
            boolean wasPlaying = sequencer.isPlaying();
            int currentStep = sequencer.getCurrentStep();

            // Simply set the data reference - no need to copy properties individually
            sequencer.setSequenceData(data);

            // Update sequencer state based on the new data
            sequencer.updateQuantizer();

            // Restore runtime state if needed
            if (wasPlaying) {
                sequencer.setPlaying(true);
            }
            sequencer.setCurrentStep(Math.min(currentStep, data.getPatternLength() - 1));

            // Log successful application
            logger.info("Applied melodic sequence ID {} to sequencer {}",
                    data.getId(), sequencer.getId());

            // Publish event so UI components can update
            CommandBus.getInstance().publish(
                    Commands.MELODIC_PATTERN_SWITCHED,
                    this,
                    new PatternSwitchEvent(null, data.getId()));

            // Add explicit debug about harmonic tilt values
            if (data.getHarmonicTiltValuesRaw() != null) {
                logger.info("Source data has {} harmonic tilt values",
                        data.getHarmonicTiltValuesRaw().length);

                // Make a copy of the harmonic tilt values array
                int[] tiltArrayCopy = Arrays.copyOf(data.getHarmonicTiltValuesRaw(),
                        data.getHarmonicTiltValuesRaw().length);

                // Set explicitly on sequencer's data
                sequencer.getSequenceData().setHarmonicTiltValues(tiltArrayCopy);

                // Verify
                logger.info("After setting, sequencer has {} harmonic tilt values",
                        sequencer.getSequenceData().getHarmonicTiltValuesRaw().length);
            } else {
                logger.warn("Source data has NULL harmonic tilt values array");
            }

        } catch (Exception e) {
            logger.error("Error applying melodic sequence: {}", e.getMessage(), e);
        }
    }

    public MelodicSequenceData findMelodicSequenceById(Long id) {
        // Default to sequencer ID 0 when no ID is specified
        return melodicSequencerHelper.findMelodicSequenceById(id, 0);
    }

    public MelodicSequenceData findMelodicSequenceById(Long id, Integer sequencerId) {
        return melodicSequencerHelper.findMelodicSequenceById(id, sequencerId);
    }

    public void saveMelodicSequence(MelodicSequencer sequencer) {
        melodicSequencerHelper.saveMelodicSequence(sequencer);
    }

    public List<Long> getAllMelodicSequenceIds(Integer sequencerId) {
        return melodicSequencerHelper.getAllMelodicSequenceIds(sequencerId);
    }

    public Long getMinimumMelodicSequenceId(Integer sequencerId) {
        return melodicSequencerHelper.getMinimumMelodicSequenceId(sequencerId);
    }

    public Long getMaximumMelodicSequenceId(Integer sequencerId) {
        return melodicSequencerHelper.getMaximumMelodicSequenceId(sequencerId);
    }

    public MelodicSequenceData newMelodicSequence(Integer sequencerId) {
        return melodicSequencerHelper.newMelodicSequence(sequencerId);
    }

    public void deleteMelodicSequence(Integer sequencerId, Long id) {
        melodicSequencerHelper.deleteMelodicSequence(sequencerId, id);
    }

    public Long getNextMelodicSequenceId(Integer sequencerId, Long currentId) {
        return melodicSequencerHelper.getNextMelodicSequenceId(sequencerId, currentId);
    }

    public Long getPreviousMelodicSequenceId(Integer sequencerId, Long currentId) {
        return melodicSequencerHelper.getPreviousMelodicSequenceId(sequencerId, currentId);
    }

    /**
 * Get an instrument by ID
 * 
 * @param id The instrument ID to look up
 * @return The instrument with the specified ID, or null if not found
 */
public InstrumentWrapper getInstrumentById(Long id) {
    if (id == null) {
        logger.warn("getInstrumentById called with null ID");
        return null;
    }
    
    logger.debug("Looking up instrument by ID: {}", id);
    
    // Delegate to the InstrumentHelper 
    return instrumentHelper.findInstrumentById(id);
}


    public Player findPlayerById(Long id) {
        return playerHelper.findPlayerById(id);
    }
}
