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
public class SequencerManager implements IBusListener {

    private static final Logger logger = Logger.getLogger(SequencerManager.class.getName());

    private static SequencerManager instance;

    private boolean metronomeAudible = false;

    // Add timing state tracking
    private long currentTick = 0;
    private double currentBeat = 0.0;
    private long currentBar = 0;

    // Reference to current active session instead of duplicating parameters
    private Session activeSession = null;

    // These remain for initial/default values only
    private int defaultPpq = 24;
    private float defaultBpm = 120;
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

    // Private constructor for singleton
    private SequencerManager() {
        initialize();
    }

    // Add static accessor
    public static SequencerManager getInstance() {
        if (instance == null) {
            instance = new SequencerManager();
        }
        return instance;
    }

    private void initialize() {
        try {
            System.out.println("SequencerManager: Initializing...");
            setupSequencer();
            System.out.println("SequencerManager: Sequencer setup complete");
            setupSynthesizer();
            System.out.println("SequencerManager: Synthesizer setup complete");
            createSequence();
            System.out.println("SequencerManager: Sequence created");
            connectDevices();
            System.out.println("SequencerManager: Devices connected");
        } catch (Exception e) {
            System.err.println("SequencerManager: Error initializing MIDI: " + e.getMessage());
            e.printStackTrace();
        }

        CommandBus.getInstance().register(this);
        System.out.println("SequencerManager: Initialization complete");
    }

    private void setupSequencer() throws MidiUnavailableException {
        System.out.println("SequencerManager: Setting up sequencer...");
        sequencer = MidiSystem.getSequencer(false);
        if (sequencer == null) {
            throw new MidiUnavailableException("Could not obtain MIDI sequencer");
        }
        System.out.println("SequencerManager: Got sequencer: " + sequencer.getDeviceInfo().getName());
        sequencer.open();
        System.out.println("SequencerManager: Sequencer opened successfully");
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
        sequence = new Sequence(Sequence.PPQ, getPpq());
        Track track = sequence.createTrack();

        // Add events for one bar with configurable beats per bar
        for (int beat = 0; beat < beatsPerBar; beat++) {
            // Add timing clocks
            for (int clock = 0; clock < getPpq(); clock++) {
                track.add(new MidiEvent(new ShortMessage(0xF8), beat * getPpq() + clock));
            }

            // Add metronome notes
            track.add(new MidiEvent(
                    new ShortMessage(ShortMessage.NOTE_ON, metronomeChannel, metronomeNote, metronomeVelocity),
                    beat * getPpq()));
            track.add(new MidiEvent(new ShortMessage(ShortMessage.NOTE_OFF, metronomeChannel, metronomeNote, 0),
                    beat * getPpq() + getPpq() / 2));
        }

        sequencer.setSequence(sequence);
        sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
        sequencer.setTempoInBPM(getBpm());
        sequencer.setMasterSyncMode(Sequencer.SyncMode.MIDI_SYNC);
        sequencer.setSlaveSyncMode(Sequencer.SyncMode.MIDI_SYNC);
    }

