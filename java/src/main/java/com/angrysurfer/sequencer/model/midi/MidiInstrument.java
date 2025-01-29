// // package com.angrysurfer.sequencer.model.midi;

// import com.angrysurfer.sequencer.converter.IntegerArrayConverter;
// import com.angrysurfer.sequencer.model.Pad;
// import com.fasterxml.jackson.annotation.JsonIgnore;

// import jakarta.persistence.*;
// import lombok.Getter;
// import lombok.Setter;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import javax.sound.midi.InvalidMidiDataException;
// import javax.sound.midi.MidiDevice;
// import javax.sound.midi.MidiUnavailableException;
// import javax.sound.midi.Receiver;
// import javax.sound.midi.ShortMessage;
// import java.io.Serializable;
// import java.util.*;
// import java.util.concurrent.atomic.AtomicReference;

// @Getter
// @Setter
// @Entity
// public class MidiInstrument implements Serializable {

//     static Logger logger = LoggerFactory.getLogger(MidiInstrument.class.getCanonicalName());

//     static final Random rand = new Random();

//     @Id
//     @GeneratedValue(strategy = GenerationType.SEQUENCE)
//     @Column(name = "id", nullable = false, unique = true)
//     private Long id;

//     @OneToMany(fetch = FetchType.EAGER)
//     @JoinTable(name = "instrument_control_code", joinColumns = {@JoinColumn(name = "instrument_id")}, inverseJoinColumns = {
//             @JoinColumn(name = "control_code_id")})
//     private List<ControlCode> controlCodes = new ArrayList<>();

    
//     @ManyToMany
//     @JoinTable(
//             name = "instrument_pad",
//             joinColumns = @JoinColumn(name = "pad_id"),
//             inverseJoinColumns = @JoinColumn(name = "instrument_id"))

//     private Set<Pad> pads = new HashSet<>();

//     @Transient
//     private Map<Integer, String> assignments = new HashMap<>();

//     @Transient
//     private Map<Integer, Integer[]> boundaries = new HashMap<>();

//     @Transient
//     private Map<Integer, Map<Long, String>> captions = new HashMap<>();

//     @JsonIgnore
//     @Transient
//     private MidiDevice device;
    
//     @JsonIgnore
//     @Transient
//     private AtomicReference<Receiver> receiver = new AtomicReference<>();
    
//     @Column(name = "name", unique = true)
//     private String name;
    
//     private String deviceName;

//     @Convert(converter = IntegerArrayConverter.class)
//     @Column(name = "channels")
//     private Integer[] channels;
    
//     private Integer lowestNote = 0;
    
//     private Integer highestNote = 127;
    
//     private Integer highestPreset;
    
//     private Integer preferredPreset;
    
//     private Boolean hasAssignments;
    
//     private String playerClassName;
    
//     private Boolean available = false;

//     public MidiInstrument() {

//     }

//     public MidiInstrument(String name, MidiDevice device, int channel) {
//         setName(Objects.isNull(name) ? device.getDeviceInfo().getName() : name);
//         setDevice(device);
//         setDeviceName(device.getDeviceInfo().getName());
//         setChannels(new Integer[]{channel});
//         logger.info(String.join(" ", getName(), "created on channel", Arrays.toString(getChannels())));
//     }

//     public String assignedControl(int cc) {
//         return assignments.getOrDefault(cc, "NONE");
//     }

//     public void channelPressure(long data1, long data2) throws MidiUnavailableException, InvalidMidiDataException {
//         sendToDevice(new ShortMessage(ShortMessage.CHANNEL_PRESSURE, getChannel(), (int) data1, (int) data2));
//     }

//     public void controlChange(long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
//         sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, getChannel(), (int) data1, (int) data2));
//     }

//     public void noteOn(long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
//         sendToDevice(new ShortMessage(data1 == -1 ? ShortMessage.NOTE_OFF : ShortMessage.NOTE_ON, getChannel(), (int) data1, (int) data2));
//     }

//     public void noteOff(long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
//         sendToDevice(new ShortMessage(ShortMessage.NOTE_OFF, getChannel(), (int) data1, (int) data2));
//     }

