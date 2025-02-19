package com.angrysurfer.beats;

import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
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

        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        editMenu.setMnemonic(KeyEvent.VK_E);
        addMenuItem(editMenu, "Cut", Commands.CUT);
        addMenuItem(editMenu, "Copy", Commands.COPY);
        addMenuItem(editMenu, "Paste", Commands.PASTE);
        editMenu.addSeparator();

        // Preferences submenu
        JMenu preferencesMenu = new JMenu("Preferences");
        
        // Add Theme menu
        preferencesMenu.add(themeManager.createThemeMenu());
        
        // Add Database menu
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
        JMenu dbMenu = new JMenu("Database");
        dbMenu.setMnemonic(KeyEvent.VK_D);
        dbMenu.add(clearDb);
        dbMenu.addSeparator();
        
        // Add Load Instruments item
        JMenuItem loadInstruments = new JMenuItem("Load Instruments from File");
        loadInstruments.addActionListener(e -> {
            commandBus.publish(Commands.LOAD_INSTRUMENTS_FROM_FILE, this);
        });
        dbMenu.add(loadInstruments);
        preferencesMenu.add(dbMenu);

        editMenu.add(preferencesMenu);

        // Register visualization listener
        commandBus.register(new CommandListener() {
            final boolean[] visualizationsEnabled = { false };
            JMenu visualizationMenu = new JMenu("Visualization");
            final JMenuItem startVisualizationItem = new JMenuItem("Start Visualization");
            final JMenuItem stopVisualizationItem = new JMenuItem("Stop Visualization");
            final JMenuItem refreshVisualizationItem = new JMenuItem("Refresh");
            final List<CategoryMenuItem> categoryMenus = new ArrayList<>();
            final List<VisualizationMenuItem> defaultItems = new ArrayList<>();

            private void rebuildVisualizationMenu() {
                visualizationMenu.removeAll();
                
                // Add control items at the top
                visualizationMenu.add(startVisualizationItem);
                visualizationMenu.add(stopVisualizationItem);
                visualizationMenu.add(refreshVisualizationItem);
                visualizationMenu.addSeparator();
                
                // Sort category menus alphabetically by label
                categoryMenus.sort((a, b) -> a.getCategory().getLabel().compareToIgnoreCase(b.getCategory().getLabel()));
                
                // Process each category menu
                for (CategoryMenuItem categoryMenu : categoryMenus) {
                    if (!categoryMenu.isEmpty()) {
                        // Get all visualization items from this category
                        List<VisualizationMenuItem> categoryItems = new ArrayList<>();
                        for (int i = 0; i < categoryMenu.getItemCount(); i++) {
                            if (categoryMenu.getItem(i) instanceof VisualizationMenuItem) {
                                categoryItems.add((VisualizationMenuItem) categoryMenu.getItem(i));
                            }
                        }
                        
                        // Sort items within category alphabetically
                        categoryItems.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                        
                        // Clear and rebuild category menu with sorted items
                        categoryMenu.removeAll();
                        categoryItems.forEach(categoryMenu::add);
                        
                        // Add sorted category to main menu
                        visualizationMenu.add(categoryMenu);
                    }
                }
                
                // Sort and add default items
                defaultItems.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                defaultItems.forEach(visualizationMenu::add);
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

            private void removeExistingHandler(IVisualizationHandler handler) {
                // Remove from default items if present
                defaultItems.removeIf(item -> item.getHandler().getName().equals(handler.getName()));
                
                // Remove from category menus if present
                for (CategoryMenuItem categoryMenu : categoryMenus) {
                    for (int i = 0; i < categoryMenu.getItemCount(); i++) {
                        JMenuItem item = categoryMenu.getItem(i);
                        if (item instanceof VisualizationMenuItem && 
                            ((VisualizationMenuItem)item).getHandler().getName().equals(handler.getName())) {
                            categoryMenu.remove(item);
                            break;
                        }
                    }
                }
            }

            public void onAction(Command action) {
                if (action.getCommand() == null) return;

                switch (action.getCommand()) {
                    case Commands.VISUALIZATION_REGISTERED:
                        if (!visualizationsEnabled[0]) {
                            visualizationsEnabled[0] = true;
                            addMenuItem(visualizationMenu, startVisualizationItem, Commands.START_VISUALIZATION, null, null);
                            addMenuItem(visualizationMenu, stopVisualizationItem, Commands.STOP_VISUALIZATION, null, null);
                            addMenuItem(visualizationMenu, refreshVisualizationItem, 
                                    Commands.VISUALIZATION_HANDLER_REFRESH_REQUESTED, null, null);
                            preferencesMenu.add(visualizationMenu);  // No separator added

                            startVisualizationItem.setVisible(true);
                            stopVisualizationItem.setVisible(false);
                            refreshVisualizationItem.setVisible(true);
                        }

                        IVisualizationHandler handler = (IVisualizationHandler) action.getData();
                        
                        // Remove existing handler if present
                        removeExistingHandler(handler);
                        
                        // Create new menu item
                        VisualizationMenuItem newItem = new VisualizationMenuItem(handler.getName(), handler);
                        
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
        private final IVisualizationHandler handler;
        
        public VisualizationMenuItem(String name, IVisualizationHandler handler) {
            super(name);
            this.sortName = name.toLowerCase();
            this.handler = handler;
        }
        
        public String getName() {
            return sortName;
        }
        
        public IVisualizationHandler getHandler() {
            return handler;
        }
    }
}
