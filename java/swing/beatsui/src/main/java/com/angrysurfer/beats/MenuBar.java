package com.angrysurfer.beats;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;

public class MenuBar extends JMenuBar {

    private final JFrame parentFrame;
    private final ThemeManager themeManager;
    private final StatusConsumer statusConsumer;
    private final CommandBus commandBus = CommandBus.getInstance();

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
                    JOptionPane.YES_NO_OPTION);
            if (option == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });

        // Database Menu
        JMenu dbMenu = new JMenu("Database");
        dbMenu.setMnemonic(KeyEvent.VK_D);
        JMenuItem clearDb = new JMenuItem("Clear Database");
        clearDb.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    parentFrame,
                    "Are you sure you want to clear the entire database?\nThis cannot be undone.",
                    "Clear Database",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                commandBus.publish(Commands.CLEAR_DATABASE, this);
            }
        });
        dbMenu.add(clearDb);

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);

        // Preferences submenu
        JMenu preferencesMenu = new JMenu("Preferences");
        preferencesMenu.add(themeManager.createThemeMenu());

        commandBus.register(new CommandListener() {
            final boolean[] visualizationsEnabled = { false };
            JMenu visualizationMenu = new JMenu("Visualization");
            final JMenuItem startVisualizationItem = new JMenuItem("Start Visualization");
            final JMenuItem stopVisualizationItem = new JMenuItem("Stop Visualization");
            final List<CategoryMenuItem> categoryMenus = new ArrayList<>();
            final List<VisualizationMenuItem> defaultItems = new ArrayList<>();

            private void rebuildVisualizationMenu() {
                visualizationMenu.removeAll();
                
                // Add control items first
                visualizationMenu.add(startVisualizationItem);
                visualizationMenu.add(stopVisualizationItem);
                visualizationMenu.addSeparator();
                
                // Add category submenus
                categoryMenus.sort((a, b) -> a.getCategory().getLabel().compareToIgnoreCase(b.getCategory().getLabel()));
                for (CategoryMenuItem categoryMenu : categoryMenus) {
                    if (!categoryMenu.isEmpty()) {
                        visualizationMenu.add(categoryMenu);
                    }
                }
                
                // Add default items directly to main menu
                defaultItems.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                for (VisualizationMenuItem item : defaultItems) {
                    visualizationMenu.add(item);
                }
            }

            private CategoryMenuItem findOrCreateCategoryMenu(VisualizationCategory category) {
                return categoryMenus.stream()
                    .filter(menu -> menu.getCategory() == category)
                    .findFirst()
                    .orElseGet(() -> {
                        CategoryMenuItem newMenu = new CategoryMenuItem(category);
                        categoryMenus.add(newMenu);
                        return newMenu;
                    });
            }

            public void onAction(Command action) {
                if (action.getCommand() == null) return;

                switch (action.getCommand()) {
                    case Commands.VISUALIZATION_REGISTERED:
                        if (!visualizationsEnabled[0]) {
                            visualizationsEnabled[0] = true;
                            preferencesMenu.addSeparator();
                            
                            addMenuItem(visualizationMenu, startVisualizationItem, Commands.START_VISUALIZATION, null, null);
                            addMenuItem(visualizationMenu, stopVisualizationItem, Commands.STOP_VISUALIZATION, null, null);
                            preferencesMenu.add(visualizationMenu);

                            startVisualizationItem.setVisible(true);
                            stopVisualizationItem.setVisible(false);
                        }

                        IVisualizationHandler handler = (IVisualizationHandler) action.getData();
                        VisualizationMenuItem newItem = new VisualizationMenuItem(handler.getName());
                        
                        if (handler.getVisualizationCategory() == VisualizationCategory.DEFAULT) {
                            addMenuItem(visualizationMenu, newItem, Commands.VISUALIZATION_SELECTED, handler, null);
                            defaultItems.add(newItem);
                        } else {
                            CategoryMenuItem categoryMenu = findOrCreateCategoryMenu(handler.getVisualizationCategory());
                            addMenuItem(categoryMenu, newItem, Commands.VISUALIZATION_SELECTED, handler, null);
                        }
                        
                        // Rebuild menu with sorted items
                        rebuildVisualizationMenu();
                        break;

                    case Commands.VISUALIZATION_STARTED:
                        startVisualizationItem.setVisible(false);
                        stopVisualizationItem.setVisible(visualizationsEnabled[0]);
                        break;

                    case Commands.VISUALIZATION_STOPPED:
                        startVisualizationItem.setVisible(visualizationsEnabled[0]);
                        stopVisualizationItem.setVisible(false);
                        break;
                }
            }
        });

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
        add(dbMenu); // Add Database menu between Edit and Help
        add(helpMenu);
    }

    private void addMenuItem(JMenu menu, String name, String command) {
        addMenuItem(menu, name, command, null);
    }

    private void addMenuItem(JMenu menu, JMenuItem item, String command, Object data, ActionListener extraAction) {
        item.addActionListener(e -> {
            if (statusConsumer != null) {
                statusConsumer.setStatus("Menu: " + item.getName());
            }
            commandBus.publish(command, this, data);
            if (extraAction != null) {
                extraAction.actionPerformed(e);
            }
        });
        menu.add(item);
    }

    private void addMenuItem(JMenu menu, String name, String command, ActionListener extraAction) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> {
            if (statusConsumer != null) {
                statusConsumer.setStatus("Menu: " + name);
            }
            commandBus.publish(command, this);
            if (extraAction != null) {
                extraAction.actionPerformed(e);
            }
        });
        menu.add(item);
    }

    private static class CategoryMenuItem extends JMenu {
        private final VisualizationCategory category;
        
        public CategoryMenuItem(VisualizationCategory category) {
            super(category.getLabel());
            this.category = category;
        }
        
        public VisualizationCategory getCategory() {
            return category;
        }
        
        public boolean isEmpty() {
            return getItemCount() == 0;
        }
    }

    private static class VisualizationMenuItem extends JMenuItem {
        private final String sortName;
        
        public VisualizationMenuItem(String name) {
            super(name);
            this.sortName = name.toLowerCase();
        }
        
        public String getName() {
            return sortName;
        }
    }
}
