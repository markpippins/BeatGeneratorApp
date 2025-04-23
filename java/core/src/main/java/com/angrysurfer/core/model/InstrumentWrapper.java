package com.angrysurfer.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.model.feature.Pad;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.ReceiverManager;
import com.angrysurfer.core.util.IntegerArrayConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public final class InstrumentWrapper implements Serializable {

    static Logger logger = LoggerFactory.getLogger(InstrumentWrapper.class.getCanonicalName());

    static final Random rand = new Random();

    public static final Integer DEFAULT_CHANNEL = 0;

    public static final Integer[] DEFAULT_CHANNELS = new Integer[] { DEFAULT_CHANNEL };

    public static final Integer[] ALL_CHANNELS = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    private Long id;

    private Boolean internal;

    private List<ControlCode> controlCodes = new ArrayList<>();

    private Set<Pad> pads = new HashSet<>();

    @JsonIgnore
    private transient Map<Integer, String> assignments = new HashMap<>();

    @JsonIgnore
    private Map<Integer, Integer[]> boundaries = new HashMap<>();

    @JsonIgnore
    private Map<Integer, Map<Long, String>> captions = new HashMap<>();

    @JsonIgnore
    private MidiDevice device;

    @JsonIgnore
    private AtomicReference<Receiver> receiver = new AtomicReference<>();

    @Column(name = "name", unique = true)
    private String name;

    private String deviceName;

    @Convert(converter = IntegerArrayConverter.class)
    @Column(name = "channels")
    private Integer[] channels;

    private Integer lowestNote = 0;

    private Integer highestNote = 126;

    private Integer highestPreset;

    private Integer preferredPreset;

    private Boolean hasAssignments;

    private String playerClassName;

    private Boolean available = true;

    private Set<Pattern> patterns;

    // Add fields for soundbank support
    private String soundbankName;
    private Integer bankIndex;
    private Integer currentPreset;

    // Add these fields:
    @JsonIgnore
    private final ShortMessage cachedNoteOn = new ShortMessage();
    @JsonIgnore
    private final ShortMessage cachedNoteOff = new ShortMessage();
    @JsonIgnore
    private final ShortMessage cachedControlChange = new ShortMessage();

    public InstrumentWrapper() {

    }

    public InstrumentWrapper(String name, MidiDevice device) {
        this(name, device, DEFAULT_CHANNELS);
    }

    public InstrumentWrapper(String name, MidiDevice device, int channel) {
        this(name, device, new Integer[] { channel });
    }

    public InstrumentWrapper(String name, MidiDevice device, Integer[] channels) {
        setName(Objects.isNull(name) ? device.getDeviceInfo().getName() : name);
        setDevice(device);
        setDeviceName(device.getDeviceInfo().getName());
        setChannels(channels);
        logger.info("Created instrument {} with channels: {}", getName(), Arrays.toString(channels));
    }

    // Helper method to determine if device is likely multi-timbral
    public boolean isMultiTimbral() {
        return channels != null && channels.length > 1;
    }

    public boolean receivesOn(Integer channel) {
        return Arrays.asList(this.channels).contains(channel);
    }

    // Convenience method for single-channel devices
    public int getDefaultChannel() {
        return channels != null && channels.length > 0 ? channels[0] : DEFAULT_CHANNEL;
    }

    public String assignedControl(int cc) {
        return assignments.getOrDefault(cc, "NONE");
    }

    public void channelPressure(int channel, int data1, int data2)
            throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, (int) data1, (int) data2));
    }

    public void controlChange(int channel, int controller, int value)
            throws InvalidMidiDataException, MidiUnavailableException {
        synchronized(cachedControlChange) {
            cachedControlChange.setMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value);
            sendToDevice(cachedControlChange);
        }
    }

    public void noteOn(int channel, int note, int velocity) throws InvalidMidiDataException, MidiUnavailableException {
        synchronized(cachedNoteOn) {
            cachedNoteOn.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
            sendToDevice(cachedNoteOn);
        }
    }

    public void noteOff(int channel, int note, int velocity) throws InvalidMidiDataException, MidiUnavailableException {
        synchronized(cachedNoteOff) {
            cachedNoteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, velocity);
            sendToDevice(cachedNoteOff);
        }
    }

    public void polyPressure(int channel, int data1, int data2)
            throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.POLY_PRESSURE, channel, (int) data1, (int) data2));
    }

    public void programChange(int channel, int data1, int data2)
            throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.PROGRAM_CHANGE, channel, (int) data1, (int) data2));
    }

    public void start(int channel) throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.START, channel, 0, 0));
    }

    public void stop(int channel) throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.STOP, channel, 1, 1));
    }

    public void randomize(int channel, List<Integer> params) {

        new Thread(() -> params.forEach(cc -> {
            try {
                int value = getBoundaries().containsKey(cc) ? rand.nextInt(getBoundaries().get(cc)[0],
                        getBoundaries().get(cc)[0] >= getBoundaries().get(cc)[1] ? getBoundaries().get(cc)[0] + 1
                                : getBoundaries().get(cc)[1])
                        : rand.nextInt(0, 126);

                sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, cc, value));
            } catch (IllegalArgumentException | MidiUnavailableException | InvalidMidiDataException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })).start();
    }

    public void sendToDevice(ShortMessage message) throws MidiUnavailableException {
        Receiver currentReceiver = ReceiverManager.getInstance().getOrCreateReceiver(deviceName, device);
        
        if (currentReceiver != null) {
            currentReceiver.send(message, -1);
            // Comment out or remove this debug logging - it's in a critical path
            // logger.debug("Sent message: {} to device: {}", 
            //            MidiMessage.lookupCommand(message.getCommand()), getName());
        } else {
            // Still log errors
            logger.error("No valid receiver available for device: {}", deviceName);
            throw new MidiUnavailableException("No valid receiver available for " + deviceName);
        }
    }

    public void playMidiNote(int channel, int noteNumber, int velocity, int durationMS) {
        try {
            // Get selected output device from device selection
            
            if (device == null) {
                device = DeviceManager.getInstance().getMidiDevice(deviceName);
            }
            if (device == null) {
                CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate("MIDI Test", "Error", "No MIDI output device selected")
                );
                return;
            }
            
            // Open device if not already open
            if (!device.isOpen()) {
                device.open();
            }
            
            // Get receiver
            Receiver receiver = device.getReceiver();
            
            // Create note on message
            ShortMessage noteOn = new ShortMessage();
            noteOn.setMessage(ShortMessage.NOTE_ON, channel, noteNumber, velocity);
            receiver.send(noteOn, -1);
            
            // Create note off message (to be sent after a delay)
            ShortMessage noteOff = new ShortMessage();
            noteOff.setMessage(ShortMessage.NOTE_OFF, channel, noteNumber, 0);
            
            // Schedule note off message after 500ms
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    receiver.send(noteOff, -1);
                }
            }, durationMS);
            
            // Log and update status
            CommandBus.getInstance().publish(
                Commands.STATUS_UPDATE,
                this,
                new StatusUpdate("MIDI Test", "Info", 
                    String.format("Sent note: %d on channel: %d with velocity: %d", 
                        noteNumber, channel + 1, velocity))
            );
            
        } catch (Exception e) {
            CommandBus.getInstance().publish(
                Commands.STATUS_UPDATE,
                this,
                new StatusUpdate("MIDI Test", "Error", "Failed to send MIDI note: " + e.getMessage())
            );
            logger.error("Error sending MIDI note", e);
        }
    }

    public void cleanup() {
        logger.info("Cleaning up device: {}", getName());
        
        // Add null check before calling closeReceiver
        if (deviceName != null) {
            ReceiverManager.getInstance().closeReceiver(deviceName);
        } else {
            logger.debug("Skipping receiver cleanup: device name is null for {}", getName());
        }
        
        // Close device if we own it
        if (device != null && device.isOpen()) {
            try {
                device.close();
            } catch (Exception e) {
                logger.debug("Error closing device: {}", e.getMessage());
            }
        }
    }

    boolean initialized = false;

    public void setDevice(MidiDevice device) {
        // Clean up existing resources
        cleanup();
        
        // Update device reference
        this.device = device;
        
        if (device != null) {
            setDeviceName(device.getDeviceInfo().getName());
            
            // Open device if needed
            try {
                if (!device.isOpen()) {
                    device.open();
                }
                logger.info("Device {} initialized successfully", getName());
            } catch (MidiUnavailableException e) {
                logger.error("Failed to open device: {}", e.getMessage());
            }
        }
    }

    // Add finalizer to ensure cleanup
    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }

    public void assign(int cc, String control) {
        getAssignments().put(cc, control);
    }

    public void setBounds(int cc, int lowerBound, int upperBound) {
        getBoundaries().put(cc, new Integer[] { lowerBound, upperBound });
    }

    public Integer getAssignmentCount() {
        return getAssignments().size();
    }

    private String lookupTarget(int key) {
        return assignments.getOrDefault(key, Integer.toString(key));
    }

    /**
     * Get the name of the currently selected soundbank
     * @return The soundbank name
     */
    public String getSoundbankName() {
        return soundbankName;
    }

    /**
     * Set the currently selected soundbank name
     * @param soundbankName The soundbank name
     */
    public void setSoundbankName(String soundbankName) {
        this.soundbankName = soundbankName;
        logger.info("Set soundbank name to: {}", soundbankName);
    }

    /**
     * Get the currently selected bank index
     * @return The bank index
     */
    public Integer getBankIndex() {
        return bankIndex;
    }

    /**
     * Set the currently selected bank index
     * @param bankIndex The bank index
     */
    public void setBankIndex(Integer bankIndex) {
        this.bankIndex = bankIndex;
        logger.info("Set bank index to: {}", bankIndex);
    }

    /**
     * Get the currently selected preset
     * @return The preset number
     */
    public Integer getCurrentPreset() {
        return currentPreset;
    }

    /**
     * Set the currently selected preset
     * @param preset The preset number
     */
    public void setCurrentPreset(int preset) {
        this.currentPreset = preset;
        logger.info("Set current preset to: {}", preset);
    }

    /**
     * Apply the current bank and program settings to the specified channel
     * @param channel The MIDI channel to apply settings to
     * @throws InvalidMidiDataException If the MIDI data is invalid
     * @throws MidiUnavailableException If the MIDI device is unavailable
     */
    public void applyBankAndProgram(int channel) throws InvalidMidiDataException, MidiUnavailableException {
        if (bankIndex != null) {
            // Send bank select MSB (CC 0)
            controlChange(channel, 0, 0);

            // Send bank select LSB (CC 32)
            controlChange(channel, 32, bankIndex);

            // Send program change if we have a preset set
            if (currentPreset != null) {
                programChange(channel, currentPreset, 0);
                logger.info("Applied bank={}, program={} to channel={}", bankIndex, currentPreset, channel);
            }
        }
    }

    public Receiver getReceiver() throws MidiUnavailableException {
        if (device == null || !device.isOpen()) {
            throw new MidiUnavailableException("Device unavailable");
        }
        return device.getReceiver();
    }

    /**
     * Send multiple MIDI CC messages efficiently in bulk
     */
    public void sendBulkCC(int channel, int[] controllers, int[] values) throws MidiUnavailableException {
        if (controllers.length != values.length) {
            throw new IllegalArgumentException("Controller and value arrays must be same length");
        }
        
        Receiver currentReceiver = ReceiverManager.getInstance().getOrCreateReceiver(deviceName, device);
        if (currentReceiver == null) {
            throw new MidiUnavailableException("No receiver available");
        }
        
        // Reuse single message for all CC messages
        ShortMessage msg = new ShortMessage();
        try {
            // Send all CC messages with same timestamp for efficiency
            for (int i = 0; i < controllers.length; i++) {
                msg.setMessage(ShortMessage.CONTROL_CHANGE, channel, controllers[i], values[i]);
                currentReceiver.send(msg, -1);
            }
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data in bulk CC: {}", e.getMessage());
        }
    }
}
