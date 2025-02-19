package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
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
    private static final int MIN_WIDTH = 300;  // Add minimum width constant
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
            Commands.RULE_DELETE_REQUEST
        );
        this.contextMenu = new ContextMenuHelper(
            Commands.RULE_ADD_REQUEST,
            Commands.RULE_EDIT_REQUEST,
            Commands.RULE_DELETE_REQUEST
        );
        
        setupTable();
        setupLayout();
        setupCommandBusListener();
        setupButtonListeners();
        setupContextMenu();
    }

    private void setupLayout() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(buttonPanel, BorderLayout.NORTH);
        
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
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
        String[] columnNames = {"Operator", "Comparison", "Value", "Part"};
        DefaultTableModel model = new DefaultTableModel(columnNames, 0);
        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Set very small initial column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(60); // Operator
        table.getColumnModel().getColumn(1).setPreferredWidth(60); // Comparison
        table.getColumnModel().getColumn(2).setPreferredWidth(40); // Value
        table.getColumnModel().getColumn(3).setPreferredWidth(40); // Part
        
        // Allow table to be smaller than the sum of column widths
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
    }

    private void setupButtonListeners() {
        buttonPanel.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            ProxyRule selectedRule = selectedRow >= 0 ? getSelectedRule() : null;
            
            CommandBus.getInstance().publish(
                e.getActionCommand(),
                this,
                new RuleActionData(currentPlayer, selectedRule)
            );
        });
    }

    private void setupContextMenu() {
        contextMenu.install(table);
        contextMenu.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            ProxyRule selectedRule = selectedRow >= 0 ? getSelectedRule() : null;
            
            CommandBus.getInstance().publish(
                e.getActionCommand(),
                this,
                new RuleActionData(currentPlayer, selectedRule)
            );
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
                }
            }
        });

        // Update button states on selection
        table.getSelectionModel().addListSelectionListener(e -> {
            updateButtonStates();
        });
    }

    private void updateButtonStates() {
        boolean hasPlayer = currentPlayer != null;
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
        if (row >= 0 && currentPlayer != null) {
            return new ArrayList<>(currentPlayer.getRules()).get(row);
        }
        return null;
    }

    private void clearRules() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    public void loadRules(ProxyStrike player) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        if (player != null && player.getRules() != null) {
            for (ProxyRule rule : player.getRules()) {
                model.addRow(new Object[]{
                    ProxyRule.OPERATORS[rule.getOperator()],
                    ProxyRule.COMPARISONS[rule.getComparison()],
                    rule.getValue(),
                    rule.getPart()
                });
            }
        }
    }

    // Helper class for rule actions
    public record RuleActionData(ProxyStrike player, ProxyRule rule) {}
}
