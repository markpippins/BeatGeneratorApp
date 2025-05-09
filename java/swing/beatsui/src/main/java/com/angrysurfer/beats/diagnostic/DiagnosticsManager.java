package com.angrysurfer.beats.diagnostic;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Synthesizer;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.diagnostic.helper.*;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.ReceiverManager;

/**
 * Singleton manager for application diagnostics
 */
public class DiagnosticsManager {
    // Singleton instance
    private static DiagnosticsManager instance;
    
    // Reference to main frame
    private final JFrame parentFrame;
    private final CommandBus commandBus;
    
    // Helper instances
    private final MidiDiagnosticsHelper midiHelper;
    private final DrumSequencerDiagnostics sequencerHelper;
    private final PlayerDiagnosticsHelper playerHelper;
    private final InstrumentDiagnosticsHelper instrumentHelper;
    private final SessionDiagnosticsHelper sessionHelper;
    private final UserConfigDiagnosticsHelper configHelper;
    private final ChannelManagerDiagnostics channelHelper;
    private final DeviceManagerDiagnostics deviceHelper;
    private final ReceiverManagerDiagnosticsHelper receiverHelper;
    private final PlayerManagerDiagnosticsHelper playerManagerHelper;
    private final SessionManagerDiagnosticsHelper sessionManagerHelper;
    private final UserConfigManagerDiagnosticsHelper userConfigManagerHelper;
    private final MelodicSequencerDiagnosticsHelper melodicSequencerHelper;
    private final MelodicSequencerManagerDiagnosticsHelper melodicSequencerManagerHelper;
    
