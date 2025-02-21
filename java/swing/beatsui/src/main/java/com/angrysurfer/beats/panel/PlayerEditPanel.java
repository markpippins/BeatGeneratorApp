package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.logging.Logger;

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
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.service.InstrumentManager;

public class PlayerEditPanel extends StatusProviderPanel {
    private static final Logger logger = Logger.getLogger(PlayerEditPanel.class.getName());
    private final ProxyStrike player;

    // Basic properties
    private final JTextField nameField;
    private final JComboBox<ProxyInstrument> instrumentCombo;
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
    private final JTable rulesTable;
    private final JButton addRuleButton;
    private final JButton editRuleButton;
    private final JButton deleteRuleButton;

    // Add these fields near the top with other UI components
    private final JButton prevButton;
    private final JButton nextButton;

    public PlayerEditPanel(ProxyStrike player, StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        this.player = player;

        // Set fixed size
        setPreferredSize(new Dimension(600, 600));
        setMinimumSize(new Dimension(600, 600));
        setMaximumSize(new Dimension(600, 600));

        // Initialize basic properties
        nameField = new JTextField(player.getName(), 20);
        instrumentCombo = createInstrumentCombo();

        // Fix spinner initializations with proper value clamping
        int channelValue = Math.min(Math.max(1, (int) player.getChannel()), 16);
        channelSpinner = new JSpinner(new SpinnerNumberModel(channelValue, 1, 16, 1));

        // Fix preset range to 1-127 and clamp value
        int presetValue = Math.min(Math.max(1, player.getPreset().intValue()), 127);
        presetSpinner = new JSpinner(new SpinnerNumberModel(presetValue, 1, 127, 1));

        // Initialize performance controls as JSliders with safe value clamping
        swingSlider = createSlider("Swing", clampValue(player.getSwing(), 0, 100), 0, 100);
        levelSlider = createSlider("Level", clampValue(player.getLevel(), 0, 127), 0, 127);
        noteSlider = createSlider("Note", clampValue(player.getNote(), 0, 127), 0, 127);
        velocityMinSlider = createSlider("Min Vel", clampValue(player.getMinVelocity(), 0, 127), 0, 127);
        velocityMaxSlider = createSlider("Max Vel", clampValue(player.getMaxVelocity(), 0, 127), 0, 127);
        probabilitySlider = createSlider("Prob", clampValue(player.getProbability(), 0, 100), 0, 100);
        randomSlider = createSlider("Random", clampValue(player.getRandomDegree(), 0, 100), 0, 100);
        panSlider = createSlider("Pan", clampValue(player.getPanPosition(), 0, 127), 0, 127);
        sparseSlider = createSlider("Sparse", clampValue((long) (player.getSparse() * 100), 0, 100), 0, 100);

        // Initialize ratchet sliders with safe value clamping
        ratchetCountSlider = createSlider("Count", clampValue(player.getRatchetCount(), 1, 8), 1, 8, true);
        ratchetIntervalSlider = createSlider("Interval", clampValue(player.getRatchetInterval(), 1, 16), 1, 16, true);

        // Initialize switches
        stickyPresetSwitch = createToggleSwitch("Sticky", player.getStickyPreset());
        useInternalBeatsSwitch = createToggleSwitch("Int.Beats", player.getUseInternalBeats());
        useInternalBarsSwitch = createToggleSwitch("Int.Bars", player.getUseInternalBars());
        preserveOnPurgeSwitch = createToggleSwitch("Preserve", player.getPreserveOnPurge());

        // Initialize rules components - moved after player initialization
        rulesTable = new JTable();
        addRuleButton = new JButton("Add");
        editRuleButton = new JButton("Edit");
        deleteRuleButton = new JButton("Delete");

        // Setup rules table with player's current rules
        setupRulesTable();  // This now happens after player is properly initialized
        updateRulesTable(); // Explicitly update the table with player's rules

        // Add button listeners
        deleteRuleButton.addActionListener(e -> deleteSelectedRule());

        // Initialize navigation buttons with arrows
        prevButton = new JButton("↑");
        nextButton = new JButton("↓");
        prevButton.setEnabled(false); // Default to disabled
        nextButton.setEnabled(true); // Default to enabled

        // Add navigation button handling
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                if (Commands.PLAYER_ROW_INDEX_RESPONSE.equals(action.getCommand()) &&
                        action.getData() instanceof Integer rowIndex) {
                    // Enable prev button only if we're past the first row
                    prevButton.setEnabled(rowIndex > 0);
                    // Enable next button always for now (we can refine this later)
                    nextButton.setEnabled(true);
                }
            }
        });

        layoutComponents();
        setPreferredSize(new Dimension(800, 500));

        // Request row index after initialization
        CommandBus.getInstance().publish(Commands.PLAYER_ROW_INDEX_REQUEST, this, player);
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for basic controls - now using GridBagLayout for two rows
        JPanel topPanel = new JPanel(new GridBagLayout());
        topPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Basic Properties"),
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
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                mainContent, createRulesPanel());
        splitPane.setResizeWeight(0.7);
        add(splitPane, BorderLayout.CENTER);
    }

    // Helper record for dial entries
    private record DialEntry(String label, Dial dial) {
    }

    private JPanel createParameterColumn(String title, DialEntry... entries) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i < entries.length; i++) {
            gbc.gridx = 0;
            gbc.gridy = i;
            panel.add(new JLabel(entries[i].label()), gbc);
            gbc.gridx = 1;
            panel.add(entries[i].dial(), gbc);
        }

        return panel;
    }

    private JPanel createPropertiesPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Player Properties"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Basic properties section
        addBasicProperties(panel, gbc);

        // Performance controls section
        addPerformanceControls(panel, gbc);

        // Ratchet controls section
        addRatchetControls(panel, gbc);

        // Switches section
        addSwitches(panel, gbc);

        return panel;
    }

    private void addBasicProperties(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Name:"), gbc);
        gbc.gridx = 1;
        panel.add(nameField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Instrument:"), gbc);
        gbc.gridx = 1;
        panel.add(instrumentCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Channel:"), gbc);
        gbc.gridx = 1;
        panel.add(channelSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Preset:"), gbc);
        gbc.gridx = 1;
        panel.add(presetSpinner, gbc);
    }

    private void addPerformanceControls(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Swing:"), gbc);
        gbc.gridx = 1;
        panel.add(swingSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Level:"), gbc);
        gbc.gridx = 1;
        panel.add(levelSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Note:"), gbc);
        gbc.gridx = 1;
        panel.add(noteSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Min Vel:"), gbc);
        gbc.gridx = 1;
        panel.add(velocityMinSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Max Vel:"), gbc);
        gbc.gridx = 1;
        panel.add(velocityMaxSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Prob:"), gbc);
        gbc.gridx = 1;
        panel.add(probabilitySlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Random:"), gbc);
        gbc.gridx = 1;
        panel.add(randomSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Pan:"), gbc);
        gbc.gridx = 1;
        panel.add(panSlider, gbc);
    }

    private void addRatchetControls(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Count:"), gbc);
        gbc.gridx = 1;
        panel.add(ratchetCountSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Interval:"), gbc);
        gbc.gridx = 1;
        panel.add(ratchetIntervalSlider, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Sparse:"), gbc);
        gbc.gridx = 1;
        panel.add(sparseSlider, gbc);
    }

    private void addSwitches(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(stickyPresetSwitch, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(useInternalBeatsSwitch, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(useInternalBarsSwitch, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(preserveOnPurgeSwitch, gbc);
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

        return panel;
    }

    // Add method to handle rule editing
    private void editSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            ProxyRule rule = ProxyRule.fromRow(new Object[] {
                    rulesTable.getValueAt(selectedRow, 0),
                    rulesTable.getValueAt(selectedRow, 1),
                    rulesTable.getValueAt(selectedRow, 2),
                    rulesTable.getValueAt(selectedRow, 3)
            });

            RuleEditPanel editPanel = new RuleEditPanel(rule, statusConsumer);
            // Show dialog with edit panel...
            // Update table after editing...
        }
    }

    private void deleteSelectedRule() {
        int selectedRow = rulesTable.getSelectedRow();
        if (selectedRow >= 0) {
            ProxyRule rule = ProxyRule.fromRow(new Object[] {
                    rulesTable.getValueAt(selectedRow, 0),
                    rulesTable.getValueAt(selectedRow, 1),
                    rulesTable.getValueAt(selectedRow, 2),
                    rulesTable.getValueAt(selectedRow, 3)
            });

            // Just send the rule - TickerManager will handle finding the player
            CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this, rule);
        }
    }

    public ProxyStrike getUpdatedPlayer() {
        // Update player with current UI values
        player.setInstrument((ProxyInstrument) instrumentCombo.getSelectedItem());
        player.setChannel((Integer) channelSpinner.getValue());
        player.setPreset(((Number) presetSpinner.getValue()).longValue());
        player.setName(nameField.getText());
        player.setSwing((long) swingSlider.getValue());
        player.setLevel((long) levelSlider.getValue());
        player.setNote((long) noteSlider.getValue());
        player.setMinVelocity((long) velocityMinSlider.getValue());
        player.setMaxVelocity((long) velocityMaxSlider.getValue());
        player.setProbability((long) probabilitySlider.getValue());
        player.setRandomDegree((long) randomSlider.getValue());
        player.setPanPosition((long) panSlider.getValue());
        player.setRatchetCount((long) ratchetCountSlider.getValue());
        player.setRatchetInterval((long) ratchetIntervalSlider.getValue());
        player.setSparse(sparseSlider.getValue() / 100.0); // Convert percentage back to 0-1 range
        player.setStickyPreset(stickyPresetSwitch.isSelected());
        player.setUseInternalBeats(useInternalBeatsSwitch.isSelected());
        player.setUseInternalBars(useInternalBarsSwitch.isSelected());
        player.setPreserveOnPurge(preserveOnPurgeSwitch.isSelected());
        return player;
    }

    // Helper methods for creating components
    private JComboBox<ProxyInstrument> createInstrumentCombo() {
        JComboBox<ProxyInstrument> combo = new JComboBox<>(
                InstrumentManager.getInstance().getAvailableInstruments().toArray(new ProxyInstrument[0]));
        return combo;
    }

    static int SLIDER_HEIGHT = 80;

    private JSlider createSlider(String name, long value, int min, int max) {
        JSlider slider = new JSlider(JSlider.VERTICAL, min, max, (int) value);
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

    private long clampValue(Long value, int min, int max) {
        if (value == null) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
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
        JPanel rulesPanel = new JPanel(new BorderLayout(5, 5));
        rulesPanel.setBorder(BorderFactory.createTitledBorder("Rules"));

        // Create rules table with same columns as RulesPanel
        String[] columnNames = { "Operator", "Comparison", "Value", "Part" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        rulesTable.setModel(model);

        // Match the RulesPanel column alignments
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        rulesTable.getColumnModel().getColumn(0).setCellRenderer(leftRenderer); // Operator column left-aligned
        for (int i = 1; i < rulesTable.getColumnCount(); i++) {
            rulesTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Add double-click handler
        rulesTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    ProxyRule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
            }
        });

        // Add button handlers
        addRuleButton.addActionListener(e -> CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, player));

        editRuleButton.addActionListener(e -> {
            ProxyRule selectedRule = getSelectedRule();
            if (selectedRule != null) {
                CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
            }
        });

        deleteRuleButton.addActionListener(e -> {
            ProxyRule selectedRule = getSelectedRule();
            if (selectedRule != null) {
                CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this,
                        new ProxyRule[] { selectedRule });
            }
        });

        // Listen for rule updates
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                switch (action.getCommand()) {
                    case Commands.PLAYER_UPDATED -> {
                        if (action.getData() instanceof ProxyStrike updatedPlayer &&
                                updatedPlayer.getId().equals(player.getId())) {
                            player.setRules(updatedPlayer.getRules());
                            updateRulesTable();
                            updateButtonStates();
                        }
                    }
                }
            }
        });

        // Layout
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttonPanel.add(addRuleButton);
        buttonPanel.add(editRuleButton);
        buttonPanel.add(deleteRuleButton);
        rulesPanel.add(buttonPanel, BorderLayout.NORTH);
        rulesPanel.add(new JScrollPane(rulesTable), BorderLayout.CENTER);
    }

    private ProxyRule getSelectedRule() {
        int row = rulesTable.getSelectedRow();
        if (row >= 0 && player != null && player.getRules() != null) {
            return new ArrayList<>(player.getRules()).get(row);
        }
        return null;
    }

    private void updateRulesTable() {
        DefaultTableModel model = (DefaultTableModel) rulesTable.getModel();
        model.setRowCount(0);
        
        if (player == null) {
            logger.warning("Cannot update rules table - player is null");
            return;
        }
        
        if (player.getRules() == null) {
            logger.warning("Player " + player.getName() + " has null rules collection");
            return;
        }
        
        logger.info("Updating rules table for player " + player.getName() + 
                   " (ID: " + player.getId() + ") with " + player.getRules().size() + " rules");
        
        for (ProxyRule rule : player.getRules()) {
            if (rule == null) {
                logger.warning("Encountered null rule in player's rules collection");
                continue;
            }
            
            logger.info("Adding rule: Operator=" + rule.getOperator() + 
                       ", Comparison=" + rule.getComparison() + 
                       ", Value=" + rule.getValue() + 
                       ", Part=" + rule.getPart());
            
            String operatorText = rule.getOperator() >= 0 && rule.getOperator() < ProxyRule.OPERATORS.length ? 
                ProxyRule.OPERATORS[rule.getOperator()] : "Unknown";
            String comparisonText = rule.getComparison() >= 0 && rule.getComparison() < ProxyRule.COMPARISONS.length ? 
                ProxyRule.COMPARISONS[rule.getComparison()] : "Unknown";
            String partText = rule.getPart() == 0 ? "All" : String.valueOf(rule.getPart());
            
            model.addRow(new Object[] {
                operatorText,
                comparisonText,
                rule.getValue(),
                partText
            });
        }
        
        // Force table to refresh
        rulesTable.revalidate();
        rulesTable.repaint();
    }

    // Add this method to call manually after initialization
    private void setupRulesTable() {
        // Create rules table with proper columns
        String[] columnNames = { "Operator", "Comparison", "Value", "Part" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        rulesTable.setModel(model);

        // Set up column renderers
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        rulesTable.getColumnModel().getColumn(0).setCellRenderer(leftRenderer); 
        for (int i = 1; i < rulesTable.getColumnCount(); i++) {
            rulesTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Load initial rules
        updateRulesTable();

        // ... rest of table setup (buttons, listeners, etc.)
    }

    private void updateButtonStates() {
        boolean hasSelection = rulesTable.getSelectedRow() >= 0;
        editRuleButton.setEnabled(hasSelection);
        deleteRuleButton.setEnabled(hasSelection);
    }
}
