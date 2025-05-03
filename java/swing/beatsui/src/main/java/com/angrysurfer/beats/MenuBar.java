package com.angrysurfer.beats;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.sound.midi.*;
import javax.swing.*;

import com.angrysurfer.beats.diagnostic.DiagnosticLogBuilder;
import com.angrysurfer.beats.diagnostic.DiagnosticsSplashScreen;
import com.angrysurfer.beats.diagnostic.RedisDiagnosticsHelper;
import com.angrysurfer.beats.visualization.IVisualizationHandler;
import com.angrysurfer.beats.visualization.VisualizationCategory;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.redis.InstrumentHelper;
import com.angrysurfer.core.redis.PlayerHelper;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.sequencer.DrumSequencer;
import com.angrysurfer.core.service.DrumSequencerManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import redis.clients.jedis.JedisPool;

public class MenuBar extends JMenuBar {

    private final JFrame parentFrame;
    private final ThemeManager themeManager;
    private final CommandBus commandBus = CommandBus.getInstance();

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
                commandBus.publish(Commands.DELETE_UNUSED_INSTRUMENTS, this);
            }
        });

        JMenu dbMenu = new JMenu("Database");
        dbMenu.setMnemonic(KeyEvent.VK_D);
        dbMenu.add(clearDb);
        dbMenu.add(clearInvalidSessions);
        dbMenu.add(deleteUnusedInstruments);

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

        // Add 000000000000s menu
        JMenu diagnosticsMenu = new JMenu("Diagnostics");
        diagnosticsMenu.setMnemonic(KeyEvent.VK_D);

        // Initialize DiagnosticsManager
        DiagnosticsManager diagnosticsManager = DiagnosticsManager.getInstance(parentFrame, commandBus);

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
                    DiagnosticLogBuilder log = RedisDiagnosticsHelper.runAllRedisDiagnostics();
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

        // Add the diagnostics menu to the menu bar
        add(diagnosticsMenu);

        // Register visualization listener
        commandBus.register(new IBusListener() {
            final boolean[] visualizationsEnabled = { false };
            JMenu visualizationMenu = new JMenu("Visualization");
            final JMenuItem startVisualizationItem = new JMenuItem("Start Visualization");
            final JMenuItem stopVisualizationItem = new JMenuItem("Stop Visualization");
            final JMenuItem lockVisualizationItem = new JMenuItem("Lock Current Visualization");
            final JMenuItem unlockVisualizationItem = new JMenuItem("Unlock Visualization"); // Add this
            final JMenuItem refreshVisualizationItem = new JMenuItem("Refresh");
            final List<CategoryMenuItem> categoryMenus = new ArrayList<>();
            final List<VisualizationMenuItem> defaultItems = new ArrayList<>();

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

    public void addMenuItem(JMenu menu, String name, String command) {
        addMenuItem(menu, name, command, null);
    }

    private void addMenuItem(JMenu menu, JMenuItem item, String command, Object data, ActionListener extraAction) {
        item.addActionListener(e -> {
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

    /**
     * Runs comprehensive diagnostics on the DrumSequencer
     */
    private void runDrumSequencerDiagnostics() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("DrumSequencer Diagnostics");
        
        try {
            // Get the active DrumSequencer
            DrumSequencer sequencer = getActiveDrumSequencer();
            if (sequencer == null) {
                log.addError("No active DrumSequencer found");
                DiagnosticsManager.showDiagnosticLogDialog(parentFrame, log);
                return;
            }
            
            // 1. Check sequencer status
            log.addSection("1. Sequencer Status");
            log.addIndentedLine("Playing: " + sequencer.isPlaying(), 1)
               .addIndentedLine("BPM: " + sequencer.getMasterTempo(), 1)
               .addIndentedLine("Swing: " + (sequencer.isSwingEnabled() ? "Enabled" : "Disabled"), 1)
               .addIndentedLine("Swing Amount: " + sequencer.getSwingPercentage(), 1);
            
            // 2. Check pattern data
            log.addSection("2. Pattern Data");
            int totalActiveSteps = 0;
            for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                int activeSteps = 0;
                for (int j = 0; j < sequencer.getPatternLength(i); j++) {
                    if (sequencer.isStepActive(i, j)) {
                        activeSteps++;
                        totalActiveSteps++;
                    }
                }
                log.addIndentedLine("Drum " + i + ": " + activeSteps + " active steps (Length: " + 
                                 sequencer.getPatternLength(i) + ")", 1);
            }
            log.addIndentedLine("Total active steps: " + totalActiveSteps, 1);
            
            if (totalActiveSteps == 0) {
                log.addWarning("No active steps found in patterns");
            }
            
            // 3. Check players and instruments
            log.addSection("3. Player/Instrument Check");
            int playersWithInstruments = 0;
            int openDevices = 0;
            
            for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                Player player = sequencer.getPlayer(i);
                if (player != null) {
                    log.addIndentedLine("Drum " + i + " - Player: " + player.getName() + 
                                     " (Type: " + player.getClass().getSimpleName() + ")", 1);
                    log.addIndentedLine("Root Note: " + player.getRootNote() + 
                                     ", Channel: " + player.getChannel(), 2);
                    
                    InstrumentWrapper instrument = player.getInstrument();
                    if (instrument != null) {
                        playersWithInstruments++;
                        log.addIndentedLine("Instrument: " + instrument.getName() + 
                                         " (ID: " + instrument.getId() + ")", 2);
                        log.addIndentedLine("Device Name: " + instrument.getDeviceName() + 
                                         ", Channel: " + instrument.getChannel(), 2);
                        
                        MidiDevice device = instrument.getDevice();
                        if (device != null) {
                            log.addIndentedLine("Device: " + device.getDeviceInfo().getName() + 
                                             " (Open: " + device.isOpen() + ")", 2);
                            if (device.isOpen()) {
                                openDevices++;
                            }

                            Receiver receiver = null;
                            try {
                                receiver = instrument.getReceiver();
                                log.addIndentedLine("Receiver: " + (receiver != null ? "OK" : "NULL"), 2);
                            } catch (MidiUnavailableException e) {
                                log.addIndentedLine("Receiver: ERROR - " + e.getMessage(), 2);
                            }
                        } else {
                            log.addIndentedLine("Device: NULL", 2);
                        }
                    } else {
                        log.addIndentedLine("Instrument: NULL", 2);
                    }
                } else {
                    log.addIndentedLine("Drum " + i + " - Player: NULL", 1);
                }
            }
            
            log.addIndentedLine("Players with instruments: " + playersWithInstruments + 
                             " out of " + DrumSequencer.DRUM_PAD_COUNT, 1);
            log.addIndentedLine("Open MIDI devices: " + openDevices, 1);
            
            if (playersWithInstruments == 0) {
                log.addError("No players have instruments assigned");
            }
            
            if (openDevices == 0) {
                log.addError("No open MIDI devices found");
            }
            
            // 4. Attempt to trigger a test note
            log.addSection("4. Test Note Trigger");
            try {
                // Find first valid drum
                boolean noteTriggered = false;
                for (int i = 0; i < DrumSequencer.DRUM_PAD_COUNT; i++) {
                    Player player = sequencer.getPlayer(i);
                    if (player != null && player.getInstrument() != null && 
                        player.getInstrument().getDevice() != null &&
                        player.getInstrument().getDevice().isOpen()) {
                        
                        log.addIndentedLine("Triggering test note on drum " + i, 1);
                        sequencer.playDrumNote(i, 100);
                        noteTriggered = true;
                        Thread.sleep(200);
                        break;
                    }
                }
                
                if (!noteTriggered) {
                    log.addWarning("Could not find a valid drum to trigger");
                }
            } catch (Exception e) {
                log.addError("Error triggering test note: " + e.getMessage());
            }
            
            log.addLine("\nDrumSequencer diagnostics completed.");
            
            // Display the diagnostic results
            DiagnosticsManager.showDiagnosticLogDialog(parentFrame, log);
            
        } catch (Exception e) {
            log.addException(e);
            DiagnosticsManager.showDiagnosticLogDialog(parentFrame, log);
        }
    }

    /**
     * Tests MIDI connections by listing all devices and their status
     */
    private void testMidiConnections() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("MIDI Connection Test");
        
        try {
            // Get available MIDI devices
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            log.addLine("Found " + infos.length + " MIDI devices:");
            
            int availableReceivers = 0;
            for (MidiDevice.Info info : infos) {
                try {
                    MidiDevice device = MidiSystem.getMidiDevice(info);
                    boolean isReceiver = device.getMaxReceivers() != 0;
                    boolean isTransmitter = device.getMaxTransmitters() != 0;
                    
                    log.addLine(" - " + info.getName() + " (" + info.getDescription() + ")");
                    log.addIndentedLine("Vendor: " + info.getVendor() + ", Version: " + info.getVersion(), 1);
                    log.addIndentedLine("Receivers: " + device.getMaxReceivers() + 
                                     ", Transmitters: " + device.getMaxTransmitters(), 1);
                    
                    if (isReceiver) {
                        availableReceivers++;
                        try {
                            if (!device.isOpen()) {
                                device.open();
                            }
                            Receiver receiver = device.getReceiver();
                            if (receiver != null) {
                                log.addIndentedLine("Successfully obtained receiver", 1);
                                
                                // Send a test note to the device
                                ShortMessage msg = new ShortMessage();
                                msg.setMessage(ShortMessage.NOTE_ON, 9, 60, 100);
                                receiver.send(msg, -1);
                                
                                Thread.sleep(200);
                                
                                msg.setMessage(ShortMessage.NOTE_OFF, 9, 60, 0);
                                receiver.send(msg, -1);
                                
                                log.addIndentedLine("Sent test note to device", 1);
                                
                                receiver.close();
                            }
                            if (device.isOpen()) {
                                device.close();
                            }
                        } catch (Exception e) {
                            log.addIndentedLine("Error accessing receiver: " + e.getMessage(), 1);
                        }
                    }
                } catch (Exception e) {
                    log.addLine(" - Error accessing " + info.getName() + ": " + e.getMessage());
                }
            }
            
            if (availableReceivers == 0) {
                log.addError("No MIDI output devices with receivers found");
            }
            
            // Display the diagnostic results
            DiagnosticsManager.showDiagnosticLogDialog(parentFrame, log);
            
        } catch (Exception e) {
            log.addException(e);
            DiagnosticsManager.showDiagnosticLogDialog(parentFrame, log);
        }
    }

    /**
     * Tests player and instrument integrity in the database
     */
    private void testPlayerInstrumentIntegrity() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("Player/Instrument Integrity Test");
        
        try {
            // Get the JedisPool and ObjectMapper from RedisService
            JedisPool jedisPool = RedisService.getInstance().getJedisPool();
            ObjectMapper objectMapper = RedisService.getInstance().getObjectMapper();
            
            // Create helpers
            PlayerHelper playerHelper = new PlayerHelper(jedisPool, objectMapper);
            InstrumentHelper instrumentHelper = new InstrumentHelper(jedisPool, objectMapper);
            
            // Test 1: List all players and check their instruments
            log.addSection("1. Checking all players");
            List<Long> playerIds = Arrays.asList(playerHelper.getCachedPlayerIds());
            log.addIndentedLine("Found " + playerIds.size() + " players", 1);
            
            int playersWithInstruments = 0;
            int playersWithoutInstruments = 0;
            int playersWithInvalidInstruments = 0;
            
            for (Long playerId : playerIds) {
                for (String className : Arrays.asList("Strike", "Note")) {
                    try {
                        Player player = playerHelper.findPlayerById(playerId, className);
                        if (player != null) {
                            log.addIndentedLine("Player " + playerId + " (" + className + "): " + 
                                             player.getName(), 1);
                            
                            if (player.getInstrumentId() != null) {
                                InstrumentWrapper instrument = 
                                    instrumentHelper.findInstrumentById(player.getInstrumentId());
                                
                                if (instrument != null) {
                                    log.addIndentedLine("Instrument: " + instrument.getName() + 
                                                     " (ID: " + instrument.getId() + ")", 2);
                                    playersWithInstruments++;
                                } else {
                                    log.addIndentedLine("ERROR: Referenced instrument " + 
                                                     player.getInstrumentId() + " not found", 2);
                                    playersWithInvalidInstruments++;
                                }
                            } else {
                                log.addIndentedLine("No instrument assigned", 2);
                                playersWithoutInstruments++;
                            }
                        }
                    } catch (Exception e) {
                        // Skip exceptions for player type mismatches
                    }
                }
            }
            
            log.addIndentedLine("Players with valid instruments: " + playersWithInstruments, 1)
               .addIndentedLine("Players without instruments: " + playersWithoutInstruments, 1)
               .addIndentedLine("Players with invalid instruments: " + playersWithInvalidInstruments, 1);
            
            if (playersWithInvalidInstruments > 0) {
                log.addError(playersWithInvalidInstruments + " players have invalid instrument references");
            }
            
            // Test 2: List all instruments and check for orphans
            log.addSection("2. Checking all instruments");
            List<Long> instrumentIds = instrumentHelper.findAllInstruments().stream()
                .map(InstrumentWrapper::getId)
                .collect(Collectors.toList());
            log.addIndentedLine("Found " + instrumentIds.size() + " instruments", 1);
            
            int usedInstruments = 0;
            int unusedInstruments = 0;
            
            for (Long instrumentId : instrumentIds) {
                InstrumentWrapper instrument = instrumentHelper.findInstrumentById(instrumentId);
                if (instrument != null) {
                    log.addIndentedLine("Instrument " + instrumentId + ": " + instrument.getName(), 1);
                    
                    // Check if any player uses this instrument
                    boolean isUsed = false;
                    for (Long playerId : playerIds) {
                        for (String className : Arrays.asList("Strike", "Note")) {
                            try {
                                Player player = playerHelper.findPlayerById(playerId, className);
                                if (player != null && player.getInstrumentId() != null && 
                                    player.getInstrumentId().equals(instrumentId)) {
                                    log.addIndentedLine("Used by player " + playerId + 
                                                     " (" + player.getName() + ")", 2);
                                    isUsed = true;
                                    break;
                                }
                            } catch (Exception e) {
                                // Skip exceptions for player type mismatches
                            }
                        }
                        if (isUsed) break;
                    }
                    
                    if (isUsed) {
                        usedInstruments++;
                    } else {
                        log.addIndentedLine("Not used by any player", 2);
                        unusedInstruments++;
                    }
                }
            }
            
            log.addIndentedLine("Used instruments: " + usedInstruments, 1)
               .addIndentedLine("Unused instruments: " + unusedInstruments, 1);
            
            // Display the diagnostic results
            DiagnosticsManager.showDiagnosticLogDialog(parentFrame, log);
            
        } catch (Exception e) {
            log.addException(e);
            DiagnosticsManager.showDiagnosticLogDialog(parentFrame, log);
        }
    }

    /**
     * Tests MIDI sound output by playing a sequence of notes
     */
    private void testMidiSound() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("MIDI Sound Test");
        
        try {
            log.addLine("=== MIDI Sound Test ===");
            
            // Use a single dialog that stays open throughout the test process
            final JDialog testDialog = new JDialog(parentFrame, "MIDI Sound Test", true);
            testDialog.setLayout(new BorderLayout());
            
            JLabel statusLabel = new JLabel("Ready to start MIDI sound test");
            statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            statusLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            statusLabel.setHorizontalAlignment(JLabel.CENTER);
            
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            JButton nextButton = new JButton("Start Test");
            JButton cancelButton = new JButton("Cancel");
            
            buttonPanel.add(nextButton);
            buttonPanel.add(cancelButton);
            
            testDialog.add(statusLabel, BorderLayout.CENTER);
            testDialog.add(buttonPanel, BorderLayout.SOUTH);
            
            testDialog.setSize(400, 200);
            testDialog.setLocationRelativeTo(parentFrame);
            
            // Set up a flag to track if the test should continue
            final boolean[] continueTest = {false};
            final boolean[] testCancelled = {false};
            
            // Set up button actions
            nextButton.addActionListener(e -> {
                continueTest[0] = true;
                testDialog.setVisible(false);
            });
            
            cancelButton.addActionListener(e -> {
                testCancelled[0] = true;
                testDialog.setVisible(false);
            });
            
            // Show initial dialog
            testDialog.setVisible(true);
            
            if (testCancelled[0]) {
                log.addLine("Test cancelled by user");
                // Show diagnostic results later
                return;
            }
            
            // Find suitable MIDI device
            log.addLine("Opening synthesizer...");
            Synthesizer synth = MidiSystem.getSynthesizer();
            synth.open();
            log.addLine("Opened synthesizer: " + synth.getDeviceInfo().getName());
            
            // Play a major scale
            log.addLine("Playing major scale...");
            MidiChannel channel = synth.getChannels()[0];
            int[] notes = {60, 62, 64, 65, 67, 69, 71, 72};
            for (int note : notes) {
                channel.noteOn(note, 100);
                Thread.sleep(300);
                channel.noteOff(note);
                log.addIndentedLine("Played note: " + note, 1);
            }
            
            // Reset dialog for percussion test
            nextButton.setText("Continue to Percussion");
            cancelButton.setText("Stop Test");
            statusLabel.setText("<html>Did you hear the scale?<br>Click 'Continue' to test percussion sounds</html>");
            continueTest[0] = false;
            testDialog.setVisible(true);
            
            if (testCancelled[0]) {
                log.addLine("Test stopped after scale");
                synth.close();
                log.addLine("Synthesizer closed");
                // Show diagnostic results later
                return;
            }
            
            // Test percussion (channel 9)
            log.addLine("Playing percussion sounds...");
            MidiChannel drumChannel = synth.getChannels()[9];
            int[] drumNotes = {35, 38, 42, 46, 49, 51};
            for (int note : drumNotes) {
                drumChannel.noteOn(note, 100);
                Thread.sleep(300);
                drumChannel.noteOff(note);
                log.addIndentedLine("Played drum note: " + note, 1);
            }
            
            synth.close();
            log.addLine("Synthesizer closed");
            
            // Final confirmation dialog
            nextButton.setText("All Sounds Good");
            JButton someIssuesButton = new JButton("Some Issues");
            JButton noSoundButton = new JButton("No Sound");
            
            buttonPanel.removeAll();
            buttonPanel.add(nextButton);
            buttonPanel.add(someIssuesButton);
            buttonPanel.add(noSoundButton);
            
            statusLabel.setText("<html>MIDI sound test completed.<br>How did it sound?</html>");
            
            // Set up result flags
            final int[] result = {0}; // 0=good, 1=issues, 2=no sound
            
            someIssuesButton.addActionListener(e -> {
                result[0] = 1;
                testDialog.setVisible(false);
            });
            
            noSoundButton.addActionListener(e -> {
                result[0] = 2;
                testDialog.setVisible(false);
            });
            
            testDialog.setVisible(true);
            
            // Process results
            switch (result[0]) {
                case 0:
                    log.addLine("Result: User confirmed all sounds played correctly");
                    break;
                case 1:
                    log.addLine("Result: User reported some issues with playback");
                    break;
                case 2:
                    log.addLine("Result: User reported no sound");
                    log.addError("MIDI sound test failed - No sound heard");
                    break;
            }
            
        } catch (Exception e) {
            log.addException(e);
        } finally {
            // Now we can safely show the diagnostic dialog without conflicts
            DiagnosticsManager.showDiagnosticLogDialog(parentFrame, log);
        }
    }

    /**
     * Helper method to get the active DrumSequencer
     */
    private DrumSequencer getActiveDrumSequencer() {
        // Get from whatever service/manager holds the active sequencer
        // This is application-specific and needs to be adapted
        try {
            return DrumSequencerManager.getInstance().getActiveSequencer();
        } catch (Exception e) {
            System.out.println("Error getting active sequencer: " + e.getMessage());
            return null;
        }
    }
}
