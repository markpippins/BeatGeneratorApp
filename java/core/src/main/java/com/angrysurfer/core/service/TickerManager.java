// package com.angrysurfer.core.service;

// import java.util.Collections;
// import java.util.List;
// import java.util.Objects;
// import java.util.logging.Logger;

// import com.angrysurfer.core.api.Command;
// import com.angrysurfer.core.api.CommandBus;
// import com.angrysurfer.core.api.CommandListener;
// import com.angrysurfer.core.api.Commands;
// import com.angrysurfer.core.model.Player;
// import com.angrysurfer.core.model.Rule;
// import com.angrysurfer.core.model.Ticker;
// import com.angrysurfer.core.redis.RedisService;

// import lombok.Getter;
// import lombok.Setter;

// @Getter
// @Setter
// public class TickerManager {

//     private static final Logger logger = Logger.getLogger(TickerManager.class.getName());
//     private static TickerManager instance;
//     private final CommandBus commandBus = CommandBus.getInstance();
//     private Ticker activeTicker;
//     private final RedisService redisService = RedisService.getInstance();

//     private TickerManager() {
//         commandBus.register(new CommandListener() {
//             public void onAction(Command action) {
//                 if (action == null || action.getCommand() == null)
//                     return;

//                 switch (action.getCommand()) {
//                     case Commands.TICKER_REQUEST -> {
//                         if (Objects.nonNull(activeTicker)) {
//                             commandBus.publish(Commands.TICKER_SELECTED, this, activeTicker);
//                         }
//                     }
//                     // case Commands.TRANSPORT_REWIND -> moveBack();
//                     // case Commands.TRANSPORT_FORWARD -> moveForward();
//                     // case Commands.PLAYER_ADD_REQUEST -> {
//                     //     // Just show the editor, don't create player yet
//                     //     commandBus.publish(Commands.SHOW_PLAYER_EDITOR, this, null);
//                     // }
//                     // case Commands.PLAYER_EDIT_REQUEST -> {
//                     //     if (action.getData() instanceof Player player) { // Changed from ProxyStrike[]
//                     //         // Open editor with the selected player
//                     //         commandBus.publish(Commands.SHOW_PLAYER_EDITOR, this, player);
//                     //     }
//                     // }
//                     // case Commands.SHOW_PLAYER_EDITOR_OK -> {
//                     //     if (action.getData() instanceof Player player) {
//                     //         logger.info("Processing player edit/add: " + player.getName());
//                     //         RedisService redis = RedisService.getInstance();

//                     //         if (player.getId() == null) {
//                     //             // New player
//                     //             Player newPlayer = redis.newPlayer();
//                     //             // Copy edited properties to new player
//                     //             newPlayer.setName(player.getName());
//                     //             newPlayer.setInstrument(player.getInstrument());
//                     //             newPlayer.setInstrumentId(player.getInstrumentId());
//                     //             // ... copy other properties ...

//                     //             // Save player first
//                     //             redis.savePlayer(newPlayer);

//                     //             // Add to ticker
//                     //             if (activeTicker != null) {
//                     //                 activeTicker.getPlayers().add(newPlayer);
//                     //                 redis.saveTicker(activeTicker);

//                     //                 // Get fresh ticker state
//                     //                 activeTicker = redis.findTickerById(activeTicker.getId());

//                     //                 // Notify UI
//                     //                 commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
//                     //                 logger.info("Added new player and updated ticker");
//                     //             }
//                     //         } else {
//                     //             // Existing player update
//                     //             redis.savePlayer(player);
//                     //             activeTicker = redis.findTickerById(activeTicker.getId());
//                     //             redis.saveTicker(activeTicker);
//                     //             commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
//                     //             logger.info("Updated existing player and ticker");
//                     //         }
//                     //     }
//                     // }
//                     case Commands.RULE_DELETE_REQUEST -> {
//                         Player selected = PlayerManager.getInstance().getActivePlayer();
//                         boolean wasDeleted = false;

//                         if (selected != null && action.getData() instanceof Rule[] rules)
//                             for (Rule rule : rules)
//                                 if (selected.getRules().remove(rule)) {
//                                     logger.info("Processing rule delete request for Rule " + rule.getId());
//                                     redisService.deleteRule(rule.getId());
//                                     redisService.savePlayer(selected);
//                                     commandBus.publish(Commands.PLAYER_UPDATED, this, selected);
//                                     logger.info("Rule deleted and player updated");
//                                     wasDeleted = true;
//                                 }

