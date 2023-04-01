package com.angrysurfer.midi.model;

import com.angrysurfer.midi.util.MidiMessage;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import java.io.Serializable;
import java.util.*;

@Getter
@Setter
@Entity
public class MidiInstrument implements Serializable {

    static Logger logger = LoggerFactory.getLogger(MidiInstrument.class.getCanonicalName());

    static final Random rand = new Random();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false, unique = true)
    private Long id;

    @OneToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "instrument_control_code", joinColumns = {@JoinColumn(name = "instrument_id")}, inverseJoinColumns = {
            @JoinColumn(name = "control_code_id")})
    private List<ControlCode> controlCodes = new ArrayList<>();

    
    @JsonIgnore
    @ManyToMany
    @JoinTable(
            name = "instrument_pad",
            joinColumns = @JoinColumn(name = "pad_id"),
            inverseJoinColumns = @JoinColumn(name = "instrument_id"))
    private Set<Pad> pads = new HashSet<>();

    @Transient
    private Map<Integer, String> assignments = new HashMap<>();
    @Transient
    private Map<Integer, Integer[]> boundaries = new HashMap<>();
    
    @JsonIgnore
    @Transient
    private MidiDevice device;
    
    @Column(name = "name", unique = true)
    private String name;
    
    private String deviceName;

    private Integer channel;
    
    private Integer lowestNote = 0;
    
    private Integer highestNote = 127;
    
    private Integer highestPreset;
    
    private Integer preferredPreset;
    
    private Boolean hasAssignments;
    
    public MidiInstrument() {

    }

    public MidiInstrument(String name, MidiDevice device, int channel) {
        setName(Objects.isNull(name) ? device.getDeviceInfo().getName() : name);
        setDevice(device);
        setChannel(channel);
        logger.info(String.join(" ", getName(), "created on channel", Integer.toString(getChannel())));
    }

    public String assignedControl(int cc) {
        return assignments.getOrDefault(cc, "NONE");
    }

    public void channelPressure(long data1, long data2) throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.CHANNEL_PRESSURE, getChannel(), (int) data1, (int) data2));
    }

    public void controlChange(long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, getChannel(), (int) data1, (int) data2));
    }

    public void noteOn(long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(data1 == -1 ? ShortMessage.NOTE_OFF : ShortMessage.NOTE_ON, getChannel(), (int) data1, (int) data2));
    }

    public void noteOff(long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.NOTE_OFF, getChannel(), (int) data1, (int) data2));
    }

    public void polyPressure(long data1, long data2) throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.POLY_PRESSURE, getChannel(), (int) data1, (int) data2));
    }

    public void programChange(long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
        sendToDevice(new ShortMessage(ShortMessage.PROGRAM_CHANGE, getChannel(), (int) data1, (int) data2));
    }

    public void start() throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.START, getChannel(), 0, 0));
    }

    public void stop() throws MidiUnavailableException, InvalidMidiDataException {
        sendToDevice(new ShortMessage(ShortMessage.STOP, getChannel(), 1, 1));
    }

    public void randomize(List<Integer> params) {

        new Thread(() -> params.forEach(cc ->
        {
            try {
                int value = getBoundaries().containsKey(cc) ?
                        rand.nextInt(getBoundaries().get(cc)[0], getBoundaries().get(cc)[0] >= getBoundaries().get(cc)[1] ? getBoundaries().get(cc)[0] + 1 : getBoundaries().get(cc)[1]) :
                        rand.nextInt(0, 127);

                sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, getChannel(), cc, value));
            } catch (IllegalArgumentException | MidiUnavailableException | InvalidMidiDataException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })).start();
    }


    public void sendToDevice(ShortMessage message) throws MidiUnavailableException {
        logger.info(String.join(", ",
                toString(),
                MidiMessage.lookupCommand(message.getCommand()),
                message.getCommand() != ShortMessage.NOTE_ON && message.getCommand() != ShortMessage.NOTE_OFF ? lookupTarget(message.getData1()) : Integer.toString(message.getData1()),
                Integer.toString(message.getData2())));
        
            
        if (!getDevice().isOpen())
            getDevice().open();

        Receiver reciever = getDevice().getReceiver();
        reciever.send(message, new Date().getTime());
    }

    boolean initialized = false;

    public void setDevice(MidiDevice device){
        this.device = device;
        initialized = true;     
    }

    public void assign(int cc, String control) {
        getAssignments().put(cc, control);
    }


    public void setBounds(int cc, int lowerBound, int upperBound) {
        getBoundaries().put(cc, new Integer[]{lowerBound, upperBound});
    }

    public Integer getAssignmentCount() {
        return getAssignments().size();
    }

    private String lookupTarget(int key) {
        return assignments.getOrDefault(key, Integer.toString(key));
    }
}

