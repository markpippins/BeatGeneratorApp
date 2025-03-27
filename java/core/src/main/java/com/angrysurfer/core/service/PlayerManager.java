package com.angrysurfer.core.service;

import static com.angrysurfer.core.util.update.PlayerUpdateType.BEAT_FRACTION;
import static com.angrysurfer.core.util.update.PlayerUpdateType.CHANNEL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.FADE_IN;
import static com.angrysurfer.core.util.update.PlayerUpdateType.FADE_OUT;
import static com.angrysurfer.core.util.update.PlayerUpdateType.LEVEL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MAX_VELOCITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MIN_VELOCITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MUTE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.NOTE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.PRESET;
import static com.angrysurfer.core.util.update.PlayerUpdateType.PROBABILITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RANDOM_DEGREE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RATCHET_COUNT;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RATCHET_INTERVAL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SKIPS;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SOLO;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SUBDIVISIONS;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SWING;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerManager {
    private static final Logger logger = LoggerFactory.getLogger(PlayerManager.class);
    private static PlayerManager instance;
    private CommandBus commandBus = CommandBus.getInstance();
    private Player activePlayer;
    private final RedisService redisService;
    private java.util.Timer saveTimer;

    private PlayerManager() {
        setupCommandBusListener();
        redisService = RedisService.getInstance();
    }

    public static PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    public Player initializeNewPlayer() {
        Player player = new Strike();
        player.setId(redisService.getNextPlayerId());
        player.setRules(new HashSet<>());
        return player;
    }

    private void setupCommandBusListener() {
        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) return;
                
                switch (action.getCommand()) {
                    case Commands.PLAYER_SELECTED -> {
                        if (action.getData() instanceof Player) {
                            playerSelected((Player) action.getData());
                        }
                    }
                    
                    case Commands.PLAYER_UNSELECTED -> {
                        logger.info("Player unselected");
                        activePlayer = null;
                    }
                    
                    case Commands.PLAYER_UPDATED -> {
                        if (action.getData() instanceof Player && !action.getSender().equals(this)) {
                            playerUpdated((Player) action.getData());
                        }
                    }
                    
                    case Commands.MINI_NOTE_SELECTED -> {
                        if (action.getData() instanceof Number) {
                            int midiNote = ((Number) action.getData()).intValue();
                            sendNoteToActivePlayer(midiNote);
                        }
                    }
                    
                    // Add preset change handlers with preview - replace existing implementation
                    case Commands.PRESET_UP -> {
                        if (activePlayer != null) {
                            // Increment preset value (with upper bound of 127)
                            int currentPreset = activePlayer.getPreset() != null ? 
                                              activePlayer.getPreset().intValue() : 0;
                            int newPreset = Math.min(127, currentPreset + 1);
                            
                            // Update the preset
                            activePlayer.setPreset((long) newPreset);
                            
                            // First publish lightweight update for immediate UI feedback
                            commandBus.publish(Commands.PRESET_CHANGED, this, 
                                Map.of("playerId", activePlayer.getId(), 
                                       "preset", newPreset, 
                                       "playerName", activePlayer.getName()));
                            
                            // Then publish full player update for complete UI refresh
                            commandBus.publish(Commands.PLAYER_UPDATED, activePlayer);
                            
                            // Play a preview note immediately
                            sendNoteToActivePlayer(60);
                            
                            // Schedule saving to Redis after a delay to avoid rapid successive saves
                            schedulePlayerSave(activePlayer);
                        }
                    }
                    
                    case Commands.PRESET_DOWN -> {
                        if (activePlayer != null) {
                            // Decrement preset value (with lower bound of 0)
                            int currentPreset = activePlayer.getPreset() != null ? 
                                              activePlayer.getPreset().intValue() : 0;
                            int newPreset = Math.max(0, currentPreset - 1);
                            
                            // Update the preset
                            activePlayer.setPreset((long) newPreset);
                            
                            // First publish lightweight update for immediate UI feedback
                            commandBus.publish(Commands.PRESET_CHANGED, this, 
                                Map.of("playerId", activePlayer.getId(), 
                                       "preset", newPreset, 
                                       "playerName", activePlayer.getName()));
                            
                            // Then publish full player update for complete UI refresh
                            commandBus.publish(Commands.PLAYER_UPDATED, activePlayer);
                            
                            // Play a preview note immediately
                            sendNoteToActivePlayer(60);
                            
                            // Schedule saving to Redis after a delay to avoid rapid successive saves
                            schedulePlayerSave(activePlayer);
                        }
                    }
                    
                    case Commands.RULE_DELETE_REQUEST -> {
                        if (action.getData() instanceof Rule[] rules) {
                            logger.info("Processing rule delete request for {} rules", rules.length);

                            // Get player for first rule (all rules should be from same player)
                            Player player = getActivePlayer();
                            if (player != null) {
                                // Get a fresh copy of the player to ensure we have current state
                                // Player freshPlayer = redisService.findPlayerById(player.getId());
                                // if (freshPlayer == null) {
                                //     logger.error("Could not find player with ID: {}", player.getId());
                                //     return;
                                // }
                                
                                // Track if we deleted any rules
                                boolean deletedRules = false;
                                
                                // Remove rules from player and Redis
                                for (Rule rule : rules) {
                                    Long ruleId = rule.getId();
                                    
                                    // First remove from Redis directly
                                    redisService.deleteRule(ruleId);
                                    logger.info("Attempted to delete rule from Redis: {}", ruleId);
                                    
                                    // Then remove from player's collection
                                    if (player.getRules() != null) {
                                        boolean removed = player.getRules().removeIf(r -> r.getId().equals(ruleId));
                                        if (removed) {
                                            deletedRules = true;
                                            logger.info("Removed rule {} from player's collection", ruleId);
                                        } else {
                                            logger.warn("Rule {} not found in player's collection", ruleId);
                                        }
                                    }
                                }
                                
                                if (deletedRules) {
                                    // Save updated player - Don't assign to boolean since method returns void
                                    redisService.savePlayer(player);
                                    logger.info("Player {} saved after rule deletion", player.getId());
                                    
                                    // Get another fresh copy after saving
                                    // Player refreshedPlayer = redisService.findPlayerById(freshPlayer.getId());
                                    
                                    // Notify UI
                                    commandBus.publish(Commands.RULE_DELETED, this, player);
                                    commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                                    commandBus.publish(Commands.PLAYER_SELECTED, this, player);
                                    
                                    // Log rules count after operation
                                    int rulesCount = player.getRules() != null ? player.getRules().size() : 0;
                                    logger.info("Player {} now has {} rules after deletion", player.getId(), rulesCount);
                                    
                                    debugPlayerRules(player);
                                }
                            }
                        }
                    }

                    case Commands.NEW_VALUE_VELOCITY_MIN -> {
                        if (action.getData() instanceof Object[] data && data.length >= 2) {
                            if (data[0] instanceof Long playerId && data[1] instanceof Long value) {
                                Session currentSession = SessionManager.getInstance().getActiveSession();
                                if (currentSession != null) {
                                    Player player = currentSession.getPlayer(playerId);
                                    if (player != null) {
                                        // Set new min velocity
                                        player.setMinVelocity(value);
                                        
                                        // Ensure max velocity is at least min velocity
                                        if (player.getMaxVelocity() < value) {
                                            player.setMaxVelocity(value);
                                        }
                                        
                                        // Save player
                                        savePlayerProperties(player);
                                        
                                        // Publish player update
                                        commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                                        commandBus.publish(Commands.PLAYER_ROW_REFRESH, this, player);
                                    }
                                }
                            }
                        }
                    }

                    case Commands.NEW_VALUE_VELOCITY_MAX -> {
                        if (action.getData() instanceof Object[] data && data.length >= 2) {
                            if (data[0] instanceof Long playerId && data[1] instanceof Long value) {
                                Session currentSession = SessionManager.getInstance().getActiveSession();
                                if (currentSession != null) {
                                    Player player = currentSession.getPlayer(playerId);
                                    if (player != null) {
                                        // Set new max velocity
                                        player.setMaxVelocity(value);
                                        
                                        // Ensure min velocity doesn't exceed max velocity
                                        if (player.getMinVelocity() > value) {
                                            player.setMinVelocity(value);
                                        }
                                        
                                        // Save player
                                        savePlayerProperties(player);
                                        
                                        // Publish player update
                                        commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                                        commandBus.publish(Commands.PLAYER_ROW_REFRESH, this, player);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    // Fix the playerSelected method
    public void playerSelected(Player player) {
        // DON'T publish another event here - this method should just update state
        if (!Objects.equals(activePlayer, player)) {
            this.activePlayer = player;
            // Remove this line to prevent event loop
            // commandBus.publish(Commands.PLAYER_SELECTED, this, player);
        }
    }

    public void playerUpdated(Player player) {
        if (Objects.equals(activePlayer.getId(), player.getId())) {
            this.activePlayer = player;
            commandBus.publish(Commands.PLAYER_SELECTED, this, player);
        }
    }

    public Player addPlayer(Session session, Instrument instrument, long note) {
        String name = instrument.getName() + session.getPlayers().size();
        Player player = new Strike(name, session, instrument, note,
                instrument.getControlCodes().stream().map(cc -> cc.getCode()).toList());
        player.setSession(session);
        session.getPlayers().add(player);
        return player;
    }

    public Rule addRule(Player player, int operator, int comparison, double value, int part) {
        Rule rule = new Rule(operator, comparison, value, part, true);
        
        // Set bidirectional relationship
        rule.setPlayer(player);
        
        // Initialize rules collection if needed
        if (player.getRules() == null) {
            player.setRules(new HashSet<>());
        }
        
        // Don't add duplicate rules
        if (player.getRules().stream().noneMatch(r -> r.isEqualTo(rule))) {
            player.getRules().add(rule);
            
            // IMPORTANT: Save the player to persist the rule relationship
            savePlayerWithRules(player);
            
            // Publish events
            commandBus.publish(Commands.RULE_ADDED, this, player);
            commandBus.publish(Commands.PLAYER_SELECTED, this, player);
            
            return rule;
        }
        return null;
    }

    /**
     * Saves a player and ensures rules are properly persisted
     * @param player The player to save
     */
    private void savePlayerWithRules(Player player) {
        try {
            // Save to Redis without expecting boolean return
            redisService.savePlayer(player);
            
            // Also update in session to ensure consistency
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null) {
                // Find and update player in session
                Set<Player> players = session.getPlayers();
                
                // Remove existing player with same ID
                players.removeIf(p -> p.getId().equals(player.getId()));
                
                // Add updated player
                players.add(player);
                
                // Save session to persist changes without expecting boolean return
                redisService.saveSession(session);
                
                logger.info("Saved player " + player.getName() + " with " + 
                          (player.getRules() != null ? player.getRules().size() : 0) + " rules");
            }
        } catch (Exception e) {
            logger.error("Error saving player: " + e.getMessage());
        }
    }

    public void removeRule(Player player, Long ruleId) {
        if (player == null || ruleId == null) {
            logger.error("Cannot remove rule: player or ruleId is null");
            return;
        }
        
        // Get the rule from player's rules collection
        Rule ruleToRemove = null;
        if (player.getRules() != null) {
            for (Rule r : player.getRules()) {
                if (r.getId().equals(ruleId)) {
                    ruleToRemove = r;
                    break;
                }
            }
        }
        
        if (ruleToRemove != null) {
            // Remove from player's collection
            boolean removed = player.getRules().remove(ruleToRemove);
            if (!removed) {
                logger.warn("Failed to remove rule from player's collection: {}", ruleId);
            }
            
            // Delete from Redis directly without expecting boolean return
            redisService.deleteRule(ruleId);
            logger.info("Attempted to delete rule from Redis: {}", ruleId);
            
            // Save the updated player (without the rule)
            savePlayerWithRules(player);
            
            // Debug the state after removal
            debugPlayerRules(player);
            
            // Notify listeners about the change
            commandBus.publish(Commands.RULE_DELETED, this, player);
            commandBus.publish(Commands.PLAYER_UPDATED, this, player);
            commandBus.publish(Commands.PLAYER_SELECTED, this, player);
            
            logger.info("Removed rule {} from player {}", ruleId, player.getName());
        } else {
            logger.warn("Rule {} not found in player {}", ruleId, player.getName());
        }
    }

    public Set<Player> removePlayer(Session session, Long playerId) {
        Player player = session.getPlayer(playerId);
        session.getPlayers().remove(player);
        return session.getPlayers();
    }

    public void clearPlayers(Session session) {
        Set<Player> players = session.getPlayers();
        players.stream()
                .filter(p -> p.getRules().isEmpty())
                .forEach(p -> {
                    session.getPlayers().remove(p);
                    p.setSession(null);
                });
    }

    public Player updatePlayer(Session session, Long playerId, int updateType, long updateValue) {
        Player player = session.getPlayer(playerId);
        if (player == null)
            return null;

        switch (updateType) {
            case CHANNEL -> {
                player.noteOff(0, 0);
                player.setChannel((int) updateValue);
            }
            case NOTE -> player.setNote(updateValue);
            case PRESET -> handlePresetChange(player, updateValue);
            case PROBABILITY -> player.setProbability(updateValue);
            case MIN_VELOCITY -> player.setMinVelocity(updateValue);
            case MAX_VELOCITY -> player.setMaxVelocity(updateValue);
            case RATCHET_COUNT -> player.setRatchetCount(updateValue);
            case RATCHET_INTERVAL -> player.setRatchetInterval(updateValue);
            case MUTE -> player.setMuted(updateValue > 0);
            case SOLO -> player.setSolo(updateValue > 0);
            case SKIPS -> {
                player.setSkips(updateValue);
                player.getSkipCycler().setLength(updateValue);
                player.getSkipCycler().reset();
            }
            case RANDOM_DEGREE -> player.setRandomDegree(updateValue);
            case BEAT_FRACTION -> player.setBeatFraction(updateValue);
            case SUBDIVISIONS -> {
                player.setSubDivisions(updateValue);
                player.getSubCycler().setLength(updateValue);
                player.getSubCycler().reset();
            }
            case SWING -> player.setSwing(updateValue);
            case LEVEL -> player.setLevel(updateValue);
            case FADE_IN -> player.setFadeIn(updateValue);
            case FADE_OUT -> player.setFadeOut(updateValue);
        }

        return player;
    }

    private void handlePresetChange(Player player, long updateValue) {
        try {
            player.noteOff(0, 0);
            player.setPreset(updateValue);
            player.getInstrument().setDevice(DeviceManager.getMidiDevice(player.getInstrument().getDeviceName()));
            player.getInstrument().programChange(player.getChannel(), updateValue, 0);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            logger.error(e.getMessage(), e);
        }
    }

    public Player mutePlayer(Session session, Long playerId) {
        Player player = session.getPlayer(playerId);
        if (player != null) {
            player.setMuted(!player.isMuted());
        }
        return player;
    }

    public void clearPlayersWithNoRules(Session session) {
        session.getPlayers().stream()
                .filter(p -> p.getRules().isEmpty())
                .forEach(p -> {
                    session.getPlayers().remove(p);
                    p.setSession(null);
                });
    }

    // Player property update methods
    public void updatePlayerLevel(Player player, int level) {
        player.setLevel((long) level);
    }

    public void updatePlayerSparse(Player player, int sparse) {
        player.setSparse(sparse / 100.0);
    }

    public void updatePlayerPan(Player player, int pan) {
        player.setPanPosition((long) pan);
    }

    public void updatePlayerRandom(Player player, int random) {
        player.setRandomDegree((long) random);
    }

    public void updatePlayerVelocityMax(Player player, int velocityMax) {
        player.setMaxVelocity((long) velocityMax);
    }

    public void updatePlayerVelocityMin(Player player, int velocityMin) {
        player.setMinVelocity((long) velocityMin);
    }

    public void updatePlayerProbability(Player player, int probability) {
        player.setProbability((long) probability);
    }

    public void updatePlayerSwing(Player player, int swing) {
        player.setSwing((long) swing);
    }

    public void updatePlayerNote(Player player, int note) {
        player.setNote((long) note);
    }

    /**
     * Saves player properties and ensures persistence
     * @param player The player to save
     */
    public void savePlayerProperties(Player player) {
        if (player == null) {
            logger.error("Cannot save null player");
            return;
        }
        
        try {
            // Save to Redis without expecting boolean return
            redisService.savePlayer(player);
            logger.info("Saved player properties: " + player.getName());
            
            // Also update in session to ensure consistency
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null) {
                // Find and update player in session
                Set<Player> players = session.getPlayers();
                
                // Remove existing player with same ID and add updated one
                players.removeIf(p -> p.getId().equals(player.getId()));
                players.add(player);
                
                // Update session in Redis if needed
                redisService.saveSession(session);
            }
        } catch (Exception e) {
            logger.error("Error saving player properties: " + e.getMessage());
        }
    }

    // Rule management methods
    public void clearRules(Player player) {
        player.getRules().clear();
        commandBus.publish(Commands.RULES_CLEARED, this, player);
    }

    public void deleteRule(Rule rule) {
        if (rule == null || rule.getId() == null) {
            logger.error("Cannot delete null rule");
            return;
        }
        
        Player player = rule.getPlayer();
        if (player == null) {
            logger.error("Rule {} has no associated player", rule.getId());
            return;
        }
        
        // Delete from Redis first without expecting boolean return
        redisService.deleteRule(rule.getId());
        logger.info("Attempted to delete rule from Redis: {}", rule.getId());
        
        // Remove from player
        if (player.getRules() != null) {
            boolean removed = player.getRules().remove(rule);
            logger.info("Rule {} removed from player's collection: {}", rule.getId(), removed);
        }
        
        // Save updated player
        savePlayerWithRules(player);
        
        // Notify listeners
        commandBus.publish(Commands.RULE_DELETED, this, player);
        commandBus.publish(Commands.PLAYER_UPDATED, this, player);
        commandBus.publish(Commands.PLAYER_SELECTED, this, player);
    }

    public void updatePlayer(Player player) {
        commandBus.publish(Commands.PLAYER_UPDATED, this, player);
    }

    public void addPlayer(Player player) {
        commandBus.publish(Commands.PLAYER_ADDED, this, player);
    }

    public void removeAllPlayers(Session session) {
        logger.info("Removing all players from session: {}", session.getId());

        Set<Player> players = session.getPlayers();

        session.setPlayers(new HashSet<>());
        redisService.saveSession(session);

        for (Player player : players)
            redisService.deletePlayer(player);
    }

    public void setActivePlayer(Player player) {
        if (!Objects.equals(this.activePlayer, player)) {
            logger.info("PlayerManager.setActivePlayer: " + 
                      (player != null ? player.getName() + " (ID: " + player.getId() + ")" : "null"));
            this.activePlayer = player;
        }
    }

    /**
     * Sends a MIDI note to the active player without triggering heavy updates
     * @param midiNote The MIDI note to send
     * @return true if note was successfully sent, false otherwise
     */
    public boolean sendNoteToActivePlayer(int midiNote) {
        if (activePlayer == null) {
            logger.debug("No active player to receive MIDI note: {}", midiNote);
            return false;
        }
        
        try {
            // Use the player's instrument, channel, and a reasonable velocity
            Instrument instrument = activePlayer.getInstrument();
            if (instrument == null) {
                logger.debug("Active player has no instrument");
                return false;
            }
            
            int channel = activePlayer.getChannel();
            
            // CRITICAL FIX: Send program change before playing the note
            if (activePlayer.getPreset() != null) {
                try {
                    logger.debug("Sending program change: channel={}, preset={}", 
                               channel, activePlayer.getPreset());
                    instrument.programChange(channel, activePlayer.getPreset(), 0);
                } catch (Exception e) {
                    logger.warn("Failed to send program change: {}", e.getMessage());
                    // Continue anyway to play the note
                }
            }
            
            // Calculate velocity from player settings
            int velocity = (int) Math.round((activePlayer.getMinVelocity() + activePlayer.getMaxVelocity()) / 2.0);
            
            // Just update the note in memory temporarily - don't save to Redis
            activePlayer.setNote((long) midiNote);
            
            // Send the note to the device
            logger.debug("Sending note: note={}, channel={}, velocity={}", midiNote, channel, velocity);
            instrument.noteOn(channel, midiNote, velocity);
            
            // Schedule note-off after a reasonable duration
            long duration = 250; // milliseconds
            new java.util.Timer(true).schedule( // Use daemon timer
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        try {
                            instrument.noteOff(channel, midiNote, 0);
                        } catch (Exception e) {
                            // Just log at debug level - not a critical error
                            logger.debug("Error sending note-off: {}", e.getMessage());
                        }
                    }
                },
                duration
            );
            
            return true;
            
        } catch (Exception e) {
            logger.warn("Error sending MIDI note: {}", e.getMessage());
            return false;
        }
    }

    private void schedulePlayerSave(Player player) {
        // Cancel any pending save
        if (saveTimer != null) {
            saveTimer.cancel();
        }
        
        // Create new timer
        saveTimer = new java.util.Timer(true);
        
        // Schedule save after a short delay (500ms)
        saveTimer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                try {
                    // Save to Redis
                    redisService.savePlayer(player);
                    logger.debug("Saved player {} after preset change", player.getName());
                } catch (Exception e) {
                    logger.warn("Error saving player after preset change: {}", e.getMessage());
                }
            }
        }, 500);
    }

    // Add a utility method to help debug rule-player associations
    public void debugPlayerRules(Player player) {
        logger.info("=== DEBUG PLAYER RULES ===");
        logger.info("Player: " + player.getName() + " (ID: " + player.getId() + ")");
        
        if (player.getRules() == null) {
            logger.info("Rules collection is NULL");
            return;
        }
        
        logger.info("Rules count: " + player.getRules().size());
        
        for (Rule rule : player.getRules()) {
            logger.info("Rule ID: " + rule.getId() + 
                      " - Player ID: " + (rule.getPlayer() != null ? rule.getPlayer().getId() : "NULL") +
                      " - " + rule.getOperatorText() + " " + 
                      rule.getComparisonText() + " " + rule.getValue());
        }
        logger.info("=========================");
    }
}