    /**
     * Private constructor for singleton pattern
     */
    private DiagnosticsManager(JFrame parentFrame, CommandBus commandBus) {
        this.parentFrame = parentFrame;
        this.commandBus = commandBus;
        
        // Initialize helpers
        this.midiHelper = new MidiDiagnosticsHelper(parentFrame);
        this.sequencerHelper = new DrumSequencerDiagnostics();
        this.playerHelper = new PlayerDiagnosticsHelper();
        this.instrumentHelper = new InstrumentDiagnosticsHelper();
        this.sessionHelper = new SessionDiagnosticsHelper();
        this.configHelper = new UserConfigDiagnosticsHelper();
        this.channelHelper = new ChannelManagerDiagnostics();
        this.deviceHelper = new DeviceManagerDiagnostics();
        this.receiverHelper = new ReceiverManagerDiagnosticsHelper();
        this.playerManagerHelper = new PlayerManagerDiagnosticsHelper();
        this.sessionManagerHelper = new SessionManagerDiagnosticsHelper();
        this.userConfigManagerHelper = new UserConfigManagerDiagnosticsHelper();
        this.melodicSequencerHelper = new MelodicSequencerDiagnosticsHelper();
        this.melodicSequencerManagerHelper = new MelodicSequencerManagerDiagnosticsHelper();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized DiagnosticsManager getInstance(JFrame parentFrame, CommandBus commandBus) {
        if (instance == null) {
            instance = new DiagnosticsManager(parentFrame, commandBus);
        }
        return instance;
    }
    
    /**
     * Get singleton instance (only if already initialized)
     */
    public static DiagnosticsManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DiagnosticsManager not initialized. Call getInstance(JFrame, CommandBus) first.");
        }
        return instance;
    }

    /**
     * Show error log in a JOptionPane
     */
    public static void showError(String testName, String message) {
        JOptionPane.showMessageDialog(null, testName + "\n\nERROR: " + message);
    }

    /**
     * Show diagnostic log in a dialog
     */
    public void showDiagnosticLogDialog(DiagnosticLogBuilder log) {
        showDiagnosticLogDialog(parentFrame, log);
    }
    
    /**
     * Show diagnostic log in a dialog
     */
    public static void showDiagnosticLogDialog(Component parent, DiagnosticLogBuilder log) {
        String logText = log.build();
        
        // If we have errors, also show an error dialog first
        if (log.hasErrors()) {
            StringBuilder errorMessage = new StringBuilder("Diagnostics found errors:\n\n");
            int count = 0;
            for (String error : log.getErrors()) {
                if (count < 5) { // Show max 5 errors in the summary dialog
                    errorMessage.append("• ").append(error).append("\n");
                    count++;
                } else {
                    errorMessage.append("• ... and ").append(log.getErrors().size() - 5)
                              .append(" more errors\n");
                    break;
                }
            }
            errorMessage.append("\nSee detailed report for more information.");
            showError(log.getTitle(), errorMessage.toString());
        }
        
        // Use invokeLater to show the detailed log dialog after any other dialogs are dismissed
        SwingUtilities.invokeLater(() -> {
            JDialog dialog = new JDialog(JOptionPane.getFrameForComponent(parent), log.getTitle() + " Report", false);
            
            // Create text area with scrolling
            JTextArea textArea = new JTextArea(30, 80);
            textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            textArea.setText(logText);
            textArea.setEditable(false);
            textArea.setCaretPosition(0); // Scroll to top
            
            JScrollPane scrollPane = new JScrollPane(textArea);
            
            // Add copy and close buttons
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            
            JButton copyButton = new JButton("Copy to Clipboard");
            copyButton.addActionListener(e -> {
                textArea.selectAll();
                textArea.copy();
                textArea.setCaretPosition(0);
            });
            
            JButton closeButton = new JButton("Close");
            closeButton.addActionListener(e -> dialog.dispose());
            
            buttonPanel.add(copyButton);
            buttonPanel.add(closeButton);
            
            dialog.setLayout(new BorderLayout());
            dialog.add(scrollPane, BorderLayout.CENTER);
            dialog.add(buttonPanel, BorderLayout.SOUTH);
            
            dialog.setSize(800, 600);
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
        });
    }
    
    /**
     * Run all diagnostics with progress splash screen
     */
    public void runAllDiagnostics() {
        // Create splash screen for progress reporting
        DiagnosticsSplashScreen splash = new DiagnosticsSplashScreen("Running Diagnostics", "Initializing...");
        splash.setMaxProgress(14); // Updated for new tests
        splash.setVisible(true);
        
        // Create a comprehensive log builder
        DiagnosticLogBuilder masterLog = new DiagnosticLogBuilder("Complete System Diagnostics");
        
        // Run in background thread
        new Thread(() -> {
            try {
                // Redis diagnostics
                splash.setProgress(0, "Testing Redis connection...");
                DiagnosticLogBuilder redisLog = RedisDiagnosticsHelper.runAllRedisDiagnostics();
                masterLog.addSection("1. Redis Diagnostics");
                masterLog.addLine(redisLog.buildWithoutHeader());
                
                // Check if Redis failed (critical dependency)
                if (redisLog.hasErrors()) {
                    splash.setVisible(false);
                    masterLog.addError("Redis diagnostics failed. Some tests skipped.");
                    showDiagnosticLogDialog(masterLog);
                    return;
                }
                
                // MIDI connection diagnostics
                splash.setProgress(1, "Testing MIDI connections...");
                DiagnosticLogBuilder midiLog = testMidiConnections();
                masterLog.addSection("2. MIDI Connection Diagnostics");
                masterLog.addLine(midiLog.buildWithoutHeader());
                
                // Session tests
                splash.setProgress(2, "Testing sessions...");
                DiagnosticLogBuilder sessionLog = testSessions();
                masterLog.addSection("3. Session Diagnostics");
                masterLog.addLine(sessionLog.buildWithoutHeader());

                // User config tests
                splash.setProgress(3, "Testing user configurations...");
                DiagnosticLogBuilder configLog = testUserConfig();
                masterLog.addSection("4. User Configuration");
                masterLog.addLine(configLog.buildWithoutHeader());

                // DrumSequencer diagnostics
                splash.setProgress(4, "Testing DrumSequencer...");
                DiagnosticLogBuilder sequencerLog = testDrumSequencer();
                masterLog.addSection("5. DrumSequencer Diagnostics");
                masterLog.addLine(sequencerLog.buildWithoutHeader());
                
                // MelodicSequencer diagnostics
                splash.setProgress(5, "Testing MelodicSequencer...");
                DiagnosticLogBuilder melodicSequencerLog = testMelodicSequencer();
                masterLog.addSection("6. MelodicSequencer Diagnostics");
                masterLog.addLine(melodicSequencerLog.buildWithoutHeader());

                // Player/Instrument integrity
                splash.setProgress(6, "Testing player and instrument integrity...");
                DiagnosticLogBuilder playerLog = testPlayerInstrumentIntegrity();
                masterLog.addSection("7. Player/Instrument Integrity");
                masterLog.addLine(playerLog.buildWithoutHeader());
                
                // MIDI sound test
                splash.setProgress(7, "Testing MIDI sound output...");
                DiagnosticLogBuilder soundLog = testMidiSound();
                masterLog.addSection("8. MIDI Sound Test");
                masterLog.addLine(soundLog.buildWithoutHeader());
                
                // Channel Manager test
                splash.setProgress(8, "Testing Channel Manager...");
                DiagnosticLogBuilder channelLog = testChannelManager();
                masterLog.addSection("9. Channel Manager Diagnostics");
                masterLog.addLine(channelLog.buildWithoutHeader());
                
                // Device Manager test
                splash.setProgress(9, "Testing Device Manager...");
                DiagnosticLogBuilder deviceLog = testDeviceManager();
                masterLog.addSection("10. Device Manager Diagnostics");
                masterLog.addLine(deviceLog.buildWithoutHeader());
                
                // Receiver Manager test
                splash.setProgress(10, "Testing Receiver Manager...");
                DiagnosticLogBuilder receiverLog = testReceiverManager();
                masterLog.addSection("11. Receiver Manager Diagnostics");
                masterLog.addLine(receiverLog.buildWithoutHeader());
                
                // PlayerManager test
                splash.setProgress(11, "Testing PlayerManager...");
                DiagnosticLogBuilder playerManagerLog = testPlayerManager();
                masterLog.addSection("12. PlayerManager Diagnostics");
                masterLog.addLine(playerManagerLog.buildWithoutHeader());
                
                // SessionManager test
                splash.setProgress(12, "Testing SessionManager...");
                DiagnosticLogBuilder sessionManagerLog = testSessionManager();
                masterLog.addSection("13. SessionManager Diagnostics");
                masterLog.addLine(sessionManagerLog.buildWithoutHeader());
                
                // UserConfigManager test
                splash.setProgress(13, "Testing UserConfigManager...");
                DiagnosticLogBuilder userConfigManagerLog = testUserConfigManager();
                masterLog.addSection("14. UserConfigManager Diagnostics");
                masterLog.addLine(userConfigManagerLog.buildWithoutHeader());

                // MelodicSequencerManager test
                splash.setProgress(14, "Testing MelodicSequencerManager...");
                DiagnosticLogBuilder melodicSequencerManagerLog = testMelodicSequencerManager();
                masterLog.addSection("15. MelodicSequencerManager Diagnostics");
                masterLog.addLine(melodicSequencerManagerLog.buildWithoutHeader());
                
                // Complete
                splash.setProgress(15, "Completed all diagnostics");
                
                // Compile errors and warnings from all logs
                for (DiagnosticLogBuilder log : Arrays.asList(
                        redisLog, midiLog, sessionLog, configLog, sequencerLog, 
                        melodicSequencerLog, playerLog, soundLog, channelLog, deviceLog, receiverLog,
                        playerManagerLog, sessionManagerLog, userConfigManagerLog, melodicSequencerManagerLog)) {
                    if (log != null) {
                        for (String error : log.getErrors()) {
                            masterLog.addError(error);
                        }
                        for (String warning : log.getWarnings()) {
                            masterLog.addWarning(warning);
                        }
                    }
                }
                
            } catch (Exception e) {
                masterLog.addException(e);
            } finally {
                splash.setVisible(false);
                showDiagnosticLogDialog(masterLog);
            }
        }).start();
    }
    
    // Delegating methods to the appropriate helpers
    
    /**
     * Test DrumSequencer functionality
     */
    public DiagnosticLogBuilder testDrumSequencer() {
        return sequencerHelper.runDrumSequencerDiagnostics();
    }
    
    /**
     * Test DrumSequencer pattern operations
     */
    public DiagnosticLogBuilder testPatternOperations() {
        return sequencerHelper.testPatternOperations();
    }
    
    /**
     * Test MIDI connections
     */
    public DiagnosticLogBuilder testMidiConnections() {
        return midiHelper.testMidiConnections();
    }
    
    /**
     * Test MIDI sound
     */
    public DiagnosticLogBuilder testMidiSound() {
        return midiHelper.testMidiSound();
    }
    
    /**
     * Test MIDI synthesizer capabilities
     */
    public DiagnosticLogBuilder testSynthesizerCapabilities() {
        return midiHelper.testSynthesizerCapabilities();
    }
    
    /**
     * Test player and instrument integrity
     */
    public DiagnosticLogBuilder testPlayerInstrumentIntegrity() {
        return playerHelper.testPlayerInstrumentIntegrity();
    }
    
    /**
     * Test player operations
     */
    public DiagnosticLogBuilder testPlayerOperations() {
        return playerHelper.testPlayerOperations();
    }
    
    /**
     * Test instrument operations
     */
    public DiagnosticLogBuilder testInstrumentOperations() {
        return instrumentHelper.testInstrumentOperations();
    }
    
    /**
     * Test sessions
     */
    public DiagnosticLogBuilder testSessions() {
        return sessionHelper.testSessionDiagnostics();
    }
    
    /**
     * Test session navigation
     */
    public DiagnosticLogBuilder testSessionNavigation() {
        return sessionHelper.testSessionNavigation();
    }
    
    /**
     * Test session validity
     */
    public DiagnosticLogBuilder testSessionValidity() {
        return sessionHelper.testSessionValidity();
    }
    
    /**
     * Test user config
     */
    public DiagnosticLogBuilder testUserConfig() {
        return configHelper.testUserConfigDiagnostics();
    }
    
    /**
     * Test channel manager functionality
     */
    public DiagnosticLogBuilder testChannelManager() {
        return channelHelper.testChannelManager();
    }

    /**
     * Test device manager functionality
     */
    public DiagnosticLogBuilder testDeviceManager() {
        return deviceHelper.testDeviceManager();
    }

    /**
     * Test a specific MIDI device
     */
    public DiagnosticLogBuilder testSpecificDevice(String deviceName) {
        return deviceHelper.testSpecificDevice(deviceName);
    }

    /**
     * Test receiver manager functionality
     */
    public DiagnosticLogBuilder testReceiverManager() {
        return receiverHelper.testReceiverManager();
    }

    /**
     * Test receiver reliability under load
     */
    public DiagnosticLogBuilder testReceiverReliability() {
        return receiverHelper.testReceiverReliability();
    }

    /**
     * Test PlayerManager functionality
     */
    public DiagnosticLogBuilder testPlayerManager() {
        return playerManagerHelper.testPlayerManager();
    }

    /**
     * Test a specific player
     */
    public DiagnosticLogBuilder testSpecificPlayer(Long playerId) {
        return playerManagerHelper.testSpecificPlayer(playerId);
    }

    /**
     * Test SessionManager functionality
     */
    public DiagnosticLogBuilder testSessionManager() {
        return sessionManagerHelper.testSessionManager();
    }

    /**
     * Test session persistence
     */
    public DiagnosticLogBuilder testSessionPersistence() {
        return sessionManagerHelper.testSessionPersistence();
    }

    /**
     * Test UserConfigManager functionality
     */
    public DiagnosticLogBuilder testUserConfigManager() {
        return userConfigManagerHelper.testUserConfigManager();
    }

    /**
     * Test config transaction support
     */
    public DiagnosticLogBuilder testConfigTransactions() {
        return userConfigManagerHelper.testConfigTransactions();
    }

    /**
     * Test MelodicSequencer functionality
     */
    public DiagnosticLogBuilder testMelodicSequencer() {
        return melodicSequencerHelper.runMelodicSequencerDiagnostics();
    }

    /**
     * Test MelodicSequencer pattern operations
     */
    public DiagnosticLogBuilder testMelodicPatternOperations() {
        return melodicSequencerHelper.testPatternOperations();
    }

    /**
     * Test MelodicSequencerManager functionality
     */
    public DiagnosticLogBuilder testMelodicSequencerManager() {
        return melodicSequencerManagerHelper.testMelodicSequencerManager();
    }

    /**
     * Test melodic sequence persistence
     */
    public DiagnosticLogBuilder testMelodicSequencePersistence() {
        return melodicSequencerManagerHelper.testSequencePersistence();
    }

    /**
     * Diagnoses and attempts to repair MIDI device connections
     * across the entire application
     */
    public DiagnosticLogBuilder repairMidiConnections() {
        DiagnosticLogBuilder log = new DiagnosticLogBuilder("MIDI Repair Utility");
        
        try {
            log.addSection("MIDI Device Check");
            
            // Check available devices
            DeviceManager deviceManager = DeviceManager.getInstance();
            List<String> deviceNames = deviceManager.getAvailableOutputDeviceNames();
            
            if (deviceNames.isEmpty()) {
                log.addWarning("No MIDI output devices found");
                
                // Try to initialize the Gervill synthesizer
                log.addLine("Attempting to initialize Gervill synthesizer...");
                boolean success = deviceManager.ensureGervillAvailable();
                log.addLine("Gervill initialization " + (success ? "successful" : "failed"));
                
                // Check again
                deviceNames = deviceManager.getAvailableOutputDeviceNames();
                log.addLine("Available devices after initialization: " + deviceNames.size());
            } else {
                log.addLine("Found " + deviceNames.size() + " MIDI output devices:");
                for (String name : deviceNames) {
                    log.addIndentedLine(name, 1);
                }
            }
            
            // Check Gervill specifically
            log.addSection("Checking Gervill Status");
            MidiDevice gervill = DeviceManager.getMidiDevice("Gervill");
            if (gervill != null) {
                log.addLine("Gervill synthesizer found");
                if (!gervill.isOpen()) {
                    try {
                        gervill.open();
                        log.addLine("Opened Gervill synthesizer");
                    } catch (Exception e) {
                        log.addError("Failed to open Gervill: " + e.getMessage());
                    }
                } else {
                    log.addLine("Gervill is already open");
                }
            } else {
                log.addWarning("Gervill synthesizer not found");
                log.addLine("Attempting to initialize the synthesizer...");
                try {
                    Synthesizer synth = MidiSystem.getSynthesizer();
                    synth.open();
                    log.addLine("Synthesizer initialized");
                    
                    // Check again
                    gervill = DeviceManager.getMidiDevice("Gervill");
                    if (gervill != null) {
                        log.addLine("Gervill synthesizer is now available");
                    } else {
                        log.addWarning("Gervill still not available after initialization");
                    }
                } catch (Exception e) {
                    log.addError("Failed to initialize synthesizer: " + e.getMessage());
                }
            }
            
            // Clear receiver cache
            log.addSection("Clearing Receiver Cache");
            ReceiverManager receiverManager = ReceiverManager.getInstance();
            receiverManager.clearAllReceivers();
            log.addLine("Receiver cache cleared");
            
            // Test a receiver
            log.addSection("Testing Receiver Creation");
            MidiDevice device = deviceManager.getDefaultOutputDevice();
            if (device != null) {
                log.addLine("Got default device: " + device.getDeviceInfo().getName());
                try {
                    Receiver receiver = receiverManager.getOrCreateReceiver(
                        device.getDeviceInfo().getName(), device);
                    if (receiver != null) {
                        log.addLine("Successfully created receiver");
                        
                        // Test the receiver with a note-on message
                        try {
                            ShortMessage msg = new ShortMessage();
                            msg.setMessage(ShortMessage.NOTE_ON, 0, 60, 64);
                            receiver.send(msg, -1);
                            log.addLine("Successfully sent test message to receiver");
                            
                            // Send note-off after a short delay
                            Thread.sleep(500);
                            msg.setMessage(ShortMessage.NOTE_OFF, 0, 60, 0);
                            receiver.send(msg, -1);
                        } catch (Exception e) {
                            log.addError("Failed to send test message: " + e.getMessage());
                        }
                    } else {
                        log.addError("Failed to create receiver");
                    }
                } catch (Exception e) {
                    log.addError("Error testing receiver: " + e.getMessage());
                }
            } else {
                log.addError("No default device available for testing");
            }
            
            // Send command to repair connections
            log.addSection("Repairing Instrument Connections");
            log.addLine("Sending repair command to sequencers...");
            CommandBus.getInstance().publish(Commands.REPAIR_MIDI_CONNECTIONS, this);
            log.addLine("Repair command sent");
            
            log.addLine("\nRepair process complete. If MIDI issues persist, try restarting the application.");
        } catch (Exception e) {
            log.addException(e);
        }
        
        return log;
    }
}