package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.BusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
class RulesPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RulesPanel.class.getName());
    private Player currentPlayer;

    private final JTable table;
    private final StatusConsumer status;
    private final ButtonPanel buttonPanel;
    private final ContextMenuHelper contextMenu;

    public RulesPanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
        this.table = new JTable();
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
        setupKeyboardShortcuts(); // Add this line

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
        String[] columnNames = { "Comparison", "Operator", "Value", "Part" };
        DefaultTableModel model = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells non-editable
            }
        };
        table.setModel(model);

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

        // Set very small initial column widths
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
                boolean hasSelection = table.getSelectedRow() >= 0;

                // Update button states based on selection
                buttonPanel.setEditEnabled(hasSelection);
                buttonPanel.setDeleteEnabled(hasSelection);
                contextMenu.setEditEnabled(hasSelection);
                contextMenu.setDeleteEnabled(hasSelection);

                // Add button enabled if we have a current player
                buttonPanel.setAddEnabled(currentPlayer != null);
                contextMenu.setAddEnabled(currentPlayer != null);

                logger.info("Rule selection changed - Has selection: " + hasSelection);
            }
        });
    }

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
                        logger.warning("Cannot add rule - no player selected");
                    }
                }
                case Commands.RULE_EDIT_REQUEST -> {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        logger.info("Publishing RULE_EDIT_REQUEST for rule: " + selectedRule.getId());
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    } else {
                        logger.warning("Cannot edit rule - no rule selected");
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    Rule[] selectedRules = getSelectedRules();
                    if (selectedRules.length > 0) {
                        logger.info("Publishing RULE_DELETE_REQUEST for " + selectedRules.length + " rules");
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this, selectedRules);
                    } else {
                        logger.warning("Cannot delete rules - no rules selected");
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

    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new BusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) return;
                
                String cmd = action.getCommand();
                logger.info("RulesPanel received command: " + cmd);
                
                try {
                    if (Commands.PLAYER_SELECTED.equals(cmd)) {
                        if (action.getData() instanceof Player selectedPlayer) {
                            logger.info("RulesPanel processing PLAYER_SELECTED for: " + 
                                   selectedPlayer.getName() + " (ID: " + selectedPlayer.getId() + ")");
                            
                            // Get a fresh copy of the player from SessionManager
                            Player freshPlayer = getFreshPlayer(selectedPlayer.getId());
                            
                            if (freshPlayer != null) {
                                currentPlayer = freshPlayer;
                                loadRules(freshPlayer);
                                logger.info("Loaded " + (freshPlayer.getRules() != null ? 
                                         freshPlayer.getRules().size() : 0) + " rules for player");
                            } else {
                                currentPlayer = selectedPlayer;
                                loadRules(selectedPlayer);
                                logger.warning("Using provided player - could not get fresh copy");
                            }
                            
                            updateButtonStates();
                        }
                    }
                    // Other cases remain the same...
                    switch (cmd) {
                        // Existing cases...
                        
                        case Commands.RULE_ADDED -> {
                            logger.info("Rule added, refreshing table");
                            Player player = PlayerManager.getInstance().getActivePlayer();
                            if (player != null) {
                                refreshRules(player.getRules());
                                
                                // Select the newly added rule if available
                                if (action.getData() instanceof Rule rule) {
                                    selectRuleById(rule.getId());
                                } else {
                                    // If no specific rule, select the last one
                                    selectLastRule();
                                }
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
                                    logger.info("Got fresh player data with " + 
                                              (player.getRules() != null ? player.getRules().size() : 0) + " rules");
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
                                            logger.info("  Rule ID: " + r.getId() + ", Op: " + r.getOperatorText() + 
                                                    ", Comp: " + r.getComparisonText() + ", Value: " + r.getValue());
                                        }
                                    }
                                    
                                    selectRuleById(editedRuleId);
                                }
                            } else {
                                logger.warning("No player available after rule edit");
                            }
                        }
                        
                        case Commands.RULE_DELETED -> {
                            logger.info("Rule(s) deleted, refreshing table");
                            
                            // Get a fresh copy of the player to ensure we have the most up-to-date rules
                            Player player = null;
                            
                            // First check if the data is a Player object
                            if (action.getData() instanceof Player updatedPlayer) {
                                player = updatedPlayer;
                                logger.info("Using player from command data");
                            } else {
                                // Fall back to active player
                                player = PlayerManager.getInstance().getActivePlayer();
                                logger.info("Using active player from PlayerManager");
                            }
                            
                            if (player != null) {
                                // Get a fresh copy to ensure we have current data
                                Player freshPlayer = getFreshPlayer(player.getId());
                                if (freshPlayer != null) {
                                    player = freshPlayer;
                                    logger.info("Using fresh copy of player with " + 
                                              (player.getRules() != null ? player.getRules().size() : 0) + " rules");
                                }
                                
                                // Update current player and refresh display
                                currentPlayer = player;
                                refreshRules(player.getRules());
                                
                                // Select closest available rule or clear selection
                                if (table.getRowCount() > 0) {
                                    // Try to select same row index if possible
                                    int rowToSelect = Math.min(lastSelectedRow, table.getRowCount() - 1);
                                    if (rowToSelect >= 0) {
                                        table.setRowSelectionInterval(rowToSelect, rowToSelect);
                                        handleRuleSelection(rowToSelect);
                                        logger.info("Selected rule at row " + rowToSelect);
                                    }
                                } else {
                                    // No rules left
                                    CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this);
                                    logger.info("No rules left, published RULE_UNSELECTED");
                                }
                            } else {
                                logger.warning("No player available after rule deletion");
                                clearRules();
                            }
                        }

                        // Other cases...
                    }
                } catch (Exception e) {
                    logger.severe("Error processing command: " + e.getMessage());
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
        
        logger.info("Button states updated - Add: " + hasPlayer + 
                   ", Edit/Delete: " + canEdit);
    }

    private Rule getSelectedRule() {
        int row = table.getSelectedRow();
        if (row < 0 || currentPlayer == null || currentPlayer.getRules() == null || currentPlayer.getRules().isEmpty()) {
            return null;
        }
        
        try {
            // Convert view row to model row if table is sorted
            int modelRow = table.convertRowIndexToModel(row);
            
            // Get rules as a list for indexing
            List<Rule> rulesList = new ArrayList<>(currentPlayer.getRules());
            
            if (modelRow < rulesList.size()) {
                Rule rule = rulesList.get(modelRow);
                logger.info("Selected rule: " + rule);
                return rule;
            } else {
                logger.warning("Selected row " + modelRow + " is out of bounds (rules size: " + rulesList.size() + ")");
            }
        } catch (Exception e) {
            logger.severe("Error getting selected rule: " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }

    private Rule[] getSelectedRules() {
        int[] selectedRows = table.getSelectedRows();
        List<Rule> rules = new ArrayList<>();

        if (Objects.nonNull(currentPlayer) && Objects.nonNull(currentPlayer.getRules())) {
            List<Rule> playerRules = new ArrayList<>(currentPlayer.getRules());
            for (int row : selectedRows) {
                // Use model index to get correct rule when table is sorted
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow < playerRules.size()) {
                    rules.add(playerRules.get(modelRow));
                }
            }
        }
        return rules.toArray(new Rule[0]);
    }

    private void clearRules() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    public void setPlayer(Player player) {
        if (player != null) {
            // Try to get a fresh copy of the player
            Player freshPlayer = getFreshPlayer(player.getId());
            if (freshPlayer != null) {
                currentPlayer = freshPlayer;
                loadRules(freshPlayer);
                logger.info("Set player using fresh copy with " + 
                           (freshPlayer.getRules() != null ? freshPlayer.getRules().size() : 0) + " rules");
            } else {
                // Fall back to the provided player
                currentPlayer = player;
                loadRules(player);
                logger.info("Set player using provided reference with " + 
                           (player.getRules() != null ? player.getRules().size() : 0) + " rules");
            }
        } else {
            currentPlayer = null;
            clearRules();
            logger.info("Cleared player reference and rules display");
        }
        
        updateButtonStates();
    }

    // Ensure the loadRules method properly clears and populates the table
    private void loadRules(Player player) {
        try {
            logger.info("Loading rules for player: " + (player != null ? player.getName() : "null"));
            
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);  // Clear existing content
        
            if (player != null && player.getRules() != null && !player.getRules().isEmpty()) {
                logger.info("Found " + player.getRules().size() + " rules to display");
                
                // Debug each rule
                int count = 0;
                for (Rule rule : player.getRules()) {
                    logger.info("Rule " + (++count) + ": " + rule);
                }
                
                // Add rules to table
                for (Rule rule : player.getRules()) {
                    if (rule == null) continue;
                    
                    // Add row with correct column order
                    model.addRow(new Object[] {
                        rule.getOperatorText(),     // Property column - "Beat", "Tick", etc.
                        rule.getComparisonText(),   // Operator column - "==", "<", etc.
                        rule.getValue(),            // Value column
                        rule.getPartText()          // Part column
                    });
                }
            } else {
                logger.warning("No rules to display for player: " + 
                            (player != null ? player.getName() : "null"));
            }
            
            table.revalidate();
            table.repaint();
        } catch (Exception e) {
            logger.severe("Error loading rules: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets a fresh copy of a player from SessionManager
     * @param playerId The ID of the player to fetch
     * @return A fresh Player instance or null if not found
     */
    private Player getFreshPlayer(Long playerId) {
        try {
            Session session = SessionManager.getInstance().getActiveSession();
            if (session != null && session.getPlayers() != null) {
                for (Player player : session.getPlayers()) {
                    if (player.getId().equals(playerId)) {
                        return player;
                    }
                }
            }
        } catch (Exception e) {
            logger.severe("Error getting fresh player: " + e.getMessage());
        }
        return null;
    }
   
    // Add a field to track the last selected rule row
    private int lastSelectedRow = -1;

    // Add a helper method to select a rule by ID
    private void selectRuleById(Long ruleId) {
        if (ruleId == null || currentPlayer == null || currentPlayer.getRules() == null) {
            logger.warning("Cannot select rule: " + 
                          (ruleId == null ? "null rule ID" : 
                           currentPlayer == null ? "null player" : "null rules collection"));
            return;
        }
        
        try {
            logger.info("Looking for rule with ID: " + ruleId + " among " + 
                       currentPlayer.getRules().size() + " rules");
            
            // Convert rules to a list with predictable order
            List<Rule> playerRules = new ArrayList<>(currentPlayer.getRules());
            
            // Find the rule by ID
            int matchIndex = -1;
            for (int i = 0; i < playerRules.size(); i++) {
                Rule rule = playerRules.get(i);
                if (rule.getId().equals(ruleId)) {
                    matchIndex = i;
                    logger.info("Found matching rule at index: " + i);
                    break;
                }
            }
            
            // If found, select it
            if (matchIndex >= 0 && matchIndex < table.getRowCount()) {
                logger.info("Selecting rule at row: " + matchIndex);
                table.setRowSelectionInterval(matchIndex, matchIndex);
                lastSelectedRow = matchIndex;
                table.scrollRectToVisible(table.getCellRect(matchIndex, 0, true));
                
                // Also trigger selection event
                handleRuleSelection(matchIndex);
            } else {
                logger.warning("Could not find matching row for rule ID: " + ruleId + 
                             " (match index: " + matchIndex + ", table rows: " + table.getRowCount() + ")");
            }
        } catch (Exception e) {
            logger.severe("Error selecting rule by ID: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Add a helper method to select the last rule
    private void selectLastRule() {
        if (table.getRowCount() > 0) {
            int lastRow = table.getRowCount() - 1;
            table.setRowSelectionInterval(lastRow, lastRow);
            lastSelectedRow = lastRow;
            handleRuleSelection(lastRow);
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
     * @param rules The rules to display
     */
    private void refreshRules(Set<Rule> rules) {
        try {
            logger.info("Refreshing rules table with " + (rules != null ? rules.size() : 0) + " rules");
            
            // Always get a fresh copy of the player's rules
            if (currentPlayer != null) {
                Player freshPlayer = PlayerManager.getInstance().getActivePlayer(); //, getFreshPlayer(currentPlayer.getId());
                if (freshPlayer != null) {
                    // Use the fresh player's rules instead
                    rules = freshPlayer.getRules();
                    currentPlayer = freshPlayer; // Update current player reference
                    logger.info("Using fresh player with " + 
                               (rules != null ? rules.size() : 0) + " rules");
                }
            }
            
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            model.setRowCount(0);  // Clear existing content
            
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
                for (Rule rule : sortedRules) {
                    if (rule == null) continue;
                    
                    // Add row with correct column order
                    model.addRow(new Object[] {
                        rule.getOperatorText(),     // Property column - "Beat", "Tick", etc.
                        rule.getComparisonText(),   // Operator column - "==", "<", etc.
                        rule.getValue(),            // Value column
                        rule.getPartText()          // Part column
                    });
                }
            } else {
                logger.warning("No rules to display");
            }
            
            table.revalidate();
            table.repaint();
            updateButtonStates();
        } catch (Exception e) {
            logger.severe("Error refreshing rules: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Helper method to get column index by name
    private int getColumnIndex(String columnName) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnName(i).equals(columnName)) {
                return i;
            }
        }
        return -1; // Column not found
    }
}
