package com.angrysurfer.beats.widget.panel;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.service.UserConfigManager;

public class PlayerEditPanel extends StatusProviderPanel {
    private static final Logger logger = LoggerFactory.getLogger(PlayerEditPanel.class.getName());
    private final Player player;

    // Basic properties
    private final JTextField nameField;
    private JComboBox<Instrument> instrumentCombo;
    private final JSpinner channelSpinner; // Changed from Dial to JSpinner
    private JSpinner presetSpinner; // Changed from Dial to JSpinner

    // Replace all these slider and button fields with just the detail panel
    private final PlayerEditDetailPanel detailPanel;

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

    // First, add a new field for the preset combo box
    private JComboBox presetCombo; // Changed type to more flexible JComboBox
    private JPanel presetControlPanel; // Panel to swap between spinner and combo
    private boolean usingInternalSynth = false;

    // Add these fields to PlayerEditPanel class
    private JSpinner internalBarsSpinner;
    private JSpinner internalBeatsSpinner;

    // Add a new flag to track drum channel mode
    private boolean isDrumChannel = false;

    // Helper class to represent presets in the combo box
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

    // Add a helper class for drum items
    private static class DrumItem {
        private final int noteNumber;
        private final String name;

        public DrumItem(int noteNumber, String name) {
            this.noteNumber = noteNumber;
            this.name = name;
        }

        public int getNoteNumber() {
            return noteNumber;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name + " [" + noteNumber + "]";
        }
    }

    // Modify the constructor to fix initialization sequence
    public PlayerEditPanel(Player player) {
        super(new BorderLayout());
        this.player = player; // Store player reference immediately

        // Initialize UI components
        nameField = new JTextField(player.getName());
        channelSpinner = new JSpinner(createLongSpinnerModel(player.getChannel(), 0, 15, 1));
        presetSpinner = new JSpinner(createLongSpinnerModel(player.getPreset(), 0, 127, 1));

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

        // Create the detail panel that contains all the sliders
        detailPanel = new PlayerEditDetailPanel(player);

        // Setup instrument combo
        setupInstrumentCombo();

        // Now setup preset controls based on the selected instrument
        setupPresetControls();

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
        topPanel.add(presetControlPanel, gbc);

        add(topPanel, BorderLayout.NORTH);

        // Main content with parameters and rules
        JPanel mainContent = new JPanel(new BorderLayout());

        // Add the detail panel instead of creating performance/modulation/ratchet
        // panels
        mainContent.add(detailPanel, BorderLayout.CENTER);

        // Replace the original options panel with our new one
        JPanel optionsPanel = createOptionsPanel();

        mainContent.add(optionsPanel, BorderLayout.SOUTH);

        // Rules panel
        setupRulesPanel();

        // Combine parameters and rules with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, mainContent, createRulesPanel());
        splitPane.setResizeWeight(1.0);
        add(splitPane, BorderLayout.CENTER);
    }

