package com.angrysurfer.beats.panel;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ContextMenuHelper {
    private final JPopupMenu popupMenu;
    private final JMenuItem addMenuItem;
    private final JMenuItem editMenuItem;
    private final JMenuItem deleteMenuItem;

    public ContextMenuHelper(String addCommand, String editCommand, String deleteCommand) {
        popupMenu = new JPopupMenu();
        
        addMenuItem = new JMenuItem("Add");
        editMenuItem = new JMenuItem("Edit");
        deleteMenuItem = new JMenuItem("Delete");
        
        addMenuItem.setEnabled(true);
        editMenuItem.setEnabled(false);
        deleteMenuItem.setEnabled(false);
        
        addMenuItem.setActionCommand(addCommand);
        editMenuItem.setActionCommand(editCommand);
        deleteMenuItem.setActionCommand(deleteCommand);
        
        popupMenu.add(addMenuItem);
        popupMenu.add(editMenuItem);
        popupMenu.add(deleteMenuItem);
    }

    public void addActionListener(ActionListener listener) {
        addMenuItem.addActionListener(listener);
        editMenuItem.addActionListener(listener);
        deleteMenuItem.addActionListener(listener);
    }

    public void setAddEnabled(boolean enabled) {
        addMenuItem.setEnabled(enabled);
    }

    public void setEditEnabled(boolean enabled) {
        editMenuItem.setEnabled(enabled);
    }

    public void setDeleteEnabled(boolean enabled) {
        deleteMenuItem.setEnabled(enabled);
    }

    public void install(JComponent component) {
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    showMenu(e);
                }
            }
            
            private void showMenu(MouseEvent e) {
                popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });
    }

    public void addSeparator() {
        popupMenu.addSeparator();
    }

    public void addMenuItem(String text, ActionListener listener) {
        JMenuItem menuItem = new JMenuItem(text);
        menuItem.addActionListener(listener);
        popupMenu.add(menuItem);
    }
}
