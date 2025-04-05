package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
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
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;

/**
 * Panel containing detailed player controls for performance, modulation, and
 * ratcheting. This panel is used inside PlayerEditPanel to manage the middle
 * section of controls.
 */
public class PlayerEditDetailPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PlayerEditDetailPanel.class);
    private static final int SLIDER_HEIGHT = 80;
    private static final int PANEL_HEIGHT = 125; 
    
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
        sparseSlider = createSlider("Sparse", (int) (player.getSparse() * 100), 0, 100);

        // Initialize ratchet controls with tick spacing
        ratchetCountSlider = createSlider("Count", player.getRatchetCount(), 0, 6, true);
        ratchetIntervalSlider = createSlider("Interval", player.getRatchetInterval(), 1, 16, true);

        // Set up the UI components
        setupLayout();
        setupActionListeners();
    }

    /**
     * Sets up the overall panel layout with Ratchet panel on the right side
     */
    private void setupLayout() {
        // Use BorderLayout for the main container to have left and right sections
        setLayout(new BorderLayout(10, 5));

        // Create left panel to stack Performance and Modulation panels vertically
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // Create and add performance panel to left stack
        JPanel performancePanel = createPerformancePanel();
        leftPanel.add(performancePanel);

        // Create and add modulation panel to left stack
        JPanel modulationPanel = createModulationPanel();
        leftPanel.add(modulationPanel);

        // Create ratchet panel for right side
        JPanel ratchetPanel = createRatchetPanel();

        // Add panels to main layout
        add(leftPanel, BorderLayout.CENTER);
        add(ratchetPanel, BorderLayout.EAST);
    }

    /**
     * Creates the performance panel with reordered controls: Octave, Note,
     * Level, Pan - with vertical centering and fixed height
     */
    private JPanel createPerformancePanel() {
        // Use GridBagLayout for better component positioning and centering
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Performance"));
        
        // Set fixed height for the panel
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, PANEL_HEIGHT));
        panel.setMinimumSize(new Dimension(0, PANEL_HEIGHT));
        
        // Create a flow sub-panel to hold the actual controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
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
        controlsPanel.add(navPanel);

        // 2. NOTE DIAL
        Dimension dialSize = new Dimension(100, 100);
        noteDial.setPreferredSize(dialSize);
        noteDial.setMinimumSize(dialSize);
        noteDial.setMaximumSize(dialSize);
        noteDial.setCommand(Commands.NEW_VALUE_NOTE);
        noteDial.setValue(player.getRootNote().intValue());

        JPanel notePanel = new JPanel(new BorderLayout(5, 2));
        JLabel noteLabel = new JLabel("Note", JLabel.CENTER);
        notePanel.add(noteLabel, BorderLayout.NORTH);
        notePanel.add(noteDial, BorderLayout.CENTER);
        controlsPanel.add(notePanel);

        // 3. LEVEL SLIDER
        controlsPanel.add(createLabeledSlider("Level", levelSlider));

        // 4. PAN DIAL
        Dimension panDialSize = new Dimension(60, 60);
        panDial.setPreferredSize(panDialSize);
        panDial.setMinimumSize(panDialSize);
        panDial.setMaximumSize(panDialSize);

        JPanel panPanel = new JPanel(new BorderLayout(5, 2));
        JLabel panLabel = new JLabel("Pan", JLabel.CENTER);
        panPanel.add(panLabel, BorderLayout.NORTH);
        panPanel.add(panDial, BorderLayout.CENTER);
        controlsPanel.add(panPanel);
        
        // Add the controls panel to the main panel, centered vertically
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; // Center vertically and horizontally
        gbc.weighty = 1.0; // Take all vertical space
        panel.add(controlsPanel, gbc);

        return panel;
    }

    /**
     * Creates the modulation panel with velocity controls first, then swing,
     * probability, etc. - with vertical centering and fixed height
     */
    private JPanel createModulationPanel() {
        // Use GridBagLayout for better component positioning and centering
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Modulation"));
        
        // Set fixed height for the panel - SAME HEIGHT as performance panel
        panel.setPreferredSize(new Dimension(panel.getPreferredSize().width, PANEL_HEIGHT));
        panel.setMinimumSize(new Dimension(0, PANEL_HEIGHT));
        
        // Create a flow sub-panel to hold the actual controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));

        // 1. VELOCITY CONTROLS (added first)
        controlsPanel.add(createLabeledSlider("Min Vel", velocityMinSlider));
        controlsPanel.add(createLabeledSlider("Max Vel", velocityMaxSlider));

        // 2. OTHER MODULATION CONTROLS
        controlsPanel.add(createLabeledSlider("Swing", swingSlider));
        controlsPanel.add(createLabeledSlider("Probability", probabilitySlider));
        controlsPanel.add(createLabeledSlider("Random", randomSlider));
        controlsPanel.add(createLabeledSlider("Sparse", sparseSlider));
        
        // Add the controls panel to the main panel, centered vertically
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; // Center vertically and horizontally
        gbc.weighty = 1.0; // Take all vertical space
        panel.add(controlsPanel, gbc);

        return panel;
    }

    /**
     * Creates the ratchet panel with count and interval controls Redesigned to
     * be vertically oriented
     */
    private JPanel createRatchetPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Ratchet"));

        // Create panel for sliders with vertical layout
        JPanel slidersPanel = new JPanel();
        slidersPanel.setLayout(new BoxLayout(slidersPanel, BoxLayout.Y_AXIS));

        // Set preferred width for ratchet panel
        panel.setPreferredSize(new Dimension(120, 400));

        // Add count slider with label
        JPanel countPanel = new JPanel(new BorderLayout(2, 5));
        JLabel countLabel = new JLabel("Count", JLabel.CENTER);
        countPanel.add(countLabel, BorderLayout.NORTH);

        // Make slider horizontal for this layout
        ratchetCountSlider.setOrientation(SwingConstants.HORIZONTAL);
        ratchetCountSlider.setPreferredSize(new Dimension(100, 50));
        countPanel.add(ratchetCountSlider, BorderLayout.CENTER);

        // Add interval slider with label
        JPanel intervalPanel = new JPanel(new BorderLayout(2, 5));
        JLabel intervalLabel = new JLabel("Interval", JLabel.CENTER);
        intervalPanel.add(intervalLabel, BorderLayout.NORTH);

        // Make slider horizontal for this layout
        ratchetIntervalSlider.setOrientation(SwingConstants.HORIZONTAL);
        ratchetIntervalSlider.setPreferredSize(new Dimension(100, 50));
        intervalPanel.add(ratchetIntervalSlider, BorderLayout.CENTER);

        // Add spacing between components
        slidersPanel.add(Box.createVerticalStrut(20));
        slidersPanel.add(countPanel);
        slidersPanel.add(Box.createVerticalStrut(30));
        slidersPanel.add(intervalPanel);
        slidersPanel.add(Box.createVerticalStrut(20));

        // Center the sliders panel
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
                    new Object[]{player.getId(), (long) value});

            // Show the note name in logs
            logger.debug("Note changed: {} (MIDI: {})",
                    noteDial.getNoteWithOctave(), value);
        });

        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getSender() != this && action.getCommand() == Commands.NEW_VALUE_NOTE) {
                    noteDial.setValue((int) action.getData(), false);
                }
            }
        });

        // Add pan dial change listener
        panDial.addChangeListener(e
                -> {
            int value = panDial.getValue();
            // Publish the new pan value with player ID
            CommandBus.getInstance().publish(Commands.NEW_VALUE_PAN, this,
                    new Object[]{player.getId(), (long) value});

            logger.debug("Pan changed: {}", value);
        }
        );

        // Add velocity min slider change listener
        velocityMinSlider.addChangeListener(e
                -> {
            if (!velocityMinSlider.getValueIsAdjusting()) {
                int minVelocity = velocityMinSlider.getValue();
                int maxVelocity = velocityMaxSlider.getValue();

                // Ensure min value is never greater than max value
                if (minVelocity > maxVelocity) {
                    // Set max value equal to min value
                    velocityMaxSlider.setValue(minVelocity);
                    maxVelocity = minVelocity;
                }

                // Publish the new min velocity with player ID
                CommandBus.getInstance().publish(Commands.NEW_VALUE_VELOCITY_MIN, this,
                        new Object[]{player.getId(), (long) minVelocity});

                logger.debug("Min velocity changed: {} (max: {})", minVelocity, maxVelocity);
            }
        }
        );

        // Add velocity max slider change listener
        velocityMaxSlider.addChangeListener(e
                -> {
            if (!velocityMaxSlider.getValueIsAdjusting()) {
                int minVelocity = velocityMinSlider.getValue();
                int maxVelocity = velocityMaxSlider.getValue();

                // Ensure max value is never less than min value
                if (maxVelocity < minVelocity) {
                    // Set min value equal to max value
                    velocityMinSlider.setValue(maxVelocity);
                    minVelocity = maxVelocity;
                }

                // Publish the new max velocity with player ID
                CommandBus.getInstance().publish(Commands.NEW_VALUE_VELOCITY_MAX, this,
                        new Object[]{player.getId(), (long) maxVelocity});

                logger.debug("Max velocity changed: {} (min: {})", maxVelocity, minVelocity);
            }
        }
        );

        // Add ratchet count slider change listener
        ratchetCountSlider.addChangeListener(e
                -> {
            if (!ratchetCountSlider.getValueIsAdjusting()) {
                int value = ratchetCountSlider.getValue();
                // Publish the new ratchet count with player ID
                CommandBus.getInstance().publish(Commands.NEW_VALUE_RATCHET_COUNT, this,
                        new Object[]{player.getId(), (long) value});

                logger.debug("Ratchet count changed: {}", value);
            }
        }
        );

        // Add ratchet interval slider change listener
        ratchetIntervalSlider.addChangeListener(e
                -> {
            if (!ratchetIntervalSlider.getValueIsAdjusting()) {
                int value = ratchetIntervalSlider.getValue();
                // Publish the new ratchet interval with player ID
                CommandBus.getInstance().publish(Commands.NEW_VALUE_RATCHET_INTERVAL, this,
                        new Object[]{player.getId(), (long) value});

                logger.debug("Ratchet interval changed: {}", value);
            }
        }
        );
    }

    /**
     * Updates the player object with current control values
     */
    public void updatePlayer() {
        player.setLevel(levelSlider.getValue());
        player.setRootNote(noteDial.getValue());
        player.setMinVelocity(velocityMinSlider.getValue());
        player.setMaxVelocity(velocityMaxSlider.getValue());
        player.setPanPosition(panDial.getValue());  // Changed from panSlider to panDial

        player.setSwing(swingSlider.getValue());
        player.setProbability(probabilitySlider.getValue());
        player.setRandomDegree(randomSlider.getValue());
        player.setSparse(((double) sparseSlider.getValue()) / 100.0);

        player.setRatchetCount(ratchetCountSlider.getValue());
        player.setRatchetInterval(ratchetIntervalSlider.getValue());

        logger.debug("Updated player parameters: level={}, note={}, swing={}",
                player.getLevel(), player.getRootNote(), player.getSwing());
    }

    /**
     * Creates a vertical slider with the given parameters
     */
    private JSlider createSlider(String name, Integer value, int min, int max) {
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
    private JSlider createSlider(String name, int value, int min, int max, boolean setMajorTickSpacing) {
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
