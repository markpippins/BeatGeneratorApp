package com.angrysurfer.core.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.exception.MidiDeviceException;
import com.angrysurfer.core.model.midi.Instrument;

public class DeviceManager {

    static Logger logger = LoggerFactory.getLogger(DeviceManager.class.getCanonicalName());

    public static void cleanupMidiDevices() {
        logger.info("cleanupMidiDevices()");
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                if (device.isOpen()) {
                    device.getReceivers().forEach(Receiver::close);
                    device.close();
                }
            } catch (MidiUnavailableException e) {
                logger.warn("Error during cleanup of device: " + info.getName(), e);
            }
        }
    }

    public static MidiDevice getMidiDevice(String name) {
        logger.info("getMidiDevice() - name: {}", name);
        cleanupMidiDevices(); // Add cleanup before getting new device
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            if (info.getName().contains(name)) {
                try {

                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    logger.info("Found requested MIDI device: {} (Receivers: {}, Transmitters: {})",
                            info.getName(),
                            device.getMaxReceivers(),
                            device.getMaxTransmitters());
                    return device;
                } catch (MidiUnavailableException e) {
                    logger.error("Error accessing MIDI device: " + info.getName(), e);
                }
            }
        }
        return null;
    }

    public static List<MidiDevice> getMidiOutDevices() {
        logger.info("getMidiOutDevices()");
        List<MidiDevice> devices = new ArrayList<>();
        MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();

        for (MidiDevice.Info info : infos) {
            try {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                // Only add devices that support output (MaxReceivers > 0 or unlimited (-1))
                if (device.getMaxReceivers() != 0) {
                    devices.add(device);
                    logger.info("Found MIDI output device: {} (Receivers: {})",
                            info.getName(),
                            device.getMaxReceivers());
                }
            } catch (MidiUnavailableException e) {
                logger.error("Error accessing MIDI device: " + info.getName(), e);
            }
        }
        return devices;
    }

    // Improved error handling and validation
    public static List<MidiDevice.Info> getMidiDeviceInfos() {
        logger.info("getMidiDeviceInfos()");
        try {
            return Arrays.stream(MidiSystem.getMidiDeviceInfo())
                    .filter(Objects::nonNull)
                    .map(info -> {
                        return info;
                    })
                    .filter(Objects::nonNull)
                    .toList();
        } catch (Exception ex) {
            throw new MidiDeviceException("Failed to get MIDI device infos", ex);
        }
    }

    public static int midiChannel(int realChannel) {
        return realChannel - 1;
    }

    // Better error handling for reset
    public static void reset() {
        logger.info("reset()");
        try {
            var sequencer = MidiSystem.getSequencer();
            if (sequencer != null) {
                sequencer.getReceivers().forEach(Receiver::close);
            }
        } catch (MidiUnavailableException ex) {
            logger.warn("Failed to reset MIDI system", ex);
            // Don't throw - this is cleanup code
        }
    }

    // Improved resource management
    public static boolean select(MidiDevice device) {
        if (device == null)
            throw new IllegalArgumentException("Device cannot be null");

        logger.info("select({})", device.getDeviceInfo().getName());

        try {
            if (!device.isOpen()) {
                device.open();
            }
            return device.isOpen();
        } catch (MidiUnavailableException ex) {
            throw new MidiDeviceException("Failed to select MIDI device", ex);
        }
    }

    public static boolean select(String name) throws MidiUnavailableException {
        logger.info("select() - name: {}", name);
        logger.info("select({})", name);
        MidiDevice device = getMidiDevice(name);
        return device != null && select(device);
    }

    // Add proper cleanup
    // @Override
    // public void finalize() {
    // reset();
    // }

    // Improved message sending with validation
    @SuppressWarnings("unused")
    public static void sendMessage(Instrument instrument, int channel, int messageType, int data1, int data2) {
        logger.info("sendMessage() - instrument: {}, channel: {}, messageType: {}, data1: {}, data2: {}",
                instrument.getName(), channel, messageType, data1, data2);

        if (Objects.isNull(instrument)) {
            logger.warn("Instrument cannot be null");
            return;
        }

        try {
            ShortMessage message = new ShortMessage(messageType,
                    channel,
                    validateData(data1),
                    validateData(data2));
            instrument.sendToDevice(message);
        } catch (InvalidMidiDataException | MidiUnavailableException ex) {
            throw new MidiDeviceException("Failed to send MIDI message", ex);
        }

    }

    static int validateData(int data) {
        if (data < 0 || data > 126) {
            throw new IllegalArgumentException("MIDI data must be between 0 and 126");
        }
        return data;
    }

}