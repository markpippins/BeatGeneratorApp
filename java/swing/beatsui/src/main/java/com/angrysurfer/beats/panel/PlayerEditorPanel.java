package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beats.App;
import com.angrysurfer.beats.Dialog;
import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;
import com.angrysurfer.core.service.InstrumentManager;

public class PlayerEditorPanel extends StatusProviderPanel {

    private static final Logger logger = Logger.getLogger(PlayerEditorPanel.class.getName());

    private final ProxyStrike player;
    private final JComboBox<ProxyInstrument> instrumentCombo; // Replace nameField
    private final JComboBox<Integer> channelCombo;
    private final JComboBox<Integer> presetCombo;
    private final Dial swingDial;
    private final Dial levelDial;
    private final Dial noteDial;
    private final Dial minVelocityDial;
    private final Dial maxVelocityDial;
    private final Dial probabilityDial;
    private final Dial randomDegreeDial;
    private final Dial ratchetCountDial;
    private final Dial ratchetIntervalDial;
    private final Dial panPositionDial;
    private final ToggleSwitch stickyPresetSwitch;
    private final ToggleSwitch useInternalBeatsSwitch;
    private final ToggleSwitch useInternalBarsSwitch;
    private final ToggleSwitch preserveOnPurgeSwitch;
    private final JSpinner sparseSpinner;

    // New dials
    private final Dial internalBeatsDial;
    private final Dial internalBarsDial;
    private final Dial subDivisionsDial;
    private final Dial beatFractionDial;
    private final Dial fadeInDial;
    private final Dial fadeOutDial;

    // New toggle
    private final ToggleSwitch accentSwitch;

    // Rules components
    private JTable rulesTable;
    private JButton addRuleButton;
    private JButton editRuleButton;
    private JButton deleteRuleButton;

    public PlayerEditorPanel(ProxyStrike player) {
        this(player, null);
    }

    public PlayerEditorPanel(ProxyStrike player, StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        this.player = player;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Initialize combo box first, then populate it
        instrumentCombo = new JComboBox<>();
        setupInstrumentCombo(); // Call this instead of loadInstruments()

        if (player.getInstrumentId() != null) {
            for (int i = 0; i < instrumentCombo.getItemCount(); i++) {
                ProxyInstrument inst = instrumentCombo.getItemAt(i);
                if (inst.getId() != null && inst.getId().equals(player.getInstrumentId())) {
                    instrumentCombo.setSelectedIndex(i);
                    break;
                }
            }
        }

        channelCombo = new JComboBox<>(createChannelOptions());
        channelCombo.setSelectedItem(player.getChannel());

        presetCombo = new JComboBox<>(createPresetOptions());
        presetCombo.setSelectedItem(player.getPreset().intValue());

        swingDial = createDial("Swing", player.getSwing(), 0, 100);
        levelDial = createDial("Level", player.getLevel(), 0, 127);
        noteDial = createDial("Note", player.getNote(), 0, 127);
        minVelocityDial = createDial("Min Velocity", player.getMinVelocity(), 0, 127);
        maxVelocityDial = createDial("Max Velocity", player.getMaxVelocity(), 0, 127);
        probabilityDial = createDial("Probability", player.getProbability(), 0, 100);
        randomDegreeDial = createDial("Random", player.getRandomDegree(), 0, 100);
        ratchetCountDial = createDial("Ratchet #", player.getRatchetCount(), 0, 6);
        ratchetIntervalDial = createDial("Ratchet Int", player.getRatchetInterval(), 1, 16);
        panPositionDial = createDial("Pan", player.getPanPosition(), 0, 127);

        stickyPresetSwitch = createToggleSwitch("Sticky Preset", player.getStickyPreset());
        useInternalBeatsSwitch = createToggleSwitch("Internal Beats", player.getUseInternalBeats());
        useInternalBarsSwitch = createToggleSwitch("Internal Bars", player.getUseInternalBars());
        preserveOnPurgeSwitch = createToggleSwitch("Preserve", player.getPreserveOnPurge());

        sparseSpinner = new JSpinner(new SpinnerNumberModel(player.getSparse(), 0.0, 1.0, 0.1));

        // Initialize new dials
        internalBeatsDial = createDial("Int Beats", player.getInternalBeats(), 1, 16);
        internalBarsDial = createDial("Int Bars", player.getInternalBars(), 1, 16);
        subDivisionsDial = createDial("Subdivisions", player.getSubDivisions(), 1, 16);
        beatFractionDial = createDial("Beat Fraction", player.getBeatFraction(), 1, 16);
        fadeInDial = createDial("Fade In", player.getFadeIn(), 0, 127);
        fadeOutDial = createDial("Fade Out", player.getFadeOut(), 0, 127);

        // Initialize new toggle
        accentSwitch = createToggleSwitch("Accent", player.getAccent());

        // Initialize rules components
        setupRulesComponents();

        layoutComponents();

        setPreferredSize(new Dimension(475, 800)); // Made taller to accommodate rules
    }

