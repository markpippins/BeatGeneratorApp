package com.angrysurfer.beats.ui;

import javax.swing.*;

import com.angrysurfer.beats.api.Command;
import com.angrysurfer.beats.api.CommandBus;
import com.angrysurfer.beats.api.Commands;
import com.formdev.flatlaf.*;

public class ThemeManager {
    private static ThemeManager instance;
    private final CommandBus actionBus = CommandBus.getInstance();
    private final JFrame mainFrame;

    private ThemeManager(JFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public static ThemeManager getInstance(JFrame mainFrame) {
        if (instance == null) {
            instance = new ThemeManager(mainFrame);
        }
        return instance;
    }

    public JMenu createThemeMenu() {
        JMenu themeMenu = new JMenu("Theme");
        
        // Platform Themes
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
        
        return themeMenu;
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
            SwingUtilities.updateComponentTreeUI(mainFrame);
            notifyThemeChange(className);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void setTheme(ThemeSupplier supplier) {
        try {
            LookAndFeel laf = supplier.get();
            UIManager.setLookAndFeel(laf);
            SwingUtilities.updateComponentTreeUI(mainFrame);
            notifyThemeChange(laf.getClass().getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void notifyThemeChange(String themeName) {
        Command action = new Command();
        action.setCommand(Commands.CHANGE_THEME);
        action.setSender(this);
        action.setData(themeName);
        actionBus.publish(action);
    }

    @FunctionalInterface
    private interface ThemeSupplier {
        LookAndFeel get();
    }
}
