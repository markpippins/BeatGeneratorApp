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

import com.angrysurfer.beats.service.ThemeManager;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
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
        editMenu.setEnabled(false);
        // editMenu.addSeparator();

        // options menu
        JMenu optionsMenu = new JMenu("Options");

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

        JMenuItem clearInvalidSessions = new JMenuItem("Clear Invalid Sessions");
        clearInvalidSessions.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    parentFrame,
                    "Are you sure you want to remove all invalid sessions?",
                    "Clear Invalid Sessions",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                commandBus.publish(Commands.CLEAR_INVALID_SESSIONS, this);
            }
        });

        JMenu dbMenu = new JMenu("Database");
        dbMenu.setMnemonic(KeyEvent.VK_D);
        dbMenu.add(clearDb);
        dbMenu.add(clearInvalidSessions); // Add the new menu item

        JMenuItem loadConfig = new JMenuItem("Load Configuration...");
        loadConfig.addActionListener(e -> {
            commandBus.publish(Commands.LOAD_CONFIG, this);
        });
        dbMenu.add(loadConfig);
        optionsMenu.add(dbMenu);

        // Add Load Config
        JMenuItem saveConfig = new JMenuItem("Save Configuration");
        saveConfig.addActionListener(e -> {
            commandBus.publish(Commands.SAVE_CONFIG, this);
        });
        dbMenu.add(saveConfig);
        optionsMenu.add(dbMenu);

        // Add Theme menu
        optionsMenu.add(themeManager.createThemeMenu());

        // Register visualization listener
        commandBus.register(new IBusListener() {
            final boolean[] visualizationsEnabled = { false };
            JMenu visualizationMenu = new JMenu("Visualization");
            final JMenuItem startVisualizationItem = new JMenuItem("Start Visualization");
            final JMenuItem stopVisualizationItem = new JMenuItem("Stop Visualization");
            final JMenuItem lockVisualizationItem = new JMenuItem("Lock Current Visualization");
            final JMenuItem unlockVisualizationItem = new JMenuItem("Unlock Visualization");  // Add this
            final JMenuItem refreshVisualizationItem = new JMenuItem("Refresh");
            final List<CategoryMenuItem> categoryMenus = new ArrayList<>();
            final List<VisualizationMenuItem> defaultItems = new ArrayList<>();

            private void rebuildVisualizationMenu() {
                visualizationMenu.removeAll();

                // Add control items at the top
                visualizationMenu.add(startVisualizationItem);
                visualizationMenu.add(stopVisualizationItem);
                visualizationMenu.add(lockVisualizationItem);  // Add this line
                visualizationMenu.add(unlockVisualizationItem);  // Add unlock item
                visualizationMenu.add(refreshVisualizationItem);
                visualizationMenu.addSeparator();

                // Sort category menus alphabetically by label
                categoryMenus
                        .sort((a, b) -> a.getCategory().getLabel().compareToIgnoreCase(b.getCategory().getLabel()));

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
                                ((VisualizationMenuItem) item).getHandler().getName().equals(handler.getName())) {
                            categoryMenu.remove(item);
                            break;
                        }
                    }
                }
            }

            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                switch (action.getCommand()) {
                    case Commands.VISUALIZATION_REGISTERED:
                        if (!visualizationsEnabled[0]) {
                            visualizationsEnabled[0] = true;

                            visualizationMenu.add(startVisualizationItem);
                            visualizationMenu.add(stopVisualizationItem);
                            visualizationMenu.add(lockVisualizationItem);
                            visualizationMenu.add(unlockVisualizationItem);  // Add unlock item
                            visualizationMenu.add(refreshVisualizationItem);
                            visualizationMenu.addSeparator();
                            
                            // Add action listeners
                            addMenuItem(visualizationMenu, startVisualizationItem, Commands.START_VISUALIZATION, null, null);
                            addMenuItem(visualizationMenu, stopVisualizationItem, Commands.STOP_VISUALIZATION, null, null);
                            addMenuItem(visualizationMenu, lockVisualizationItem, Commands.LOCK_CURRENT_VISUALIZATION, null, null);
                            addMenuItem(visualizationMenu, unlockVisualizationItem, Commands.UNLOCK_CURRENT_VISUALIZATION, null, null);
                            addMenuItem(visualizationMenu, refreshVisualizationItem, Commands.VISUALIZATION_HANDLER_REFRESH_REQUESTED, null, null);

                            optionsMenu.add(visualizationMenu);

                            // Set initial states
                            startVisualizationItem.setVisible(true);
                            stopVisualizationItem.setVisible(false);
                            lockVisualizationItem.setVisible(true);  // Make visible but disabled
                            lockVisualizationItem.setEnabled(false);
                            unlockVisualizationItem.setVisible(false);  // Initially hidden
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
                            CategoryMenuItem categoryMenu = findOrCreateCategoryMenu(
                                    handler.getVisualizationCategory());
                            addMenuItem(categoryMenu, newItem, Commands.VISUALIZATION_SELECTED, handler, null);
                        }

                        // Rebuild menu with sorted items
                        rebuildVisualizationMenu();
                        break;

                    case Commands.VISUALIZATION_STARTED:
                        startVisualizationItem.setVisible(false);
                        stopVisualizationItem.setVisible(true);
                        lockVisualizationItem.setVisible(true);  // Ensure visible
                        lockVisualizationItem.setEnabled(true);  // Enable when visualization starts
                        unlockVisualizationItem.setVisible(false);
                        break;

                    case Commands.LOCK_CURRENT_VISUALIZATION:
                        // The lock command was sent - follow up with the locked event
                        commandBus.publish(Commands.VISUALIZATION_LOCKED, this);
                        break;

                    case Commands.VISUALIZATION_LOCKED:
                        // Handle the locked event by updating menu items
                        lockVisualizationItem.setVisible(false);
                        unlockVisualizationItem.setVisible(true);
                        unlockVisualizationItem.setEnabled(true);
                        break;

                    case Commands.UNLOCK_CURRENT_VISUALIZATION:
                        // The unlock command was sent - follow up with the unlocked event
                        commandBus.publish(Commands.VISUALIZATION_UNLOCKED, this);
                        break;

                    case Commands.VISUALIZATION_UNLOCKED:
                        // Handle the unlocked event by updating menu items
                        unlockVisualizationItem.setVisible(false);
                        lockVisualizationItem.setVisible(true);
                        lockVisualizationItem.setEnabled(true);
                        break;

                    case Commands.VISUALIZATION_STOPPED:
                        startVisualizationItem.setVisible(true);
                        stopVisualizationItem.setVisible(false);
                        lockVisualizationItem.setEnabled(false);
                        unlockVisualizationItem.setVisible(false);
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
        add(optionsMenu);
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
