package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
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
import com.angrysurfer.core.proxy.ProxyRule;
import com.angrysurfer.core.proxy.ProxyStrike;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RulesPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(RulesPanel.class.getName());

    private final JTable table;
    private final StatusConsumer status;
    private final ButtonPanel buttonPanel;
    private final ContextMenuHelper contextMenu;
    private ProxyStrike currentPlayer;

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

    }

    private void setupLayout() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(buttonPanel, BorderLayout.NORTH);

        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));  // Remove the margin completely
        add(topPanel, BorderLayout.NORTH);

        // Wrap the scroll pane in a panel that respects preferred size
        JScrollPane scrollPane = new JScrollPane(table);
        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.add(scrollPane);

        // Make the panel use the minimum width needed
        tableWrapper.setPreferredSize(buttonPanel.getPreferredSize());

        add(tableWrapper, BorderLayout.CENTER);
    }

    private void setupTable() {
        String[] columnNames = { "Operator", "Comparison", "Value", "Part" };
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
        table.getColumnModel().getColumn(0).setCellRenderer(leftRenderer);  // Operator column left-aligned
        for (int i = 1; i < table.getColumnCount(); i++) {                  // Rest centered
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
                    ProxyRule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
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
                    ProxyRule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    ProxyRule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this,
                                new ProxyRule[] { selectedRule });
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
                    ProxyRule[] selectedRules = getSelectedRules();
                    if (selectedRules.length > 0) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRules[0]);
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    ProxyRule[] selectedRules = getSelectedRules();
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
                    ProxyRule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_EDIT_REQUEST, this, selectedRule);
                    }
                }
                case Commands.RULE_DELETE_REQUEST -> {
                    ProxyRule selectedRule = getSelectedRule();
                    if (selectedRule != null) {
                        CommandBus.getInstance().publish(Commands.RULE_DELETE_REQUEST, this,
                                new ProxyRule[] { selectedRule });
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
                        if (action.getData() instanceof ProxyStrike player) {
                            currentPlayer = player;
                            loadRules(player);
                            updateButtonStates();

                            // Auto-select first rule if available
                            if (player.getRules() != null && !player.getRules().isEmpty()) {
                                table.setRowSelectionInterval(0, 0);
                                ProxyRule firstRule = player.getRules().iterator().next();
                                CommandBus.getInstance().publish(Commands.RULE_SELECTED, this, firstRule);
                            }
                        }
                    }
                    case Commands.PLAYER_UPDATED -> {

                        try {
                            if (action.getData() instanceof ProxyStrike player && (Objects.isNull(currentPlayer)
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
                        currentPlayer = null;
                        clearRules();
                        updateButtonStates();
                    }
                    case Commands.RULE_SELECTED -> {
                        if (action.getData() instanceof ProxyRule rule) {
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
                ProxyRule selectedRule = getSelectedRule();
                if (selectedRule != null) {
                    CommandBus.getInstance().publish(Commands.RULE_SELECTED, this, selectedRule);
                } else {
                    CommandBus.getInstance().publish(Commands.RULE_UNSELECTED, this);
                }
                updateButtonStates();
            }
        });
    }

    private void updateButtonStates() {
        boolean hasPlayer = Objects.nonNull(currentPlayer);
        boolean hasSelection = table.getSelectedRow() >= 0;

        buttonPanel.setAddEnabled(hasPlayer);
        buttonPanel.setEditEnabled(hasSelection);
        buttonPanel.setDeleteEnabled(hasSelection);

        contextMenu.setAddEnabled(hasPlayer);
        contextMenu.setEditEnabled(hasSelection);
        contextMenu.setDeleteEnabled(hasSelection);
    }

    private ProxyRule getSelectedRule() {
        int row = table.getSelectedRow();
        if (row >= 0 && Objects.nonNull(currentPlayer) && Objects.nonNull(currentPlayer.getRules())) {
            return new ArrayList<>(currentPlayer.getRules()).get(row);
        }
        return null;
    }

    private ProxyRule[] getSelectedRules() {
        int[] selectedRows = table.getSelectedRows();
        List<ProxyRule> rules = new ArrayList<>();

        if (Objects.nonNull(currentPlayer) && Objects.nonNull(currentPlayer.getRules())) {
            List<ProxyRule> playerRules = new ArrayList<>(currentPlayer.getRules());
            for (int row : selectedRows) {
                // Use model index to get correct rule when table is sorted
                int modelRow = table.convertRowIndexToModel(row);
                if (modelRow < playerRules.size()) {
                    rules.add(playerRules.get(modelRow));
                }
            }
        }
        return rules.toArray(new ProxyRule[0]);
    }

    private void clearRules() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    public void loadRules(ProxyStrike player) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        if (Objects.nonNull(player) && Objects.nonNull(player.getRules())) {
            for (ProxyRule rule : player.getRules()) {
                model.addRow(new Object[] {
                        ProxyRule.OPERATORS[rule.getOperator()],
                        ProxyRule.COMPARISONS[rule.getComparison()],
                        rule.getValue(),
                        rule.getPart() == 0 ? "All" : rule.getPart()
                });
                // Notify that a rule was loaded
                CommandBus.getInstance().publish(Commands.RULE_ADDED, this, rule);
            }
        }
    }

    private int findRuleIndex(ProxyRule rule) {
        if (Objects.nonNull(currentPlayer) && Objects.nonNull(currentPlayer.getRules()) && Objects.nonNull(rule)) {
            List<ProxyRule> rules = new ArrayList<>(currentPlayer.getRules());
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
