package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beatsui.App;
import com.angrysurfer.beatsui.Dialog;
import com.angrysurfer.beatsui.api.CommandBus;
import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.mock.Caption;
import com.angrysurfer.beatsui.mock.ControlCode;
import com.angrysurfer.beatsui.mock.Instrument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InstrumentsPanel extends StatusProviderPanel {
    private final CommandBus actionBus = CommandBus.getInstance();
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

    public InstrumentsPanel() {
        this(null);
    }

    public InstrumentsPanel(StatusConsumer statusConsumer) {
        super(new BorderLayout(), statusConsumer);
        setup();
    }

    private void setup() {
        setLayout(new BorderLayout());
        add(createOptionsPanel(), BorderLayout.CENTER);
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

        // Create horizontal split pane for instruments and control codes
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Create tables first
        instrumentsTable = createConfigsTable();
        controlCodesTable = createControlCodesTable();
        captionsTable = createCaptionsTable();

        // Right side - Control Codes and Captions
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Create control codes panel with toolbar
        JPanel controlCodesPanel = new JPanel(new BorderLayout());
        JPanel controlCodesToolbar = setupControlCodeToolbar(); // Now tables exist when creating toolbars
        controlCodesPanel.add(controlCodesToolbar, BorderLayout.NORTH);
        controlCodesPanel.add(new JScrollPane(controlCodesTable), BorderLayout.CENTER);
        
        // Create captions panel with toolbar
        JPanel captionsPanel = new JPanel(new BorderLayout());
        JPanel captionsToolbar = setupCaptionsToolbar();
        captionsPanel.add(captionsToolbar, BorderLayout.NORTH);
        captionsPanel.add(new JScrollPane(captionsTable), BorderLayout.CENTER);

        // Set fixed widths for the right panels
        controlCodesPanel.setPreferredSize(new Dimension(300, controlCodesPanel.getPreferredSize().height));
        captionsPanel.setPreferredSize(new Dimension(300, captionsPanel.getPreferredSize().height));
        
        rightSplitPane.setLeftComponent(controlCodesPanel);
        rightSplitPane.setRightComponent(captionsPanel);
        rightSplitPane.setResizeWeight(0.5);

        // Left side - Instruments panel with toolbar
        JPanel instrumentsPanel = new JPanel(new BorderLayout());
        JPanel instrumentsToolbar = setupInstrumentToolbar();
        instrumentsPanel.add(instrumentsToolbar, BorderLayout.NORTH);
        instrumentsPanel.add(new JScrollPane(instrumentsTable), BorderLayout.CENTER);

        mainSplitPane.setLeftComponent(instrumentsPanel);
        mainSplitPane.setRightComponent(rightSplitPane);
        mainSplitPane.setResizeWeight(0.3);

        panel.add(mainSplitPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel setupInstrumentToolbar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addInstrumentButton = new JButton("Add");
        editInstrumentButton = new JButton("Edit");
        deleteInstrumentButton = new JButton("Delete");

        addInstrumentButton.addActionListener(e -> showInstrumentDialog(null));
        editInstrumentButton.addActionListener(e -> editSelectedInstrument());
        deleteInstrumentButton.addActionListener(e -> deleteSelectedInstrument());

        toolBar.add(addInstrumentButton);
        toolBar.add(editInstrumentButton);
        toolBar.add(deleteInstrumentButton);

        // Initial button states
        editInstrumentButton.setEnabled(false);
        deleteInstrumentButton.setEnabled(false);

        return toolBar;
    }

    private JPanel setupControlCodeToolbar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        addButton.addActionListener(e -> showControlCodeDialog(null));
        editButton.addActionListener(e -> editSelectedControlCode());
        deleteButton.addActionListener(e -> deleteSelectedControlCode());

        toolBar.add(addButton);
        toolBar.add(editButton);
        toolBar.add(deleteButton);

        // Initial button states - all disabled
        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);

        // Update add button when instrument is selected
        instrumentsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                addButton.setEnabled(instrumentsTable.getSelectedRow() >= 0);
            }
        });

        // Update edit/delete buttons when control code is selected
        controlCodesTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = controlCodesTable.getSelectedRow() >= 0;
                editButton.setEnabled(hasSelection);
                deleteButton.setEnabled(hasSelection);
            }
        });

        return toolBar;
    }

    private JPanel setupCaptionsToolbar() {
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addCaptionButton = new JButton("Add");
        editCaptionButton = new JButton("Edit");
        deleteCaptionButton = new JButton("Delete");

        addCaptionButton.addActionListener(e -> showCaptionDialog(null));
        editCaptionButton.addActionListener(e -> editSelectedCaption());
        deleteCaptionButton.addActionListener(e -> deleteSelectedCaption());

        toolBar.add(addCaptionButton);
        toolBar.add(editCaptionButton);
        toolBar.add(deleteCaptionButton);

        // Initial button states
        addCaptionButton.setEnabled(false);
        editCaptionButton.setEnabled(false);
        deleteCaptionButton.setEnabled(false);

        return toolBar;
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

        // Load MIDI devices
        try {
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            for (MidiDevice.Info info : infos) {
                MidiDevice device = MidiSystem.getMidiDevice(info);
                model.addRow(new Object[] {
                    info.getName(),
                    info.getDescription(),
                    info.getVendor(),
                    info.getVersion(),
                    device.getMaxReceivers(),
                    device.getMaxTransmitters(),
                    device.getReceivers().size(),
                    device.getTransmitters().size(),
                    device.getMaxReceivers() != 0,
                    device.getMaxTransmitters() != 0
                });
            }
        } catch (MidiUnavailableException e) {
            setStatus("Error loading MIDI devices: " + e.getMessage());
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);

        // Center-align numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 4; i < 8; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Set column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(150); // Name
        table.getColumnModel().getColumn(1).setPreferredWidth(200); // Description
        table.getColumnModel().getColumn(2).setPreferredWidth(100); // Vendor
        
        // Set fixed widths for numeric columns
        for (int i = 4; i <= 7; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(60);
            table.getColumnModel().getColumn(i).setMaxWidth(60);
        }

        return table;
    }

    private JTable createConfigsTable() {
        String[] columns = { 
            "Name", "Device Name", "Available", "Lowest Note", "Highest Note", "Initialized" 
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
        List<Instrument> instruments = App.getRedisService().findAllInstruments();
        for (Instrument instrument : instruments) {
            model.addRow(new Object[] {
                instrument.getName(),
                instrument.getDeviceName(),
                false, // instrument.isAvailable(),
                instrument.getLowestNote(),
                instrument.getHighestNote(),
                instrument.isInitialized()
            });
        }

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);  // Enable sorting

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

        return table;
    }

    private JTable createControlCodesTable() {
        String[] columns = {
            "Name", "Code", "Lower Bound", "Upper Bound"
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
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        table.setAutoCreateRowSorter(true);  // Enable sorting

        // Center-align all numeric columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        
        // Code, Lower Bound, Upper Bound
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);

        // Set column widths - minimize all except name
        table.getColumnModel().getColumn(0).setPreferredWidth(120); // Name
        
        // Fixed minimum widths for all numeric columns
        table.getColumnModel().getColumn(1).setMaxWidth(50); // Code
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setMaxWidth(70); // Lower Bound
        table.getColumnModel().getColumn(2).setPreferredWidth(70);
        table.getColumnModel().getColumn(3).setMaxWidth(70); // Upper Bound
        table.getColumnModel().getColumn(3).setPreferredWidth(70);

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
        table.setAutoCreateRowSorter(true);  // Enable sorting

        // Center-align the numeric code column
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);

        // Set column widths
        table.getColumnModel().getColumn(0).setMaxWidth(60); // Code
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(240); // Description

        // Modify selection listener to only enable edit/delete when caption is selected
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0 && selectedControlCode != null;
                editCaptionButton.setEnabled(hasSelection);
                deleteCaptionButton.setEnabled(hasSelection);
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
            
            for (Caption caption : selectedControlCode.getCaptions()) {
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
        return App.getRedisService().findAllInstruments().stream()
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

        ControlCodeEditorPanel editorPanel = new ControlCodeEditorPanel(controlCode);
        Dialog<ControlCode> dialog = new Dialog<>(controlCode, editorPanel);
        dialog.setTitle(isNew ? "Add Control Code" : "Edit Control Code");

        if (dialog.showDialog()) {
            ControlCode updatedControlCode = editorPanel.getUpdatedControlCode();
            if (isNew) {
                selectedInstrument.getControlCodes().add(updatedControlCode);
            }
            App.getRedisService().saveInstrument(selectedInstrument);
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
                App.getRedisService().saveInstrument(selectedInstrument);
                updateControlCodesTable();
            }
        }
    }

    private void showCaptionDialog(Caption caption) {
        if (selectedControlCode == null) {
            setStatus("No control code selected");
            return;
        }

        boolean isNew = (caption == null);
        if (isNew) {
            caption = new Caption();
            caption.setCode((long) selectedControlCode.getCode());
        }

        CaptionEditorPanel editorPanel = new CaptionEditorPanel(caption);
        Dialog<Caption> dialog = new Dialog<>(caption, editorPanel);
        dialog.setTitle(isNew ? "Add Caption" : "Edit Caption");

        if (dialog.showDialog()) {
            Caption updatedCaption = editorPanel.getUpdatedCaption();
            if (isNew) {
                if (selectedControlCode.getCaptions() == null) {
                    selectedControlCode.setCaptions(new HashSet<>());
                }
                selectedControlCode.getCaptions().add(updatedCaption);
            }
            App.getRedisService().saveInstrument(selectedInstrument);
            updateCaptionsTable();
        }
    }

    private void editSelectedCaption() {
        int row = captionsTable.getSelectedRow();
        if (row >= 0) {
            Caption caption = getCaptionFromRow(row);
            showCaptionDialog(caption);
        }
    }

    private void deleteSelectedCaption() {
        int row = captionsTable.getSelectedRow();
        if (row >= 0) {
            Caption caption = getCaptionFromRow(row);
            selectedControlCode.getCaptions().remove(caption);
            App.getRedisService().saveInstrument(selectedInstrument);
            updateCaptionsTable();
        }
    }

    private Caption getCaptionFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) captionsTable.getModel();
        Caption caption = new Caption();
        caption.setCode((Long) model.getValueAt(row, 0));
        caption.setDescription((String) model.getValueAt(row, 1));
        return caption;
    }

    private void showInstrumentDialog(Instrument instrument) {
        boolean isNew = (instrument == null);
        if (isNew) {
            instrument = new Instrument();
        }

        InstrumentEditorPanel editorPanel = new InstrumentEditorPanel(instrument);
        Dialog<Instrument> dialog = new Dialog<>(instrument, editorPanel);
        dialog.setTitle(isNew ? "Add Instrument" : "Edit Instrument");

        if (dialog.showDialog()) {
            Instrument updatedInstrument = editorPanel.getUpdatedInstrument();
            if (isNew) {
                // Let Redis assign an ID
                App.getRedisService().saveInstrument(updatedInstrument);
            } else {
                App.getRedisService().saveInstrument(updatedInstrument);
            }
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

    private void deleteSelectedInstrument() {
        int row = instrumentsTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) instrumentsTable.getValueAt(row, 0);
            Instrument instrument = findInstrumentByName(name);
            if (instrument != null) {
                App.getRedisService().deleteInstrument(instrument);
                refreshInstrumentsTable();
            }
        }
    }

    private void refreshInstrumentsTable() {
        DefaultTableModel model = (DefaultTableModel) instrumentsTable.getModel();
        model.setRowCount(0);
        List<Instrument> instruments = App.getRedisService().findAllInstruments();
        for (Instrument instrument : instruments) {
            model.addRow(new Object[] {
                instrument.getName(),
                instrument.getDeviceName(),
                false, // instrument.isAvailable(),
                instrument.getLowestNote(),
                instrument.getHighestNote(),
                instrument.isInitialized()
            });
        }
    }
}
