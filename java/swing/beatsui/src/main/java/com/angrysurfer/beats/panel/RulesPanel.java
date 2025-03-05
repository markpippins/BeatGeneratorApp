package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
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
            switch (e.getActionCommand()) {
                case Commands.RULE_ADD_REQUEST -> {
                    if (Objects.nonNull(currentPlayer)) {
                        CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, currentPlayer);
                    }
                }
                case Commands.RULE_EDIT_REQUEST -> {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this,
                                new Rule[] { selectedRule });
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
            switch (e.getActionCommand()) {
                case Commands.RULE_ADD_REQUEST -> {
                    if (Objects.nonNull(currentPlayer)) {
                        CommandBus.getInstance().publish(Commands.RULE_ADD_REQUEST, this, currentPlayer);
                    }
                }
                case Commands.RULE_EDIT_REQUEST -> {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    Rule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this,
                                new Rule[] { selectedRule });
                    }
                }
            }
        });
    }

    private void setupCommandBusListener() {
        CommandBus.getInstance().register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                switch (action.getCommand()) {
                    case Commands.TICKER_SELECTED -> {
                        clearRules();
                        currentPlayer = null;
                        updateButtonStates();
                    }
                    case Commands.PLAYER_SELECTED -> {
                        if (action.getData() instanceof Player player) {
                            currentPlayer = player;
                            loadRules(player);
                            updateButtonStates();

                            // Auto-select first rule if available
                            if (player.getRules() != null && !player.getRules().isEmpty()) {
                                table.setRowSelectionInterval(0, 0);
                                Rule firstRule = player.getRules().iterator().next();
                                CommandBus.getInstance().publish(Commands.RULE_SELECTED, this, firstRule);
                            }
                        }
                    }

                    case Commands.PLAYER_UPDATED -> {

                        try {
                            if (action.getData() instanceof Player player && (Objects.isNull(currentPlayer)
                                    || player.getId().equals(currentPlayer.getId()))) {
                                currentPlayer = player; // Update reference
                                loadRules(player);
                                // CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this);
                            }
                        } catch (Exception e) {
                            logger.severe("Error updating player: " + e.getMessage());
                        }

                    }
                    case Commands.PLAYER_UNSELECTED -> {
                        setPlayer(null);
                        currentPlayer = null;
                        clearRules();
                        updateButtonStates();
                    }
                    case Commands.RULE_SELECTED -> {
                        if (action.getData() instanceof Rule rule) {
                            // Find and select the rule in the table
                            int index = findRuleIndex(rule);
                            if (index >= 0) {
                                table.setRowSelectionInterval(index, index);
                                table.scrollRectToVisible(table.getCellRect(index, 0, true));
                            }
                        }
                    }
                }
            }
        });

        // Update selection listener to use command bus
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Rule selectedRule = getSelectedRule();
                if (selectedRule != null) {
                    CommandBus.getInstance().publish(Commands.RULE_SELECTED, this, selectedRule);
                } else {
                    CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this);
                }
                updateButtonStates();
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
        boolean hasPlayer = currentPlayer != null;
        // Update button states based on having a player
        buttonPanel.setAddEnabled(hasPlayer);
        buttonPanel.setEditEnabled(false); // Will be enabled when a rule is selected
        buttonPanel.setDeleteEnabled(false);
    }

    private Rule getSelectedRule() {
        int row = table.getSelectedRow();
        if (row >= 0 && Objects.nonNull(currentPlayer) && Objects.nonNull(currentPlayer.getRules())) {
            return new ArrayList<>(currentPlayer.getRules()).get(row);
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

    private void loadRules(Player player) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        if (player != null && player.getRules() != null) {
            List<Rule> sortedRules = new ArrayList<>(player.getRules());
            Collections.sort(sortedRules, (a, b) -> Long.compare(a.getId(), b.getId()));

            for (Rule rule : sortedRules) {
                model.addRow(new Object[] {
                        Rule.OPERATORS[rule.getOperator()],
                        Rule.COMPARISONS[rule.getComparison()],
                        rule.getValue(),
                        rule.getPart() == 0 ? "All" : rule.getPart()
                });
                logger.fine("Added rule: " + rule.getId() +
                        " - Op: " + rule.getOperator() +
                        ", Comp: " + rule.getComparison() +
                        ", Value: " + rule.getValue() +
                        ", Part: " + rule.getPart());
            }
        }
    }

    // private void refreshRules() {
    // DefaultTableModel model = (DefaultTableModel) table.getModel();
    // model.setRowCount(0);

    // if (currentPlayer != null && currentPlayer.getRules() != null) {
    // logger.info("Refreshing rules for player: " + currentPlayer.getName() +
    // " (Rules: " + currentPlayer.getRules().size() + ")");
    // // Add rules to table
    // currentPlayer.getRules().forEach(rule -> {
    // model.addRow(new Object[]{
    // Rule.OPERATORS[rule.getOperator()],
    // Rule.COMPARISONS[rule.getComparison()],
    // rule.getValue(),
    // rule.getPart()
    // });
    // });
    // } else {
    // logger.info("No player or rules to display");
    // }
    // }

    private int findRuleIndex(Rule rule) {
        if (Objects.nonNull(currentPlayer) && Objects.nonNull(currentPlayer.getRules()) && Objects.nonNull(rule)) {
            List<Rule> rules = new ArrayList<>(currentPlayer.getRules());
            for (int i = 0; i < rules.size(); i++) {
                if (Objects.nonNull(rules.get(i)) && Objects.nonNull(rules.get(i).getId()) &&
                        rules.get(i).getId().equals(rule.getId())) {
                    return i;
                }
            }
        }
        return -1;
    }
}
