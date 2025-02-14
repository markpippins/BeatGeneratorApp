package com.angrysurfer.beatsui.proxy;

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
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.converter.IntegerArrayConverter;
import com.angrysurfer.core.model.midi.MidiMessage;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "instrument")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class ProxyInstrument implements Serializable {

    static Logger logger = LoggerFactory.getLogger(ProxyInstrument.class.getCanonicalName());

    static final Random rand = new Random();

    public static final Integer DEFAULT_CHANNEL = 0;

    public static final Integer[] DEFAULT_CHANNELS = new Integer[] { DEFAULT_CHANNEL };

    public static final Integer[] ALL_CHANNELS = new Integer[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 };

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "instrument_control_code", joinColumns = {
            @JoinColumn(name = "instrument_id") }, inverseJoinColumns = {
                    @JoinColumn(name = "control_code_id") })
    private List<ProxyControlCode> controlCodes = new ArrayList<>();

    @ManyToMany
    @JoinTable(name = "instrument_pad", joinColumns = @JoinColumn(name = "instrument_id"), inverseJoinColumns = @JoinColumn(name = "pad_id"))
    private Set<ProxyPad> pads = new HashSet<>();

    @Transient
    private Map<Integer, String> assignments = new HashMap<>();

    @Transient
    private Map<Integer, Integer[]> boundaries = new HashMap<>();

    // @Transient
    // private Map<Integer, Map<Long, String>> captions = new HashMap<>();

    @JsonIgnore
    @Transient
    private MidiDevice device;

    @JsonIgnore
    @Transient
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

    // private Set<Pattern> patterns;

    public ProxyInstrument() {

    }

    public ProxyInstrument(String name, MidiDevice device) {
        this(name, device, DEFAULT_CHANNELS);
    }

    public ProxyInstrument(String name, MidiDevice device, int channel) {
        this(name, device, new Integer[] { channel });
    }

    public ProxyInstrument(String name, MidiDevice device, Integer[] channels) {
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
                logger.debug("Sent message: {} to device: {}",
                        MidiMessage.lookupCommand(message.getCommand()),
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
    // @Override
    // protected void finalize() throws Throwable {
    //     cleanup();
    //     super.finalize();
    // }

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

    @Override
    public String toString() {
        return getName(); // This will be displayed in the combo box
    }
}