    private void connectDevices() throws MidiUnavailableException {
        // Create and connect timing receiver
        Receiver timingReceiver = new Receiver() {
            @Override
            public void send(MidiMessage message, long timeStamp) {
                if (message instanceof ShortMessage msg) {
                    if (msg.getStatus() == 0xF8) {
                        handleMidiClock();
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

    // In SequencerManager.java - fix handleMidiClock() method
    private void handleMidiClock() {
        // Increment tick counter for this clock pulse
        currentTick++;

        // Calculate beat position based on PPQ
        int ppq = getPpq();
        double beat = (double) currentTick / ppq;

        // Output consolidated timing info
        System.out.println("TIMING: tick=" + currentTick + ", beat=" + beat + ", bar=" + currentBar + ", ppq=" + ppq);

        // Update our active session's cycler values if available
        if (activeSession != null) {
            try {
                // CRITICAL FIX: Advance the session's cyclers
                // This is what was missing in your code
                activeSession.getTickCycler().advance();

                // Check if we need to advance the beat cycler (every PPQ ticks)
                if (currentTick % ppq == 0) {
                    activeSession.getBeatCycler().advance();

                    // Check if we need to advance the bar cycler (every beatsPerBar beats)
                    int beatsPerBar = getBeatsPerBar();
                    if (currentBeat % beatsPerBar == 0 && currentBeat > 0) {
                        activeSession.getBarCycler().advance();

                        // Optionally advance part cycler if appropriate
                        // (implement your part advancement logic here)
                    }
                }

                System.out
                        .println("Session cyclers updated: " + "tick=" + activeSession.getTickCycler().get() + ", beat="
                                + activeSession.getBeatCycler().get() + ", bar=" + activeSession.getBarCycler().get());

            } catch (Exception e) {
                System.err.println("Error updating session cyclers: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // After updating cyclers, publish timing events
        timingBus.publish(Commands.TIMING_TICK, this, currentTick);

        // On beat boundaries
        if (currentTick % ppq == 0) {
            currentBeat++;
            // Also publish TIMING_BEAT events
            timingBus.publish(Commands.TIMING_BEAT, this, currentBeat);

            // On bar boundaries
            int beatsPerBar = getBeatsPerBar();
            if (currentBeat % beatsPerBar == 0) {
                currentBar++;
                // Also publish TIMING_BAR events
                timingBus.publish(Commands.TIMING_BAR, this, currentBar);
            }
        }
    }

    public void start() {
        try {
            System.out.println("SequencerManager: Attempting to start sequencer");
            if (sequencer != null && !sequencer.isRunning()) {
                System.out.println("SequencerManager: Sequencer exists and is not running");

                // Reset counters
                currentTick = 0;
                currentBeat = 0;
                currentBar = 0;

                // Important: Make sure any active session is properly initialized
                if (activeSession != null) {
                    System.out.println("SequencerManager: Active session found: " + activeSession.getId());
                    activeSession.beforeStart();
                    System.out.println("SequencerManager: Called session.beforeStart()");
                } else {
                    System.out.println("SequencerManager: No active session found!");
                }

                // Start sequencer
                sequencer.start();
                System.out.println("SequencerManager: Sequencer started");

                // Publish state change - CRITICAL for UI updates
                commandBus.publish(Commands.TRANSPORT_STATE_CHANGED, this, true);
                System.out.println("SequencerManager: Published TRANSPORT_STATE_CHANGED event");
            } else {
                System.out.println("SequencerManager: Cannot start - sequencer is null or already running");
                if (sequencer == null) {
                    System.out.println("SequencerManager: Sequencer is null!");
                } else {
                    System.out.println("SequencerManager: Sequencer is already running!");
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
                System.out.println("SequencerManager: Stopped sequencer, publishing state change event");

                if (activeSession != null) {
                    activeSession.onStop();
                }
            }
        } catch (Exception e) {
            // logger.warning(String.format"Error stopping sequencer: {}", e.getMessage(), e);
        }
    }

    public void cleanup() {
        try {
            // Unregister from timing bus
            timingBus.unregister(this);

            // Clean up sequencer
            SequencerManager.getInstance().cleanup();
            if (sequencer != null) {
                sequencer.stop();
                sequencer.close();
            }
            if (synthesizer != null) {
                synthesizer.close();
            }
            // logManager.info("MIDI resources cleaned up");
        } catch (Exception e) {
            // logManager.error("Error cleaning up MIDI resources: " + e.getMessage());
        }
    }

    // Add state query methods
    public boolean isRunning() {
        return sequencer != null && sequencer.isRunning();
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public double getCurrentBeat() {
        return currentBeat;
    }

    public long getCurrentBar() {
        return currentBar;
    }

    // Add a method to update timing parameters
    public void updateTimingParameters(float tempoInBPM, int ticksPerBeat, int beatsPerBar) {
        boolean wasRunning = isRunning();

        // If running, stop temporarily
        if (wasRunning) {
            sequencer.stop();
        }

        try {
            // Update our internal values
            this.defaultBpm = tempoInBPM;
            this.defaultPpq = ticksPerBeat;

            // Update sequencer parameters
            sequencer.setTempoInBPM(tempoInBPM);

            // Recreate the sequence with the new PPQ value if it changed
            createSequence(beatsPerBar);

            // Restart if it was running
            if (wasRunning) {
                sequencer.start();
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    // Add getter methods that check activeSession first
    public int getPpq() {
        return (activeSession != null) ? activeSession.getTicksPerBeat() : defaultPpq;
    }

    public float getBpm() {
        return (activeSession != null) ? activeSession.getTempoInBPM() : defaultBpm;
    }

    public int getBeatsPerBar() {
        return (activeSession != null) ? activeSession.getBeatsPerBar() : 4;
    }

    // Update the method to set the active session
    public void setActiveSession(Session session) {
        this.activeSession = session;
        if (session != null) {
            // Apply settings from session
            updateSequencerSettings();
        }
    }

    // Method to apply current session settings to sequencer
    private void updateSequencerSettings() {
        if (activeSession == null)
            return;

        boolean wasRunning = isRunning();

        // If running, stop temporarily
        if (wasRunning) {
            sequencer.stop();
        }

        try {
            // Update sequencer parameters from session
            sequencer.setTempoInBPM(activeSession.getTempoInBPM());

            // Recreate sequence if PPQ changed
            createSequence(activeSession.getBeatsPerBar());

            // Restart if it was running
            if (wasRunning) {
                sequencer.start();
            }
        } catch (Exception e) {
            // Handle error
        }
    }

    // Replace syncFromSession with setActiveSession
    public void syncFromSession(Session session) {
        setActiveSession(session);
    }

    @Override
    public void onAction(Command action) {
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