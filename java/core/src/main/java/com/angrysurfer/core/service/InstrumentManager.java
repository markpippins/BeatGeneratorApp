package com.angrysurfer.core.service;

import java.util.*;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.redis.InstrumentHelper;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentManager implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentManager.class);
    private static InstrumentManager instance;
    private final InstrumentHelper instrumentHelper;
    private final Map<Long, InstrumentWrapper> instrumentCache = new HashMap<>();
    private List<MidiDevice> midiDevices = new ArrayList<>();
    private List<String> devices = new ArrayList<>();
    private boolean needsRefresh = true;
    private final CommandBus commandBus = CommandBus.getInstance();
    private boolean isInitializing = false;

    // Private constructor for singleton pattern
    private InstrumentManager() {
        this.instrumentHelper = RedisService.getInstance().getInstrumentHelper();
        // Register for command events
        commandBus.register(this);
        // Initial cache load
        refreshInstruments();
    }

    // Static method to get the singleton instance
    public static synchronized InstrumentManager getInstance() {
        if (instance == null) {
            instance = new InstrumentManager();
        }
        return instance;
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;

        switch (action.getCommand()) {
            case Commands.USER_CONFIG_LOADED -> {
                // Check if we're already initializing to prevent recursion
                if (!isInitializing) {
                    refreshInstruments();
                } else {
                    logger.debug("Skipping refresh during initialization");
                }
            }
            case Commands.INSTRUMENT_UPDATED -> {
                // Update single instrument in cache
                if (action.getData() instanceof InstrumentWrapper instrument) {
                    instrumentCache.put(instrument.getId(), instrument);
                    logger.info("Updated instrument in cache: {}", instrument.getName());
                }
            }
            case Commands.INSTRUMENTS_REFRESHED -> {
                // Force cache refresh
                refreshInstruments();
            }
        }
    }

    public void initializeCache() {
        logger.info("Initializing instrument cache");

        // Set flag to prevent recursion
        isInitializing = true;

        try {
            // Get instruments from UserConfigManager which is the source of truth
            List<InstrumentWrapper> instruments = UserConfigManager.getInstance().getInstruments();
            instrumentCache.clear();

            if (instruments != null) {
                for (InstrumentWrapper instrument : instruments) {
                    instrumentCache.put(instrument.getId(), instrument);
                }
                logger.info("Cached {} instruments", instrumentCache.size());
            } else {
                logger.warn("No instruments found in UserConfigManager");
            }

            needsRefresh = false;
        } finally {
            // Always reset the flag when done
            isInitializing = false;
        }
    }

    public void refreshInstruments() {
        // Skip if we're already initializing
        if (isInitializing) {
            logger.debug("Skipping recursive refresh call");
            return;
        }

        logger.info("Refreshing instruments cache");
        initializeCache();
        needsRefresh = false;
    }

    /**
     * Get instruments that can be used on a specific channel
     */
    public List<InstrumentWrapper> getInstrumentByChannel(int channel) {
        return instrumentCache.values().stream()
                .filter(instrument -> {
                    // Safety check for null instrument
                    if (instrument == null)
                        return false;

                    try {
                        // Use safe receivesOn method
                        return instrument.receivesOn(channel);
                    } catch (Exception e) {
                        // Fallback for any unexpected errors
                        logger.warn("Error checking if instrument receives on channel {}: {}",
                                channel, e.getMessage());

                        // Check channel directly as fallback
                        return instrument.getChannel() != null &&
                                instrument.getChannel() == channel;
                    }
                })
                .sorted(Comparator.comparing(InstrumentWrapper::getName))
                .collect(Collectors.toList());
    }

    /**
     * Get instrument by ID
     * 
     * @param id The instrument ID
     * @return The instrument, or null if not found
     */
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

        // Check cache first
        InstrumentWrapper instrument = instrumentCache.get(id);

        // If not in cache, try to load from Redis
        if (instrument == null) {
            logger.info("Instrument with ID {} not found in cache, checking database", id);
            try {
                instrument = RedisService.getInstance().getInstrumentById(id);

                // Add to cache if found
                if (instrument != null) {
                    logger.info("Found instrument in database: {} (ID: {})",
                            instrument.getName(), instrument.getId());
                    instrumentCache.put(id, instrument);
                } else {
                    logger.warn("Instrument with ID {} not found in database", id);
                }
            } catch (Exception e) {
                logger.error("Error retrieving instrument with ID {}: {}", id, e.getMessage(), e);
            }
        } else {
            logger.debug("Found instrument in cache: {} (ID: {})",
                    instrument.getName(), instrument.getId());
        }

        return instrument;
    }

    public List<String> getInstrumentNames() {
        if (needsRefresh) {
            refreshInstruments();
        }
        return instrumentCache.values().stream()
                .map(InstrumentWrapper::getName)
                .collect(Collectors.toList());
    }

    public InstrumentWrapper findByName(String name) {
        if (needsRefresh) {
            refreshInstruments();
        }
        return instrumentCache.values().stream()
                .filter(i -> i.getName().toLowerCase().equals(name.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    public InstrumentWrapper getInstrumentFromCache(Long instrumentId) {
        if (needsRefresh) {
            refreshInstruments();
        }

        InstrumentWrapper instrument = instrumentCache.get(instrumentId);
        if (instrument == null) {
            logger.warn("Cache miss for instrument ID: {}, refreshing cache", instrumentId);
            refreshInstruments();
            instrument = instrumentCache.get(instrumentId);
        }
        return instrument;
    }

    public List<InstrumentWrapper> getCachedInstruments() {
        if (instrumentCache.isEmpty()) {
            logger.info("Cache is empty, initializing...");
            initializeCache();
        }
        return new ArrayList<>(instrumentCache.values());
    }

    public void setMidiDevices(List<MidiDevice> devices) {
        logger.info("Setting MIDI devices: {}", devices.size());
        List<String> deviceNames = devices.stream()
                .map(device -> device.getDeviceInfo().getName())
                .collect(Collectors.toList());

        this.midiDevices = devices;
        this.devices = deviceNames;
        needsRefresh = true;
    }

    /**
     * Update instrument in cache and persist it
     */
    public void updateInstrument(InstrumentWrapper instrument) {
        if (instrument == null || instrument.getId() == null) {
            logger.warn("Cannot update instrument: null or missing ID");
            return;
        }

        // Store in cache
        instrumentCache.put(instrument.getId(), instrument);

        // Persist to storage using RedisService instead of persistenceService
        try {
            RedisService.getInstance().saveInstrument(instrument);
            logger.debug("Saved instrument: {} (ID: {})",
                    instrument.getName(), instrument.getId());
        } catch (Exception e) {
            logger.error("Failed to persist instrument: {}", e.getMessage());
        }
    }

    /**
     * Removes an instrument from the cache and from UserConfigManager
     * 
     * @param instrumentId The ID of the instrument to remove
     */
    public void removeInstrument(Long instrumentId) {
        if (instrumentId == null) {
            logger.warn("Attempt to remove instrument with null ID");
            return;
        }

        // Get instrument for logging before removal
        InstrumentWrapper instrument = instrumentCache.get(instrumentId);
        String name = instrument != null ? instrument.getName() : "Unknown";

        // Remove from cache
        instrumentCache.remove(instrumentId);

        // Remove from UserConfigManager
        UserConfigManager.getInstance().removeInstrument(instrumentId);

        logger.info("Instrument removed: {} (ID: {})", name, instrumentId);
    }

    /**
     * Find or create an internal instrument for the specified channel
     * 
     * @param channel The MIDI channel
     * @return An InstrumentWrapper for the internal synth
     */
    public InstrumentWrapper findOrCreateInternalInstrument(int channel) {
        // Try to find an existing internal instrument for this channel
        for (InstrumentWrapper instrument : getCachedInstruments()) {
            if (Boolean.TRUE.equals(instrument.getInternal()) &&
                    instrument.getChannel() == channel) {
                return instrument;
            }
        }

        // Create a new internal instrument
        InstrumentWrapper internalInstrument = new InstrumentWrapper(
                "Internal Synth",
                null, // Internal synth uses null device
                channel);

        // Configure as internal instrument
        internalInstrument.setInternal(true);
        internalInstrument.setDeviceName("Gervill");
        internalInstrument.setSoundbankName("Default");
        internalInstrument.setBankIndex(0);
        internalInstrument.setPreset(0); // Piano
        internalInstrument.setId(9985L + channel);

        // Add to cache and persist
        updateInstrument(internalInstrument);

        return internalInstrument;
    }

    /**
     * Get the default internal instrument for the sequencer's channel
     * 
     * @return An InstrumentWrapper for the internal synth
     */
    private InstrumentWrapper getDefaultInstrument(int channel) {
        // Create with null device (indicates internal synth)
        InstrumentWrapper internalInstrument = new InstrumentWrapper(
                "Internal Synth",
                null,
                channel // Use the sequencer's channel
        );

        // Configure as internal instrument
        internalInstrument.setInternal(true);
        internalInstrument.setDeviceName("Gervill");
        internalInstrument.setSoundbankName("Default");
        internalInstrument.setBankIndex(0);
        internalInstrument.setPreset(0); // Default to piano
        internalInstrument.setId(9985L + channel); // Use channel for unique ID

        // Register with manager
        InstrumentManager.getInstance().updateInstrument(internalInstrument);

        return internalInstrument;
    }

    /**
     * Get instrument for internal synthesizer on a specific channel
     * 
     * @param channel MIDI channel
     * @return The instrument, creating it if necessary
     */
    public InstrumentWrapper getOrCreateInternalSynthInstrument(int channel, boolean exclusive) {
        // First try to find an existing instrument
        Long id = 9985L + channel;

        // Try to find by ID first
        InstrumentWrapper instrument = instrumentCache.get(id);
        if (instrument != null && (!exclusive || !instrument.getAssignedToPlayer())) {
            if (exclusive) {
                instrument.setAssignedToPlayer(true);
            }
            return instrument;
        }

        // Next try by device name and channel
        for (InstrumentWrapper cached : instrumentCache.values()) {
            if (InternalSynthManager.getInstance().isInternalSynthInstrument(cached) &&
                    cached.getChannel() != null &&
                    cached.getChannel() == channel &&
                    (!exclusive || !cached.getAssignedToPlayer())) {

                if (exclusive) {
                    cached.setAssignedToPlayer(true);
                }
                return cached;
            }
        }

        // No instrument found, create a new one using InternalSynthManager
        boolean isDrumChannel = (channel == 9);
        instrument = InternalSynthManager.getInstance().createInternalInstrument(
                channel, isDrumChannel, null);

        if (instrument != null) {
            // Store in our cache
            updateInstrument(instrument);

            // Mark as assigned if exclusive
            if (exclusive) {
                instrument.setAssignedToPlayer(true);
            }
        }

        return instrument;
    }

    /**
 * Refresh the instrument cache with the provided list of instruments
 */
public void refreshCache(List<InstrumentWrapper> instruments) {
    if (instruments == null) return;
    
    // Clear the existing cache
    instrumentCache.clear();
    
    // Add all instruments to the cache
    for (InstrumentWrapper instrument : instruments) {
        if (instrument != null && instrument.getId() != null) {
            instrumentCache.put(instrument.getId(), instrument);
        }
    }
    
    logger.debug("Refreshed instrument cache with {} instruments", instruments.size());
}


    /**
     * Determine who owns/uses an instrument
     *
     * @param instrument The instrument to check
     * @return A string description of the owner(s)
     */
    public String determineInstrumentOwner(InstrumentWrapper instrument) {
        if (instrument == null || instrument.getId() == null) {
            return "";
        }

        List<String> owners = new ArrayList<>();

        try {
            // Check session players
            Set<Player> sessionPlayers = SessionManager.getInstance().getActiveSession().getPlayers();
            if (sessionPlayers != null) {
                for (Player player : sessionPlayers) {
                    if (player != null &&
                            player.getInstrumentId() != null &&
                            player.getInstrumentId().equals(instrument.getId())) {
                        owners.add("Session: " + player.getName());
                    }
                }
            }

            // Check melodic sequencers
            for (MelodicSequencer sequencer : MelodicSequencerManager.getInstance().getAllSequencers()) {
                if (sequencer != null && sequencer.getPlayer() != null &&
                        sequencer.getPlayer().getInstrumentId() != null &&
                        sequencer.getPlayer().getInstrumentId().equals(instrument.getId())) {
                    owners.add("Melodic: " + sequencer.getClass().getName());
                }
            }

            // Check drum sequencers (which have multiple players)
            for (DrumSequencer sequencer : DrumSequencerManager.getInstance().getAllSequencers()) {
                if (sequencer != null && sequencer.getPlayers() != null) {
                    for (Player player : sequencer.getPlayers()) {
                        if (player != null &&
                                player.getInstrumentId() != null &&
                                player.getInstrumentId().equals(instrument.getId())) {
                            owners.add(sequencer.getClass().getName() + " (" + player.getName() + ")");
                        }
                    }
                }
            }

            if (owners.isEmpty()) {
                return "None";
            } else if (owners.size() <= 2) {
                return String.join(", ", owners);
            } else {
                // If there are many owners, show a count
                return owners.get(0) + " and " + (owners.size() - 1) + " more";
            }
        } catch (Exception e) {
            logger.error("Error determining instrument owner: {}", e.getMessage(), e);
            return "Error";
        }
    }
}
