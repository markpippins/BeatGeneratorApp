package com.angrysurfer.beats;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatPropertiesLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf; 
import com.formdev.flatlaf.themes.FlatMacLightLaf; 
import com.formdev.flatlaf.themes.FlatMacDarkLaf; 
// import com.formdev.flatlaf.themes.FlatArcOrangeIJTheme;

public class ThemeManager {
    private static ThemeManager instance;
    private final CommandBus commandBus = CommandBus.getInstance();
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
        addThemeItem(flatThemes, "Mac Dark", () -> new FlatMacDarkLaf());
        addThemeItem(flatThemes, "Mac Light", () -> new FlatMacLightLaf());

        // com.formdev.flatlaf.themes.
        // com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme.setup();
        // FlatArcOrangeIJTheme.setup();

        
        

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
        commandBus.publish(Commands.CHANGE_THEME, this, themeName);
    }

    @FunctionalInterface
    private interface ThemeSupplier {
        LookAndFeel get();
    }
}
