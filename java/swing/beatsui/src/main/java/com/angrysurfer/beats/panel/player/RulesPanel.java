package com.angrysurfer.beats.panel.player;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;

import com.angrysurfer.core.event.PlayerRuleUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.panel.ButtonPanel;
import com.angrysurfer.beats.panel.ContextMenuHelper;
import com.angrysurfer.beats.panel.PlayerAwarePanel;
import com.angrysurfer.beats.widget.RuleTableModel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;

/**
 * Panel for viewing and editing player rules
 * Now extends PlayerAwarePanel for improved player handling
 */
@Getter
public class RulesPanel extends PlayerAwarePanel {
    private static final Logger logger = LoggerFactory.getLogger(RulesPanel.class);

    private final JTable table;
    private final RuleTableModel tableModel;
    private final ButtonPanel buttonPanel;
    private final ContextMenuHelper contextMenu;
    private int lastSelectedRow = -1;

    /**
     * Create a new Rules Panel
     */
    public RulesPanel() {
        super(); // Call PlayerAwarePanel constructor
        setLayout(new BorderLayout());

        // Create our custom table model
        this.tableModel = new RuleTableModel();

        // Use the model in the table
        this.table = new JTable(tableModel);
        this.buttonPanel = new ButtonPanel(
                Commands.RULE_ADD_REQUEST,
                Commands.RULE_EDIT_REQUEST,
                Commands.RULE_DELETE_REQUEST);
        this.contextMenu = new ContextMenuHelper(
                Commands.RULE_ADD_REQUEST,
                Commands.RULE_EDIT_REQUEST,
                Commands.RULE_DELETE_REQUEST);

        setupTable();
        setupLayout();
        setupButtonListeners();
        setupContextMenu();
        setupKeyboardShortcuts();
    }

    /**
     * Called when a player is activated in this panel
     */
    @Override
    public void handlePlayerActivated() {
        logger.info("Player activated: {}", getPlayer() != null ? getPlayer().getName() : "null");
        loadRules(getPlayer());
        updateButtonStates();
    }

    /**
     * Called when the panel's player is updated
     */
    @Override
    public void handlePlayerUpdated() {
        logger.info("Player updated: {}", getPlayer() != null ? getPlayer().getName() : "null");
        loadRules(getPlayer());
        updateButtonStates();
    }