    private void setupRulesComponents() {
        rulesTable = createRulesTable();
        addRuleButton = new JButton("Add");
        editRuleButton = new JButton("Edit");
        deleteRuleButton = new JButton("Delete");

        addRuleButton.addActionListener(e -> {
            // Check if player needs to be saved first
            if (player.getId() == null) {
                if (showSavePlayerDialog()) {
                    showRuleDialog(null);
                }
            } else {
                showRuleDialog(null);
            }
        });

        editRuleButton.addActionListener(e -> editSelectedRule());
        deleteRuleButton.addActionListener(e -> deleteSelectedRule());

        // Initial button states
        editRuleButton.setEnabled(false);
        deleteRuleButton.setEnabled(false);

        // Selection listener for buttons
        rulesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = rulesTable.getSelectedRow() >= 0;
                editRuleButton.setEnabled(hasSelection);
                deleteRuleButton.setEnabled(hasSelection);
            }
        });
    }

    private boolean showSavePlayerDialog() {
        int result = javax.swing.JOptionPane.showConfirmDialog(
                this,
                "The player needs to be saved before adding rules. Save now?",
                "Save Player",
                javax.swing.JOptionPane.YES_NO_OPTION);

        if (result == javax.swing.JOptionPane.YES_OPTION) {
            // Save the player
            ProxyStrike savedPlayer = App.getRedisService().saveStrike(getUpdatedPlayer());
            if (savedPlayer != null && savedPlayer.getId() != null) {
                player.setId(savedPlayer.getId()); // Update the ID
                return true;
            }
        }
        return false;
    }

    private JTable createRulesTable() {
        String[] columns = { "Operator", "Comparison", "Value", "Part" };
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        // Load existing rules
        if (player.getId() != null) {
            List<ProxyRule> loadedRules = App.getRedisService().findRulesByPlayer(player);
            Set<ProxyRule> ruleSet = new HashSet<>(loadedRules); // Convert List to Set
            player.setRules(ruleSet); // Update the player's rules with the Set

            for (ProxyRule rule : ruleSet) {
                model.addRow(rule.toRow());
            }
            logger.info("Loaded " + ruleSet.size() + " rules for player: " + player.getName());
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);

        // Add custom renderer for the Part column (index 3)
        table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Integer && ((Integer) value) == ProxyRule.ALL_PARTS) {
                    setText("All");
                }
                setHorizontalAlignment(JLabel.CENTER);
                return c;
            }
        });

        // Center align all other columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (i != 3) { // Skip the Part column as it has its own renderer
                table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        return table;
    }

    private Integer[] createChannelOptions() {
        Integer[] options = new Integer[12];
        for (int i = 0; i < 12; i++) {
            options[i] = i + 1;
        }
        return options;
    }

    private Integer[] createPresetOptions() {
        Integer[] options = new Integer[127];
        for (int i = 0; i < 127; i++) {
            options[i] = i + 1;
        }
        return options;
    }

    private Dial createDial(String name, long value, int min, int max) {
        Dial dial = new Dial();
        dial.setValue((int) value);
        return dial;
    }

    private ToggleSwitch createToggleSwitch(String name, boolean value) {
        ToggleSwitch toggle = new ToggleSwitch();
        toggle.setSelected(value);
        toggle.setPreferredSize(new Dimension(60, 30)); // Set a reasonable size
        toggle.setMinimumSize(new Dimension(60, 30)); // Ensure minimum size
        return toggle;
    }

    private ProxyInstrument[] loadInstruments() {
        List<ProxyInstrument> instruments = getFilteredInstruments();
        logger.info("Loaded " + instruments.size() + " instruments"); // Add logging
        return instruments.toArray(new ProxyInstrument[0]);
    }

    private void setupInstrumentCombo() {
        List<ProxyInstrument> instruments = getFilteredInstruments();
        DefaultComboBoxModel<ProxyInstrument> model = new DefaultComboBoxModel<>();

        // Add logging to debug
        logger.info("Setting up instrument combo with " + instruments.size() + " instruments");

        instruments.forEach(inst -> {
            model.addElement(inst);
            logger.info("Added instrument: " + inst.getName() + " (device: " + inst.getDeviceName() + ")");
        });

        instrumentCombo.setModel(model);

        if (player != null && player.getInstrument() != null) {
            instrumentCombo.setSelectedItem(player.getInstrument());
        } else if (model.getSize() > 0) {
            instrumentCombo.setSelectedIndex(0);
        }
    }

    private void layoutComponents() {
        // Create left panel for all controls except rules
        JPanel leftPanel = new JPanel(new BorderLayout());

        // Top panel for name, channel, preset
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints topGbc = new GridBagConstraints();
        topGbc.insets = new Insets(5, 5, 5, 5);
        topGbc.fill = GridBagConstraints.HORIZONTAL;
        topGbc.weightx = 1.0;

        // Replace "Name" with "Instrument" in the top panel
        addComponent("Instrument", instrumentCombo, 0, 0, 2, topGbc, topPanel);

        // Add channel and preset combos
        addComponent("Channel", channelCombo, 2, 0, 1, topGbc, topPanel);
        addComponent("Preset", presetCombo, 3, 0, 1, topGbc, topPanel);

        // Center panel for dials in rows
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints dialGbc = new GridBagConstraints();
        dialGbc.insets = new Insets(5, 5, 5, 5);
        dialGbc.fill = GridBagConstraints.NONE;
        dialGbc.anchor = GridBagConstraints.CENTER;

        // First row of dials
        addDialComponent("Swing", swingDial, 0, 0, centerPanel);
        addDialComponent("Level", levelDial, 1, 0, centerPanel);
        addDialComponent("Note", noteDial, 2, 0, centerPanel);
        addDialComponent("Pan", panPositionDial, 3, 0, centerPanel);
        addDialComponent("Min Vel", minVelocityDial, 4, 0, centerPanel);

        // Second row of dials
        addDialComponent("Max Vel", maxVelocityDial, 0, 1, centerPanel);
        addDialComponent("Probability", probabilityDial, 1, 1, centerPanel);
        addDialComponent("Random", randomDegreeDial, 2, 1, centerPanel);
        addDialComponent("Ratchet #", ratchetCountDial, 3, 1, centerPanel);
        addDialComponent("Ratchet Int", ratchetIntervalDial, 4, 1, centerPanel);

        // Third row of dials (new)
        addDialComponent("Int Beats", internalBeatsDial, 0, 2, centerPanel);
        addDialComponent("Int Bars", internalBarsDial, 1, 2, centerPanel);
        addDialComponent("Subdivisions", subDivisionsDial, 2, 2, centerPanel);
        addDialComponent("Beat Fraction", beatFractionDial, 3, 2, centerPanel);

        // Fourth row (fade controls)
        addDialComponent("Fade In", fadeInDial, 0, 3, centerPanel);
        addDialComponent("Fade Out", fadeOutDial, 1, 3, centerPanel);

        // Bottom panel for toggles and sparse spinner
        JPanel bottomPanel = new JPanel(new GridBagLayout());
        GridBagConstraints toggleGbc = new GridBagConstraints();
        toggleGbc.insets = new Insets(5, 5, 5, 5);
        toggleGbc.fill = GridBagConstraints.NONE;
        toggleGbc.anchor = GridBagConstraints.CENTER;

        // Add toggles in a row
        addToggleComponent("Sticky", stickyPresetSwitch, 0, 0, bottomPanel);
        addToggleComponent("Int Beats", useInternalBeatsSwitch, 1, 0, bottomPanel);
        addToggleComponent("Int Bars", useInternalBarsSwitch, 2, 0, bottomPanel);
        addToggleComponent("Preserve", preserveOnPurgeSwitch, 3, 0, bottomPanel);
        addToggleComponent("Accent", accentSwitch, 4, 0, bottomPanel);

        // Add sparse spinner
        GridBagConstraints sparseGbc = (GridBagConstraints) toggleGbc.clone();
        sparseGbc.fill = GridBagConstraints.HORIZONTAL;
        addComponent("Sparse", sparseSpinner, 5, 0, 1, sparseGbc, bottomPanel);

        // Assemble left side
        leftPanel.add(topPanel, BorderLayout.NORTH);
        leftPanel.add(centerPanel, BorderLayout.CENTER);
        leftPanel.add(bottomPanel, BorderLayout.SOUTH);

        // Create right side rules panel
        JPanel rulesPanel = new JPanel(new BorderLayout());
        rulesPanel.setBorder(BorderFactory.createTitledBorder("Rules"));

        // Rules toolbar
        JPanel rulesToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rulesToolbar.add(addRuleButton);
        rulesToolbar.add(editRuleButton);
        rulesToolbar.add(deleteRuleButton);

        rulesPanel.add(rulesToolbar, BorderLayout.NORTH);
        rulesPanel.add(new JScrollPane(rulesTable), BorderLayout.CENTER);

        // Set preferred sizes
        leftPanel.setPreferredSize(new Dimension(475, 700));
        rulesPanel.setPreferredSize(new Dimension(300, 700));

        // Main layout assembly using JSplitPane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rulesPanel);
        splitPane.setResizeWeight(0.7); // Give more weight to the left side

        add(splitPane, BorderLayout.CENTER);
    }

    private void addComponent(String label, JComponent component, int x, int y, int width,
            GridBagConstraints gbc, JPanel targetPanel) {
        gbc = (GridBagConstraints) gbc.clone();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        targetPanel.add(panel, gbc);
    }

    private void addDialComponent(String label, Dial dial, int x, int y, JPanel targetPanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(dial, BorderLayout.CENTER);
        targetPanel.add(panel, gbc);
    }

    private void addToggleComponent(String label, ToggleSwitch toggle, int x, int y, JPanel targetPanel) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.NONE; // Changed from NONE to HORIZONTAL
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel(label), BorderLayout.NORTH);

        // Create a wrapper panel to center the toggle
        JPanel toggleWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        toggleWrapper.add(toggle);
        panel.add(toggleWrapper, BorderLayout.CENTER);

        targetPanel.add(panel, gbc);
    }

    private void showRuleDialog(ProxyRule rule) {
        boolean isNew = (rule == null);
        if (isNew) {
            rule = new ProxyRule();
        }

        RuleEditorPanel editorPanel = new RuleEditorPanel(rule);
        Dialog<ProxyRule> dialog = new Dialog<>(rule, editorPanel);
        dialog.setTitle(isNew ? "Add Rule" : "Edit Rule");

        if (dialog.showDialog()) {
            ProxyRule updatedRule = editorPanel.getUpdatedRule();
            if (isNew) {
                player.getRules().add(updatedRule);
            }
            refreshRulesTable();
        }
    }

    private void editSelectedRule() {
        int row = rulesTable.getSelectedRow();
        if (row >= 0) {
            ProxyRule rule = getRuleFromRow(row);
            showRuleDialog(rule);
        }
    }

    private void deleteSelectedRule() {
        int row = rulesTable.getSelectedRow();
        if (row >= 0) {
            ProxyRule rule = getRuleFromRow(row);
            player.getRules().remove(rule);
            refreshRulesTable();
        }
    }

    private ProxyRule getRuleFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) rulesTable.getModel();
        ProxyRule rule = new ProxyRule();

        // Convert displayed text back to indices
        String operatorText = (String) model.getValueAt(row, 0);
        String comparisonText = (String) model.getValueAt(row, 1);

        rule.setOperator(java.util.Arrays.asList(ProxyRule.OPERATORS).indexOf(operatorText));
        rule.setComparison(java.util.Arrays.asList(ProxyRule.COMPARISONS).indexOf(comparisonText));
        rule.setValue((Double) model.getValueAt(row, 2));
        rule.setPart((Integer) model.getValueAt(row, 3));

        return rule;
    }

    private void refreshRulesTable() {
        DefaultTableModel model = (DefaultTableModel) rulesTable.getModel();
        model.setRowCount(0);

        for (ProxyRule rule : player.getRules()) {
            model.addRow(new Object[] {
                    ProxyRule.OPERATORS[rule.getOperator()],
                    ProxyRule.COMPARISONS[rule.getComparison()],
                    rule.getValue(),
                    rule.getPart()
            });
        }
    }

    public ProxyStrike getUpdatedPlayer() {
        // Since we're modifying the original player object directly,
        // we just need to preserve its ID and return it
        Long originalId = player.getId();

        // Update all the fields
        player.setName(((ProxyInstrument) instrumentCombo.getSelectedItem()).getName());
        player.setInstrument((ProxyInstrument) instrumentCombo.getSelectedItem());
        player.setChannel((Integer) channelCombo.getSelectedItem());
        player.setPreset(((Integer) presetCombo.getSelectedItem()).longValue());
        player.setSwing((long) swingDial.getValue());
        player.setLevel((long) levelDial.getValue());
        player.setNote((long) noteDial.getValue());
        player.setMinVelocity((long) minVelocityDial.getValue());
        player.setMaxVelocity((long) maxVelocityDial.getValue());
        player.setProbability((long) probabilityDial.getValue());
        player.setRandomDegree((long) randomDegreeDial.getValue());
        player.setRatchetCount((long) ratchetCountDial.getValue());
        player.setRatchetInterval((long) ratchetIntervalDial.getValue());
        player.setPanPosition((long) panPositionDial.getValue());

        // New values
        player.setInternalBeats(internalBeatsDial.getValue());
        player.setInternalBars(internalBarsDial.getValue());
        player.setSubDivisions((long) subDivisionsDial.getValue());
        player.setBeatFraction((long) beatFractionDial.getValue());
        player.setFadeIn((long) fadeInDial.getValue());
        player.setFadeOut((long) fadeOutDial.getValue());

        // Toggles
        player.setStickyPreset(stickyPresetSwitch.isSelected());
        player.setUseInternalBeats(useInternalBeatsSwitch.isSelected());
        player.setUseInternalBars(useInternalBarsSwitch.isSelected());
        player.setPreserveOnPurge(preserveOnPurgeSwitch.isSelected());
        player.setAccent(accentSwitch.isSelected());

        // Spinner
        player.setSparse((double) sparseSpinner.getValue());

        // Update Instrument ID instead of name
        ProxyInstrument selectedInstrument = (ProxyInstrument) instrumentCombo.getSelectedItem();
        player.setInstrumentId(selectedInstrument != null ? selectedInstrument.getId() : null);

        // Ensure ID is preserved
        player.setId(originalId);

        return player;
    }

    private List<ProxyInstrument> getFilteredInstruments() {
        return InstrumentManager.getInstance().getAvailableInstruments();
    }

    public void showDialog() {
        setupInstrumentCombo(); // Ensure fresh instrument list when dialog opens
        // ...existing code...
    }
}
