package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.NoteSelectionDial;
import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.service.UserConfigManager;

public class PlayerEditPanel extends StatusProviderPanel {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEditPanel.class.getName());
    private final Player player;

    // Basic properties
    private final JTextField nameField;
    private JComboBox<Instrument> instrumentCombo;
    private final JSpinner channelSpinner; // Changed from Dial to JSpinner
    private final JSpinner presetSpinner; // Changed from Dial to JSpinner

    // Change Dial fields to JSlider
    private final JSlider swingSlider;
    private final JSlider levelSlider;
    private final JSlider noteSlider;
    private final JSlider velocityMinSlider;
    private final JSlider velocityMaxSlider;
    private final JSlider probabilitySlider;
    private final JSlider randomSlider;
    private final JSlider panSlider;
    private final JSlider sparseSlider;

    // Ratchet controls
    private final JSlider ratchetCountSlider; // Changed from JSpinner to JSlider
    private final JSlider ratchetIntervalSlider; // Changed from JSpinner to JSlider

    // Toggle switches
    private final ToggleSwitch stickyPresetSwitch;
    private final ToggleSwitch useInternalBeatsSwitch;
    private final ToggleSwitch useInternalBarsSwitch;
    private final ToggleSwitch preserveOnPurgeSwitch;

    // Rules table
    private JTable rulesTable;
    private final JButton addRuleButton;
    private final JButton editRuleButton;
    private final JButton deleteRuleButton;

    // Add these fields near the top with other UI components
    private final JButton prevButton;
    private final JButton nextButton;

    // Modify the constructor to fix initialization sequence
    public PlayerEditPanel(Player player) {
        super(new BorderLayout());
        this.player = player; // Store player reference immediately

        // Initialize UI components
        nameField = new JTextField(player.getName());
        channelSpinner = new JSpinner(createLongSpinnerModel(player.getChannel(), 0, 15, 1));
        presetSpinner = new JSpinner(createLongSpinnerModel(player.getPreset(), 0, 127, 1));

        // Initialize all sliders
        swingSlider = createSlider("Swing", player.getSwing(), 0, 100);
        levelSlider = createSlider("Level", player.getLevel(), 0, 100);
        noteSlider = createSlider("Note", player.getNote(), 0, 127);
        velocityMinSlider = createSlider("Min Velocity", player.getMinVelocity(), 0, 127);
        velocityMaxSlider = createSlider("Max Velocity", player.getMaxVelocity(), 0, 127);
        probabilitySlider = createSlider("Probability", player.getProbability(), 0, 100);
        randomSlider = createSlider("Random", player.getRandomDegree(), 0, 100);
        panSlider = createSlider("Pan", player.getPanPosition(), -64, 63);
        sparseSlider = createSlider("Sparse", (long) (player.getSparse() * 100), 0, 100);

        // Ratchet controls with tick spacing
        ratchetCountSlider = createSlider("Count", player.getRatchetCount(), 0, 6, true);
        ratchetIntervalSlider = createSlider("Interval", player.getRatchetInterval(), 1, 16, true);

        // Initialize toggle switches
        stickyPresetSwitch = createToggleSwitch("Sticky Preset", player.getStickyPreset());
        useInternalBeatsSwitch = createToggleSwitch("Internal Beats", player.getUseInternalBeats());
        useInternalBarsSwitch = createToggleSwitch("Internal Bars", player.getUseInternalBars());
        preserveOnPurgeSwitch = createToggleSwitch("Preserve", player.getPreserveOnPurge());

        // Initialize table and buttons
        rulesTable = new JTable();
        addRuleButton = new JButton("Add");
        editRuleButton = new JButton("Edit");
        deleteRuleButton = new JButton("Delete");
        prevButton = new JButton("▲");
        nextButton = new JButton("▼");

        // Setup instrument combo
        setupInstrumentCombo();

        // Layout all components
        layoutComponents();

        // Now setup the rules table AFTER all components are initialized
        setupRulesTable();

        // Debug player rules
        debugPlayerState();

        // The rest of your constructor...

        // Rest of initialization
        setPreferredSize(new Dimension(800, 500));

        // Register command listeners
        // ...existing command listener code...

        // Add debugging to verify player and rules
        logger.info(
                player != null
                        ? "PlayerEditPanel initialized for player: " + player.getName() + " with "
                                + (player.getRules() != null ? player.getRules().size() : 0) + " rules"
                        : "Player is null");

        // Register for rule-related commands
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                String cmd = action.getCommand();

                // Use traditional if/else instead of pattern matching
                if (Commands.RULE_ADDED.equals(cmd) || Commands.RULE_EDITED.equals(cmd)
                        || Commands.RULE_DELETED.equals(cmd)) {

                    // If a rule was added/edited/deleted for any player
                    if (player != null) {
                        // Refresh player from session to get latest rules
                        Player updatedPlayer = SessionManager.getInstance().getActiveSession()
                                .getPlayer(player.getId());
                        if (updatedPlayer != null) {
                            // Update our player reference with latest rules
                            player.setRules(updatedPlayer.getRules());
                            updateRulesTable();
                        }
                    }
                }
            }
        });

        // Add this in the constructor after other initializations
        registerForInstrumentUpdates();
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for basic controls - now using GridBagLayout for two rows
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Basic Properties"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // First row
        gbc.gridy = 0;
        gbc.gridx = 0;
        topPanel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(nameField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        topPanel.add(new JLabel("Instrument:"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        topPanel.add(instrumentCombo, gbc);

        // Second row
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        topPanel.add(new JLabel("Channel:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        topPanel.add(channelSpinner, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        topPanel.add(new JLabel("Preset:"), gbc);
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        topPanel.add(presetSpinner, gbc);

        add(topPanel, BorderLayout.NORTH);

        // Main content with parameters and rules
        JPanel mainContent = new JPanel(new BorderLayout());

        // Performance controls panel with size constraints
        JPanel performancePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        performancePanel.setBorder(BorderFactory.createTitledBorder("Performance"));
        performancePanel.setPreferredSize(new Dimension(400, 150));
        performancePanel.setMaximumSize(new Dimension(400, 150));

        var dialSize = new Dimension(85, 85);
        NoteSelectionDial noteDial = new NoteSelectionDial();
        noteDial.setPreferredSize(dialSize);
        noteDial.setMinimumSize(dialSize);
        noteDial.setMaximumSize(dialSize);
        noteDial.setCommand(Commands.NEW_VALUE_NOTE);

        performancePanel.add(noteDial);

        // Performance controls panel layout
        performancePanel.add(createLabeledSlider("Level", levelSlider));
        performancePanel.add(createLabeledSlider("Note", noteSlider));

        // Add navigation buttons panel with label
        JPanel navPanel = new JPanel(new BorderLayout(0, 2));
        navPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5)); // Add margins
        JLabel octaveLabel = new JLabel("Octave", JLabel.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        prevButton.setPreferredSize(new Dimension(35, 35)); // Reduce from 40x40 to 35x35
        nextButton.setPreferredSize(new Dimension(35, 35));
        buttonPanel.add(prevButton);
        buttonPanel.add(nextButton);

        navPanel.add(octaveLabel, BorderLayout.NORTH);
        navPanel.add(buttonPanel, BorderLayout.CENTER);
        performancePanel.add(navPanel);

        // Continue with remaining sliders
        performancePanel.add(createLabeledSlider("Min Vel", velocityMinSlider));
        performancePanel.add(createLabeledSlider("Max Vel", velocityMaxSlider));
        performancePanel.add(createLabeledSlider("Pan", panSlider));

        // Modulation controls panel with size constraints
        JPanel modulationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        modulationPanel.setBorder(BorderFactory.createTitledBorder("Modulation"));
        modulationPanel.setPreferredSize(new Dimension(400, 80));
        modulationPanel.setMaximumSize(new Dimension(400, 80));
        modulationPanel.add(createLabeledSlider("Swing", swingSlider));
        modulationPanel.add(createLabeledSlider("Probability", probabilitySlider));
        modulationPanel.add(createLabeledSlider("Random", randomSlider));
        modulationPanel.add(createLabeledSlider("Sparse", sparseSlider));

        // Create a container with GridBagLayout for all panels
        JPanel controlsContainer = new JPanel(new GridBagLayout());
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(2, 2, 2, 2);

        // Left column: Performance and Modulation stacked vertically
        JPanel leftColumn = new JPanel(new GridLayout(2, 1, 0, 5));
        leftColumn.add(performancePanel);
        leftColumn.add(modulationPanel);

        // Add left column
        gbc2.gridx = 0;
        gbc2.gridy = 0;
        gbc2.weightx = 0.8;
        gbc2.weighty = 1.0;
        gbc2.fill = GridBagConstraints.BOTH;
        controlsContainer.add(leftColumn, gbc2);

        // Create and add ratchet panel
        JPanel ratchetPanel = new JPanel(new BorderLayout(5, 5));
        ratchetPanel.setBorder(BorderFactory.createTitledBorder("Ratchet"));
        ratchetPanel.setPreferredSize(new Dimension(120, 160)); // Match height of left column

        // Create panel for sliders with vertical layout
        JPanel slidersPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        // Add count slider with label in its own panel
        JPanel countPanel = new JPanel(new BorderLayout(2, 2));
        JLabel countLabel = new JLabel("Count", JLabel.CENTER);
        countPanel.add(countLabel, BorderLayout.NORTH);
        countPanel.add(ratchetCountSlider, BorderLayout.CENTER);
        slidersPanel.add(countPanel);

        // Add interval slider with label in its own panel
        JPanel intervalPanel = new JPanel(new BorderLayout(2, 2));
        JLabel intervalLabel = new JLabel("Interval", JLabel.CENTER);
        intervalPanel.add(intervalLabel, BorderLayout.NORTH);
        intervalPanel.add(ratchetIntervalSlider, BorderLayout.CENTER);
        slidersPanel.add(intervalPanel);

        ratchetPanel.add(slidersPanel, BorderLayout.CENTER);

        // Add ratchet panel to the right
        gbc2.gridx = 1;
        gbc2.weightx = 0.2;
        controlsContainer.add(ratchetPanel, gbc2);

        mainContent.add(controlsContainer, BorderLayout.CENTER);

        // Options panel with improved toggle switch layout
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));

        // Add switches with labels in vertical panels
        optionsPanel.add(createLabeledSwitch("Sticky Preset", stickyPresetSwitch));
        optionsPanel.add(createLabeledSwitch("Internal Beats", useInternalBeatsSwitch));
        optionsPanel.add(createLabeledSwitch("Internal Bars", useInternalBarsSwitch));
        optionsPanel.add(createLabeledSwitch("Preserve", preserveOnPurgeSwitch));

        mainContent.add(optionsPanel, BorderLayout.SOUTH);

        // Rules panel
        setupRulesPanel();

        // Combine parameters and rules with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainContent, createRulesPanel());
        splitPane.setResizeWeight(0.7);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createRulesPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Rules"));

        // Move buttons to the top
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addRuleButton);
        buttonPanel.add(editRuleButton);
        buttonPanel.add(deleteRuleButton);
        panel.add(buttonPanel, BorderLayout.NORTH);

        // Add table below buttons
        panel.add(new JScrollPane(rulesTable), BorderLayout.CENTER);

        // Add this to your createRulesPanel() method
        JButton debugButton = new JButton("Debug");
        debugButton.addActionListener(e -> {
            debugPlayerState();
            updateRulesTable(); // Force refresh
        });
        buttonPanel.add(debugButton);

        return panel;
    }

    // Fix the getUpdatedPlayer() method with proper null checking
    public Player getUpdatedPlayer() {
        // Get currently selected instrument from combo box
        Instrument selectedInstrument = (Instrument) instrumentCombo.getSelectedItem();

        // Ensure the device setting is preserved when returning the updated player
        if (selectedInstrument != null) {
            player.setInstrument(selectedInstrument);
        }

        // Only set instrument and ID if an instrument is actually selected
        if (selectedInstrument != null) {
            player.setInstrument(selectedInstrument);
            player.setInstrumentId(selectedInstrument.getId());
            logger.debug("Selected instrument: " + selectedInstrument.getName() + " (ID: " + selectedInstrument.getId()
                    + ")");
        } else {
            // Log warning and keep existing instrument (if any)
            logger.error("No instrument selected in combo box");
        }

        // These operations are safe as spinners always have values
        player.setChannel(((Number) channelSpinner.getValue()).intValue());
        player.setPreset(((Number) presetSpinner.getValue()).longValue());

        // Set other slider values
        player.setLevel((long) levelSlider.getValue());
        player.setNote((long) noteSlider.getValue());
        player.setSwing((long) swingSlider.getValue());
        player.setMinVelocity((long) velocityMinSlider.getValue());
        player.setMaxVelocity((long) velocityMaxSlider.getValue());
        player.setProbability((long) probabilitySlider.getValue());
        player.setRandomDegree((long) randomSlider.getValue());
        player.setPanPosition((long) panSlider.getValue());
        player.setSparse(((double) sparseSlider.getValue()) / 100.0);
        player.setRatchetCount((long) ratchetCountSlider.getValue());
        player.setRatchetInterval((long) ratchetIntervalSlider.getValue());
        player.setName(nameField.getText());

        // Set toggle switch values
        player.setStickyPreset(stickyPresetSwitch.isSelected());
        player.setUseInternalBeats(useInternalBeatsSwitch.isSelected());
        player.setUseInternalBars(useInternalBarsSwitch.isSelected());
        player.setPreserveOnPurge(preserveOnPurgeSwitch.isSelected());

        return player;
    }

    // Helper methods for creating components
    private void setupInstrumentCombo() {
        instrumentCombo = new JComboBox<>();

        // CHANGE: Use UserConfigManager instead of SessionManager.getInstrumentEngine()
        List<Instrument> instruments = UserConfigManager.getInstance().getInstruments();

        if (instruments == null || instruments.isEmpty()) {
            logger.error("No instruments found in UserConfigManager");
            // Add a default instrument to prevent null selections
            Instrument defaultInstrument = new Instrument();
            defaultInstrument.setId(0L);
            defaultInstrument.setName("Default Instrument");
            instrumentCombo.addItem(defaultInstrument);
        } else {
            // Sort instruments by name
            instruments.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

            for (Instrument inst : instruments) {
                if (inst.getAvailable())
                    instrumentCombo.addItem(inst);
            }
        }

        // Select the player's instrument if it exists
        if (player.getInstrument() != null) {
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                Instrument item = instrumentCombo.getItemAt(i);
                if (item.getId().equals(player.getInstrument().getId())) {
                    instrumentCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    static int SLIDER_HEIGHT = 80;

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
                logger.error(String.format("%s value %d out of range [%d-%d], clamped to %d", name, value, min, max,
                        safeValue));
            }
        }

        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, safeValue);
        slider.setPreferredSize(new Dimension(20, SLIDER_HEIGHT));
        slider.setMinimumSize(new Dimension(20, SLIDER_HEIGHT));
        slider.setMaximumSize(new Dimension(20, SLIDER_HEIGHT));
        return slider;
    }

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

    private ToggleSwitch createToggleSwitch(String name, boolean value) {
        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(value);
        toggle.setPreferredSize(new Dimension(60, 30));
        return toggle;
    }

    private JPanel createLabeledSlider(String label, JSlider slider) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        panel.add(new JLabel(label, JLabel.CENTER), BorderLayout.NORTH);
        panel.add(slider, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createLabeledSwitch(String label, ToggleSwitch toggle) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        panel.add(new JLabel(label, JLabel.CENTER), BorderLayout.NORTH);
        panel.add(toggle, BorderLayout.CENTER);
        return panel;
    }

    private void setupRulesPanel() {
        // No need to create JPanel here as createRulesPanel() already does this

        // Add button handlers
        addRuleButton.addActionListener(e -> CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, player));

        editRuleButton.addActionListener(e -> {
            Rule selectedRule = getSelectedRule();
            if (selectedRule != null) {
                CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
            }
        });

        deleteRuleButton.addActionListener(e -> {
            Rule selectedRule = getSelectedRule();
            if (selectedRule != null) {
                CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this, new Rule[] { selectedRule });
            }
        });

        // Listen for rule updates
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                if (Commands.PLAYER_UPDATED.equals(action.getCommand())) {
                    if (action.getData() instanceof Player) {
                        Player updatedPlayer = (Player) action.getData();
                        if (updatedPlayer.getId().equals(player.getId())) {
                            player.setRules(updatedPlayer.getRules());
                            updateRulesTable();
                            updateButtonStates();
                        }
                    }
                }
            }
        });
    }

    private Rule getSelectedRule() {
        int row = rulesTable.getSelectedRow();
        if (row >= 0 && player != null && player.getRules() != null) {
            try {
                // Get all rules as a list for indexed access
                List<Rule> rulesList = new ArrayList<>(player.getRules());

                // Get the rule at the selected row
                if (row < rulesList.size()) {
                    return rulesList.get(row);
                } else {
                    logger.error("Selected row " + row + " is out of bounds (rules size: " + rulesList.size() + ")");
                }
            } catch (Exception e) {
                logger.error("Error accessing rule at row " + row + ": " + e.getMessage());
            }
        }
        return null;
    }

    // Thoroughly fix updateRulesTable method to debug and handle all edge cases
    private void updateRulesTable() {
        try {
            // Debug output
            logger.info("Updating rules table for player: " + (player != null ? player.getName() : "null"));

            // Basic validation
            if (rulesTable == null || player == null || player.getRules() == null) {
                logger.error("Cannot update rules table - " + (rulesTable == null ? "table is null"
                        : player == null ? "player is null" : "player rules is null"));
                return;
            }

            // Get table model
            DefaultTableModel model = (DefaultTableModel) rulesTable.getModel();
            model.setRowCount(0); // Clear existing rows

            // Log rule count
            logger.info("Player " + player.getName() + " has " + player.getRules().size() + " rules");

            // Add each rule to the table
            for (Rule rule : player.getRules()) {
                if (rule == null)
                    continue;

                try {
                    // Get display text for rule properties with CORRECT VARIABLE NAMES
                    // Rule.OPERATORS = {"Beat", "Tick", "Bar", ...} - what we're comparing
                    String operatorText = rule.getOperator() >= 0 && rule.getOperator() < Rule.OPERATORS.length
                            ? Rule.OPERATORS[rule.getOperator()]
                            : "Unknown";

                    // Rule.COMPARISONS = {"==", "<", ">", ...} - how we're comparing
                    String comparisonText = rule.getComparison() >= 0 && rule.getComparison() < Rule.COMPARISONS.length
                            ? Rule.COMPARISONS[rule.getComparison()]
                            : "Unknown";

                    String partText = rule.getPart() == 0 ? "All" : String.valueOf(rule.getPart());

                    // SWAP the order to match column headers
                    model.addRow(new Object[] { operatorText, // Column 0: "Comparison" (Beat, Tick, etc)
                            comparisonText, // Column 1: "Operator" (==, <, >, etc)
                            rule.getValue(), // Column 2: Value
                            partText // Column 3: Part
                    });
                } catch (IllegalFormatConversionException e) {
                    logger.error("Format conversion error: " + e.getMessage());

                    // Use safer string conversion with SWAPPED order
                    model.addRow(
                            new Object[] { rule.getOperator() >= 0 ? Rule.OPERATORS[rule.getOperator()] : "Unknown", // Column
                                                                                                                     // 0:
                                                                                                                     // "Comparison"
                                    rule.getComparison() >= 0 ? Rule.COMPARISONS[rule.getComparison()] : "Unknown", // Column
                                                                                                                    // 1:
                                                                                                                    // "Operator"
                                    String.valueOf(rule.getValue()), // Column 2: Value
                                    rule.getPart() == 0 ? "All" : String.valueOf(rule.getPart()) // Column 3: Part
                            });
                }
            }

            // Refresh display
            rulesTable.revalidate();
            rulesTable.repaint();
        } catch (Exception e) {
            logger.error("Error updating rules table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add a debug method to check player state
    private void debugPlayerState() {
        if (player == null) {
            logger.error("Player is null");
            return;
        }

        logger.info("Player: " + player.getName() + " (ID: " + player.getId() + ")");
        logger.info("Rules count: " + (player.getRules() != null ? player.getRules().size() : "NULL"));

        if (player.getRules() != null) {
            int i = 0;
            for (Rule rule : player.getRules()) {
                logger.info("Rule " + (i++) + ": " + rule);
            }
        }
    }

    // Replace BOTH setupRulesTable methods with this single implementation
    private void setupRulesTable() {
        // Debug output
        logger.info("Setting up rules table for player: " + player.getName());

        // Define column names in the CORRECT order to match the data order used in
        // updateRulesTable
        String[] columnNames = { "Comparison", "Operator", "Value", "Part" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);

        // Apply model to table
        rulesTable.setModel(model);
        rulesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Configure cell renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        // Apply renderers - first column left aligned, others centered
        rulesTable.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);
        for (int i = 1; i < rulesTable.getColumnCount(); i++) {
            rulesTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Add selection listener for button state management
        rulesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
            }
        });

        // Add double-click handler
        rulesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
            }
        });

        // Load initial rules - this is crucial
        updateRulesTable();

        // Initialize button states
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = rulesTable.getSelectedRow() >= 0;
        editRuleButton.setEnabled(hasSelection);
        deleteRuleButton.setEnabled(hasSelection);
    }

    private SpinnerNumberModel createLongSpinnerModel(long value, long min, long max, long step) {
        return new SpinnerNumberModel(value, min, max, step);
    }

    private void registerForInstrumentUpdates() {
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                // Listen for instrument changes
                if (Commands.INSTRUMENT_UPDATED.equals(action.getCommand())
                        || Commands.USER_CONFIG_LOADED.equals(action.getCommand())) {

                    // Refresh the instrument combo
                    SwingUtilities.invokeLater(() -> {
                        // Remember selected instrument
                        Instrument selected = (Instrument) instrumentCombo.getSelectedItem();

                        // Update combo with fresh instruments
                        instrumentCombo.removeAllItems();
                        List<Instrument> instruments = UserConfigManager.getInstance().getInstruments();

                        if (instruments != null && !instruments.isEmpty()) {
                            instruments.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

                            for (Instrument inst : instruments) {
                                instrumentCombo.addItem(inst);
                            }

                            // Restore selection if possible
                            if (selected != null) {
                                for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                                    Instrument item = instrumentCombo.getItemAt(i);
                                    if (item.getId().equals(selected.getId())) {
                                        instrumentCombo.setSelectedIndex(i);
                                        break;
                                    }
                                }
                            }
                        }
                    });
                }
            }
        });
    }
}
