package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import com.angrysurfer.beats.Dialog;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.midi.ControlCode;
import com.angrysurfer.core.model.midi.ControlCodeCaption;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentsPanel extends StatusProviderPanel {
    private final CommandBus commandBus = CommandBus.getInstance();
    private JTable instrumentsTable;
    private JTable controlCodesTable;
    private JTable captionsTable;
    private Instrument selectedInstrument;
    private ControlCode selectedControlCode;
    private JButton addCaptionButton;
    private JButton editCaptionButton;
    private JButton deleteCaptionButton;
    private JButton addInstrumentButton;
    private JButton editInstrumentButton;
    private JButton deleteInstrumentButton;
    private static final Logger logger = Logger.getLogger(InstrumentsPanel.class.getName());
    private ContextMenuHelper instrumentsContextMenu;
    private ContextMenuHelper controlCodesContextMenu;
    private ContextMenuHelper captionsContextMenu;

    public InstrumentsPanel() {
        this(null);
    }

    public InstrumentsPanel(StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        setup();
        registerCommandListener();
    }

    private void setup() {
        setLayout(new BorderLayout());

        // Create tables first
        instrumentsTable = createInstrumentsTable();
        setupInstrumentsTableSelectionListener();

        controlCodesTable = createControlCodesTable();
        setupControlCodesTableSelectionListener();

        captionsTable = createCaptionsTable();
        setupCaptionsTableSelectionListener();

        // Setup context menus after tables exist
        setupContextMenus();

        // Finally add everything to the panel
        add(createOptionsPanel(), BorderLayout.CENTER);
    }

    private void registerCommandListener() {
        commandBus.register(command -> {
            if (Commands.LOAD_CONFIG.equals(command.getCommand())) {
                // SwingUtilities.invokeLater(() -> showConfigFileChooserDialog());
            }
        });
    }

    private JPanel createOptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        // Create horizontal split pane for instruments and control codes
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Right side - Control Codes and Captions
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        // Create panels using already created tables
        JPanel controlCodesPanel = new JPanel(new BorderLayout());
        JPanel controlCodesToolbar = setupControlCodeToolbar();
        controlCodesPanel.add(controlCodesToolbar, BorderLayout.NORTH);
        controlCodesPanel.add(new JScrollPane(controlCodesTable), BorderLayout.CENTER);
        controlCodesPanel.setPreferredSize(new Dimension(250, controlCodesPanel.getPreferredSize().height));
        controlCodesPanel.setMinimumSize(new Dimension(250, 0));

        // Create captions panel with toolbar and fixed width
        JPanel captionsPanel = new JPanel(new BorderLayout());
        JPanel captionsToolbar = setupCaptionsToolbar();
        captionsPanel.add(captionsToolbar, BorderLayout.NORTH);
        captionsPanel.add(new JScrollPane(captionsTable), BorderLayout.CENTER);
        captionsPanel.setPreferredSize(new Dimension(250, captionsPanel.getPreferredSize().height));
        captionsPanel.setMinimumSize(new Dimension(250, 0));

        rightSplitPane.setLeftComponent(controlCodesPanel);
        rightSplitPane.setRightComponent(captionsPanel);
        rightSplitPane.setResizeWeight(0.5);

        // Left side - Instruments panel gets all extra space
        JPanel instrumentsPanel = new JPanel(new BorderLayout());
        JPanel instrumentsToolbar = setupInstrumentToolbar();
        instrumentsPanel.add(instrumentsToolbar, BorderLayout.NORTH);
        instrumentsPanel.add(new JScrollPane(instrumentsTable), BorderLayout.CENTER);

        mainSplitPane.setLeftComponent(instrumentsPanel);
        mainSplitPane.setRightComponent(rightSplitPane);
        mainSplitPane.setResizeWeight(0.6); // Give more weight to instruments panel

        panel.add(mainSplitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel setupInstrumentToolbar() {
        JPanel toolBar = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        addInstrumentButton = new JButton("Add");
        editInstrumentButton = new JButton("Edit");
        deleteInstrumentButton = new JButton("Delete");

        addInstrumentButton.addActionListener(e -> showInstrumentDialog(null));
        editInstrumentButton.addActionListener(e -> editSelectedInstrument());
        // deleteInstrumentButton.addActionListener(e -> deleteSelectedInstrument());

        buttonPanel.add(addInstrumentButton);
        buttonPanel.add(editInstrumentButton);
        buttonPanel.add(deleteInstrumentButton);

        toolBar.add(buttonPanel, BorderLayout.CENTER);
        return toolBar;
    }

    private JPanel setupControlCodeToolbar() {
        JPanel toolBar = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        addButton.addActionListener(e -> showControlCodeDialog(null));
        editButton.addActionListener(e -> editSelectedControlCode());
        deleteButton.addActionListener(e -> deleteSelectedControlCode());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        toolBar.add(buttonPanel, BorderLayout.CENTER);
        return toolBar;
    }

    private JPanel setupCaptionsToolbar() {
        JPanel toolBar = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        addCaptionButton = new JButton("Add");
        editCaptionButton = new JButton("Edit");
        deleteCaptionButton = new JButton("Delete");

        addCaptionButton.addActionListener(e -> showCaptionDialog(null));
        editCaptionButton.addActionListener(e -> editSelectedCaption());
        deleteCaptionButton.addActionListener(e -> deleteSelectedCaption());

        buttonPanel.add(addCaptionButton);
        buttonPanel.add(editCaptionButton);
        buttonPanel.add(deleteCaptionButton);

        toolBar.add(buttonPanel, BorderLayout.CENTER);
        return toolBar;
    }

    private JTable createInstrumentsTable() {
        String[] columns = {
                "Name", "Device Name", "Available", "Low", "High", "Initialized"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make all cells read-only for now
            }

            @Override
            public Class<?> getColumnClass(int column) {
                switch (column) {
                    case 2: // Available
                    case 5: // Initialized
                        return Boolean.class;
                    case 3: // Lowest Note
                    case 4: // Highest Note
                        return Integer.class;
                    default:
                        return String.class;
                }
            }
        };

        // Load data from Redis
        List<Instrument> instruments = RedisService.getInstance().findAllInstruments();
        for (Instrument instrument : instruments) {
            model.addRow(new Object[] {
                    instrument.getName(),
                    instrument.getDeviceName(),
                    instrument.getAvailable(),
                    instrument.getLowestNote(),
                    instrument.getHighestNote(),
                    instrument.isInitialized()
            });
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true); // Enable sorting

        // Add default sorting by name (column 0)
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 3; i <= 4; i++) { // Center-align note columns
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Center-align and set up boolean renderer for numeric and boolean columns
        table.getColumnModel().getColumn(2).setCellRenderer(table.getDefaultRenderer(Boolean.class)); // Available
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Lowest Note
        table.getColumnModel().getColumn(4).setCellRenderer(centerRenderer); // Highest Note
        table.getColumnModel().getColumn(5).setCellRenderer(table.getDefaultRenderer(Boolean.class)); // Initialized

        // Set column widths - make name column extra wide
        table.getColumnModel().getColumn(0).setPreferredWidth(250); // Name - wider
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // Device Name

        // Fixed minimum widths for numeric and boolean columns
        table.getColumnModel().getColumn(2).setMaxWidth(60); // Available
        table.getColumnModel().getColumn(2).setPreferredWidth(60);
        table.getColumnModel().getColumn(3).setMaxWidth(80); // Lowest Note
        table.getColumnModel().getColumn(3).setPreferredWidth(80);
        table.getColumnModel().getColumn(4).setMaxWidth(80); // Highest Note
        table.getColumnModel().getColumn(4).setPreferredWidth(80);
        table.getColumnModel().getColumn(5).setMaxWidth(60); // Initialized
        table.getColumnModel().getColumn(5).setPreferredWidth(60);

        // Add selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    String name = (String) table.getValueAt(row, 0);
                    selectedInstrument = findInstrumentByName(name);
                    updateControlCodesTable();
                    // Auto-select first control code if available
                    if (controlCodesTable.getRowCount() > 0) {
                        controlCodesTable.setRowSelectionInterval(0, 0);
                    }
                }
            }
        });

        // Add selection listener for button states
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0;
                editInstrumentButton.setEnabled(hasSelection);
                deleteInstrumentButton.setEnabled(hasSelection);
            }
        });

        // Add double-click handler
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedInstrument();
                }
            }
        });

        return table;
    }

    private JTable createControlCodesTable() {
        String[] columns = {
                "Name", "Code", "Min", "Max"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column > 0 ? Integer.class : String.class;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true); // Enable sorting

        // Add default sorting by name (column 0)
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        // Center-align all numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);

        // Code, Lower Bound, Upper Bound
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Set fixed column widths for control codes table
        table.getColumnModel().getColumn(0).setPreferredWidth(100); // Name
        table.getColumnModel().getColumn(0).setMinWidth(100);

        // Fixed widths for numeric columns
        table.getColumnModel().getColumn(1).setMaxWidth(50); // Code
        table.getColumnModel().getColumn(1).setMinWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(45); // Lower Bound
        table.getColumnModel().getColumn(2).setMinWidth(45);
        table.getColumnModel().getColumn(3).setMaxWidth(45); // Upper Bound
        table.getColumnModel().getColumn(3).setMinWidth(45);

        // Modify selection listener to handle caption buttons
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = table.getSelectedRow();
                boolean hasSelection = row >= 0;
                if (hasSelection && selectedInstrument != null) {
                    String name = (String) table.getValueAt(row, 0);
                    selectedControlCode = findControlCodeByName(name);
                    updateCaptionsTable();
                    addCaptionButton.setEnabled(true);
                } else {
                    selectedControlCode = null;
                    updateCaptionsTable();
                    addCaptionButton.setEnabled(false);
                    editCaptionButton.setEnabled(false);
                    deleteCaptionButton.setEnabled(false);
                }
            }
        });

        // Add double-click handler
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedControlCode();
                }
            }
        });

        return table;
    }

    private JTable createCaptionsTable() {
        String[] columns = {
                "Code", "Description"
        };

        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Long.class : String.class;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true); // Enable sorting

        // Add default sorting by description (column 1)
        RowSorter<? extends TableModel> sorter = table.getRowSorter();
        ArrayList<RowSorter.SortKey> sortKeys = new ArrayList<>();
        sortKeys.add(new RowSorter.SortKey(1, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        // Center-align the numeric code column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        // Set fixed column widths for captions table
        table.getColumnModel().getColumn(0).setMaxWidth(50); // Code
        table.getColumnModel().getColumn(0).setMinWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(190); // Description
        table.getColumnModel().getColumn(1).setMinWidth(190);

        // Modify selection listener to only enable edit/delete when caption is selected
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0 && selectedControlCode != null;
                editCaptionButton.setEnabled(hasSelection);
                deleteCaptionButton.setEnabled(hasSelection);
            }
        });

        // Add double-click handler
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedCaption();
                }
            }
        });

        return table;
    }

    private void updateControlCodesTable() {
        DefaultTableModel model = (DefaultTableModel) controlCodesTable.getModel();
        model.setRowCount(0);

        if (selectedInstrument != null && selectedInstrument.getControlCodes() != null) {
            logger.info("Updating control codes table for instrument: " + selectedInstrument.getName() +
                    " with " + selectedInstrument.getControlCodes().size() + " codes");

            for (ControlCode cc : selectedInstrument.getControlCodes()) {
                model.addRow(new Object[] {
                        cc.getName(),
                        cc.getCode(),
                        cc.getLowerBound(),
                        cc.getUpperBound()
                });
                logger.info("Added control code to table: " + cc.getName());
            }
        } else {
            logger.warning("No control codes to display - instrument: " +
                    (selectedInstrument == null ? "null" : selectedInstrument.getName()));
        }
    }

    private void updateCaptionsTable() {
        DefaultTableModel model = (DefaultTableModel) captionsTable.getModel();
        model.setRowCount(0);

        if (selectedControlCode != null && selectedControlCode.getCaptions() != null) {
            logger.info("Updating captions table for control code: " + selectedControlCode.getName() +
                    " with " + selectedControlCode.getCaptions().size() + " captions");

            for (ControlCodeCaption caption : selectedControlCode.getCaptions()) {
                model.addRow(new Object[] {
                        caption.getCode(),
                        caption.getDescription()
                });
                logger.info("Added caption to table: " + caption.getDescription());
            }
        } else {
            logger.warning("No captions to display - control code: " +
                    (selectedControlCode == null ? "null" : selectedControlCode.getName()));
        }
    }

    private Instrument findInstrumentByName(String name) {
        // Convert view index to model index when getting data
        return RedisService.getInstance().findAllInstruments().stream()
                .filter(i -> i.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    private ControlCode findControlCodeByName(String name) {
        if (selectedInstrument != null && selectedInstrument.getControlCodes() != null) {
            return selectedInstrument.getControlCodes().stream()
                    .filter(cc -> cc.getName().equals(name))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private void showControlCodeDialog(ControlCode controlCode) {
        boolean isNew = (controlCode == null);
        if (isNew) {
            controlCode = new ControlCode();
        }

        ControlCodeEditPanel editorPanel = new ControlCodeEditPanel(controlCode);
        Dialog<ControlCode> dialog = new Dialog<>(controlCode, editorPanel);
        dialog.setTitle(isNew ? "Add Control Code" : "Edit Control Code");

        if (dialog.showDialog()) {
            ControlCode updatedControlCode = editorPanel.getUpdatedControlCode();
            if (isNew) {
                selectedInstrument.getControlCodes().add(updatedControlCode);
            }
            RedisService.getInstance().saveInstrument(selectedInstrument);
            updateControlCodesTable();
        }
    }

    private void editSelectedControlCode() {
        int row = controlCodesTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) controlCodesTable.getValueAt(row, 0);
            ControlCode controlCode = findControlCodeByName(name);
            if (controlCode != null) {
                showControlCodeDialog(controlCode);
            }
        }
    }

    private void deleteSelectedControlCode() {
        int row = controlCodesTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) controlCodesTable.getValueAt(row, 0);
            ControlCode controlCode = findControlCodeByName(name);
            if (controlCode != null) {
                selectedInstrument.getControlCodes().remove(controlCode);
                RedisService.getInstance().saveInstrument(selectedInstrument);
                updateControlCodesTable();
            }
        }
    }

    private void showCaptionDialog(ControlCodeCaption caption) {
        if (selectedControlCode == null) {
            setStatus("No control code selected");
            return;
        }

        boolean isNew = (caption == null);
        if (isNew) {
            caption = new ControlCodeCaption();
            caption.setCode((long) selectedControlCode.getCode());
        }

        CaptionEditPanel editorPanel = new CaptionEditPanel(caption);
        Dialog<ControlCodeCaption> dialog = new Dialog<>(caption, editorPanel);
        dialog.setTitle(isNew ? "Add Caption" : "Edit Caption");

        if (dialog.showDialog()) {
            ControlCodeCaption updatedCaption = editorPanel.getUpdatedCaption();
            if (isNew) {
                if (selectedControlCode.getCaptions() == null) {
                    selectedControlCode.setCaptions(new HashSet<>());
                }
                selectedControlCode.getCaptions().add(updatedCaption);
            }
            RedisService.getInstance().saveInstrument(selectedInstrument);
            updateCaptionsTable();
        }
    }

    private void editSelectedCaption() {
        int selectedRow = captionsTable.getSelectedRow();
        if (selectedRow >= 0) {
            // Get caption from selected row, not just first caption
            ControlCodeCaption caption = getCaptionFromRow(selectedRow);
            if (caption != null) {
                logger.info("Editing caption: " + caption.getDescription() + " for control code: " + selectedControlCode.getName());
                showCaptionDialog(caption);
            }
        }
    }

    private void deleteSelectedCaption() {
        int row = captionsTable.getSelectedRow();
        if (row >= 0) {
            ControlCodeCaption caption = getCaptionFromRow(row);
            selectedControlCode.getCaptions().remove(caption);
            RedisService.getInstance().saveInstrument(selectedInstrument);
            updateCaptionsTable();
        }
    }

    private ControlCodeCaption getCaptionFromRow(int row) {
        if (selectedControlCode != null && selectedControlCode.getCaptions() != null) {
            // Convert view index to model index if table is sorted
            int modelRow = captionsTable.convertRowIndexToModel(row);
            
            // Get values from table model
            Long code = (Long) captionsTable.getModel().getValueAt(modelRow, 0);
            String description = (String) captionsTable.getModel().getValueAt(modelRow, 1);
            
            // Find matching caption in control code's captions
            return selectedControlCode.getCaptions().stream()
                .filter(c -> c.getCode().equals(code) && 
                            c.getDescription().equals(description))
                .findFirst()
                .orElse(null);
        }
        return null;
    }

    private void showInstrumentDialog(Instrument instrument) {
        boolean isNew = (instrument == null);
        if (isNew) {
            instrument = new Instrument();
        }

        InstrumentEditPanel editorPanel = new InstrumentEditPanel(instrument);
        Dialog<Instrument> dialog = new Dialog<>(instrument, editorPanel);
        dialog.setTitle(isNew ? "Add Instrument" : "Edit Instrument");

        if (dialog.showDialog()) {
            Instrument updatedInstrument = editorPanel.getUpdatedInstrument();
            if (isNew) {
                // Let Redis assign an ID
                RedisService.getInstance().saveInstrument(updatedInstrument);
            } else {
                RedisService.getInstance().saveInstrument(updatedInstrument);
            }
            refreshInstrumentsTable();
        }
    }

    private void deleteSelectedInstrument() {
        if (selectedInstrument != null) {
            if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this instrument?",
                    "Delete Instrument", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
                return;
            }

            RedisService.getInstance().deleteInstrument(selectedInstrument);
            refreshInstrumentsTable();
        }
    }

    private void editSelectedInstrument() {
        int row = instrumentsTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) instrumentsTable.getValueAt(row, 0);
            Instrument instrument = findInstrumentByName(name);
            if (instrument != null) {
                showInstrumentDialog(instrument);
            }
        }
    }

    // private void deleteSelectedInstrument() {
    // int row = instrumentsTable.getSelectedRow();
    // if (row >= 0) {
    // String name = (String) instrumentsTable.getValueAt(row, 0);
    // ProxyInstrument instrument = findInstrumentByName(name);
    // if (instrument != null) {
    // RedisService.getInstance().deleteInstrument(instrument);
    // refreshInstrumentsTable();
    // }
    // }
    // }

    private void refreshInstrumentsTable() {
        DefaultTableModel model = (DefaultTableModel) instrumentsTable.getModel();
        model.setRowCount(0);

        // Get fresh data from Redis
        List<Instrument> instruments = RedisService.getInstance().findAllInstruments();
        logger.info("Refreshing instruments table with " + instruments.size() + " instruments");

        // Add each instrument to the table
        for (Instrument instrument : instruments) {
            logger.info("Adding instrument to table: " + instrument.getName());
            model.addRow(new Object[] {
                    instrument.getName(),
                    instrument.getDeviceName(),
                    instrument.getAvailable(),
                    instrument.getLowestNote(),
                    instrument.getHighestNote(),
                    instrument.isInitialized()
            });
        }

        // Notify the table that the model has changed
        model.fireTableDataChanged();
        instrumentsTable.revalidate();
        instrumentsTable.repaint();

        // Clear selection and related data
        instrumentsTable.clearSelection();
        selectedInstrument = null;
        updateControlCodesTable();

        // Log the current state
        logger.info("Table refreshed with " + model.getRowCount() + " rows");
    }

    private void setupContextMenus() {
        // Create context menus
        instrumentsContextMenu = new ContextMenuHelper(
                "ADD_INSTRUMENT",
                "EDIT_INSTRUMENT",
                "DELETE_INSTRUMENT");

        controlCodesContextMenu = new ContextMenuHelper(
                "ADD_CONTROL_CODE",
                "EDIT_CONTROL_CODE",
                "DELETE_CONTROL_CODE");

        captionsContextMenu = new ContextMenuHelper(
                "ADD_CAPTION",
                "EDIT_CAPTION",
                "DELETE_CAPTION");

        // Now safe to install on existing tables
        instrumentsContextMenu.install(instrumentsTable);
        controlCodesContextMenu.install(controlCodesTable);
        captionsContextMenu.install(captionsTable);

        // Setup action listeners
        setupContextMenuListeners();
    }

    private void setupContextMenuListeners() {
        instrumentsContextMenu.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case "ADD_INSTRUMENT" -> showInstrumentDialog(null);
                case "EDIT_INSTRUMENT" -> editSelectedInstrument();
                case "DELETE_INSTRUMENT" -> deleteSelectedInstrument();
            }
        });

        controlCodesContextMenu.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case "ADD_CONTROL_CODE" -> showControlCodeDialog(null);
                case "EDIT_CONTROL_CODE" -> editSelectedControlCode();
                case "DELETE_CONTROL_CODE" -> deleteSelectedControlCode();
            }
        });

        captionsContextMenu.addActionListener(e -> {
            switch (e.getActionCommand()) {
                case "ADD_CAPTION" -> showCaptionDialog(null);
                case "EDIT_CAPTION" -> editSelectedCaption();
                case "DELETE_CAPTION" -> deleteSelectedCaption();
            }
        });
    }

    // Update selection listeners to handle context menu state

    private void setupInstrumentsTableSelectionListener() {
        instrumentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = instrumentsTable.getSelectedRow() >= 0;
                // Update both button and context menu states
                editInstrumentButton.setEnabled(hasSelection);
                deleteInstrumentButton.setEnabled(hasSelection);
                instrumentsContextMenu.setEditEnabled(hasSelection);
                instrumentsContextMenu.setDeleteEnabled(hasSelection);
            }
        });
    }

    private void setupControlCodesTableSelectionListener() {
        controlCodesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = controlCodesTable.getSelectedRow() >= 0;
                controlCodesContextMenu.setEditEnabled(hasSelection);
                controlCodesContextMenu.setDeleteEnabled(hasSelection);
                addCaptionButton.setEnabled(hasSelection);
                captionsContextMenu.setAddEnabled(hasSelection);
            }
        });
    }

    private void setupCaptionsTableSelectionListener() {
        captionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = captionsTable.getSelectedRow() >= 0 && selectedControlCode != null;
                editCaptionButton.setEnabled(hasSelection);
                deleteCaptionButton.setEnabled(hasSelection);
                captionsContextMenu.setEditEnabled(hasSelection);
                captionsContextMenu.setDeleteEnabled(hasSelection);
            }
        });
    }
}