    private JPanel createOptionsPanel() {
        // Main options panel using FlowLayout with reduced gap spacing
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 3));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));

        // Initialize spinners with player values
        internalBeatsSpinner = new JSpinner(
                createLongSpinnerModel(player.getInternalBeats() != null ? player.getInternalBeats()
                        : SessionManager.getInstance().getActiveSession().getBeatsPerBar(), 1, 256, 1));
        internalBarsSpinner = new JSpinner(
                createLongSpinnerModel(player.getInternalBars() != null ? player.getInternalBars()
                        : SessionManager.getInstance().getActiveSession().getPartLength(), 1, 256, 1));
        
        // Make spinners smaller
        Dimension spinnerSize = new Dimension(50, 25);
        internalBeatsSpinner.setPreferredSize(spinnerSize);
        internalBarsSpinner.setPreferredSize(spinnerSize);
        
        // Create individual toggle switch panels
        JPanel preservePanel = createLabeledSwitch("Preserve", preserveOnPurgeSwitch);
        JPanel stickyPresetPanel = createLabeledSwitch("Sticky Preset", stickyPresetSwitch);

        // Internal Beats section - more compact FlowLayout 
        JPanel internalBeatsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 1));
        internalBeatsPanel.setBorder(BorderFactory.createTitledBorder("Internal Beats"));

        // Add components directly to the FlowLayout panel
        internalBeatsPanel.add(useInternalBeatsSwitch);
        internalBeatsPanel.add(internalBeatsSpinner);

        // Internal Bars section - more compact FlowLayout
        JPanel internalBarsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 1));
        internalBarsPanel.setBorder(BorderFactory.createTitledBorder("Internal Bars"));

        // Add components directly to the FlowLayout panel
        internalBarsPanel.add(useInternalBarsSwitch);
        internalBarsPanel.add(internalBarsSpinner);

        // Add all components to the options panel in one row
        optionsPanel.add(preservePanel);
        optionsPanel.add(stickyPresetPanel);
        optionsPanel.add(internalBeatsPanel);
        optionsPanel.add(internalBarsPanel);

        // Add state change listeners to enable/disable controls based on toggle state
        useInternalBeatsSwitch.addActionListener(e -> updateInternalBeatsControls());
        useInternalBarsSwitch.addActionListener(e -> updateInternalBarsControls());

        // Initialize control states
        updateInternalBeatsControls();
        updateInternalBarsControls();

        return optionsPanel;
    }

    private void updateInternalBeatsControls() {
        boolean enabled = useInternalBeatsSwitch.isSelected();
        internalBeatsSpinner.setEnabled(enabled);
    }

    private void updateInternalBarsControls() {
        boolean enabled = useInternalBarsSwitch.isSelected();
        internalBarsSpinner.setEnabled(enabled);
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
        } else {
            // Log warning and keep existing instrument (if any)
            logger.error("No instrument selected in combo box");
        }

        // Get channel from spinner
        int channelValue = ((Number) channelSpinner.getValue()).intValue();
        player.setChannel(channelValue);
        
        // Update isDrumChannel flag based on current channel
        isDrumChannel = (player.getChannel() == 9);

        // Get preset or note value based on active mode
        if (usingInternalSynth) {
            if (isDrumChannel && presetCombo.getSelectedItem() instanceof DrumItem) {
                // For drum channel, update the note value but keep preset at 0 (standard GM drums)
                DrumItem selectedDrum = (DrumItem) presetCombo.getSelectedItem();
                player.setRootNote((long) selectedDrum.getNoteNumber());
                player.setPreset(0L); // Standard GM drum set
            } 
            else if (presetCombo.getSelectedItem() instanceof PresetItem) {
                // For normal internal synth channels, update preset
                player.setPreset((long) ((PresetItem) presetCombo.getSelectedItem()).getNumber());
                
                // Don't clear note value here - it might be needed for other purposes
            }
        } 
        else {
            // For external instruments, use spinner value
            player.setPreset(((Number) presetSpinner.getValue()).longValue());
        }

        // Use detailPanel to update all the slider values
        detailPanel.updatePlayer();

        // Set other properties (name, toggle switches, etc.)
        player.setName(nameField.getText());
        player.setStickyPreset(stickyPresetSwitch.isSelected());
        player.setUseInternalBeats(useInternalBeatsSwitch.isSelected());
        player.setUseInternalBars(useInternalBarsSwitch.isSelected());
        player.setPreserveOnPurge(preserveOnPurgeSwitch.isSelected());
        player.setInternalBars(((Number) internalBarsSpinner.getValue()).intValue());
        player.setInternalBeats(((Number) internalBeatsSpinner.getValue()).intValue());

        return player;
    }

    // Helper methods for creating components
    private void setupInstrumentCombo() {
        instrumentCombo = new JComboBox<>();

        // Add custom renderer to display instrument names
        instrumentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof Instrument) {
                    Instrument instrument = (Instrument) value;
                    setText(instrument.getName());
                }
                return this;
            }
        });

        // Get instruments from UserConfigManager
        List<Instrument> instruments = UserConfigManager.getInstance().getInstruments();

        // Add internal synths from InternalSynthManager
        instruments.addAll(InternalSynthManager.getInstance().getInternalSynths());

        if (instruments == null || instruments.isEmpty()) {
            logger.error("No instruments found");
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

                    // Check if it's an internal synth
                    usingInternalSynth = InternalSynthManager.getInstance().isInternalSynth(item);
                    break;
                }
            }
        }

        // Add listener to update preset controls when instrument changes
        instrumentCombo.addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged")) {
                Instrument selectedInstrument = (Instrument) instrumentCombo.getSelectedItem();
                if (selectedInstrument != null) {
                    boolean isInternal = InternalSynthManager.getInstance().isInternalSynth(selectedInstrument);
                    if (isInternal != usingInternalSynth) {
                        usingInternalSynth = isInternal;
                        updatePresetControls();
                    }
                }
            }
        });
    }

    // Create a new method to initialize the preset controls
    private void setupPresetControls() {
        // Create both the spinner and combo
        presetSpinner = new JSpinner(createLongSpinnerModel(player.getPreset(), 0, 127, 1));

        presetCombo = new JComboBox<>();
        presetCombo.setPreferredSize(presetSpinner.getPreferredSize());

        // Create container panel with CardLayout
        presetControlPanel = new JPanel(new CardLayout());
        presetControlPanel.add(presetSpinner, "spinner");
        presetControlPanel.add(presetCombo, "combo");

        // Initial state based on the instrument
        if (player.getInstrument() != null) {
            usingInternalSynth = InternalSynthManager.getInstance().isInternalSynth(player.getInstrument());
            
            // Check for drum channel condition (channel 9)
            isDrumChannel = (player.getChannel() == 9);
        }

        // Add change listener for channel spinner to detect drum channel changes
        channelSpinner.addChangeListener(e -> {
            int channelValue = ((Number) channelSpinner.getValue()).intValue();
            boolean isDrumChannelNew = (channelValue == 9);
            
            // If drum channel status changed and using internal synth, update the UI
            if (isDrumChannelNew != isDrumChannel && usingInternalSynth) {
                isDrumChannel = isDrumChannelNew;
                updatePresetControls();
            }
        });

        // Populate the appropriate control
        updatePresetControls();
    }

    // Add method to update preset controls based on instrument type
    private void updatePresetControls() {
        CardLayout cl = (CardLayout) presetControlPanel.getLayout();

        if (usingInternalSynth) {
            // Check if we're on the drum channel
            if (isDrumChannel) {
                // Populate with drum names instead of presets
                populateDrumCombo();
            } else {
                // Normal preset population
                populatePresetCombo();
            }
            cl.show(presetControlPanel, "combo");
        } else {
            // Switch to spinner with numeric values
            // Ensure the spinner has the current preset value
            presetSpinner.setValue(player.getPreset());
            cl.show(presetControlPanel, "spinner");
        }
    }

    // Add method to populate the preset combo
    private void populatePresetCombo() {
        Instrument selectedInstrument = (Instrument) instrumentCombo.getSelectedItem();
        if (selectedInstrument == null)
            return;

        // Remember current preset
        long currentPreset = player.getPreset() != null ? player.getPreset() : 0;

        // Clear the combo
        presetCombo.removeAllItems();

        // Get preset names from InternalSynthManager
        List<String> presetNames = InternalSynthManager.getInstance().getPresetNames(selectedInstrument.getId());

        // If no presets found, use generic names
        if (presetNames.isEmpty()) {
            for (int i = 0; i < 128; i++) {
                presetCombo.addItem(new PresetItem(i, "Program " + i));
            }
        } else {
            // Add all named presets
            for (int i = 0; i < presetNames.size(); i++) {
                presetCombo.addItem(new PresetItem(i, presetNames.get(i)));
            }

            // Add remaining numbered presets if needed
            for (int i = presetNames.size(); i < 128; i++) {
                presetCombo.addItem(new PresetItem(i, "Program " + i));
            }
        }

        // Select the current preset
        for (int i = 0; i < presetCombo.getItemCount(); i++) {
            PresetItem item = (PresetItem) presetCombo.getItemAt(i);
            if (item.getNumber() == currentPreset) {
                presetCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    // Add new method to populate drum names
    private void populateDrumCombo() {
        // Clear the combo
        presetCombo.removeAllItems();
        
        // Get current note from player, defaulting to 36 (Bass Drum) if not set
        int currentNote = player.getRootNote() != null ? player.getRootNote().intValue() : 36;
        
        // Get drum names from InternalSynthManager
        for (int i = 27; i < 88; i++) {  // Standard GM drum range
            String drumName = InternalSynthManager.getInstance().getDrumName(i);
            if (drumName == null || drumName.isEmpty()) {
                drumName = "Drum " + i;
            }
            presetCombo.addItem(new DrumItem(i, drumName));
        }
        
        // Select the current drum
        for (int i = 0; i < presetCombo.getItemCount(); i++) {
            DrumItem item = (DrumItem) presetCombo.getItemAt(i);
            if (item.getNoteNumber() == currentNote) {
                presetCombo.setSelectedIndex(i);
                break;
            }
        }
    }

    static int SLIDER_HEIGHT = 80;

    private ToggleSwitch createToggleSwitch(String name, boolean value) {
        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(value);
        toggle.setPreferredSize(new Dimension(60, 30));
        return toggle;
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

    /**
     * Create the rules panel with table and control buttons
     */
    private JPanel createRulesPanel() {
        // Create main panel
        JPanel rulesPanel = new JPanel(new BorderLayout(5, 5));
        rulesPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder("Rules"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        // Add table in a scroll pane
        JScrollPane scrollPane = new JScrollPane(rulesTable);
        scrollPane.setPreferredSize(new Dimension(300, 200));
        rulesPanel.add(scrollPane, BorderLayout.CENTER);

        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        buttonPanel.add(addRuleButton);
        buttonPanel.add(editRuleButton);
        buttonPanel.add(deleteRuleButton);

        // Add button panel to bottom of rules panel
        rulesPanel.add(buttonPanel, BorderLayout.SOUTH);

        return rulesPanel;
    }
}
