package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;

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

    public List<InstrumentWrapper> getInstrumentByChannel(int channel) {
        if (needsRefresh) {
            refreshInstruments();
        }
        return instrumentCache.values().stream()
                .filter(i -> i.receivesOn(channel) &&
                        (devices == null || devices.isEmpty() || devices.contains(i.getDeviceName())))
                .collect(Collectors.toList());
    }

    /**
     * Get instrument by ID
     * 
     * @param id The instrument ID
     * @return The instrument, or null if not found
     */
    public InstrumentWrapper getInstrumentById(Long id) {
        if (id == null)
            return null;
        return instrumentCache.get(id);
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
        // Generate the ID using the same formula as MelodicSequencer
        Long id = 9985L + channel;

        // Try to find by ID first
        InstrumentWrapper instrument = instrumentCache.get(id);
        if (instrument != null && !instrument.getAssignedToPlayer()) {
            instrument.setAssignedToPlayer(true);
            return instrument;
        }

        // Next try by name and channel
        for (InstrumentWrapper cached : instrumentCache.values()) {
            if (Boolean.TRUE.equals(cached.getInternal()) &&
                    cached.getChannel() != null &&
                    (!exclusive || !cached.getAssignedToPlayer()) &&
                    cached.getChannel() == channel) {
                cached.setAssignedToPlayer(exclusive);
                return cached;
            }
        }

        // No instrument found, create a new one
        try {
            MidiDevice synthDevice = InternalSynthManager.getInstance().getInternalSynthDevice();
            instrument = new InstrumentWrapper(
                    "Internal Synth " + channel,
                    synthDevice,
                    channel);
            instrument.setId(id);
            instrument.setInternal(true);
            instrument.setDeviceName("Gervill");
            instrument.setSoundbankName("Default");
            instrument.setBankIndex(0);
            updateInstrument(instrument);

            return instrument;
        } catch (MidiUnavailableException e) {
            logger.error("Failed to create internal instrument: {}", e.getMessage());
            return null;
        }
    }
}
