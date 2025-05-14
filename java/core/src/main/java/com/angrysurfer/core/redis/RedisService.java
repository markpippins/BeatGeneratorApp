package com.angrysurfer.core.redis;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.config.TableState;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.*;
import com.angrysurfer.core.sequencer.DrumSequenceData;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequenceData;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.ReceiverManager;
import com.angrysurfer.core.util.ErrorHandler;
import com.angrysurfer.core.util.SessionDeserializer;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final DrumSequenceDataHelper drumSequenceHelper;
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
        this.drumSequenceHelper = new DrumSequenceDataHelper(jedisPool, objectMapper);
        this.melodicSequencerHelper = new MelodicSequenceDataHelper(jedisPool);
        // this.configHelper = new RedisConfigHelper(jedisPool, objectMapper);

        commandBus.register(this);
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

            // Skip saving default players
            if (Boolean.TRUE.equals(player.getIsDefault())) {
                logger.debug("Skipping Redis save for default player: {}", player.getName());
                return;
            }

            logger.debug("Saving player ID: {} with name: {}", player.getId(), player.getName());

            // Save the instrument first if it exists and is not default
            if (player.getInstrument() != null && !Boolean.TRUE.equals(player.getInstrument().getIsDefault())) {
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
        if (instrument == null) {
            logger.warn("Cannot save null instrument");
            return;
        }

        // Skip saving default instruments
        if (Boolean.TRUE.equals(instrument.getIsDefault())) {
            logger.debug("Skipping Redis save for default instrument: {}", instrument.getName());
            return;
        }

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
        sequencer.setData(data);
        //drumSequenceHelper.applyToSequencer(data, sequencer);
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
     * Apply melodic sequence data to a sequencer
     */
    public void applyMelodicSequenceToSequencer(MelodicSequenceData data, MelodicSequencer sequencer) {
        if (data == null || sequencer == null) {
            logger.warn("Cannot apply null data or sequencer");
            return;
        }

        try {
            // Store current playback state
            boolean wasPlaying = sequencer.isPlaying();

            // Apply the sequence data
            sequencer.setSequenceData(data);

            // Update instrument settings if possible
            if (sequencer.getPlayer() != null) {
                Player player = sequencer.getPlayer();
                InstrumentWrapper instrument = player.getInstrument();

                // If no instrument, try to get by ID
                if (instrument == null && data.getInstrumentId() != null) {
                    instrument = InstrumentManager.getInstance().getInstrumentById(data.getInstrumentId());
                    if (instrument != null) {
                        player.setInstrument(instrument);
                        player.setInstrumentId(instrument.getId());
                    }
                }

                // If we have an instrument, apply the saved settings
                if (instrument != null) {
                    // Update from saved data
                    if (data.getPreset() != null) {
                        instrument.setPreset(data.getPreset());
                    }

                    if (data.getBankIndex() != null) {
                        instrument.setBankIndex(data.getBankIndex());
                    }

                    if (data.getSoundbankName() != null) {
                        instrument.setSoundbankName(data.getSoundbankName());
                    }

                    if (data.getDeviceName() != null) {
                        // Try to reconnect to saved device
                        DeviceManager.getInstance();
                        MidiDevice device = DeviceManager.getMidiDevice(data.getDeviceName());
                        if (device != null) {
                            try {
                                if (!device.isOpen()) {
                                    device.open();
                                }
                                instrument.setDevice(device);
                                instrument.setDeviceName(data.getDeviceName());

                                // Get a receiver
                                Receiver receiver = ReceiverManager.getInstance()
                                        .getOrCreateReceiver(data.getDeviceName(), device);
                                if (receiver != null) {
                                    instrument.setReceiver(receiver);
                                }
                            } catch (Exception e) {
                                logger.warn("Could not connect to device {}: {}",
                                        data.getDeviceName(), e.getMessage());
                            }
                        }
                    }

                    // Apply the instrument settings
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                }
            }

            // Restore playback state
            if (wasPlaying) {
                sequencer.start();
            }

            // Notify that pattern has been updated
            commandBus.publish(Commands.MELODIC_SEQUENCE_UPDATED, this,
                    Map.of("sequencerId", sequencer.getId(), "sequenceId", data.getId()));

        } catch (Exception e) {
            logger.error("Error applying melodic sequence data: " + e.getMessage(), e);
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
