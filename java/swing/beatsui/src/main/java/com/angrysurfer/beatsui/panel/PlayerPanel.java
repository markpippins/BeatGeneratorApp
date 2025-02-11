package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Objects;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beatsui.Utils;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.Dialog;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PlayerPanel extends JPanel {

    private JTable leftTable;
    private JTable rightTable;
    private StatusConsumer status;

    public PlayerPanel(StatusConsumer status) {
        super(new BorderLayout());
        this.status = status;
        setup();
        setupTables();
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createBeatsPanel(), BorderLayout.CENTER);
    }

    private void setStatus(String status) {
        if (Objects.nonNull(this.status))
            this.status.setStatus(status);
    }

    private JPanel createBeatsPanel() {
        JPanel beatsPanel = new JPanel(new BorderLayout());
        JPanel mainPane = new JPanel(new BorderLayout());

        // Create and configure split pane
        JSplitPane tablesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        tablesSplitPane.setResizeWeight(1.0); // Give all extra space to left component

        // Left panel setup
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel leftButtonPanel = createButtonPanel();
        leftTable = new JTable();
        JScrollPane leftScrollPane = new JScrollPane(leftTable);
        leftPanel.add(leftButtonPanel, BorderLayout.NORTH);
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);

        // Right panel setup with fixed width
        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel rightButtonPanel = createButtonPanel();
        rightTable = new JTable();
        JScrollPane rightScrollPane = new JScrollPane(rightTable);
        rightPanel.add(rightButtonPanel, BorderLayout.NORTH);
        rightPanel.add(rightScrollPane, BorderLayout.CENTER);

        // Set fixed width for right panel
        int rightPanelWidth = 200; // Adjust this value as needed
        rightPanel.setPreferredSize(new Dimension(rightPanelWidth, 0));
        rightPanel.setMinimumSize(new Dimension(rightPanelWidth, 0));
        rightPanel.setMaximumSize(new Dimension(rightPanelWidth, Integer.MAX_VALUE));

        tablesSplitPane.setLeftComponent(leftPanel);
        tablesSplitPane.setRightComponent(rightPanel);

        // Bottom section with button grid

        JPanel buttonGridPanel = new GridPanel();
        JScrollPane buttonScrollPane = new JScrollPane(buttonGridPanel);

        // Add components to main split pane
        mainPane.add(tablesSplitPane, BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new PianoPanel(), BorderLayout.NORTH);
        panel.add(buttonScrollPane, BorderLayout.SOUTH);

        mainPane.add(panel, BorderLayout.SOUTH);

        beatsPanel.add(mainPane, BorderLayout.CENTER);

        return beatsPanel;
    }

    private JPanel createButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(new JButton("Add"));
        buttonPanel.add(new JButton("Remove"));
        buttonPanel.add(new JButton("Clear"));
        return buttonPanel;
    }

    private void setupTables() {
        // Define column names
        String[] columnNames = {
                "Name", "Channel", "Swing", "Level", "Note", "Min Vel", "Max Vel",
                "Preset", "Sticky", "Prob", "Random", "Ratchet #", "Ratchet Int",
                "Int Beats", "Int Bars", "Pan", "Preserve", "Sparse"
        };

        // Create table model with column names
        DefaultTableModel leftModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0:
                        return String.class; // Name
                    case 8:
                    case 13:
                    case 14:
                    case 16:
                        return Boolean.class; // Sticky, IntBeats, IntBars, Preserve
                    case 17:
                        return Double.class; // Sparse
                    default:
                        return Long.class; // All other numeric fields
                }
            }
        };

        // Add sample data
        MockPlayer[] samplePlayers = {
                new MockPlayer("Kick", 1, 36),
                new MockPlayer("Snare", 2, 38),
                new MockPlayer("HiHat", 3, 42),
                new MockPlayer("Crash", 4, 49),
                new MockPlayer("Tom1", 5, 45),
                new MockPlayer("Tom2", 6, 47),
                new MockPlayer("Tom3", 7, 48),
                new MockPlayer("Ride", 8, 51)
        };

        for (MockPlayer player : samplePlayers) {
            leftModel.addRow(player.toRow());
        }

        // Set up left table
        leftTable.setModel(leftModel);
        leftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftTable.setAutoCreateRowSorter(true);
        leftTable.getTableHeader().setReorderingAllowed(false);

        // Adjust column widths
        leftTable.getColumnModel().getColumn(0).setPreferredWidth(100); // Name
        for (int i = 1; i < leftTable.getColumnCount(); i++) {
            leftTable.getColumnModel().getColumn(i).setPreferredWidth(120);
        }

        // Set up custom editors for specific columns
        Utils.setupColumnEditor(leftTable, "Channel", 1, 16);
        Utils.setupColumnEditor(leftTable, "Swing", 0, 100);
        Utils.setupColumnEditor(leftTable, "Level", 1, 127);
        Utils.setupColumnEditor(leftTable, "Note", 1, 127);
        Utils.setupColumnEditor(leftTable, "Min Vel", 1, 127);
        Utils.setupColumnEditor(leftTable, "Max Vel", 1, 127);
        Utils.setupColumnEditor(leftTable, "Preset", 1, 127);
        Utils.setupColumnEditor(leftTable, "Prob", 0, 100);
        Utils.setupColumnEditor(leftTable, "Random", 0, 100);
        Utils.setupColumnEditor(leftTable, "Ratchet #", 0, 6);
        Utils.setupColumnEditor(leftTable, "Pan", 1, 127);

        // Create center-aligned renderer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Apply center alignment to all columns except first and boolean columns in
        // left table
        for (int i = 1; i < leftTable.getColumnCount(); i++) {
            String columnName = leftTable.getColumnName(i);
            if (!isBooleanColumn(columnName)) {
                leftTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
            }
        }

        // Setup right (Rules) table
        String[] ruleColumns = { "Operator", "Comparison", "Value", "Part" };

        DefaultTableModel rightModel = new DefaultTableModel(ruleColumns, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 0:
                    case 1:
                        return String.class;
                    case 2:
                        return Double.class;
                    case 3:
                        return Integer.class;
                    default:
                        return Object.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };

        // Set up right table
        rightTable.setModel(rightModel);
        rightTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        rightTable.setAutoCreateRowSorter(true);
        rightTable.getTableHeader().setReorderingAllowed(false);

        // Setup combo box for Operator column
        JComboBox<String> operatorCombo = new JComboBox<>(MockRule.OPERATORS);
        rightTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(operatorCombo));

        // Setup combo box for Comparison column
        JComboBox<String> comparisonCombo = new JComboBox<>(MockRule.COMPARISONS);
        rightTable.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(comparisonCombo));

        // Setup spinner for Value column
        JSpinner valueSpinner = new JSpinner(new SpinnerNumberModel(0.0, 0.0, null, 1.0));
        rightTable.getColumnModel().getColumn(2).setCellEditor(new DefaultCellEditor(new JTextField()) {
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

        // Setup spinner for Part column
        JSpinner partSpinner = new JSpinner(new SpinnerNumberModel(0, 0, null, 1));
        rightTable.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(new JTextField()) {
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

        // Adjust right table column widths to minimum
        rightTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        rightTable.getColumnModel().getColumn(0).setPreferredWidth(60); // Operator
        rightTable.getColumnModel().getColumn(1).setPreferredWidth(40); // Comparison
        rightTable.getColumnModel().getColumn(2).setPreferredWidth(50); // Value
        rightTable.getColumnModel().getColumn(3).setPreferredWidth(40); // Part

        // Force the table to be compact
        int totalWidth = 0;
        for (int i = 0; i < rightTable.getColumnCount(); i++) {
            totalWidth += rightTable.getColumnModel().getColumn(i).getPreferredWidth();
        }

        // Add small buffer for borders
        totalWidth += 10;
        rightTable.setPreferredScrollableViewportSize(new Dimension(totalWidth, 0));

        // Apply center alignment to all columns except first in right table
        for (int i = 1; i < rightTable.getColumnCount(); i++) {
            rightTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Add some sample data
        MockRule[] sampleRules = {
                new MockRule(), // Add default rule
                new MockRule(), // Add default rule
                new MockRule(), // Add default rule
        };

        for (MockRule rule : sampleRules) {
            rightModel.addRow(rule.toRow());
        }

        // Add popup menu to left table
        JPopupMenu playerPopup = new JPopupMenu();
        JMenuItem addPlayer = new JMenuItem("Add...");
        JMenuItem editPlayer = new JMenuItem("Edit...");
        JMenuItem deletePlayer = new JMenuItem("Delete");

        addPlayer.addActionListener(e -> showPlayerDialog(null));
        editPlayer.addActionListener(e -> {
            int row = leftTable.getSelectedRow();
            if (row >= 0) {
                MockPlayer player = getPlayerFromRow(row);
                showPlayerDialog(player);
            }
        });
        deletePlayer.addActionListener(e -> {
            int row = leftTable.getSelectedRow();
            if (row >= 0) {
                ((DefaultTableModel) leftTable.getModel()).removeRow(row);
            }
        });

        playerPopup.add(addPlayer);
        playerPopup.add(editPlayer);
        playerPopup.addSeparator();
        playerPopup.add(deletePlayer);

        leftTable.setComponentPopupMenu(playerPopup);

        // Add popup menu to right table
        JPopupMenu rulePopup = new JPopupMenu();
        JMenuItem addRule = new JMenuItem("Add...");
        JMenuItem editRule = new JMenuItem("Edit...");
        JMenuItem deleteRule = new JMenuItem("Delete");

        addRule.addActionListener(e -> showRuleDialog(null));
        editRule.addActionListener(e -> {
            int row = rightTable.getSelectedRow();
            if (row >= 0) {
                MockRule rule = getRuleFromRow(row);
                showRuleDialog(rule);
            }
        });
        deleteRule.addActionListener(e -> {
            int row = rightTable.getSelectedRow();
            if (row >= 0) {
                ((DefaultTableModel) rightTable.getModel()).removeRow(row);
            }
        });

        rulePopup.add(addRule);
        rulePopup.add(editRule);
        rulePopup.addSeparator();
        rulePopup.add(deleteRule);

        rightTable.setComponentPopupMenu(rulePopup);

        // Add double click listener to left table
        leftTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = leftTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        MockPlayer player = getPlayerFromRow(row);
                        showPlayerDialog(player);
                    }
                }
            }
        });

        // Add double click listener to right table
        rightTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = rightTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        MockRule rule = getRuleFromRow(row);
                        showRuleDialog(rule);
                    }
                }
            }
        });
    }

    private void showPlayerDialog(MockPlayer player) {
        if (player == null) {
            player = new MockPlayer("New Player", 1, 36);
        }

        PlayerEditorPanel editorPanel = new PlayerEditorPanel(player);
        Dialog<MockPlayer> dialog = new Dialog<>(player, editorPanel);
        dialog.setTitle(player.getName() == null ? "Add Player" : "Edit Player: " + player.getName());

        if (dialog.showDialog()) {
            MockPlayer updatedPlayer = editorPanel.getUpdatedPlayer();
            updatePlayerTable(updatedPlayer, leftTable.getSelectedRow());
        }
    }

    private void showRuleDialog(MockRule rule) {
        if (rule == null) {
            rule = new MockRule();
        }

        RuleEditorPanel editorPanel = new RuleEditorPanel(rule);
        Dialog<MockRule> dialog = new Dialog<>(rule, editorPanel);
        dialog.setTitle(rule.getOperator() == null ? "Add Rule" : "Edit Rule");

        if (dialog.showDialog()) {
            MockRule updatedRule = editorPanel.getUpdatedRule();
            updateRuleTable(updatedRule, rightTable.getSelectedRow());
        }
    }

    private MockPlayer getPlayerFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) leftTable.getModel();

        // Create new player with basic info
        MockPlayer player = new MockPlayer(
                (String) model.getValueAt(row, 0),
                ((Number) model.getValueAt(row, 1)).intValue(), // channel
                ((Number) model.getValueAt(row, 4)).longValue() // note
        );

        // Set all other values using the model data
        player.setSwing(((Number) model.getValueAt(row, 2)).longValue());
        player.setLevel(((Number) model.getValueAt(row, 3)).longValue());
        player.setMinVelocity(((Number) model.getValueAt(row, 5)).longValue());
        player.setMaxVelocity(((Number) model.getValueAt(row, 6)).longValue());
        player.setPreset(((Number) model.getValueAt(row, 7)).longValue());
        player.setStickyPreset((Boolean) model.getValueAt(row, 8));
        player.setProbability(((Number) model.getValueAt(row, 9)).longValue());
        player.setRandomDegree(((Number) model.getValueAt(row, 10)).longValue());
        player.setRatchetCount(((Number) model.getValueAt(row, 11)).longValue());
        player.setRatchetInterval(((Number) model.getValueAt(row, 12)).longValue());
        player.setUseInternalBeats((Boolean) model.getValueAt(row, 13));
        player.setUseInternalBars((Boolean) model.getValueAt(row, 14));
        player.setPanPosition(((Number) model.getValueAt(row, 15)).longValue());
        player.setPreserveOnPurge((Boolean) model.getValueAt(row, 16));
        player.setSparse((Double) model.getValueAt(row, 17));

        return player;
    }

    private MockRule getRuleFromRow(int row) {
        MockRule rule = new MockRule();
        DefaultTableModel model = (DefaultTableModel) rightTable.getModel();
        rule.setOperator(getOperatorIndex((String) model.getValueAt(row, 0)));
        rule.setComparison(getComparisonIndex((String) model.getValueAt(row, 1)));
        rule.setValue((Double) model.getValueAt(row, 2));
        rule.setPart((Integer) model.getValueAt(row, 3));
        return rule;
    }

    private void updatePlayerTable(MockPlayer player, int selectedRow) {
        DefaultTableModel model = (DefaultTableModel) leftTable.getModel();
        Object[] rowData = player.toRow();

        if (selectedRow >= 0) {
            // Update existing row
            for (int i = 0; i < rowData.length; i++) {
                model.setValueAt(rowData[i], selectedRow, i);
            }
        } else {
            // Add new row
            model.addRow(rowData);
        }
    }

    private void updateRuleTable(MockRule rule, int selectedRow) {
        DefaultTableModel model = (DefaultTableModel) rightTable.getModel();
        Object[] rowData = rule.toRow();

        if (selectedRow >= 0) {
            // Update existing row
            for (int i = 0; i < rowData.length; i++) {
                model.setValueAt(rowData[i], selectedRow, i);
            }
        } else {
            // Add new row
            model.addRow(rowData);
        }
    }

    private int getOperatorIndex(String operator) {
        for (int i = 0; i < MockRule.OPERATORS.length; i++) {
            if (MockRule.OPERATORS[i].equals(operator))
                return i;
        }
        return 0;
    }

    private int getComparisonIndex(String comparison) {
        for (int i = 0; i < MockRule.COMPARISONS.length; i++) {
            if (MockRule.COMPARISONS[i].equals(comparison))
                return i;
        }
        return 0;
    }

    // Update the isValidValue method to include Level and Preset
    private boolean isValidValue(String columnName, long value) {
        switch (columnName) {
            case "Channel":
                return value >= 1 && value <= 12;
            case "Swing":
            case "Prob":
            case "Random":
                return value >= 0 && value <= 100;
            case "Level":
            case "Note":
            case "Min Vel":
            case "Max Vel":
            case "Preset":
            case "Pan":
                return value >= 1 && value <= 127;
            case "Ratchet #":
                return value >= 0 && value <= 6;
            default:
                return true;
        }
    }

    private boolean isBooleanColumn(String columnName) {
        return columnName.equals("Sticky") ||
                columnName.equals("Int Beats") ||
                columnName.equals("Int Bars") ||
                columnName.equals("Preserve");
    }

}
