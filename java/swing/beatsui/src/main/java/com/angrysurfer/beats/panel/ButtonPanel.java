package com.angrysurfer.beats.panel;

import javax.swing.*;
import java.awt.*;

public class ButtonPanel extends JPanel {
    private final JButton addButton;
    private final JButton editButton;
    private final JButton deleteButton;

    public ButtonPanel(String addCommand, String editCommand, String deleteCommand) {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        
        addButton.setEnabled(false);
        editButton.setEnabled(false);
        deleteButton.setEnabled(false);
        
        // Set action commands
        addButton.setActionCommand(addCommand);
        editButton.setActionCommand(editCommand);
        deleteButton.setActionCommand(deleteCommand);
        
        add(addButton);
        add(editButton);
        add(deleteButton);
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