//     public void polyPressure(long data1, long data2) throws MidiUnavailableException, InvalidMidiDataException {
//         sendToDevice(new ShortMessage(ShortMessage.POLY_PRESSURE, getChannel(), (int) data1, (int) data2));
//     }

//     public void programChange(long data1, long data2) throws InvalidMidiDataException, MidiUnavailableException {
//         sendToDevice(new ShortMessage(ShortMessage.PROGRAM_CHANGE, getChannel(), (int) data1, (int) data2));
//     }

//     public void start() throws MidiUnavailableException, InvalidMidiDataException {
//         sendToDevice(new ShortMessage(ShortMessage.START, getChannel(), 0, 0));
//     }

//     public void stop() throws MidiUnavailableException, InvalidMidiDataException {
//         sendToDevice(new ShortMessage(ShortMessage.STOP, getChannel(), 1, 1));
//     }

//     public void randomize(List<Integer> params) {

//         new Thread(() -> params.forEach(cc -> {
//             try {
//                 int value = getBoundaries().containsKey(cc) ?
//                         rand.nextInt(getBoundaries().get(cc)[0], getBoundaries().get(cc)[0] >= getBoundaries().get(cc)[1] ? getBoundaries().get(cc)[0] + 1 : getBoundaries().get(cc)[1]) :
//                         rand.nextInt(0, 127);

//                 sendToDevice(new ShortMessage(ShortMessage.CONTROL_CHANGE, getChannel(), cc, value));
//             } catch (IllegalArgumentException | MidiUnavailableException | InvalidMidiDataException e) {
//                 e.printStackTrace();
//                 throw new RuntimeException(e);
//             }
//         })).start();
//     }

//     private synchronized Receiver getOrCreateReceiver() throws MidiUnavailableException {
//         Receiver current = receiver.get();
//         if (current == null) {
//             if (!getDevice().isOpen()) {
//                 getDevice().open();
//             }
//             current = getDevice().getReceiver();
//             receiver.set(current);
//             logger.info("Created new receiver for device: {}", getName());
//         }
//         return current;
//     }

//     public void sendToDevice(ShortMessage message) throws MidiUnavailableException {
//         try {
//             Receiver currentReceiver = getOrCreateReceiver();
//             currentReceiver.send(message, -1);
//             logger.debug("Sent message: {} to device: {}", 
//                 MidiMessage.lookupCommand(message.getCommand()),
//                 getName());
//         } catch (Exception e) {
//             logger.error("Send failed: {} - will attempt recovery", e.getMessage());
//             cleanup();
//             // One retry attempt
//             getOrCreateReceiver().send(message, -1);
//         }
//     }

//     public void cleanup() {
//         logger.info("Cleaning up device: {}", getName());
//         try {
//             Receiver oldReceiver = receiver.get();
//             if (oldReceiver != null) {
//                 receiver.set(null);
//                 oldReceiver.close();
//             }
//         } catch (Exception e) {
//             logger.debug("Error closing receiver: {}", e.getMessage());
//         }
//     }

//     boolean initialized = false;

//     public void setDevice(MidiDevice device) {
//         cleanup();
//         this.device = device;
//         try {
//             if (device != null) {
//                 if (!device.isOpen()) {
//                     device.open();
//                 }
//                 receiver.set(device.getReceiver());
//                 logger.info("Device {} initialized successfully", getName());
//             }
//         } catch (MidiUnavailableException e) {
//             logger.error("Failed to initialize device: {}", e.getMessage());
//         }
//         initialized = true;
//     }

//     // Add finalizer to ensure cleanup
//     @Override
//     protected void finalize() throws Throwable {
//         cleanup();
//         super.finalize();
//     }

//     public void assign(int cc, String control) {
//         getAssignments().put(cc, control);
//     }

//     public void setBounds(int cc, int lowerBound, int upperBound) {
//         getBoundaries().put(cc, new Integer[]{lowerBound, upperBound});
//     }

//     public Integer getAssignmentCount() {
//         return getAssignments().size();
//     }

//     private String lookupTarget(int key) {
//         return assignments.getOrDefault(key, Integer.toString(key));
//     }

//     public int getChannel() {
//         return channels != null && channels.length > 0 ? channels[0] : 0;
//     }

//     public void setChannel(int channel) {
//         this.channels = new Integer[]{channel};
//     }
// }

