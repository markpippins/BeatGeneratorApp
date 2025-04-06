package com.angrysurfer.beats.panel;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

import com.angrysurfer.beats.widget.ColorUtils;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.DrumSequencer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A panel to control the individual drum volumes in the sequencer
 */
public class StrikeMixerPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(StrikeMixerPanel.class);
    
    private final DrumSequencer sequencer;
    private final List<JSlider> volumeSliders = new ArrayList<>();
    private final List<JLabel> valueLabels = new ArrayList<>();
    private final int DRUM_PAD_COUNT;
    
    /**
     * Creates a new StrikeMixerPanel
     * 
     * @param sequencer The drum sequencer to control
     */
    public StrikeMixerPanel(DrumSequencer sequencer) {
        super(new BorderLayout(10, 10));
        this.sequencer = sequencer;
        this.DRUM_PAD_COUNT = 16; // Match the sequencer's drum count
        
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        initializeUI();
    }
    
    /**
     * Initialize the UI components
     */
    private void initializeUI() {
        // Create header with title
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("Drum Mixer");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerPanel.add(titleLabel);
        
        // Create main panel for sliders
        JPanel slidersPanel = new JPanel();
        slidersPanel.setLayout(new BoxLayout(slidersPanel, BoxLayout.X_AXIS));
        
        // Create channels based on drum names
        String[] drumNames = {
            "Kick", "Snare", "Closed HH", "Open HH", 
            "Tom 1", "Tom 2", "Tom 3", "Crash", 
            "Ride", "Rim", "Clap", "Cow", 
            "Clave", "Shaker", "Perc 1", "Perc 2"
        };
        
        // Add sliders for each drum
        for (int i = 0; i < DRUM_PAD_COUNT; i++) {
            JPanel channelPanel = createChannelPanel(i, drumNames[i]);
            slidersPanel.add(channelPanel);
            
            // Add spacing between channels
            if (i < DRUM_PAD_COUNT - 1) {
                slidersPanel.add(Box.createHorizontalStrut(3));
            }
        }
        
        // Wrap sliders panel in a scroll pane for horizontal scrolling
        JScrollPane scrollPane = new JScrollPane(slidersPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.setPreferredSize(new Dimension(600, 300)); // Height < 400px
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(e -> resetVolumes());
        
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> applyChanges());
        
        JButton cancelButton = new JButton("Close");
        cancelButton.addActionListener(e -> {
            // Close dialog
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispose();
            }
        });
        
        buttonPanel.add(resetButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(cancelButton);
        
        // Add components to main panel
        add(headerPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Initialize slider values
        syncSliderValues();
    }
    
    /**
     * Create a channel panel with slider and labels
     */
    private JPanel createChannelPanel(int drumIndex, String drumName) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorUtils.slateGray, 1), 
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        
        // Create label with drum name
        JLabel nameLabel = new JLabel(drumName);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 10));
        
        // Create vertical slider
        JSlider slider = new JSlider(JSlider.VERTICAL, 0, 127, 100);
        slider.setMajorTickSpacing(32);
        slider.setMinorTickSpacing(16);
        slider.setPaintTicks(true);
        slider.setPreferredSize(new Dimension(30, 200));
        slider.setMaximumSize(new Dimension(30, 200));
        
        // Create value display label
        JLabel valueLabel = new JLabel("100");
        valueLabel.setFont(new Font("Monospaced", Font.PLAIN, 10));
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        
        // Add listener to update value label
        slider.addChangeListener(e -> {
            int value = slider.getValue();
            valueLabel.setText(String.valueOf(value));
        });
        
        // Store components for later access
        volumeSliders.add(slider);
        valueLabels.add(valueLabel);
        
        // Add components to panel
        panel.add(nameLabel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(slider);
        panel.add(valueLabel);
        
        return panel;
    }
    
    /**
     * Sync slider values with sequencer
     */
    private void syncSliderValues() {
        for (int i = 0; i < DRUM_PAD_COUNT && i < volumeSliders.size(); i++) {
            // Get current velocity from sequencer
            int velocity = sequencer.getVelocity(i);
            
            // Update slider and label
            JSlider slider = volumeSliders.get(i);
            JLabel label = valueLabels.get(i);
            
            slider.setValue(velocity);
            label.setText(String.valueOf(velocity));
        }
    }
    
    /**
     * Reset all volumes to default
     */
    private void resetVolumes() {
        for (int i = 0; i < DRUM_PAD_COUNT && i < volumeSliders.size(); i++) {
            volumeSliders.get(i).setValue(100);
            valueLabels.get(i).setText("100");
        }
    }
    
    /**
     * Apply changes from sliders to sequencer
     */
    private void applyChanges() {
        for (int i = 0; i < DRUM_PAD_COUNT && i < volumeSliders.size(); i++) {
            int value = volumeSliders.get(i).getValue();
            sequencer.setVelocity(i, value);
            
            // Also update the Strike object directly
            Strike strike = sequencer.getStrike(i);
            if (strike != null) {
                strike.setLevel(value);
            }
        }
        
        logger.info("Applied mixer changes to all drum volumes");
    }
    
    /**
     * Show this panel in a dialog
     */
    public static void showDialog(JComponent parent, DrumSequencer sequencer) {
        StrikeMixerPanel panel = new StrikeMixerPanel(sequencer);
        
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Drum Mixer");
        dialog.setContentPane(panel);
        dialog.setSize(new Dimension(650, 380));
        dialog.setLocationRelativeTo(parent);
        dialog.setModal(true);
        dialog.setResizable(true);
        dialog.setVisible(true);
    }
}