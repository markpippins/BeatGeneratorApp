package com.angrysurfer.core.service;

import com.angrysurfer.core.Constants;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.midi.MIDIConstants;
import com.angrysurfer.core.config.UserConfig;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.redis.UserConfigHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
// Make UserConfigManager the single source of truth
public class UserConfigManager {

    private static final Logger logger = LoggerFactory.getLogger(UserConfigManager.class.getName());
    private static UserConfigManager instance;
    private final UserConfigHelper configHelper;
    private final CommandBus commandBus = CommandBus.getInstance();
    private boolean initialized = false;
    private boolean isInitializing;
    private UserConfig currentConfig = new UserConfig();

    // Make constructor private for singleton pattern
    private UserConfigManager() {
        isInitializing = true;
        this.configHelper = RedisService.getInstance().getUserConfigHelper();
    }

    // Static method to get singleton instance
    public static synchronized UserConfigManager getInstance() {
        if (instance == null) {
            instance = new UserConfigManager();
        }
        return instance;
    }

    private static InstrumentWrapper createMelodicInstrument(int i, int[] melodicChannels, InternalSynthManager synthManager) throws MidiUnavailableException {
        InstrumentWrapper meloInst = new InstrumentWrapper();

        // Basic properties
        String name = "Default Melo " + (i + 1);
        meloInst.setName(name);
        meloInst.setId(RedisService.getInstance().getPlayerHelper().getNextPlayerId());
        meloInst.setChannel(melodicChannels[i % melodicChannels.length]);
        meloInst.setInternal(true);
        meloInst.setInternalSynth(true);
        meloInst.setIsDefault(true);

        // Set up with InternalSynthManager - get device info and receiver
        meloInst.setDeviceInfo(synthManager.getSynthesizer().getDeviceInfo());
        meloInst.setDeviceName("Gervill");
        meloInst.setReceiver(synthManager.getSynthesizer().getReceiver());

        // Set melodic-specific properties
        meloInst.setBankIndex(0); // Standard melodic bank
        meloInst.setPreset((i * 10) % 127); // Distribute across GM sounds with 10-instrument gaps

        // Set appropriate note range for melodic
        meloInst.setLowestNote(0);
        meloInst.setHighestNote(127);

        // Additional properties
        meloInst.setDescription("Default melodic instrument " + (i + 1));
        meloInst.setAvailable(true);
        return meloInst;
    }

    private static InstrumentWrapper createDrumInstrument(int i, InternalSynthManager synthManager) throws MidiUnavailableException {
        InstrumentWrapper drumInst = new InstrumentWrapper();

        // Basic properties
        String name = "Default Drum " + i;
        drumInst.setName(name);
        drumInst.setId(RedisService.getInstance().getInstrumentHelper().getNextInstrumentId());
        drumInst.setChannel(9);  // MIDI channel 10 (indexed from 0)
        drumInst.setInternal(true);
        drumInst.setInternalSynth(true);
        drumInst.setIsDefault(true);

        // Set up with InternalSynthManager - get device info and receiver
        drumInst.setDeviceInfo(synthManager.getSynthesizer().getDeviceInfo());
        drumInst.setDeviceName("Gervill");
        drumInst.setReceiver(synthManager.getSynthesizer().getReceiver());

        // Set drum-specific properties
        // drumInst.setBankIndex(128); // Standard drum bank
        // drumInst.setPreset(i % 5); // Cycle through drum kits (0-4)

        // Set appropriate note range for drums
        drumInst.setLowestNote(35);
        drumInst.setHighestNote(50);

        // Additional properties
        drumInst.setDescription("Default drum instrument " + i);
        drumInst.setAvailable(true);
        return drumInst;
    }

    private static Strike createStrike(String drumName, int rootNote, InstrumentWrapper instrument) {
        Strike player = new Strike();
        player.setId(RedisService.getInstance().getNextPlayerId());
        player.setName(drumName);
        player.setIsDefault(true);
        player.setDrumPlayer(true);
        player.setRootNote(rootNote);

        // Set channel to 9 (MIDI channel 10 for drums)
        player.setDefaultChannel(9);

        // Associate with instrument
        player.setInstrument(instrument);

        // Configure velocities for natural sound
        player.setMinVelocity(70);
        player.setMaxVelocity(100);

        // Configure sequence properties
        player.setProbability(100);
        player.setLevel(90);
        player.setRatchetCount(0);
        player.setRatchetInterval(40);
        player.setSparse(0);
        player.setRandomDegree(0);
        return player;
    }

    public void initialize() {
        try {
            loadConfiguration();
        } finally {
            isInitializing = false;
        }

        // Now that initialization is complete, we can safely notify listeners
        commandBus.publish(Commands.USER_CONFIG_LOADED, this, null);
        initializeInstruments();
    }

