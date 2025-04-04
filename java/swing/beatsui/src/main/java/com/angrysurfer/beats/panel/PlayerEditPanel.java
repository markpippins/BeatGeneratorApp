package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.IllegalFormatConversionException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.service.SessionManager;

/**
 * Panel for editing player properties. Uses the PlayerEditBasicPropertiesPanel 
 * for handling instrument/preset selection and the PlayerEditDetailPanel for sliders.
 */
public class PlayerEditPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(PlayerEditPanel.class.getName());
    private final Player player;

    // Basic properties panel
    private PlayerEditBasicPropertiesPanel basicPropertiesPanel;

    // Detail panel for sliders
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

    // Internal beats/bars spinners
    private JSpinner internalBarsSpinner;
    private JSpinner internalBeatsSpinner;

    // Constructor - initialize all components
    public PlayerEditPanel(Player player) {
        super(new BorderLayout());
        this.player = player; 

        // Create the basic properties panel
        basicPropertiesPanel = new PlayerEditBasicPropertiesPanel(player);

        // Initialize toggle switches
        stickyPresetSwitch = createToggleSwitch("Sticky Preset", player.getStickyPreset());
        useInternalBeatsSwitch = createToggleSwitch("Internal Beats", player.getUseInternalBeats());
        useInternalBarsSwitch = createToggleSwitch("Internal Bars", player.getUseInternalBars());
        preserveOnPurgeSwitch = createToggleSwitch("Preserve", player.getPreserveOnPurge());

        // Initialize internal beats/bars spinners
        internalBeatsSpinner = new JSpinner(new SpinnerNumberModel(
                player.getInternalBeats() != null ? player.getInternalBeats().intValue() : 4, 1, 64, 1));
        
        internalBarsSpinner = new JSpinner(new SpinnerNumberModel(
                player.getInternalBars() != null ? player.getInternalBars().intValue() : 4, 1, 64, 1));

        // Initialize table and buttons
        rulesTable = new JTable();
        addRuleButton = new JButton("Add");
        editRuleButton = new JButton("Edit");
        deleteRuleButton = new JButton("Delete");

        // Create the detail panel that contains all the sliders
        detailPanel = new PlayerEditDetailPanel(player);

        // Layout all components
        layoutComponents();
        setupRulesTable();
        debugPlayerState();

        // Add listeners for internal beats/bars controls
        useInternalBeatsSwitch.addActionListener(e -> updateInternalBeatsControls());
        useInternalBarsSwitch.addActionListener(e -> updateInternalBarsControls());
        
        // Initialize control states
        updateInternalBeatsControls();
        updateInternalBarsControls();
        
        // Set preferred size
        setPreferredSize(new Dimension(800, 600));

        // Register command listeners
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) {
                    return;
                }

                if (Commands.RULE_ADDED.equals(action.getCommand()) || 
                    Commands.RULE_EDITED.equals(action.getCommand()) ||
                    Commands.RULE_DELETED.equals(action.getCommand())) {

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
    }

    /**
     * Get the updated player with all changes applied
     */
    public Player getUpdatedPlayer() {
        // Apply basic properties from panel
        basicPropertiesPanel.applyToPlayer();
        
        // Apply detailed properties
        detailPanel.updatePlayer();
        
        // Apply toggle switches
        player.setStickyPreset(stickyPresetSwitch.isSelected());
        player.setUseInternalBeats(useInternalBeatsSwitch.isSelected());
        player.setUseInternalBars(useInternalBarsSwitch.isSelected());
        player.setPreserveOnPurge(preserveOnPurgeSwitch.isSelected());
        
        // Apply internal beats/bars
        player.setInternalBeats(((Number) internalBeatsSpinner.getValue()).intValue());
        player.setInternalBars(((Number) internalBarsSpinner.getValue()).intValue());
        
        return player;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add the basic properties panel at the top
        add(basicPropertiesPanel, BorderLayout.NORTH);

        // Main content with parameters and rules
        JPanel mainContent = new JPanel(new BorderLayout());

        // Add the detail panel for sliders
        mainContent.add(detailPanel, BorderLayout.CENTER);

        // Add options panel at the bottom
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
        // Add button handlers
        addRuleButton.addActionListener(e -> CommandBus.getInstance().publish(
                Commands.RULE_ADD_REQUEST, this, player));

        editRuleButton.addActionListener(e -> {
            Rule selectedRule = getSelectedRule();
            if (selectedRule != null) {
                CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
            }
        });

        deleteRuleButton.addActionListener(e -> {
            Rule selectedRule = getSelectedRule();
            if (selectedRule != null) {
                CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this, new Rule[]{selectedRule});
            }
        });

        // Listen for rule updates
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) {
                    return;
                }

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

    private void updateRulesTable() {
        try {
            // Basic validation
            if (rulesTable == null || player == null || player.getRules() == null) {
                logger.error("Cannot update rules table - " + (rulesTable == null ? "table is null"
                        : player == null ? "player is null" : "player rules is null"));
                return;
            }

            // Get table model
            DefaultTableModel model = (DefaultTableModel) rulesTable.getModel();
            model.setRowCount(0); // Clear existing rows

            // Add each rule to the table
            for (Rule rule : player.getRules()) {
                if (rule == null) {
                    continue;
                }

                try {
                    // Get display text for rule properties
                    String operatorText = rule.getOperator() >= 0 && rule.getOperator() < Rule.OPERATORS.length
                            ? Rule.OPERATORS[rule.getOperator()]
                            : "Unknown";

                    String comparisonText = rule.getComparison() >= 0 && rule.getComparison() < Rule.COMPARISONS.length
                            ? Rule.COMPARISONS[rule.getComparison()]
                            : "Unknown";

                    String partText = rule.getPart() == 0 ? "All" : String.valueOf(rule.getPart());

                    model.addRow(new Object[]{operatorText, comparisonText, rule.getValue(), partText});
                } catch (IllegalFormatConversionException e) {
                    logger.error("Format conversion error: " + e.getMessage());
                    model.addRow(new Object[]{
                        rule.getOperator() >= 0 ? Rule.OPERATORS[rule.getOperator()] : "Unknown", 
                        rule.getComparison() >= 0 ? Rule.COMPARISONS[rule.getComparison()] : "Unknown", 
                        String.valueOf(rule.getValue()), 
                        rule.getPart() == 0 ? "All" : String.valueOf(rule.getPart())
                    });
                }
            }

            // Refresh display
            rulesTable.revalidate();
            rulesTable.repaint();
        } catch (Exception e) {
            logger.error("Error updating rules table: " + e.getMessage());
        }
    }

    private void debugPlayerState() {
        logger.info("Rules count: " + (player.getRules() != null ? player.getRules().size() : "NULL"));

        if (player.getRules() != null) {
            int i = 0;
            for (Rule rule : player.getRules()) {
                logger.info("Rule " + (i++) + ": " + rule);
            }
        }
    }

    private void setupRulesTable() {
        // Define column names
        String[] columnNames = {"Comparison", "Operator", "Value", "Part"};
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

        // Load initial rules
        updateRulesTable();

        // Initialize button states
        updateButtonStates();
    }

    private void updateButtonStates() {
        boolean hasSelection = rulesTable.getSelectedRow() >= 0;
        editRuleButton.setEnabled(hasSelection);
        deleteRuleButton.setEnabled(hasSelection);
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
