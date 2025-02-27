package com.angrysurfer.core.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import javax.sound.midi.MidiDevice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisInstrumentHelper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentManager {
    private static final Logger logger = LoggerFactory.getLogger(InstrumentManager.class);
    private static InstrumentManager instance;
    private final RedisInstrumentHelper instrumentHelper;
    private final Map<Long, Instrument> instrumentCache = new HashMap<>();
    private List<MidiDevice> midiDevices;
    private List<String> devices;
    private boolean needsRefresh = true;

    private InstrumentManager(RedisInstrumentHelper instrumentHelper) {
        this.instrumentHelper = instrumentHelper;
        initializeCache();
    }

    public static InstrumentManager getInstance(RedisInstrumentHelper instrumentHelper) {
        if (instance == null) {
            synchronized (InstrumentManager.class) {
                if (instance == null) {
                    instance = new InstrumentManager(instrumentHelper);
                }
            }
        }
        return instance;
    }

    private void initializeCache() {
        logger.info("Initializing instrument cache");
        // List<Instrument> instruments = instrumentHelper.findAllInstruments();
        List<Instrument> instruments = UserConfigurationManager.getInstance().getCurrentConfig().getInstruments();
        instrumentCache.clear();
        
        for (Instrument instrument : instruments) {
            instrumentCache.put(instrument.getId(), instrument);
        }
        
        logger.info("Cached {} instruments", instrumentCache.size());
    }

    public void refreshCache() {
        logger.info("Refreshing instrument cache");
        initializeCache();
    }

    public void refreshInstruments() {
        logger.info("Refreshing instruments cache");
        initializeCache();
        needsRefresh = false;
    }

    public List<Instrument> getInstrumentByChannel(int channel) {
        if (needsRefresh)
            refreshInstruments();
        return instrumentCache.values().stream()
                .filter(i -> i.receivesOn(channel) && devices.contains(i.getDeviceName()))
                .collect(Collectors.toList());
    }

    public Instrument getInstrumentById(Long id) {
        if (needsRefresh)
            refreshInstruments();
        return instrumentCache.get(id);
    }

    public List<String> getInstrumentNames() {
        if (needsRefresh)
            refreshInstruments();
        return instrumentCache.values().stream()
                .map(Instrument::getName)
                .collect(Collectors.toList());
    }

    public Instrument findByName(String name) {
        if (needsRefresh)
            refreshInstruments();
        return instrumentCache.values().stream()
                .filter(i -> i.getName().toLowerCase().equals(name.toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    public Instrument getInstrumentFromCache(Long instrumentId) {
        Instrument instrument = instrumentCache.get(instrumentId);
        if (instrument == null) {
            logger.warn("Cache miss for instrument ID: " + instrumentId + ", refreshing cache");
            initializeCache();
            instrument = instrumentCache.get(instrumentId);
        }
        return instrument;
    }

    public List<Instrument> getCachedInstruments() {
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
}
