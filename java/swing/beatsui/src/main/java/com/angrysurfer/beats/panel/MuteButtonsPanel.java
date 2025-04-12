package com.angrysurfer.beats.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.sequencer.MelodicSequencer;

/**
 * Panel that handles all mute buttons for both drum and melodic sequencers
 */
public class MuteButtonsPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(MuteButtonsPanel.class);
    
    private final List<JToggleButton> drumMuteButtons = new ArrayList<>();
    private final List<JToggleButton> melodicMuteButtons = new ArrayList<>();
    
    private DrumSequencer drumSequencer;
    private List<MelodicSequencer> melodicSequencers;
    
    /**
     * Create a new mute buttons panel (without sequencers initially)
     */
    public MuteButtonsPanel() {
        initializeUI();
    }
    
    /**
     * Create a new mute buttons panel with sequencers
     */
    public MuteButtonsPanel(DrumSequencer drumSequencer, List<MelodicSequencer> melodicSequencers) {
        this.drumSequencer = drumSequencer;
        this.melodicSequencers = melodicSequencers;
        initializeUI();
    }
    
    private void initializeUI() {
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        
        add(Box.createVerticalGlue());
        add(createButtonsPanel());
        add(Box.createVerticalGlue());
    }
    
    private JPanel createButtonsPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 8));

        // Create drum mute buttons
        for (int i = 0; i < 16; i++) {
            JToggleButton muteButton = createMuteButton(i, true);
            buttonPanel.add(muteButton);
            drumMuteButtons.add(muteButton);
        }

        buttonPanel.add(Box.createHorizontalStrut(8));

        // Create melodic sequencer mute buttons
        for (int i = 0; i < 4; i++) {
            JToggleButton muteButton = createMuteButton(i, false);
            buttonPanel.add(muteButton);
            melodicMuteButtons.add(muteButton);
        }
        
        return buttonPanel;
    }
    
    private JToggleButton createMuteButton(int index, boolean isDrum) {
        JToggleButton muteButton = new JToggleButton();

        Dimension size = new Dimension(16, 16);
        muteButton.setPreferredSize(size);
        muteButton.setMinimumSize(size);
        muteButton.setMaximumSize(size);

        muteButton.setText("");
        muteButton.setToolTipText("Mute " + (isDrum ? "Drum " : "Synth ") + (index + 1));
        muteButton.putClientProperty("JButton.squareSize", true);

        Color defaultColor = isDrum
                ? ColorUtils.fadedOrange.darker()
                : ColorUtils.coolBlue.darker();
        Color activeColor = isDrum
                ? ColorUtils.fadedOrange
                : ColorUtils.coolBlue;

        muteButton.setBackground(defaultColor);

        muteButton.addActionListener(e -> {
            boolean isMuted = muteButton.isSelected();
            muteButton.setBackground(isMuted ? activeColor : defaultColor);

            if (isDrum) {
                toggleDrumMute(index, isMuted);
            } else {
                toggleMelodicMute(index, isMuted);
            }
        });

        return muteButton;
    }
    
    private void toggleDrumMute(int drumIndex, boolean muted) {
        logger.info("{}muting drum {}", muted ? "" : "Un", drumIndex + 1);
        if (drumSequencer != null) {
            drumSequencer.setVelocity(drumIndex, muted ? 0 : 100);
        }
    }
    
    private void toggleMelodicMute(int seqIndex, boolean muted) {
        logger.info("{}muting melodic sequencer {}", muted ? "" : "Un", seqIndex + 1);
        if (melodicSequencers != null && seqIndex < melodicSequencers.size()) {
            MelodicSequencer sequencer = melodicSequencers.get(seqIndex);
            if (sequencer != null) {
                sequencer.setLevel(muted ? 0 : 100);
            }
        }
    }
    
    /**
     * Set the drum sequencer to control
     */
    public void setDrumSequencer(DrumSequencer sequencer) {
        this.drumSequencer = sequencer;
    }
    
    /**
     * Set the melodic sequencers to control
     */
    public void setMelodicSequencers(List<MelodicSequencer> sequencers) {
        this.melodicSequencers = sequencers;
    }
    
    /**
     * Check if a drum is muted
     */
    public boolean isDrumMuted(int index) {
        if (index >= 0 && index < drumMuteButtons.size()) {
            return drumMuteButtons.get(index).isSelected();
        }
        return false;
    }
    
    /**
     * Check if a melodic sequencer is muted
     */
    public boolean isMelodicMuted(int index) {
        if (index >= 0 && index < melodicMuteButtons.size()) {
            return melodicMuteButtons.get(index).isSelected();
        }
        return false;
    }
    
    /**
     * Set mute state for a drum
     */
    public void setDrumMuted(int index, boolean muted) {
        if (index >= 0 && index < drumMuteButtons.size()) {
            JToggleButton button = drumMuteButtons.get(index);
            if (button.isSelected() != muted) {
                button.setSelected(muted);
                // Update button appearance
                Color color = muted 
                    ? ColorUtils.fadedOrange 
                    : ColorUtils.fadedOrange.darker();
                button.setBackground(color);
                // Update sequencer
                toggleDrumMute(index, muted);
            }
        }
    }
    
    /**
     * Set mute state for a melodic sequencer
     */
    public void setMelodicMuted(int index, boolean muted) {
        if (index >= 0 && index < melodicMuteButtons.size()) {
            JToggleButton button = melodicMuteButtons.get(index);
            if (button.isSelected() != muted) {
                button.setSelected(muted);
                // Update button appearance
                Color color = muted 
                    ? ColorUtils.coolBlue 
                    : ColorUtils.coolBlue.darker();
                button.setBackground(color);
                // Update sequencer
                toggleMelodicMute(index, muted);
            }
        }
    }
    
    /**
     * Mute all drums
     */
    public void muteAllDrums() {
        for (int i = 0; i < drumMuteButtons.size(); i++) {
            setDrumMuted(i, true);
        }
    }
    
    /**
     * Unmute all drums
     */
    public void unmuteAllDrums() {
        for (int i = 0; i < drumMuteButtons.size(); i++) {
            setDrumMuted(i, false);
        }
    }
    
    /**
     * Mute all melodic sequencers
     */
    public void muteAllMelodic() {
        for (int i = 0; i < melodicMuteButtons.size(); i++) {
            setMelodicMuted(i, true);
        }
    }
    
    /**
     * Unmute all melodic sequencers
     */
    public void unmuteAllMelodic() {
        for (int i = 0; i < melodicMuteButtons.size(); i++) {
            setMelodicMuted(i, false);
        }
    }
}