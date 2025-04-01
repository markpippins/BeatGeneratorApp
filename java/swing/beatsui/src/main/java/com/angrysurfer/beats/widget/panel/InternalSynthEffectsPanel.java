package com.angrysurfer.beats.widget.panel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Hashtable;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

/**
 * Panel for controlling effects parameters of a synthesizer
 */
public class InternalSynthEffectsPanel extends JPanel {
    
    // Effects control constants
    public static final int CC_EFFECT_TYPE = 91;
    public static final int CC_PARAM1 = 92;
    public static final int CC_PARAM2 = 93;
    public static final int CC_MIX = 94;
    
    private final Synthesizer synthesizer;
    private final int midiChannel;
    
    // UI components
    private JSlider effectTypeSlider;
    private JSlider param1Slider;
    private JSlider param2Slider;
    private JSlider mixSlider;
    
    // Parameter group panels for dynamic labeling
    private JPanel param1Group;
    private JPanel param2Group;
    
    /**
     * Create a new Effects control panel
     * 
     * @param synthesizer The MIDI synthesizer to control
     * @param midiChannel The MIDI channel to send control changes to
     */
    public InternalSynthEffectsPanel(Synthesizer synthesizer, int midiChannel) {
        super();
        this.synthesizer = synthesizer;
        this.midiChannel = midiChannel;
        
        initializeUI();
    }
    