    // Update the existing loadConfiguration method to handle IDs
    public void loadConfiguration() {
        logger.info("Loading user configuration from Redis");
        try {
            // Try to get the first available config ID
            Integer firstId = configHelper.findFirstConfigId();

            if (firstId != null) {
                UserConfig config = configHelper.loadConfigFromRedis(firstId);
                if (config != null) {
                    this.currentConfig = config;
                    initialized = true;

                    logger.info("Loaded user configuration with ID: {}", config.getId());

                    // Ensure defaults exist in the loaded configuration
                    ensureDefaultsExist();

                    if (!isInitializing) {
                        commandBus.publish(Commands.USER_CONFIG_LOADED, this, this.currentConfig);
                    }
                    return;
                }
            }

            // If we get here, no configuration was found
            logger.info("No user configuration found, creating default");
            this.currentConfig = new UserConfig();
            this.currentConfig.setId(1);
            this.currentConfig.setName("Default Configuration");
            this.currentConfig.setLastUpdated(new Date());
            configHelper.saveConfig(this.currentConfig);

            initialized = true;
            logger.info("Created new default configuration with ID: 1");

            // Ensure defaults exist in the new configuration
            ensureDefaultsExist();

        } catch (Exception e) {
            logger.error("Error loading user configuration: {}", e.getMessage());
            // Clean up resources
            // Notify UI of failure
        }
    }

    public void loadConfigurationFromFile(String configPath) {
        logger.info("Loading configuration from file: {}", configPath);
        try {
            UserConfig loadedConfig = configHelper.loadConfigFromJSON(configPath);
            if (loadedConfig != null) {
                currentConfig = loadedConfig;
                configHelper.saveConfig(currentConfig);
                commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
                logger.info("Configuration loaded and saved successfully");
            }
        } catch (Exception e) {
            logger.error("Error loading configuration from file: {}", e.getMessage());
            // Clean up resources
            // Notify UI of failure
        }
    }

