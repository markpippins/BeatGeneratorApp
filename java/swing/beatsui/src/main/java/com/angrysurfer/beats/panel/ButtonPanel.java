package com.angrysurfer.beats.panel;

import javax.swing.*;
import java.awt.*;

public class ButtonPanel extends JPanel {
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;

    public ButtonPanel(String addCommand, String editCommand, String deleteCommand) {
        // Change from FlowLayout to BorderLayout + FlowLayout combination
        setLayout(new BorderLayout());
        
        // Create inner panel with FlowLayout.CENTER
        JPanel innerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        
        // Create buttons
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");

        addButton.setActionCommand(addCommand);
        editButton.setActionCommand(editCommand);
        deleteButton.setActionCommand(deleteCommand);

        // Add buttons to inner panel
        innerPanel.add(addButton);
        innerPanel.add(editButton);
        innerPanel.add(deleteButton);

        // Add inner panel to center of ButtonPanel
        add(innerPanel, BorderLayout.CENTER);
    }

    public void addActionListener(java.awt.event.ActionListener listener) {
        addButton.addActionListener(listener);
        editButton.addActionListener(listener);
        deleteButton.addActionListener(listener);
    }

    public void setAddEnabled(boolean enabled) {
        addButton.setEnabled(enabled);
    }

    public void setEditEnabled(boolean enabled) {
        editButton.setEnabled(enabled);
    }

    public void setDeleteEnabled(boolean enabled) {
        deleteButton.setEnabled(enabled);
    }
}
