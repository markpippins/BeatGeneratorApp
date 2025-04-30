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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import com.angrysurfer.core.service.InternalSynthManager;
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

    @JsonIgnore
    private transient MidiDevice.Info deviceInfo = null;

    private Boolean internal;

    // Add these fields to InstrumentWrapper
    private int bankMSB = 0; // Default to bank 0
    private int bankLSB = 0; // Default to bank 0

    private int program = 0; // Default to program 0
    
    private Boolean assignedToPlayer = false;

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

    private String deviceName = "Gervill";

    private boolean internalSynth;

    private String description;

    @Convert(converter = IntegerArrayConverter.class)
    private Integer[] channels;

    private Integer channel;

    private Integer lowestNote = 0;

    private Integer highestNote = 127;

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

    // Add this static field to the class
    private static ScheduledExecutorService NOTE_OFF_SCHEDULER;

    /**
     * Default constructor with safe boolean initialization
     */
    public InstrumentWrapper() {
        // Initialize boolean fields to prevent NPE
        this.internal = Boolean.FALSE;
        this.available = Boolean.FALSE;
        this.initialized = Boolean.FALSE;
    }

    public InstrumentWrapper(String name, MidiDevice device) {
        this(name, device, DEFAULT_CHANNELS);
    }

    /**
     * Constructor for creating an instrument wrapper
     */
    public InstrumentWrapper(String name, MidiDevice device, int channel) {
        this.name = name;
        this.device = device;
        this.channel = channel;
        
        // Safely get device info if device is not null
        if (device != null) {
            this.deviceInfo = device.getDeviceInfo();
            this.deviceName = deviceInfo.getName();
        } else {
            // Set default values for null device
            this.deviceName = "Internal";
        }
        
        // Set default values
        this.internal = (device == null);  // Assume internal if device is null
    }

    public InstrumentWrapper(String name, MidiDevice device, Integer[] channels) {
        setName(Objects.isNull(name) ? device.getDeviceInfo().getName() : name);
        setDevice(device);
        setDeviceName(device.getDeviceInfo().getName());
        setChannels(channels);
        logger.info("Created instrument {} with channels: {}", getName(), Arrays.toString(channels));
    }

    public void setName(String name) {
        this.name = name;
        if (name != null) {
 
            if (name.toLowerCase().contains("gervill")) {
                this.internal = true;
            }
            if (name.toLowerCase().contains("synth")) {
                this.internal = true;
            }

            if (name.toLowerCase().contains("drum")) {
                this.internal = true;
            }
 
            // this.name = name.replaceAll("[^a-zA-Z0-9]", "_");
        }
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

    public void channelPressure(int data1, int data2)
            throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, (int) data1, (int) data2));
    }

    public void controlChange(int controller, int value) {
        if (device == null) {
            logger.warn("Cannot send control change - device is null");
            return;
        }
        
        try {
            ShortMessage message = new ShortMessage();
            message.setMessage(ShortMessage.CONTROL_CHANGE, channel, controller, value);
            sendToDevice(message);
        } catch (InvalidMidiDataException e) {
            logger.error("Invalid MIDI data for control change: {}", e.getMessage());
        } catch (Exception e) {
            // Catch all other exceptions to prevent UI disruption
            logger.error("Error sending control change: {}", e.getMessage());
        }
    }

    public void noteOn(int note, int velocity) throws InvalidMidiDataException, MidiUnavailableException {
        synchronized (cachedNoteOn) {
            cachedNoteOn.setMessage(ShortMessage.NOTE_ON, channel, note, velocity);
            sendToDevice(cachedNoteOn);
        }
    }

    public void noteOff(int note, int velocity) throws InvalidMidiDataException, MidiUnavailableException {
        synchronized (cachedNoteOff) {
            cachedNoteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, velocity);
            sendToDevice(cachedNoteOff);
        }
    }

    public void polyPressure(int data1, int data2)
            throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.POLY_PRESSURE, channel, (int) data1, (int) data2));
    }

    public void programChange(int data1, int data2)
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
                        : rand.nextInt(0, 127);

                sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, cc, value));
            } catch (IllegalArgumentException | MidiUnavailableException | InvalidMidiDataException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })).start();
    }

    public void sendToDevice(MidiMessage message) throws MidiUnavailableException {
        if (device == null) {
            logger.warn("Cannot send MIDI message - device is null");
            return; // Just return instead of throwing exception
        }
        
        try {
            // Get receiver but handle null case gracefully
            Receiver receiver = device.getReceiver();
            if (receiver != null) {
                receiver.send(message, -1); // -1 means process immediately
            } else {
                logger.warn("No receiver available for device: {}", 
                          device.getDeviceInfo().getName());
                throw new MidiUnavailableException("Receiver is null");
            }
        } catch (MidiUnavailableException e) {
            // Log but don't rethrow - prevent cascading errors
            logger.error("MIDI device unavailable: {}", e.getMessage());
            
            // Try to recover the device if it's closed
            if (device != null && !device.isOpen()) {
                try {
                    device.open();
                    logger.info("Successfully reopened MIDI device");
                    // Try again with newly opened device
                    Receiver receiver = device.getReceiver();
                    if (receiver != null) {
                        receiver.send(message, -1);
                    }
                } catch (Exception reopenEx) {
                    logger.error("Failed to reopen MIDI device: {}", reopenEx.getMessage());
                    throw e;
                }
            }
        }
    }

    /**
     * Helper method to initialize an instrument with its stored preset
     * 
     * @param instrument The instrument to initialize
     */
    public void initializeInstrument(InstrumentWrapper instrument) {
        if (instrument == null || instrument.getCurrentPreset() == null) {
            return;
        }

        try {
            int channel = 0; // Default channel
            int preset = instrument.getCurrentPreset();

            // Apply bank if specified
            if (instrument.getBankIndex() != null && instrument.getBankIndex() > 0) {
                instrument.controlChange( 0, 0); // Bank MSB
                instrument.controlChange( 32, instrument.getBankIndex()); // Bank LSB
            }

            // Apply program change
            instrument.programChange( preset, 0);
            logger.debug("Initialized instrument {} with preset {}",
                    instrument.getName(), preset);
        } catch (Exception e) {
            logger.warn("Failed to initialize instrument: {}", e.getMessage());
        }
    }

    /**
     * Play a note with specified decay time, using optimized path when possible
     */
    public void playMidiNote(int note, int velocity, int decay) {
        // Try internal synth optimization first
        // if ("Gervill".equals(deviceName)) {
        //     // Use optimized internal synth path
        //     InternalSynthManager.getInstance().playNote(note, velocity, decay, channel);
        //     return;
        // }

        // Fall back to standard MIDI path for external devices
        try {
            noteOn( note, velocity);
            // Schedule note off
            scheduleNoteOff( note, velocity, decay);
        } catch (InvalidMidiDataException e) {
            throw new RuntimeException(e);
        } catch (MidiUnavailableException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Schedule a noteOff command after specified delay
     * 
     * @param note     Note number to turn off
     * @param velocity Release velocity (usually 0)
     * @param delayMs  Delay in milliseconds before sending note off
     */
    private void scheduleNoteOff(int note, int velocity, int delayMs) {
        // Use a shared scheduled executor service for better performance than
        // individual timers
        if (NOTE_OFF_SCHEDULER == null) {
            NOTE_OFF_SCHEDULER = Executors.newScheduledThreadPool(1, r -> {
                Thread t = new Thread(r, "NoteOffScheduler");
                t.setDaemon(true); // Make sure this doesn't prevent JVM shutdown
                return t;
            });
        }

        // Schedule the note-off message
        NOTE_OFF_SCHEDULER.schedule(() -> {
            try {
                // Send note-off message using existing method
                noteOff( note, 0); // Usually 0 velocity for note off

                // Debug logging if needed (uncomment for debugging)
                // logger.debug("Note off sent for note {} on channel {}", note, channel);
            } catch (Exception e) {
                // Log any errors but don't propagate - this is running asynchronously
                logger.warn("Failed to send note-off for note {} on channel {}: {}",
                        note, channel, e.getMessage());

                // Try a second time with a direct receiver approach as fallback
                try {
                    Receiver receiver = ReceiverManager.getInstance()
                            .getOrCreateReceiver(deviceName, device);
                    if (receiver != null) {
                        ShortMessage noteOff = new ShortMessage();
                        noteOff.setMessage(ShortMessage.NOTE_OFF, channel, note, 0);
                        receiver.send(noteOff, -1);
                        logger.debug("Note off sent via fallback method");
                    }
                } catch (Exception ex) {
                    logger.error("Fallback note-off also failed: {}", ex.getMessage());
                }
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Shutdown the scheduler when needed
     */
    public static void shutdownScheduler() {
        if (NOTE_OFF_SCHEDULER != null) {
            NOTE_OFF_SCHEDULER.shutdown();
            try {
                if (!NOTE_OFF_SCHEDULER.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    NOTE_OFF_SCHEDULER.shutdownNow();
                }
            } catch (InterruptedException e) {
                NOTE_OFF_SCHEDULER.shutdownNow();
                Thread.currentThread().interrupt();
            }
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
     * 
     * @return The soundbank name
     */
    public String getSoundbankName() {
        return soundbankName;
    }

    /**
     * Set the currently selected soundbank name
     * 
     * @param soundbankName The soundbank name
     */
    public void setSoundbankName(String soundbankName) {
        this.soundbankName = soundbankName;
        logger.info("Set soundbank name to: {}", soundbankName);
    }

    /**
     * Get the currently selected bank index, calculated from MSB/LSB if available
     * 
     * @return The bank index
     */
    public Integer getBankIndex() {
        // If we have MSB/LSB values set, calculate the combined index
        if (bankMSB != 0 || bankLSB != 0) {
            return (bankMSB << 7) | bankLSB;
        }
        // Otherwise return the stored bankIndex field
        return bankIndex;
    }

    /**
     * Set the currently selected bank index
     * Updates both the bankIndex field and the MSB/LSB values
     * 
     * @param bankIndex The bank index
     */
    public void setBankIndex(Integer bankIndex) {
        this.bankIndex = bankIndex;
        
        if (bankIndex == null) {
            this.bankMSB = 0;
            this.bankLSB = 0;
        } else {
            // Use upper/lower bytes of the integer for MSB/LSB
            this.bankMSB = (bankIndex >> 7) & 0x7F;  // Upper 7 bits
            this.bankLSB = bankIndex & 0x7F;         // Lower 7 bits
        }
        
        logger.info("Set bank index to: {} (MSB: {}, LSB: {})", 
                    bankIndex, bankMSB, bankLSB);
    }

    /**
     * Get the currently selected preset
     * 
     * @return The preset number
     */
    public Integer getCurrentPreset() {
        return currentPreset;
    }

    /**
     * Set the currently selected preset
     * 
     * @param preset The preset number
     */
    public void setCurrentPreset(int preset) {
        this.currentPreset = preset;
        logger.info("Set current preset to: {}", preset);
    }

    /**
     * Apply the current bank and program settings to the specified channel
     * 
     * @param channel The MIDI channel to apply settings to
     * @throws InvalidMidiDataException If the MIDI data is invalid
     * @throws MidiUnavailableException If the MIDI device is unavailable
     */
    public void applyBankAndProgram(int channel) throws InvalidMidiDataException, MidiUnavailableException {
        if (bankIndex != null) {
            // Send bank select MSB (CC 0)
            controlChange(0, 0);

            // Send bank select LSB (CC 32)
            controlChange(32, bankIndex);

            // Send program change if we have a preset set
            if (currentPreset != null) {
                programChange(currentPreset, 0);
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
    public void sendBulkCC(int[] controllers, int[] values) throws MidiUnavailableException {
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

    /**
     * Checks if this is an internal synth instrument
     * @return true if this is an internal synth
     */
    public boolean isInternalSynth() {
        // Safely handle null values to prevent NPE
        return Boolean.TRUE.equals(internal);
    }

    /**
     * Getter for the internal flag
     * @return the internal flag value, never null
     */
    public Boolean getInternal() {
        // Make sure we never return null
        return internal != null ? internal : Boolean.FALSE;
    }

    /**
     * Gets the Bank Select MSB (CC #0) value
     * @return MSB value (0-127)
     */
    public int getBankMSB() {
        return bankMSB;
    }

    /**
     * Sets the Bank Select MSB (CC #0) value
     * @param msb MSB value (0-127)
     */
    public void setBankMSB(int msb) {
        this.bankMSB = Math.max(0, Math.min(127, msb));
        // Update the combined index
        this.bankIndex = (bankMSB << 7) | bankLSB;
    }

    /**
     * Gets the Bank Select LSB (CC #32) value
     * @return LSB value (0-127)
     */
    public int getBankLSB() {
        return bankLSB;
    }

    /**
     * Sets the Bank Select LSB (CC #32) value
     * @param lsb LSB value (0-127)
     */
    public void setBankLSB(int lsb) {
        this.bankLSB = Math.max(0, Math.min(127, lsb));
        // Update the combined index
        this.bankIndex = (bankMSB << 7) | bankLSB;
    }
}
