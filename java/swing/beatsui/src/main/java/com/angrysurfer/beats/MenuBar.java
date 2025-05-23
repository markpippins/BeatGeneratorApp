package com.angrysurfer.beats;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.beats.diagnostic.DiagnosticsManager;
import com.angrysurfer.beats.diagnostic.DiagnosticsSplashScreen;
import com.angrysurfer.beats.diagnostic.suite.RedisServiceDiagnostics;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class MenuBar extends JMenuBar {

    private final JFrame parentFrame;
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
                CommandBus.getInstance().publish(Commands.CLEAR_DATABASE, this);
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
                CommandBus.getInstance().publish(Commands.CLEAR_INVALID_SESSIONS, this);
            }
        });

        JMenuItem deleteUnusedInstruments = new JMenuItem("Delete Unused Instruments");
        deleteUnusedInstruments.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    parentFrame,
                    "Are you sure you want to delete all instruments that aren't in use?\n" +
                            "This action will permanently remove all instruments with no owners.",
                    "Delete Unused Instruments",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                CommandBus.getInstance().publish(Commands.DELETE_UNUSED_INSTRUMENTS, this);
            }
        });

        JMenu dbMenu = new JMenu("Database");
        dbMenu.setMnemonic(KeyEvent.VK_D);
        dbMenu.add(clearDb);
        dbMenu.add(clearInvalidSessions);
        dbMenu.add(deleteUnusedInstruments);

        JMenuItem loadConfig = new JMenuItem("Load Configuration...");
        loadConfig.addActionListener(e -> {
            CommandBus.getInstance().publish(Commands.LOAD_CONFIG, this);
        });
        dbMenu.add(loadConfig);
        optionsMenu.add(dbMenu);

        // Add Load Config
        JMenuItem saveConfig = new JMenuItem("Save Configuration");
        saveConfig.addActionListener(e -> {
            CommandBus.getInstance().publish(Commands.SAVE_CONFIG, this);
        });
        dbMenu.add(saveConfig);
        optionsMenu.add(dbMenu);

        // Add Theme menu
        optionsMenu.add(themeManager.createThemeMenu());

        // Add 000000000000s menu
        JMenu diagnosticsMenu = new JMenu("Diagnostics");
        diagnosticsMenu.setMnemonic(KeyEvent.VK_D);

        // Initialize DiagnosticsManager
        DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(parentFrame);

        // All diagnostics
        JMenuItem allDiagnostics = new JMenuItem("Run All Diagnostics");
        allDiagnostics.addActionListener(e -> diagnosticsManager.runAllDiagnostics());
        diagnosticsMenu.add(allDiagnostics);

        diagnosticsMenu.addSeparator();

        // Redis diagnostics
        JMenuItem redisDiagnostics = new JMenuItem("Redis Diagnostics");
        redisDiagnostics.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Redis Diagnostics", "Running Redis tests...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = RedisServiceDiagnostics.runAllRedisDiagnostics();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Redis Diagnostics",
                            "Error running Redis diagnostics: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(redisDiagnostics);

        // DrumSequencer diagnostics
        JMenuItem drumSequencerDiagnostics = new JMenuItem("Drum Sequencer Diagnostics");
        drumSequencerDiagnostics.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Drum Sequencer Diagnostics", "Analyzing sequencer...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testDrumSequencer();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Drum Sequencer Diagnostics",
                            "Error diagnosing sequencer: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(drumSequencerDiagnostics);

        // MelodicSequencer diagnostics
        JMenuItem melodicSequencerDiagnostics = new JMenuItem("Melodic Sequencer Diagnostics");
        melodicSequencerDiagnostics.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Melodic Sequencer Diagnostics", "Analyzing sequencer...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testMelodicSequencer();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Melodic Sequencer Diagnostics",
                            "Error diagnosing sequencer: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(melodicSequencerDiagnostics);

        // Melodic pattern operations test
        JMenuItem melodicPatternTest = new JMenuItem("Melodic Pattern Operations Test");
        melodicPatternTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Melodic Pattern Test", "Testing pattern operations...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testMelodicPatternOperations();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Melodic Pattern Operations Test",
                            "Error testing pattern operations: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(melodicPatternTest);

        // MIDI Connection Test
        JMenuItem midiConnectionTest = new JMenuItem("Test MIDI Connections");
        midiConnectionTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("MIDI Connection Test", "Scanning MIDI devices...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testMidiConnections();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("MIDI Connection Test",
                            "Error testing MIDI connections: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(midiConnectionTest);

        // Sound Test
        JMenuItem soundTest = new JMenuItem("MIDI Sound Test");
        soundTest.addActionListener(e -> {
            try {
                DiagnosticLogBuilder log = diagnosticsManager.testMidiSound();
                diagnosticsManager.showDiagnosticLogDialog(log);
            } catch (Exception ex) {
                DiagnosticsManager.showError("Sound Test",
                        "Error running sound test: " + ex.getMessage());
            }
        });
        diagnosticsMenu.add(soundTest);

        // Player/Instrument Test
        JMenuItem playerInstrumentTest = new JMenuItem("Player/Instrument Integrity Test");
        playerInstrumentTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Player/Instrument Test", "Analyzing database relationships...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testPlayerInstrumentIntegrity();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Player/Instrument Test",
                            "Error testing player/instrument integrity: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(playerInstrumentTest);

        // Add channel manager diagnostics
        JMenuItem channelManagerTest = new JMenuItem("Channel Manager Test");
        channelManagerTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Channel Manager Test", "Testing channel allocation...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testChannelManager();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Channel Manager Test",
                            "Error testing channel manager: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(channelManagerTest);

        // Add device manager diagnostics
        JMenuItem deviceManagerTest = new JMenuItem("Device Manager Test");
        deviceManagerTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Device Manager Test", "Testing MIDI devices...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testDeviceManager();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Device Manager Test",
                            "Error testing device manager: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(deviceManagerTest);

        // Add receiver manager diagnostics
        JMenuItem receiverManagerTest = new JMenuItem("Receiver Manager Test");
        receiverManagerTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Receiver Manager Test", "Testing MIDI receivers...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testReceiverManager();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Receiver Manager Test",
                            "Error testing receiver manager: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(receiverManagerTest);

        // Add receiver reliability test
        JMenuItem receiverReliabilityTest = new JMenuItem("Receiver Reliability Test");
        receiverReliabilityTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Receiver Reliability", "Testing MIDI message throughput...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testReceiverReliability();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Receiver Reliability Test",
                            "Error testing receiver reliability: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(receiverReliabilityTest);

        diagnosticsMenu.addSeparator();

        // Add player manager diagnostics
        JMenuItem playerManagerTest = new JMenuItem("Player Manager Test");
        playerManagerTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Player Manager Test", "Analyzing player database...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testPlayerManager();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Player Manager Test",
                            "Error testing player manager: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(playerManagerTest);

        // Add session manager diagnostics
        JMenuItem sessionManagerTest = new JMenuItem("Session Manager Test");
        sessionManagerTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Session Manager Test", "Analyzing session database...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testSessionManager();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Session Manager Test",
                            "Error testing session manager: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(sessionManagerTest);

        // Add session persistence diagnostics
        JMenuItem sessionPersistenceTest = new JMenuItem("Session Persistence Test");
        sessionPersistenceTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Session Persistence Test", "Testing session save/load...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testSessionPersistence();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Session Persistence Test",
                            "Error testing session persistence: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(sessionPersistenceTest);

        // Add user config manager diagnostics
        JMenuItem userConfigManagerTest = new JMenuItem("User Config Manager Test");
        userConfigManagerTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("User Config Manager Test", "Testing configuration manager...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testUserConfigManager();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("User Config Manager Test",
                            "Error testing user config manager: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(userConfigManagerTest);

        // Add melodic sequencer manager diagnostics
        JMenuItem melodicSequencerManagerTest = new JMenuItem("Melodic Sequencer Manager Test");
        melodicSequencerManagerTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Melodic Sequencer Manager Test", "Testing melodic sequencer database...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testMelodicSequencerManager();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Melodic Sequencer Manager Test",
                            "Error testing melodic sequencer manager: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(melodicSequencerManagerTest);

        // Add melodic sequence persistence diagnostics
        JMenuItem melodicSequencePersistenceTest = new JMenuItem("Melodic Sequence Persistence Test");
        melodicSequencePersistenceTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Melodic Sequence Persistence Test", "Testing sequence save/load...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.testMelodicSequencePersistence();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Melodic Sequence Persistence Test",
                            "Error testing melodic sequence persistence: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(melodicSequencePersistenceTest);

        // Add config transaction diagnostics
        JMenuItem configTransactionTest = new JMenuItem("Config Transaction Test");
        configTransactionTest.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Config Transaction Test", "Testing configuration transactions...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    // DiagnosticLogBuilder log = diagnosticsManager.testConfigTransactions();
                    splash.setVisible(false);
                    // diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("Config Transaction Test",
                            "Error testing config transactions: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(configTransactionTest);

        // Add MIDI repair utility
        JMenuItem repairMidiItem = new JMenuItem("Repair MIDI Connections");
        repairMidiItem.addActionListener(e -> {
            // Create splash screen
            DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("MIDI Repair Utility", "Repairing MIDI connections...");
            splash.setVisible(true);

            // Run in background thread
            new Thread(() -> {
                try {
                    DiagnosticLogBuilder log = diagnosticsManager.repairMidiConnections();
                    splash.setVisible(false);
                    diagnosticsManager.showDiagnosticLogDialog(log);
                } catch (Exception ex) {
                    splash.setVisible(false);
                    DiagnosticsManager.showError("MIDI Repair",
                            "Error repairing MIDI connections: " + ex.getMessage());
                }
            }).start();
        });
        diagnosticsMenu.add(repairMidiItem);

        // Add the diagnostics menu to the menu bar
        optionsMenu.add(diagnosticsMenu);

        // Register visualization listener
        CommandBus.getInstance().register(new IBusListener() {
            final boolean[] visualizationsEnabled = {false};
            final JMenuItem startVisualizationItem = new JMenuItem("Start Visualization");
            final JMenuItem stopVisualizationItem = new JMenuItem("Stop Visualization");
            final JMenuItem lockVisualizationItem = new JMenuItem("Lock Current Visualization");
            final JMenuItem unlockVisualizationItem = new JMenuItem("Unlock Visualization"); // Add this
            final JMenuItem refreshVisualizationItem = new JMenuItem("Refresh");
            final List<CategoryMenuItem> categoryMenus = new ArrayList<>();
            final List<VisualizationMenuItem> defaultItems = new ArrayList<>();
            final JMenu visualizationMenu = new JMenu("Visualization");

            private void rebuildVisualizationMenu() {
                visualizationMenu.removeAll();

                // Add control items at the top
                visualizationMenu.add(startVisualizationItem);
                visualizationMenu.add(stopVisualizationItem);
                visualizationMenu.add(lockVisualizationItem); // Add this line
                visualizationMenu.add(unlockVisualizationItem); // Add unlock item
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
                            visualizationMenu.add(unlockVisualizationItem); // Add unlock item
                            visualizationMenu.add(refreshVisualizationItem);
                            visualizationMenu.addSeparator();

                            // Add action listeners
                            addMenuItem(visualizationMenu, startVisualizationItem, Commands.START_VISUALIZATION, null,
                                    null);
                            addMenuItem(visualizationMenu, stopVisualizationItem, Commands.STOP_VISUALIZATION, null,
                                    null);
                            addMenuItem(visualizationMenu, lockVisualizationItem, Commands.LOCK_CURRENT_VISUALIZATION,
                                    null, null);
                            addMenuItem(visualizationMenu, unlockVisualizationItem,
                                    Commands.UNLOCK_CURRENT_VISUALIZATION, null, null);
                            addMenuItem(visualizationMenu, refreshVisualizationItem,
                                    Commands.VISUALIZATION_HANDLER_REFRESH_REQUESTED, null, null);

                            optionsMenu.add(visualizationMenu);

                            // Set initial states
                            startVisualizationItem.setVisible(true);
                            stopVisualizationItem.setVisible(false);
                            lockVisualizationItem.setVisible(true); // Make visible but disabled
                            lockVisualizationItem.setEnabled(false);
                            unlockVisualizationItem.setVisible(false); // Initially hidden
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
                        lockVisualizationItem.setVisible(true); // Ensure visible
                        lockVisualizationItem.setEnabled(true); // Enable when visualization starts
                        unlockVisualizationItem.setVisible(false);
                        break;

                    case Commands.LOCK_CURRENT_VISUALIZATION:
                        // The lock command was sent - follow up with the locked event
                        CommandBus.getInstance().publish(Commands.VISUALIZATION_LOCKED, this);
                        break;

                    case Commands.VISUALIZATION_LOCKED:
                        // Handle the locked event by updating menu items
                        lockVisualizationItem.setVisible(false);
                        unlockVisualizationItem.setVisible(true);
                        unlockVisualizationItem.setEnabled(true);
                        break;

                    case Commands.UNLOCK_CURRENT_VISUALIZATION:
                        // The unlock command was sent - follow up with the unlocked event
                        CommandBus.getInstance().publish(Commands.VISUALIZATION_UNLOCKED, this);
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
        }, new String[]{
                Commands.VISUALIZATION_REGISTERED,
                Commands.VISUALIZATION_STARTED,
                Commands.LOCK_CURRENT_VISUALIZATION,
                Commands.VISUALIZATION_LOCKED,
                Commands.UNLOCK_CURRENT_VISUALIZATION,
                Commands.VISUALIZATION_UNLOCKED,
                Commands.VISUALIZATION_STOPPED
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

    public void addMenuItem(JMenu menu, String name, String command) {
        addMenuItem(menu, name, command, null);
    }

    private void addMenuItem(JMenu menu, JMenuItem item, String command, Object data, ActionListener extraAction) {
        item.addActionListener(e -> {
            CommandBus.getInstance().publish(command, this, data);
            if (extraAction != null) {
                extraAction.actionPerformed(e);
            }
        });
        menu.add(item);
    }

    private void addMenuItem(JMenu menu, String name, String command, ActionListener extraAction) {
        JMenuItem item = new JMenuItem(name);
        item.addActionListener(e -> {
            CommandBus.getInstance().publish(command, this);
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
