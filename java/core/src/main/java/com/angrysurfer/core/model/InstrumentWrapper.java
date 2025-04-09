package com.angrysurfer.core.model;

import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Soundbank;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.feature.MidiMessage;
import com.angrysurfer.core.model.feature.Pad;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InternalSynthManager;
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

    private Boolean available = false;

    private Set<Pattern> patterns;

    // Add fields for soundbank support
    private String soundbankName;
    private Integer bankIndex;
    private Integer currentPreset;

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
        List<Integer> channels = Arrays.asList(this.channels);
        return channels.contains(channel);
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

    public void controlChange(int channel, int data1, int data2)
            throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, (int) data1, (int) data2));
    }

    public void noteOn(int channel, int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(data1 == -1 ? ShortMessage.NOTE_OFF : ShortMessage.NOTE_ON, channel, (int) data1,
                (int) data2));
    }

    public void noteOff(int channel, int data1, int data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.NOTE_OFF, channel, (int) data1, (int) data2));
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

    /**
     * Get or create a MIDI receiver with improved error handling and recovery
     */
    private synchronized Receiver getOrCreateReceiver() throws MidiUnavailableException {
        Receiver current = receiver.get();
        
        // Debug the current state
        logger.debug("getOrCreateReceiver for {}: device={}, current receiver={}", 
                     getName(),
                     (device != null ? (device.isOpen() ? "open" : "closed") : "null"),
                     (current != null ? "present" : "null"));
        
        // If we have a null receiver but the device is present and open, try to create a new receiver directly
        if (current == null && device != null && device.isOpen()) {
            try {
                // Try to get a receiver directly
                logger.info("Creating new receiver for {} as current is null", getName());
                current = device.getReceiver();
                
                if (current != null) {
                    receiver.set(current);
                    logger.info("Successfully created new receiver for {}", getName());
                    return current;
                }
            } catch (MidiUnavailableException e) {
                logger.error("Failed to create receiver for {}: {}", getName(), e.getMessage());
                // Continue to fallback path
            }
        }
        
        // If we get here, either the current receiver is valid, or we need more recovery
        boolean receiverValid = false;
        
        // Test if current receiver is still valid
        if (current != null) {
            try {
                // Try a real short message that won't affect anything audible
                current.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 120, 0), -1); // All Sound Off
                receiverValid = true;
                logger.debug("Existing receiver is valid for {}", getName());
            } catch (Exception e) {
                logger.warn("Existing receiver appears invalid for {}: {}", getName(), e.getMessage());
                receiverValid = false;
                receiver.set(null); // Clear invalid reference
                current = null;
            }
        }
        
        // Create a new receiver if current one is invalid
        if (!receiverValid) {
            // Force device reconnection logic
            MidiDevice dev = null;
            
            // First try to use the existing device if available
            if (device != null) {
                try {
                    // Ensure device is open
                    if (!device.isOpen()) {
                        device.open();
                        logger.debug("Reopened existing device for {}", getName());
                    }
                    
                    if (device.isOpen()) {
                        dev = device; // Use existing device
                    }
                } catch (Exception e) {
                    logger.warn("Error reopening existing device for {}: {}", getName(), e.getMessage());
                    // Continue to device reconnection
                }
            }
            
            // If we couldn't use the existing device, try to get a new one by name
            if (dev == null && deviceName != null && !deviceName.isEmpty()) {
                try {
                    logger.info("Trying to reconnect to device {} for {}", deviceName, getName());
                    dev = DeviceManager.getMidiDevice(deviceName);
                    
                    if (dev != null && !dev.isOpen()) {
                        dev.open();
                    }
                    
                    if (dev != null && dev.isOpen()) {
                        device = dev; // Update device reference
                        logger.info("Successfully reconnected to device {} for {}", deviceName, getName());
                    } else {
                        logger.warn("Failed to reconnect to {} - device is {} or not open", 
                                   deviceName, (dev == null ? "null" : "non-null"));
                    }
                } catch (Exception e) {
                    logger.error("Error reconnecting to device {} for {}: {}", 
                                deviceName, getName(), e.getMessage());
                }
            }
            
            // Now try to create a receiver from the device we have
            if (dev != null && dev.isOpen()) {
                try {
                    logger.info("Creating receiver from reconnected device for {}", getName());
                    current = dev.getReceiver();
                    if (current != null) {
                        receiver.set(current);
                        logger.info("Successfully created receiver from reconnected device for {}", getName());
                        
                        // Verify the receiver works
                        try {
                            current.send(new ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 120, 0), -1);
                            logger.debug("Verified new receiver is working for {}", getName());
                        } catch (Exception e) {
                            logger.error("New receiver verification failed for {}: {}", getName(), e.getMessage());
                            current = null;
                            receiver.set(null);
                        }
                    } else {
                        logger.error("Reconnected device returned null receiver for {}", getName());
                    }
                } catch (Exception e) {
                    logger.error("Error creating receiver from reconnected device for {}: {}", 
                                getName(), e.getMessage());
                }
            } else {
                logger.error("No valid device available for {} (device={}, deviceName={})",
                            getName(), (dev != null ? "non-null" : "null"), deviceName);
            }
        }
        
        // One last check for Gervill - try to create a fresh device if all else fails
        if (current == null && "Gervill".equals(deviceName)) {
            try {
                logger.info("Attempting last-resort reconnection for Gervill synth for {}", getName());
                
                // Clean up any existing synthesizers first
                DeviceManager.cleanupMidiDevices();
                
                // Get a fresh Gervill device
                MidiDevice newDevice = null;
                MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
                for (MidiDevice.Info info : infos) {
                    if (info.getName().contains("Gervill")) {
                        try {
                            newDevice = MidiSystem.getMidiDevice(info);
                            if (!newDevice.isOpen()) {
                                newDevice.open();
                            }
                            break;
                        } catch (Exception e) {
                            logger.warn("Failed to open device {}: {}", info.getName(), e.getMessage());
                        }
                    }
                }
                
                if (newDevice != null && newDevice.isOpen()) {
                    device = newDevice;
                    current = newDevice.getReceiver();
                    if (current != null) {
                        receiver.set(current);
                        logger.info("Successfully created new Gervill receiver for {}", getName());
                    }
                }
            } catch (Exception e) {
                logger.error("Last-resort Gervill connection failed for {}: {}", getName(), e.getMessage());
            }
        }
        
        if (current == null) {
            logger.error("Failed to get or create receiver for {}", getName());
        }
        
        return current;
    }

    public void sendToDevice(ShortMessage message) throws MidiUnavailableException {
        try {
            Receiver currentReceiver = getOrCreateReceiver();
            if (currentReceiver != null) {
                currentReceiver.send(message, -1);
                logger.debug("Sent message: {} to device: {}", 
                           MidiMessage.lookupCommand(message.getCommand()), getName());
            } else {
                // Emergency fallback - try to get the default receiver from MidiSystem
                logger.warn("Primary receiver null for {} - trying MidiSystem.getReceiver()", getName());
                
                try {
                    Receiver fallbackReceiver = MidiSystem.getReceiver();
                    if (fallbackReceiver != null) {
                        fallbackReceiver.send(message, -1);
                        logger.info("Successfully sent message using fallback receiver");
                        
                        // Store this receiver for future use
                        receiver.set(fallbackReceiver);
                    } else {
                        logger.error("Failed to get fallback receiver");
                    }
                } catch (Exception e) {
                    logger.error("Failed message to {} - even fallback failed: {}", getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Send failed: {} - will attempt recovery", e.getMessage());
            cleanup();
            
            // One retry attempt
            try {
                Receiver retryReceiver = getOrCreateReceiver();
                if (retryReceiver != null) {
                    retryReceiver.send(message, -1);
                    logger.info("Retry successful for {}", getName());
                } else {
                    logger.error("Failed retry message to {} - receiver still null", getName());
                }
            } catch (Exception retryEx) {
                logger.error("Failed retry for {}: {}", getName(), retryEx.getMessage());
            }
        }
    }

    public void cleanup() {
        logger.info("Cleaning up device: {}", getName());
        try {
            Receiver oldReceiver = receiver.get();
            if (oldReceiver != null) {
                receiver.set(null);
                oldReceiver.close();
            }
        } catch (Exception e) {
            logger.debug("Error closing receiver: {}", e.getMessage());
        }
    }

    boolean initialized = false;

    public void setDevice(MidiDevice device) {
        cleanup();
        this.device = device;
        try {
            if (device != null) {
                setDeviceName(device.getDeviceInfo().getName());
                if (!device.isOpen()) {
                    device.open();
                }
                receiver.set(device.getReceiver());
                logger.info("Device {} initialized successfully", getName());
            }
        } catch (MidiUnavailableException e) {
            logger.error("Failed to initialize device: {}", e.getMessage());
        }
        initialized = true;
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

    /**
     * Load a soundbank file directly and add it to InternalSynthManager
     */
    private void loadSoundbankFile() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Load Soundbank");
            fileChooser.setFileFilter(new FileNameExtensionFilter("Soundbank Files (*.sf2, *.dls)", "sf2", "dls"));
            
            // Show dialog
            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                
                // Log that we're loading the file
                logger.info("Loading soundbank file: {}", selectedFile.getAbsolutePath());
                
                // Load the soundbank directly
                try {
                    // Load the soundbank with MidiSystem
                    Soundbank soundbank = MidiSystem.getSoundbank(selectedFile);
                    
                    if (soundbank != null) {
                        // Get the soundbank name
                        String name = soundbank.getName();
                        
                        // Log success
                        logger.info("Successfully loaded soundbank: {}", name);
                        
                        // Add to InternalSynthManager's collection
                        InternalSynthManager manager = InternalSynthManager.getInstance();
                        Map<String, Soundbank> soundbanks = new HashMap<>();
                        
                        // Use reflection to access the private soundbanks map
                        try {
                            Field soundbanksField = InternalSynthManager.class.getDeclaredField("soundbanks");
                            soundbanksField.setAccessible(true);
                            soundbanks = (Map<String, Soundbank>) soundbanksField.get(manager);
                            
                            // Add the new soundbank
                            soundbanks.put(name, soundbank);
                            
                            // Update available banks for this soundbank
                            try {
                                // Get the availableBanksMap field
                                Field availableBanksMapField = InternalSynthManager.class.getDeclaredField("availableBanksMap");
                                availableBanksMapField.setAccessible(true);
                                Map<String, List<Integer>> availableBanksMap = 
                                    (Map<String, List<Integer>>) availableBanksMapField.get(manager);
                                
                                // Call determineAvailableBanks
                                Method determineAvailableBanksMethod = 
                                    InternalSynthManager.class.getDeclaredMethod("determineAvailableBanks", Soundbank.class);
                                determineAvailableBanksMethod.setAccessible(true);
                                List<Integer> banks = (List<Integer>) determineAvailableBanksMethod.invoke(manager, soundbank);
                                
                                // Store result
                                availableBanksMap.put(name, banks);
                                
                            } catch (Exception e) {
                                logger.error("Error setting available banks: {}", e.getMessage());
                                // Default to bank 0 if we can't determine available banks
                                try {
                                    Field availableBanksMapField = InternalSynthManager.class.getDeclaredField("availableBanksMap");
                                    availableBanksMapField.setAccessible(true);
                                    Map<String, List<Integer>> availableBanksMap = 
                                        (Map<String, List<Integer>>) availableBanksMapField.get(manager);
                                    
                                    List<Integer> defaultBanks = new ArrayList<>();
                                    defaultBanks.add(0);
                                    availableBanksMap.put(name, defaultBanks);
                                } catch (Exception ex) {
                                    logger.error("Error setting default bank: {}", ex.getMessage());
                                }
                            }
                        } catch (Exception e) {
                            logger.error("Error adding soundbank: {}", e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error loading soundbank file: {}", e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.error("Error in loadSoundbankFile: {}", e.getMessage());
        }
    }

    /**
     * Load a soundbank file and assign it to this instrument.
     * This uses the InternalSynthManager's public loadSoundbank(File) method.
     */
    public void loadSoundbankFileForInstrument() {
        try {
            javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
            fileChooser.setDialogTitle("Load Soundbank");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Soundbank Files (*.sf2, *.dls)", "sf2", "dls"));
            
            // Show the file chooser dialog
            int result = fileChooser.showOpenDialog(null);
            if (result == javax.swing.JFileChooser.APPROVE_OPTION) {
                java.io.File selectedFile = fileChooser.getSelectedFile();
                logger.info("Attempting to load soundbank file: {}", selectedFile.getAbsolutePath());
                
                // Call the public method from InternalSynthManager to load the soundbank
                String loadedSoundbankName = com.angrysurfer.core.service.InternalSynthManager.getInstance().loadSoundbank(selectedFile);
                if (loadedSoundbankName != null) {
                    // Update this instrument with the new soundbank name
                    setSoundbankName(loadedSoundbankName);
                    logger.info("Updated instrument {} with soundbank: {}", getName(), loadedSoundbankName);
                } else {
                    logger.error("InternalSynthManager failed to load soundbank from: {}", selectedFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            logger.error("Error loading soundbank file for instrument: {}", e.getMessage(), e);
        }
    }
}
