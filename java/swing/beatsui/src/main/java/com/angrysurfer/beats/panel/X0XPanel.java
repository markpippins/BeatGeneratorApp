package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.StepUpdateEvent;
import com.angrysurfer.core.service.InternalSynthManager;

public class X0XPanel extends JPanel implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(X0XPanel.class);

    // Add this field to store the tabbedPane reference
    private JTabbedPane tabbedPane;
    
    // Existing fields
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();
    private boolean isPlaying = false;
    private Synthesizer synthesizer = null;
    private int latencyCompensation = 20;
    private int lookAheadMs = 40;
    private boolean useAheadScheduling = true;
    private int activeMidiChannel = 15;
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

        // Store reference to tabbedPane as class field
        tabbedPane = createX0XPanel();
        containerPanel.add(tabbedPane, BorderLayout.CENTER);

        add(containerPanel);
    }

    private JTabbedPane createX0XPanel() {
        JTabbedPane tabbedPane = new JTabbedPane();

        // Add tabs
        tabbedPane.addTab("Poly Drum", createDrumPanel());
        tabbedPane.addTab("Poly Effects", createDrumEffectsPanel());
        tabbedPane.addTab("Mono 1", createMelodicSequencerPanel(3));
        tabbedPane.addTab("Mono 2", createMelodicSequencerPanel(4));
        tabbedPane.addTab("Mono 3", createMelodicSequencerPanel(5));
        tabbedPane.addTab("Mono 4", createMelodicSequencerPanel(6));
        tabbedPane.addTab("Synth", createInstrumentPanel());
        tabbedPane.addTab("Chords", createChordSequencerPanel());
        tabbedPane.addTab("Mixer", createMixerPanel());
        
        // Add trailing toolbar with mute buttons to the tabbed pane
        JPanel tabToolbar = createMuteButtonsToolbar();
        tabbedPane.putClientProperty("JTabbedPane.trailingComponent", tabToolbar);

        return tabbedPane;
    }

    /**
     * Creates a toolbar with mute buttons for drum pads and melodic sequencers
     * @return A JPanel containing the mute buttons
     */
    private JPanel createMuteButtonsToolbar() {
        // Main container with vertical centering
        JPanel tabToolbar = new JPanel();
        tabToolbar.setLayout(new BoxLayout(tabToolbar, BoxLayout.X_AXIS));
        tabToolbar.setOpaque(false);
        
        // Add vertical glue for centering
        tabToolbar.add(Box.createVerticalGlue());
        
        // Create panel for the buttons with small margins
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 8));
        
        // Create 16 mute buttons for drum pads
        for (int i = 0; i < 16; i++) {
            JToggleButton muteButton = createMuteButton(i, true);
            buttonPanel.add(muteButton);
        }
        
        // Add a small separator
        buttonPanel.add(Box.createHorizontalStrut(8));
        
        // Create 4 mute buttons for melodic sequencers
        for (int i = 0; i < 4; i++) {
            JToggleButton muteButton = createMuteButton(i, false);
            buttonPanel.add(muteButton);
        }
        
        // Add button panel to toolbar
        tabToolbar.add(buttonPanel);
        tabToolbar.add(Box.createVerticalGlue());
        
        return tabToolbar;
    }

    /**
     * Creates a single mute button with proper styling
     * 
     * @param index The index of the button (0-15 for drums, 0-3 for melodic)
     * @param isDrum Whether this is a drum mute button (true) or melodic (false)
     * @return A styled toggle button
     */
    private JToggleButton createMuteButton(int index, boolean isDrum) {
        JToggleButton muteButton = new JToggleButton();
        
        // Make buttons very compact
        Dimension size = new Dimension(16, 16);
        muteButton.setPreferredSize(size);
        muteButton.setMinimumSize(size);
        muteButton.setMaximumSize(size);
        
        // Set appearance
        muteButton.setText("");
        muteButton.setToolTipText("Mute " + (isDrum ? "Drum " : "Synth ") + (index + 1));
        
        // Use FlatLaf styling for rounded look
        muteButton.putClientProperty("JButton.buttonType", "roundRect");
        muteButton.putClientProperty("JButton.squareSize", true);
        
        // Apply different colors for drum vs melodic buttons
        Color defaultColor = isDrum ? 
                ColorUtils.fadedOrange.darker() : 
                ColorUtils.coolBlue.darker();
        Color activeColor = isDrum ? 
                ColorUtils.fadedOrange : 
                ColorUtils.coolBlue;
        
        muteButton.setBackground(defaultColor);
        
        // Add action listener for mute functionality
        final int buttonIndex = index;
        final boolean isDrumButton = isDrum;
        
        muteButton.addActionListener(e -> {
            boolean isMuted = muteButton.isSelected();
            muteButton.setBackground(isMuted ? activeColor : defaultColor);
            
            // Apply mute to the appropriate sequencer
            if (isDrumButton) {
                if (drumSequencerPanel != null) {
                    // Mute drum pad
                    toggleDrumMute(buttonIndex, isMuted);
                }
            } else {
                // Mute melodic sequencer
                toggleMelodicMute(buttonIndex, isMuted);
            }
        });
        
        return muteButton;
    }

    /**
     * Toggle mute state for a specific drum pad
     * 
     * @param drumIndex The index of the drum pad to mute/unmute (0-15)
     * @param muted Whether to mute (true) or unmute (false)
     */
    private void toggleDrumMute(int drumIndex, boolean muted) {
        logger.info("{}muting drum {}", muted ? "" : "Un", drumIndex + 1);
        if (drumSequencerPanel != null) {
            // Use the sequencer's API to mute the specific drum
            DrumSequencer sequencer = drumSequencerPanel.getSequencer();
            if (sequencer != null) {
                sequencer.setVelocity(drumIndex, muted ? 0 : 100);
            }
        }
    }

    /**
     * Toggle mute state for a melodic sequencer
     * 
     * @param seqIndex The index of the melodic sequencer (0-3)
     * @param muted Whether to mute (true) or unmute (false)
     */
    private void toggleMelodicMute(int seqIndex, boolean muted) {
        logger.info("{}muting melodic sequencer {}", muted ? "" : "Un", seqIndex + 1);
        // Calculate the actual tab index (Mono 1 starts at tab index 2)
        int tabIndex = seqIndex + 2;
        
        if (tabIndex >= 0 && tabIndex < 6) {
            Component comp = tabbedPane.getComponentAt(tabIndex);
            if (comp instanceof MelodicSequencerPanel) {
                MelodicSequencerPanel panel = (MelodicSequencerPanel) comp;
                MelodicSequencer sequencer = panel.getSequencer();
                if (sequencer != null) {
                    // Set volume to 0 when muted, 100 when unmuted
                    sequencer.setLevel(muted ? 0 : 100);
                }
            }
        }
    }

    private Component createMixerPanel() {
        return new MixerPanel(synthesizer);
    }

    private Component createChordSequencerPanel() {
        return new JPanel();
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

    private Component createMelodicSequencerPanel(int channel) {
        melodicSequencerPanel = new MelodicSequencerPanel(channel, noteEvent -> {
            // This callback should only be used for UI updates if needed
            // The actual note playing is handled inside the sequencer
            logger.debug("Note event received from sequencer: note={}, velocity={}, duration={}",
                    noteEvent.getNote(), noteEvent.getVelocity(), noteEvent.getDurationMs());
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
