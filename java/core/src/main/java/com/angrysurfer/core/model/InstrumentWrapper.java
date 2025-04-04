package com.angrysurfer.core.model;

import java.io.Serializable;
import java.io.File;
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
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.util.IntegerArrayConverter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentWrapper implements Serializable {

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
    private Long currentPreset;

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

    public void channelPressure(int channel, long data1, long data2)
            throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.CHANNEL_PRESSURE, channel, (int) data1, (int) data2));
    }

    public void controlChange(int channel, long data1, long data2)
            throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, channel, (int) data1, (int) data2));
    }

    public void noteOn(int channel, long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(data1 == -1 ? ShortMessage.NOTE_OFF : ShortMessage.NOTE_ON, channel, (int) data1,
                (int) data2));
    }

    public void noteOff(int channel, long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.NOTE_OFF, channel, (int) data1, (int) data2));
    }

    public void polyPressure(int channel, long data1, long data2)
            throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.POLY_PRESSURE, channel, (int) data1, (int) data2));
    }

    public void programChange(int channel, long data1, long data2)
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

    private synchronized Receiver getOrCreateReceiver() throws MidiUnavailableException {
        Receiver current = receiver.get();
        if (current == null && Objects.nonNull(getDevice())) {
            if (!getDevice().isOpen()) {
                getDevice().open();
            }
            current = getDevice().getReceiver();
            receiver.set(current);
            logger.info("Created new receiver for device: {}", getName());
        }
        return current;
    }

    public void sendToDevice(ShortMessage message) throws MidiUnavailableException {
        try {
            Receiver currentReceiver = getOrCreateReceiver();
            if (Objects.nonNull(currentReceiver)) {
                currentReceiver.send(message, -1);
                logger.debug("Sent message: {} to device: {}", MidiMessage.lookupCommand(message.getCommand()),
                        getName());
            } else
                logger.error("Failed message to {}", getName());
        } catch (Exception e) {
            logger.error("Send failed: {} - will attempt recovery", e.getMessage());
            cleanup();
            // One retry attempt
            Receiver receiver = getOrCreateReceiver();
            if (Objects.nonNull(receiver))
                receiver.send(message, -1);
            else
                logger.error("Failed retry message to {}", getName());
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
    public Long getCurrentPreset() {
        return currentPreset;
    }

    /**
     * Set the currently selected preset
     * @param preset The preset number
     */
    public void setCurrentPreset(Long preset) {
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
