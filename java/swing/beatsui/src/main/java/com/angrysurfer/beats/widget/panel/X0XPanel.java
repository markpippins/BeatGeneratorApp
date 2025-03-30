package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DrumButton;
import com.angrysurfer.beats.widget.NoteSelectionDial;
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
    private final List<NoteSelectionDial> noteDials = new ArrayList<>();
    private final List<Dial> velocityDials = new ArrayList<>();
    private final List<Dial> gateDials = new ArrayList<>();

    private boolean isPlaying = false;
    private int currentStep = 0;

    // Add these fields for tick-based timing
    private int stepCounter = 0; // Current step in X0X pattern (0-15)
    private int tickCounter = 0; // Count ticks within current step
    private int ticksPerStep = 6; // How many ticks make one X0X step
    private int nextStepTick = 0; // When to trigger the next step

    private Synthesizer synthesizer = null;

    // Add this field to X0XPanel
    private JComboBox<PresetItem> presetCombo;

    // Add these fields to X0XPanel
    private int latencyCompensation = 20; // milliseconds to compensate for system latency

    // Add these fields to X0XPanel
    private int lookAheadMs = 40; // How far ahead to schedule notes
    private boolean useAheadScheduling = true; // Enable/disable look-ahead

    // Constructor - remove the stepTimer initialization
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

    // Add a method to calculate timing parameters
    private void updateTimingParameters() {
        try {
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null) {
                // Calculate ticks per step based on session settings
                int ppq = session.getTicksPerBeat(); // Pulses per quarter note
                int stepsPerBeat = 4; // Standard X0X uses 4 steps per beat

                // Calculate timing parameters
                ticksPerStep = ppq / stepsPerBeat;
                nextStepTick = ticksPerStep; // Reset next step counter

                System.out.println("X0X timing: " + ticksPerStep + " ticks per step");
            }
        } catch (Exception ex) {
            // Use default values if something goes wrong
            ticksPerStep = 6;
            nextStepTick = ticksPerStep;
        }
    }

    private void initializeSynthesizer() {
        try {
            // Get information about all available MIDI devices
            MidiSystem.getMidiDeviceInfo();

            // Look for Gervill specifically
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            MidiDevice.Info gervillInfo = null;

            for (MidiDevice.Info info : infos) {
                if (info.getName().contains("Gervill")) {
                    gervillInfo = info;
                    break;
                }
            }

            // If we found Gervill, use it
            if (gervillInfo != null) {
                MidiDevice device = MidiSystem.getMidiDevice(gervillInfo);
                if (device instanceof Synthesizer) {
                    synthesizer = (Synthesizer) device;
                }
            }

            // If we didn't find Gervill specifically, just get the default synthesizer
            if (synthesizer == null) {
                synthesizer = MidiSystem.getSynthesizer();
            }

            // Open the synthesizer
            if (synthesizer != null && !synthesizer.isOpen()) {
                synthesizer.open();
                System.out.println("Opened synthesizer: " + synthesizer.getDeviceInfo().getName());
            }

            // Configure to use channel 16 (index 15)
            if (synthesizer != null && synthesizer.isOpen()) {
                // Get channel 16 (index 15)
                MidiChannel channel = synthesizer.getChannels()[15];

                // Basic configuration - ensure it's not muted
                if (channel != null) {
                    channel.controlChange(7, 100); // Set volume to 100
                    channel.controlChange(10, 64); // Pan center
                    channel.programChange(0); // Default program (Grand Piano)
                    System.out.println("Configured channel 16 (index 15) on synthesizer");

                    // Get name of the current program
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

            // Reset all counters
            stepCounter = 0;
            tickCounter = 0;
            nextStepTick = ticksPerStep;

            // Update timing parameters in case they changed
            updateTimingParameters();

            // Reset and highlight the first step
            SwingUtilities.invokeLater(() -> {
                // Reset all buttons
                for (TriggerButton button : triggerButtons) {
                    button.setHighlighted(false);
                }
                // Highlight first step
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
            // Process timing ticks for sequencing
            if (isPlaying && action.getData() instanceof Number) {
                tickCounter++;

                // Check if it's time for the next step
                if (tickCounter >= nextStepTick) {
                    // Calculate the next step
                    int oldStep = stepCounter;
                    stepCounter = (stepCounter + 1) % 16;

                    // Play the step and update UI
                    updateStepAndPlayNote(oldStep, stepCounter);

                    // Reset tick counter and calculate next step time
                    tickCounter = 0;
                    nextStepTick = ticksPerStep;
                }
            }
        }

        case Commands.SESSION_UPDATED -> {
            // Update timing parameters when session changes
            if (action.getData() instanceof Session) {
                updateTimingParameters();
            }
        }
        }
    }

    private void updateStepAndPlayNote(int oldStep, int newStep) {
        // First check if we need to play a note, and do that immediately with minimum
        // latency
        if (isPlaying && newStep >= 0 && newStep < triggerButtons.size() && newStep < noteDials.size()) {
            TriggerButton newButton = triggerButtons.get(newStep);
            if (newButton.isSelected()) { // Check if the trigger is active (selected)
                // Get note value
                NoteSelectionDial noteDial = noteDials.get(newStep);
                int noteValue = noteDial.getValue();

                // Get velocity from velocity dial
                int velocity = 100; // Default
                if (newStep < velocityDials.size()) {
                    // Scale dial value (0-100) to MIDI velocity range (0-127)
                    velocity = (int) Math.round(velocityDials.get(newStep).getValue() * 1.27);
                    // Ensure it's within valid MIDI range
                    velocity = Math.max(1, Math.min(127, velocity));
                }

                // Get gate time from gate dial
                int gateTime = 100; // Default (ms)
                if (newStep < gateDials.size()) {
                    // Scale dial value (0-100) to reasonable gate times (10-500ms)
                    gateTime = (int) Math.round(10 + gateDials.get(newStep).getValue() * 4.9);
                }

                // Play note with the parameters from all three dials
                playNote(noteValue, velocity, gateTime);
            }
        }

        // Then update UI (less time-critical)
        SwingUtilities.invokeLater(() -> {
            // Clear previous step highlight
            if (oldStep >= 0 && oldStep < triggerButtons.size()) {
                TriggerButton oldButton = triggerButtons.get(oldStep);
                oldButton.setHighlighted(false);
            }

            // Highlight current step
            if (newStep >= 0 && newStep < triggerButtons.size()) {
                TriggerButton newButton = triggerButtons.get(newStep);
                newButton.setHighlighted(true);

                if (getStatusConsumer() != null) {
                    getStatusConsumer().setStatus("Step: " + (newStep + 1) + " of 16");
                }
            }
        });
    }

    private void resetSequence() {
        currentStep = 0;
        SwingUtilities.invokeLater(() -> {
            // Clear all highlights when stopped
            for (TriggerButton button : triggerButtons) {
                button.setHighlighted(false);
            }
        });
    }

    private void updateTriggerButtons() {
        try {
            // First clear all button highlights
            for (TriggerButton button : triggerButtons) {
                button.setHighlighted(false);
            }

            // Then highlight only the current step
            if (isPlaying && currentStep >= 0 && currentStep < triggerButtons.size()) {
                triggerButtons.get(currentStep).setHighlighted(true);

                // Update status
                if (getStatusConsumer() != null) {
                    getStatusConsumer().setStatus("Step: " + (currentStep + 1));
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating trigger buttons: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for better debugging
        }
    }

    private void setup() {
        // Create a container panel with BorderLayout
        JPanel containerPanel = new JPanel(new BorderLayout(10, 10));
        containerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Create synth preset panel at the top
        JPanel presetPanel = createPresetPanel();
        containerPanel.add(presetPanel, BorderLayout.NORTH);

        // Create X0X sequencer panel in center
        JTabbedPane x0xPanel = createX0XPanel();
        containerPanel.add(new JScrollPane(x0xPanel), BorderLayout.CENTER);

        // Create control buttons at the bottom
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetSequence());

        JButton playButton = new JButton("Play");
        playButton.addActionListener(e -> {
            isPlaying = !isPlaying;
            playButton.setText(isPlaying ? "Stop" : "Play");

            if (isPlaying) {
                // Start playing
            } else {
                resetSequence();
            }
        });

        controlPanel.add(resetButton);
        controlPanel.add(playButton);

        containerPanel.add(controlPanel, BorderLayout.SOUTH);

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
        JPanel mainPanel = new JPanel(new BorderLayout());
        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 5, 0));
        sequencePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Wrap in scroll pane in case window gets too small
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        mainPanel.add(scrollPane, BorderLayout.CENTER);

        return mainPanel;
    }

    private Component createEffectsPanel() {
        return null;
    }

    private Component createPerformancePanel() {
        return null;
    }

    private Component createInstrumentPanel() {
        JPanel instrumentPanel = new JPanel(new BorderLayout(10, 10));
        instrumentPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create the main layout using a tabbed approach for parameter groups
        JTabbedPane paramTabs = new JTabbedPane();
        
        // Add tabs for different parameter groups
        paramTabs.addTab("Oscillator", createOscillatorPanel());
        paramTabs.addTab("Envelope", createEnvelopePanel());
        paramTabs.addTab("Filter", createFilterPanel());
        paramTabs.addTab("Modulation", createModulationPanel());
        
        // Add the tabs to the main panel
        instrumentPanel.add(paramTabs, BorderLayout.CENTER);
        
        return instrumentPanel;
    }

    private JPanel createOscillatorPanel() {
        JPanel panel = new JPanel(new GridLayout(4, 2, 10, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Oscillator type selector
        JComboBox<String> waveformCombo = new JComboBox<>(
                new String[]{"Sine", "Square", "Saw", "Triangle", "Pulse"});
        
        // Create parameter dials
        Dial oscMixDial = new Dial();
        oscMixDial.setLabel("Mix");
        oscMixDial.setToolTipText("Oscillator Mix");
        oscMixDial.setValue(50); // Start at midpoint
        oscMixDial.setMaximumSize(new Dimension(50,50));
        oscMixDial.setPreferredSize(new Dimension(50,50));

        Dial detuneDial = new Dial();
        detuneDial.setLabel("Detune");
        detuneDial.setToolTipText("Detune Amount");
        detuneDial.setValue(0); // Start at no detune
        detuneDial.setMaximumSize(new Dimension(50,50));
        detuneDial.setPreferredSize(new Dimension(50,50));
        
        Dial pulseDial = new Dial();
        pulseDial.setLabel("Width");
        pulseDial.setToolTipText("Pulse Width");
        pulseDial.setValue(50); // Start at midpoint
        pulseDial.setMaximumSize(new Dimension(50,50));
        pulseDial.setPreferredSize(new Dimension(50,50));
        
        // Add controls with labels
        panel.add(createLabeledControl("Waveform", waveformCombo));
        panel.add(createDialPanel(oscMixDial));
        panel.add(createLabeledControl("Octave", new JComboBox<>(new String[]{"-2", "-1", "0", "+1", "+2"})));
        panel.add(createDialPanel(detuneDial));
        panel.add(createLabeledControl("Sync", new JToggleButton("Off")));
        panel.add(createDialPanel(pulseDial));
        
        // Add control change listeners using different approach
        waveformCombo.addActionListener(e -> {
            if (synthesizer != null && synthesizer.isOpen()) {
                try {
                    MidiChannel channel = synthesizer.getChannels()[15];
                    int waveformType = waveformCombo.getSelectedIndex();
                    
                    // Try multiple approaches to set waveform
                    
                    // 1. Standard MIDI CC
                    channel.controlChange(70, waveformType * 25);
                    
                    // 2. Try other CCs that might affect timbre
                    channel.controlChange(71, waveformType * 25); // Resonance
                    channel.controlChange(74, waveformType * 25); // Brightness
                    
                    // 3. Use NRPN (Non-Registered Parameter Numbers) which some synths use
                    sendNRPN(channel, 1, waveformType);
                    
                    System.out.println("Set waveform to: " + waveformCombo.getSelectedItem() + " using multiple methods");
                } catch (Exception ex) {
                    System.err.println("Error setting waveform: " + ex.getMessage());
                }
            }
        });
        
        // Try multiple control parameters for each dial
        oscMixDial.addChangeListener(e -> applyMultipleParams(oscMixDial, new int[] {7, 74, 1}));
        detuneDial.addChangeListener(e -> applyMultipleParams(detuneDial, new int[] {94, 1, 5}));
        pulseDial.addChangeListener(e -> applyMultipleParams(pulseDial, new int[] {70, 74, 1}));
        
        // Add a special reset button to ensure controllers can be changed
        JButton resetButton = new JButton("Reset Controllers");
        resetButton.addActionListener(e -> {
            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[15];
                // Send controller reset
                channel.resetAllControllers();
                // Reapply the current dials
                applyMultipleParams(oscMixDial, new int[] {7, 74, 1});
                applyMultipleParams(detuneDial, new int[] {94, 1, 5});
                applyMultipleParams(pulseDial, new int[] {70, 74, 1});
                System.out.println("Reset all controllers and reapplied settings");
            }
        });
        
        panel.add(createLabeledControl("Reset", resetButton));
        
        return panel;
    }

    // Helper method to apply multiple CC parameters for each dial
    private void applyMultipleParams(Dial dial, int[] ccNumbers) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel channel = synthesizer.getChannels()[15];
                int value = dial.getValue();
                
                // Try multiple CC messages to find the one that works
                for (int cc : ccNumbers) {
                    channel.controlChange(cc, value);
                }
                
                System.out.println("Applied " + dial.getLabel() + " value: " + value + " to multiple CCs");
            } catch (Exception ex) {
                System.err.println("Error applying parameter: " + ex.getMessage());
            }
        }
    }

    // Helper method to send NRPN messages (some synths use these instead of CCs)
    private void sendNRPN(MidiChannel channel, int parameter, int value) {
        // NRPN Parameter MSB
        channel.controlChange(99, (parameter >> 7) & 0x7F);
        // NRPN Parameter LSB
        channel.controlChange(98, parameter & 0x7F);
        // Data Entry MSB
        channel.controlChange(6, (value >> 7) & 0x7F);
        // Data Entry LSB
        channel.controlChange(38, value & 0x7F);
    }

    private JPanel createEnvelopePanel() {
        JPanel panel = new JPanel(new GridLayout(2, 4, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create ADSR dials
        Dial attackDial = new Dial();
        attackDial.setLabel("Attack");
        attackDial.setToolTipText("Attack Time");
        attackDial.setMaximumSize(new Dimension(50,50));
        attackDial.setPreferredSize(new Dimension(50,50));

        Dial decayDial = new Dial();
        decayDial.setLabel("Decay");
        decayDial.setToolTipText("Decay Time");
        decayDial.setMaximumSize(new Dimension(50,50));
        decayDial.setPreferredSize(new Dimension(50,50));
        
        Dial sustainDial = new Dial();
        sustainDial.setLabel("Sustain");
        sustainDial.setToolTipText("Sustain Level");
        sustainDial.setMaximumSize(new Dimension(50,50));
        sustainDial.setPreferredSize(new Dimension(50,50));
        
        Dial releaseDial = new Dial();
        releaseDial.setLabel("Release");
        releaseDial.setToolTipText("Release Time");
        releaseDial.setMaximumSize(new Dimension(50,50));
        releaseDial.setPreferredSize(new Dimension(50,50));
        
        // Add dials to panel
        panel.add(createDialPanel(attackDial));
        panel.add(createDialPanel(decayDial));
        panel.add(createDialPanel(sustainDial));
        panel.add(createDialPanel(releaseDial));
        
        // Add control change listeners
        attackDial.addChangeListener(e -> {
            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[15];
                int value = attackDial.getValue();
                // CC 73 is typically used for Attack Time
                channel.controlChange(73, value);
            }
        });
        
        decayDial.addChangeListener(e -> {
            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[15];
                int value = decayDial.getValue();
                // CC 75 is often used for Decay Time
                channel.controlChange(75, value);
            }
        });
        
        sustainDial.addChangeListener(e -> {
            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[15];
                int value = sustainDial.getValue();
                // CC 79 is sometimes used for Sustain Level
                channel.controlChange(79, value);
            }
        });
        
        releaseDial.addChangeListener(e -> {
            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[15];
                int value = releaseDial.getValue();
                // CC 72 is often used for Release Time
                channel.controlChange(72, value);
            }
        });
        
        return panel;
    }

    private JPanel createFilterPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Filter controls
        JComboBox<String> filterTypeCombo = new JComboBox<>(
                new String[]{"Low Pass", "High Pass", "Band Pass", "Notch"});
        
        Dial cutoffDial = new Dial();
        cutoffDial.setLabel("Cutoff");
        cutoffDial.setToolTipText("Filter Cutoff Frequency");
        cutoffDial.setMaximumSize(new Dimension(50,50));
        cutoffDial.setPreferredSize(new Dimension(50,50));

        Dial resonanceDial = new Dial();
        resonanceDial.setLabel("Resonance");
        resonanceDial.setToolTipText("Filter Resonance");
        resonanceDial.setMaximumSize(new Dimension(50,50));
        resonanceDial.setPreferredSize(new Dimension(50,50));
        
        Dial envAmountDial = new Dial();
        envAmountDial.setLabel("Env Amt");
        envAmountDial.setToolTipText("Envelope Modulation Amount");
        envAmountDial.setMaximumSize(new Dimension(50,50));
        envAmountDial.setPreferredSize(new Dimension(50,50));
        
        // Add controls to panel
        panel.add(createLabeledControl("Type", filterTypeCombo));
        panel.add(createDialPanel(cutoffDial));
        panel.add(createDialPanel(resonanceDial));
        panel.add(new JLabel()); // Empty cell for spacing
        panel.add(createDialPanel(envAmountDial));
        
        // Add control change listeners
        cutoffDial.addChangeListener(e -> {
            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[15];
                int value = cutoffDial.getValue();
                // CC 74 is typically used for filter cutoff
                channel.controlChange(74, value);
            }
        });
        
        resonanceDial.addChangeListener(e -> {
            if (synthesizer != null && synthesizer.isOpen()) {
                MidiChannel channel = synthesizer.getChannels()[15];
                int value = resonanceDial.getValue();
                // CC 71 is typically used for resonance
                channel.controlChange(71, value);
            }
        });
        
        return panel;
    }

    private JPanel createModulationPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // LFO controls
        JComboBox<String> lfoWaveformCombo = new JComboBox<>(
                new String[]{"Sine", "Triangle", "Square", "Sample & Hold"});
        
        Dial lfoRateDial = new Dial();
        lfoRateDial.setLabel("Rate");
        lfoRateDial.setToolTipText("LFO Speed");
        lfoRateDial.setMaximumSize(new Dimension(50,50));
        lfoRateDial.setPreferredSize(new Dimension(50,50));        

        Dial lfoAmountDial = new Dial();
        lfoAmountDial.setLabel("Amount");
        lfoAmountDial.setToolTipText("LFO Amount");
        lfoAmountDial.setMaximumSize(new Dimension(50,50));
        lfoAmountDial.setPreferredSize(new Dimension(50,50));

        JComboBox<String> lfoDestCombo = new JComboBox<>(
                new String[]{"Off", "Pitch", "Filter", "Amp"});
        
        // Add controls to panel
        panel.add(createLabeledControl("LFO Wave", lfoWaveformCombo));
        panel.add(createDialPanel(lfoRateDial));
        panel.add(createDialPanel(lfoAmountDial));
        panel.add(createLabeledControl("LFO Dest", lfoDestCombo));
        
        return panel;
    }

    // Helper method to create a labeled control panel
    private JPanel createLabeledControl(String labelText, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(5, 0));
        panel.add(new JLabel(labelText), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    // Helper method to create a panel containing a dial with label
    private JPanel createDialPanel(Dial dial) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(dial, BorderLayout.CENTER);
        if (dial.getLabel() != null) {
            panel.add(new JLabel(dial.getLabel(), JLabel.CENTER), BorderLayout.SOUTH);
        }
        return panel;
    }

    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        // Add 4 knobs
        for (int i = 0; i < 3; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setForeground(Color.GRAY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            if (i > 0) {
                JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                labelPanel.add(label);
                // Add label to the column
                column.add(labelPanel);
            }

            // Create dial - first one is always a NoteSelectionDial
            Dial dial = i == 0 ? new NoteSelectionDial() : new Dial();

            // Store the dial in the appropriate collection based on its type
            switch (i) {
            case 0:
                noteDials.add((NoteSelectionDial) dial); // Store the note dial for this column
                break; // <-- Missing break statement causing case 1 to execute too!

            case 1:
                velocityDials.add(dial); // Store the velocity dial
                break;

            case 2:
                gateDials.add(dial); // Store the gate dial
                break;

            default:
                // Probability dial - could be stored in a separate collection if needed
                break;
            }

            dial.setUpdateOnResize(false);
            dial.setToolTipText(String.format("Step %d %s", index + 1, getKnobLabel(i)));
            dial.setName("JDial-" + index + "-" + i);

            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);
        }
        // Add small spacing between knobs
        column.add(Box.createRigidArea(new Dimension(0, 5)));

        // Add the trigger button - make it a toggle button
        TriggerButton triggerButton = new TriggerButton("");
        triggerButton.setName("TriggerButton-" + index);
        triggerButton.setToolTipText("Step " + (index + 1));

        // Make it toggleable
        triggerButton.setToggleable(true);

        // Add a clean action listener that doesn't interfere with toggle behavior
        triggerButton.addActionListener(e -> {
            // No need to manually toggle - JToggleButton handles it automatically
            System.out.println("Trigger " + index + " is now " + (triggerButton.isSelected() ? "ON" : "OFF"));
        });

        triggerButtons.add(triggerButton);
        // Center the button horizontally
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        // Add the pad button
        JButton padButton = new DrumButton();
        padButton.setName("PadButton-" + index);
        padButton.setToolTipText("Pad " + (index + 1));
        padButton.setText(Integer.toString(index + 1));

        // Add action to manually trigger the note when pad button is clicked
        padButton.addActionListener(e -> {
            if (index < noteDials.size()) {
                // Get note from dial
                NoteSelectionDial noteDial = noteDials.get(index);
                int noteValue = noteDial.getValue();

                // Get velocity
                int velocity = 127; // Full velocity for manual triggers
                if (index < velocityDials.size()) {
                    velocity = (int) Math.round(velocityDials.get(index).getValue() * 1.27);
                    velocity = Math.max(1, Math.min(127, velocity));
                }

                // Get gate time
                int gateTime = 250; // Longer gate time for manual triggers
                if (index < gateDials.size()) {
                    gateTime = (int) Math.round(50 + gateDials.get(index).getValue() * 4.5);
                }

                // Play the note with velocity and gate time from dials
                playNote(noteValue, velocity, gateTime);
            }
        });

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(padButton);
        column.add(buttonPanel2);

        return column;
    }

    private String getKnobLabel(int i) {
        return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
    }

    /**
     * Inner class to represent preset items in the combo box
     */
    private static class PresetItem {
        private final int number;
        private final String name;

        public PresetItem(int number, String name) {
            this.number = number;
            this.name = name;
        }

        public int getNumber() {
            return number;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Create a panel with a preset selector combo box
     */
    private JPanel createPresetPanel() {
        JPanel presetPanel = new JPanel(new BorderLayout(5, 5));
        presetPanel.setBorder(BorderFactory.createTitledBorder("Synth Presets"));

        // Create preset combo box
        presetCombo = new JComboBox<>();
        populatePresetCombo();

        // Add listener to change synth preset when selected
        presetCombo.addActionListener(e -> {
            if (presetCombo.getSelectedItem() instanceof PresetItem) {
                PresetItem item = (PresetItem) presetCombo.getSelectedItem();
                setProgramChange(item.getNumber());
                System.out.println("Selected preset: " + item.getName() + " (#" + item.getNumber() + ")");
            }
        });

        // Add a label and the combo box to the panel
        JPanel innerPanel = new JPanel(new BorderLayout(5, 0));
        innerPanel.add(new JLabel("Preset:"), BorderLayout.WEST);
        innerPanel.add(presetCombo, BorderLayout.CENTER);

        presetPanel.add(innerPanel, BorderLayout.NORTH);

        return presetPanel;
    }

    /**
     * Load presets for the current instrument into the combo box
     */
    private void populatePresetCombo() {
        presetCombo.removeAllItems();

        // Get preset names from InternalSynthManager - use GM synth ID (1)
        List<String> presetNames = InternalSynthManager.getInstance().getPresetNames(1L);

        // Always add all 128 GM presets
        for (int i = 0; i < 128; i++) {
            // Get name from the list if available, otherwise use generic name
            String presetName;
            if (i < presetNames.size() && presetNames.get(i) != null && !presetNames.get(i).isEmpty()) {
                presetName = presetNames.get(i);
            } else {
                presetName = "Program " + i;
            }

            // Add the preset to the combo box
            presetCombo.addItem(new PresetItem(i, i + ": " + presetName));
        }

        // Select the first preset by default
        if (presetCombo.getItemCount() > 0) {
            presetCombo.setSelectedIndex(0);
        }
    }

    /**
     * Send program change to the synthesizer
     */
    private void setProgramChange(int program) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // Use channel 16 (index 15)
                MidiChannel channel = synthesizer.getChannels()[15];
                
                if (channel != null) {
                    channel.programChange(program);
                    System.out.println("Changed synth program to " + program);
                    
                    // Reset and reinitialize controllers after program change
                    reinitializeControllers();
                }
            } catch (Exception e) {
                System.err.println("Error changing program: " + e.getMessage());
            }
        }
    }

    /**
     * Reset controllers and synchronize controls after preset change
     */
    private void reinitializeControllers() {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel channel = synthesizer.getChannels()[15];
                
                // First reset all controllers
                channel.resetAllControllers();
                
                // Wait a tiny bit
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {}
                
                // Set basic channel parameters
                channel.controlChange(7, 100); // Volume
                channel.controlChange(10, 64); // Pan center
                
                // Enable expression
                channel.controlChange(11, 127);
                
                System.out.println("Reinitialized controllers after preset change");
            } catch (Exception e) {
                System.err.println("Error reinitializing controllers: " + e.getMessage());
            }
        }
    }

    /**
     * Play a note on the synthesizer
     * 
     * @param note       MIDI note number (0-127)
     * @param velocity   Velocity value (0-127)
     * @param durationMs Duration in milliseconds
     */
    private void playNote(int note, int velocity, int durationMs) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // Play on channel 16 (index 15)
                final MidiChannel channel = synthesizer.getChannels()[15];

                if (channel != null) {
                    if (useAheadScheduling) {
                        // Schedule note in advance to compensate for latency
                        new Thread(() -> {
                            try {
                                // Sleep for a very short time to allow thread to get high priority
                                Thread.sleep(1);
                                // Set this thread to max priority
                                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);

                                // Calculate when to play the note (slightly in the future)
                                long currentTime = System.currentTimeMillis();
                                long targetTime = currentTime + lookAheadMs;
                                long waitTime = targetTime - System.currentTimeMillis();

                                // Wait until precise time to play the note
                                if (waitTime > 0) {
                                    Thread.sleep(waitTime);
                                }

                                // Play the note exactly when needed
                                channel.noteOn(note, velocity);

                                // Sleep for note duration
                                Thread.sleep(durationMs);

                                // Turn off the note
                                channel.noteOff(note);
                            } catch (InterruptedException e) {
                                // Ignore interruptions
                            }
                        }).start();
                    } else {
                        // Original direct playback code
                        channel.noteOn(note, velocity);

                        // Schedule note off with our gate time
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
