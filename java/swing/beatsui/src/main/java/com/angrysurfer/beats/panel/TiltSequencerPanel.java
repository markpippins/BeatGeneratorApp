package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.sequencer.MelodicSequencer;
import com.angrysurfer.core.sequencer.TimingUpdate;

/**
 * A panel with 16 tilt sliders that respond to bar changes via TimingBus
 */
public class TiltSequencerPanel extends JPanel implements IBusListener {
    
    private static final Logger logger = LoggerFactory.getLogger(TiltSequencerPanel.class);
    private static final int SLIDER_COUNT = 16;
    private static final int MIN_VALUE = -7;
    private static final int MAX_VALUE = 7;
    private static final int DEFAULT_VALUE = 0;
    
    private List<JSlider> tiltSliders = new ArrayList<>(SLIDER_COUNT);
    private List<JPanel> sliderContainers = new ArrayList<>(SLIDER_COUNT);
    private int currentBar = 0;
    private MelodicSequencer sequencer;
    
    public TiltSequencerPanel(MelodicSequencer sequencer) {
        this.sequencer = sequencer;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Tilt"));
        setPreferredSize(new Dimension(getPreferredSize().width, 250));
        
        JPanel slidersPanel = new JPanel(new GridLayout(1, SLIDER_COUNT, 2, 0));
        JPanel labelsPanel = new JPanel(new GridLayout(1, SLIDER_COUNT, 2, 0));
        
        // Create the sliders and labels
        for (int i = 0; i < SLIDER_COUNT; i++) {
            // Create a vertical slider
            JSlider slider = createTiltSlider(i);
            tiltSliders.add(slider);
            
            // Create container for slider with spacing
            JPanel sliderContainer = new JPanel();
            sliderContainer.setLayout(new BorderLayout());
            sliderContainer.add(slider, BorderLayout.CENTER);
            
            // Add padding on sides of slider
            sliderContainer.add(Box.createHorizontalStrut(2), BorderLayout.WEST);
            sliderContainer.add(Box.createHorizontalStrut(2), BorderLayout.EAST);
            
            sliderContainers.add(sliderContainer);
            slidersPanel.add(sliderContainer);
            
            // Create label for slider
            JLabel label = new JLabel(String.valueOf(i + 1), JLabel.CENTER);
            label.setFont(label.getFont().deriveFont(10f));
            labelsPanel.add(label);
        }
        
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(slidersPanel, BorderLayout.CENTER);
        mainPanel.add(labelsPanel, BorderLayout.SOUTH);
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Register with the TimingBus
        TimingBus.getInstance().register(this);
    }
    
    private JSlider createTiltSlider(int index) {
        // Create a vertical slider with the appropriate range
        JSlider slider = new JSlider(SwingConstants.VERTICAL, MIN_VALUE, MAX_VALUE, DEFAULT_VALUE);
        
        // Configure appearance
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setMajorTickSpacing(1);
        slider.setSnapToTicks(true);
        
        // Create custom Roman numeral labels
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        
        // Negative scale degrees (flat)
        labelTable.put(-7, new JLabel("♭VII"));
        labelTable.put(-6, new JLabel("♭VI"));
        labelTable.put(-5, new JLabel("♭V"));
        labelTable.put(-4, new JLabel("♭IV"));
        labelTable.put(-3, new JLabel("♭III"));
        labelTable.put(-2, new JLabel("♭II"));
        labelTable.put(-1, new JLabel("♭I"));
        
        // Root
        labelTable.put(0, new JLabel("I"));
        
        // Positive scale degrees
        labelTable.put(1, new JLabel("II"));
        labelTable.put(2, new JLabel("III"));
        labelTable.put(3, new JLabel("IV"));
        labelTable.put(4, new JLabel("V"));
        labelTable.put(5, new JLabel("VI"));
        labelTable.put(6, new JLabel("VII"));
        labelTable.put(7, new JLabel("VIII")); // Octave
        
        // Make labels smaller and use a music-friendly font
        for (JLabel label : labelTable.values()) {
            // Try different fonts that might support music symbols better
            label.setFont(new Font("Lucida Sans Unicode", Font.PLAIN, 9));
            // Alternative fonts to try: "DejaVu Sans", "Arial Unicode MS", "Segoe UI Symbol"
        }
        
        // Set the custom label table
        slider.setLabelTable(labelTable);
        
        // Set slider size
        slider.setPreferredSize(new Dimension(40, 230));
        
        // Create tooltip that shows both Roman numeral and numeric value
        updateSliderTooltip(slider, index);
        
        // Add change listener
        slider.addChangeListener(e -> {
            if (!slider.getValueIsAdjusting()) {
                int tiltValue = slider.getValue();
                updateSliderTooltip(slider, index);
                logger.debug("Tilt value changed for step {}: {}", index + 1, tiltValue);
            }
        });
        
        return slider;
    }

