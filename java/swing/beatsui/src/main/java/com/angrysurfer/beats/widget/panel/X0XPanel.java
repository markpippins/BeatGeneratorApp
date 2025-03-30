package com.angrysurfer.beats.widget.panel;

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
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.TriggerButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.SessionManager;

class X0XPanel extends StatusProviderPanel implements IBusListener {
    private final List<TriggerButton> triggerButtons = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();

    private boolean isPlaying = false;
    private int currentStep = 0;

    private int stepCounter = 0; // Current step in X0X pattern (0-15)
    private int tickCounter = 0; // Count ticks within current step
    private int ticksPerStep = 6; // How many ticks make one X0X step
    private int nextStepTick = 0; // When to trigger the next step

    private Synthesizer synthesizer = null;

    private int latencyCompensation = 20; // milliseconds to compensate for system latency
    private int lookAheadMs = 40; // How far ahead to schedule notes
    private boolean useAheadScheduling = true; // Enable/disable look-ahead

    private SequencerPanel sequencerPanel;

    public X0XPanel() {
        super(new BorderLayout());
        setStatusConsumer(statusConsumer);

        // Initialize the synthesizer
        initializeSynthesizer();

        // Register with both buses
        TimingBus.getInstance().register(this);
        CommandBus.getInstance().register(this);
        setup();

        // Calculate initial timing
        updateTimingParameters();
    }

    private void updateTimingParameters() {
        try {
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null) {
                int ppq = session.getTicksPerBeat(); // Pulses per quarter note
                int stepsPerBeat = 4; // Standard X0X uses 4 steps per beat

                ticksPerStep = ppq / stepsPerBeat;
                nextStepTick = ticksPerStep; // Reset next step counter

                System.out.println("X0X timing: " + ticksPerStep + " ticks per step");
            }
        } catch (Exception ex) {
            ticksPerStep = 6;
            nextStepTick = ticksPerStep;
        }
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
                System.out.println("Opened synthesizer: " + synthesizer.getDeviceInfo().getName());
            }

            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[15];

                if (channel != null) {
                    channel.controlChange(7, 100); // Set volume to 100
                    channel.controlChange(10, 64); // Pan center
                    channel.programChange(0); // Default program (Grand Piano)
                    System.out.println("Configured channel 16 (index 15) on synthesizer");

                    String presetName = InternalSynthManager.getInstance().getPresetName(1L, 0);
                    System.out.println("Initial preset: " + presetName);
                }
            }
        } catch (Exception e) {
            System.err.println("Error initializing synthesizer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;

        switch (action.getCommand()) {
        case Commands.TRANSPORT_PLAY -> {
            isPlaying = true;

            stepCounter = 0;
            tickCounter = 0;
            nextStepTick = ticksPerStep;

            updateTimingParameters();

            SwingUtilities.invokeLater(() -> {
                for (TriggerButton button : triggerButtons) {
                    button.setHighlighted(false);
                }
                if (!triggerButtons.isEmpty()) {
                    triggerButtons.get(0).setHighlighted(true);
                }
            });
        }

        case Commands.TRANSPORT_STOP -> {
            isPlaying = false;
            resetSequence();
        }

        case Commands.TIMING_TICK -> {
            if (isPlaying && action.getData() instanceof Number) {
                tickCounter++;

                if (tickCounter >= nextStepTick) {
                    int oldStep = stepCounter;

                    stepCounter = (stepCounter + 1);

                    if (stepCounter >= sequencerPanel.getPatternLength()) {
                        if (sequencerPanel.isLooping()) {
                            stepCounter = 0;
                        } else {
                            isPlaying = false;
                            resetSequence();

                            CommandBus.getInstance().publish(Commands.TRANSPORT_STOP);
                            return;
                        }
                    }

                    SequencerPanel.NoteEvent noteEvent = sequencerPanel.updateStep(oldStep, stepCounter);

                    if (noteEvent != null) {
                        playNote(noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());
                    }

                    if (getStatusConsumer() != null) {
                        getStatusConsumer()
                                .setStatus("Step: " + (stepCounter + 1) + " of " + sequencerPanel.getPatternLength());
                    }

                    tickCounter = 0;
                    nextStepTick = ticksPerStep;
                }
            }
        }

        case Commands.SESSION_UPDATED -> {
            if (action.getData() instanceof Session) {
                updateTimingParameters();
            }
        }
        }
    }

    private void resetSequence() {
        stepCounter = 0;
        if (sequencerPanel != null) {
            sequencerPanel.reset();
        }
    }

    private void setup() {
        JPanel containerPanel = new JPanel(new BorderLayout(10, 10));
        containerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JTabbedPane x0xPanel = createX0XPanel();
        containerPanel.add(new JScrollPane(x0xPanel), BorderLayout.CENTER);

        add(containerPanel);
    }

    private JTabbedPane createX0XPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Sequence", createSequencerPanel());
        tabbedPane.addTab("Instrument", createInstrumentPanel());
        tabbedPane.addTab("Effects", createEffectsPanel());
        tabbedPane.addTab("Performance", createPerformancePanel());

        return tabbedPane;
    }

    private Component createSequencerPanel() {
        sequencerPanel = new SequencerPanel(noteEvent -> {
            playNote(noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());
        });

        return sequencerPanel;
    }

    private Component createEffectsPanel() {
        return null;
    }

    private Component createPerformancePanel() {
        return null;
    }

    private Component createInstrumentPanel() {
        return new InternalSynthControlPanel(synthesizer);
    }

    private void playNote(int note, int velocity, int durationMs) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                final MidiChannel channel = synthesizer.getChannels()[15];

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
                System.err.println("Error playing note: " + e.getMessage());
            }
        }
    }
}