//                         if (wasDeleted) {
//                             redisService.saveTicker(activeTicker);
//                             commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
//                             commandBus.publish(Commands.PLAYER_SELECTED, this, selected);
//                         }
//                     }

//                     case Commands.PLAYER_DELETE_REQUEST -> {
//                         if (action.getData() instanceof Player[] players) {
//                             logger.info("Processing delete request for " + players.length + " players");

//                             // We already have the active ticker
//                             if (activeTicker != null) {
//                                 boolean wasDeleted = false;

//                                 // Process each player
//                                 for (Player player : players) {
//                                     logger.info("Deleting player: " + player.getId());
//                                     if (activeTicker.getPlayers().remove(player)) {
//                                         wasDeleted = true;
//                                         redisService.deletePlayer(player);
//                                         logger.info("Player deleted: " + player.getId());
//                                     }
//                                 }

//                                 if (wasDeleted) {
//                                     // Save updated ticker
//                                     redisService.saveTicker(activeTicker);
//                                     logger.info("Saved ticker after player deletion");

//                                     // Notify UI
//                                     commandBus.publish(Commands.PLAYER_UNSELECTED, this);
//                                     commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
//                                 }
//                             }
//                         }
//                     }
//                     case Commands.RULE_ADD_REQUEST -> {
//                         if (action.getData() instanceof Player player) {
//                             // Just show the dialog, don't create rule yet
//                             commandBus.publish(Commands.SHOW_RULE_EDITOR, this, player);
//                         }
//                     }
//                     case Commands.SHOW_RULE_EDITOR_OK -> {
//                         if (action.getData() instanceof Rule rule) {
//                             RedisService redis = RedisService.getInstance();
//                             Player player = redis.findPlayerForRule(rule);

//                             if (player != null) {
//                                 // For existing rule, just save it
//                                 if (rule.getId() != null) {
//                                     redis.saveRule(rule);
//                                 } else {
//                                     // For new rule, add it to player
//                                     redis.addRuleToPlayer(player, rule);
//                                     // Notify about new rule
//                                     commandBus.publish(Commands.RULE_ADDED, this, new Object[] { player, rule });
//                                 }

//                                 // Get fresh states
//                                 Player refreshedPlayer = redis.findPlayerById(player.getId());
//                                 activeTicker = redis.findTickerById(activeTicker.getId());

//                                 // Notify UI in correct order
//                                 commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
//                                 // Force rules panel to update by re-selecting the player
//                                 commandBus.publish(Commands.PLAYER_UNSELECTED, this);
//                                 commandBus.publish(Commands.PLAYER_SELECTED, this, refreshedPlayer);

//                                 logger.info(String.format("Rule saved and player refreshed - Player: %d, Rules: %d",
//                                         refreshedPlayer.getName(),
//                                         refreshedPlayer.getRules() != null ? refreshedPlayer.getRules().size() : 0));
//                             }
//                         }
//                     }
//                     case Commands.RULE_EDIT_REQUEST -> {
//                         if (action.getData() instanceof Rule rule) {
//                             commandBus.publish(Commands.SHOW_RULE_EDITOR, this, rule);
//                         }
//                     }
//                     case Commands.RULE_UPDATED -> {
//                         if (action.getData() instanceof Rule rule) {
//                             RedisService redis = RedisService.getInstance();
//                             Player player = redis.findPlayerForRule(rule);
//                             if (player != null) {
//                                 redis.saveRule(rule);

//                                 // Get fresh state of both player and ticker
//                                 player = redis.findPlayerById(player.getId());
//                                 activeTicker = redis.findTickerById(activeTicker.getId());

//                                 // Notify UI of updates in correct order:
//                                 commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
//                                 commandBus.publish(Commands.PLAYER_UPDATED, this, player);
//                                 // Send rule selected to restore selection
//                                 // commandBus.publish(Commands.RULE_SELECTED, this, rule);
//                             }
//                         }
//                     }
//                     case Commands.CLEAR_INVALID_TICKERS -> {
//                         RedisService redis = RedisService.getInstance();
//                         Long currentTickerId = activeTicker != null ? activeTicker.getId() : null;
//                         boolean wasActiveTickerDeleted = false;

