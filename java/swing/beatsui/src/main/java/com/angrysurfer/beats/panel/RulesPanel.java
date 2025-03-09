package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.service.SessionManager;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RulesPanel extends JPanel {
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
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null) return;
                
                String cmd = action.getCommand();
                
                // Add debug logging to trace command flow
                logger.info("RulesPanel received command: " + cmd);
                
                if (Commands.PLAYER_SELECTED.equals(cmd)) {
                    if (action.getData() instanceof Player) {
                        Player player = (Player) action.getData();
                        logger.info("RulesPanel received PLAYER_SELECTED: " + player.getName() + 
                                   " (ID: " + player.getId() + ") with " + 
                                   (player.getRules() != null ? player.getRules().size() : 0) + " rules");
                        
                        // Update current player reference
                        currentPlayer = player;
                        
                        // Get fresh copy from SessionManager for most up-to-date rules
                        Session session = SessionManager.getInstance().getActiveSession();
                        if (session != null) {
                            Player updatedPlayer = session.getPlayer(player.getId());
                            if (updatedPlayer != null) {
                                logger.info("Using fresh player copy with " + 
                                          (updatedPlayer.getRules() != null ? 
                                           updatedPlayer.getRules().size() : 0) + " rules");
                                currentPlayer = updatedPlayer;
                            }
                        }
                        
                        // Clear and reload rules table with this player's rules
                        loadRules(currentPlayer);
                        updateButtonStates();
                    }
                }
                else if (Commands.PLAYER_UNSELECTED.equals(cmd)) {
                    logger.info("RulesPanel received PLAYER_UNSELECTED");
                    currentPlayer = null;
                    clearRules();
                    updateButtonStates();
                }
                else if (Commands.PLAYER_UPDATED.equals(cmd)) {
                    if (action.getData() instanceof Player) {
                        Player updatedPlayer = (Player) action.getData();
                        if (currentPlayer != null && 
                                updatedPlayer.getId().equals(currentPlayer.getId())) {
                            
                            logger.info("RulesPanel updating rules for player: " + updatedPlayer.getName());
                            currentPlayer = updatedPlayer;  // Update our reference
                            loadRules(updatedPlayer);
                            updateButtonStates();
                        }
                    }
                }
                else if (Commands.RULE_DELETED.equals(cmd) || 
                         Commands.RULE_ADDED.equals(cmd) || 
                         Commands.RULE_EDITED.equals(cmd)) {
                    if (currentPlayer != null) {
                        logger.info("Rule changed, refreshing rules for player: " + currentPlayer.getName());
                        // Get fresh player data from SessionManager
                        Player updatedPlayer = SessionManager.getInstance()
                            .getActiveSession()
                            .getPlayer(currentPlayer.getId());
                            
                        if (updatedPlayer != null) {
                            currentPlayer = updatedPlayer;  // Update reference
                            loadRules(updatedPlayer);
                            updateButtonStates();
                        }
                    }
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
        // logger.info(
        // "Setting player: " + (player != null ? player.getName() + " (ID: " +
        // player.getId() + ")" : "null"));
        this.currentPlayer = player;

        // Update rules display
        if (player != null) {
            // logger.info("Loading rules for player. Rules count: " +
            // (player.getRules() != null ? player.getRules().size() : 0));
            loadRules(player);
        } else {
            logger.info("Clearing rules display - no player selected");
            clearRules();
        }

        // Update UI state
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
   
}
