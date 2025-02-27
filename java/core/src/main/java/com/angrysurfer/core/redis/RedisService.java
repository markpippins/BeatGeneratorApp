package com.angrysurfer.core.redis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.config.FrameState;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Song;
import com.angrysurfer.core.model.Step;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.model.Ticker;
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
    private final RedisPlayerHelper playerHelper;
    private final RedisRuleHelper ruleHelper;
    private final RedisSongHelper songHelper;
    private final RedisPatternHelper patternHelper;
    private final RedisStepHelper stepHelper;
    private final RedisInstrumentHelper instrumentHelper;
    private final RedisTickerHelper tickerHelper;
    private final RedisUserConfigurationHelper userConfigHelper;
    // private final RedisConfigHelper configHelper;

    private RedisService() {
        this.jedisPool = initJedisPool();
        this.objectMapper = initObjectMapper();

        // Initialize helpers
        this.instrumentHelper = new RedisInstrumentHelper(jedisPool, objectMapper);
        this.stepHelper = new RedisStepHelper(jedisPool, objectMapper);
        this.patternHelper = new RedisPatternHelper(jedisPool, objectMapper);
        this.songHelper = new RedisSongHelper(jedisPool, objectMapper);
        this.ruleHelper = new RedisRuleHelper(jedisPool, objectMapper);
        this.playerHelper = new RedisPlayerHelper(jedisPool, objectMapper);
        this.tickerHelper = new RedisTickerHelper(jedisPool, objectMapper);
        this.userConfigHelper = new RedisUserConfigurationHelper(jedisPool, objectMapper);
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

    // Ticker operations
    public Ticker findTickerById(Long id) {
        return tickerHelper.findTickerById(id);
    }

    public void saveTicker(Ticker ticker) {
        tickerHelper.saveTicker(ticker);
    }

    public Long getMinimumTickerId() {
        return tickerHelper.getMinimumTickerId();
    }

    public Long getMaximumTickerId() {
        return tickerHelper.getMaximumTickerId();
    }

    public Long getPreviousTickerId(Ticker ticker) {
        return tickerHelper.getPreviousTickerId(ticker);
    }

    public Long getNextTickerId(Ticker ticker) {
        return tickerHelper.getNextTickerId(ticker);
    }

    public List<Long> getAllTickerIds() {
        return tickerHelper.getAllTickerIds();
    }

    public void deleteTicker(Long tickerId) {
        tickerHelper.deleteTicker(tickerId);
    }

    public Ticker newTicker() {
        return tickerHelper.newTicker();
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

    // ... other facade methods ...

    @Override
    public void onAction(Command action) {
        if (Commands.CLEAR_DATABASE.equals(action.getCommand())) {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.flushDB();
                logger.info("Database cleared");
                // Ticker ticker = tickerHelper.newTicker();
                // commandBus.publish(new Command(Commands.TICKER_LOADED, this, ticker));
            }
        }
    }

    public boolean isDatabaseEmpty() {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.keys("*").isEmpty();
        }
    }

    public List<Instrument> findAllInstruments() {
        List<Instrument> instruments = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> instrumentKeys = jedis.keys("DialogManagerinstrument:*");
            for (String key : instrumentKeys) {
                String json = jedis.get(key);
                if (json != null) {
                    Instrument instrument = objectMapper.readValue(json, Instrument.class);
                    instruments.add(instrument);
                }
            }
        } catch (Exception e) {
            logger.severe("Error finding instruments: " + e.getMessage());
        }
        return instruments;
    }

    public void saveInstrument(Instrument instrument) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (instrument.getId() == null) {
                String seqKey = "seq:DialogManagerinstrument";
                instrument.setId(jedis.incr(seqKey));
            }
            String json = objectMapper.writeValueAsString(instrument);
            jedis.set("DialogManagerinstrument:" + instrument.getId(), json);
        } catch (Exception e) {
            logger.severe("Error saving instrument: " + e.getMessage());
        }
    }

    public FrameState loadFrameState() {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = jedis.get("framestate");
            if (json != null) {
                FrameState state = objectMapper.readValue(json, FrameState.class);
                logger.info("Loaded frame state with column order: " +
                        (state.getPlayerColumnOrder() != null ? String.join(", ", state.getPlayerColumnOrder())
                                : "null"));
                return state;
            }
            logger.info("No existing frame state found, creating new one");
        } catch (Exception e) {
            logger.severe("Error loading frame state: " + e.getMessage());
        }
        return new FrameState();
    }

    public void saveFrameState(FrameState state) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(state);
            jedis.set("framestate", json);
            logger.info("Saved frame state with column order: " +
                    (state.getPlayerColumnOrder() != null ? String.join(", ", state.getPlayerColumnOrder()) : "null"));
        } catch (Exception e) {
            logger.severe("Error saving frame state: " + e.getMessage());
        }
    }

    public UserConfig loadConfigFromJSON(String configPath) {
        try {
            UserConfig config = objectMapper.readValue(new File(configPath), UserConfig.class);

            // Log overall config summary
            logger.info("Loading UserConfig from: " + configPath);
            logger.info(String.format("Found %d instruments, %d players, %d configs",
                    config.getInstruments().size(),
                    config.getPlayers() != null ? config.getPlayers().size() : 0,
                    config.getConfigs() != null ? config.getConfigs().size() : 0));

            // Log detailed instrument information
            if (config.getInstruments() != null) {
                for (Instrument instrument : config.getInstruments()) {
                    logger.info("\nInstrument: " + instrument.getName());
                    logger.info("  ID: " + instrument.getId());
                    // logger.info(" Channel: " + instrument. getChannel());

                    // Log control codes
                    if (instrument.getControlCodes() != null) {
                        logger.info("  Control Codes:");
                        instrument.getControlCodes().forEach(code -> {
                            logger.info("    Code: " + code.getCode());
                            if (code.getCaptions() != null) {
                                code.getCaptions()
                                        .forEach(caption -> logger.info("      Caption: " + caption.getCode() +
                                                " (From: " + caption.getDescription()));
                            }
                        });
                    }
                }
            }

            // Log configuration details
            if (config.getConfigs() != null) {
                logger.info("\nInstrument Configurations:");
                config.getConfigs().forEach(cfg -> {
                    logger.info("  Device: " + cfg.getDevice());
                    logger.info("    Port: " + cfg.getPort());
                    logger.info("    Channels: " + cfg.getChannels());
                    logger.info("    Range: " + cfg.getLow() + " - " + cfg.getHigh());
                    logger.info("    Available: " + cfg.isAvailable());
                });
            }

            return config;
        } catch (Exception e) {
            logger.severe("Error loading config: " + e.getMessage());
            throw new RuntimeException("Failed to load config", e);
        }
    }

    public void saveConfig(UserConfig config) {
        try (Jedis jedis = jedisPool.getResource()) {
            String json = objectMapper.writeValueAsString(config);
            jedis.set("userconfig", json);
        } catch (Exception e) {
            logger.severe("Error saving config: " + e.getMessage());
        }
    }

    // Player methods
    public Set<Player> findPlayersForTicker(Long tickerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Player> players = new HashSet<>();
            String playersKey = "ticker:" + tickerId + ":players";
            Set<String> playerIds = jedis.smembers(playersKey);
            for (String id : playerIds) {
                Player player = findPlayerById(Long.valueOf(id));
                if (player != null) {
                    players.add(player);
                }
            }
            return players;
        }
    }

    public Player newPlayer() {
        try (Jedis jedis = jedisPool.getResource()) {
            Player player = new Strike();
            player.setId(jedis.incr("seq:player"));
            player.setRules(new HashSet<>()); // Ensure rules are initialized
            savePlayer(player);
            return player;
        }
    }

    public void deletePlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            logger.info("Deleting player: " + player.getId());
            
            // Delete player's rules first
            String rulesKey = "player:" + player.getId() + ":rules";
            Set<String> ruleIds = jedis.smembers(rulesKey);
            for (String ruleId : ruleIds) {
                deleteRule(Long.valueOf(ruleId));
            }
            jedis.del(rulesKey);
            logger.info("Deleted player's rules");

            // Remove from ticker's player set
            if (player.getTicker() != null) {
                String tickerPlayersKey = "ticker:" + player.getTicker().getId() + ":players";
                jedis.srem(tickerPlayersKey, player.getId().toString());
                logger.info("Removed player from ticker's player set in Redis");
            }

            // Delete the player itself
            jedis.del("player:" + player.getId());
            logger.info("Deleted player from Redis");
        } catch (Exception e) {
            logger.severe("Error deleting player: " + e.getMessage());
            throw new RuntimeException("Failed to delete player", e);
        }
    }

    // Rule methods
    public Rule newRule() {
        try (Jedis jedis = jedisPool.getResource()) {
            Rule rule = new Rule();
            rule.setId(jedis.incr("seq:DialogManagerrule"));
            saveRule(rule);
            return rule;
        }
    }

    public Set<Rule> findRulesForPlayer(Long playerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Rule> rules = new HashSet<>();
            String rulesKey = "player:" + playerId + ":rules";
            Set<String> ruleIds = jedis.smembers(rulesKey);
            for (String id : ruleIds) {
                Rule rule = findRuleById(Long.valueOf(id));
                if (rule != null) {
                    rules.add(rule);
                }
            }
            return rules;
        }
    }

    public boolean isValidNewRule(Player player, Rule newRule) {
        if (player == null || player.getRules() == null || newRule == null) {
            return false;
        }

        // Check if there's already a rule with the same operator and part
        return player.getRules().stream()
                .noneMatch(existingRule -> existingRule.getOperator() == newRule.getOperator() &&
                        existingRule.getPart() == newRule.getPart());
    }

    public void addRuleToPlayer(Player player, Rule rule) {
        if (!isValidNewRule(player, rule)) {
            throw new IllegalArgumentException(
                    "A rule with this operator already exists for part " +
                            (rule.getPart() == 0 ? "All" : rule.getPart()));
        }

        try (Jedis jedis = jedisPool.getResource()) {
            saveRule(rule);
            String rulesKey = "player:" + player.getId() + ":rules";
            jedis.sadd(rulesKey, rule.getId().toString());
            if (player.getRules() == null) {
                player.setRules(new HashSet<>());
            }
            player.getRules().add(rule);
        }
    }

    public void removeRuleFromPlayer(Player player, Rule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            String rulesKey = "player:" + player.getId() + ":rules";
            jedis.srem(rulesKey, rule.getId().toString());

            // Update player's rules set
            if (player.getRules() != null) {
                player.getRules().remove(rule);
            }

            // Delete the actual rule from Redis
            jedis.del("DialogManagerrule:" + rule.getId());

            // Save player state
            savePlayer(player);

            // Find and update player in its ticker's player list
            if (player.getTicker() != null) {
                Ticker ticker = (Ticker) player.getTicker();
                ticker.getPlayers().stream()
                        .filter(p -> p.getId().equals(player.getId()))
                        .findFirst()
                        .ifPresent(p -> {
                            ((Player) p).setRules(player.getRules());
                        });
            }
        }
    }

    public void deleteRule(Long ruleId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("rule:" + ruleId);
        }
    }

    public Player findPlayerForRule(Rule rule) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> playerKeys = jedis.keys("player:*");
            for (String playerKey : playerKeys) {
                String playerId = playerKey.split(":")[1];
                String rulesKey = "player:" + playerId + ":rules";
                if (jedis.sismember(rulesKey, rule.getId().toString())) {
                    return findPlayerById(Long.valueOf(playerId));
                }
            }
        }
        return null;
    }

    public Ticker findTickerForPlayer(Player player) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> tickerKeys = jedis.keys("ticker:*");
            for (String tickerKey : tickerKeys) {
                String playersKey = tickerKey + ":players";
                if (jedis.sismember(playersKey, player.getId().toString())) {
                    String tickerId = tickerKey.split(":")[1];
                    return findTickerById(Long.valueOf(tickerId));
                }
            }
        }
        return null;
    }

    public void clearInvalidTickers() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            for (String key : keys) {
                Ticker ticker = findTickerById(Long.parseLong(key.split(":")[1]));
                if (ticker != null && !ticker.isValid()) {
                    deleteTicker(ticker.getId());
                }
            }
        }
    }

    public boolean tickerExists(Long tickerId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.exists("ticker:" + tickerId);
        }
    }

    public Ticker findFirstValidTicker() {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<String> keys = jedis.keys("ticker:*");
            for (String key : keys) {
                Ticker ticker = findTickerById(Long.parseLong(key.split(":")[1]));
                if (ticker != null && ticker.isValid()) {
                    return ticker;
                }
            }
            return null;
        }
    }

    public Set<Step> findStepsByPatternId(Long patternId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Set<Step> steps = new HashSet<>();
            String stepsKey = "pattern:" + patternId + ":steps";
            Set<String> stepIds = jedis.smembers(stepsKey);
            for (String id : stepIds) {
                Step step = findStepById(Long.valueOf(id));
                if (step != null) {
                    steps.add(step);
                }
            }
            return steps;
        }
    }

    public void deletePattern(Pattern pattern) {
        patternHelper.deletePattern(pattern.getId());
    }

    public Set<Pattern> findPatternsBySongId(Long songId) {
        return patternHelper.findPatternsForSong(songId);
    }

    public Long getMaximumSongId() {
        return songHelper.getMaximumSongId();
    }

    public Long getNextSongId(long currentSongId) {
        return songHelper.getNextSongId(currentSongId);
    }

    public Long getPreviousSongId(long currentSongId) {
        return songHelper.getPreviousSongId(currentSongId);
    }

    public Long getMinimumSongId() {
        return songHelper.getMinimumSongId();
    }

    public void addPlayerToTicker(Ticker ticker, Player player) {
        playerHelper.addPlayerToTicker(ticker, player);
        // Save the ticker after updating its players
        saveTicker(ticker);
    }
}
