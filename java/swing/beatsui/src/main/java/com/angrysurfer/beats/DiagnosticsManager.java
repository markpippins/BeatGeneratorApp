package com.angrysurfer.beats;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Arrays;

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

import com.angrysurfer.beats.diagnostic.*;
import com.angrysurfer.core.api.CommandBus;

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
    private final DrumSequencerDiagnosticsHelper sequencerHelper;
    private final PlayerDiagnosticsHelper playerHelper;
    private final InstrumentDiagnosticsHelper instrumentHelper;
    private final SessionDiagnosticsHelper sessionHelper;
    private final UserConfigDiagnosticsHelper configHelper;
    
    /**
     * Private constructor for singleton pattern
     */
    private DiagnosticsManager(JFrame parentFrame, CommandBus commandBus) {
        this.parentFrame = parentFrame;
        this.commandBus = commandBus;
        
        // Initialize helpers
        this.midiHelper = new MidiDiagnosticsHelper(parentFrame);
        this.sequencerHelper = new DrumSequencerDiagnosticsHelper();
        this.playerHelper = new PlayerDiagnosticsHelper();
        this.instrumentHelper = new InstrumentDiagnosticsHelper();
        this.sessionHelper = new SessionDiagnosticsHelper();
        this.configHelper = new UserConfigDiagnosticsHelper();
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
        splash.setMaxProgress(7); // 7 major test categories
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
                
                // Player/Instrument integrity
                splash.setProgress(5, "Testing player and instrument integrity...");
                DiagnosticLogBuilder playerLog = testPlayerInstrumentIntegrity();
                masterLog.addSection("6. Player/Instrument Integrity");
                masterLog.addLine(playerLog.buildWithoutHeader());
                
                // MIDI sound test
                splash.setProgress(6, "Testing MIDI sound output...");
                DiagnosticLogBuilder soundLog = testMidiSound();
                masterLog.addSection("7. MIDI Sound Test");
                masterLog.addLine(soundLog.buildWithoutHeader());
                
                // Complete
                splash.setProgress(7, "Completed all diagnostics");
                
                // Compile errors and warnings from all logs
                for (DiagnosticLogBuilder log : Arrays.asList(redisLog, midiLog, sessionLog, configLog, sequencerLog, playerLog, soundLog)) {
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
}