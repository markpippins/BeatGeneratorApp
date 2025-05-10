package com.angrysurfer.core.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.event.PlayerInstrumentChangeEvent;
import com.angrysurfer.core.event.PlayerPresetChangeEvent;
import com.angrysurfer.core.event.PlayerRefreshEvent;
import com.angrysurfer.core.event.PlayerSelectionEvent;
import com.angrysurfer.core.event.PlayerUpdateEvent;
import com.angrysurfer.core.event.PlayerRuleUpdateEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;

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
                case Commands.PLAYER_SELECTION_EVENT -> handlePlayerSelectionEvent(action);
                case Commands.PLAYER_UPDATE_EVENT -> handlePlayerUpdateEvent(action);
                case Commands.PLAYER_PRESET_CHANGE_EVENT -> handlePlayerPresetChangeEvent(action);
                case Commands.PLAYER_INSTRUMENT_CHANGE_EVENT -> handlePlayerInstrumentChangeEvent(action);
                case Commands.PLAYER_REFRESH_EVENT -> handlePlayerRefreshEvent(action);

                case Commands.PLAYER_ACTIVATION_REQUEST -> handleLegacyPlayerActivationRequest(action);
                case Commands.PLAYER_UPDATE_REQUEST -> handleLegacyPlayerUpdateRequest(action);
                case Commands.PLAYER_PRESET_CHANGE_REQUEST -> handleLegacyPresetChangeRequest(action);
                case Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST -> handleLegacyInstrumentChangeRequest(action);
                case Commands.REFRESH_ALL_INSTRUMENTS -> handleLegacyRefreshRequest(action);
            }
        } catch (Exception e) {
            logger.error("Error processing player action: {}", e.getMessage(), e);
        }
    }

    private void handlePlayerSelectionEvent(Command action) {
        if (action.getData() instanceof PlayerSelectionEvent event && event.getPlayer() != null) {
            logger.info("Player selected for UI: {} (ID: {})",
                    event.getPlayer().getName(), event.getPlayerId());
            commandBus.publish(Commands.PLAYER_ACTIVATED, this, event.getPlayer());
        }
    }

    private void handlePlayerUpdateEvent(Command action) {
        if (action.getData() instanceof PlayerUpdateEvent event && event.getPlayer() != null) {
            Player player = event.getPlayer();
            savePlayerProperties(player);
            commandBus.publish(Commands.PLAYER_UPDATED, this, player);
        }
    }

    private void handlePlayerPresetChangeEvent(Command action) {
        if (action.getData() instanceof PlayerPresetChangeEvent event &&
                event.getPlayer() != null &&
                event.getPlayer().getInstrument() != null) {

            Player player = event.getPlayer();
            Integer bankIndex = event.getBankIndex();
            Integer presetNumber = event.getPresetNumber();

            if (bankIndex != null) {
                player.getInstrument().setBankIndex(bankIndex);
            }

            if (presetNumber != null) {
                player.getInstrument().setPreset(presetNumber);
            }

            savePlayerProperties(player);
            applyInstrumentPreset(player);

            commandBus.publish(Commands.PLAYER_UPDATED, this, player);
            commandBus.publish(Commands.PLAYER_PRESET_CHANGED, this,
                    new Object[]{player.getId(), presetNumber});
        }
    }

    private void handlePlayerInstrumentChangeEvent(Command action) {
        if (action.getData() instanceof PlayerInstrumentChangeEvent event &&
                event.getPlayer() != null &&
                event.getInstrument() != null) {

            Player player = event.getPlayer();
            InstrumentWrapper instrument = event.getInstrument();

            Long previousInstrumentId = player.getInstrumentId();

            boolean isDrumPlayer = player.getChannel() == 9;
            boolean isPartOfSequencer = false;
            DrumSequencer owningSequencer = null;
            int playerIndexInSequencer = -1;

            if (isDrumPlayer && player.getOwner() instanceof DrumSequencer) {
                owningSequencer = (DrumSequencer) player.getOwner();
                isPartOfSequencer = true;

                for (int i = 0; i < owningSequencer.getPlayers().length; i++) {
                    if (player.equals(owningSequencer.getPlayers()[i])) {
                        playerIndexInSequencer = i;
                        break;
                    }
                }
            }

            player.setInstrument(instrument);
            player.setInstrumentId(instrument.getId());

            instrument.setAssignedToPlayer(true);
            instrument.setChannel(player.getChannel());

            if (previousInstrumentId != null && !previousInstrumentId.equals(instrument.getId())) {
                InstrumentWrapper previousInstrument = InstrumentManager.getInstance()
                        .getInstrumentById(previousInstrumentId);
                if (previousInstrument != null) {
                    previousInstrument.setAssignedToPlayer(false);
                }
            }

            savePlayerProperties(player);
            applyInstrumentPreset(player);

            if (isPartOfSequencer && owningSequencer != null) {
                commandBus.publish(Commands.DRUM_PLAYER_INSTRUMENT_CHANGED, this,
                        new Object[]{owningSequencer, playerIndexInSequencer, instrument});
            }

            commandBus.publish(Commands.PLAYER_UPDATED, this, player);
            commandBus.publish(Commands.PLAYER_INSTRUMENT_CHANGED, this,
                    new Object[]{player.getId(), instrument.getId()});

            logger.info("Changed instrument for player {} from {} to {}",
                    player.getName(),
                    previousInstrumentId,
                    instrument.getId());
        }
    }

    private void handlePlayerRefreshEvent(Command action) {
        if (action.getData() instanceof PlayerRefreshEvent event && event.getPlayer() != null) {
            Player player = event.getPlayer();

            if (player.getInstrument() != null) {
                logger.info("Refreshing preset for player: {} (ID: {})",
                        player.getName(), player.getId());

                applyInstrumentPreset(player);
            }
        }
    }

    @Deprecated
    private void handleLegacyPlayerActivationRequest(Command action) {
        if (action.getData() instanceof Player player) {
            PlayerSelectionEvent event = new PlayerSelectionEvent(player);
            commandBus.publish(Commands.PLAYER_SELECTION_EVENT, this, event);
        } else if (action.getData() instanceof Long playerId) {
            Player player = getPlayerById(playerId);
            if (player != null) {
                PlayerSelectionEvent event = new PlayerSelectionEvent(player);
                commandBus.publish(Commands.PLAYER_SELECTION_EVENT, this, event);
            }
        }
    }

    @Deprecated
    private void handleLegacyPlayerUpdateRequest(Command action) {
        if (action.getData() instanceof Player player) {
            PlayerUpdateEvent event = new PlayerUpdateEvent(player);
            commandBus.publish(Commands.PLAYER_UPDATE_EVENT, this, event);
        }
    }

    @Deprecated
    private void handleLegacyPresetChangeRequest(Command action) {
        if (action.getData() instanceof Object[] data && data.length >= 2) {
            Long playerId = (Long) data[0];
            Integer presetNumber = (Integer) data[1];
            Integer bankIndex = null;

            if (data.length >= 3 && data[2] instanceof Integer) {
                bankIndex = (Integer) data[2];
            }

            Player player = getPlayerById(playerId);
            if (player != null) {
                PlayerPresetChangeEvent event = new PlayerPresetChangeEvent(player, bankIndex, presetNumber);
                commandBus.publish(Commands.PLAYER_PRESET_CHANGE_EVENT, this, event);
            }
        }
    }

    @Deprecated
    private void handleLegacyInstrumentChangeRequest(Command action) {
        if (action.getData() instanceof Object[] data && data.length >= 2) {
            Long playerId = (Long) data[0];
            InstrumentWrapper instrument = (InstrumentWrapper) data[1];

            Player player = getPlayerById(playerId);
            if (player != null && instrument != null) {
                PlayerInstrumentChangeEvent event = new PlayerInstrumentChangeEvent(player, instrument);
                commandBus.publish(Commands.PLAYER_INSTRUMENT_CHANGE_EVENT, this, event);
            }
        }
    }

    @Deprecated
    private void handleLegacyRefreshRequest(Command action) {
        if (action.getData() instanceof Player player) {
            PlayerRefreshEvent event = new PlayerRefreshEvent(player);
            commandBus.publish(Commands.PLAYER_REFRESH_EVENT, this, event);
        }
    }

    public Player getPlayerById(Long id) {
        if (id == null)
            return null;

        Player player = playerCache.get(id);

        if (player == null) {
            player = redisService.findPlayerById(id);
            if (player != null) {
                playerCache.put(id, player);
            }
        }

        return player;
    }

    public void savePlayerProperties(Player player) {
        if (player == null) {
            logger.warn("Cannot save null player");
            return;
        }

        try {
            logger.info("Saving player: {} (ID: {})", player.getName(), player.getId());

            if (player.getInstrument() != null) {
                RedisService.getInstance().saveInstrument(player.getInstrument());
                player.setInstrumentId(player.getInstrument().getId());

                logger.debug("Saved instrument: {} (ID: {}) with preset {}",
                        player.getInstrument().getName(),
                        player.getInstrument().getId(),
                        player.getInstrument().getPreset());
            }

            playerCache.put(player.getId(), player);

            logger.debug("Player saved successfully");
        } catch (Exception e) {
            logger.error("Error saving player: {}", e.getMessage(), e);
        }
    }

    public void applyInstrumentPreset(Player player) {
        if (player == null || player.getInstrument() == null) {
            return;
        }

        try {
            InstrumentWrapper instrument = player.getInstrument();

            boolean isInternalSynth = InternalSynthManager.getInstance().isInternalSynthInstrument(instrument);
            boolean deviceOpen = false;

            if (instrument.getDevice() != null) {
                if (!instrument.getDevice().isOpen()) {
                    try {
                        instrument.getDevice().open();
                    } catch (Exception e) {
                        logger.warn("Could not open device: {}", e.getMessage());
                    }
                }
                deviceOpen = instrument.getDevice().isOpen();
            }

            if (instrument.getReceiver() == null && instrument.getDevice() != null && instrument.getDevice().isOpen()) {
                try {
                    instrument.setReceiver(instrument.getDevice().getReceiver());
                } catch (Exception e) {
                    logger.warn("Could not get receiver: {}", e.getMessage());
                }
            }

            logger.info(
                    "Applying preset for {} on channel {}: bank={}, program={}, device={}, isInternal={}, deviceOpen={}",
                    player.getName(), player.getChannel(),
                    instrument.getBankIndex(), instrument.getPreset(),
                    instrument.getDeviceName(), isInternalSynth, deviceOpen);

            if (isInternalSynth) {
                InternalSynthManager.getInstance().updateInstrumentPreset(
                        instrument,
                        instrument.getBankIndex(),
                        instrument.getPreset());

                try {
                    javax.sound.midi.Synthesizer synth = InternalSynthManager.getInstance().getSynthesizer();
                    if (synth != null && synth.isOpen()) {
                        int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
                        int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
                        int channel = player.getChannel();

                        javax.sound.midi.MidiChannel[] channels = synth.getChannels();
                        if (channels != null && channel < channels.length) {
                            channels[channel].controlChange(0, (bankIndex >> 7) & 0x7F);
                            channels[channel].controlChange(32, bankIndex & 0x7F);
                            channels[channel].programChange(preset);

                            logger.info("Directly applied program change to synth channel {}: bank={}, program={}",
                                    channel, bankIndex, preset);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not apply direct synth program change: {}", e.getMessage());
                }
            } else {
                int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
                int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
                int channel = player.getChannel();

                try {
                    instrument.controlChange(0, (bankIndex >> 7) & 0x7F);
                    instrument.controlChange(32, bankIndex & 0x7F);
                    instrument.programChange(preset, 0);

                    if (instrument.getReceiver() != null) {
                        javax.sound.midi.ShortMessage bankMSB = new javax.sound.midi.ShortMessage();
                        bankMSB.setMessage(0xB0 | channel, 0, (bankIndex >> 7) & 0x7F);
                        instrument.getReceiver().send(bankMSB, -1);

                        javax.sound.midi.ShortMessage bankLSB = new javax.sound.midi.ShortMessage();
                        bankLSB.setMessage(0xB0 | channel, 32, bankIndex & 0x7F);
                        instrument.getReceiver().send(bankLSB, -1);

                        javax.sound.midi.ShortMessage pc = new javax.sound.midi.ShortMessage();
                        pc.setMessage(0xC0 | channel, preset, 0);
                        instrument.getReceiver().send(pc, -1);

                        logger.info("Sent raw MIDI program change messages to channel {}", channel);
                    }
                } catch (Exception e) {
                    logger.warn("Error sending MIDI program change: {}", e.getMessage());
                }
            }

            logger.info("Applied preset for player: {}", player.getName());
        } catch (Exception e) {
            logger.error("Error applying player preset: {}", e.getMessage(), e);
        }
    }

    /**
     * Apply a player's instrument settings to MIDI system
     */
    public void applyPlayerInstrument(Player player) {
        if (player == null || player.getInstrument() == null) {
            return;
        }

        try {
            InstrumentWrapper instrument = player.getInstrument();

            // Check if this is an internal synth instrument
            if (InternalSynthManager.getInstance().isInternalSynthInstrument(instrument)) {
                // Use InternalSynthManager for internal instruments
                InternalSynthManager.getInstance().initializeInstrumentState(instrument);
            } else {
                // Use standard method for external instruments
                if (instrument.getBankIndex() != null && instrument.getPreset() != null) {
                    instrument.controlChange(0, (instrument.getBankIndex() >> 7) & 0x7F);
                    instrument.controlChange(32, instrument.getBankIndex() & 0x7F);
                    instrument.programChange(instrument.getPreset(), 0);
                }
            }

            logger.debug("Applied instrument settings for player {}", player.getName());
        } catch (Exception e) {
            logger.error("Error applying player instrument: {}", e.getMessage(), e);
        }
    }
    
    public Rule addRule(Player player, int beat, int equals, double v, int i) {
        return null;
    }

    public void removeRule(Player player, Long ruleId) {
    }

    public Player updatePlayer(Session session, Long playerId, int updateType, int updateValue) {
        return null;
    }

    public Set<Player> removePlayer(Session session, Long playerId) {
        return Collections.emptySet();
    }

    public void clearPlayers(Session session) {
    }

    public void clearPlayersWithNoRules(Session session) {
    }

    public void removeAllPlayers(Session session) {
    }

    /**
     * Apply a player's preset to MIDI system
     */
    public void applyPlayerPreset(Player player) {
        if (player == null || player.getInstrument() == null) {
            return;
        }

        try {
            InstrumentWrapper instrument = player.getInstrument();

            // Get device type and ensure it's open
            boolean isInternalSynth = InternalSynthManager.getInstance().isInternalSynthInstrument(instrument);
            boolean deviceOpen = false;

            if (instrument.getDevice() != null) {
                if (!instrument.getDevice().isOpen()) {
                    try {
                        instrument.getDevice().open();
                    } catch (Exception e) {
                        logger.warn("Could not open device: {}", e.getMessage());
                    }
                }
                deviceOpen = instrument.getDevice().isOpen();
            }

            // Ensure receiver is available
            if (instrument.getReceiver() == null && instrument.getDevice() != null && instrument.getDevice().isOpen()) {
                try {
                    instrument.setReceiver(instrument.getDevice().getReceiver());
                } catch (Exception e) {
                    logger.warn("Could not get receiver: {}", e.getMessage());
                }
            }

            // Debug info
            logger.info(
                    "Applying preset for {} on channel {}: bank={}, program={}, device={}, isInternal={}, deviceOpen={}",
                    player.getName(), player.getChannel(),
                    instrument.getBankIndex(), instrument.getPreset(),
                    instrument.getDeviceName(), isInternalSynth, deviceOpen);

            // For internal synth, use InternalSynthManager for best reliability
            if (isInternalSynth) {
                InternalSynthManager.getInstance().updateInstrumentPreset(
                        instrument,
                        instrument.getBankIndex(),
                        instrument.getPreset());

                // Double-check by directly accessing the Java Synthesizer
                try {
                    javax.sound.midi.Synthesizer synth = InternalSynthManager.getInstance().getSynthesizer();
                    if (synth != null && synth.isOpen()) {
                        int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
                        int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
                        int channel = player.getChannel();

                        // Get the MidiChannel for this instrument's channel
                        javax.sound.midi.MidiChannel[] channels = synth.getChannels();
                        if (channels != null && channel < channels.length) {
                            // Send bank select and program change directly to the MidiChannel
                            channels[channel].controlChange(0, (bankIndex >> 7) & 0x7F); // Bank MSB
                            channels[channel].controlChange(32, bankIndex & 0x7F); // Bank LSB
                            channels[channel].programChange(preset);

                            logger.info("Directly applied program change to synth channel {}: bank={}, program={}",
                                    channel, bankIndex, preset);
                        }
                    }
                } catch (Exception e) {
                    logger.warn("Could not apply direct synth program change: {}", e.getMessage());
                }
            } else {
                // Use standard method for external instruments
                int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
                int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;
                int channel = player.getChannel();

                try {
                    // Apply bank and program changes through the instrument
                    instrument.controlChange(0, (bankIndex >> 7) & 0x7F); // Bank MSB
                    instrument.controlChange(32, bankIndex & 0x7F); // Bank LSB
                    instrument.programChange(preset, 0);

                    // Also try alternate way with raw MIDI messages if available
                    if (instrument.getReceiver() != null) {
                        // Bank select MSB
                        javax.sound.midi.ShortMessage bankMSB = new javax.sound.midi.ShortMessage();
                        bankMSB.setMessage(0xB0 | channel, 0, (bankIndex >> 7) & 0x7F);
                        instrument.getReceiver().send(bankMSB, -1);

                        // Bank select LSB
                        javax.sound.midi.ShortMessage bankLSB = new javax.sound.midi.ShortMessage();
                        bankLSB.setMessage(0xB0 | channel, 32, bankIndex & 0x7F);
                        instrument.getReceiver().send(bankLSB, -1);

                        // Program change
                        javax.sound.midi.ShortMessage pc = new javax.sound.midi.ShortMessage();
                        pc.setMessage(0xC0 | channel, preset, 0);
                        instrument.getReceiver().send(pc, -1);

                        logger.info("Sent raw MIDI program change messages to channel {}", channel);
                    }
                } catch (Exception e) {
                    logger.warn("Error sending MIDI program change: {}", e.getMessage());
                }
            }

            logger.info("Applied preset for player: {}", player.getName());
        } catch (Exception e) {
            logger.error("Error applying player preset: {}", e.getMessage(), e);
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
                if (channel == 9)
                    continue;

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
                    if (channel == 9)
                        continue;

                    // If this player's channel has a conflict
                    if (channelConflicts.containsKey(channel)) {
                        // Only reassign if this isn't the first player that claimed the channel
                        if (!player.getId().equals(channelToPlayerId.get(channel))) {
                            // Assign a new channel
                            int newChannel = channelManager.getNextAvailableMelodicChannel();

                            logger.info("Reassigning player {} from channel {} to channel {}",
                                    player.getId(), channel, newChannel);

                            player.setDefaultChannel(newChannel);

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

                player.setDefaultChannel(newChannel);

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

    public void initializeInternalInstrument(Player player, boolean exclusive, int tag) {
        if (player == null) {
            logger.warn("Cannot initialize internal instrument for null player");
            return;
        }

        try {
            // Get an internal instrument from InstrumentManager
            InstrumentWrapper internalInstrument = InstrumentManager.getInstance()
                    .getOrCreateInternalSynthInstrument(player.getChannel(), exclusive, tag);

            if (internalInstrument != null) {
                // Assign to player
                player.setInstrument(internalInstrument);
                player.setInstrumentId(internalInstrument.getId());
                player.setUsingInternalSynth(true);

                // Save the player
                savePlayerProperties(player);

                // Initialize the instrument state
                InternalSynthManager.getInstance().initializeInstrumentState(internalInstrument);

                logger.info("Player {} initialized with internal instrument", player.getId());
            }
        } catch (Exception e) {
            logger.error("Failed to initialize internal instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Initialize the instrument for this sequencer
     * Called by PlayerManager during setup
     */
    public void initializeInstrument(Player player, boolean exclusive, int tag) {
        // If we don't have a player yet, exit
        if (player == null) {
            logger.warn("Cannot initialize instrument - no player assigned to sequencer");
            return;
        }

        try {
            // Get the instrument from the player
            if (player.getInstrument() == null) {
                // No instrument assigned, try to create a default one
                // First try to find an existing instrument for this channel
                int channel = player.getChannel();
                player.setInstrument(InstrumentManager.getInstance()
                        .getOrCreateInternalSynthInstrument(channel, exclusive, tag));

                logger.info("Created default instrument for sequencer on channel {}", channel);
            }

            // Ensure proper channel alignment
            if (player.getInstrument() != null) {
                player.getInstrument().setChannel(player.getChannel());

                // Apply the instrument preset
                applyInstrumentPreset(player);

                logger.info("Initialized instrument {} for player {} on channel {}",
                        player.getInstrument().getName(),
                        player.getName(),
                        player.getChannel());
            }
        } catch (Exception e) {
            logger.error("Error initializing instrument: {}", e.getMessage(), e);
        }
    }

    /**
     * Handle a rule update and broadcast an event
     * 
     * @param player The player whose rules are updated
     * @param rule The specific rule that was modified (can be null for bulk operations)
     * @param updateType The type of update that occurred
     */
    public void handleRuleUpdate(Player player, Rule rule, PlayerRuleUpdateEvent.RuleUpdateType updateType) {
        if (player == null) {
            logger.warn("Cannot handle rule update - player is null");
            return;
        }
        
        // Save the player state
        savePlayerRules(player);
        
        // Create and publish the rule update event
        PlayerRuleUpdateEvent event = new PlayerRuleUpdateEvent(player, rule, updateType);
        CommandBus.getInstance().publish(Commands.PLAYER_RULE_UPDATE_EVENT, this, event);
        
        // Also publish a player update event for backward compatibility
        CommandBus.getInstance().publish(
            Commands.PLAYER_UPDATE_EVENT,
            this,
            new PlayerUpdateEvent(player)
        );
    }

    /**
     * Save the rules for a player
     * 
     * @param player The player whose rules to save
     */
    public void savePlayerRules(Player player) {
        if (player == null) {
            logger.warn("Cannot save rules - player is null");
            return;
        }
        
        try {
            // Update the player in our cache
            playerCache.put(player.getId(), player);
            
            // Update the player in the session
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null) {
                session.addOrUpdatePlayer(player);
            }
            
            // Persist to storage
            redisService.savePlayer(player);
            
            logger.info("Saved rules for player {}", player.getId());
        } catch (Exception e) {
            logger.error("Error saving player rules: {}", e.getMessage(), e);
        }
    }
}