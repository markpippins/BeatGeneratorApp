package com.angrysurfer.beatsui;

import java.awt.event.KeyEvent;
import javax.swing.*;

import com.formdev.flatlaf.*;

public class MenuBar extends JMenuBar {
    private final JFrame parentFrame;

    public MenuBar(JFrame parentFrame) {
        super();
        this.parentFrame = parentFrame;
        setup();
    }

    private void setup() {
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
        
        // Preferences submenu
        JMenu preferencesMenu = new JMenu("Preferences");
        JMenu themeMenu = new JMenu("Theme");
        
        // Platform Themes - dynamically populate
        JMenu platformThemes = new JMenu("Platform");
        UIManager.LookAndFeelInfo[] looks = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo look : looks) {
            addThemeItem(platformThemes, look.getName(), look.getClassName());
        }
        
        // FlatLaf Themes
        JMenu flatThemes = new JMenu("FlatLaf");
        addThemeItem(flatThemes, "Dark", () -> new FlatDarkLaf());
        addThemeItem(flatThemes, "Light", () -> new FlatLightLaf());
        addThemeItem(flatThemes, "Darcula", () -> new FlatDarculaLaf());
        addThemeItem(flatThemes, "IntelliJ", () -> new FlatIntelliJLaf());

        themeMenu.add(platformThemes);
        themeMenu.add(flatThemes);
        preferencesMenu.add(themeMenu);
        
        editMenu.add(new JMenuItem("Cut"));
        editMenu.add(new JMenuItem("Copy"));
        editMenu.add(new JMenuItem("Paste"));
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

    private void addThemeItem(JMenu menu, String name, String className) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> setTheme(className));
        menu.add(item);
    }

    private void addThemeItem(JMenu menu, String name, ThemeSupplier supplier) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> setTheme(supplier));
        menu.add(item);
    }

    private void setTheme(String className) {
        try {
            UIManager.setLookAndFeel(className);
            SwingUtilities.updateComponentTreeUI(parentFrame);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setTheme(ThemeSupplier supplier) {
        try {
            UIManager.setLookAndFeel(supplier.get());
            SwingUtilities.updateComponentTreeUI(parentFrame);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface ThemeSupplier {
        LookAndFeel get();
    }
}
