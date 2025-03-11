package com.angrysurfer.beats.panel;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicButtonUI;

import com.angrysurfer.beats.ColorUtils;
import com.angrysurfer.beats.animation.ColorAnimator;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.BusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.util.Scale;

public class PianoPanel extends StatusProviderPanel {
    private static final String DEFAULT_ROOT = "C";
    private String currentRoot = DEFAULT_ROOT; // Add root note tracking
    private String currentScale = "Chromatic"; // Default scale
    private final CommandBus commandBus = CommandBus.getInstance();
    private final Set<Integer> heldNotes = new HashSet<>();
    private Map<Integer, JButton> noteToKeyMap = new HashMap<>();
    private final ColorAnimator colorAnimator;
    private JButton activeButton = null; // Add this field to track active button

    public PianoPanel() {
        this(null);
        setupActionBusListener();
    }

    private JButton followScaleBtn;

    // private JButton followChordBtn;

    public PianoPanel(StatusConsumer statusConsumer) {
        super(null, statusConsumer);
        // Increase width from 230 to 265 to accommodate the extra buttons
        setPreferredSize(new Dimension(265, 80));
        setMinimumSize(new Dimension(265, 80));
        setBorder(BorderFactory.createLineBorder(Color.lightGray, 1));
        setOpaque(true);
        setBackground(ColorUtils.fadedOrange);

        // Add three colored buttons on the right side
        int buttonWidth = 25;
        int buttonHeight = 15;
        int startX = getPreferredSize().width - buttonWidth - 5;
        int startY = 12; // Changed from 10 to 12 to push buttons down 2px
        int spacing = 5;

        followScaleBtn = new JButton();
        followScaleBtn.setBounds(startX, startY, buttonWidth, buttonHeight);
        followScaleBtn.setBackground(ColorUtils.coolBlue);

        configureToggleButton(followScaleBtn);

        JButton button2 = new JButton();
        button2.setBounds(startX, startY + buttonHeight + spacing, buttonWidth, buttonHeight);
        button2.setBackground(ColorUtils.warmMustard);
        configureToggleButton(button2);

        JButton button3 = new JButton();
        button3.setBounds(startX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight);
        button3.setBackground(ColorUtils.fadedOrange);
        configureToggleButton(button3);

        add(followScaleBtn);
        add(button2);
        add(button3);

        // Dimensions for keys
        int whiteKeyWidth = 30;
        int whiteKeyHeight = 60;
        int blackKeyWidth = 17;
        int blackKeyHeight = 30;

        // Create white keys

        String[] whiteNotes = { "C", "D", "E", "F", "G", "A", "B" };
        int[] whiteNoteValues = { 60, 62, 64, 65, 67, 69, 71 }; // MIDI note values
        for (int i = 0; i < 7; i++) {
            JButton whiteKey = createPianoKey(true, whiteNotes[i]);
            whiteKey.setBounds(i * whiteKeyWidth + 10, 10, whiteKeyWidth - 1, whiteKeyHeight);
            add(whiteKey);
            noteToKeyMap.put(whiteNoteValues[i], whiteKey); // Map MIDI note to key
        }

        // Create black keys
        String[] blackNotes = { "C#", "D#", "", "F#", "G#", "A#", "" };
        int[] blackNoteValues = { 61, 63, -1, 66, 68, 70, -1 }; // MIDI note values
        for (int i = 0; i < 7; i++) {
            if (!blackNotes[i].isEmpty()) {
                JButton blackKey = createPianoKey(false, blackNotes[i]);
                blackKey.setBounds(i * whiteKeyWidth + whiteKeyWidth / 2 + 10, 10, blackKeyWidth, blackKeyHeight);
                add(blackKey, 0); // Add black keys first so they appear on top
                noteToKeyMap.put(blackNoteValues[i], blackKey); // Map MIDI note to key
            }
        }

        // Initialize and start color animator
        colorAnimator = new ColorAnimator();
        colorAnimator.setOnColorUpdate(() -> repaint());
        colorAnimator.start();

        setupActionBusListener();
        setupPlayerStatusIndicator();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // Call super first
        Graphics2D g2d = (Graphics2D) g.create();

        // Create gradient paint using current animated color
        Color currentColor = colorAnimator.getCurrentColor();
        Color darkerColor = darker(currentColor, 0.7f);

        GradientPaint gradient = new GradientPaint(
                0, 0, currentColor,
                0, getHeight(), darkerColor);

        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        g2d.dispose();
    }

