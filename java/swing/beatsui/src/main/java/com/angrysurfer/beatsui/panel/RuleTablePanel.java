package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beatsui.App;
import com.angrysurfer.beatsui.Dialog;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.mock.Rule;
import com.angrysurfer.beatsui.mock.Strike;
import com.angrysurfer.beatsui.api.Action;
import com.angrysurfer.beatsui.api.ActionBus;
import com.angrysurfer.beatsui.api.ActionListener;
import com.angrysurfer.beatsui.api.Commands;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RuleTablePanel extends JPanel implements ActionListener {
    private static final Logger logger = Logger.getLogger(RuleTablePanel.class.getName());
    private final JTable table;
    private final StatusConsumer status;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JMenuItem editMenuItem;
    private final JMenuItem deleteMenuItem;
    private Strike selectedPlayer;
    private JPopupMenu popup; // Add this field
    private JMenuItem addPopupMenuItem; // Add this field
    private final ActionBus actionBus = ActionBus.getInstance();

    public RuleTablePanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
        this.table = new JTable();
        this.addButton = new JButton("Add");
        this.editButton = new JButton("Edit");
        this.deleteButton = new JButton("Delete");
        this.editMenuItem = new JMenuItem("Edit...");
        this.deleteMenuItem = new JMenuItem("Delete");
        
        actionBus.register(this); // Register for events
        setupTable();
        setupButtons();
        setupLayout();
        setupPopupMenu(); // Renamed from setupContextMenu for clarity
    }

    @Override
    public void onAction(Action action) {
        if (action.getSender() == this) return; // Ignore own actions
        
        switch (action.getCommand()) {
            case Commands.PLAYER_SELECTED:
                handlePlayerSelected((Strike) action.getData());
                break;
            case Commands.PLAYER_UNSELECTED:
                handlePlayerUnselected();
                break;
        }
    }

    private void handlePlayerSelected(Strike player) {
        this.selectedPlayer = player;
        addButton.setEnabled(true);
        addPopupMenuItem.setEnabled(true);
        loadRulesFromRedis(); // Load rules for the new player
        status.setStatus("Selected player: " + player.getName());
    }

    private void handlePlayerUnselected() {
        this.selectedPlayer = null;
        clearTable();
        addButton.setEnabled(false);
        addPopupMenuItem.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        status.setStatus("No player selected");
    }

    private void clearTable() {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    private void setupLayout() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        add(buttonPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        // Set fixed width for the panel
        int panelWidth = 200;
        setPreferredSize(new Dimension(panelWidth, 0));
        setMinimumSize(new Dimension(panelWidth, 0));
        setMaximumSize(new Dimension(panelWidth, Integer.MAX_VALUE));
    }

    private void setupButtons() {
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        addButton.setEnabled(false); // Initially disabled until player is selected

        addButton.addActionListener(e -> showRuleDialog(null));
        editButton.addActionListener(e -> editSelectedRule());
        deleteButton.addActionListener(e -> deleteSelectedRule());
    }

    private void setupPopupMenu() {
        popup = new JPopupMenu();
        addPopupMenuItem = new JMenuItem("Add...");
        
        addPopupMenuItem.addActionListener(e -> showRuleDialog(null));
        editMenuItem.addActionListener(e -> editSelectedRule());
        deleteMenuItem.addActionListener(e -> deleteSelectedRule());

        popup.add(addPopupMenuItem);
        popup.add(editMenuItem);
        popup.add(deleteMenuItem);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    // Update enabled state before showing
                    boolean hasSelection = table.getSelectedRow() >= 0;
                    editMenuItem.setEnabled(hasSelection);
                    deleteMenuItem.setEnabled(hasSelection);
                    addPopupMenuItem.setEnabled(selectedPlayer != null);
                    
                    popup.show(table, e.getX(), e.getY());
                }
            }
        });
    }

    private void setupTable() {
        String[] ruleColumns = { "Operator", "Comparison", "Value", "Part" };

        DefaultTableModel model = new DefaultTableModel(ruleColumns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0:
                    case 1: return String.class;
                    case 2: return Double.class;
                    case 3: return Integer.class;
                    default: return Object.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        table.setModel(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoCreateRowSorter(true);
        table.getTableHeader().setReorderingAllowed(false);

        // Setup combo boxes and spinners
        JComboBox<String> operatorCombo = new JComboBox<>(Rule.OPERATORS);
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(operatorCombo));

        JComboBox<String> comparisonCombo = new JComboBox<>(Rule.COMPARISONS);
        table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(comparisonCombo));

        setupValueSpinner();
        setupPartSpinner();

        // Column widths
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(40);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
        table.getColumnModel().getColumn(3).setPreferredWidth(40);

        // Single selection listener that updates everything
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0;
                boolean hasPlayer = selectedPlayer != null;

                // Update button states
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
                addButton.setEnabled(hasPlayer);

                if (hasSelection) {
                    int row = table.getSelectedRow();
                    String operator = (String) table.getValueAt(row, 0);
                    String comparison = (String) table.getValueAt(row, 1);
                    Object value = table.getValueAt(row, 2);
                    status.setStatus(String.format("Selected rule: %s %s %s", operator, comparison, value));
                }
            }
        });

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedRule();
                }
            }
        });
    }

    private void loadRulesFromRedis() {
        try {
            if (selectedPlayer == null || selectedPlayer.getId() == null) {
                return; // Silent return - no need to log this
            }

            clearTable();
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            
            List<Rule> rules = App.getRedisService().findRulesByPlayer(selectedPlayer);
            logger.info("Loaded " + rules.size() + " rules for player: " + selectedPlayer.getName());
            
            rules.forEach(rule -> model.addRow(rule.toRow()));
            
        } catch (Exception e) {
            logger.severe("Error loading rules: " + e.getMessage());
            status.setStatus("Error loading rules: " + e.getMessage());
        }
    }

    private void setupValueSpinner() {
        JSpinner valueSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
        table.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
            private final JSpinner spinner = valueSpinner;

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                spinner.setValue(value == null ? 0.0 : value);
                status.setStatus("Set value at row " + row);
                return spinner;
            }

            @Override
            public Object getCellEditorValue() {
                return spinner.getValue();
            }
        });
    }

    private void setupPartSpinner() {
        JSpinner partSpinner = new JSpinner(new SpinnerNumberModel(0, 0, null, 1));
        table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JTextField()) {
            private final JSpinner spinner = partSpinner;

            @Override
            public Component getTableCellEditorComponent(JTable table, Object value,
                    boolean isSelected, int row, int column) {
                spinner.setValue(value == null ? 0 : value);
                status.setStatus("Set value at row " + row);
                return spinner;
            }

            @Override
            public Object getCellEditorValue() {
                return spinner.getValue();
            }
        });
    }

    private void showRuleDialog(Rule rule) {
        if (selectedPlayer == null) {
            status.setStatus("No player selected");
            return;
        }

        if (rule == null) {
            rule = new Rule();
        }

        RuleEditorPanel editorPanel = new RuleEditorPanel(rule);
        Dialog<Rule> dialog = new Dialog<>(rule, editorPanel);
        dialog.setTitle(rule.getId() == null ? "Add Rule" : "Edit Rule");

        if (dialog.showDialog()) {
            Rule updatedRule = editorPanel.getUpdatedRule();
            saveRuleToRedis(updatedRule);
            updateRuleTable(updatedRule, table.getSelectedRow());
        }
    }

    private void saveRuleToRedis(Rule rule) {
        try {
            if (rule.getId() == null) {
                // Let Redis service handle ID generation
                Rule savedRule = App.getRedisService().saveRule(rule, selectedPlayer);
                // Update the rule with generated ID
                rule.setId(savedRule.getId());
                status.setStatus("Created new rule for player: " + selectedPlayer.getName());
            } else {
                App.getRedisService().saveRule(rule, selectedPlayer);
                status.setStatus("Updated rule for player: " + selectedPlayer.getName());
            }
        } catch (Exception e) {
            logger.severe("Error saving rule: " + e.getMessage());
            status.setStatus("Error saving rule: " + e.getMessage());
        }
    }

    private void editSelectedRule() {
        int row = table.getSelectedRow();
        if (row >= 0) {
            Rule rule = getRuleFromRow(row);
            showRuleDialog(rule);
        }
    }

    private void deleteSelectedRule() {
        int row = table.getSelectedRow();
        if (row >= 0 && selectedPlayer != null) {
            Rule rule = getRuleFromRow(row);
            try {
                App.getRedisService().deleteRule(rule, selectedPlayer);
                ((DefaultTableModel) table.getModel()).removeRow(row);
                status.setStatus("Deleted rule from player: " + selectedPlayer.getName());
            } catch (Exception e) {
                logger.severe("Error deleting rule: " + e.getMessage());
                status.setStatus("Error deleting rule: " + e.getMessage());
            }
        }
    }

    private Rule getRuleFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        return Rule.fromRow(new Object[] {
            model.getValueAt(row, 0),
            model.getValueAt(row, 1),
            model.getValueAt(row, 2),
            model.getValueAt(row, 3)
        });
    }

    private void updateRuleTable(Rule rule, int selectedRow) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        Object[] rowData = rule.toRow();

        if (selectedRow >= 0) {
            for (int i = 0; i < rowData.length; i++) {
                model.setValueAt(rowData[i], selectedRow, i);
            }
        } else {
            model.addRow(rowData);
        }
    }
}
