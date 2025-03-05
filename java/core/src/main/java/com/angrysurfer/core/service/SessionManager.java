package com.angrysurfer.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionManager {

    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());
    private static SessionManager instance;

    private final CommandBus commandBus = CommandBus.getInstance();
    private final RedisService redisService = RedisService.getInstance();

    private final Map<Long, Instrument> instrumentCache = new HashMap<>();

    private TickerEngine tickerEngine;

    private UserConfigurationEngine userConfigurationEngine;

    private InstrumentEngine instrumentEngine;

    private SongEngine songEngine;

    private Player[] activePlayers[];
    private Rule[] selectedRules;

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void addPlayerToTicker(Ticker currentTicker, Player updatedPlayer) {
        // getTickerEngine()addPlayerToTicker(currentTicker, updatedPlayer);
        redisService.addPlayerToTicker(currentTicker, updatedPlayer);
    }

    void handleTickerRequest() {
        if (Objects.nonNull(getTickerEngine().getActiveTicker()))
            commandBus.publish(Commands.TICKER_SELECTED, this, getTickerEngine().getActiveTicker());
    }

    public Ticker getActiveTicker() {
        return getTickerEngine().getActiveTicker();
    }

    public void setActiveTicker(Ticker ticker) {
        getTickerEngine().setActiveTicker(ticker);
    }

    public void initialize() {
        logger.info("Initializing session manager");

        userConfigurationEngine = new UserConfigurationEngine();
        // userConfigurationEngine.loadConfiguration();

        instrumentEngine = new InstrumentEngine();
        List<Instrument> instruments = userConfigurationEngine.getCurrentConfig().getInstruments();
        instrumentEngine.initializeCache(instruments);

        tickerEngine = new TickerEngine();
        getTickerEngine().loadActiveTicker();
        logTickerState(getTickerEngine().getActiveTicker());

        songEngine = new SongEngine();
        // songEngine.initialize();

        commandBus.register(new CommandListener() {
            public void onAction(Command action) {
                if (action == null || action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                    case Commands.TICKER_REQUEST -> handleTickerRequest();
                    case Commands.TRANSPORT_REWIND -> moveBack();
                    case Commands.TRANSPORT_FORWARD -> moveForward();
                    case Commands.TRANSPORT_PLAY -> getActiveTicker().play();
                    case Commands.TRANSPORT_STOP -> getActiveTicker().stop();
                    case Commands.TRANSPORT_RECORD -> redisService.saveTicker(getActiveTicker());
                    case Commands.SHOW_PLAYER_EDITOR_OK -> processPlayerEdit((Player) action.getData());
                    case Commands.SHOW_RULE_EDITOR_OK -> processRuleEdit((Rule) action.getData());
                }
            }
        });
    }

    public boolean canMoveBack() {
        return getTickerEngine().canMoveBack();
    }

    public boolean canMoveForward() {
        return getTickerEngine().canMoveForward();
    }

    public void moveBack() {
        getTickerEngine().moveBack();
    }

    public void moveForward() {
        getTickerEngine().moveForward();
    }

    public void tickerSelected(Ticker ticker) {
        getTickerEngine().tickerSelected(ticker);
    }

    private void processPlayerEdit(Player player) {
        logger.info("Processing player edit/add: " + player.getName());

        RedisService redis = RedisService.getInstance();

        if (player.getId() == null) {
            redis.savePlayer(player);

            // Add to ticker
            getActiveTicker().getPlayers().add(player);

            // TODO: don't save ticker here

            redis.saveTicker(getActiveTicker());

            // Get fresh ticker state
            // activeTicker = redis.findTickerById(activeTicker.getId());

            logger.info("Added new player and updated ticker");
        } else {
            // TODO: don't save player here, just update the active ticker

            // Existing player update
            redis.savePlayer(player);
            // activeTicker = redis.findTickerById(activeTicker.getId());
            redis.saveTicker(getActiveTicker());
            logger.info("Updated existing player and ticker");
        }

        // Notify UI
        commandBus.publish(Commands.TICKER_UPDATED, this, getActiveTicker());
    }

    private Object processRuleEdit(Rule data) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'processRuleEdit'");
    }

    // case Commands.SHOW_PLAYER_EDITOR_OK -> {
    // if (action.getData() instanceof Player player) {
    // logger.info("Processing player edit/add: " + player.getName());
    // RedisService redis = RedisService.getInstance();

    // if (player.getId() == null) {
    // // New player
    // Player newPlayer = redis.newPlayer();
    // // Copy edited properties to new player
    // newPlayer.setName(player.getName());
    // newPlayer.setInstrument(player.getInstrument());
    // newPlayer.setInstrumentId(player.getInstrumentId());
    // // ... copy other properties ...

    // // Save player first
    // redis.savePlayer(newPlayer);

    // // Add to ticker
    // if (activeTicker != null) {
    // activeTicker.getPlayers().add(newPlayer);
    // redis.saveTicker(activeTicker);

    // // Get fresh ticker state
    // activeTicker = redis.findTickerById(activeTicker.getId());

    // // Notify UI
    // commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
    // logger.info("Added new player and updated ticker");
    // }
    // } else {
    // // Existing player update
    // redis.savePlayer(player);
    // activeTicker = redis.findTickerById(activeTicker.getId());
    // redis.saveTicker(activeTicker);
    // commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
    // logger.info("Updated existing player and ticker");
    // }
    // }
    // }
    // case Commands.RULE_DELETE_REQUEST -> {
    // Player selected = PlayerManager.getInstance().getActivePlayer();
    // boolean wasDeleted = false;

    // if (selected != null && action.getData() instanceof Rule[] rules)
    // for (Rule rule : rules)
    // if (selected.getRules().remove(rule)) {
    // logger.info("Processing rule delete request for Rule " + rule.getId());
    // redisService.deleteRule(rule.getId());
    // redisService.savePlayer(selected);
    // commandBus.publish(Commands.PLAYER_UPDATED, this, selected);
    // logger.info("Rule deleted and player updated");
    // wasDeleted = true;
    // }

    // if (wasDeleted) {
    // redisService.saveTicker(activeTicker);
    // commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
    // commandBus.publish(Commands.PLAYER_SELECTED, this, selected);
    // }
    // }

    // case Commands.PLAYER_DELETE_REQUEST -> {
    // if (action.getData() instanceof Player[] players) {
    // logger.info("Processing delete request for " + players.length + " players");

    // // We already have the active ticker
    // if (activeTicker != null) {
    // boolean wasDeleted = false;

    // // Process each player
    // for (Player player : players) {
    // logger.info("Deleting player: " + player.getId());
    // if (activeTicker.getPlayers().remove(player)) {
    // wasDeleted = true;
    // redisService.deletePlayer(player);
    // logger.info("Player deleted: " + player.getId());
    // }
    // }

    // if (wasDeleted) {
    // // Save updated ticker
    // redisService.saveTicker(activeTicker);
    // logger.info("Saved ticker after player deletion");

    // // Notify UI
    // commandBus.publish(Commands.PLAYER_UNSELECTED, this);
    // commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
    // }
    // }
    // }
    // }
    // case Commands.RULE_ADD_REQUEST -> {
    // if (action.getData() instanceof Player player) {
    // // Just show the dialog, don't create rule yet
    // commandBus.publish(Commands.SHOW_RULE_EDITOR, this, player);
    // }
    // }
    // case Commands.SHOW_RULE_EDITOR_OK -> {
    // if (action.getData() instanceof Rule rule) {
    // RedisService redis = RedisService.getInstance();
    // Player player = redis.findPlayerForRule(rule);

    // if (player != null) {
    // // For existing rule, just save it
    // if (rule.getId() != null) {
    // redis.saveRule(rule);
    // } else {
    // // For new rule, add it to player
    // redis.addRuleToPlayer(player, rule);
    // // Notify about new rule
    // commandBus.publish(Commands.RULE_ADDED, this, new Object[] { player, rule });
    // }

    // // Get fresh states
    // Player refreshedPlayer = redis.findPlayerById(player.getId());
    // activeTicker = redis.findTickerById(activeTicker.getId());

    // // Notify UI in correct order
    // commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
    // // Force rules panel to update by re-selecting the player
    // commandBus.publish(Commands.PLAYER_UNSELECTED, this);
    // commandBus.publish(Commands.PLAYER_SELECTED, this, refreshedPlayer);

    // logger.info(String.format("Rule saved and player refreshed - Player: %d,
    // Rules: %d",
    // refreshedPlayer.getName(),
    // refreshedPlayer.getRules() != null ? refreshedPlayer.getRules().size() : 0));
    // }
    // }
    // }
    // case Commands.RULE_EDIT_REQUEST -> {
    // if (action.getData() instanceof Rule rule) {
    // commandBus.publish(Commands.SHOW_RULE_EDITOR, this, rule);
    // }
    // }
    // case Commands.RULE_UPDATED -> {
    // if (action.getData() instanceof Rule rule) {
    // RedisService redis = RedisService.getInstance();
    // Player player = redis.findPlayerForRule(rule);
    // if (player != null) {
    // redis.saveRule(rule);

    // // Get fresh state of both player and ticker
    // player = redis.findPlayerById(player.getId());
    // activeTicker = redis.findTickerById(activeTicker.getId());

    // // Notify UI of updates in correct order:
    // commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
    // commandBus.publish(Commands.PLAYER_UPDATED, this, player);
    // // Send rule selected to restore selection
    // // commandBus.publish(Commands.RULE_SELECTED, this, rule);
    // }
    // }
    // }
    // case Commands.CLEAR_INVALID_TICKERS -> {
    // RedisService redis = RedisService.getInstance();
    // Long currentTickerId = activeTicker != null ? activeTicker.getId() : null;
    // boolean wasActiveTickerDeleted = false;

    // // Get all ticker IDs sorted before we start deleting
    // List<Long> tickerIds = redis.getAllTickerIds();
    // Collections.sort(tickerIds);

    // // Track the nearest valid ticker to our current one
    // Long nearestValidTickerId = null;
    // Long lastValidTickerId = null;

    // // First pass: find nearest valid ticker and track the last valid one
    // for (Long id : tickerIds) {
    // Ticker ticker = redis.findTickerById(id);
    // if (ticker != null && ticker.isValid()) {
    // if (currentTickerId != null) {
    // if (id < currentTickerId
    // && (nearestValidTickerId == null || id > nearestValidTickerId)) {
    // nearestValidTickerId = id;
    // }
    // }
    // lastValidTickerId = id;
    // }
    // }

    // // Second pass: delete invalid tickers
    // for (Long id : tickerIds) {
    // Ticker ticker = redis.findTickerById(id);
    // if (ticker != null && !ticker.isValid()) {
    // if (id.equals(currentTickerId)) {
    // wasActiveTickerDeleted = true;
    // }
    // redis.deleteTicker(id);
    // }
    // }

    // // Handle active ticker selection if needed
    // if (wasActiveTickerDeleted || activeTicker == null) {
    // Ticker newActiveTicker = null;

    // if (nearestValidTickerId != null) {
    // // Use the nearest valid ticker before our current position
    // newActiveTicker = redis.findTickerById(nearestValidTickerId);
    // } else if (lastValidTickerId != null) {
    // // Fall back to the last valid ticker we found
    // newActiveTicker = redis.findTickerById(lastValidTickerId);
    // } else {
    // // No valid tickers exist, create a new one
    // newActiveTicker = redis.newTicker();
    // }

    // // Update active ticker and notify UI
    // activeTicker = newActiveTicker;
    // commandBus.publish(Commands.TICKER_SELECTED, this, activeTicker);
    // } else {
    // // Our ticker was valid, but refresh the UI anyway to show updated navigation
    // // state
    // commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
    // }
    // }
    // case Commands.TRANSPORT_PLAY -> {
    // if (activeTicker != null) {
    // activeTicker.play();
    // // Notify UI about transport state change
    // commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
    // }
    // }
    // case Commands.TRANSPORT_STOP -> {
    // if (activeTicker != null) {
    // activeTicker.stop();
    // // Notify UI about transport state change
    // commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
    // }
    // }
    // case Commands.TRANSPORT_RECORD -> {
    // if (activeTicker != null) {
    // redisService.saveTicker(activeTicker);
    // }
    // }
    // }
    // }
    // });
    // loadActiveTicker();
    // logger.info("TickerManager initialized");

    private void logTickerState(Ticker ticker) {
        if (ticker != null) {
            logger.info("Ticker state:");
            logger.info("  ID: " + ticker.getId());
            logger.info("  BPM: " + ticker.getTempoInBPM());
            logger.info("  Ticks per beat: " + ticker.getTicksPerBeat());
            logger.info("  Beats per bar: " + ticker.getBeatsPerBar());
            logger.info("  Bars: " + ticker.getBars());
            logger.info("  Parts: " + ticker.getParts());

            if (ticker.getPlayers() != null) {
                logger.info("  Players: " + ticker.getPlayers().size());
                ticker.getPlayers().forEach(this::logPlayerState);
            }
        }
    }

    private void logPlayerState(Player player) {
        logger.info("    Player: " + player.getId() + " - " + player.getName());
        if (player.getRules() != null) {
            logger.info("      Rules: " + player.getRules().size());
            player.getRules().forEach(r -> logger.info("        Rule: " + r.getId() +
                    " - Op: " + r.getOperator() +
                    ", Comp: " + r.getComparison() +
                    ", Value: " + r.getValue() +
                    ", Part: " + r.getPart()));
        }
    }

    public Instrument getInstrumentFromCache(Long instrumentId) {
        return instrumentEngine.getInstrumentFromCache(instrumentId);
    }
}