//                         // Get all ticker IDs sorted before we start deleting
//                         List<Long> tickerIds = redis.getAllTickerIds();
//                         Collections.sort(tickerIds);

//                         // Track the nearest valid ticker to our current one
//                         Long nearestValidTickerId = null;
//                         Long lastValidTickerId = null;

//                         // First pass: find nearest valid ticker and track the last valid one
//                         for (Long id : tickerIds) {
//                             Ticker ticker = redis.findTickerById(id);
//                             if (ticker != null && ticker.isValid()) {
//                                 if (currentTickerId != null) {
//                                     if (id < currentTickerId
//                                             && (nearestValidTickerId == null || id > nearestValidTickerId)) {
//                                         nearestValidTickerId = id;
//                                     }
//                                 }
//                                 lastValidTickerId = id;
//                             }
//                         }

//                         // Second pass: delete invalid tickers
//                         for (Long id : tickerIds) {
//                             Ticker ticker = redis.findTickerById(id);
//                             if (ticker != null && !ticker.isValid()) {
//                                 if (id.equals(currentTickerId)) {
//                                     wasActiveTickerDeleted = true;
//                                 }
//                                 redis.deleteTicker(id);
//                             }
//                         }

//                         // Handle active ticker selection if needed
//                         if (wasActiveTickerDeleted || activeTicker == null) {
//                             Ticker newActiveTicker = null;

//                             if (nearestValidTickerId != null) {
//                                 // Use the nearest valid ticker before our current position
//                                 newActiveTicker = redis.findTickerById(nearestValidTickerId);
//                             } else if (lastValidTickerId != null) {
//                                 // Fall back to the last valid ticker we found
//                                 newActiveTicker = redis.findTickerById(lastValidTickerId);
//                             } else {
//                                 // No valid tickers exist, create a new one
//                                 newActiveTicker = redis.newTicker();
//                             }

//                             // Update active ticker and notify UI
//                             activeTicker = newActiveTicker;
//                             commandBus.publish(Commands.TICKER_SELECTED, this, activeTicker);
//                         } else {
//                             // Our ticker was valid, but refresh the UI anyway to show updated navigation
//                             // state
//                             commandBus.publish(Commands.TICKER_UPDATED, this, activeTicker);
//                         }
//                     }
//                     case Commands.TRANSPORT_PLAY -> {
//                         if (activeTicker != null) {
//                             activeTicker.play();
//                             // Notify UI about transport state change
//                             commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
//                         }
//                     }
//                     case Commands.TRANSPORT_STOP -> {
//                         if (activeTicker != null) {
//                             activeTicker.stop();
//                             // Notify UI about transport state change
//                             commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
//                         }
//                     }
//                     case Commands.TRANSPORT_RECORD -> {
//                         if (activeTicker != null) {
//                             redisService.saveTicker(activeTicker);
//                         }
//                     }
//                 }
//             }
//         });
//         loadActiveTicker();
//         logger.info("TickerManager initialized");
//     }

//     // public static TickerManager getInstance() {
//     // if (instance == null) {
//     // synchronized (TickerManager.class) {
//     // if (instance == null) {
//     // instance = new TickerManager();
//     // }
//     // }
//     // }
//     // return instance;
//     // }

//     public void setActiveTicker(Ticker ticker) {
//         activeTicker = ticker;
//         commandBus.publish(Commands.TICKER_SELECTED, this, ticker);
//     }

//     public Ticker getActiveTicker() {
//         return activeTicker;
//     }

//     public boolean canMoveBack() {
//         RedisService redis = RedisService.getInstance();
//         Long minId = redis.getMinimumTickerId();
//         return (activeTicker != null && minId != null && activeTicker.getId() > minId);
//     }

//     public boolean canMoveForward() {
//         if (activeTicker == null || activeTicker.getPlayers() == null || activeTicker.getPlayers().isEmpty()) {
//             return false;
//         }

//         for (Player player : activeTicker.getPlayers()) {
//             if (player.getRules() != null && !player.getRules().isEmpty()) {
//                 return true;
//             }
//         }
//         return false;
//     }

