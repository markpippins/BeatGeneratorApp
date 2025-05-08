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
                case Commands.PLAYER_ACTIVATION_REQUEST -> handleActivatePlayerRequest(action);
                case Commands.PLAYER_UPDATE_REQUEST -> handleUpdatePlayerRequest(action);
                case Commands.PLAYER_PRESET_CHANGE_REQUEST -> handlePresetChangeRequest(action);
                case Commands.PLAYER_INSTRUMENT_CHANGE_REQUEST -> handleInstrumentChangeRequest(action);
                case Commands.REFRESH_ALL_INSTRUMENTS -> handleApplyPresetRequest(action);
            }
        }
        catch (Exception e){
            logger.error("Error processing player action: {}", e.getMessage(), e);
        }
    }

    private void handleApplyPresetRequest(Command action) {
        if (action.getData() instanceof Player player) {
            if (player != null && player.getInstrument() != null) {
                PlayerManager.getInstance().applyInstrumentPreset(player);
            }
        }
    }

    /**
     * Handle request to activate a player
     */
    private void handleActivatePlayerRequest(Command action) {
        if (action.getData() instanceof Player player) {
            setActivePlayer(player);
            commandBus.publish(Commands.PLAYER_ACTIVATED, this, player);
        } else if (action.getData() instanceof Long playerId) {
            Player player = getPlayerById(playerId);
            if (player != null) {
                setActivePlayer(player);
                // Broadcast successful update
                commandBus.publish(Commands.PLAYER_ACTIVATED, this, player);
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
                player.getInstrument().setPreset(presetNumber);

                savePlayerProperties(player);

                // Apply MIDI changes if needed
                applyPlayerPreset(player);

                // Broadcast successful update
                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                commandBus.publish(Commands.PLAYER_PRESET_CHANGED, this,
                        new Object[] { playerId, presetNumber });
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
                // Store previous instrument ID for logging
                Long previousInstrumentId = player.getInstrumentId();

                // Check if this is a drum player (channel 9) and part of a DrumSequencer
                boolean isDrumPlayer = player.getChannel() == 9;
                boolean isPartOfSequencer = false;
                DrumSequencer owningSequencer = null;
                int playerIndexInSequencer = -1;

                // Find if this player belongs to a DrumSequencer
                if (isDrumPlayer && player.getOwner() instanceof DrumSequencer) {
                    owningSequencer = (DrumSequencer) player.getOwner();
                    isPartOfSequencer = true;

                    // Find the index of this player in the sequencer
                    for (int i = 0; i < owningSequencer.getPlayers().length; i++) {
                        if (player.equals(owningSequencer.getPlayers()[i])) {
                            playerIndexInSequencer = i;
                            break;
                        }
                    }
                }

                // Update player's instrument
                player.setInstrument(instrument);
                player.setInstrumentId(instrument.getId());

                // Mark instrument as assigned
                instrument.setAssignedToPlayer(true);
                instrument.setChannel(player.getChannel());

                // If previous instrument exists, mark it as unassigned
                if (previousInstrumentId != null && !previousInstrumentId.equals(instrument.getId())) {
                    InstrumentWrapper previousInstrument = InstrumentManager.getInstance()
                            .getInstrumentById(previousInstrumentId);
                    if (previousInstrument != null) {
                        previousInstrument.setAssignedToPlayer(false);
                    }
                }

                // Save and apply the change
                savePlayerProperties(player);
                applyPlayerInstrument(player);

                // If this is a drum player in a sequencer, ask if user wants to apply to all
                // drum pads
                if (isPartOfSequencer && owningSequencer != null) {
                    // This should be handled by the UI component that initiated the change
                    commandBus.publish(Commands.DRUM_PLAYER_INSTRUMENT_CHANGED, this,
                            new Object[] { owningSequencer, playerIndexInSequencer, instrument });
                }

                // If this is the active player, update the reference
                if (activePlayer != null && activePlayer.getId().equals(player.getId())) {
                    activePlayer = player;
                }

                // Broadcast successful update
                commandBus.publish(Commands.PLAYER_UPDATED, this, player);
                commandBus.publish(Commands.PLAYER_INSTRUMENT_CHANGED, this,
                        new Object[] { playerId, instrument.getId() });

                logger.info("Changed instrument for player {} from {} to {}",
                        player.getName(),
                        previousInstrumentId,
                        instrument.getId());
            }
        }
    }

    /**
     * Set the currently active player and notify the system
     */
    public void setActivePlayer(Player player) {
        if (player == null)
            return;
        activePlayer = player;
    }

    /**
     * Get a player by ID with guaranteed fresh data
     */
    public Player getPlayerById(Long id) {
        if (id == null)
            return null;

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
     * This is the primary method applications should use to save player data
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
                RedisService.getInstance().saveInstrument(player.getInstrument());

                // Ensure the reference is maintained
                player.setInstrumentId(player.getInstrument().getId());

                logger.debug("Saved instrument: {} (ID: {}) with preset {}",
                        player.getInstrument().getName(),
                        player.getInstrument().getId(),
                        player.getInstrument().getPreset());
            }

            // Save player using RedisService (which handles session references)
            // This internally calls playerHelper.savePlayer() for persistence
            // RedisService.getInstance().savePlayer(player);

            // Update cache AFTER successful save to ensure consistency
            playerCache.put(player.getId(), player);

            // Update active player reference if needed
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

    public void initializeInternalInstrument(Player player, boolean exclusive) {
        if (player == null) {
            logger.warn("Cannot initialize internal instrument for null player");
            return;
        }

        Player currentPlayer = getActivePlayer();

        try {
            // Get an internal instrument from InstrumentManager
            InstrumentWrapper internalInstrument = InstrumentManager.getInstance()
                    .getOrCreateInternalSynthInstrument(player.getChannel(), exclusive);

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

        setActivePlayer(currentPlayer);
    }

    /**
     * Initialize the instrument for this sequencer
     * Called by PlayerManager during setup
     */
    public void initializeInstrument(Player player, boolean exclusive) {
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
                        .getOrCreateInternalSynthInstrument(channel, exclusive));

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

    public void applyInstrumentPreset(Player player) {
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
}