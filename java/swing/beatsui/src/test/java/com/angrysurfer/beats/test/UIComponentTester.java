//package com.angrysurfer.beats.test;
//
//import com.angrysurfer.beats.panel.player.SoundParametersPanel;
//import com.angrysurfer.beats.panel.sequencer.mono.MelodicSequencerGeneratorPanel;
//import com.angrysurfer.beats.panel.sequencer.mono.ScalePanel;
//import com.angrysurfer.beats.panel.sequencer.poly.DrumParamsSequencerPanel;
//import com.angrysurfer.beats.util.UIHelper;
//import com.angrysurfer.core.api.CommandBus;
//import com.angrysurfer.core.event.NoteEvent;
//import com.angrysurfer.core.model.InstrumentWrapper;
//import com.angrysurfer.core.model.Player;
//import com.angrysurfer.core.sequencer.DrumSequencer;
//import com.angrysurfer.core.sequencer.MelodicSequencer;
//import com.angrysurfer.core.sequencer.SequencerConstants;
//import com.angrysurfer.core.service.*;
//import com.formdev.flatlaf.FlatLightLaf;
//
//import javax.sound.midi.MidiDevice;
//import javax.swing.*;
//import java.awt.*;
//import java.util.concurrent.atomic.AtomicReference;
//
/// **
// * Test application for verifying UI components functionality.
// * This application provides a way to test:
// * 1. Mousewheel support in various panels
// * 2. Soundbank changes in SoundParametersPanel
// */
//public class UIComponentTester {
//
//    private static final int DEFAULT_WINDOW_WIDTH = 800;
//    private static final int DEFAULT_WINDOW_HEIGHT = 600;
//
//    public static void main(String[] args) {
//        // Set up look and feel
//        try {
//            UIManager.setLookAndFeel(new FlatLightLaf());
//        } catch (UnsupportedLookAndFeelException e) {
//            System.err.println("Failed to initialize look and feel: " + e.getMessage());
//        }
//
//        // Initialize services
//        initializeServices();
//
//        // Create the UI on the EDT
//        SwingUtilities.invokeLater(() -> {
//            JFrame frame = new JFrame("UI Component Tester");
//            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//            frame.setSize(DEFAULT_WINDOW_WIDTH, DEFAULT_WINDOW_HEIGHT);
//
//            // Create the main panel with tabs
//            JTabbedPane tabbedPane = new JTabbedPane();
//
//            // Add test panels
//            tabbedPane.addTab("ScalePanel Test", createScalePanelTest());
//            tabbedPane.addTab("DrumParams Test", createDrumParamsPanelTest());
//            tabbedPane.addTab("MelodicGenerator Test", createMelodicGeneratorPanelTest());
//            tabbedPane.addTab("Soundbank Test", createSoundbankTest());
//
//            frame.getContentPane().add(tabbedPane);
//
//            // Center on screen
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//        });
//    }
//
//    private static void initializeServices() {
//        try {
//            System.out.println("Initializing services...");
//
//            // Initialize in the proper order
//            DeviceManager.getInstance().refreshDeviceList();
//            System.out.println("MIDI devices initialized");
//
//            InternalSynthManager.getInstance().initializeSynthesizer();
//            System.out.println("Synthesizer initialized");
//
//            SoundbankManager.getInstance().initializeSoundbanks();
//            System.out.println("Soundbanks initialized");
//
//            InstrumentManager.getInstance().initializeCache();
//            System.out.println("Instrument cache initialized");
//
//            System.out.println("All services initialized successfully");
//        } catch (Exception e) {
//            System.err.println("Failed to initialize services: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    private static JPanel createScalePanelTest() {
//        JPanel containerPanel = new JPanel(new BorderLayout());
//
//        // Create a melodic sequencer
//        MelodicSequencer sequencer = new MelodicSequencer();
//
//        // Create the scale panel
//        ScalePanel scalePanel = new ScalePanel(sequencer);
//
//        // Add instruction label
//        JLabel instructionLabel = new JLabel(
//                "<html><h3>Scale Panel Test</h3>" +
//                "<p>Test mousewheel functionality:</p>" +
//                "<ul>" +
//                "<li>Focus on Root Note combo and use mousewheel</li>" +
//                "<li>Focus on Scale combo and use mousewheel</li>" +
//                "<li>Focus on Octave spinner and use mousewheel</li>" +
//                "<li>Focus on Quantize toggle and use mousewheel</li>" +
//                "<li>Position mouse over components without focus and use mousewheel</li>" +
//                "</ul></html>"
//        );
//        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//        containerPanel.add(instructionLabel, BorderLayout.NORTH);
//        containerPanel.add(scalePanel, BorderLayout.CENTER);
//
//        return containerPanel;
//    }
//
//    private static JPanel createDrumParamsPanelTest() {
//        JPanel containerPanel = new JPanel(new BorderLayout());
//
//        // Create a NoteEvent consumer that just logs the events
//        DrumParamsSequencerPanel drumParamsPanel = new DrumParamsSequencerPanel(noteEvent ->
//            System.out.println("Note event: " + noteEvent)
//        );
//
//        // Create a drum sequencer instance
//        DrumSequencer sequencer = new DrumSequencer();
//        drumParamsPanel.setSequencer(sequencer);
//
//        // Add instruction label
//        JLabel instructionLabel = new JLabel(
//                "<html><h3>Drum Parameters Panel Test</h3>" +
//                "<p>Test mousewheel functionality:</p>" +
//                "<ul>" +
//                "<li>Focus on dials and use mousewheel to adjust values</li>" +
//                "<li>Focus on toggle buttons and use mousewheel to toggle</li>" +
//                "<li>Position mouse over components without focus and use mousewheel</li>" +
//                "</ul></html>"
//        );
//        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//        containerPanel.add(instructionLabel, BorderLayout.NORTH);
//        containerPanel.add(drumParamsPanel, BorderLayout.CENTER);
//
//        return containerPanel;
//    }
//
//    private static JPanel createMelodicGeneratorPanelTest() {
//        JPanel containerPanel = new JPanel(new BorderLayout());
//
//        // Create a melodic sequencer
//        MelodicSequencer sequencer = new MelodicSequencer();
//
//        // Create the generator panel
//        MelodicSequencerGeneratorPanel generatorPanel = new MelodicSequencerGeneratorPanel(sequencer);
//
//        // Add instruction label
//        JLabel instructionLabel = new JLabel(
//                "<html><h3>Melodic Generator Panel Test</h3>" +
//                "<p>Test mousewheel functionality:</p>" +
//                "<ul>" +
//                "<li>Focus on Range combo and use mousewheel</li>" +
//                "<li>Position mouse over components without focus and use mousewheel</li>" +
//                "</ul></html>"
//        );
//        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//        containerPanel.add(instructionLabel, BorderLayout.NORTH);
//        containerPanel.add(generatorPanel, BorderLayout.CENTER);
//
//        return containerPanel;
//    }
//
//    private static JPanel createSoundbankTest() {
//        JPanel containerPanel = new JPanel(new BorderLayout());
//
//        // Create a player with an instrument for testing
//        Player player = createTestPlayerWithInstrument();
//
//        // Create the sound parameters panel
//        SoundParametersPanel soundParamsPanel = new SoundParametersPanel();
//        soundParamsPanel.setPlayer(player);
//
//        // Add controls panel with test button
//        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
//
//        JButton playTestNoteButton = new JButton("Play Test Note");
//        playTestNoteButton.addActionListener(e -> {
//            // Play a test note on the current player to verify sound
//            Player currentPlayer = soundParamsPanel.getPlayer();
//            if (currentPlayer != null) {
//                NoteEvent noteEvent = new NoteEvent();
//                noteEvent.setChannel(currentPlayer.getChannel());
//                noteEvent.setNote(60); // Middle C
//                noteEvent.setVelocity(100);
//                noteEvent.setDuration(500);
//
//                // Send via CommandBus to ensure proper routing
//                CommandBus.getInstance().publish("NOTE_ON", soundParamsPanel, noteEvent);
//
//                // Also try direct playback through SoundbankManager for verification
//                SoundbankManager.getInstance().playPreviewNote(currentPlayer, 100);
//            }
//        });
//        controlsPanel.add(playTestNoteButton);
//
//        // Add instruction label
//        JLabel instructionLabel = new JLabel(
//                "<html><h3>Soundbank Test</h3>" +
//                "<p>Test soundbank functionality:</p>" +
//                "<ul>" +
//                "<li>Select different soundbanks from the dropdown</li>" +
//                "<li>Select different banks and presets</li>" +
//                "<li>Click 'Play Test Note' to verify the changes are applied</li>" +
//                "</ul></html>"
//        );
//        instructionLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
//
//        containerPanel.add(instructionLabel, BorderLayout.NORTH);
//        containerPanel.add(soundParamsPanel, BorderLayout.CENTER);
//        containerPanel.add(controlsPanel, BorderFactory.createEmptyBorder(10, 10, 10, 10), BorderLayout.SOUTH);
//
//        return containerPanel;
//    }
//
//    private static Player createTestPlayerWithInstrument() {
//        try {
//            // Create a new player
//            Player player = new Player();
//            player.setId(1L);
//            player.setName("Test Player");
//
//            // Ensure we have an internal synth instrument on a melodic channel
//            int channel = ChannelManager.getInstance().getNextAvailableMelodicChannel();
//
//            // Create an instrument for this player
//            InstrumentWrapper instrument = InternalSynthManager.getInstance()
//                    .createInternalInstrument(channel, "Test Instrument");
//
//            // Set it as the player's instrument
//            player.setInstrument(instrument);
//
//            // Initialize the instrument state
//            InternalSynthManager.getInstance().initializeInstrumentState(instrument);
//
//            return player;
//        } catch (Exception e) {
//            System.err.println("Failed to create test player: " + e.getMessage());
//            e.printStackTrace();
//
//            // Create a minimal player as fallback
//            Player fallbackPlayer = new Player();
//            fallbackPlayer.setId(999L);
//            fallbackPlayer.setName("Fallback Player");
//            fallbackPlayer.setChannel(0);
//
//            return fallbackPlayer;
//        }
//    }
//}
