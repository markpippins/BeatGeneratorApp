package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;

import com.angrysurfer.core.proxy.ProxyInstrument;

public class InstrumentManager {
    private static final Logger logger = Logger.getLogger(InstrumentManager.class.getName());
    private static InstrumentManager instance;
    private MidiDeviceManager midiDeviceService;
    
    private InstrumentManager() {}
    
    public static InstrumentManager getInstance() {
        if (instance == null) {
            instance = new InstrumentManager();
        }
        return instance;
    }

    public void setMidiDeviceService(MidiDeviceManager service) {
        this.midiDeviceService = service;
    }
    
    public List<ProxyInstrument> getAvailableInstruments() {
        try {
            Set<String> availableDeviceNames = getAvailableMidiDeviceNames();
            List<ProxyInstrument> allInstruments = new ArrayList<>();
            
            // Add database instruments that match connected devices
            List<ProxyInstrument> dbInstruments = midiDeviceService.findAllInstruments();
            for (ProxyInstrument dbInst : dbInstruments) {
                if (dbInst.getDeviceName() != null && 
                    availableDeviceNames.contains(dbInst.getDeviceName())) {
                    allInstruments.add(dbInst);
                    logger.info("Added configured instrument: " + dbInst.getName());
                }
            }

            // Add any new connected devices that aren't in the database
            for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
                String deviceName = info.getName();
                
                // Skip if not in available devices
                if (!availableDeviceNames.contains(deviceName)) {
                    continue;
                }
                
                // Check if device already exists in our list
                boolean exists = allInstruments.stream()
                        .anyMatch(i -> deviceName.equals(i.getDeviceName()));
                
                // If not found, create new ProxyInstrument
                if (!exists) {
                    ProxyInstrument newInstrument = new ProxyInstrument();
                    newInstrument.setName(info.getName());
                    newInstrument.setDeviceName(deviceName);
                    allInstruments.add(newInstrument);
                    logger.info("Added new device as instrument: " + deviceName);
                }
            }

            // Return sorted list
            return allInstruments.stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            logger.severe("Error getting available instruments: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    private Set<String> getAvailableMidiDeviceNames() {
        Set<String> availableDeviceNames = new HashSet<>();
        try {
            for (MidiDevice.Info info : MidiSystem.getMidiDeviceInfo()) {
                String deviceName = info.getName();
                
                // Skip system devices
                if (deviceName.contains("Mapper") || 
                    deviceName.contains("Sequencer") || 
                    deviceName.contains("Real Time")) {
                    logger.info("Skipping system device: " + deviceName);
                    continue;
                }
                
                MidiDevice device = MidiSystem.getMidiDevice(info);
                // Only include MIDI OUT devices
                if (device.getMaxReceivers() != 0) {
                    availableDeviceNames.add(deviceName);
                    logger.info("Found MIDI OUT device: " + deviceName + 
                              " (" + info.getDescription() + ")");
                }
            }
        } catch (Exception e) {
            logger.severe("Error getting MIDI device names: " + e.getMessage());
        }
        return availableDeviceNames;
    }
}
