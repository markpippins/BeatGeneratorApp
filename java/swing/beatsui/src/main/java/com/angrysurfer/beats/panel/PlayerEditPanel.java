package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.ToggleSwitch;
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
    private final JSpinner channelSpinner;  // Changed from Dial to JSpinner
    private final JSpinner presetSpinner;   // Changed from Dial to JSpinner
    
    // Performance controls
    private final Dial swingDial;
    private final Dial levelDial;
    private final Dial noteDial;
    private final Dial velocityMinDial;
    private final Dial velocityMaxDial;
    private final Dial probabilityDial;
    private final Dial randomDial;
    private final Dial panDial;
    
    // Ratchet controls
    private final JSpinner ratchetCountSpinner;  // Changed from Dial to JSpinner
    private final JSpinner ratchetIntervalSpinner;  // Changed from Dial to JSpinner
    private final Dial sparseDial;
    
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

    public PlayerEditPanel(ProxyStrike player, StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        this.player = player;
        
        // Initialize basic properties
        nameField = new JTextField(player.getName(), 20);
        instrumentCombo = createInstrumentCombo();
        
        // Fix spinner initializations with proper value clamping
        int channelValue = Math.min(Math.max(1, (int)player.getChannel()), 16);
        channelSpinner = new JSpinner(new SpinnerNumberModel(channelValue, 1, 16, 1));
        
        // Fix preset range to 1-127 and clamp value
        int presetValue = Math.min(Math.max(1, player.getPreset().intValue()), 127);
        presetSpinner = new JSpinner(new SpinnerNumberModel(presetValue, 1, 127, 1));
        
        // Initialize performance controls
        swingDial = createDial("Swing", player.getSwing(), 0, 100);
        levelDial = createDial("Level", player.getLevel(), 0, 127);
        noteDial = createDial("Note", player.getNote(), 0, 127);
        velocityMinDial = createDial("Min Vel", player.getMinVelocity(), 0, 127);
        velocityMaxDial = createDial("Max Vel", player.getMaxVelocity(), 0, 127);
        probabilityDial = createDial("Prob", player.getProbability(), 0, 100);
        randomDial = createDial("Random", player.getRandomDegree(), 0, 100);
        panDial = createDial("Pan", player.getPanPosition(), 0, 127);
        
        // Initialize ratchet controls
        ratchetCountSpinner = new JSpinner(new SpinnerNumberModel(
            player.getRatchetCount().intValue(), 1, 8, 1));
        ratchetIntervalSpinner = new JSpinner(new SpinnerNumberModel(
            player.getRatchetInterval().intValue(), 1, 16, 1));
        sparseDial = createDial("Sparse", (long)(player.getSparse() * 100), 0, 100);
        
        // Initialize switches
        stickyPresetSwitch = createToggleSwitch("Sticky", player.getStickyPreset());
        useInternalBeatsSwitch = createToggleSwitch("Int.Beats", player.getUseInternalBeats());
        useInternalBarsSwitch = createToggleSwitch("Int.Bars", player.getUseInternalBars());
        preserveOnPurgeSwitch = createToggleSwitch("Preserve", player.getPreserveOnPurge());
        
        // Initialize rules components
        rulesTable = new JTable(new DefaultTableModel(
            new Object[]{"Operator", "Comparison", "Value", "Part"}, 0));
        updateRulesTable();
        addRuleButton = new JButton("Add");
        editRuleButton = new JButton("Edit");
        deleteRuleButton = new JButton("Delete");
        
        layoutComponents();
        setPreferredSize(new Dimension(800, 600));
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Top panel for basic controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Basic Properties"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // Add basic controls in a row
        topPanel.add(new JLabel("Name:"));
        topPanel.add(nameField);
        topPanel.add(new JLabel("Instrument:"));
        topPanel.add(instrumentCombo);
        topPanel.add(new JLabel("Channel:"));
        topPanel.add(channelSpinner);
        topPanel.add(new JLabel("Preset:"));
        topPanel.add(presetSpinner);
        
        add(topPanel, BorderLayout.NORTH);

        // Main content with parameters and rules
        JPanel mainContent = new JPanel(new BorderLayout());
        
        // Performance controls panel (horizontal layout)
        JPanel performancePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        performancePanel.setBorder(BorderFactory.createTitledBorder("Performance"));
        performancePanel.add(createLabeledDial("Level", levelDial));
        performancePanel.add(createLabeledDial("Note", noteDial));
        performancePanel.add(createLabeledDial("Min Vel", velocityMinDial));
        performancePanel.add(createLabeledDial("Max Vel", velocityMaxDial));
        performancePanel.add(createLabeledDial("Pan", panDial));
        
        // Modulation controls panel (horizontal layout)
        JPanel modulationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        modulationPanel.setBorder(BorderFactory.createTitledBorder("Modulation"));
        modulationPanel.add(createLabeledDial("Swing", swingDial));
        modulationPanel.add(createLabeledDial("Probability", probabilityDial));
        modulationPanel.add(createLabeledDial("Random", randomDial));
        modulationPanel.add(createLabeledDial("Sparse", sparseDial));

        // Combine performance and modulation panels
        JPanel controlsPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        controlsPanel.add(performancePanel);
        controlsPanel.add(modulationPanel);
        mainContent.add(controlsPanel, BorderLayout.CENTER);
        
        // Options panel with switches and ratchet controls
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        optionsPanel.setBorder(BorderFactory.createTitledBorder("Options"));
        
        // Add ratchet controls
        JPanel ratchetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        ratchetPanel.add(new JLabel("Count:"));
        ratchetPanel.add(ratchetCountSpinner);
        ratchetPanel.add(new JLabel("Interval:"));
        ratchetPanel.add(ratchetIntervalSpinner);
        optionsPanel.add(ratchetPanel);
        
        // Add switches
        optionsPanel.add(stickyPresetSwitch);
        optionsPanel.add(useInternalBeatsSwitch);
        optionsPanel.add(useInternalBarsSwitch);
        optionsPanel.add(preserveOnPurgeSwitch);
        
        mainContent.add(optionsPanel, BorderLayout.SOUTH);
        
        // Rules panel
        JPanel rulesPanel = createRulesPanel();
        
        // Combine parameters and rules with split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, 
                                            mainContent, rulesPanel);
        splitPane.setResizeWeight(0.7);
        add(splitPane, BorderLayout.CENTER);
    }

    // Helper record for dial entries
    private record DialEntry(String label, Dial dial) {}

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
        panel.add(swingDial, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Level:"), gbc);
        gbc.gridx = 1;
        panel.add(levelDial, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Note:"), gbc);
        gbc.gridx = 1;
        panel.add(noteDial, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Min Vel:"), gbc);
        gbc.gridx = 1;
        panel.add(velocityMinDial, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Max Vel:"), gbc);
        gbc.gridx = 1;
        panel.add(velocityMaxDial, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Prob:"), gbc);
        gbc.gridx = 1;
        panel.add(probabilityDial, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Random:"), gbc);
        gbc.gridx = 1;
        panel.add(randomDial, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Pan:"), gbc);
        gbc.gridx = 1;
        panel.add(panDial, gbc);
    }

    private void addRatchetControls(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Count:"), gbc);
        gbc.gridx = 1;
        panel.add(ratchetCountSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Interval:"), gbc);
        gbc.gridx = 1;
        panel.add(ratchetIntervalSpinner, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        panel.add(new JLabel("Sparse:"), gbc);
        gbc.gridx = 1;
        panel.add(sparseDial, gbc);
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
        panel.add(new JScrollPane(rulesTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(addRuleButton);
        buttonPanel.add(editRuleButton);
        buttonPanel.add(deleteRuleButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateRulesTable() {
        DefaultTableModel model = (DefaultTableModel) rulesTable.getModel();
        model.setRowCount(0);
        if (player.getRules() != null) {
            for (ProxyRule rule : player.getRules()) {
                model.addRow(rule.toRow());
            }
        }
    }

    public ProxyStrike getUpdatedPlayer() {
        // Update player with current UI values
        player.setInstrument((ProxyInstrument) instrumentCombo.getSelectedItem());
        player.setChannel((Integer) channelSpinner.getValue());
        player.setPreset(((Number) presetSpinner.getValue()).longValue());
        player.setName(nameField.getText());
        player.setSwing((long) swingDial.getValue());
        player.setLevel((long) levelDial.getValue());
        player.setNote((long) noteDial.getValue());
        player.setMinVelocity((long) velocityMinDial.getValue());
        player.setMaxVelocity((long) velocityMaxDial.getValue());
        player.setProbability((long) probabilityDial.getValue());
        player.setRandomDegree((long) randomDial.getValue());
        player.setPanPosition((long) panDial.getValue());
        player.setRatchetCount(((Number) ratchetCountSpinner.getValue()).longValue());
        player.setRatchetInterval(((Number) ratchetIntervalSpinner.getValue()).longValue());
        player.setSparse(sparseDial.getValue() / 100.0); // Convert percentage back to 0-1 range
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

    private Dial createDial(String name, long value, int min, int max) {
        Dial dial = new Dial();
        dial.setValue((int) value);
        return dial;
    }

    private ToggleSwitch createToggleSwitch(String name, boolean value) {
        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(value);
        toggle.setPreferredSize(new Dimension(60, 30));
        return toggle;
    }

    private JPanel createLabeledDial(String label, Dial dial) {
        JPanel panel = new JPanel(new BorderLayout(5, 2));
        panel.add(new JLabel(label, JLabel.CENTER), BorderLayout.NORTH);
        panel.add(dial, BorderLayout.CENTER);
        return panel;
    }
}
