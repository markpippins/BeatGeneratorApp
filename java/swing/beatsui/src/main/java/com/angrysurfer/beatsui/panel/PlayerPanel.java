package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beatsui.Dialog;
import com.angrysurfer.beatsui.Utils;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.mock.Rule;
import com.angrysurfer.beatsui.mock.Strike;

public class PlayerPanel extends StatusProviderPanel {
    private JTable leftTable;
    private JTable rightTable;
    private StatusConsumer status;

    // Add buttons as fields
    private JButton addPlayerButton;
    private JButton editPlayerButton;
    private JButton deletePlayerButton;
    private JButton addRuleButton;
    private JButton editRuleButton;
    private JButton deleteRuleButton;

    // Add menu items as fields
    private JMenuItem editPlayerMenuItem;
    private JMenuItem deletePlayerMenuItem;
    private JMenuItem editRuleMenuItem;
    private JMenuItem deleteRuleMenuItem;

    public PlayerPanel(StatusConsumer status) {
        super(new BorderLayout(), status);
        this.status = status;
        setup();
        setupTables();
    }

    public PlayerPanel() {
        this(null);
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createBeatsPanel(), BorderLayout.CENTER);
    }

    private JPanel createBeatsPanel() {
        JPanel beatsPanel = new JPanel(new BorderLayout());
        JPanel mainPane = new JPanel(new BorderLayout());

        // Create and configure split pane
        JSplitPane tablesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        tablesSplitPane.setResizeWeight(1.0); // Give all extra space to left component

        // Left panel setup with player buttons
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel leftButtonPanel = createPlayerButtonPanel();
        leftTable = new JTable();
        JScrollPane leftScrollPane = new JScrollPane(leftTable);
        leftPanel.add(leftButtonPanel, BorderLayout.NORTH);
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);

        // Right panel setup with rule buttons
        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel rightButtonPanel = createRuleButtonPanel();
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

        JPanel buttonGridPanel = new GridPanel(this.status);
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

    private JPanel createPlayerButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        addPlayerButton = new JButton("Add");
        editPlayerButton = new JButton("Edit");
        deletePlayerButton = new JButton("Delete");

        editPlayerButton.setEnabled(false);
        deletePlayerButton.setEnabled(false);

        addPlayerButton.addActionListener(e -> showPlayerDialog(null));
        editPlayerButton.addActionListener(e -> {
            int row = leftTable.getSelectedRow();
            if (row >= 0) {
                Strike player = getPlayerFromRow(row);
                showPlayerDialog(player);
            }
        });
        deletePlayerButton.addActionListener(e -> {
            int row = leftTable.getSelectedRow();
            if (row >= 0) {
                ((DefaultTableModel) leftTable.getModel()).removeRow(row);
            }
        });

        buttonPanel.add(addPlayerButton);
        buttonPanel.add(editPlayerButton);
        buttonPanel.add(deletePlayerButton);

        return buttonPanel;
    }

    private JPanel createRuleButtonPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        addRuleButton = new JButton("Add");
        editRuleButton = new JButton("Edit");
        deleteRuleButton = new JButton("Delete");

        editRuleButton.setEnabled(false);
        deleteRuleButton.setEnabled(false);

        addRuleButton.addActionListener(e -> showRuleDialog(null));
        editRuleButton.addActionListener(e -> {
            int row = rightTable.getSelectedRow();
            if (row >= 0) {
                Rule rule = getRuleFromRow(row);
                showRuleDialog(rule);
            }
        });
        deleteRuleButton.addActionListener(e -> {
            int row = rightTable.getSelectedRow();
            if (row >= 0) {
                ((DefaultTableModel) rightTable.getModel()).removeRow(row);
            }
        });

        buttonPanel.add(addRuleButton);
        buttonPanel.add(editRuleButton);
        buttonPanel.add(deleteRuleButton);

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
        Strike[] samplePlayers = {
                new Strike("Kick", null, null, Strike.KICK, Strike.kickParams),
                new Strike("Snare", null, null, Strike.SNARE, Strike.snarePrams),
                new Strike("Closed Hat", null, null, Strike.CLOSED_HAT, Strike.closedHatParams),
                new Strike("Open Hat", null, null, Strike.OPEN_HAT, Strike.razParams)
        };

        for (Strike player : samplePlayers) {
            player.setChannel(1);
            player.setLevel((long) 100);
            player.setMinVelocity((long) 64);
            player.setMaxVelocity((long) 127);
            player.setProbability(100L);
            player.setRandomDegree(0L);
            player.setRatchetCount(0L);
            player.setRatchetInterval(1L);
            player.setPanPosition(64L);
            player.setSparse(0.0);
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
        JComboBox<String> operatorCombo = new JComboBox<>(Rule.OPERATORS);
        rightTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(operatorCombo));

        // Setup combo box for Comparison column
        JComboBox<String> comparisonCombo = new JComboBox<>(Rule.COMPARISONS);
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

        // Add some sample data for rules
        Rule[] sampleRules = {
                new Rule(0, 0, 1.0, 0), // Tick equals 1
                new Rule(2, 1, 3.0, 0), // Bar not equals 3
                new Rule(1, 4, 2.0, 0) // Beat modulo 2
        };

        for (Rule rule : sampleRules) {
            rightModel.addRow(rule.toRow());
        }

        // Add selection listeners to tables
        leftTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = leftTable.getSelectedRow() >= 0;
                editPlayerButton.setEnabled(hasSelection);
                deletePlayerButton.setEnabled(hasSelection);
                editPlayerMenuItem.setEnabled(hasSelection);
                deletePlayerMenuItem.setEnabled(hasSelection);
                
                // Add status update for player selection
                if (hasSelection) {
                    String playerName = (String) leftTable.getValueAt(leftTable.getSelectedRow(), 0);
                    setStatus("Selected player: " + playerName);
                }
            }
        });

        rightTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = rightTable.getSelectedRow() >= 0;
                editRuleButton.setEnabled(hasSelection);
                deleteRuleButton.setEnabled(hasSelection);
                editRuleMenuItem.setEnabled(hasSelection);
                deleteRuleMenuItem.setEnabled(hasSelection);
                
                // Add status update for rule selection
                if (hasSelection) {
                    String operator = (String) rightTable.getValueAt(rightTable.getSelectedRow(), 0);
                    String comparison = (String) rightTable.getValueAt(rightTable.getSelectedRow(), 1);
                    Object value = rightTable.getValueAt(rightTable.getSelectedRow(), 2);
                    setStatus(String.format("Selected rule: %s %s %s", operator, comparison, value));
                }
            }
        });

        // Create popup menus
        JPopupMenu playerPopup = new JPopupMenu();
        JMenuItem addPlayerMenuItem = new JMenuItem("Add...");
        editPlayerMenuItem = new JMenuItem("Edit...");
        deletePlayerMenuItem = new JMenuItem("Delete");

        addPlayerMenuItem.addActionListener(e -> showPlayerDialog(null));
        editPlayerMenuItem.addActionListener(e -> {
            int row = leftTable.getSelectedRow();
            if (row >= 0) {
                Strike player = getPlayerFromRow(row);
                showPlayerDialog(player);
            }
        });
        deletePlayerMenuItem.addActionListener(e -> {
            int row = leftTable.getSelectedRow();
            if (row >= 0) {
                ((DefaultTableModel) leftTable.getModel()).removeRow(row);
            }
        });

        // Set initial menu item states
        editPlayerMenuItem.setEnabled(false);
        deletePlayerMenuItem.setEnabled(false);

        playerPopup.add(addPlayerMenuItem);
        playerPopup.add(editPlayerMenuItem);
        playerPopup.addSeparator();
        playerPopup.add(deletePlayerMenuItem);

        leftTable.setComponentPopupMenu(playerPopup);

        // Add popup menu to right table
        JPopupMenu rulePopup = new JPopupMenu();
        JMenuItem addRuleMenuItem = new JMenuItem("Add...");
        editRuleMenuItem = new JMenuItem("Edit...");
        deleteRuleMenuItem = new JMenuItem("Delete");

        addRuleMenuItem.addActionListener(e -> showRuleDialog(null));
        editRuleMenuItem.addActionListener(e -> {
            int row = rightTable.getSelectedRow();
            if (row >= 0) {
                Rule rule = getRuleFromRow(row);
                showRuleDialog(rule);
            }
        });
        deleteRuleMenuItem.addActionListener(e -> {
            int row = rightTable.getSelectedRow();
            if (row >= 0) {
                ((DefaultTableModel) rightTable.getModel()).removeRow(row);
            }
        });

        // Set initial menu item states
        editRuleMenuItem.setEnabled(false);
        deleteRuleMenuItem.setEnabled(false);

        rulePopup.add(addRuleMenuItem);
        rulePopup.add(editRuleMenuItem);
        rulePopup.addSeparator();
        rulePopup.add(deleteRuleMenuItem);

        rightTable.setComponentPopupMenu(rulePopup);

        // Add double click listener to left table
        leftTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = leftTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        Strike player = getPlayerFromRow(row);
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
                        Rule rule = getRuleFromRow(row);
                        showRuleDialog(rule);
                    }
                }
            }
        });
    }

    private void showPlayerDialog(Strike player) {
        if (player == null) {
            player = new Strike();
            player.setName("New Strike");
        }

        PlayerEditorPanel editorPanel = new PlayerEditorPanel(player);
        Dialog<Strike> dialog = new Dialog<>(player, editorPanel);
        dialog.setTitle(player.getName() == null ? "Add Player" : "Edit Player: " + player.getName());

        if (dialog.showDialog()) {
            Strike updatedPlayer = editorPanel.getUpdatedPlayer();
            updatePlayerTable(updatedPlayer, leftTable.getSelectedRow());
        }
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
            updateRuleTable(updatedRule, rightTable.getSelectedRow());
        }
    }

    private Strike getPlayerFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) leftTable.getModel();

        // Create new player with basic info
        Strike player = new Strike();
        player.setName((String) model.getValueAt(row, 0));
        player.setChannel(((Number) model.getValueAt(row, 1)).intValue());
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

    private Rule getRuleFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) rightTable.getModel();
        Object[] rowData = new Object[] {
                model.getValueAt(row, 0),
                model.getValueAt(row, 1),
                model.getValueAt(row, 2),
                model.getValueAt(row, 3)
        };
        return Rule.fromRow(rowData);
    }

    private void updatePlayerTable(Strike player, int selectedRow) {
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

    private void updateRuleTable(Rule rule, int selectedRow) {
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
        for (int i = 0; i < Rule.OPERATORS.length; i++) {
            if (Rule.OPERATORS[i].equals(operator))
                return i;
        }
        return 0;
    }

    private int getComparisonIndex(String comparison) {
        for (int i = 0; i < Rule.COMPARISONS.length; i++) {
            if (Rule.COMPARISONS[i].equals(comparison))
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
