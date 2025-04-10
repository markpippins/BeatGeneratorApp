package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;

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
                // Refresh instruments when user config changes
                refreshInstruments();
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
    }

    public void refreshInstruments() {
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

    public InstrumentWrapper getInstrumentById(Long id) {
        if (needsRefresh) {
            refreshInstruments();
        }
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
     * Updates an instrument in the cache and in the UserConfigManager
     * 
     * @param instrument The instrument to update
     */
    public void updateInstrument(InstrumentWrapper instrument) {
        if (instrument == null) {
            logger.warn("Attempt to update null instrument");
            return;
        }
        
        // Update in local cache
        instrumentCache.put(instrument.getId(), instrument);
        
        // Update in UserConfigManager
        UserConfigManager.getInstance().updateInstrument(instrument);
        
        // Publish event for listeners
        commandBus.publish(Commands.INSTRUMENT_UPDATED, this, instrument);
        
        logger.info("Instrument updated: {} (ID: {})", instrument.getName(), instrument.getId());
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
}
