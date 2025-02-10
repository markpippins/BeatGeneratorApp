package com.angrysurfer.beatsui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.stream.IntStream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.Timer;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import com.angrysurfer.beatsui.widgets.LaunchPanel;
import com.angrysurfer.core.model.Rule;

public class AppFrame extends JFrame {
    private JLabel statusBar;
    private JTable leftTable;
    private JTable rightTable;

    // Temporary Player class for UI development
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

    public AppFrame() {
        super("Beats");
        setupFrame();
        setupMenuBar();
        setupToolBar();
        setupMainContent();
        setupStatusBar();
        setupTables();
    }

    private void setupFrame() {
        setMinimumSize(new Dimension(1200, 800));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        fileMenu.add(new JMenuItem("New"));
        fileMenu.add(new JMenuItem("Open"));
        fileMenu.add(new JMenuItem("Save"));
        fileMenu.add(new JMenuItem("Save As..."));
        fileMenu.addSeparator();
        fileMenu.add(new JMenuItem("Exit"));

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        editMenu.add(new JMenuItem("Undo"));
        editMenu.add(new JMenuItem("Redo"));
        editMenu.addSeparator();
        editMenu.add(new JMenuItem("Cut"));
        editMenu.add(new JMenuItem("Copy"));
        editMenu.add(new JMenuItem("Paste"));

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(new JMenuItem("About"));

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void setupToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Left status fields
        JPanel leftStatusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        String[] leftLabels = { "Tick", "Beat", "Bar", "Part", "Players", "Ticks", "Beats", "Bars" };
        for (String label : leftLabels) {
            // Create panel for vertical stacking
            JPanel fieldPanel = new JPanel();
            fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

            // Create and add label
            JLabel nameLabel = new JLabel(label);
            nameLabel.setForeground(Color.GRAY);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(nameLabel);

            // Create and add text field
            JTextField field = new JTextField("0");
            field.setColumns(4);
            field.setEditable(false);
            field.setEnabled(false);
            field.setHorizontalAlignment(JTextField.CENTER);
            field.setBackground(new Color(240, 240, 240));
            field.setToolTipText("Current " + label.toLowerCase() + " value");
            field.setMaximumSize(new Dimension(50, 25));
            field.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(field);

            leftStatusPanel.add(fieldPanel);
            leftStatusPanel.add(Box.createHorizontalStrut(5));
        }
        toolBar.add(leftStatusPanel);

        // Center glue
        toolBar.add(Box.createHorizontalGlue());

        // Transport controls with correct Unicode characters
        JButton rewindBtn = createToolbarButton("⏮", "Rewind");
        JButton pauseBtn = createToolbarButton("⏸", "Pause");
        JButton recordBtn = createToolbarButton("⏺", "Record");
        JButton stopBtn = createToolbarButton("⏹", "Stop");
        JButton playBtn = createToolbarButton("▶", "Play");
        JButton forwardBtn = createToolbarButton("⏭", "Forward");

        toolBar.add(rewindBtn);
        toolBar.add(pauseBtn);
        toolBar.add(stopBtn);
        toolBar.add(recordBtn);
        toolBar.add(playBtn);
        toolBar.add(forwardBtn);

        toolBar.addSeparator();

        // Use arrow icons instead of plus/minus
        JButton upButton = createToolbarButton("↓", "Down");
        JButton downButton = createToolbarButton("↑", "Up");

        // Or alternatively, if you prefer different arrows:
        // JButton upButton = new
        // JButton(UIManager.getIcon("ScrollBar.northButtonIcon"));
        // JButton downButton = new
        // JButton(UIManager.getIcon("ScrollBar.southButtonIcon"));

        upButton.setFocusPainted(false);
        downButton.setFocusPainted(false);

        toolBar.add(upButton);
        toolBar.add(downButton);
        // Center glue
        toolBar.add(Box.createHorizontalGlue());

        // Right status fields
        JPanel rightStatusPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        String[][] rightFields = {
                { "Ticker", "1" },
                { "Ticks", "0" },
                { "BPM", "120" },
                { "B/Bar", "4" },
                { "Bars", "4" },
                { "Parts", "1" },
                { "Length", "0" },
                { "Offset", "0" }
        };

        for (String[] field : rightFields) {
            // Create panel for vertical stacking
            JPanel fieldPanel = new JPanel();
            fieldPanel.setLayout(new BoxLayout(fieldPanel, BoxLayout.Y_AXIS));

            // Create and add label
            JLabel nameLabel = new JLabel(field[0]);
            nameLabel.setForeground(Color.GRAY);
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(nameLabel);

            // Create and add text field
            JTextField textField = new JTextField(field[1]);
            textField.setColumns(4);
            textField.setEditable(false);
            textField.setEnabled(false);
            textField.setHorizontalAlignment(JTextField.CENTER);
            textField.setBackground(new Color(240, 240, 240));
            textField.setToolTipText(field[0] + " value");
            textField.setMaximumSize(new Dimension(50, 25));
            textField.setAlignmentX(Component.CENTER_ALIGNMENT);
            fieldPanel.add(textField);

            rightStatusPanel.add(fieldPanel);
            rightStatusPanel.add(Box.createHorizontalStrut(5));
        }

        toolBar.add(rightStatusPanel);

        add(toolBar, BorderLayout.NORTH);
    }

