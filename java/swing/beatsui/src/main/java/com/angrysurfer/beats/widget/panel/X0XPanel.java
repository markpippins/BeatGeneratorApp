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

public class X0XPanel extends StatusProviderPanel implements IBusListener {
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

    private boolean patternCompleted = false; // Flag for when pattern has completed but transport continues

    private int activeMidiChannel = 15;  // Use channel 16 (15-based index) consistently

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
                
                // Get the steps per beat from the timing division
                int stepsPerBeat = 4; // Default
                if (sequencerPanel != null) {
                    SequencerPanel.TimingDivision timingDivision = sequencerPanel.getTimingDivision();
                    stepsPerBeat = timingDivision.getStepsPerBeat();
                }

                ticksPerStep = ppq / stepsPerBeat;
                nextStepTick = ticksPerStep; // Reset next step counter

                System.out.println("X0X timing updated: " + ticksPerStep + " ticks per step, " + 
                                  stepsPerBeat + " steps per beat");
                
                // For triplet timing, adjust ticksPerStep to be 2/3 of normal step length
                if (stepsPerBeat == 3 || stepsPerBeat == 6 || stepsPerBeat == 12) {
                    // For triplets, we need to multiply by 2/3 to get the correct timing
                    ticksPerStep = (int) Math.round(ticksPerStep * (4.0 / stepsPerBeat) * (2.0 / 3.0));
                    System.out.println("Triplet timing: adjusted to " + ticksPerStep + " ticks per step");
                }
            }
        } catch (Exception ex) {
            ticksPerStep = 6;
            nextStepTick = ticksPerStep;
            System.err.println("Error updating timing parameters: " + ex.getMessage());
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
                MidiChannel channel = synthesizer.getChannels()[activeMidiChannel];

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
            patternCompleted = false;

            stepCounter = 0;
            tickCounter = 0;
            nextStepTick = ticksPerStep;

            updateTimingParameters();

            SwingUtilities.invokeLater(() -> {
                if (sequencerPanel != null) {
                    sequencerPanel.reset();
                    // Highlight first step
                    sequencerPanel.updateStep(-1, 0);
                }
            });
        }

        case Commands.TRANSPORT_STOP -> {
            isPlaying = false;
            patternCompleted = false;
            resetSequence();
        }
        
        case Commands.TIMING_BEAT -> {
            // If pattern has completed and we're looping, restart on beat
            if (isPlaying && patternCompleted && sequencerPanel != null && sequencerPanel.isLooping()) {
                stepCounter = 0;
                patternCompleted = false;
                tickCounter = 0;
                nextStepTick = ticksPerStep;
                
                // Update UI to show first step is active
                int oldStep = -1; // No previous step to unhighlight
                sequencerPanel.updateStep(oldStep, stepCounter);
                
                // Update status display
                if (getStatusConsumer() != null) {
                    getStatusConsumer().setStatus("Step: " + (stepCounter + 1) + " of " + sequencerPanel.getPatternLength());
                }
            }
        }

        case Commands.TIMING_TICK -> {
            if (isPlaying && !patternCompleted && action.getData() instanceof Number) {
                tickCounter++;

                if (tickCounter >= nextStepTick) {
                    int oldStep = stepCounter;
                    int patternLength = sequencerPanel.getPatternLength();
                    SequencerPanel.Direction direction = sequencerPanel.getCurrentDirection();
                    
                    // Calculate the next step based on direction
                    int nextStep = calculateNextStep(stepCounter, patternLength, direction);
                    
                    // Check if we've completed a full pattern
                    boolean patternEnded = hasPatternEnded(stepCounter, nextStep, patternLength, direction);
                    
                    if (patternEnded) {
                        // Mark pattern as completed - will restart on next TIMING_BEAT if looping
                        patternCompleted = true;
                        
                        // Reset tick counter for next step timing
                        tickCounter = 0;
                        nextStepTick = ticksPerStep;
                        
                        // Update UI for current step before stopping
                        SequencerPanel.NoteEvent noteEvent = sequencerPanel.updateStep(oldStep, stepCounter);
                        
                        // If there's a note to play, play it (final step still plays its note)
                        if (noteEvent != null) {
                            playNote(noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());
                        }
                        
                        // Update status display
                        if (getStatusConsumer() != null) {
                            getStatusConsumer().setStatus("Step: " + (stepCounter + 1) + " of " + patternLength + 
                                                         (sequencerPanel.isLooping() ? " (waiting for beat)" : " (end)"));
                        }
                        
                        // We're done with this tick - wait for TIMING_BEAT to restart if looping
                        return;
                    }
                    
                    // Normal case - continue pattern
                    stepCounter = nextStep;
                    
                    // Update UI and get note event if needed
                    SequencerPanel.NoteEvent noteEvent = sequencerPanel.updateStep(oldStep, stepCounter);
                    
                    // If there's a note to play, play it
                    if (noteEvent != null) {
                        playNote(noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());
                    }
                    
                    // Update status display
                    if (getStatusConsumer() != null) {
                        getStatusConsumer().setStatus("Step: " + (stepCounter + 1) + " of " + patternLength);
                    }

                    // Reset tick counter and calculate next step time
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

        case Commands.ROOT_NOTE_SELECTED -> {
            if (action.getData() instanceof String) {
                String rootNote = (String) action.getData();
                if (sequencerPanel != null) {
                    sequencerPanel.setRootNote(rootNote);
                }
            }
        }

        case Commands.SCALE_SELECTED -> {
            if (action.getData() instanceof String) {
                String scaleName = (String) action.getData();
                if (sequencerPanel != null) {
                    sequencerPanel.setSelectedScale(scaleName);
                }
            }
        }
        }
    }

    private int calculateNextStep(int currentStep, int patternLength, SequencerPanel.Direction direction) {
        switch (direction) {
            case FORWARD:
                return (currentStep + 1) % patternLength;
                
            case BACKWARD:
                return (currentStep - 1 + patternLength) % patternLength;
                
            case BOUNCE:
                boolean forward = sequencerPanel.isBounceForward();
                int nextStep;
                
                if (forward) {
                    nextStep = currentStep + 1;
                    // If we hit the end, change direction
                    if (nextStep >= patternLength - 1) {
                        sequencerPanel.setBounceForward(false);
                    }
                } else {
                    nextStep = currentStep - 1;
                    // If we hit the beginning, change direction
                    if (nextStep <= 0) {
                        sequencerPanel.setBounceForward(true);
                        nextStep = 0; // Ensure we don't go below 0
                    }
                }
                return nextStep;
                
            case RANDOM:
                // Generate a random step that's different from current
                int next;
                do {
                    next = (int)(Math.random() * patternLength);
                } while (patternLength > 1 && next == currentStep);
                return next;
                
            default:
                return (currentStep + 1) % patternLength; // Default to forward
        }
    }

    private boolean hasPatternEnded(int currentStep, int nextStep, int patternLength, SequencerPanel.Direction direction) {
        if (!sequencerPanel.isLooping()) {
            // In non-looping mode, check for ending conditions based on direction
            switch (direction) {
                case FORWARD:
                    return currentStep == patternLength - 1;
                    
                case BACKWARD:
                    return currentStep == 0;
                    
                case BOUNCE:
                    // For bounce, we end at either extreme if not looping
                    return (currentStep == 0 && !sequencerPanel.isBounceForward()) || 
                           (currentStep == patternLength - 1 && sequencerPanel.isBounceForward());
                    
                case RANDOM:
                    // For random, we play exactly patternLength steps before ending
                    // This would require a step counter that we don't have
                    // So for simplicity, we'll end after visiting the last step in the pattern
                    return currentStep == patternLength - 1;
            }
        }
        
        // In looping mode, pattern never ends
        return false;
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

        tabbedPane.addTab("Sequencer", createSequencerPanel());
        tabbedPane.addTab("Synth", createInstrumentPanel());

        return tabbedPane;
    }

    private Component createSequencerPanel() {
        sequencerPanel = new SequencerPanel(noteEvent -> {
            playNote(noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());
        });
        
        // Listen for timing division changes
        sequencerPanel.setTimingChangeListener(timingDivision -> {
            updateTimingParameters();
        });

        return sequencerPanel;
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
                e.printStackTrace();
            }
        }
    }
}