    /**
     * Set up the panel layout
     */
    private void setupLayout() {
        JPanel topPanel = new JPanel(new BorderLayout());

        // Create a wrapper panel for the button panel with BorderLayout
        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.add(buttonPanel, BorderLayout.CENTER);
        topPanel.add(buttonWrapper, BorderLayout.NORTH);

        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        add(topPanel, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(table);
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.add(scrollPane);
        tableWrapper.setPreferredSize(buttonPanel.getPreferredSize());

        add(tableWrapper, BorderLayout.CENTER);
    }

    /**
     * Set up the rules table
     */
    private void setupTable() {
        // Center align all columns first
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Left align for Operator column
        DefaultTableCellRenderer leftRenderer = new DefaultTableCellRenderer();
        leftRenderer.setHorizontalAlignment(JLabel.LEFT);

        // Apply renderers to columns
        table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer); // Operator column left-aligned
        for (int i = 1; i < table.getColumnCount(); i++) { // Rest centered
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Change selection mode to allow multiple selections
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(60); // Operator
        table.getColumnModel().getColumn(1).setPreferredWidth(60); // Comparison
        table.getColumnModel().getColumn(2).setPreferredWidth(40); // Value
        table.getColumnModel().getColumn(3).setPreferredWidth(40); // Part

        // Allow table to be smaller than the sum of column widths
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        // Add double-click listener
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
            }
        });

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates();
                Rule selectedRule = getSelectedRule();
                if (selectedRule != null) {
                    CommandBus.getInstance().publish(Commands.RULE_SELECTED, this, selectedRule);
                } else {
                    CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this, null);
                }
            }
        });
    }

    /**
     * Set up button listeners
     */
    private void setupButtonListeners() {
        buttonPanel.addActionListener(e -> {
            String command = e.getActionCommand();
            logger.info("Button clicked: {}", command);

            switch (command) {
                case Commands.RULE_ADD_REQUEST -> {
                    Player player = getPlayer();
                    if (player != null) {
                        logger.info("Publishing RULE_ADD_REQUEST for player: {}", player.getName());
                        CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, player);
                    } else {
                        logger.error("Cannot add rule - no player selected");
                    }
                }
                case Commands.RULE_EDIT_REQUEST -> {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        logger.info("Publishing RULE_EDIT_REQUEST for rule: {}", selectedRule.getId());
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    } else {
                        logger.error("Cannot edit rule - no rule selected");
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    Rule[] selectedRules = getSelectedRules();
                    if (selectedRules.length > 0) {
                        // Remember row for reselection
                        lastSelectedRow = table.getSelectedRow();

                        // Publish the delete request with the rules to delete
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this, selectedRules);

                        // Log deletion request
                        logger.info("Published rule delete request for {} rules", selectedRules.length);
                    }
                }
            }
        });
    }

    /**
     * Set up context menu
     */
    private void setupContextMenu() {
        contextMenu.install(table);
        contextMenu.addActionListener(e -> {
            String command = e.getActionCommand();
            logger.info("Context menu action: {}", command);

            switch (command) {
                case Commands.RULE_ADD_REQUEST -> {
                    Player player = getPlayer();
                    if (player != null) {
                        logger.info("Publishing RULE_ADD_REQUEST from context menu");
                        CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, player);
                    }
                }
                case Commands.RULE_EDIT_REQUEST -> {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        logger.info("Publishing RULE_EDIT_REQUEST from context menu");
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    Rule[] selectedRules = getSelectedRules();
                    if (selectedRules.length > 0) {
                        logger.info("Publishing RULE_DELETE_REQUEST from context menu");
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this, selectedRules);
                    }
                }
            }
        });
    }

    /**
     * Set up keyboard shortcuts
     */
    private void setupKeyboardShortcuts() {
        // Make the table focusable
        table.setFocusable(true);

        // Add key listener to handle delete key
        table.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE) {
                    Rule[] selectedRules = getSelectedRules();
                    if (selectedRules.length > 0) {
                        int confirm = JOptionPane.showConfirmDialog(
                                RulesPanel.this,
                                "Are you sure you want to delete the selected rule(s)?",
                                "Confirm Delete",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.WARNING_MESSAGE);

                        if (confirm == JOptionPane.YES_OPTION) {
                            CommandBus.getInstance().publish(
                                    Commands.RULE_DELETE_REQUEST,
                                    RulesPanel.this,
                                    selectedRules);
                        }
                    }
                }
            }
        });
    }

    /**
     * Update the enabled state of the buttons based on selection
     */
    private void updateButtonStates() {
        // Enable add button if we have a current player
        boolean hasPlayer = getPlayer() != null;
        buttonPanel.setAddEnabled(hasPlayer);
        contextMenu.setAddEnabled(hasPlayer);

        // Enable edit and delete only if we have both a player AND a selection
        boolean hasSelection = table.getSelectedRow() >= 0;
        boolean canEdit = hasPlayer && hasSelection;

        buttonPanel.setEditEnabled(canEdit);
        buttonPanel.setDeleteEnabled(canEdit);
        contextMenu.setEditEnabled(canEdit);
        contextMenu.setDeleteEnabled(canEdit);

        logger.debug("Button states updated - Add: {}, Edit/Delete: {}", hasPlayer, canEdit);
    }

    /**
     * Get the selected rule
     */
    private Rule getSelectedRule() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }

        return tableModel.getRuleAt(row);
    }

    /**
     * Get all selected rules
     */
    private Rule[] getSelectedRules() {
        int[] selectedRows = table.getSelectedRows();
        return tableModel.getRulesAt(selectedRows);
    }

    /**
     * Clear all rules from the table
     */
    private void clearRules() {
        tableModel.setRules(null);
    }

    /**
     * Override onAction from IBusListener to handle rule-specific commands
     * PlayerAwarePanel already handles player events for us
     */
    @Override
    public void onAction(Command action) {
        super.onAction(action); // Let parent handle player events
        
        if (action == null || action.getCommand() == null || action.getSender() == this) {
            return;
        }

        try {
            switch (action.getCommand()) {
                // Add case for new rule update event
                case Commands.PLAYER_RULE_UPDATE_EVENT -> {
                    if (action.getData() instanceof PlayerRuleUpdateEvent event) {
                        // Only process if this event affects our current player
                        if (getPlayer() != null && getPlayer().getId().equals(event.getPlayer().getId())) {
                            logger.info("Rule update event received: {}", event.getUpdateType());
                            
                            // Store the updated rule ID before refreshing
                            Long updatedRuleId = event.getUpdatedRule() != null ? event.getUpdatedRule().getId() : null;
                            
                            // Refresh table based on update type
                            switch (event.getUpdateType()) {
                                case RULE_ADDED -> {
                                    refreshRules(event.getPlayer().getRules());
                                    // Select the newly added rule
                                    if (updatedRuleId != null) {
                                        selectRuleById(updatedRuleId);
                                    } else {
                                        selectLastRule();
                                    }
                                }
                                case RULE_EDITED -> {
                                    refreshRules(event.getPlayer().getRules());
                                    // Re-select the edited rule
                                    if (updatedRuleId != null) {
                                        selectRuleById(updatedRuleId);
                                    }
                                }
                                case RULE_DELETED, ALL_RULES_DELETED -> {
                                    refreshRules(event.getPlayer().getRules());
                                    // Try to maintain selection position
                                    if (table.getRowCount() > 0) {
                                        int rowToSelect = Math.min(lastSelectedRow, table.getRowCount() - 1);
                                        if (rowToSelect >= 0) {
                                            table.setRowSelectionInterval(rowToSelect, rowToSelect);
                                            lastSelectedRow = rowToSelect;
                                        }
                                    }
                                }
                                default -> refreshRules(event.getPlayer().getRules());
                            }
                        }
                    }
                }
                
                // Keep your existing cases for backward compatibility
                case Commands.RULE_ADDED -> {
                    logger.info("Rule added, refreshing table");

                    // Get added rule from command data
                    Rule[] addedRule = new Rule[1];
                    if (action.getData() instanceof Rule rule) {
                        addedRule[0] = rule;
                        logger.info("Added rule ID: {}", rule.getId());
                    }

                    // Wait for player to update, then refresh with player's updated rules
                    SwingUtilities.invokeLater(() -> {
                        // Check if our targetPlayer has been updated with the new rule
                        if (getPlayer() != null) {
                            Player playerWithUpdatedRules = getFreshPlayer(getPlayer().getId());
                            if (playerWithUpdatedRules != null) {
                                // Update our current table with fresh rules
                                clearRules();
                                refreshRules(playerWithUpdatedRules.getRules());

                                // Select the newly added rule if we know its ID
                                if (addedRule[0] != null) {
                                    selectRuleById(addedRule[0].getId());
                                } else {
                                    selectLastRule();
                                }
                            }
                        }
                    });
                }

                case Commands.RULE_EDITED -> {
                    logger.info("Rule edited, refreshing table");

                    // Store the edited rule ID before refreshing
                    Long[] editedRuleId = new Long[1];
                    if (action.getData() instanceof Rule rule) {
                        editedRuleId[0] = rule.getId();
                        logger.info("Edited rule ID: {}", editedRuleId[0]);
                    }

                    // Wait for player to update, then refresh with player's updated rules
                    SwingUtilities.invokeLater(() -> {
                        // Get a fresh copy of the player to ensure we have updated rules
                        if (getPlayer() != null) {
                            Player playerWithUpdatedRules = getFreshPlayer(getPlayer().getId());
                            if (playerWithUpdatedRules != null) {
                                refreshRules(playerWithUpdatedRules.getRules());

                                // Re-select the edited rule if we have its ID
                                if (editedRuleId[0] != null) {
                                    selectRuleById(editedRuleId[0]);
                                }
                            }
                        }
                    });
                }

                case Commands.RULE_DELETED -> {
                    logger.info("Rule(s) deleted, refreshing table");

                    // Wait for player to update, then refresh with player's updated rules
                    SwingUtilities.invokeLater(() -> {
                        // Get the freshest possible player data
                        if (getPlayer() != null) {
                            Player playerWithUpdatedRules = getFreshPlayer(getPlayer().getId());
                            if (playerWithUpdatedRules != null) {
                                // Update our table with fresh rules
                                Set<Rule> rulesToDisplay = playerWithUpdatedRules.getRules();
                                refreshRules(rulesToDisplay);

                                // Select an appropriate row if there are any rules left
                                if (table.getRowCount() > 0) {
                                    int rowToSelect = Math.min(lastSelectedRow, table.getRowCount() - 1);
                                    if (rowToSelect >= 0) {
                                        table.setRowSelectionInterval(rowToSelect, rowToSelect);
                                        lastSelectedRow = rowToSelect;
                                        table.scrollRectToVisible(table.getCellRect(rowToSelect, 0, true));
                                        logger.info("Selected rule at row {} after deletion", rowToSelect);
                                    }
                                } else {
                                    logger.info("No rules left after deletion");
                                    // Publish rule unselected event since no rules remain
                                    CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this);
                                }
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            logger.error("Error processing command: {}", e.getMessage(), e);
        }
    }


    /**
     * Load rules for a player
     */
    private void loadRules(Player player) {
        try {
            logger.info("Loading rules for player: {}", player != null ? player.getName() : "null");

            if (player != null && player.getRules() != null) {
                tableModel.setRules(player.getRules());
            } else {
                tableModel.setRules(null);
                logger.info("No rules to display for player");
            }

            table.revalidate();
            table.repaint();
        } catch (Exception e) {
            logger.error("Error loading rules: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets a fresh copy of a player from SessionManager
     *
     * @param playerId The ID of the player to fetch
     * @return A fresh Player instance or null if not found
     */
    private Player getFreshPlayer(Long playerId) {
        if (playerId == null) {
            logger.error("Cannot get fresh player: null ID");
            return null;
        }

        try {
            // Try from active session
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null && session.getPlayers() != null) {
                for (Player player : session.getPlayers()) {
                    if (Objects.equals(playerId, player.getId())) {
                        logger.info("Found player {} in active session", playerId);
                        return player;
                    }
                }
            }

            logger.info("Player {} not found in active session", playerId);
            return null;
        } catch (Exception e) {
            logger.error("Error getting fresh player: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Select a rule by ID
     */
    private void selectRuleById(Long ruleId) {
        if (ruleId == null) {
            return;
        }

        int row = tableModel.findRuleRowById(ruleId);
        if (row >= 0) {
            logger.info("Selecting rule at row: {}", row);
            table.setRowSelectionInterval(row, row);
            lastSelectedRow = row;
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
        } else {
            logger.info("Could not find rule with ID: {}", ruleId);
        }
    }

    /**
     * Select the last rule in the table
     */
    private void selectLastRule() {
        int lastRow = tableModel.getRowCount() - 1;
        if (lastRow >= 0) {
            table.setRowSelectionInterval(lastRow, lastRow);
            lastSelectedRow = lastRow;
        }
    }

    /**
     * Refresh the rules table with a new set of rules
     */
    private void refreshRules(Set<Rule> rules) {
        try {
            logger.info("Refreshing rules table with {} rules", rules != null ? rules.size() : 0);

            // Clear existing rules
            tableModel.setRules(Collections.emptySet());

            // Reset selection tracking
            lastSelectedRow = -1;

            if (rules != null && !rules.isEmpty()) {
                // Sort rules for consistent display
                List<Rule> sortedRules = new ArrayList<>(rules);
                sortedRules.sort((r1, r2) -> {
                    int comp = Integer.compare(r1.getOperator(), r2.getOperator());
                    if (comp == 0) {
                        return Double.compare(r1.getValue(), r2.getValue());
                    }
                    return comp;
                });

                // Set sorted rules to table model
                tableModel.setRules(rules);
            } else {
                logger.info("No rules to display");
            }

            table.revalidate();
            table.repaint();
            updateButtonStates();
        } catch (Exception e) {
            logger.error("Error refreshing rules: {}", e.getMessage(), e);
        }
    }
}
