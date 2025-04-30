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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

/**
 * The central source of truth for all player-related operations.
 */
@Getter
@Setter
public class PlayerManager implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(PlayerManager.class);
    private static PlayerManager instance;
    private final Map<Long, Player> playerCache = new HashMap<>();
    private Player activePlayer;
    private final RedisService redisService;
    private final CommandBus commandBus;

    private PlayerManager() {
        this.redisService = RedisService.getInstance();
        this.commandBus = CommandBus.getInstance();
        registerForEvents();
    }

    public static synchronized PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    /**
     * Register for all player-related events
     */
    private void registerForEvents() {
        commandBus.register(this);
    }

    @Override
    public void onAction(Command action) {
        if (action == null || action.getCommand() == null) {
            return;
        }

        try {
            switch (action.getCommand()) {
                case Commands.PLAYER_SELECTED -> handleActivatePlayerRequest(action);
                case Commands.PLAYER_UPDATE_REQUEST -> handleUpdatePlayerRequest(action);
                case Commands.PLAYER_PRESET_CHANGE_REQUEST -> handlePresetChangeRequest(action);
                case Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST -> handleInstrumentChangeRequest(action);
                // Handle other player-related commands
            }
        } catch (Exception e) {
            logger.error("Error processing player action: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle request to activate a player
     */
    private void handleActivatePlayerRequest(Command action) {
        if (action.getData() instanceof Player player) {
            setActivePlayer(player);
        } else if (action.getData() instanceof Long playerId) {
            Player player = getPlayerById(playerId);
            if (player != null) {
                setActivePlayer(player);
            }
        }
    }

    /**
     * Handle request to update a player
     */
    private void handleUpdatePlayerRequest(Command action) {
        if (action.getData() instanceof Player player) {
            savePlayerProperties(player);
            
            // If this is the active player, keep that status
            if (activePlayer != null && activePlayer.getId().equals(player.getId())) {
                activePlayer = player;
            }
            
            // Broadcast successful update
            commandBus.publish(Commands.PLAYER_UPDATED, this, player);
        }
    }

    /**
     * Handle request to change a player's preset
     */
    private void handlePresetChangeRequest(Command action) {
        if (action.getData() instanceof Object[] data && data.length >= 2) {
            Long playerId = (Long) data[0];
            Integer presetNumber = (Integer) data[1];
            
            Player player = getPlayerById(playerId);
            if (player != null && player.getInstrument() != null) {
                player.setPreset(presetNumber);
                player.getInstrument().setCurrentPreset(presetNumber);
                
                savePlayerProperties(player);
                
                // Apply MIDI changes if needed
                applyPlayerPreset(player);
                
                // Broadcast successful update
                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                commandBus.publish(Commands.PLAYER_PRESET_CHANGED, this, 
                    new Object[]{playerId, presetNumber});
            }
        }
    }
    
    /**
     * Handle request to change a player's instrument
     */
    private void handleInstrumentChangeRequest(Command action) {
        if (action.getData() instanceof Object[] data && data.length >= 2) {
            Long playerId = (Long) data[0];
            InstrumentWrapper instrument = (InstrumentWrapper) data[1];
            
            Player player = getPlayerById(playerId);
            if (player != null && instrument != null) {
                player.setInstrument(instrument);
                player.setInstrumentId(instrument.getId());
                
                savePlayerProperties(player);
                
                // Apply MIDI changes if needed
                applyPlayerInstrument(player);
                
                // Broadcast successful update
                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                commandBus.publish(Commands.PLAYER_INSTRUMENT_CHANGED, this, 
                    new Object[]{playerId, instrument.getId()});
            }
        }
    }

    /**
     * Set the currently active player and notify the system
     */
    public void setActivePlayer(Player player) {
        if (player == null) return;
        
        // Always get a fresh copy from the cache/storage
        Player freshPlayer = getPlayerById(player.getId());
        if (freshPlayer == null) return;
        
        activePlayer = freshPlayer;
        logger.info("Active player set to: {} (ID: {})", activePlayer.getName(), activePlayer.getId());
        
        // Notify the system about active player change
        // commandBus.publish(Commands.PLAYER_SELECTED, this, activePlayer);
    }

    /**
     * Get a player by ID with guaranteed fresh data
     */
    public Player getPlayerById(Long id) {
        if (id == null) return null;
        
        // First try cache
        Player player = playerCache.get(id);
        
        // If not in cache or we want fresh data, load from Redis
        if (player == null) {
            player = redisService.findPlayerById(id);
            if (player != null) {
                // Update cache
                playerCache.put(id, player);
            }
        }
        
        return player;
    }

    /**
     * Save player properties to persistent storage with complete consistency
     */
    public void savePlayerProperties(Player player) {
        if (player == null) {
            logger.warn("Cannot save null player");
            return;
        }
        
        try {
            logger.info("Saving player: {} (ID: {})", player.getName(), player.getId());
            
            // Ensure instrument consistency
            if (player.getInstrument() != null) {
                // Save instrument first
                redisService.saveInstrument(player.getInstrument());
                
                // Ensure the reference is maintained
                player.setInstrumentId(player.getInstrument().getId());
                
                logger.debug("Saved instrument: {} (ID: {}) with preset {}",
                    player.getInstrument().getName(),
                    player.getInstrument().getId(),
                    player.getInstrument().getCurrentPreset());
            }
            
            // Save the player
            redisService.savePlayer(player);
            
            // Update cache
            playerCache.put(player.getId(), player);
            
            // If this is the active player, update the reference
            if (activePlayer != null && activePlayer.getId().equals(player.getId())) {
                activePlayer = player;
            }
            
            logger.debug("Player saved successfully");
        } catch (Exception e) {
            logger.error("Error saving player: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Apply a player's preset to MIDI system
     */
    private void applyPlayerPreset(Player player) {
        if (player == null || player.getInstrument() == null) return;
        
        try {
            InstrumentWrapper instrument = player.getInstrument();
            int channel = player.getChannel() != null ? player.getChannel() : 0;
            int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
            int presetIndex = instrument.getCurrentPreset() != null ? instrument.getCurrentPreset() : 0;
            
            // Apply bank and program changes
            int bankMSB = (bankIndex >> 7) & 0x7F;
            int bankLSB = bankIndex & 0x7F;
            instrument.controlChange(0, bankMSB);
            instrument.controlChange(32, bankLSB);
            instrument.programChange(presetIndex, 0);
            
            logger.debug("Applied MIDI changes for player: {} (preset: {})", 
                player.getName(), presetIndex);
                
            // Also update sequencer if player belongs to one
            if (player.getOwner() instanceof MelodicSequencer sequencer) {
                sequencer.initializeInstrument();
            }
        } catch (Exception e) {
            logger.error("Error applying player preset: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Apply a player's instrument settings to MIDI system
     */
    private void applyPlayerInstrument(Player player) {
        if (player == null || player.getInstrument() == null) return;
        
        try {
            // If player belongs to a sequencer, refresh its instrument
            if (player.getOwner() instanceof MelodicSequencer sequencer) {
                sequencer.initializeInstrument();
            }
            
            // Also apply the preset
            applyPlayerPreset(player);
            
            logger.debug("Applied instrument for player: {}", player.getName());
        } catch (Exception e) {
            logger.error("Error applying player instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Ensures that all players have consistent channel assignments
     * and resolves any potential channel conflicts
     */
    public void ensureChannelConsistency() {
        logger.info("Ensuring channel consistency across all players");
        
        // Reset the channel manager's state
        ChannelManager channelManager = ChannelManager.getInstance();
        
        // First pass: collect all channel assignments
        Map<Integer, Long> channelToPlayerId = new HashMap<>();
        Map<Integer, Integer> channelConflicts = new HashMap<>();
        
        // Check for conflicts
        for (Player player : playerCache.values()) {
            if (player != null && player.getChannel() != null) {
                Integer channel = player.getChannel();
                
                // Skip drum channel 9 which can have multiple assignments
                if (channel == 9) continue;
                
                if (channelToPlayerId.containsKey(channel)) {
                    // Conflict detected - track it
                    channelConflicts.put(channel, channelConflicts.getOrDefault(channel, 1) + 1);
                    logger.warn("Channel conflict detected for channel {}: players {} and {}", 
                        channel, channelToPlayerId.get(channel), player.getId());
                } else {
                    // First player using this channel
                    channelToPlayerId.put(channel, player.getId());
                    
                    // Reserve this channel
                    channelManager.reserveChannel(channel);
                }
            }
        }
        
        // Second pass: resolve conflicts if any
        if (!channelConflicts.isEmpty()) {
            logger.info("Resolving {} channel conflicts", channelConflicts.size());
            
            for (Player player : playerCache.values()) {
                if (player != null && player.getChannel() != null) {
                    Integer channel = player.getChannel();
                    
                    // Skip drum channel
                    if (channel == 9) continue;
                    
                    // If this player's channel has a conflict
                    if (channelConflicts.containsKey(channel)) {
                        // Only reassign if this isn't the first player that claimed the channel
                        if (!player.getId().equals(channelToPlayerId.get(channel))) {
                            // Assign a new channel
                            int newChannel = channelManager.getNextAvailableMelodicChannel();
                            
                            logger.info("Reassigning player {} from channel {} to channel {}", 
                                player.getId(), channel, newChannel);
                            
                            player.setChannel(newChannel);
                            
                            // Save the updated player
                            savePlayerProperties(player);
                            
                            // Update any associated instruments
                            if (player.getInstrument() != null) {
                                applyPlayerInstrument(player);
                            }
                        }
                    }
                }
            }
        }
        
        // Final pass: ensure all players have valid channels
        for (Player player : playerCache.values()) {
            if (player != null && (player.getChannel() == null || 
                                  (!player.isDrumPlayer() && player.getChannel() == 9))) {
                // Assign an appropriate channel
                int newChannel = player.isDrumPlayer() ? 9 : channelManager.getNextAvailableMelodicChannel();
                
                logger.info("Assigning channel {} to player {} with missing/invalid channel", 
                    newChannel, player.getId());
                
                player.setChannel(newChannel);
                
                // Save the updated player
                savePlayerProperties(player);
                
                // Update any associated instruments
                if (player.getInstrument() != null) {
                    applyPlayerInstrument(player);
                }
            }
        }
        
        logger.info("Channel consistency check completed");
    }
}