    private JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);

        // Increase button size
        int size = 32;
        button.setPreferredSize(new Dimension(size, size));
        button.setMinimumSize(new Dimension(size, size));
        button.setMaximumSize(new Dimension(size, size));

        // Use a font that supports Unicode symbols
        button.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        // Fallback fonts if Segoe UI Symbol isn't available
        if (!button.getFont().canDisplay('⏮')) {
            button.setFont(new Font("Dialog", Font.PLAIN, 18));
        }

        // Optional: Add some padding around the text
        button.setMargin(new Insets(5, 5, 5, 5));

        return button;
    }

    private void setupMainContent() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

        // Create tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane();

        // Add Beats tab
        JPanel beatsPanel = createBeatsPanel();
        tabbedPane.addTab("Beats", beatsPanel);

        // Add Launch tab with drum pads
        JPanel launchPanel = new LaunchPanel();
        tabbedPane.addTab("Launch", launchPanel);

        // Add X0X tab
        JPanel x0xPanel = createX0XPanel();
        tabbedPane.addTab("X0X", x0xPanel);

        // Add other tabs
        tabbedPane.addTab("Params", new JPanel());
        tabbedPane.addTab("Controls", new JPanel());
        tabbedPane.addTab("Options", createOptionsPanel());

        mainPanel.add(tabbedPane, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
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
        tablesSplitPane.setDividerLocation(0.75);

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
        JPanel buttonGridPanel = createButtonGridPanel();
        // JScrollPane buttonScrollPane = new JScrollPane(buttonGridPanel);

        // Add components to main split pane
        mainPane.add(tablesSplitPane, BorderLayout.CENTER);
        mainPane.add(buttonGridPanel, BorderLayout.SOUTH);

        Dimension size = new Dimension(BUTTON_SIZE * GRID_COLS, BUTTON_SIZE * GRID_ROWS);

        buttonGridPanel.setMaximumSize(size);
        buttonGridPanel.setMinimumSize(size);
        buttonGridPanel.setPreferredSize(size);

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

    static int BUTTON_SIZE = 25;
    static int GRID_ROWS = 5;
    static int GRID_COLS = 36;

    private JPanel createButtonGridPanel() {
        JPanel panel = new JPanel(new GridLayout(5, 33, 2, 2));
        panel.setBorder(BorderFactory.createEmptyBorder());

        // Create 5x33 grid of buttons with varying colors
        Color[] colors = {
                new Color(255, 200, 200), // Light red
                new Color(200, 255, 200), // Light green
                new Color(200, 200, 255), // Light blue
                new Color(255, 255, 200), // Light yellow
                new Color(255, 200, 255), // Light purple
                new Color(200, 255, 255) // Light cyan
        };

        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE)); // Make buttons square

                // Vary colors based on position
                int colorIndex = (row * col) % colors.length;
                button.setBackground(colors[colorIndex]);
                button.setOpaque(true);
                button.setBorderPainted(true);

                // Optional: Add tooltip showing position
                button.setToolTipText(String.format("Row: %d, Col: %d", row + 1, col + 1));

                panel.add(button);
            }
        }

        return panel;
    }

    private void setupStatusBar() {
        statusBar = new JLabel(" Ready");
        statusBar.setBorder(BorderFactory.createLoweredBevelBorder());
        add(statusBar, BorderLayout.SOUTH);
    }

    public void setStatus(String message) {
        statusBar.setText(" " + message);
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
        setupColumnEditor(leftTable, "Channel", 1, 16);
        setupColumnEditor(leftTable, "Swing", 0, 100);
        setupColumnEditor(leftTable, "Level", 1, 127);
        setupColumnEditor(leftTable, "Note", 1, 127);
        setupColumnEditor(leftTable, "Min Vel", 1, 127);
        setupColumnEditor(leftTable, "Max Vel", 1, 127);
        setupColumnEditor(leftTable, "Preset", 1, 127);
        setupColumnEditor(leftTable, "Prob", 0, 100);
        setupColumnEditor(leftTable, "Random", 0, 100);
        setupColumnEditor(leftTable, "Ratchet #", 0, 6);
        setupColumnEditor(leftTable, "Pan", 1, 127);

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

    private void setupColumnEditor(JTable table, String columnName, int min, int max) {
        int columnIndex = getColumnIndex(table, columnName);
        if (columnIndex != -1) {
            TableColumn column = table.getColumnModel().getColumn(columnIndex);

            // Create combo box with range values
            JComboBox<Integer> comboBox = new JComboBox<>(
                    IntStream.rangeClosed(min, max)
                            .boxed()
                            .toArray(Integer[]::new));

            // Make combo box editable for quick value entry
            comboBox.setEditable(true);

            // Set preferred width based on maximum value's width
            int width = Math.max(60, comboBox.getPreferredSize().width + 20);
            column.setPreferredWidth(width);

            // Create and set the cell editor
            DefaultCellEditor editor = new DefaultCellEditor(comboBox) {
                @Override
                public boolean stopCellEditing() {
                    try {
                        // Validate input range
                        int value = Integer.parseInt(comboBox.getEditor().getItem().toString());
                        if (value >= min && value <= max) {
                            return super.stopCellEditing();
                        }
                        // Show error message if out of range
                        setStatus(String.format("Value must be between %d and %d", min, max));
                        return false;
                    } catch (NumberFormatException e) {
                        setStatus("Please enter a valid number");
                        return false;
                    }
                }
            };

            column.setCellEditor(editor);
        }
    }

    private int getColumnIndex(JTable table, String columnName) {
        for (int i = 0; i < table.getColumnCount(); i++) {
            if (table.getColumnName(i).equals(columnName)) {
                return i;
            }
        }
        return -1;
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

    private static class Device {
        String name;
        String description;
        String vendor;
        String version;
        int maxReceivers;
        int maxTransmitters;
        int receivers;
        int transmitters;
        boolean receiver;
        boolean transmitter;

        public Device(String name, String description, String vendor) {
            this.name = name;
            this.description = description;
            this.vendor = vendor;
            this.version = "1.0";
            this.maxReceivers = 1;
            this.maxTransmitters = 1;
            this.receivers = 0;
            this.transmitters = 0;
            this.receiver = true;
            this.transmitter = true;
        }

        public Object[] toRow() {
            return new Object[] {
                    name, description, vendor, version, maxReceivers, maxTransmitters,
                    receivers, transmitters, receiver, transmitter
            };
        }
    }

    private static class Config {
        String port;
        String device;
        boolean available;
        int channels;
        int low;
        int high;

        public Config(String port, String device, boolean available) {
            this.port = port;
            this.device = device;
            this.available = available;
            this.channels = 16;
            this.low = 0;
            this.high = 127;
        }

        public Object[] toRow() {
            return new Object[] { port, device, available, channels, low, high };
        }
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create vertical split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.5); // Equal split

        // Top section - Devices table
        JTable devicesTable = createDevicesTable();
        splitPane.setTopComponent(new JScrollPane(devicesTable));

        // Bottom section - Multiple tables
        JPanel bottomPanel = new JPanel(new GridLayout(1, 3, 5, 0));

        // Configs table
        JTable configsTable = createConfigsTable();
        bottomPanel.add(new JScrollPane(configsTable));

        // Placeholder panels for future tables
        bottomPanel.add(new JPanel());
        bottomPanel.add(new JPanel());

        splitPane.setBottomComponent(bottomPanel);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JTable createDevicesTable() {
        String[] columns = {
                "Name", "Description", "Vendor", "Version", "Max Receivers",
                "Max Transmitters", "Receivers", "Transmitters", "Receiver", "Transmitter"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells read-only
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 8 || column == 9)
                    return Boolean.class; // Receiver and Transmitter columns
                if (column >= 4 && column <= 7)
                    return Integer.class;
                return String.class;
            }
        };

        // Add sample data
        Device[] sampleDevices = {
                new Device("Midi Port 1", "Internal MIDI Port", "System"),
                new Device("USB Device", "External USB MIDI Device", "Roland"),
                new Device("Virtual Port", "Virtual MIDI Connection", "System")
        };

        for (Device device : sampleDevices) {
            model.addRow(device.toRow());
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Center-align numeric columns (but not boolean columns)
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 4; i < 8; i++) { // Only center Max Receivers through Transmitters
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set preferred column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // Description
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Vendor

        return table;
    }

    private JTable createConfigsTable() {
        String[] columns = { "Port", "Device", "Available", "Channels", "Low", "High" };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 1 && column != 2; // Device and Available are read-only
            }

            @Override
            public Class<?> getColumnClass(int column) {
                if (column == 2)
                    return Boolean.class;
                if (column >= 3)
                    return Integer.class;
                return String.class;
            }
        };

        // Add sample data
        Config[] sampleConfigs = {
                new Config("Port 1", "Midi Port 1", true),
                new Config("USB-1", "USB Device", true),
                new Config("Virtual", "Virtual Port", false)
        };

        for (Config config : sampleConfigs) {
            model.addRow(config.toRow());
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Set up port combo box
        String[] ports = { "Port 1", "USB-1", "Virtual", "Network" };
        JComboBox<String> portCombo = new JComboBox<>(ports);
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(portCombo));

        // Set up channels combo box (1-16)
        setupColumnEditor(table, "Channels", 1, 16);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 3; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        return table;
    }

    private JPanel createX0XPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel pianoPanel = createPianoPanel();
        mainPanel.add(pianoPanel, BorderLayout.NORTH);

        // Create panel for the 16 columns
        JPanel sequencePanel = new JPanel(new GridLayout(1, 16, 5, 0));
        sequencePanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Create 16 columns
        for (int i = 0; i < 16; i++) {
            JPanel columnPanel = createSequenceColumn(i);
            sequencePanel.add(columnPanel);
        }

        // Wrap in scroll pane in case window gets too small
        JScrollPane scrollPane = new JScrollPane(sequencePanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);

        mainPanel.add(scrollPane, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createPianoPanel() {
        JPanel panel = new JPanel(null); // Using null layout for precise key positioning
        panel.setPreferredSize(new Dimension(500, 80));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        panel.setBackground(new Color(40, 40, 40));

        // Dimensions for keys
        int whiteKeyWidth = 30;
        int whiteKeyHeight = 60;
        int blackKeyWidth = 17;
        int blackKeyHeight = 30;

        // Create white keys
        String[] whiteNotes = { "C", "D", "E", "F", "G", "A", "B", "C" };
        for (int i = 0; i < 8; i++) {
            JButton whiteKey = createPianoKey(true, whiteNotes[i]);
            whiteKey.setBounds(i * whiteKeyWidth + 10, 10, whiteKeyWidth - 1, whiteKeyHeight);
            panel.add(whiteKey);
        }

        // Create black keys
        String[] blackNotes = { "C#", "D#", "", "F#", "G#", "A#", "" };
        for (int i = 0; i < 7; i++) {
            if (!blackNotes[i].isEmpty()) {
                JButton blackKey = createPianoKey(false, blackNotes[i]);
                blackKey.setBounds(i * whiteKeyWidth + whiteKeyWidth / 2 + 10, 10, blackKeyWidth, blackKeyHeight);
                panel.add(blackKey, 0); // Add black keys first so they appear on top
            }
        }

        return panel;
    }

    private JButton createPianoKey(boolean isWhite, String note) {
        JButton key = new JButton();
        key.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Base color
                if (isWhite) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(0, 0, w, h);
                    // Add shadow effect
                    g2d.setColor(new Color(200, 200, 200));
                    g2d.fillRect(0, h - 10, w, 10);
                } else {
                    g2d.setColor(Color.BLACK);
                    g2d.fillRect(0, 0, w, h);
                    // Add highlight effect
                    g2d.setColor(new Color(60, 60, 60));
                    g2d.fillRect(0, 0, w, 5);
                }

                // Draw border
                g2d.setColor(Color.BLACK);
                g2d.drawRect(0, 0, w - 1, h - 1);

                // Draw note label at bottom of white keys
                if (isWhite) {
                    g2d.setColor(Color.BLACK);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    FontMetrics fm = g2d.getFontMetrics();
                    int noteWidth = fm.stringWidth(note);
                    g2d.drawString(note, (w - noteWidth) / 2, h - 15);
                }

                g2d.dispose();
            }
        });

        key.setContentAreaFilled(false);
        key.setBorderPainted(false);
        key.setFocusPainted(false);
        key.setToolTipText(note);

        return key;
    }

    private JPanel createSequenceColumn(int index) {
        // Use BoxLayout for vertical arrangement
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2));

        // Add 4 knobs
        for (int i = 0; i < 5; i++) {
            JLabel label = new JLabel(getKnobLabel(i));
            label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setForeground(Color.GRAY);
            label.setAlignmentX(Component.CENTER_ALIGNMENT);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            labelPanel.add(label);
            // Add label to the column
            column.add(labelPanel);

            JDial dial = createKnob();
            dial.setToolTipText(String.format("Step %d Knob %d", index + 1, i + 1));
            dial.setName("JDial-" + index + "-" + i);
            // Center the dial horizontally
            JPanel dialPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            dialPanel.add(dial);
            column.add(dialPanel);

            // Add small spacing between knobs
            column.add(Box.createRigidArea(new Dimension(0, 5)));
        }

        // Add the trigger button
        JButton triggerButton = createTriggerButton(index);
        triggerButton.setName("TriggerButton-" + index);

        // Center the button horizontally
        JPanel buttonPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel1.add(triggerButton);
        column.add(buttonPanel1);

        // Add the pad button
        JButton padButton = createPadButton(index);
        padButton.setName("PadButton-" + index);
        padButton.setToolTipText("Pad " + (index + 1));
        padButton.setText(Integer.toString(index + 1));

        JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonPanel2.add(padButton);
        column.add(buttonPanel2);

        return column;
    }

    private String getKnobLabel(int i) {
        return i == 0 ? "Note" : i == 1 ? "Vel." : i == 2 ? "Gate" : "Prob.";
    }

    private JDial createKnob() {
        JDial dial = new JDial();
        // Increase size by 30%
        dial.setPreferredSize(new Dimension(40, 40));
        dial.setMinimumSize(new Dimension(40, 40));
        dial.setMaximumSize(new Dimension(40, 40));
        return dial;
    }

    // Simple JDial implementation for knobs
    private class JDial extends JComponent {
        private int value = 64;
        private boolean isDragging = false;
        private int lastY;

        public JDial() {
            setPreferredSize(new Dimension(40, 40));

            addMouseListener(new java.awt.event.MouseAdapter() {
                public void mousePressed(java.awt.event.MouseEvent e) {
                    isDragging = true;
                    lastY = e.getY();
                }

                public void mouseReleased(java.awt.event.MouseEvent e) {
                    isDragging = false;
                }
            });

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                public void mouseDragged(java.awt.event.MouseEvent e) {
                    if (isDragging) {
                        int delta = lastY - e.getY();
                        value = Math.min(127, Math.max(0, value + delta));
                        lastY = e.getY();
                        repaint();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            int min = Math.min(w, h);

            // Draw knob body with blue color
            g2d.setColor(new Color(30, 100, 255));
            g2d.fillOval(0, 0, min - 1, min - 1);

            // Add a subtle gradient for 3D effect
            GradientPaint gp = new GradientPaint(
                    0, 0, new Color(60, 130, 255),
                    0, h, new Color(20, 80, 200));
            g2d.setPaint(gp);
            g2d.fillOval(2, 2, min - 4, min - 4);

            // Draw white indicator line
            g2d.setColor(Color.WHITE);
            // g2d.setStroke(new BasicStroke(2.0f));
            double angle = Math.PI * 0.75 + (Math.PI * 1.5 * value / 127.0);
            int centerX = min / 2;
            int centerY = min / 2;
            int radius = min / 2 - 6;

            g2d.drawLine(centerX, centerY,
                    centerX + (int) (Math.cos(angle) * radius),
                    centerY + (int) (Math.sin(angle) * radius));

            // Add highlight for 3D effect
            g2d.setColor(new Color(255, 255, 255, 60));
            g2d.fillOval(5, 3, min / 2 - 5, min / 2 - 5);

            g2d.dispose();
        }

        public void setValue(int newValue) {
            value = Math.min(127, Math.max(0, newValue));
            repaint();
        }

        public int getValue() {
            return value;
        }
    }

    private JButton createPadButton(int index) {
        JButton button = new JButton();
        Color baseColor = new Color(60, 60, 60); // Dark grey base
        Color flashColor = new Color(160, 160, 160); // Lighter grey for flash
        final boolean[] isFlashing = { false };

        button.addActionListener(e -> {
            isFlashing[0] = true;
            button.repaint();

            Timer timer = new Timer(100, evt -> {
                isFlashing[0] = false;
                button.repaint();
                ((Timer) evt.getSource()).stop();
            });
            timer.setRepeats(false);
            timer.start();
        });

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                if (isFlashing[0]) {
                    g2d.setColor(flashColor);
                } else {
                    g2d.setColor(baseColor);
                }

                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                g2d.dispose();
            }
        });

        button.setPreferredSize(new Dimension(40, 40));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);

        return button;
    }

    private JButton createTriggerButton(int index) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(30, 20));
        button.setMinimumSize(new Dimension(30, 20));
        button.setMaximumSize(new Dimension(30, 20));

        final boolean[] isActive = { false };

        // Orange for inactive state
        Color topColorInactive = new Color(255, 140, 0); // Bright orange
        Color bottomColorInactive = new Color(200, 110, 0); // Darker orange

        // Green for active state
        Color topColorActive = new Color(50, 255, 50); // Bright green
        Color bottomColorActive = new Color(40, 200, 40); // Darker green

        button.addActionListener(e -> {
            isActive[0] = !isActive[0];
            button.repaint();
        });

        button.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                // Choose colors based on state
                Color topColor = isActive[0] ? topColorActive : topColorInactive;
                Color bottomColor = isActive[0] ? bottomColorActive : bottomColorInactive;

                GradientPaint gp = new GradientPaint(
                        0, 0, topColor,
                        0, h, bottomColor);

                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add border
                g2d.setColor(new Color(80, 80, 80));
                g2d.drawRoundRect(0, 0, w - 1, h - 1, 10, 10);

                // Add highlight
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawLine(2, 2, w - 3, 2);

                g2d.dispose();
            }
        });

        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setToolTipText("Step " + (index + 1));

        return button;
    }
}
