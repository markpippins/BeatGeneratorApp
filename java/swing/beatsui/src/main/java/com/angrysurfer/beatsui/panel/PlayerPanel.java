package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
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
import com.angrysurfer.beatsui.widget.IStatus;

public class PlayerPanel extends JPanel {

    private JTable leftTable;
    private JTable rightTable;
    private IStatus status;

    public PlayerPanel(IStatus status) {
        super(new BorderLayout());
        this.status = status;
        setup();
        setupTables();
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createBeatsPanel(), BorderLayout.CENTER);
    }

    private static class Player {
        String name;
        int channel;
        long swing;
        long level;
        long note;
        long minVelocity;
        long maxVelocity;
        long preset;
        boolean stickyPreset;
        long probability;
        long randomDegree;
        long ratchetCount;
        long ratchetInterval;
        boolean useInternalBeats;
        boolean useInternalBars;
        long panPosition;
        boolean preserveOnPurge;
        double sparse;

        public Player(String name, int channel, long note) {
            this.name = name;
            this.channel = Math.min(12, Math.max(1, channel)); // Constrain to 1-12
            this.note = Math.min(127, Math.max(1, note)); // Constrain to 1-127
            this.level = 100L;
            this.minVelocity = 100L;
            this.maxVelocity = 110L;
            this.preset = 1L;
            this.probability = 100L;
            this.panPosition = 63L; // Center pan
            this.swing = 0L;
            this.randomDegree = 0L;
            this.ratchetCount = 0L;
            this.ratchetInterval = 1L;
            this.useInternalBeats = false;
            this.useInternalBars = false;
            this.preserveOnPurge = false;
            this.sparse = 0.0;
        }

        public Object[] toRow() {
            return new Object[] {
                    name, channel, swing, level, note, minVelocity, maxVelocity,
                    preset, stickyPreset, probability, randomDegree, ratchetCount,
                    ratchetInterval, useInternalBeats, useInternalBars, panPosition,
                    preserveOnPurge, sparse
            };
        }
    }

    // Temporary Rule class for UI development
    private static class RuleData {
        public static final String[] OPERATORS = {
                "Tick", "Beat", "Bar", "Part",
                "Ticks", "Beats", "Bars", "Parts"
        };

        public static final String[] COMPARISONS = {
                "=", "!=", "<", ">", "%"
        };

        Integer operator;
        Integer comparison;
        Double value;
        Integer part;

        public RuleData() {
            this.operator = 0;
            this.comparison = 0;
            this.value = 0.0;
            this.part = 0;
        }

        public Object[] toRow() {
            return new Object[] {
                    OPERATORS[operator],
                    COMPARISONS[comparison],
                    value,
                    part
            };
        }
    }

    private JPanel createBeatsPanel() {
        // Main panel using BorderLayout
        JPanel beatsPanel = new JPanel(new BorderLayout());

        // Create vertical split pane for main layout
        // JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        // mainSplitPane.setResizeWeight(0.75);

        JPanel mainPane = new JPanel(new BorderLayout());

        // Top section with horizontal split for tables
        JSplitPane tablesSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        tablesSplitPane.setResizeWeight(0.75); // Left table gets 75% of space
        tablesSplitPane.setDividerLocation(0.55);

        // Left table section
        JPanel leftPanel = new JPanel(new BorderLayout());
        JPanel leftButtonPanel = createButtonPanel();
        leftTable = new JTable();
        JScrollPane leftScrollPane = new JScrollPane(leftTable);
        leftPanel.add(leftButtonPanel, BorderLayout.NORTH);
        leftPanel.add(leftScrollPane, BorderLayout.CENTER);

        // Right table section
        JPanel rightPanel = new JPanel(new BorderLayout());
        JPanel rightButtonPanel = createButtonPanel();
        rightTable = new JTable();
        JScrollPane rightScrollPane = new JScrollPane(rightTable);
        rightPanel.add(rightButtonPanel, BorderLayout.NORTH);
        rightPanel.add(rightScrollPane, BorderLayout.CENTER);
        rightPanel.setMaximumSize(new Dimension(300, 600));

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

        // Dimension size = new Dimension(BUTTON_SIZE * GRID_COLS, BUTTON_SIZE *
        // GRID_ROWS);

        // buttonGridPanel.setMaximumSize(size);
        // buttonGridPanel.setMinimumSize(size);
        // buttonGridPanel.setPreferredSize(size);

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

    // static int BUTTON_SIZE = 25;
    // static int GRID_ROWS = 5;
    // static int GRID_COLS = 36;

    // private JPanel createButtonGridPanel() {
    // JPanel panel = new JPanel(new GridLayout(5, 33, 2, 2));
    // panel.setBorder(BorderFactory.createEmptyBorder());

    // // Create 5x33 grid of buttons with varying colors
    // Color[] colors = {
    // new Color(255, 200, 200), // Light red
    // new Color(200, 255, 200), // Light green
    // new Color(200, 200, 255), // Light blue
    // new Color(255, 255, 200), // Light yellow
    // new Color(255, 200, 255), // Light purple
    // new Color(200, 255, 255) // Light cyan
    // };

    // for (int row = 0; row < GRID_ROWS; row++) {
    // for (int col = 0; col < GRID_COLS; col++) {
    // JButton button = new JButton();
    // button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE)); // Make
    // buttons square

    // // Vary colors based on position
    // int colorIndex = (row * col) % colors.length;
    // button.setBackground(colors[colorIndex]);
    // button.setOpaque(true);
    // button.setBorderPainted(true);

    // // Optional: Add tooltip showing position
    // button.setToolTipText(String.format("Row: %d, Col: %d", row + 1, col + 1));

    // panel.add(button);
    // }
    // }

    // return panel;
    // }

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
        Player[] samplePlayers = {
                new Player("Kick", 1, 36),
                new Player("Snare", 2, 38),
                new Player("HiHat", 3, 42),
                new Player("Crash", 4, 49),
                new Player("Tom1", 5, 45),
                new Player("Tom2", 6, 47),
                new Player("Tom3", 7, 48),
                new Player("Ride", 8, 51)
        };

        for (Player player : samplePlayers) {
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
        JComboBox<String> operatorCombo = new JComboBox<>(RuleData.OPERATORS);
        rightTable.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(operatorCombo));

        // Setup combo box for Comparison column
        JComboBox<String> comparisonCombo = new JComboBox<>(RuleData.COMPARISONS);
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

        // Set column widths
        rightTable.getColumnModel().getColumn(0).setPreferredWidth(40); // Operator
        rightTable.getColumnModel().getColumn(1).setPreferredWidth(20); // Comparison
        rightTable.getColumnModel().getColumn(2).setPreferredWidth(40); // Value
        rightTable.getColumnModel().getColumn(3).setPreferredWidth(20); // Part

        // Apply center alignment to all columns except first in right table
        for (int i = 1; i < rightTable.getColumnCount(); i++) {
            rightTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Add some sample data
        RuleData[] sampleRules = {
                new RuleData(), // Add default rule
                new RuleData(), // Add default rule
                new RuleData(), // Add default rule
        };

        for (RuleData rule : sampleRules) {
            rightModel.addRow(rule.toRow());
        }
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