//     public void moveBack() {
//         RedisService redis = RedisService.getInstance();
//         Long prevId = redis.getPreviousTickerId(activeTicker);
//         if (prevId != null) {
//             tickerSelected(redis.findTickerById(prevId));
//             if (activeTicker != null) {
//                 logger.info("Moved back to ticker: " + activeTicker.getId());
//             }
//         }
//     }

//     public void moveForward() {
//         RedisService redis = RedisService.getInstance();
//         Long maxId = redis.getMaximumTickerId();

//         if (activeTicker != null && maxId != null && activeTicker.getId().equals(maxId)) {
//             // Only create a new ticker if current one is valid and has active rules
//             if (activeTicker.isValid() && !activeTicker.getPlayers().isEmpty() &&
//                     activeTicker.getPlayers().stream()
//                             .map(p -> p)
//                             .anyMatch(p -> p.getRules() != null && !p.getRules().isEmpty())) {

//                 Ticker newTicker = redis.newTicker();
//                 tickerSelected(newTicker);
//                 commandBus.publish(Commands.TICKER_CREATED, this, newTicker); // Add this line
//                 logger.info("Created new ticker and moved forward to it: " + newTicker.getId());
//             }
//         } else {
//             // Otherwise, move to the next existing ticker
//             Long nextId = redis.getNextTickerId(activeTicker);
//             if (nextId != null) {
//                 tickerSelected(redis.findTickerById(nextId));
//                 if (activeTicker != null) {
//                     logger.info("Moved forward to ticker: " + activeTicker.getId());
//                 }
//             }
//         }
//     }

//     public void tickerSelected(Ticker ticker) {
//         if (ticker != null && !ticker.equals(this.activeTicker)) {
//             this.activeTicker = ticker;
//             commandBus.publish(Commands.TICKER_SELECTED, this, ticker);
//             logger.info("Ticker selected: " + ticker.getId());
//         }
//     }

//     public void deleteAllTickers() {
//         RedisService redis = RedisService.getInstance();

//         logger.info("Deleting tickers");
//         redis.getAllTickerIds().forEach(id -> {
//             Ticker ticker = redis.findTickerById(id);
//             if (ticker != null) {
//                 logger.info(String.format("Loading ticker {}", id));
//                 redis.deleteTicker(id);
//             }
//         });
//     }

//     private void loadActiveTicker() {
//         RedisService redis = RedisService.getInstance();

//         logger.info("Loading ticker");
//         Long minId = redis.getMinimumTickerId();
//         Long maxId = redis.getMaximumTickerId();

//         logger.info("Minimum ticker ID: " + minId);
//         logger.info("Maximum ticker ID: " + maxId);

//         Ticker ticker = null;

//         // If we have existing tickers, try to load the first valid one
//         if (minId != null && maxId != null) {
//             for (Long id = minId; id <= maxId; id++) {
//                 ticker = redis.findTickerById(id);
//                 if (ticker != null) {
//                     logger.info("Found valid ticker " + ticker.getId());
//                     break;
//                 }
//             }
//         }

//         // If no valid ticker found or no tickers exist, create a new one
//         if (ticker == null) {
//             logger.info("No valid ticker found, creating new ticker");
//             ticker = redis.newTicker();
//             redis.saveTicker(ticker);
//             logger.info("Created new ticker with ID: " + ticker.getId());
//         }

//         setActiveTicker(ticker);
//         logTickerState(ticker);
//     }

//     private void logTickerState(Ticker ticker) {
//         if (ticker != null) {
//             logger.info("Ticker state:");
//             logger.info("  ID: " + ticker.getId());
//             logger.info("  BPM: " + ticker.getTempoInBPM());
//             logger.info("  Ticks per beat: " + ticker.getTicksPerBeat());
//             logger.info("  Beats per bar: " + ticker.getBeatsPerBar());
//             logger.info("  Bars: " + ticker.getBars());
//             logger.info("  Parts: " + ticker.getParts());

//             if (ticker.getPlayers() != null) {
//                 logger.info("  Players: " + ticker.getPlayers().size());
//                 ticker.getPlayers().forEach(this::logPlayerState);
//             }
//         }
//     }

