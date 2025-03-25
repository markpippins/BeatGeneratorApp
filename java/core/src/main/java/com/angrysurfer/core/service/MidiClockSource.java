package com.angrysurfer.core.service;

import java.util.Objects;
import java.util.logging.Logger;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Track;
import javax.sound.midi.Transmitter;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MidiClockSource implements IBusListener {

    private static final Logger logger = Logger.getLogger(MidiClockSource.class.getName());

    private boolean metronomeAudible = false;
    private int metronomeChannel = 9;
    private int metronomeNote = 60;
    private int metronomeVelocity = 100;

    // Use singletons directly instead of constructor injection
    // private final LogManager logManager = LogManager.getInstance();
    private final TimingBus timingBus = TimingBus.getInstance();
    private final CommandBus commandBus = CommandBus.getInstance();

    private Sequence sequence;
    private Sequencer sequencer;
    private Synthesizer synthesizer;

    static boolean isInitialized = false;

    private synchronized void initialize() {
        // if (!isInitialized)
            try {
                // System.out.println("SequencerManager: Initializing...");
                setupSequencer();
                // System.out.println("SequencerManager: Sequencer setup complete");
                setupSynthesizer();
                // System.out.println("SequencerManager: Synthesizer setup complete");
                createSequence();
                // System.out.println("SequencerManager: Sequence created");
                connectDevices();
                // System.out.println("SequencerManager: Devices connected");
            } catch (Exception e) {
                System.err.println("SequencerManager: Error initializing MIDI: " + e.getMessage());
                e.printStackTrace();
            }

        CommandBus.getInstance().register(this);
        // System.out.println("SequencerManager: Initialization complete");
        // isInitialized = true;
    }

    // Optimize buffer size and latency settings
    private void setupSequencer() throws MidiUnavailableException {
        // System.out.println("SequencerManager: Setting up sequencer...");
        sequencer = MidiSystem.getSequencer(false);
        if (sequencer == null) {
            throw new MidiUnavailableException("Could not obtain MIDI sequencer");
        }
        
        // Configure sequencer for low latency before opening
        try {
            // Set system properties for better timing - works with most JVM implementations
            System.setProperty("javax.sound.midi.Sequencer#RealTimeSequencing", "true");
            System.setProperty("javax.sound.midi.Sequencer#Latency", "1");
        } catch (Exception e) {
            System.err.println("Could not set sequencer properties: " + e.getMessage());
        }
        
        sequencer.open();
        
        // Try to optimize the sequencer after opening
        try {
            // These are general properties that might work across implementations
            // if (sequencer instanceof javax.sound.midi.RealTimeSequencer) {
            //     // System.out.println("Using real-time sequencer");
            // }
            
            // Additional sequencer tuning
            sequencer.setMicrosecondPosition(0);
            sequencer.setTickPosition(0);
        } catch (Exception e) {
            System.err.println("Warning: Could not optimize sequencer: " + e.getMessage());
        }
        
        // System.out.println("SequencerManager: Sequencer opened successfully");
    }

    private void setupSynthesizer() throws MidiUnavailableException {
        synthesizer = MidiSystem.getSynthesizer();
        synthesizer.open();
        synthesizer.getChannels()[metronomeChannel].setMute(!metronomeAudible);
    }

    // Modified createSequence to support dynamic parameters
    private void createSequence() throws InvalidMidiDataException {
        createSequence(4); // Default 4/4 time
    }

    private void createSequence(int beatsPerBar) throws InvalidMidiDataException {
        sequence = new Sequence(Sequence.PPQ, getActiveSession().getTicksPerBeat());
        Track track = sequence.createTrack();

        // Add events for one bar with configurable beats per bar
        for (int beat = 0; beat < beatsPerBar; beat++) {
            // Add timing clocks
            for (int clock = 0; clock < getActiveSession().getTicksPerBeat(); clock++) {
                track.add(new MidiEvent(new ShortMessage(0xF8), beat * getActiveSession().getTicksPerBeat() + clock));
            }

            // Add metronome notes
            track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_ON, metronomeChannel, metronomeNote, metronomeVelocity),
                    beat * getActiveSession().getTicksPerBeat()));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, metronomeChannel, metronomeNote, 0),
                    beat * getActiveSession().getTicksPerBeat() + getActiveSession().getTicksPerBeat() / 2));
        }

        sequencer.setSequence(sequence);
        sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        sequencer.setTempoInBPM(getActiveSession().getTempoInBPM());
        sequencer.setMasterSyncMode(Sequencer.SyncMode.MIDI_SYNC);
        sequencer.setSlaveSyncMode(Sequencer.SyncMode.MIDI_SYNC);
    }

    // Optimize MIDI clock message handling
    private void connectDevices() throws MidiUnavailableException {
        // Create and connect timing receiver with higher priority
        Receiver timingReceiver = new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof ShortMessage msg) {
                    if (msg.getStatus() == 0xF8) {
                        Session activeSession = getActiveSession();
                        // Directly call onTick first to update the tick counter
                        if (activeSession != null) {
                            activeSession.onTick();
                        }
                    }
                }
            }

            @Override
            public void close() {
            }
        };

        // Connect timing and audio
        Transmitter timingTransmitter = sequencer.getTransmitter();
        timingTransmitter.setReceiver(timingReceiver);

        Transmitter audioTransmitter = sequencer.getTransmitter();
        audioTransmitter.setReceiver(synthesizer.getReceiver());
    }

    public void startSequence() {

        initialize();

        try {
            // System.out.println("SequencerManager: Attempting to start sequencer");
            if (sequencer != null && !sequencer.isRunning()) {
                // System.out.println("SequencerManager: Sequencer exists and is not running");
                // Important: Make sure any active session is properly initialized
                if (getActiveSession() != null) {
                    // System.out.println("SequencerManager: Active session found: " + getActiveSession().getId());
                    getActiveSession().beforeStart();
                    // System.out.println("SequencerManager: Called session.beforeStart()");
                } else {
                    // System.out.println("SequencerManager: No active session found!");
                }

                // Start sequencer
                sequencer.start();
                // System.out.println("SequencerManager: Sequencer started");

                // Publish state change - CRITICAL for UI updates
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
                // System.out.println("SequencerManager: Published TRANSPORT_STATE_CHANGED event");
            } else {
                // System.out.println("SequencerManager: Cannot start - sequencer is null or already running");
                if (sequencer == null) {
                    // System.out.println("SequencerManager: Sequencer is null!");
                } else {
                    // System.out.println("SequencerManager: Sequencer is already running!");
                }
            }
        } catch (Exception e) {
            System.err.println("SequencerManager: Error starting sequencer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {

        try {
            if (sequencer != null) {
                sequencer.stop();
                sequencer.setMicrosecondPosition(0);

                // Publish state change - CRITICAL for UI updates
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, false);
                // System.out.println("SequencerManager: Stopped sequencer, publishing state change event");
            }
        } catch (Exception e) {
            logger.warning("Error stopping sequencer: " + e.getMessage());
        }
    }

    public void cleanup() {
        try {
            // Unregister from timing bus
            timingBus.unregister(this);

            // Clean up sequencer
            // SequencerManager.getInstance().cleanup();
            if (sequencer != null) {
                sequencer.stop();
                sequencer.close();
            }
            if (synthesizer != null) {
                synthesizer.close();
            }
            // logManager.info("MIDI resources cleaned up");
        } catch (Exception e) {
            logger.warning("Error cleaning up MIDI resources: " + e.getMessage());
        }
    }

    // Add state query methods
    public boolean isRunning() {
        return sequencer != null && sequencer.isRunning();
    }

    public void updateTimingParameters(float tempoInBPM, int ticksPerBeat, int beatsPerBar) {
        boolean wasRunning = isRunning();

        // If running, stop temporarily
        if (wasRunning) {
            sequencer.stop();
        }

        try {
            // Update sequencer parameters
            sequencer.setTempoInBPM(tempoInBPM);
            createSequence(beatsPerBar);

            // Restart if it was running
            if (wasRunning) {
                sequencer.start();
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    // Add getter methods that checkgetActiveSession() first

    private Session getActiveSession() {
        // Replace with actual session retrieval logic
        return SessionManager.getInstance().getActiveSession();
    }

    private void updateSequencerSettings() {
        if (getActiveSession() == null)
            return;

        boolean wasRunning = isRunning();

        // If running, stop temporarily
        if (wasRunning) {
            sequencer.stop();
        }

        try {
            // Update sequencer parameters from session
            sequencer.setTempoInBPM(getActiveSession().getTempoInBPM());

            // Recreate sequence if PPQ changed
            createSequence(getActiveSession().getBeatsPerBar());

            // Restart if it was running
            if (wasRunning) {
                sequencer.start();
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == Commands.SESSION_SELECTED) {
            updateSequencerSettings();
        }
        if (action.getCommand() == Commands.METRONOME_START) {
            setMetronomeAudible(true);
            if ((Objects.nonNull(synthesizer)) && (synthesizer.isOpen())) {
                synthesizer.getChannels()[metronomeChannel].setMute(false);
            }
        } else if (action.getCommand() == Commands.METRONOME_STOP) {
            setMetronomeAudible(false);
            if ((Objects.nonNull(synthesizer)) && (synthesizer.isOpen())) {
                synthesizer.getChannels()[metronomeChannel].setMute(true);
            }
        }
    }
}