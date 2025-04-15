package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.RulesTable;
import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.sequencer.Scale;
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
    private RulesTable rulesTable;
    private final JButton addRuleButton;
    private final JButton editRuleButton;
    private final JButton deleteRuleButton;

    // Internal beats/bars spinners
    private JSpinner internalBarsSpinner;
    private JSpinner internalBeatsSpinner;

    // New fields
    private JCheckBox quantizeCheckbox;
    private JComboBox<String> scaleCombo;

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
        
        // Apply quantize settings
        // player.setQuantizeEnabled(quantizeCheckbox.isSelected());
        if (quantizeCheckbox.isSelected() && scaleCombo.getSelectedItem() != null) {
            player.setScale((String) scaleCombo.getSelectedItem());
        }
        
        return player;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Add the basic properties panel at the top
        add(basicPropertiesPanel, BorderLayout.NORTH);

        // Create main content panel with BorderLayout
        JPanel contentPanel = new JPanel(new BorderLayout());
        
        // Create a top content panel to hold both detail panel and rules panel side by side
        JPanel topContent = new JPanel(new BorderLayout());
        
        // Add the detail panel to CENTER of top content
        topContent.add(detailPanel, BorderLayout.CENTER);
        
        // Create rules panel
        JPanel rulesPanel = createRulesPanel();
        
        // Set the preferred width for the rules panel
        rulesPanel.setPreferredSize(new Dimension(220, rulesPanel.getPreferredSize().height));
        
        // Add rules panel to EAST position of top content
        topContent.add(rulesPanel, BorderLayout.EAST);
        
        // Add top content to CENTER of content panel
        contentPanel.add(topContent, BorderLayout.CENTER);
        
        // Create container panel with no margins for options and quantize
        JPanel optionsContainer = new JPanel(new BorderLayout(0, 0));
        
        // Create the options panel and add it to the LEFT (CENTER) of the container
        JPanel optionsPanel = createOptionsPanel();
        optionsContainer.add(optionsPanel, BorderLayout.CENTER);
        
        // Create quantize panel and add it to the RIGHT (EAST) of the container
        JPanel quantizePanel = createQuantizePanel();
        quantizePanel.setPreferredSize(new Dimension(220, quantizePanel.getPreferredSize().height));
        optionsContainer.add(quantizePanel, BorderLayout.EAST);
        
        // Set a reasonable height for the options container
        optionsContainer.setPreferredSize(new Dimension(optionsContainer.getPreferredSize().width, 100));
        
        // Add the container to SOUTH position to span full width
        contentPanel.add(optionsContainer, BorderLayout.SOUTH);
        
        // Add the content panel to the CENTER of the main layout
        add(contentPanel, BorderLayout.CENTER);
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
        internalBeatsPanel.add(useInternalBeatsSwitch);
        internalBeatsPanel.add(internalBeatsSpinner);

        // Internal Bars section - more compact FlowLayout
        JPanel internalBarsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 1));
        internalBarsPanel.setBorder(BorderFactory.createTitledBorder("Internal Bars"));
        internalBarsPanel.add(useInternalBarsSwitch);
        internalBarsPanel.add(internalBarsSpinner);

        // Add all components to the options panel in one row
        optionsPanel.add(preservePanel);
        optionsPanel.add(stickyPresetPanel);
        optionsPanel.add(internalBeatsPanel);
        optionsPanel.add(internalBarsPanel);
        // Removed: no longer adding quantize panel here

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

            // Update rules in the table model
            rulesTable.getRuleTableModel().setRules(player.getRules());

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

        // Create and configure the rules table
        rulesTable = new RulesTable();
        
        // Initialize with player's rules
        if (player != null && player.getRules() != null) {
            rulesTable.getRuleTableModel().setRules(player.getRules());
        }

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

    /**
     * Creates the quantize panel with scale selection
     */
    private JPanel createQuantizePanel() {
        JPanel quantizePanel = new JPanel(new BorderLayout(5, 5));
        quantizePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Quantize"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        
        // Create container for controls
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        // Create and add quantize checkbox
        quantizeCheckbox = new JCheckBox("Q");
        quantizeCheckbox.setSelected(false);
        //  setSelected(player.getQuantizeEnabled() != null ? player.getQuantizeEnabled() : false);
        controlsPanel.add(quantizeCheckbox, BorderLayout.WEST);
        
        // Create and add scale selector
        // JPanel scalePanel = new JPanel(new BorderLayout(5, 2));
        // scalePanel.add(new JLabel("Scale:"), BorderLayout.WEST);
        
        // Get alphabetized list of scales from Scale class
        String[] scales = Scale.getScales();
        scaleCombo = new JComboBox<>(scales);
        
        // Set current scale if available
        if (player.getScale() != null) {
            scaleCombo.setSelectedItem(player.getScale());
        }
        
        // Enable/disable based on quantize checkbox
        scaleCombo.setEnabled(quantizeCheckbox.isSelected());
        
        // Add listener to enable/disable scale combo based on checkbox
        quantizeCheckbox.addActionListener(e -> 
            scaleCombo.setEnabled(quantizeCheckbox.isSelected()));
        
        // scalePanel.add(scaleCombo, BorderLayout.CENTER);
        controlsPanel.add(scaleCombo, BorderLayout.CENTER);
        
        // Add controls to panel
        quantizePanel.add(controlsPanel, BorderLayout.CENTER);
        
        return quantizePanel;
    }
}