//     private void logPlayerState(Player player) {
//         logger.info("    Player: " + player.getId() + " - " + player.getName());
//         if (player.getRules() != null) {
//             logger.info("      Rules: " + player.getRules().size());
//             player.getRules().forEach(r -> logger.info("        Rule: " + r.getId() +
//                     " - Op: " + r.getOperator() +
//                     ", Comp: " + r.getComparison() +
//                     ", Value: " + r.getValue() +
//                     ", Part: " + r.getPart()));
//         }
//     }

//     public void addPlayerToTicker(Ticker currentTicker, Player updatedPlayer) {
//         redisService.addPlayerToTicker(currentTicker, updatedPlayer);
//     }

//     // public static void main(String[] args) {
//     // Logger logger = Logger.getLogger(TickerManager.class.getName());
//     // logger.info("Starting TickerManager demonstration");

//     // try {
//     // // Get services
//     // RedisService redis = RedisService.getInstance();
//     // TickerManager manager = TickerManager.getInstance();

//     // manager.deleteAllTickers();

//     // // Create and save a ticker
//     // logger.info("Creating new ticker");
//     // Ticker ticker = redis.newTicker();
//     // ticker.setTempoInBPM(120F);
//     // ticker.setTicksPerBeat(4);
//     // ticker.setBeatsPerBar(4);
//     // ticker.setBars(4);
//     // ticker.setParts(16);

//     // // Create a player (Player)
//     // logger.info("Creating new player");
//     // Strike player = new Strike();
//     // player.setId(redis.getNextPlayerId());
//     // player.setName("Test Player");
//     // player.setMuted(false);
//     // player.setChannel(9); // Typical drum channel
//     // player.setNote(36L); // Bass drum
//     // // player.setVelocity(100);

//     // // Create a rule
//     // logger.info("Creating new rule");
//     // Rule rule = new Rule();
//     // rule.setId(redis.getNextRuleId());
//     // rule.setOperator(0);
//     // rule.setComparison(1);
//     // rule.setValue(1.0); // Hit on first beat
//     // rule.setPart(1);

//     // // Add rule to player
//     // logger.info("Adding rule to player");
//     // player.setRules(new HashSet<>());
//     // player.getRules().add(rule);

//     // // Add player to ticker
//     // logger.info("Adding player to ticker");
//     // manager.addPlayerToTicker(ticker, player);

//     // // Save ticker
//     // logger.info("Saving ticker");
//     // redis.saveTicker(ticker);
//     // Long savedTickerId = ticker.getId();
//     // logger.info("Saved ticker with ID: " + savedTickerId);

//     // // Clear references
//     // ticker = null;
//     // player = null;
//     // rule = null;
//     // manager.setActiveTicker(null);

//     // // Reload ticker
//     // logger.info("Reloading ticker from Redis");
//     // Ticker loadedTicker = redis.findTickerById(savedTickerId);

//     // // Verify loaded data
//     // if (loadedTicker != null) {
//     // logger.info("Successfully loaded ticker: " + loadedTicker.getId());
//     // logger.info("Ticker state:");
//     // logger.info(" BPM: " + loadedTicker.getTempoInBPM());
//     // logger.info(" Ticks per beat: " + loadedTicker.getTicksPerBeat());
//     // logger.info(" Beats per bar: " + loadedTicker.getBeatsPerBar());

//     // if (loadedTicker.getPlayers() != null) {
//     // loadedTicker.getPlayers().forEach(p -> {
//     // Player loadedPlayer = p;
//     // logger.info(" Player: " + loadedPlayer.getId() + " - " +
//     // loadedPlayer.getName());
//     // logger.info(" Channel: " + loadedPlayer.getChannel());
//     // logger.info(" Note: " + loadedPlayer.getNote());
//     // // logger.info(" Velocity: " + loadedPlayer.getVelocity());

//     // if (loadedPlayer.getRules() != null) {
//     // loadedPlayer.getRules().forEach(r -> logger.info(" Rule: " + r.getId() +
//     // " - Op: " + r.getOperator() +
//     // ", Comp: " + r.getComparison() +
//     // ", Value: " + r.getValue() +
//     // ", Part: " + r.getPart()));
//     // }
//     // });
//     // }
//     // }

//     // logger.info("Demonstration completed successfully");

//     // } catch (Exception e) {
//     // logger.severe("Error in demonstration: " + e.getMessage());
//     // e.printStackTrace();
//     // }
//     // }
// }