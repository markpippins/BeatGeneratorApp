package com.angrysurfer.beatsui;

import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.beatsui.api.Action;
import com.angrysurfer.beatsui.api.ActionBus;
import com.angrysurfer.beatsui.api.Commands;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;

public class MenuBar extends JMenuBar {
    private final JFrame parentFrame;
    private final ActionBus actionBus = ActionBus.getInstance();
    private final ThemeManager themeManager;

    public MenuBar(JFrame parentFrame) {
        super();
        this.parentFrame = parentFrame;
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
        addMenuItem(fileMenu, "Exit", Commands.EXIT);

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
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> {
            Action action = new Action();
            action.setCommand(command);
            action.setSender(this);
            actionBus.publish(action);
        });
        menu.add(item);
    }
}