    private void updateSliderTooltip(JSlider slider, int index) {
        int value = slider.getValue();
        String romanNumeral = getRomanNumeral(value);
        slider.setToolTipText("Bar " + (index + 1) + ": " + romanNumeral + " (" + value + ")");
    }

    private String getRomanNumeral(int value) {
        switch (value) {
            case -7: return "♭VII";
            case -6: return "♭VI";
            case -5: return "♭V";
            case -4: return "♭IV";
            case -3: return "♭III";
            case -2: return "♭II";
            case -1: return "♭I";
            case 0: return "I";
            case 1: return "II";
            case 2: return "III";
            case 3: return "IV";
            case 4: return "V";
            case 5: return "VI";
            case 6: return "VII";
            case 7: return "VIII";
            default: return String.valueOf(value);
        }
    }
    
    @Override
    public void onAction(Command action) {
        if (action.getData() instanceof TimingUpdate update) {
            // Check if it's a bar change
            if (update.bar() != currentBar) {
                currentBar = update.bar() - 1;
                highlightCurrentBar();
                applyTilt();
            }
        }
    }
    
    /**
     * Apply the tilt value for the current bar to the sequencer
     */
    private void applyTilt() {
        // Get current bar within the 16-bar pattern
        int barIndex = currentBar % SLIDER_COUNT;
        
        // Get the tilt value for the current bar
        int tiltValue = getTiltValue(barIndex);
        
        // Apply the tilt to the sequencer
        if (sequencer != null) {
            // Apply the note offset to the sequencer
            sequencer.setCurrentTilt(tiltValue);
            logger.debug("Applied tilt of {} to bar {}", tiltValue, currentBar + 1);
        }
    }

    private void highlightCurrentBar() {
        // Map the bar to a step (potentially with wrap-around)
        int stepToHighlight = currentBar % SLIDER_COUNT;
        
        // Update the appearance of all slider containers
        for (int i = 0; i < sliderContainers.size(); i++) {
            JPanel container = sliderContainers.get(i);
            
            if (i == stepToHighlight) {
                // Highlight the current step
                container.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
            } else {
                // Reset highlighting
                container.setBorder(null);
            }
            
            container.repaint();
        }
    }
    
    /**
     * Get the tilt value for a specific step
     */
    public int getTiltValue(int step) {
        if (step >= 0 && step < tiltSliders.size()) {
            return tiltSliders.get(step).getValue();
        }
        return DEFAULT_VALUE;
    }
    
    /**
     * Set the tilt value for a specific step
     */
    public void setTiltValue(int step, int value) {
        if (step >= 0 && step < tiltSliders.size()) {
            tiltSliders.get(step).setValue(Math.max(MIN_VALUE, Math.min(MAX_VALUE, value)));
        }
    }
    
    // Add to the initialize() method in MelodicSequencerPanel
    private void initialize() {
        TiltSequencerPanel tiltSequencerPanel = new TiltSequencerPanel(sequencer);
        add(tiltSequencerPanel, BorderLayout.SOUTH);
    }
}