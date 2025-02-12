package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Component;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.SpinnerNumberModel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import com.angrysurfer.beatsui.Dialog;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.mock.Rule;
import com.angrysurfer.beatsui.App;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class RuleTablePanel extends JPanel {
    private final JTable table;
    private final StatusConsumer status;
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;
    private final JMenuItem editMenuItem;
    private final JMenuItem deleteMenuItem;

    public RuleTablePanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
        this.table = new JTable();
        this.addButton = new JButton("Add");
        this.editButton = new JButton("Edit");
        this.deleteButton = new JButton("Delete");
        this.editMenuItem = new JMenuItem("Edit...");
        this.deleteMenuItem = new JMenuItem("Delete");
        
        setupTable();
        setupButtons();
        setupLayout();
        setupPopupMenu();
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

        addButton.addActionListener(e -> showRuleDialog(null));
        editButton.addActionListener(e -> editSelectedRule());
        deleteButton.addActionListener(e -> deleteSelectedRule());
    }

    private void setupPopupMenu() {
        JPopupMenu popup = new JPopupMenu();
        JMenuItem addMenuItem = new JMenuItem("Add...");
        
        addMenuItem.addActionListener(e -> showRuleDialog(null));
        editMenuItem.addActionListener(e -> editSelectedRule());
        deleteMenuItem.addActionListener(e -> deleteSelectedRule());

        editMenuItem.setEnabled(false);
        deleteMenuItem.setEnabled(false);

        popup.add(addMenuItem);
        popup.add(editMenuItem);
        popup.addSeparator();
        popup.add(deleteMenuItem);

        table.setComponentPopupMenu(popup);
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

        // Replace sample rules with Redis data
        loadDataFromRedis();

        // Selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0;
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
                editMenuItem.setEnabled(hasSelection);
                deleteMenuItem.setEnabled(hasSelection);
                
                if (hasSelection) {
                    String operator = (String) table.getValueAt(table.getSelectedRow(), 0);
                    String comparison = (String) table.getValueAt(table.getSelectedRow(), 1);
                    Object value = table.getValueAt(table.getSelectedRow(), 2);
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

    private void loadDataFromRedis() {
        try {
            List<Rule> rules = App.getRedisService().findAllRules();
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            
            for (Rule rule : rules) {
                model.addRow(rule.toRow());
            }
        } catch (Exception e) {
            e.printStackTrace();
            status.setStatus("Error loading rules from Redis: " + e.getMessage());
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
        if (rule == null) {
            rule = new Rule();
        }

        RuleEditorPanel editorPanel = new RuleEditorPanel(rule);
        Dialog<Rule> dialog = new Dialog<>(rule, editorPanel);
        dialog.setTitle(rule.getOperator() == null ? "Add Rule" : "Edit Rule");

        if (dialog.showDialog()) {
            Rule updatedRule = editorPanel.getUpdatedRule();
            updateRuleTable(updatedRule, table.getSelectedRow());
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
        if (row >= 0) {
            ((DefaultTableModel) table.getModel()).removeRow(row);
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