    private Color darker(Color color, float factor) {
        return new Color(
                Math.max((int) (color.getRed() * factor), 0),
                Math.max((int) (color.getGreen() * factor), 0),
                Math.max((int) (color.getBlue() * factor), 0));
    }

    private void setupActionBusListener() {
        commandBus.register(new BusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getData() instanceof Integer note) {
                    switch (action.getCommand()) {
                        case Commands.KEY_PRESSED -> handleKeyPress(note);
                        case Commands.KEY_HELD -> handleKeyHold(note);
                        case Commands.KEY_RELEASED -> handleKeyRelease(note);
                    }
                } else {
                    switch (action.getCommand()) {
                        case Commands.SCALE_SELECTED -> {
                            if (activeButton == followScaleBtn &&
                                    action.getData() instanceof String scaleName) {
                                currentScale = scaleName;
                                applyCurrentScale(); // This will use currentRoot
                            }
                        }
                        case Commands.ROOT_NOTE_SELECTED -> {
                            if (action.getData() instanceof String rootNote) {
                                currentRoot = rootNote;
                                applyCurrentScale(); // Reapply scale with new root
                            }
                        }
                    }
                }
            }
        });
    }

    private void handleKeyPress(int note) {
        if (!heldNotes.contains(note)) {
            // Visual feedback
            if (Objects.nonNull(statusConsumer))
                statusConsumer.setStatus("Playing note " + note);
            highlightKey(note);
            // MIDI note on
            playNote(note);
            // If not held, schedule release
            if (!heldNotes.contains(note)) {
                releaseNoteAfterDelay(note);
            }
        }
    }

    private void handleKeyHold(int note) {
        // Toggle behavior: if note is already held, release it
        if (heldNotes.contains(note)) {
            heldNotes.remove(note);
            unhighlightKey(note);
            // We don't need to send MIDI note-off here since PlayerManager
            // already scheduled it
            if (Objects.nonNull(statusConsumer)) {
                statusConsumer.setStatus("");
            }
        } else {
            // Add to held notes and play
            if (Objects.nonNull(statusConsumer)) {
                Player activePlayer = PlayerManager.getInstance().getActivePlayer();
                String playerInfo = activePlayer != null ? 
                                " through " + activePlayer.getName() : 
                                " (no active player)";
                statusConsumer.setStatus("Holding note " + note + playerInfo);
            }
            heldNotes.add(note);
            highlightKey(note);
            playNote(note);
            
            // For held notes, we want them to continue playing, so we don't 
            // schedule a release
        }
    }

    private void handleKeyRelease(int note) {
        if (!heldNotes.contains(note)) {
            unhighlightKey(note);
            stopNote(note);
            if (Objects.nonNull(statusConsumer))
                statusConsumer.setStatus("");
        }
    }

    private void highlightKey(int note) {
        SwingUtilities.invokeLater(() -> {
            JButton key = noteToKeyMap.get(note);
            if (key != null) {
                key.getModel().setPressed(true);
                key.getModel().setArmed(true);
                System.out.println("Highlighting key for note: " + note); // Debug
            }
        });
    }

    private void unhighlightKey(int note) {
        SwingUtilities.invokeLater(() -> {
            JButton key = noteToKeyMap.get(note);
            if (key != null) {
                key.getModel().setPressed(false);
                key.getModel().setArmed(false);
                System.out.println("Unhighlighting key for note: " + note); // Debug
            }
        });
    }

    private void playNote(int note) {
        System.out.println("Playing note: " + note);
        if (Objects.nonNull(statusConsumer)) {
            Player activePlayer = PlayerManager.getInstance().getActivePlayer();
            String playerInfo = activePlayer != null ? 
                                " through " + activePlayer.getName() : 
                                " (no active player)";
            statusConsumer.setStatus("Playing note " + note + playerInfo);
        }
        
        // Send MIDI note to active player
        boolean notePlayed = PlayerManager.getInstance().sendNoteToActivePlayer(note);
        
        // Visual feedback based on success
        if (!notePlayed) {
            // Flash the key briefly with red to indicate failure
            JButton key = noteToKeyMap.get(note);
            if (key != null) {
                Color originalBackground = key.getBackground();
                key.setBackground(new Color(255, 100, 100));
                
                // Reset after brief delay
                new Timer(150, e -> {
                    key.setBackground(originalBackground);
                    ((Timer) e.getSource()).stop();
                }).start();
            }
        }
    }

    private void stopNote(int note) {
        System.out.println("Stopping note: " + note);
        if (Objects.nonNull(statusConsumer)) {
            statusConsumer.setStatus("");
        }
        
        // For held notes we don't want to send MIDI note-off since the PlayerManager
        // already schedules note-off. If we implement longer held notes, we'd need
        // to modify PlayerManager to support this.
    }

    private void releaseNoteAfterDelay(int note) {
        // Schedule note release after brief delay
        new Timer(150, e -> {
            if (!heldNotes.contains(note)) {
                handleKeyRelease(note);
            }
            ((Timer) e.getSource()).stop();
        }).start();
    }

    private JButton createPianoKey(boolean isWhite, String note) {
        JButton key = new JButton();
        key.setUI(new BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int w = c.getWidth();
                int h = c.getHeight();

                boolean isPressed = ((JButton) c).getModel().isPressed();
                boolean isHeld = heldNotes.contains(getNoteForKey((JButton) c));

                // Traditional colors with better contrast
                if (isWhite) {
                    // White keys
                    if (isPressed || isHeld) {
                        g2d.setColor(new Color(180, 180, 180)); // Darker when pressed
                    } else {
                        g2d.setColor(new Color(248, 248, 248)); // Slightly off-white
                    }
                    g2d.fillRect(0, isPressed ? 2 : 0, w, h);

                    if (!isPressed && !isHeld) {
                        // Top shadow for 3D effect
                        g2d.setColor(new Color(255, 255, 255));
                        g2d.fillRect(0, 0, w, 5);
                    }
                } else {
                    // Black keys
                    if (isPressed || isHeld) {
                        g2d.setColor(new Color(100, 100, 100)); // Lighter when pressed
                    } else {
                        g2d.setColor(new Color(40, 40, 40)); // Dark but not pure black
                    }
                    g2d.fillRect(0, 0, w, h);

                    // Add subtle gradient for better visibility
                    if (!isPressed && !isHeld) {
                        for (int i = 0; i < 10; i++) {
                            int alpha = 15 + (i * 5);
                            g2d.setColor(new Color(255, 255, 255, alpha));
                            g2d.fillRect(0, h - 10 + i, w, 1);
                        }
                    }
                }

                // Border colors
                g2d.setColor(isWhite ? new Color(180, 180, 180) : new Color(0, 0, 0));
                g2d.drawRect(0, 0, w - 1, h - 1);

                // Draw note labels on white keys
                if (isWhite) {
                    g2d.setColor(Color.GRAY);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 10));
                    FontMetrics fm = g2d.getFontMetrics();
                    int noteWidth = fm.stringWidth(note);
                    g2d.drawString(note, (w - noteWidth) / 2, h - 15);
                }

                g2d.dispose();
            }
        });

        // Add press effect
        key.setPressedIcon(null);
        key.setContentAreaFilled(false);
        key.setBorderPainted(false);
        key.setFocusPainted(false);
        key.setToolTipText(note);
        
        // Add mouse listeners to handle mouse clicks on piano keys
        key.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                Integer noteValue = getNoteForKey(key);
                if (noteValue != null) {
                    if (e.isShiftDown()) {
                        // Shift+click acts like a key hold
                        handleKeyHold(noteValue);
                    } else {
                        // Regular click acts like a key press
                        handleKeyPress(noteValue);
                    }
                }
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                Integer noteValue = getNoteForKey(key);
                if (noteValue != null && !heldNotes.contains(noteValue)) {
                    // Only release if not in held notes list
                    handleKeyRelease(noteValue);
                }
            }
        });

        return key;
    }

    private Integer getNoteForKey(JButton key) {
        for (Map.Entry<Integer, JButton> entry : noteToKeyMap.entrySet()) {
            if (entry.getValue() == key) {
                return entry.getKey();
            }
        }
        return null;
    }

    private void configureToggleButton(JButton button) {
        Color defaultColor = button.getBackground();
        button.addActionListener(e -> {
            if (activeButton == button) {
                // Deactivate if clicking the active button
                button.setBackground(defaultColor);
                activeButton = null;
                // Release all held scale notes when deactivating first button
                if (button == followScaleBtn)
                    releaseAllNotes();

            } else {
                // Restore previous active button's color
                if (activeButton != null) {
                    activeButton.setBackground((Color) activeButton.getClientProperty("defaultColor"));
                }
                // Activate new button
                button.putClientProperty("defaultColor", defaultColor);
                button.setBackground(Color.GREEN);
                activeButton = button;

                // If it's the first button, apply current scale
                if (button == followScaleBtn)
                    releaseAllNotes();
            }
        });
        button.putClientProperty("defaultColor", defaultColor);
    }

    private void applyCurrentScale() {
        // First release any previously held notes
        releaseAllNotes();
        
        // Get the new scale pattern based on current root and scale type
        Boolean[] scaleNotes = Scale.getScale(currentRoot, currentScale);
        
        // Collect the MIDI notes that are part of the scale
        List<Integer> scaleNotesList = new ArrayList<>();
        
        // Map scale positions to MIDI notes (starting from middle C = 60)
        for (int i = 0; i < scaleNotes.length; i++) {
            if (scaleNotes[i]) {
                int midiNote = 60 + i; // Middle C (60) plus scale position
                if (noteToKeyMap.containsKey(midiNote)) {
                    // Mark as held and highlight
                    heldNotes.add(midiNote);
                    highlightKey(midiNote);
                    scaleNotesList.add(midiNote);
                }
            }
        }
        
        // Play the notes as a chord if the scale button is active
        if (activeButton == followScaleBtn && !scaleNotesList.isEmpty()) {
            // Update status to show what's being played
            if (Objects.nonNull(statusConsumer)) {
                statusConsumer.setStatus(String.format("Playing %s %s", currentRoot, currentScale));
            }
            
            // Play each note in the scale with slight delay for arpeggio effect
            final int[] delayMs = {0};
            Timer chordTimer = new Timer(30, null);
            chordTimer.addActionListener(e -> {
                if (delayMs[0] < scaleNotesList.size()) {
                    int noteToPlay = scaleNotesList.get(delayMs[0]);
                    playNote(noteToPlay);
                    delayMs[0]++;
                } else {
                    // Stop the timer once all notes have been played
                    ((Timer)e.getSource()).stop();
                }
            });
            chordTimer.start();
        }
    }

    private void releaseAllNotes() {
        new HashSet<>(heldNotes).forEach(note -> {
            heldNotes.remove(note);
            unhighlightKey(note);
        });
    }

    // Optional: Add player state indicator to the panel
    private JLabel playerStatusIndicator;

    // Add this to the constructor after other UI elements are set up
    private void setupPlayerStatusIndicator() {
        playerStatusIndicator = new JLabel("●");
        playerStatusIndicator.setFont(new Font("Arial", Font.BOLD, 12));
        playerStatusIndicator.setForeground(Color.RED); // Default to red (no active player)
        playerStatusIndicator.setBounds(getWidth() - 15, getHeight() - 15, 10, 10);
        playerStatusIndicator.setToolTipText("No active player selected");
        add(playerStatusIndicator);
        
        // Update the indicator when player selection changes
        commandBus.register(new BusListener() {
            @Override
            public void onAction(Command action) {
                switch (action.getCommand()) {
                    case Commands.PLAYER_SELECTED:
                    case Commands.PLAYER_UNSELECTED:
                        updatePlayerStatusIndicator();
                        break;
                }
            }
        });
        
        // Set initial state
        updatePlayerStatusIndicator();
    }

    private void updatePlayerStatusIndicator() {
        SwingUtilities.invokeLater(() -> {
            Player activePlayer = PlayerManager.getInstance().getActivePlayer();
            boolean hasActivePlayer = activePlayer != null;
            playerStatusIndicator.setForeground(hasActivePlayer ? Color.GREEN : Color.RED);
            playerStatusIndicator.setToolTipText(hasActivePlayer ? 
                    "Active player: " + activePlayer.getName() :
                    "No active player selected");
        });
    }
}