    /**
     * Save configuration with enhanced error handling and verification
     */
    public boolean saveConfiguration(UserConfig config) {
        if (config == null) {
            logger.error("Cannot save null configuration");
            return false;
        }

        try {
            // Update timestamp
            config.setLastUpdated(new Date());

            // Ensure IDs on all elements are set
            ensureIds(config);

            // Save to Redis via helper
            configHelper.saveConfig(config);

            // Update our current reference
            currentConfig = config;

            // Verify save worked by reading back
            UserConfig verifyConfig = configHelper.loadConfigFromRedis(config.getId());
            boolean verified = verifyConfig != null &&
                    verifyConfig.getLastUpdated() != null &&
                    verifyConfig.getLastUpdated().equals(config.getLastUpdated());

            if (!verified) {
                logger.error("Failed to verify UserConfig was properly saved to Redis");
                return false;
            }

            // Notify listeners
            commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);

            logger.info("Successfully saved UserConfig {} with timestamp {}",
                    config.getId(), config.getLastUpdated());
            return true;
        } catch (Exception e) {
            logger.error("Error saving user configuration: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Ensure all elements have IDs set
     */
    private void ensureIds(UserConfig config) {
        // Ensure config ID
        if (config.getId() == null) {
            config.setId(1); // Default to ID 1
        }

        // Ensure all default players have IDs
        if (config.getDefaultNotes() != null) {
            for (Note note : config.getDefaultNotes()) {
                if (note.getId() == null) {
                    note.setId(RedisService.getInstance().getNextPlayerId());
                }
            }
        }

        if (config.getDefaultStrikes() != null) {
            for (Strike strike : config.getDefaultStrikes()) {
                if (strike.getId() == null) {
                    strike.setId(RedisService.getInstance().getNextPlayerId());
                }
            }
        }

        // Ensure all instruments have IDs
        if (config.getInstruments() != null) {
            for (InstrumentWrapper instrument : config.getInstruments()) {
                if (instrument.getId() == null) {
                    instrument.setId(System.currentTimeMillis());
                }
            }
        }
    }

    public List<InstrumentWrapper> getInstruments() {
        return currentConfig.getInstruments();
    }

    private void initializeInstruments() {
        if (this.currentConfig.getInstruments() == null || getCurrentConfig().getInstruments().isEmpty()) {
            // Load instruments from Redis if config doesn't have them
            try {
                List<InstrumentWrapper> instruments = RedisService.getInstance().findAllInstruments();
                if (instruments != null && !instruments.isEmpty()) {
                    getCurrentConfig().setInstruments(instruments);
                    saveConfiguration(getCurrentConfig());
                    logger.info("Loaded {} instruments from Redis", instruments.size());
                }
            } catch (Exception e) {
                logger.error("Error loading instruments: {}", e.getMessage());
                // Clean up resources
                // Notify UI of failure
            }
        }

        // Make sure Gervill is registered
        try {
            boolean hasGervill = false;

            // Check if Gervill is already in the config
            if (currentConfig.getInstruments() != null) {
                for (InstrumentWrapper instrument : currentConfig.getInstruments()) {
                    if (instrument.getDeviceName() != null &&
                            instrument.getDeviceName().contains("Gervill")) {
                        hasGervill = true;
                        break;
                    }
                }
            }

            // If Gervill is not in config, add it
            if (!hasGervill) {
                logger.info("Adding Gervill instrument to configuration");

                // Get Gervill from InternalSynthManager
                Synthesizer synth = InternalSynthManager.getInstance().getSynthesizer();
                if (synth != null) {
                    InstrumentWrapper gervillInstrument = new InstrumentWrapper(
                            "Gervill",
                            synth,
                            InstrumentWrapper.ALL_CHANNELS // Make it available on all channels
                    );
                    gervillInstrument.setId(System.currentTimeMillis());

                    // Add to config
                    currentConfig.getInstruments().add(gervillInstrument);
                    saveConfiguration(currentConfig);

                    logger.info("Gervill instrument added to configuration");
                }
            }
        } catch (Exception e) {
            logger.error("Error registering Gervill instrument: {}", e.getMessage());
        }
    }

    // Add a method to remove an instrument from the config
    public void removeInstrument(Long instrumentId) {
        List<InstrumentWrapper> instruments = currentConfig.getInstruments();

        try {
            if (instruments != null) {
                boolean removed = instruments.removeIf(i -> i.getId().equals(instrumentId));

                if (removed) {
                    // Save the updated config
                    saveConfiguration(currentConfig);
                    logger.info("Instrument removed: ID {}", instrumentId);
                }
            }
        } catch (Exception e) {
            logger.error("Error removing instrument: {}", e.getMessage());
            // Clean up resources
            // Notify UI of failure
        }
    }

    /**
     * Update a default player in the UserConfig
     *
     * @param player The modified player to save
     * @return true if successful, false otherwise
     */
    public boolean updateDefaultPlayer(Player player) {
        if (player == null || !Boolean.TRUE.equals(player.getIsDefault())) {
            logger.warn("Cannot update non-default player in UserConfig");
            return false;
        }

        boolean updated = false;

        try {
            // Update in the appropriate default list based on player type
            if (player instanceof Strike) {
                // Update in strikes list
                for (int i = 0; i < currentConfig.getDefaultStrikes().size(); i++) {
                    Strike strike = currentConfig.getDefaultStrikes().get(i);
                    if (strike.getId().equals(player.getId())) {
                        currentConfig.getDefaultStrikes().set(i, (Strike) player);
                        updated = true;
                        break;
                    }
                }

                // Add if not found
                if (!updated) {
                    currentConfig.getDefaultStrikes().add((Strike) player);
                    updated = true;
                }
            } else if (player instanceof Note) {
                // Update in notes list
                for (int i = 0; i < currentConfig.getDefaultNotes().size(); i++) {
                    Note note = currentConfig.getDefaultNotes().get(i);
                    if (note.getId().equals(player.getId())) {
                        currentConfig.getDefaultNotes().set(i, (Note) player);
                        updated = true;
                        break;
                    }
                }

                // Add if not found
                if (!updated) {
                    currentConfig.getDefaultNotes().add((Note) player);
                    updated = true;
                }
            }

            // Save config if player was updated
            if (updated) {
                logger.info("Default player updated in UserConfig: {}", player.getName());
                saveConfiguration(currentConfig);
            } else {
                logger.warn("Could not find default player to update: {}", player.getId());
            }

            return updated;
        } catch (Exception e) {
            logger.error("Error updating default player: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Update a default instrument in the UserConfig
     *
     * @param instrument The instrument to update
     * @return true if successful, false otherwise
     */
    public boolean updateDefaultInstrument(InstrumentWrapper instrument) {
        if (instrument == null || !Boolean.TRUE.equals(instrument.getIsDefault())) {
            logger.warn("Cannot update non-default instrument in UserConfig");
            return false;
        }

        boolean updated = false;

        try {
            // Find and update in instruments list
            for (int i = 0; i < currentConfig.getInstruments().size(); i++) {
                InstrumentWrapper existing = currentConfig.getInstruments().get(i);
                if (existing.getId().equals(instrument.getId())) {
                    currentConfig.getInstruments().set(i, instrument);
                    updated = true;
                    break;
                }
            }

            // Add if not found
            if (!updated) {
                currentConfig.getInstruments().add(instrument);
                updated = true;
            }

            // Save config if instrument was updated
            if (updated) {
                logger.info("Default instrument updated in UserConfig: {}", instrument.getName());
                saveConfiguration(currentConfig);
            } else {
                logger.warn("Could not find default instrument to update: {}", instrument.getId());
            }

            return updated;
        } catch (Exception e) {
            logger.error("Error updating default instrument: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Backup current configuration to a file
     *
     * @param backupPath Path to save the backup
     * @return True if successful, false otherwise
     */
    public boolean backupConfiguration(String backupPath) {
        logger.info("Backing up configuration to: {}", backupPath);
        try {
            return configHelper.saveConfigToJSON(currentConfig, backupPath);
        } catch (Exception e) {
            logger.error("Error backing up configuration: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Restore configuration from a backup file
     *
     * @param backupPath Path to the backup file
     * @return True if successful, false otherwise
     */
    public boolean restoreConfiguration(String backupPath) {
        logger.info("Restoring configuration from: {}", backupPath);
        try {
            UserConfig restoredConfig = configHelper.loadConfigFromJSON(backupPath);
            if (restoredConfig != null) {
                // Validate and migrate the restored config
                restoredConfig = migrateConfigIfNeeded(restoredConfig);
                if (validateConfig(restoredConfig)) {
                    // Save to persistent storage
                    configHelper.saveConfig(restoredConfig);
                    currentConfig = restoredConfig;
                    commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
                    return true;
                } else {
                    logger.warn("Restored configuration failed validation");
                }
            }
            return false;
        } catch (Exception e) {
            logger.error("Error restoring configuration: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Validates the user configuration for completeness and consistency
     *
     * @param config The configuration to validate
     * @return True if valid, false otherwise
     */
    private boolean validateConfig(UserConfig config) {
        if (config == null) {
            logger.error("Configuration is null");
            return false;
        }

        // Validate instruments
        if (config.getInstruments() == null) {
            logger.warn("Instruments list is null, initializing empty list");
            config.setInstruments(new ArrayList<>());
        }

        // Validate instruments have required fields
        boolean allValid = true;
        for (InstrumentWrapper instrument : config.getInstruments()) {
            if (instrument.getId() == null) {
                logger.warn("Instrument missing ID: {}", instrument.getName());
                instrument.setId(generateUniqueId());
                allValid = false;
            }

            if (instrument.getName() == null || instrument.getName().trim().isEmpty()) {
                logger.warn("Instrument has invalid name, ID: {}", instrument.getId());
                allValid = false;
            }
        }

        return allValid;
    }

    /**
     * Check if configuration requires migration and perform if needed
     *
     * @param config The configuration to check
     * @return The migrated configuration
     */
    private UserConfig migrateConfigIfNeeded(UserConfig config) {
        if (config == null) return new UserConfig();

        // Check version and migrate as needed
        if (config.getConfigVersion() < 1) {
            // logger.info("Migrating configuration from legacy format");
            // Apply migration steps
            config.setConfigVersion(1);
        }

        // Add future version migrations here

        config.setLastUpdated(new Date());
        return config;
    }

    /**
     * Generate a unique ID for an instrument
     */
    private Long generateUniqueId() {
        return System.currentTimeMillis();
    }

    /**
     * Make multiple changes to the configuration in a single transaction
     *
     * @param configUpdater A function that modifies the configuration
     * @return True if successful, false otherwise
     */
    public synchronized boolean updateConfigInTransaction(Function<UserConfig, Boolean> configUpdater) {
        // Create a temporary copy to work with
        UserConfig tempConfig = cloneConfig(currentConfig);

        try {
            // Apply the updates to the temporary copy
            boolean successful = configUpdater.apply(tempConfig);

            if (!successful) {
                logger.warn("Configuration update transaction failed (callback returned false)");
                return false;
            }

            // Validate the updated configuration
            if (!validateConfig(tempConfig)) {
                logger.warn("Configuration update failed validation");
                return false;
            }

            // Update last modified timestamp
            tempConfig.setLastUpdated(new Date());

            // Persist the updated configuration
            configHelper.saveConfig(tempConfig);

            // Update the in-memory configuration
            currentConfig = tempConfig;

            // Notify listeners
            commandBus.publish(Commands.USER_CONFIG_UPDATED, this, currentConfig);

            return true;
        } catch (Exception e) {
            logger.error("Error during configuration transaction: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create a deep clone of the configuration
     */
    private UserConfig cloneConfig(UserConfig source) {
        try {
            // Use Jackson for deep cloning
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(mapper.writeValueAsString(source), UserConfig.class);
        } catch (Exception e) {
            logger.error("Error cloning configuration: {}", e.getMessage(), e);
            // Fallback to creating a new object
            return new UserConfig();
        }
    }

    /**
     * Populate the current configuration with a default set of instruments
     * Properly initialized with InternalSynthManager
     *
     * @return true if successfully populated, false otherwise
     */
    public boolean populateDefaultInstruments() {
        logger.info("Populating UserConfig with default instruments");

        try {
            // Get the internal synth manager
            InternalSynthManager synthManager = InternalSynthManager.getInstance();

            // Ensure synthesizer is available
            if (synthManager.getSynthesizer() == null) {
                logger.error("Cannot create default instruments - synthesizer not available");
                return false;
            }

            List<InstrumentWrapper> defaultInstruments = new ArrayList<>();

            // Add default drum instruments (channel 9 - MIDI channel 10)
            for (int i = 1; i <= 16; i++)
                defaultInstruments.add(createDrumInstrument(i, synthManager));

            // Add default melodic instruments on non-drum channels
            int[] melodicChannels = {0, 1, 2, 3, 4, 5, 6, 7, 10, 11, 12, 13, 14, 15};
            for (int i = 0; i < 12; i++)
                defaultInstruments.add(createMelodicInstrument(i, melodicChannels, synthManager));

            // Add all instruments to the config
            if (currentConfig.getInstruments() == null) {
                currentConfig.setInstruments(new ArrayList<>());
            }

            // Check for duplicates and add only new instruments
            for (InstrumentWrapper newInst : defaultInstruments) {
                boolean exists = false;
                for (InstrumentWrapper existingInst : currentConfig.getInstruments()) {
                    if (existingInst.getName().equals(newInst.getName())) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    currentConfig.getInstruments().add(newInst);
                    // Initialize the instrument in the synth (send program change)
                    try {
                        if (newInst.getReceiver() != null) {
                            ShortMessage msg = new ShortMessage();
                            msg.setMessage(ShortMessage.PROGRAM_CHANGE,
                                    newInst.getChannel(),
                                    newInst.getPreset(), 0);
                            newInst.getReceiver().send(msg, -1);
                            logger.debug("Sent program change for {}: channel {}, program {}",
                                    newInst.getName(), newInst.getChannel(), newInst.getPreset());
                        }
                    } catch (Exception e) {
                        logger.warn("Could not send program change for {}: {}", newInst.getName(), e.getMessage());
                    }
                }
            }

            // Save the updated config
            saveConfiguration(currentConfig);

            // Update the instrument manager's cache
            InstrumentManager.getInstance().refreshCache(currentConfig.getInstruments());

            logger.info("Added {} default instruments to UserConfig", defaultInstruments.size());
            return true;

        } catch (Exception e) {
            logger.error("Error populating default instruments: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Populate the current configuration with a default set of players:
     * - 16 Strike players (drums) with incrementing root notes
     * - 12 Note players (melodic) with different presets
     * <p>
     * These players will be added to UserConfig, not to a session.
     *
     * @return true if successfully populated, false otherwise
     */
    public boolean populateDefaultPlayers() {
        logger.info("Populating UserConfig with default players");

        try {
            // Make sure we have instruments to associate with players
            if (currentConfig.getInstruments() == null || currentConfig.getInstruments().isEmpty()) {
                logger.info("No instruments available - creating default instruments first");
                populateDefaultInstruments();
            }

            // Initialize players list if needed
            if (currentConfig.getDefaultNotes() == null) {
                currentConfig.setDefaultNotes(new ArrayList<>());
            }

            if (currentConfig.getDefaultStrikes() == null) {
                currentConfig.setDefaultStrikes(new ArrayList<>());
            }

            // Track created players for batch save
            List<Strike> defaultDrumPlayers = new ArrayList<>();
            List<Note> defaultMelodicPlayers = new ArrayList<>();

            // Create 16 Strike (drum) players with specific root notes
            createDefaultDrumPlayers(defaultDrumPlayers);

            // Create 12 Note (melodic) players with different presets
            createDefaultMelodicPlayers(defaultMelodicPlayers);

            // Add all created players to the UserConfig
            currentConfig.setDefaultNotes(defaultMelodicPlayers);
            currentConfig.setDefaultStrikes(defaultDrumPlayers);
            currentConfig.setHasDefaults(true);

            // Save the updated configuration
            saveConfiguration(currentConfig);

            logger.info("Added {} default players to UserConfig",
                    defaultDrumPlayers.size() + defaultMelodicPlayers.size());
            return true;

        } catch (Exception e) {
            logger.error("Error populating default players: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Create default Strike (drum) players
     *
     * @param playerList List to add created players to
     */
    private void createDefaultDrumPlayers(List<Strike> playerList) {
        logger.info("Creating default drum players");

        // Get drum instruments from config
        List<InstrumentWrapper> drumInstruments = currentConfig.getInstruments().stream()
                .filter(inst -> inst.getName().startsWith("Default Drum"))
                .toList();

        if (drumInstruments.isEmpty()) {
            logger.warn("No drum instruments found for default players");
            return;
        }

        // Get drum names from InternalSynthManager
        InternalSynthManager synthManager = InternalSynthManager.getInstance();

        // Standard GM drum kit starts at note 35/36
        for (int i = 0; i < 16; i++) {
            // Calculate root note (36 = bass drum, 38 = snare, etc.)
            int rootNote = 36 + i;

            // Choose appropriate instrument (round-robin if we have fewer instruments than players)
            InstrumentWrapper instrument = drumInstruments.get(i % drumInstruments.size());

            // Get the proper drum name from InternalSynthManager
            String drumName = synthManager.getDrumName(rootNote);
            if (drumName == null || drumName.isEmpty()) {
                drumName = "Drum " + rootNote;
            }

            // Create the drum player
            Strike player = createStrike(drumName, rootNote, instrument);

            // Add to player list
            playerList.add(player);
            logger.debug("Created default drum player: {} (note: {})", player.getName(), rootNote);
        }
    }

    /**
     * Create default Note (melodic) players
     *
     * @param playerList List to add created players to
     */
    private void createDefaultMelodicPlayers(List<Note> playerList) {
        logger.info("Creating default melodic players");

        // Get melodic instruments from config
        List<InstrumentWrapper> melodicInstruments = currentConfig.getInstruments().stream()
                .filter(inst -> inst.getName().startsWith("Default Melo"))
                .toList();

        if (melodicInstruments.isEmpty()) {
            logger.warn("No melodic instruments found for default players");
            return;
        }

        // Use variety of channels for melodic instruments (avoiding channel 9 for drums)
        int[] melodicChannels = {3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15};

        // Create 12 melodic players with distinctive sounds
        for (int i = 0; i < melodicChannels.length; i++) {
            // Always use middle C (60) as the root note
            int rootNote = 60;

            // Choose appropriate instrument and channel
            InstrumentWrapper instrument = melodicInstruments.get(i % melodicInstruments.size());

            // Make sure each player gets a unique preset/sound
            // Assign presets that make musical sense grouped by families:
            // 0-7: Pianos, 8-15: Chromatic, 16-23: Organ, 24-31: Guitar, etc.
            int preset = (i * 8) % 127; // Jump through sound families

            // Assign instrument preset (program change)
            instrument.setPreset(preset);
            String name = InternalSynthManager.getInstance().getGeneralMIDIPresetNames().get(preset);
            // Send program change to the device if possible
            try {
                if (instrument.getReceiver() != null) {
                    ShortMessage msg = new ShortMessage();
                    int channel = melodicChannels[i % melodicChannels.length];
                    msg.setMessage(ShortMessage.PROGRAM_CHANGE, channel, preset, 0);
                    instrument.getReceiver().send(msg, -1);
                    logger.debug("Sent program change for melodic player {}: channel {}, program {}",
                            i, channel, preset);
                }
            } catch (Exception e) {
                logger.warn("Could not send program change: {}", e.getMessage());
            }

            // Create the melodic player
            Note player = new Note();
            player.setId(RedisService.getInstance().getNextPlayerId());
            player.setName(name);
            player.setIsDefault(true);
            player.setMelodicPlayer(true);
            player.setRootNote(rootNote);

            // Set channel - use different channels for different players
            int channel = melodicChannels[i % melodicChannels.length];
            player.setDefaultChannel(channel);

            // Associate with instrument
            player.setInstrument(instrument);

            // Add variations to make each player sound different
            player.setPanPosition(32 + (i * 5) % 64); // Spread across stereo field

            // Configure velocities for expressive playing
            player.setMinVelocity(60 + (i % 20));
            player.setMaxVelocity(100 + (i % 27));

            // Configure sequence properties
            player.setProbability(100);
            player.setLevel(90);
            player.setRatchetCount(0);
            player.setRatchetInterval(40);
            player.setSparse(0);
            player.setRandomDegree(0);

            // Add to player list
            playerList.add(player);
            logger.debug("Created default melodic player: {} (preset: {})", player.getName(), preset);
        }
    }

    /**
     * Method to populate both default instruments and players
     *
     * @return true if successful, false otherwise
     */
    public boolean populateDefaults() {
        boolean instrumentsCreated = populateDefaultInstruments();
        if (!instrumentsCreated) {
            logger.error("Failed to create default instruments");
            return false;
        }

        boolean playersCreated = populateDefaultPlayers();
        if (!playersCreated) {
            logger.error("Failed to create default players");
            return false;
        }

        logger.info("Successfully created default instruments and players");
        return true;
    }

    /**
     * Find an instrument by ID with proper error handling
     *
     * @param id The instrument ID
     * @return The instrument or null if not found
     */
    public InstrumentWrapper findInstrumentById(Long id) {
        if (id == null || currentConfig == null || currentConfig.getInstruments() == null) {
            return null;
        }

        return currentConfig.getInstruments().stream()
                .filter(i -> id.equals(i.getId()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find instruments by name (partial match)
     *
     * @param nameFragment Name fragment to search for
     * @return List of matching instruments
     */
    public List<InstrumentWrapper> findInstrumentsByName(String nameFragment) {
        if (nameFragment == null || currentConfig == null || currentConfig.getInstruments() == null) {
            return new ArrayList<>();
        }

        return currentConfig.getInstruments().stream()
                .filter(i -> i.getName() != null && i.getName().toLowerCase().contains(nameFragment.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Handle Redis connection status changes
     */
    public void onRedisConnectionChanged(boolean connected) {
        logger.info("Redis connection status changed: {}", connected ? "Connected" : "Disconnected");

        if (connected) {
            // Try to load config on reconnect
            try {
                UserConfig redisConfig = configHelper.loadConfigFromRedis();
                if (redisConfig != null) {
                    // Handle merged changes if needed
                    handleReconnectionConfigUpdate(redisConfig);
                }
            } catch (Exception e) {
                logger.error("Error reloading config after reconnection: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Handle configuration merging when Redis reconnects
     *
     * @param redisConfig The config loaded from Redis
     */
    private void handleReconnectionConfigUpdate(UserConfig redisConfig) {
        // Check if Redis config is newer
        if (redisConfig.getLastUpdated() != null &&
                currentConfig.getLastUpdated() != null &&
                redisConfig.getLastUpdated().after(currentConfig.getLastUpdated())) {

            logger.info("Redis has a newer configuration, updating local copy");
            currentConfig = redisConfig;
            commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
        } else if (currentConfig.getLastUpdated() != null &&
                (redisConfig.getLastUpdated() == null ||
                        currentConfig.getLastUpdated().after(redisConfig.getLastUpdated()))) {

            logger.info("Local config is newer than Redis, updating Redis");
            try {
                configHelper.saveConfig(currentConfig);
            } catch (Exception e) {
                logger.error("Error saving newer config to Redis: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Get all available UserConfig IDs
     *
     * @return List of configuration IDs
     */
    public List<Integer> getAllConfigIds() {
        return configHelper.findAllConfigIds();
    }

    /**
     * Create a new empty UserConfig
     *
     * @param name Name for the new configuration
     * @return The created configuration
     */
    public UserConfig createNewConfig(String name) {
        logger.info("Creating new user configuration: {}", name);

        UserConfig newConfig = new UserConfig();
        newConfig.setName(name);
        newConfig.setLastUpdated(new Date());
        newConfig.setConfigVersion(1);
        newConfig.setInstruments(new ArrayList<>());
        newConfig.setDefaultNotes(new ArrayList<>());
        newConfig.setDefaultStrikes(new ArrayList<>());

        // Save the new config
        configHelper.saveConfig(newConfig);

        logger.info("Created new configuration with ID: {}", newConfig.getId());
        return newConfig;
    }

    /**
     * Load a specific UserConfig by ID and make it the current one
     *
     * @param id The ID of the configuration to load
     * @return True if successful, false otherwise
     */
    public boolean loadConfigById(Integer id) {
        logger.info("Loading user configuration with ID: {}", id);

        try {
            UserConfig loadedConfig = configHelper.loadConfigFromRedis(id);
            if (loadedConfig != null) {
                currentConfig = loadedConfig;
                initialized = true;

                // Initialize any required components
                initializeInstruments();

                // Ensure defaults exist in the loaded configuration
                ensureDefaultsExist();

                // Notify listeners
                commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);

                logger.info("Loaded user configuration with ID: {}", id);
                return true;
            } else {
                logger.warn("No configuration found with ID: {}", id);
                return false;
            }
        } catch (Exception e) {
            logger.error("Error loading configuration with ID {}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Get a specific UserConfig by ID without making it the current one
     *
     * @param id The ID of the configuration to get
     * @return The requested UserConfig or null if not found
     */
    public UserConfig getConfigById(Integer id) {
        return configHelper.loadConfigFromRedis(id);
    }

    /**
     * Delete a user configuration by ID
     *
     * @param id The ID of the configuration to delete
     * @return True if successful, false otherwise
     */
    public boolean deleteConfig(Integer id) {
        // Don't allow deleting the current config
        if (currentConfig != null && id.equals(currentConfig.getId())) {
            logger.warn("Cannot delete the currently active configuration");
            return false;
        }

        return configHelper.deleteConfig(id);
    }

    /**
     * Get ID of the current configuration
     *
     * @return Current configuration ID or null if not set
     */
    public Integer getCurrentConfigId() {
        return currentConfig != null ? currentConfig.getId() : null;
    }

    /**
     * Make a copy of an existing configuration
     *
     * @param sourceId ID of the configuration to copy
     * @param newName  Name for the new configuration
     * @return The newly created configuration or null if failed
     */
    public UserConfig duplicateConfig(Integer sourceId, String newName) {
        logger.info("Duplicating configuration {} as {}", sourceId, newName);

        try {
            // Load the source configuration
            UserConfig source = configHelper.loadConfigFromRedis(sourceId);
            if (source == null) {
                logger.warn("Source configuration not found: {}", sourceId);
                return null;
            }

            // Create a deep copy
            UserConfig copy = cloneConfig(source);

            // Update properties for the new config
            copy.setId(null); // Will be assigned when saved
            copy.setName(newName);
            copy.setLastUpdated(new Date());

            // Save the new config
            configHelper.saveConfig(copy);

            logger.info("Created duplicate configuration with ID: {}", copy.getId());
            return copy;
        } catch (Exception e) {
            logger.error("Error duplicating configuration: {}", e.getMessage(), e);
            return null;
        }
    }

    public boolean defaultPlayersExist(UserConfig config) {
        boolean hasPlayers = config.getDefaultStrikes() != null && !config.getDefaultStrikes().isEmpty() &&
                config.getDefaultNotes() != null && !config.getDefaultNotes().isEmpty();

        return hasPlayers && config.getDefaultStrikes().size() == 16 &&
                config.getDefaultNotes().size() == MIDIConstants.DEFAULT_MONO_SEQUENCERS;
    }

    public boolean defaultInstrumentsExist(UserConfig config) {

        boolean result = false;

        boolean hasInstruments = config.getInstruments() != null && !config.getInstruments().isEmpty();

        if (hasInstruments) {
            // Check for default instruments
            List<InstrumentWrapper> defaultInstruments = config.getInstruments().stream()
                    .filter(i -> Boolean.TRUE.equals(i.getIsDefault()))
                    .toList();

            // Test for specific instrument types
            long drumInstrumentCount = defaultInstruments.stream()
                    .filter(i -> i.getIsDefault() && i.getName() != null && Objects.equals(i.getChannel(), Constants.MIDI_DRUM_CHANNEL))
                    .count();

            long melodicInstrumentCount = defaultInstruments.stream()
                    .filter(i -> i.getIsDefault() && i.getName() != null && !Objects.equals(i.getChannel(), Constants.MIDI_DRUM_CHANNEL))
                    .count();

            result = drumInstrumentCount == 16 && melodicInstrumentCount == MIDIConstants.DEFAULT_MONO_SEQUENCERS;
        }

        return result;
    }

    /**
     * Ensures that the user configuration has default instruments and players
     * If they don't exist, they will be created and the config will be saved
     *
     * @return True if defaults existed or were successfully created, false otherwise
     */
    public boolean ensureDefaultsExist() {
        logger.info("Checking if default instruments and players exist");

        boolean needsSaving = false;

        // First check if default instruments exist
        if (!currentConfig.getHasDefaults()) {
            logger.info("Default instruments don't exist, creating them");
            if (!defaultInstrumentsExist(currentConfig)) {
                boolean success = populateDefaultInstruments();
                if (!success) {
                    logger.error("Failed to create default instruments");
                    return false;
                }
                needsSaving = true;
            }

            // Now check if default players exist
            if (!defaultPlayersExist(currentConfig)) {
                logger.info("Default players don't exist, creating them");
                boolean success = populateDefaultPlayers();
                if (!success) {
                    logger.error("Failed to create default players");
                    return false;
                }
                needsSaving = true;
            }

            // Save the config if we created defaults
            if (needsSaving) {
                logger.info("Saving configuration with newly created defaults");
                try {
                    saveConfiguration(currentConfig);
                } catch (Exception e) {
                    logger.error("Error saving configuration with defaults: {}", e.getMessage(), e);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Get all Strike objects ordered by ID
     *
     * @return List of Strike objects sorted by ID
     */
    public List<Strike> getStrikesOrderedById() {
        if (currentConfig == null || currentConfig.getDefaultStrikes() == null) {
            return new ArrayList<>();
        }

        return currentConfig.getDefaultStrikes().stream()
                .sorted(Comparator.comparing(Strike::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Get all Note objects ordered by ID
     *
     * @return List of Note objects sorted by ID
     */
    public List<Note> getNotesOrderedById() {
        if (currentConfig == null || currentConfig.getDefaultNotes() == null) {
            return new ArrayList<>();
        }

        return currentConfig.getDefaultNotes().stream()
                .sorted(Comparator.comparing(Note::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Get all InstrumentWrapper objects ordered by ID
     *
     * @return List of InstrumentWrapper objects sorted by ID
     */
    public List<InstrumentWrapper> getInstrumentsOrderedById() {
        if (currentConfig == null || currentConfig.getInstruments() == null) {
            return new ArrayList<>();
        }

        return currentConfig.getInstruments().stream()
                .sorted(Comparator.comparing(InstrumentWrapper::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Get all Player objects (both Notes and Strikes) ordered by ID
     *
     * @return List of Player objects sorted by ID
     */
    public List<Player> getAllPlayersOrderedById() {
        List<Player> allPlayers = new ArrayList<>();

        // Add Notes
        if (currentConfig != null && currentConfig.getDefaultNotes() != null) {
            allPlayers.addAll(currentConfig.getDefaultNotes());
        }

        // Add Strikes
        if (currentConfig != null && currentConfig.getDefaultStrikes() != null) {
            allPlayers.addAll(currentConfig.getDefaultStrikes());
        }

        // Sort by ID
        return allPlayers.stream()
                .sorted(Comparator.comparing(Player::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    /**
     * Handle actions based on commands
     *
     * @param command The command to handle
     */
    public void onAction(String command) {
        switch (command) {
            case Commands.DEBUG_USER_CONFIG_SAVE -> {
                logger.info("Received debug save request");
                boolean saved = saveConfiguration(currentConfig);
                logger.info("Debug save result: {}", saved ? "SUCCESS" : "FAILED");

                if (saved) {
                    // Refresh all objects after save
                    commandBus.publish(Commands.USER_CONFIG_LOADED, this, currentConfig);
                }
            }
            // Add other cases here as needed
        }
    }
}
