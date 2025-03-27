package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;

/**
 * Panel containing detailed player controls for performance, modulation, and ratcheting.
 * This panel is used inside PlayerEditPanel to manage the middle section of controls.
 */
public class PlayerEditDetailPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEditDetailPanel.class);
    private static final int SLIDER_HEIGHT = 80;
    
    // Reference to player being edited
    private final Player player;
    
    // Performance controls
    private final JSlider levelSlider;
    private final JSlider velocityMinSlider;
    private final JSlider velocityMaxSlider;
    private final Dial panDial;  // Changed from JSlider to Dial
    private final JButton prevButton;
    private final JButton nextButton;
    private final NoteSelectionDial noteDial;
    
    // Modulation controls
    private final JSlider swingSlider;
    private final JSlider probabilitySlider;
    private final JSlider randomSlider;
    private final JSlider sparseSlider;
    
    // Ratchet controls
    private final JSlider ratchetCountSlider;
    private final JSlider ratchetIntervalSlider;
    
    /**
     * Creates a new PlayerEditDetailPanel for the given player
     * 
     * @param player The player being edited
     */
    public PlayerEditDetailPanel(Player player) {
        super(new BorderLayout());
        this.player = player;
        
        // Initialize performance controls
        levelSlider = createSlider("Level", player.getLevel(), 0, 100);
        velocityMinSlider = createSlider("Min Velocity", player.getMinVelocity(), 0, 127);
        velocityMaxSlider = createSlider("Max Velocity", player.getMaxVelocity(), 0, 127);
        panDial = new Dial();
        panDial.setMinimum(0);
        panDial.setMaximum(127);
        panDial.setValue(player.getPanPosition().intValue(), false);
        panDial.setPreferredSize(new Dimension(50, 50));
        panDial.setMinimumSize(new Dimension(50, 50));
        panDial.setMaximumSize(new Dimension(50, 50));

        prevButton = new JButton("▲");
        nextButton = new JButton("▼");
        noteDial = new NoteSelectionDial();
        
        // Initialize modulation controls
        swingSlider = createSlider("Swing", player.getSwing(), 0, 100);
        probabilitySlider = createSlider("Probability", player.getProbability(), 0, 100);
        randomSlider = createSlider("Random", player.getRandomDegree(), 0, 100);
        sparseSlider = createSlider("Sparse", (long) (player.getSparse() * 100), 0, 100);
        
        // Initialize ratchet controls with tick spacing
        ratchetCountSlider = createSlider("Count", player.getRatchetCount(), 0, 6, true);
        ratchetIntervalSlider = createSlider("Interval", player.getRatchetInterval(), 1, 16, true);
        
        // Set up the UI components
        setupLayout();
        setupActionListeners();
    }
    
    /**
     * Sets up the overall panel layout
     */
    private void setupLayout() {
        // Create container with GridLayout (2 rows: performance/modulation on top, ratchet on bottom)
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        
        // Create and add performance panel
        JPanel performancePanel = createPerformancePanel();
        mainPanel.add(performancePanel);
        
        // Create and add modulation panel
        JPanel modulationPanel = createModulationPanel();
        mainPanel.add(modulationPanel);
        
        // Create and add ratchet panel
        JPanel ratchetPanel = createRatchetPanel();
        mainPanel.add(ratchetPanel);
        
        add(mainPanel, BorderLayout.CENTER);
    }
    
    /**
     * Creates the performance panel with reordered controls: Octave, Note, Level, Pan
     */
    private JPanel createPerformancePanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Performance"));
        
        // 1. OCTAVE CONTROLS
        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
        JLabel octaveLabel = new JLabel("Octave", JLabel.CENTER);
        
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        prevButton.setPreferredSize(new Dimension(35, 35));
        nextButton.setPreferredSize(new Dimension(35, 35));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);
        
        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(navPanel);
        
        // 2. NOTE DIAL
        Dimension dialSize = new Dimension(100, 100);
        noteDial.setPreferredSize(dialSize);
        noteDial.setMinimumSize(dialSize);
        noteDial.setMaximumSize(dialSize);
        noteDial.setCommand(Commands.NEW_VALUE_NOTE);
        noteDial.setValue(player.getNote().intValue());
        
        JPanel notePanel = new JPanel(new BorderLayout(5, 2));
        JLabel noteLabel = new JLabel("Note", JLabel.CENTER);
        notePanel.add(noteLabel, BorderLayout.NORTH);
        notePanel.add(noteDial, BorderLayout.CENTER);
        panel.add(notePanel);
        
        // 3. LEVEL SLIDER
        panel.add(createLabeledSlider("Level", levelSlider));
        
        // 4. PAN DIAL
        Dimension panDialSize = new Dimension(60, 60);
        panDial.setPreferredSize(panDialSize);
        panDial.setMinimumSize(panDialSize);
        panDial.setMaximumSize(panDialSize);
        
        JPanel panPanel = new JPanel(new BorderLayout(5, 2));
        JLabel panLabel = new JLabel("Pan", JLabel.CENTER);
        panPanel.add(panLabel, BorderLayout.NORTH);
        panPanel.add(panDial, BorderLayout.CENTER);
        panel.add(panPanel);
        
        return panel;
    }
    
    /**
     * Creates the modulation panel with velocity controls first, then swing, probability, etc.
     */
    private JPanel createModulationPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Modulation"));
        
        // 1. VELOCITY CONTROLS (added first)
        panel.add(createLabeledSlider("Min Vel", velocityMinSlider));
        panel.add(createLabeledSlider("Max Vel", velocityMaxSlider));
        
        // 2. OTHER MODULATION CONTROLS
        panel.add(createLabeledSlider("Swing", swingSlider));
        panel.add(createLabeledSlider("Probability", probabilitySlider));
        panel.add(createLabeledSlider("Random", randomSlider));
        panel.add(createLabeledSlider("Sparse", sparseSlider));
        
        return panel;
    }
    
    /**
     * Creates the ratchet panel with count and interval controls
     */
    private JPanel createRatchetPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Ratchet"));
        
        // Create panel for sliders with horizontal layout
        JPanel slidersPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        
        // Add count slider with label
        JPanel countPanel = new JPanel(new BorderLayout(2, 2));
        JLabel countLabel = new JLabel("Count", JLabel.CENTER);
        countPanel.add(countLabel, BorderLayout.NORTH);
        countPanel.add(ratchetCountSlider, BorderLayout.CENTER);
        slidersPanel.add(countPanel);
        
        // Add interval slider with label
        JPanel intervalPanel = new JPanel(new BorderLayout(2, 2));
        JLabel intervalLabel = new JLabel("Interval", JLabel.CENTER);
        intervalPanel.add(intervalLabel, BorderLayout.NORTH);
        intervalPanel.add(ratchetIntervalSlider, BorderLayout.CENTER);
        slidersPanel.add(intervalPanel);
        
        panel.add(slidersPanel, BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Sets up action listeners for buttons and sliders
     */
    private void setupActionListeners() {
        // Octave navigation - updated to use NoteSelectionDial directly
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Move note up an octave
                int currentOctave = noteDial.getOctave();
                noteDial.setOctaveOnly(currentOctave + 1, true);
                
                // Log the change
                logger.debug("Octave up: {}", noteDial.getNoteWithOctave());
            }
        });
        
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Move note down an octave
                int currentOctave = noteDial.getOctave();
                noteDial.setOctaveOnly(currentOctave - 1, true);
                
                // Log the change
                logger.debug("Octave down: {}", noteDial.getNoteWithOctave());
            }
        });
        
        // Note dial changes should publish the new note value
        noteDial.addChangeListener(e -> {
            int value = noteDial.getValue();
            // Publish the new note value with player ID
            CommandBus.getInstance().publish(Commands.NEW_VALUE_NOTE, this, 
                new Object[] { player.getId(), (long)value });
            
            // Show the note name in logs
            logger.debug("Note changed: {} (MIDI: {})", 
                noteDial.getNoteWithOctave(), value);
        });
        
        // Add pan dial change listener
        panDial.addChangeListener(e -> {
            int value = panDial.getValue();
            // Publish the new pan value with player ID
            CommandBus.getInstance().publish(Commands.NEW_VALUE_PAN, this, 
                new Object[] { player.getId(), (long)value });
            
            logger.debug("Pan changed: {}", value);
        });
    }
    
    /**
     * Updates the player object with current control values
     */
    public void updatePlayer() {
        player.setLevel((long) levelSlider.getValue());
        player.setNote((long) noteDial.getValue());
        player.setMinVelocity((long) velocityMinSlider.getValue());
        player.setMaxVelocity((long) velocityMaxSlider.getValue());
        player.setPanPosition((long) panDial.getValue());  // Changed from panSlider to panDial
        
        player.setSwing((long) swingSlider.getValue());
        player.setProbability((long) probabilitySlider.getValue());
        player.setRandomDegree((long) randomSlider.getValue());
        player.setSparse(((double) sparseSlider.getValue()) / 100.0);
        
        player.setRatchetCount((long) ratchetCountSlider.getValue());
        player.setRatchetInterval((long) ratchetIntervalSlider.getValue());
        
        logger.debug("Updated player parameters: level={}, note={}, swing={}", 
                player.getLevel(), player.getNote(), player.getSwing());
    }
    
    /**
     * Creates a vertical slider with the given parameters
     */
    private JSlider createSlider(String name, Long value, int min, int max) {
        // Handle null values safely
        int safeValue;
        if (value == null) {
            logger.error(name + " value is null, using default: " + min);
            safeValue = min;
        } else {
            // Clamp to valid range
            safeValue = (int) Math.max(min, Math.min(max, value));
            
            // Debug logging
            if (safeValue != value) {
                logger.error(String.format("%s value %d out of range [%d-%d], clamped to %d", 
                        name, value, min, max, safeValue));
            }
        }
        
        JSlider slider = new JSlider(SwingConstants.VERTICAL, min, max, safeValue);
        slider.setPreferredSize(new Dimension(20, SLIDER_HEIGHT));
        slider.setMinimumSize(new Dimension(20, SLIDER_HEIGHT));
        slider.setMaximumSize(new Dimension(20, SLIDER_HEIGHT));
        
        return slider;
    }
    
    /**
     * Creates a vertical slider with major tick marks
     */
    private JSlider createSlider(String name, long value, int min, int max, boolean setMajorTickSpacing) {
        JSlider slider = createSlider(name, value, min, max);
        if (setMajorTickSpacing) {
            slider.setMajorTickSpacing((max - min) / 4);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            slider.setSnapToTicks(true);
        }
        return slider;
    }
    
    /**
     * Creates a panel containing a labeled slider
     */
    private JPanel createLabeledSlider(String label, JSlider slider) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        JLabel labelComponent = new JLabel(label, JLabel.CENTER);
        labelComponent.setFont(new Font(labelComponent.getFont().getName(), Font.PLAIN, 11));
        panel.add(labelComponent, BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }
}