package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumSequencerGridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.StatusUpdate;
import com.angrysurfer.core.sequencer.StepUpdateEvent;
import com.angrysurfer.core.service.InternalSynthManager;

public class X0XPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(X0XPanel.class);

    private final List<DrumSequencerGridButton> triggerButtons = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();

    private boolean isPlaying = false;
    private Synthesizer synthesizer = null;

    // MIDI parameters for note playback only
    private int latencyCompensation = 20; // milliseconds to compensate for system latency
    private int lookAheadMs = 40; // How far ahead to schedule notes
    private boolean useAheadScheduling = true; // Enable/disable look-ahead
    private int activeMidiChannel = 15;  // Use channel 16 (15-based index) consistently

    private MelodicSequencerPanel melodicSequencerPanel;
    private DrumSequencerPanel drumSequencerPanel;
    private DrumEffectsSequencerPanel drumEffectsSequencerPanel;

    public X0XPanel() {
        super(new BorderLayout());

        // Initialize the synthesizer
        initializeSynthesizer();

        // Register with command bus
        CommandBus.getInstance().register(this);

        // Set up UI components
        setup();
    }

    private void initializeSynthesizer() {
        try {
            MidiSystem.getMidiDeviceInfo();

            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice.Info gervillInfo = null;

            for (MidiDevice.Info info : infos) {
                if (info.getName().contains("Gervill")) {
                    gervillInfo = info;
                    break;
                }
            }

            if (gervillInfo != null) {
                MidiDevice device = MidiSystem.getMidiDevice(gervillInfo);
                if (device instanceof Synthesizer) {
                    synthesizer = (Synthesizer) device;
                }
            }

            if (synthesizer == null) {
                synthesizer = MidiSystem.getSynthesizer();
            }

            if (synthesizer != null && !synthesizer.isOpen()) {
                synthesizer.open();
                logger.info("Opened synthesizer: " + synthesizer.getDeviceInfo().getName());
            }

            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[activeMidiChannel];

                if (channel != null) {
                    channel.controlChange(7, 100); // Set volume to 100
                    channel.controlChange(10, 64); // Pan center
                    channel.programChange(0); // Default program (Grand Piano)
                    logger.info("Configured channel 16 (index 15) on synthesizer");

                    String presetName = InternalSynthManager.getInstance().getPresetName(1L, 0);
                    logger.info("Initial preset: " + presetName);
                }
            }
        } catch (Exception e) {
            logger.error("Error initializing synthesizer: " + e.getMessage(), e);
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.TRANSPORT_START -> {
                isPlaying = true;
                // Sequencer handles its own state - nothing to do here
            }

            case Commands.TRANSPORT_STOP -> {
                isPlaying = false;
                // Sequencer handles its own state - nothing to do here
            }

            case Commands.SESSION_UPDATED -> {
                // Nothing to do here - sequencer handles timing updates
            }

            case Commands.ROOT_NOTE_SELECTED -> {
                if (action.getData() instanceof String) {
                    String rootNote = (String) action.getData();
                    if (melodicSequencerPanel != null) {
                        melodicSequencerPanel.getSequencer().setRootNote(rootNote);
                        melodicSequencerPanel.getSequencer().updateQuantizer();
                    }
                }
            }

            case Commands.SCALE_SELECTED -> {
                if (action.getData() instanceof String) {
                    String scaleName = (String) action.getData();
                    if (melodicSequencerPanel != null) {
                        melodicSequencerPanel.getSequencer().setScale(scaleName);
                        melodicSequencerPanel.getSequencer().updateQuantizer();
                    }
                }
            }

            // Listen for step updates from sequencer to update status display
            case Commands.SEQUENCER_STEP_UPDATE -> {
                if (action.getData() instanceof StepUpdateEvent) {
                    StepUpdateEvent stepUpdateEvent = (StepUpdateEvent) action.getData();

                    int step = stepUpdateEvent.getNewStep();
                    int patternLength = melodicSequencerPanel.getSequencer().getPatternLength();

                    // Update status display with current step
                    // CommandBus.getInstance().publish(Commands.STATUS_UPDATE, this,
                    //         new StatusUpdate("Step: " + (step + 1) + " of " + patternLength));
                }
            }
        }
    }

    private void setup() {
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.setBorder(BorderFactory.createEmptyBorder());

        JTabbedPane x0xPanel = createX0XPanel();
        containerPanel.add(new JScrollPane(x0xPanel), BorderLayout.CENTER);

        add(containerPanel);
    }

    private JTabbedPane createX0XPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Drums", createDrumPanel());
        tabbedPane.addTab("Drum Effects", createDrumEffectsPanel());
        tabbedPane.addTab("Melodic", createMelodicSequencerPanel());
        tabbedPane.addTab("Synth", createInstrumentPanel());
        return tabbedPane;
    }

    private Component createDrumPanel() {
        drumSequencerPanel = new DrumSequencerPanel(noteEvent -> {
            playDrumNote(noteEvent.getNote(), noteEvent.getVelocity());
        });
        return drumSequencerPanel;
    }

    private Component createDrumEffectsPanel() {
        drumEffectsSequencerPanel = new DrumEffectsSequencerPanel(noteEvent -> {
            playDrumNote(noteEvent.getNote(), noteEvent.getVelocity());
        });
        return drumEffectsSequencerPanel;
    }

    private Component createMelodicSequencerPanel() {
        melodicSequencerPanel = new MelodicSequencerPanel(noteEvent -> {
            // Add logging to debug
            logger.info("Playing note: {}, velocity: {}, duration: {}",
                    noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());

            // Call the playNote method
            playNote(noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());
        });

        return melodicSequencerPanel;
    }

    private Component createInstrumentPanel() {
        return new InternalSynthControlPanel(synthesizer);
    }

    public void playNote(int note, int velocity, int durationMs) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // Use the same channel consistently - very important!
                final MidiChannel channel = synthesizer.getChannels()[activeMidiChannel];

                if (channel != null) {
                    if (useAheadScheduling) {
                        new Thread(() -> {
                            try {
                                Thread.sleep(1);
                                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                                long currentTime = System.currentTimeMillis();
                                long targetTime = currentTime + lookAheadMs;
                                long waitTime = targetTime - System.currentTimeMillis();

                                if (waitTime > 0) {
                                    Thread.sleep(waitTime);
                                }

                                channel.noteOn(note, velocity);
                                Thread.sleep(durationMs);
                                channel.noteOff(note);
                            } catch (InterruptedException e) {
                                // Ignore interruptions
                            }
                        }).start();
                    } else {
                        channel.noteOn(note, velocity);

                        java.util.Timer timer = new java.util.Timer(true);
                        timer.schedule(new java.util.TimerTask() {
                            @Override
                            public void run() {
                                channel.noteOff(note);
                                timer.cancel();
                            }
                        }, durationMs);
                    }
                }
            } catch (Exception e) {
                logger.error("Error playing note: " + e.getMessage(), e);
            }
        }
    }

    public void playDrumNote(int note, int velocity) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // Use the drum channel consistently (channel 10, index 9)
                final MidiChannel channel = synthesizer.getChannels()[9];

                if (channel != null) {
                    channel.noteOn(note, velocity);
                }
            } catch (Exception e) {
                logger.error("Error playing drum note: " + e.getMessage(), e);
            }
        }
    }
}
