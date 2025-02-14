package com.angrysurfer.beatsui.panel;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashSet;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

import com.angrysurfer.beatsui.Dialog;
import com.angrysurfer.beatsui.api.Command;
import com.angrysurfer.beatsui.api.CommandBus;
import com.angrysurfer.beatsui.api.Commands;
import com.angrysurfer.beatsui.proxy.ProxyCaption;
import com.angrysurfer.beatsui.proxy.ProxyControlCode;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ControlCodeEditorPanel extends JPanel {
    private CommandBus actionBus = CommandBus.getInstance();
    private ProxyControlCode controlCode;
    private JTextField nameField;
    private JSpinner codeSpinner;
    private JSpinner lowerBoundSpinner;
    private JSpinner upperBoundSpinner;
    private JTable captionsTable;
    private JButton addCaptionButton;
    private JButton editCaptionButton;
    private JButton deleteCaptionButton;

    public ControlCodeEditorPanel(ProxyControlCode controlCode) {
        super(new BorderLayout());
        this.controlCode = controlCode;

        // Create input fields
        nameField = new JTextField(controlCode.getName(), 20);
        
        // Fix spinners to use proper initialization
        codeSpinner = new JSpinner(new SpinnerNumberModel(
            controlCode.getCode() != null ? controlCode.getCode() : 0, 
            0, 127, 1));
            
        lowerBoundSpinner = new JSpinner(new SpinnerNumberModel(
            controlCode.getLowerBound() != null ? controlCode.getLowerBound() : 0, 
            0, 127, 1));
            
        upperBoundSpinner = new JSpinner(new SpinnerNumberModel(
            controlCode.getUpperBound() != null ? controlCode.getUpperBound() : 127, 
            0, 127, 1));

        // Setup main form panel
        JPanel formPanel = createFormPanel();
        
        // Setup captions panel
        JPanel captionsPanel = createCaptionsPanel();

        // Layout
        setLayout(new BorderLayout(5, 5));
        add(formPanel, BorderLayout.NORTH);
        add(captionsPanel, BorderLayout.CENTER);
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        // Add form fields
        addFormField(panel, "Name:", nameField, gbc, 0);
        addFormField(panel, "Code:", codeSpinner, gbc, 1);
        addFormField(panel, "Lower Bound:", lowerBoundSpinner, gbc, 2);
        addFormField(panel, "Upper Bound:", upperBoundSpinner, gbc, 3);

        return panel;
    }

    private void addFormField(JPanel panel, String label, JComponent field, 
                            GridBagConstraints gbc, int row) {
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, gbc);
    }

    private JPanel createCaptionsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Create captions table
        captionsTable = createCaptionsTable();
        
        // Create button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        addCaptionButton = new JButton("Add");
        editCaptionButton = new JButton("Edit");
        deleteCaptionButton = new JButton("Delete");
        
        buttonPanel.add(addCaptionButton);
        buttonPanel.add(editCaptionButton);
        buttonPanel.add(deleteCaptionButton);

        // Add listeners
        setupButtonListeners();
        
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(captionsTable), BorderLayout.CENTER);
        
        return panel;
    }

    private JTable createCaptionsTable() {
        String[] columns = {"Code", "Description"};
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Load existing captions
        if (controlCode.getCaptions() != null) {
            for (ProxyCaption caption : controlCode.getCaptions()) {
                model.addRow(new Object[]{caption.getCode(), caption.getDescription()});
            }
        }

        // Selection listener
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean hasSelection = table.getSelectedRow() >= 0;
                editCaptionButton.setEnabled(hasSelection);
                deleteCaptionButton.setEnabled(hasSelection);
            }
        });

        return table;
    }

    private void setupButtonListeners() {
        addCaptionButton.addActionListener(e -> showCaptionDialog(null));
        editCaptionButton.addActionListener(e -> editSelectedCaption());
        deleteCaptionButton.addActionListener(e -> deleteSelectedCaption());
        
        // Initial button states
        editCaptionButton.setEnabled(false);
        deleteCaptionButton.setEnabled(false);
    }

    private void showCaptionDialog(ProxyCaption caption) {
        boolean isNew = (caption == null);
        if (isNew) {
            caption = new ProxyCaption();
            caption.setCode((Long) codeSpinner.getValue());
        }

        CaptionEditorPanel editorPanel = new CaptionEditorPanel(caption);
        Dialog<ProxyCaption> dialog = new Dialog<>(caption, editorPanel);
        dialog.setTitle(isNew ? "Add Caption" : "Edit Caption");

        if (dialog.showDialog()) {
            ProxyCaption updatedCaption = editorPanel.getUpdatedCaption();
            updateCaptionsTable(updatedCaption, captionsTable.getSelectedRow());
            
            // Notify via ActionBus
            Command action = new Command();
            action.setCommand(isNew ? Commands.CAPTION_ADDED : Commands.CAPTION_UPDATED);
            action.setData(updatedCaption);
            actionBus.publish(action);
        }
    }

    private void editSelectedCaption() {
        int row = captionsTable.getSelectedRow();
        if (row >= 0) {
            ProxyCaption caption = getCaptionFromRow(row);
            showCaptionDialog(caption);
        }
    }

    private void deleteSelectedCaption() {
        int row = captionsTable.getSelectedRow();
        if (row >= 0) {
            ProxyCaption caption = getCaptionFromRow(row);
            ((DefaultTableModel) captionsTable.getModel()).removeRow(row);
            
            Command action = new Command();
            action.setCommand(Commands.CAPTION_DELETED);
            action.setData(caption);
            actionBus.publish(action);
        }
    }

    private ProxyCaption getCaptionFromRow(int row) {
        DefaultTableModel model = (DefaultTableModel) captionsTable.getModel();
        ProxyCaption caption = new ProxyCaption();
        caption.setCode((Long) model.getValueAt(row, 0));
        caption.setDescription((String) model.getValueAt(row, 1));
        return caption;
    }

    private void updateCaptionsTable(ProxyCaption caption, int selectedRow) {
        DefaultTableModel model = (DefaultTableModel) captionsTable.getModel();
        Object[] rowData = new Object[]{caption.getCode(), caption.getDescription()};

        if (selectedRow >= 0) {
            for (int i = 0; i < rowData.length; i++) {
                model.setValueAt(rowData[i], selectedRow, i);
            }
        } else {
            model.addRow(rowData);
        }
    }

    public ProxyControlCode getUpdatedControlCode() {
        controlCode.setName(nameField.getText());
        controlCode.setCode((Integer) codeSpinner.getValue());
        controlCode.setLowerBound((Integer) lowerBoundSpinner.getValue());
        controlCode.setUpperBound((Integer) upperBoundSpinner.getValue());
        
        // Update captions
        DefaultTableModel model = (DefaultTableModel) captionsTable.getModel();
        HashSet<ProxyCaption> captions = new HashSet<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            ProxyCaption caption = new ProxyCaption();
            // Make sure we handle the Long value from caption code correctly
            caption.setCode((Long) model.getValueAt(i, 0));
            caption.setDescription((String) model.getValueAt(i, 1));
            captions.add(caption);
        }
        controlCode.setCaptions(captions);
        
        return controlCode;
    }
}
