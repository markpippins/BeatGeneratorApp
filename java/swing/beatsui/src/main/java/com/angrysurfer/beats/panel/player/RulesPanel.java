package com.angrysurfer.beats.panel.player;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.panel.ButtonPanel;
import com.angrysurfer.beats.panel.ContextMenuHelper;
import com.angrysurfer.beats.widget.RuleTableModel;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RulesPanel extends JPanel {

    private static final Logger logger = LoggerFactory.getLogger(RulesPanel.class.getName());
    private Player currentPlayer;

    private final JTable table;
    private final RuleTableModel tableModel; // Use our custom model
    private final ButtonPanel buttonPanel;
    private final ContextMenuHelper contextMenu;
    private int lastSelectedRow = -1;

    public RulesPanel() {
        super(new BorderLayout());

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
        setupCommandBusListener();
        setupButtonListeners();
        setupContextMenu();
        setupKeyboardShortcuts();
    }

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

    // Fix the RULE_DELETE_REQUEST handler in the button listener
    private void setupButtonListeners() {
        buttonPanel.addActionListener(e -> {
            String command = e.getActionCommand();
            logger.info("Button clicked: " + command);

            switch (command) {
                case Commands.RULE_ADD_REQUEST -> {
                    if (currentPlayer != null) {
                        logger.info("Publishing RULE_ADD_REQUEST for player: " + currentPlayer.getName());
                        CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, currentPlayer);
                    } else {
                        logger.error("Cannot add rule - no player selected");
                    }
                }
                case Commands.RULE_EDIT_REQUEST -> {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        logger.info("Publishing RULE_EDIT_REQUEST for rule: " + selectedRule.getId());
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

                        // Remember player for refreshing
                        Player player = currentPlayer;

                        // Publish the delete request with the rules to delete
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this, selectedRules);

                        // Log deletion request
                        logger.info("Published rule delete request for " + selectedRules.length + " rules");
                    }
                }
            }
        });

        // Update context menu handler to match button behavior
        contextMenu.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case Commands.RULE_ADD_REQUEST -> {
                    if (Objects.nonNull(currentPlayer)) {
                        CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, currentPlayer);
                    }
                }
                case Commands.RULE_EDIT_REQUEST -> {
                    Rule[] selectedRules = getSelectedRules();
                    if (selectedRules.length > 0) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRules[0]);
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    Rule[] selectedRules = getSelectedRules();
                    if (selectedRules.length > 0) {
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this, selectedRules);
                    }
                }
            }
        });
    }

    private void setupContextMenu() {
        contextMenu.install(table);
        contextMenu.addActionListener(e -> {
            String command = e.getActionCommand();
            logger.info("Context menu action: " + command);

            switch (command) {
                case Commands.RULE_ADD_REQUEST -> {
                    if (currentPlayer != null) {
                        logger.info("Publishing RULE_ADD_REQUEST from context menu");
                        CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, currentPlayer);
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

    // Fix the RULE_DELETED handler in the command listener
    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null || action.getSender() == this) {
                    return;
                }

                String cmd = action.getCommand();
                logger.info("RulesPanel received command: " + cmd);

                try {
                    if (Commands.PLAYER_ACTIVATED.equals(cmd)) {
                        if (action.getData() instanceof Player selectedPlayer) {
                            logger.info("RulesPanel processing PLAYER_ACTIVATED for: "
                                    + selectedPlayer.getName() + " (ID: " + selectedPlayer.getId() + ")");

                            // Get a fresh copy of the player from SessionManager
                            currentPlayer = selectedPlayer;
                            loadRules(selectedPlayer);
                            logger.error("Using provided player - could not get fresh copy");

                            updateButtonStates();
                        }
                    }
                    // Other cases remain the same...
                    switch (cmd) {
                        // Existing cases...

                        case Commands.RULE_ADDED -> {
                            logger.info("Rule added, refreshing table");

                            // Get added rule from command data
                            Rule addedRule = null;
                            if (action.getData() instanceof Rule rule) {
                                addedRule = rule;
                                logger.info("Added rule ID: " + rule.getId());
                            }

                            // Get a fresh copy of the player to avoid duplicate rules
                            Player player = null;

                            // Try to get the active player from PlayerManager
                            player = PlayerManager.getInstance().getActivePlayer();
                            if (player != null) {
                                // Always get a fresh copy to avoid stale data
                                Player freshPlayer = getFreshPlayer(player.getId());
                                if (freshPlayer != null) {
                                    player = freshPlayer;
                                    logger.info("Using fresh player with "
                                            + (player.getRules() != null ? player.getRules().size() : 0) + " rules");

                                    // If we have the player's rules, check for duplicates by ID
                                    if (player.getRules() != null && addedRule != null) {
                                        // Log all rule IDs to help debug
                                        logger.info("Player rule IDs:");
                                        Set<Long> ruleIds = new HashSet<>();
                                        for (Rule rule : player.getRules()) {
                                            logger.info("  Rule ID: " + rule.getId());
                                            ruleIds.add(rule.getId());
                                        }

                                        // Check if we found multiple copies of the same rule
                                        if (Collections.frequency(player.getRules().stream()
                                                .map(Rule::getId).collect(Collectors.toList()),
                                                addedRule.getId()) > 1) {
                                            logger.error("Duplicate rule detected! ID: " + addedRule.getId());
                                        }
                                    }
                                }

                                // Update current player and refresh the table
                                currentPlayer = player;
                                clearRules(); // Explicitly clear the table first
                                refreshRules(player.getRules());

                                // After refresh, select the newly added rule or the last one
                                if (addedRule != null) {
                                    selectRuleById(addedRule.getId());
                                } else {
                                    selectLastRule();
                                }
                            } else {
                                logger.error("No active player available after adding rule");
                            }
                        }

                        case Commands.RULE_EDITED -> {
                            logger.info("Rule edited, refreshing table");

                            // Store the edited rule ID before refreshing
                            Long editedRuleId = null;
                            if (action.getData() instanceof Rule rule) {
                                editedRuleId = rule.getId();
                                logger.info("Edited rule ID: " + editedRuleId);
                            }

                            // Get a fresh copy of the player to ensure we have updated rules
                            Player player = PlayerManager.getInstance().getActivePlayer();
                            if (player != null) {
                                Player freshPlayer = getFreshPlayer(player.getId());
                                if (freshPlayer != null) {
                                    player = freshPlayer;
                                    logger.info("Got fresh player data with "
                                            + (player.getRules() != null ? player.getRules().size() : 0) + " rules");
                                }

                                // Update current player and refresh rules table
                                currentPlayer = player;
                                refreshRules(player.getRules());

                                // Re-select the edited rule if we have its ID
                                if (editedRuleId != null) {
                                    // Log all available rule IDs to help diagnose selection issues
                                    if (player.getRules() != null) {
                                        logger.info("Available rule IDs for selection:");
                                        for (Rule r : player.getRules()) {
                                            logger.info("  Rule ID: " + r.getId() + ", Op: " + r.getOperatorText()
                                                    + ", Comp: " + r.getComparisonText() + ", Value: " + r.getValue());
                                        }
                                    }

                                    selectRuleById(editedRuleId);
                                }
                            } else {
                                logger.error("No player available after rule edit");
                            }
                        }

                        case Commands.RULE_DELETED -> {
                            logger.info("Rule(s) deleted, refreshing table");

                            // Get the freshest possible player data
                            Player updatedPlayer = null;

                            // Try to get player from command data first
                            if (action.getData() instanceof Player player) {
                                updatedPlayer = player;
                                logger.info("Using player from command data");
                            } // Then try active player from PlayerManager
                            else {
                                updatedPlayer = PlayerManager.getInstance().getActivePlayer();
                                logger.info("Using active player from manager");
                            }

                            // As a last resort, use our current player
                            if (updatedPlayer == null) {
                                updatedPlayer = currentPlayer;
                                logger.info("Falling back to current player reference");
                            }

                            // Always try to get a fresh copy
                            if (updatedPlayer != null) {
                                Player freshPlayer = getFreshPlayer(updatedPlayer.getId());
                                if (freshPlayer != null) {
                                    updatedPlayer = freshPlayer;
                                    logger.info("Using fresh player copy with "
                                            + (freshPlayer.getRules() != null ? freshPlayer.getRules().size() : 0)
                                            + " rules");
                                }

                                // Update our reference and refresh the table
                                currentPlayer = updatedPlayer;

                                // Use the player's rules for display
                                Set<Rule> rulesToDisplay = updatedPlayer.getRules();
                                refreshRules(rulesToDisplay);

                                // Select an appropriate row if there are any rules left
                                if (table.getRowCount() > 0) {
                                    int rowToSelect = Math.min(lastSelectedRow, table.getRowCount() - 1);
                                    if (rowToSelect >= 0) {
                                        table.setRowSelectionInterval(rowToSelect, rowToSelect);
                                        handleRuleSelection(rowToSelect);
                                        logger.info("Selected rule at row " + rowToSelect + " after deletion");
                                    }
                                } else {
                                    logger.info("No rules left after deletion");
                                    // Publish rule unselected event since no rules remain
                                    CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this);
                                }
                            } else {
                                clearRules();
                                logger.error("No player available after rule deletion");
                            }
                        }

                        // Other cases...
                    }
                } catch (Exception e) {
                    logger.error("Error processing command: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });

        // Update selection listener to use command bus
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtonStates(); // Update button states on selection change

                Rule selectedRule = getSelectedRule();
                if (selectedRule != null) {
                    CommandBus.getInstance().publish(Commands.RULE_SELECTED, this, selectedRule);
                } else {
                    CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this, null);
                }
            }
        });
    }

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

    private void updateButtonStates() {
        // Enable add button if we have a current player
        boolean hasPlayer = currentPlayer != null;
        buttonPanel.setAddEnabled(hasPlayer);
        contextMenu.setAddEnabled(hasPlayer);

        // Enable edit and delete only if we have both a player AND a selection
        boolean hasSelection = table.getSelectedRow() >= 0;
        boolean canEdit = hasPlayer && hasSelection;

        buttonPanel.setEditEnabled(canEdit);
        buttonPanel.setDeleteEnabled(canEdit);
        contextMenu.setEditEnabled(canEdit);
        contextMenu.setDeleteEnabled(canEdit);

        logger.info("Button states updated - Add: " + hasPlayer
                + ", Edit/Delete: " + canEdit);
    }

    private Rule getSelectedRule() {
        int row = table.getSelectedRow();
        if (row < 0) {
            return null;
        }

        return tableModel.getRuleAt(row);
    }

    private Rule[] getSelectedRules() {
        int[] selectedRows = table.getSelectedRows();
        return tableModel.getRulesAt(selectedRows);
    }

    private void clearRules() {
        tableModel.setRules(null); // Use model's method
    }

    public void setPlayer(Player player) {
        currentPlayer = player; // Store reference to current player
        if (player != null) {
            loadRules(player);
        } else {
            clearRules();
        }
        updateButtonStates();
    }

    // Ensure the loadRules method properly clears and populates the table
    private void loadRules(Player player) {
        try {
            logger.info("Loading rules for player: " + (player != null ? player.getName() : "null"));

            if (player != null && player.getRules() != null) {
                tableModel.setRules(player.getRules()); // Use model's method
            } else {
                tableModel.setRules(null);
                logger.error("No rules to display for player");
            }

            table.revalidate();
            table.repaint();
        } catch (Exception e) {
            logger.error("Error loading rules: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets a fresh copy of a player from SessionManager
     *
     * @param playerId The ID of the player to fetch
     * @return A fresh Player instance or null if not found
     */
    // Improve the getFreshPlayer method for reliability
    private Player getFreshPlayer(Long playerId) {
        if (playerId == null) {
            logger.error("Cannot get fresh player: null ID");
            return null;
        }

        try {
            // First try from active session
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null && session.getPlayers() != null) {
                for (Player player : session.getPlayers()) {
                    if (playerId.equals(player.getId())) {
                        logger.info("Found player " + playerId + " in active session");
                        return player;
                    }
                }
            }

            logger.error("Player " + playerId + " not found in active session");
            return null;
        } catch (Exception e) {
            logger.error("Error getting fresh player: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Add a helper method to select a rule by ID
    private void selectRuleById(Long ruleId) {
        if (ruleId == null) {
            return;
        }

        int row = tableModel.findRuleRowById(ruleId);
        if (row >= 0) {
            logger.info("Selecting rule at row: " + row);
            table.setRowSelectionInterval(row, row);
            lastSelectedRow = row;
            table.scrollRectToVisible(table.getCellRect(row, 0, true));
        } else {
            logger.error("Could not find rule with ID: " + ruleId);
        }
    }

    // Add a helper method to select the last rule
    private void selectLastRule() {
        int lastRow = tableModel.getRowCount() - 1;
        if (lastRow >= 0) {
            table.setRowSelectionInterval(lastRow, lastRow);
            lastSelectedRow = lastRow;
        }
    }

    // Update handleRuleSelection to track last selection
    private void handleRuleSelection(int row) {
        if (row >= 0) {
            lastSelectedRow = row;
        }

        // Existing code...
    }

    /**
     * Refresh the rules table with a new set of rules
     *
     * @param rules The rules to display
     */
    // Fix the refreshRules method to properly use fresh player data
    private void refreshRules(Set<Rule> rules) {
        try {
            logger.info("Refreshing rules table with " + (rules != null ? rules.size() : 0) + " rules");

            RuleTableModel model = (RuleTableModel) table.getModel();
            // model.setRowCount(0); // Clear existing content
            model.setRules(Collections.emptySet());

            // Reset selection tracking
            lastSelectedRow = -1;

            if (rules != null && !rules.isEmpty()) {
                // Debug each rule
                int count = 0;
                for (Rule rule : rules) {
                    logger.info("Rule " + (++count) + ": " + rule);
                }

                // Add rules to table in consistent order to make selection work
                List<Rule> sortedRules = new ArrayList<>(rules);
                // Sort by operator and value for consistent display order
                sortedRules.sort((r1, r2) -> {
                    int comp = Integer.compare(r1.getOperator(), r2.getOperator());
                    if (comp == 0) {
                        return Double.compare(r1.getValue(), r2.getValue());
                    }
                    return comp;
                });

                // Add sorted rules to table
                // for (Rule rule : sortedRules) {
                //     if (rule == null)
                //         continue;
                //     // Add row with correct column order
                //     model. addRow(new Object[] {
                //             rule.getOperatorText(), // Property column - "Beat", "Tick", etc.
                //             rule.getComparisonText(), // Operator column - "==", "<", etc.
                //             rule.getValue(), // Value column
                //             rule.getPartText() // Part column
                //     });
                // }
                model.setRules(rules);
            } else {
                logger.error("No rules to display");
            }

            table.revalidate();
            table.repaint();
            updateButtonStates();
        } catch (Exception e) {
            logger.error("Error refreshing rules: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