    /**
     * Initialize all UI components
     */
    private void initializeUI() {
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Effects",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Dialog", Font.BOLD, 11)
        ));

        // Use FlowLayout for sliders in a row with good spacing
        setLayout(new FlowLayout(FlowLayout.CENTER, 12, 5));

        // Create effect type selector with labeled vertical slider
        effectTypeSlider = createLabeledVerticalSlider(
                "Effect Type", 0, 3, 0,
                new String[]{"Reverb", "Delay", "Chorus", "Drive"}
        );

        // Create parameter sliders
        param1Slider = createVerticalSlider("Parameter 1", 0);
        param2Slider = createVerticalSlider("Parameter 2", 0);
        mixSlider = createVerticalSlider("Mix", 0);

        // Create slider groups with labels
        JPanel typeGroup = createSliderGroup("Type", effectTypeSlider);
        param1Group = createSliderGroup("Size/Time", param1Slider);
        param2Group = createSliderGroup("Decay/Fdbk", param2Slider);
        JPanel mixGroup = createSliderGroup("Mix", mixSlider);

        // Add slider groups to panel
        add(typeGroup);
        add(param1Group);
        add(param2Group);
        add(mixGroup);

        // Add event listeners
        setupEventHandlers();
    }
    
    /**
     * Add event listeners to all controls
     */
    private void setupEventHandlers() {
        effectTypeSlider.addChangeListener(e -> {
            if (!effectTypeSlider.getValueIsAdjusting()) {
                int effectType = effectTypeSlider.getValue();
                setControlChange(CC_EFFECT_TYPE, effectType * 32); // Scale to 0-127 range
                updateParameterLabels(effectType);
            }
        });

        param1Slider.addChangeListener(e -> {
            if (!param1Slider.getValueIsAdjusting()) {
                setControlChange(CC_PARAM1, param1Slider.getValue());
            }
        });

        param2Slider.addChangeListener(e -> {
            if (!param2Slider.getValueIsAdjusting()) {
                setControlChange(CC_PARAM2, param2Slider.getValue());
            }
        });

        mixSlider.addChangeListener(e -> {
            if (!mixSlider.getValueIsAdjusting()) {
                setControlChange(CC_MIX, mixSlider.getValue());
            }
        });
    }
    
    /**
     * Update parameter labels based on the selected effect type
     * 
     * @param effectType The effect type (0-3)
     */
    private void updateParameterLabels(int effectType) {
        switch (effectType) {
            case 0: // Reverb
                param1Group.setBorder(BorderFactory.createTitledBorder("Size"));
                param2Group.setBorder(BorderFactory.createTitledBorder("Decay"));
                break;
            case 1: // Delay
                param1Group.setBorder(BorderFactory.createTitledBorder("Time"));
                param2Group.setBorder(BorderFactory.createTitledBorder("Feedback"));
                break;
            case 2: // Chorus
                param1Group.setBorder(BorderFactory.createTitledBorder("Depth"));
                param2Group.setBorder(BorderFactory.createTitledBorder("Rate"));
                break;
            case 3: // Drive
                param1Group.setBorder(BorderFactory.createTitledBorder("Amount"));
                param2Group.setBorder(BorderFactory.createTitledBorder("Tone"));
                break;
        }
    }
    
    /**
     * Reset all controls to their default values
     */
    public void resetToDefaults() {
        effectTypeSlider.setValue(0);     // Reverb
        param1Slider.setValue(20);        // Small room size
        param2Slider.setValue(30);        // Medium decay
        mixSlider.setValue(20);           // Light mix
        
        // Update parameter labels for the default effect type
        updateParameterLabels(0);
        
        // Send these values to the synth
        updateSynthState();
    }
    
    /**
     * Send the current state of all controls to the synthesizer
     */
    public void updateSynthState() {
        setControlChange(CC_EFFECT_TYPE, effectTypeSlider.getValue() * 32);
        setControlChange(CC_PARAM1, param1Slider.getValue());
        setControlChange(CC_PARAM2, param2Slider.getValue());
        setControlChange(CC_MIX, mixSlider.getValue());
    }
    
    /**
     * Set a MIDI CC value on the synth
     * 
     * @param ccNumber The CC number to set
     * @param value The value to set (0-127)
     */
    private void setControlChange(int ccNumber, int value) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                MidiChannel midiCh = synthesizer.getChannels()[midiChannel];
                if (midiCh != null) {
                    midiCh.controlChange(ccNumber, value);
                }
            } catch (Exception e) {
                System.err.println("Error setting CC " + ccNumber + " on channel " + (midiChannel + 1) +
                        ": " + e.getMessage());
            }
        }
    }
    
    /**
     * Create a slider with a label underneath
     */
    private JPanel createSliderGroup(String title, JSlider slider) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setBorder(BorderFactory.createTitledBorder(title));

        // Center the slider
        JPanel sliderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        sliderPanel.add(slider);

        group.add(sliderPanel);

        return group;
    }

    /**
     * Create a vertical slider with consistent styling
     * 
     * @param tooltip Tooltip text
     * @param initialValue Initial value (0-127)
     * @return Configured JSlider
     */
    private JSlider createVerticalSlider(String tooltip, int initialValue) {
        JSlider slider = new JSlider(SwingConstants.VERTICAL, 0, 127, initialValue);
        slider.setToolTipText(tooltip);

        // Set up tick marks
        slider.setMajorTickSpacing(32);
        slider.setMinorTickSpacing(16);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(false);

        // Create tick labels - just show a few key values
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("0"));
        labelTable.put(32, new JLabel("32"));
        labelTable.put(64, new JLabel("64"));
        labelTable.put(96, new JLabel("96"));
        labelTable.put(127, new JLabel("127"));
        slider.setLabelTable(labelTable);

        // FlatLaf properties
        slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slider.putClientProperty("JSlider.paintThumbArrowShape", Boolean.TRUE);

        // Set reasonable size for a vertical slider
        slider.setPreferredSize(new Dimension(60, 120));

        return slider;
    }

    /**
     * Create a vertical slider with labeled tick marks
     * 
     * @param tooltip Tooltip text
     * @param min Minimum value
     * @param max Maximum value 
     * @param initialValue Initial value
     * @param labels Array of labels for tick marks
     * @return Configured JSlider with labels
     */
    private JSlider createLabeledVerticalSlider(String tooltip, int min, int max, int initialValue, String[] labels) {
        JSlider slider = new JSlider(SwingConstants.VERTICAL, min, max, initialValue);
        slider.setToolTipText(tooltip);

        // Set up tick marks and labels
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setSnapToTicks(true);

        // Create tick labels
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (int i = min; i <= max; i++) {
            JLabel label = new JLabel(labels[i]);
            label.setFont(new Font("Dialog", Font.PLAIN, 9));
            labelTable.put(i, label);
        }
        slider.setLabelTable(labelTable);

        // Add FlatLaf styling
        slider.putClientProperty("JSlider.isFilled", Boolean.TRUE);
        slider.putClientProperty("JSlider.paintThumbArrowShape", Boolean.TRUE);

        // Set reasonable size for a vertical slider
        slider.setPreferredSize(new Dimension(60, 120));

        return slider;
    }
    
    // Getters and setters for individual control values
    
    public int getEffectType() {
        return effectTypeSlider.getValue();
    }
    
    public void setEffectType(int value) {
        effectTypeSlider.setValue(value);
        updateParameterLabels(value);
    }
    
    public int getParam1() {
        return param1Slider.getValue();
    }
    
    public void setParam1(int value) {
        param1Slider.setValue(value);
    }
    
    public int getParam2() {
        return param2Slider.getValue();
    }
    
    public void setParam2(int value) {
        param2Slider.setValue(value);
    }
    
    public int getMix() {
        return mixSlider.getValue();
    }
    
    public void setMix(int value) {
        mixSlider.setValue(value);
    }
}