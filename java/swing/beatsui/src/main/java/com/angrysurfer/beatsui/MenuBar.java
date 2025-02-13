package com.angrysurfer.beatsui;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.angrysurfer.beatsui.api.Command;
import com.angrysurfer.beatsui.api.CommandBus;
import com.angrysurfer.beatsui.api.Commands;
import com.angrysurfer.beatsui.api.StatusConsumer;

public class MenuBar extends JMenuBar {
    private final JFrame parentFrame;
    private final CommandBus actionBus = CommandBus.getInstance();
    private final ThemeManager themeManager;
    private final StatusConsumer statusConsumer;

    public MenuBar(JFrame parentFrame, StatusConsumer statusConsumer) {
        super();
        this.parentFrame = parentFrame;
        this.statusConsumer = statusConsumer;
        this.themeManager = ThemeManager.getInstance(parentFrame);
        setup();
    }

    private void setup() {
        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        addMenuItem(fileMenu, "New", Commands.NEW_FILE);
        addMenuItem(fileMenu, "Open", Commands.OPEN_FILE);
        addMenuItem(fileMenu, "Save", Commands.SAVE_FILE);
        addMenuItem(fileMenu, "Save As...", Commands.SAVE_AS);
        fileMenu.addSeparator();
        addMenuItem(fileMenu, "Exit", Commands.EXIT, e -> {
            int option = JOptionPane.showConfirmDialog(
                parentFrame,
                "Are you sure you want to exit?",
                "Exit Application",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        
        // Preferences submenu
        JMenu preferencesMenu = new JMenu("Preferences");
        preferencesMenu.add(themeManager.createThemeMenu());

        addMenuItem(editMenu, "Cut", Commands.CUT);
        addMenuItem(editMenu, "Copy", Commands.COPY);
        addMenuItem(editMenu, "Paste", Commands.PASTE);
        editMenu.addSeparator();
        editMenu.add(preferencesMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic(KeyEvent.VK_H);
        helpMenu.add(new JMenuItem("About"));

        add(fileMenu);
        add(editMenu);
        add(helpMenu);
    }

    private void addMenuItem(JMenu menu, String name, String command) {
        addMenuItem(menu, name, command, null);
    }

    private void addMenuItem(JMenu menu, String name, String command, ActionListener extraAction) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> {
            // Update status first
            if (statusConsumer != null) {
                statusConsumer.setStatus("Menu: " + name); // Make the status message more distinct
            }

            // Then handle the action
            Command action = new Command();
            action.setCommand(command);
            action.setSender(this);
            actionBus.publish(action);
            
            if (extraAction != null) {
                extraAction.actionPerformed(e);
            }
        });
        menu.add(item);
    }
}
