package com.angrysurfer.beats.panel;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.core.sequencer.DrumSequencer;

public class PopupMixerPanel extends JPanel {

    private DrumSequencer sequencer;
    
    // Arrays to track mute and solo states
    private boolean[] muteStates;
    private boolean[] soloStates;
    private boolean soloActive = false;
    
    // UI components for channel strips
    private JSlider[] volumeSliders;
    private JToggleButton[] muteButtons;
    private JToggleButton[] soloButtons;

    public PopupMixerPanel(DrumSequencer sequencer) {
        super(new BorderLayout());
        this.sequencer = sequencer;
        
        // Initialize state arrays
        int drumCount = DrumSequencer.DRUM_PAD_COUNT;
        muteStates = new boolean[drumCount];
        soloStates = new boolean[drumCount];
        volumeSliders = new JSlider[drumCount];
        muteButtons = new JToggleButton[drumCount];
        soloButtons = new JToggleButton[drumCount];
        
        initialize();
    }
    
    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Main panel for channel strips
        JPanel channelStripsPanel = new JPanel(new GridLayout(1, DrumSequencer.DRUM_PAD_COUNT));
        
        // Create channel strips
        for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
            channelStripsPanel.add(createChannelStrip(i));
        }
        
        // Add scrolling if needed
        JScrollPane scrollPane = new JScrollPane(channelStripsPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        add(scrollPane, BorderLayout.CENTER);
        
        // Add master controls at bottom
        add(createMasterControls(), BorderLayout.SOUTH);
    }
    
    private JPanel createChannelStrip(int drumIndex) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // Add drum name label
        String drumName = "Drum " + (drumIndex + 1);
        if (sequencer.getStrike(drumIndex) != null) {
            drumName = sequencer.getStrike(drumIndex).getName();
        }
        
        JLabel nameLabel = new JLabel(drumName);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
        panel.add(nameLabel);
        
        panel.add(Box.createVerticalStrut(5));
        
        // Add volume slider
        JSlider volumeSlider = new JSlider(JSlider.VERTICAL, 0, 127, sequencer.getVelocity(drumIndex));
        volumeSliders[drumIndex] = volumeSlider;
        volumeSlider.setPreferredSize(new Dimension(40, 150));
        volumeSlider.setMajorTickSpacing(32);
        volumeSlider.setPaintTicks(true);
        volumeSlider.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Add change listener to update volume
        volumeSlider.addChangeListener(e -> {
            if (!volumeSlider.getValueIsAdjusting()) {
                sequencer.setVelocity(drumIndex, volumeSlider.getValue());
            }
        });
        
        // Create a panel to center the slider
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.X_AXIS));
        sliderPanel.add(Box.createHorizontalGlue());
        sliderPanel.add(volumeSlider);
        sliderPanel.add(Box.createHorizontalGlue());
        sliderPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sliderPanel);
        
        // Add volume value label
        JLabel volumeLabel = new JLabel(String.valueOf(volumeSlider.getValue()));
        volumeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        volumeSlider.addChangeListener(e -> volumeLabel.setText(String.valueOf(volumeSlider.getValue())));
        panel.add(volumeLabel);
        
        panel.add(Box.createVerticalStrut(10));
        
        // Add mute and solo buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 0));
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        // Create mute button
        muteButtons[drumIndex] = createMuteButton(drumIndex);
        buttonPanel.add(muteButtons[drumIndex]);
        
        // Create solo button
        soloButtons[drumIndex] = createSoloButton(drumIndex);
        buttonPanel.add(soloButtons[drumIndex]);
        
        panel.add(buttonPanel);
        
        return panel;
    }
    
    private JToggleButton createMuteButton(int drumIndex) {
        JToggleButton muteButton = new JToggleButton("🔇");
        muteButton.setToolTipText("Mute Drum " + (drumIndex + 1));
        muteButton.setPreferredSize(new Dimension(30, 30));
        muteButton.setMargin(new Insets(2, 2, 2, 2));
        muteButton.setBackground(ColorUtils.charcoalGray);
        muteButton.setForeground(Color.WHITE);
        
        // Store the original velocity to restore it when unmuting
        final int[] originalVelocity = {sequencer.getVelocity(drumIndex)};
        
        muteButton.addActionListener(e -> {
            boolean muted = muteButton.isSelected();
            muteStates[drumIndex] = muted;
            
            if (muted) {
                // Save current velocity before muting
                originalVelocity[0] = sequencer.getVelocity(drumIndex);
                sequencer.setVelocity(drumIndex, 0);
                muteButton.setBackground(ColorUtils.mutedRed);
            } else {
                // Restore original velocity
                sequencer.setVelocity(drumIndex, originalVelocity[0]);
                muteButton.setBackground(ColorUtils.charcoalGray);
            }
            
            // Update the slider to match
            volumeSliders[drumIndex].setValue(sequencer.getVelocity(drumIndex));
            
            // If this channel is unmuted but solo is active on other channels, 
            // we need to check if it should actually be audible
            updateChannelStates();
        });
        
        return muteButton;
    }
    
    private JToggleButton createSoloButton(int drumIndex) {
        JToggleButton soloButton = new JToggleButton("🎙️");
        soloButton.setToolTipText("Solo Drum " + (drumIndex + 1));
        soloButton.setPreferredSize(new Dimension(30, 30));
        soloButton.setMargin(new Insets(2, 2, 2, 2));
        soloButton.setBackground(ColorUtils.charcoalGray);
        soloButton.setForeground(Color.WHITE);
        
        // Store original velocities for all channels to restore when un-soloing
        final int[][] originalVelocities = {new int[DrumSequencer.DRUM_PAD_COUNT]};
        
        soloButton.addActionListener(e -> {
            boolean soloed = soloButton.isSelected();
            soloStates[drumIndex] = soloed;
            
            if (soloed) {
                // First time a solo is activated, save all channel volumes
                if (!soloActive) {
                    for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                        originalVelocities[0][i] = sequencer.getVelocity(i);
                    }
                    soloActive = true;
                }
                
                soloButton.setBackground(ColorUtils.warmMustard);
            } else {
                soloButton.setBackground(ColorUtils.charcoalGray);
                
                // Check if any other solos are active
                boolean anySoloActive = false;
                for (boolean state : soloStates) {
                    if (state) {
                        anySoloActive = true;
                        break;
                    }
                }
                
                // If no more solos are active, reset the solo state
                soloActive = anySoloActive;
            }
            
            // Update all channel states based on solo/mute configuration
            updateChannelStates();
        });
        
        return soloButton;
    }
    
    /**
     * Updates all drum channels based on mute and solo states
     */
    private void updateChannelStates() {
        // When any solo is active, only soloed channels play
        if (soloActive) {
            for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                if (soloStates[i]) {
                    // Solo trumps mute
                    if (!muteStates[i]) {
                        // Channel is soloed and not muted - ensure it's at previous volume
                        int currentVol = sequencer.getVelocity(i);
                        if (currentVol == 0) {
                            // If it was at 0, set to default value
                            sequencer.setVelocity(i, 100);
                            volumeSliders[i].setValue(100);
                        }
                    }
                } else {
                    // Channel not soloed - should be silent
                    sequencer.setVelocity(i, 0);
                    // Don't update slider position - we're temporarily silencing
                }
            }
        } else {
            // No solos active - respect only mute state
            for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                if (muteStates[i]) {
                    // Muted - should be silent
                    sequencer.setVelocity(i, 0);
                } else {
                    // Not muted - restore to slider position
                    sequencer.setVelocity(i, volumeSliders[i].getValue());
                }
            }
        }
    }
    
    private JPanel createMasterControls() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        
        // Add all mute button
        JButton allMuteButton = new JButton("Mute All");
        allMuteButton.addActionListener(e -> {
            for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                muteButtons[i].setSelected(true);
                muteStates[i] = true;
                sequencer.setVelocity(i, 0);
            }
            updateChannelStates();
        });
        panel.add(allMuteButton);
        
        // Add clear mutes button
        JButton clearMuteButton = new JButton("Clear Mutes");
        clearMuteButton.addActionListener(e -> {
            for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                muteButtons[i].setSelected(false);
                muteStates[i] = false;
            }
            updateChannelStates();
        });
        panel.add(clearMuteButton);
        
        // Add clear solos button
        JButton clearSoloButton = new JButton("Clear Solos");
        clearSoloButton.addActionListener(e -> {
            for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                soloButtons[i].setSelected(false);
                soloStates[i] = false;
            }
            soloActive = false;
            updateChannelStates();
        });
        panel.add(clearSoloButton);
        
        // Add close button
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> {
            Container container = SwingUtilities.getAncestorOfClass(Window.class, this);
            if (container instanceof Window) {
                ((Window) container).dispose();
            }
        });
        panel.add(closeButton);
        
        return panel;
    }
}