package com.angrysurfer.beats.panel;

import com.angrysurfer.beats.panel.session.SessionPanel;
import com.angrysurfer.beats.util.UIHelper;
import com.angrysurfer.beats.widget.ColorAnimator;
import com.angrysurfer.core.api.*;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.sequencer.Scale;
import com.angrysurfer.core.service.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Timer;
import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

public class PianoPanel extends LivePanel {
    private static final String DEFAULT_ROOT = "C";
    private static final Logger logger = LoggerFactory.getLogger(PianoPanel.class.getName());
    private final Set<Integer> heldNotes = new HashSet<>();
    private final ColorAnimator colorAnimator;
    private final Map<Integer, JButton> noteToKeyMap = new HashMap<>();
    private final JButton followScaleBtn;
    private String currentRoot = DEFAULT_ROOT; // Add root note tracking
    private String currentScale = "Chromatic"; // Default scale
    private JButton activeButton = null; // Add this field to track active button
    private int currentOctave = 5; // Default octave (C5 = MIDI note 60)
    // Optional: Add player state indicator to the panel
    private JLabel playerStatusIndicator;

    public PianoPanel() {
        super(); // Remove statusConsumer parameter
        // Fix #1: Make preferred size larger and consistent with minimum size
        setPreferredSize(new Dimension(280, 80)); // Increased from 255x60
        setMinimumSize(new Dimension(280, 80));   // Increased from 265x60
        setBorder(BorderFactory.createEmptyBorder(5, 2, 5, 2)); // Reduced top/bottom padding
        setOpaque(true);
        setBackground(UIHelper.fadedOrange);

        // Fix #2: Position buttons with more room
        int buttonWidth = 25;
        int buttonHeight = 15;
        int startX = getPreferredSize().width - buttonWidth - 10; // More space from edge
        int startY = 10; // Moved up slightly
        int spacing = 5;

        followScaleBtn = new JButton();
        followScaleBtn.setBounds(startX, startY, buttonWidth, buttonHeight);
        followScaleBtn.setBackground(UIHelper.coolBlue);

        configureToggleButton(followScaleBtn);

        JButton button2 = new JButton();
        button2.setBounds(startX, startY + buttonHeight + spacing, buttonWidth, buttonHeight);
        button2.setBackground(UIHelper.warmMustard);
        configureToggleButton(button2);

        JButton button3 = new JButton();
        button3.setBounds(startX, startY + (buttonHeight + spacing) * 2, buttonWidth, buttonHeight);
        button3.setBackground(UIHelper.fadedOrange);
        configureToggleButton(button3);

        add(followScaleBtn);
        add(button2);
        add(button3);

        // Fix #3: Adjust piano key dimensions and positioning
        int whiteKeyWidth = 32; // Slightly wider
        int whiteKeyHeight = 70; // Slightly taller
        int blackKeyWidth = 18;
        int blackKeyHeight = 40; // Slightly taller

        // Fix #4: More space for white keys and consistent positioning
        String[] whiteNotes = {"C", "D", "E", "F", "G", "A", "B"};
        int[] whiteNoteValues = {60, 62, 64, 65, 67, 69, 71}; // MIDI note values
        for (int i = 0; i < 7; i++) {
            JButton whiteKey = createPianoKey(true, whiteNotes[i]);
            whiteKey.setBounds(i * whiteKeyWidth + 10, 5, whiteKeyWidth - 2, whiteKeyHeight);
            add(whiteKey);
            noteToKeyMap.put(whiteNoteValues[i], whiteKey); // Map MIDI note to key
        }

        // Fix #5: Correct positioning of black keys
        String[] blackNotes = {"C#", "D#", "", "F#", "G#", "A#", ""};
        int[] blackNoteValues = {61, 63, -1, 66, 68, 70, -1}; // MIDI note values
        for (int i = 0; i < 7; i++) {
            if (!blackNotes[i].isEmpty()) {
                JButton blackKey = createPianoKey(false, blackNotes[i]);
                // Position black keys precisely between white keys
                int xPosition = (i * whiteKeyWidth) + (whiteKeyWidth / 2) - (blackKeyWidth / 2) + 10;
                blackKey.setBounds(xPosition, 5, blackKeyWidth, blackKeyHeight);
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
        setupKeyboardNavigation();
    }

    @Override
    public void handlePlayerActivated() {

    }

    @Override
    public void handlePlayerUpdated() {

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

    // Update setupActionBusListener method
    private void setupActionBusListener() {
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == null)
                    return;

                switch (action.getCommand()) {

                    case Commands.KEY_PRESSED -> {
                        if (action.getData() instanceof Integer note) {
                            logger.info("Piano panel received KEY_PRESSED: " + note);
                            handleKeyPress(note);
                        }
                    }
                    case Commands.KEY_HELD -> {
                        if (action.getData() instanceof Integer note) {
                            logger.info("Piano panel received KEY_HELD: " + note);
                            handleKeyHold(note);
                        }
                    }
                    case Commands.KEY_RELEASED -> {
                        if (action.getData() instanceof Integer note) {
                            logger.info("Piano panel received KEY_RELEASED: " + note);
                            handleKeyRelease(note);
                        }
                    }
                    // Cases for player and scale interaction
                    case Commands.PLAYER_ACTIVATED -> {
                        if (action.getData() instanceof Player player && player.getRootNote() != null) {
                            int note = player.getRootNote().intValue();
                            logger.info("Piano panel: Player selected with note " + note);
                            updateOctave(note);
                        }
                    }
                    case Commands.NEW_VALUE_NOTE -> {
                        if (action.getSender() instanceof SessionPanel &&
                                action.getData() instanceof Integer note) {
                            logger.info("Piano panel: Note value changed to " + note);
                            updateOctave(note);
                        }
                    }
                    case Commands.SCALE_SELECTED -> {
                        if (activeButton == followScaleBtn && action.getData() instanceof String scaleName) {
                            currentScale = scaleName;
                            logger.info("Piano panel: Applying scale " + scaleName + " in octave " + currentOctave);
                            applyCurrentScale();
                        }
                    }
                    case Commands.ROOT_NOTE_SELECTED -> {
                        if (action.getData() instanceof String rootNote) {
                            currentRoot = rootNote;
                            logger.info("Piano panel: Root note changed to " + rootNote);
                            applyCurrentScale();
                        }
                    }
                }
            }
        }, new String[]{
                Commands.KEY_PRESSED,
                Commands.KEY_HELD,
                Commands.KEY_RELEASED,
                Commands.PLAYER_ACTIVATED,
                Commands.NEW_VALUE_NOTE,
                Commands.SCALE_SELECTED,
                Commands.ROOT_NOTE_SELECTED
        });
    }

    private void handleKeyPress(int note) {
        if (!heldNotes.contains(note)) {
            // Update status bar using CommandBus
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(getClass().getSimpleName(), "Playing", "Playing note " + note)
            );

            // Visual feedback
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

        if (getPlayer() == null)
            logger.info("No player available");

        // Toggle behavior: if note is already held, release it
        if (heldNotes.contains(note)) {
            heldNotes.remove(note);
            unhighlightKey(note);
            // We don't need to send MIDI note-off here since PlayerManager
            // already scheduled it

            // Clear status
            CommandBus.getInstance().publish(Commands.CLEAR_STATUS, this, null);
        } else {
            // Add to held notes and play
            String playerInfo = getPlayer() != null ? " through " + getPlayer().getName() : " (no active player)";

            // Update status using CommandBus
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(getClass().getSimpleName(), "Holding note " + note + playerInfo, null)
            );

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

            // Clear status
            CommandBus.getInstance().publish(Commands.CLEAR_STATUS, this, null);
        }
    }

    private void highlightKey(int note) {
        SwingUtilities.invokeLater(() -> {
            JButton key = noteToKeyMap.get(note);
            if (key != null) {
                key.getModel().setPressed(true);
                key.getModel().setArmed(true);
            }
        });
    }

    private void unhighlightKey(int note) {
        SwingUtilities.invokeLater(() -> {
            JButton key = noteToKeyMap.get(note);
            if (key != null) {
                key.getModel().setPressed(false);
                key.getModel().setArmed(false);
            }
        });
    }

    private void playNote(int note) {
        // Update status with player information
        String playerInfo = getPlayer() != null ? " through " + getPlayer().getName() : " (no active player)";

        CommandBus.getInstance().publish(
                Commands.STATUS_UPDATE,
                this,
                new StatusUpdate(getClass().getSimpleName(), "Playing note " + note + playerInfo, null)
        );

        if (SessionManager.getInstance().isRecording()) {
            CommandBus.getInstance().publish(Commands.NEW_VALUE_NOTE, this, note);
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this,
                    getPlayer());
        }

        // Send MIDI note to active player
//        boolean notePlayed = activePlayer.noteOn(note, activePlayer.getLevel());
//
//        // Visual feedback based on success
//        if (!notePlayed) {
//            // Flash the key briefly with red to indicate failure
//            JButton key = noteToKeyMap.get(note);
//            if (key != null) {
//                Color originalBackground = key.getBackground();
//                key.setBackground(new Color(255, 100, 100));
//
//                // Reset after brief delay
//                new Timer(150, e -> {
//                    key.setBackground(originalBackground);
//                    ((Timer) e.getSource()).stop();
//                }).start();
//            }
//        }
    }

    private void stopNote(int note) {
        // Clear status
        CommandBus.getInstance().publish(Commands.CLEAR_STATUS, this, null);

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

        // Calculate base note for the current octave and root
        int rootOffset = Scale.getRootOffset(currentRoot);
        int baseNote = currentOctave * 12 + rootOffset;

        logger.info("Applying scale in octave: " + currentOctave + ", base note: " + baseNote);

        // Map scale positions to MIDI notes
        for (int i = 0; i < scaleNotes.length; i++) {
            if (scaleNotes[i]) {
                int midiNote = baseNote + i;
                JButton key = noteToKeyMap.get(midiNote);
                if (key != null) {
                    // Mark as held and highlight
                    heldNotes.add(midiNote);
                    highlightKey(midiNote);
                    scaleNotesList.add(midiNote);
                }
            }
        }

        // Play the notes as a chord if the scale button is active
        if (activeButton == followScaleBtn && !scaleNotesList.isEmpty()) {
            // Update status using CommandBus
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(
                            getClass().getSimpleName(),
                            String.format("Playing %s %s (Octave %d)", currentRoot, currentScale, currentOctave),
                            null
                    )
            );

            // Play each note in the scale with slight delay for arpeggio effect
            final int[] delayMs = {0};
            Timer chordTimer = new Timer(30, null);
            chordTimer.addActionListener(e -> {
                if (delayMs[0] < scaleNotesList.size()) {
                    int noteToPlay = scaleNotesList.get(delayMs[0]);
                    playNote(noteToPlay);
                    delayMs[0]++;
                } else {
                    ((Timer) e.getSource()).stop();
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

    // Update the player status indicator registration in setupPlayerStatusIndicator
    private void setupPlayerStatusIndicator() {
        playerStatusIndicator = new JLabel("●");
        playerStatusIndicator.setFont(new Font("Arial", Font.BOLD, 12));
        playerStatusIndicator.setForeground(Color.RED); // Default to red (no active player)
        playerStatusIndicator.setBounds(getWidth() - 15, getHeight() - 15, 10, 10);
        playerStatusIndicator.setToolTipText("No active player selected");
        add(playerStatusIndicator);

        // Update the indicator when player selection changes
        CommandBus.getInstance().register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action.getCommand() == Commands.PLAYER_ACTIVATED) {
                    updatePlayerStatusIndicator();
                }
            }
        }, new String[]{Commands.PLAYER_ACTIVATED});

        // Set initial state
        updatePlayerStatusIndicator();
    }

    private void updatePlayerStatusIndicator() {
        SwingUtilities.invokeLater(() -> {
            boolean hasActivePlayer = getPlayer() != null;
            playerStatusIndicator.setForeground(hasActivePlayer ? Color.GREEN : Color.RED);
            playerStatusIndicator.setToolTipText(
                    hasActivePlayer ? "Active player: " + getPlayer().getName() : "No active player selected");
        });
    }

    // Add this method to update the octave and piano key mappings
    public void updateOctave(int midiNote) {
        // Calculate octave (0-10) based on MIDI note (0-127)
        int newOctave = midiNote / 12;

        // Ensure octave is in valid range
        newOctave = Math.max(0, Math.min(9, newOctave));

        logger.info("Piano panel: Updating octave based on note " + midiNote +
                " (current octave: " + currentOctave + ", new octave: " + newOctave + ")");

        // Only update if octave actually changed
        if (newOctave != currentOctave) {
            currentOctave = newOctave;

            // Update note to key mapping
            updateNoteToKeyMap();

            // If scale is active, reapply it with new octave
            if (activeButton == followScaleBtn) {
                applyCurrentScale();
            }
        }
    }

    // Method to recalculate the MIDI note values for each key based on octave
    private void updateNoteToKeyMap() {
        // Clear existing mapping
        noteToKeyMap.clear();

        // Base note for C in the current octave
        int baseNote = currentOctave * 12;

        logger.info("Updating note to key map for octave " + currentOctave + ", base note: " + baseNote);

        // Get all white and black keys
        List<JButton> whiteKeys = new ArrayList<>();
        List<JButton> blackKeys = new ArrayList<>();

        for (Component c : getComponents()) {
            if (c instanceof JButton button) {
                // Skip the command buttons on the right side
                if (button == followScaleBtn ||
                        (button.getY() < 20 && button.getX() > getWidth() - 50)) {
                    continue;
                }

                // Check if it's a white or black key by examining height
                if (button.getHeight() > 40) {
                    whiteKeys.add(button);
                } else if (button.getHeight() <= 40) {
                    blackKeys.add(button);
                }
            }
        }

        // Sort white keys by X position (left to right)
        whiteKeys.sort((a, b) -> Integer.compare(a.getX(), b.getX()));

        // Map white keys (C through B)
        int[] whiteKeyOffsets = {0, 2, 4, 5, 7, 9, 11};
        for (int i = 0; i < whiteKeys.size() && i < whiteKeyOffsets.length; i++) {
            JButton key = whiteKeys.get(i);
            int noteValue = baseNote + whiteKeyOffsets[i];
            noteToKeyMap.put(noteValue, key);

            // Update tooltip to show actual MIDI note
            String noteName = Scale.getNoteNameWithOctave(noteValue);
            key.setToolTipText(noteName + " (" + noteValue + ")");
            logger.info("Mapped white key " + i + " to note " + noteValue + " (" + noteName + ")");
        }

        // Sort black keys by X position (left to right)
        blackKeys.sort((a, b) -> Integer.compare(a.getX(), b.getX()));

        // Map black keys (C# through A#)
        int[] blackKeyOffsets = {1, 3, -1, 6, 8, 10, -1}; // -1 means no black key
        int blackKeyIdx = 0;
        for (int i = 0; i < blackKeys.size(); i++) {
            if (blackKeyIdx >= blackKeyOffsets.length)
                break;

            // Skip positions with no black keys
            while (blackKeyIdx < blackKeyOffsets.length && blackKeyOffsets[blackKeyIdx] == -1) {
                blackKeyIdx++;
            }

            if (blackKeyIdx < blackKeyOffsets.length) {
                JButton key = blackKeys.get(i);
                int noteValue = baseNote + blackKeyOffsets[blackKeyIdx];
                noteToKeyMap.put(noteValue, key);

                // Update tooltip
                String noteName = Scale.getNoteNameWithOctave(noteValue);
                key.setToolTipText(noteName + " (" + noteValue + ")");
                logger.info("Mapped black key " + i + " to note " + noteValue + " (" + noteName + ")");

                blackKeyIdx++;
            }
        }

        // Force repaint to reflect changes
        repaint();
    }

    // Add this new method for keyboard navigation
    private void setupKeyboardNavigation() {
        // Make the panel focusable so it can receive key events
        setFocusable(true);

        // Add key bindings for left and right arrow keys
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getActionMap();

        // Left arrow increases octave
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "octaveUp");
        actionMap.put("octaveUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shiftOctaveUp();
            }
        });

        // Right arrow decreases octave
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "octaveDown");
        actionMap.put("octaveDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                shiftOctaveDown();
            }
        });

        // Request focus when the panel becomes visible
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                requestFocusInWindow();
            }
        });
    }

    // Add helper methods for octave shifting
    private void shiftOctaveUp() {
        if (currentOctave < 9) { // Limit the maximum octave
            currentOctave++;
            updateNoteToKeyMap();
            logger.info("Piano panel: Octave up to " + currentOctave + " (via left arrow)");

            // Show visual feedback
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(getClass().getSimpleName(), "Octave: " + currentOctave, null)
            );

            // If scale is active, reapply it with new octave
            if (activeButton == followScaleBtn) {
                applyCurrentScale();
            }
        }
    }

    private void shiftOctaveDown() {
        if (currentOctave > 0) { // Limit the minimum octave
            currentOctave--;
            updateNoteToKeyMap();
            logger.info("Piano panel: Octave down to " + currentOctave + " (via right arrow)");

            // Show visual feedback
            CommandBus.getInstance().publish(
                    Commands.STATUS_UPDATE,
                    this,
                    new StatusUpdate(getClass().getSimpleName(), "Octave: " + currentOctave, null)
            );

            // If scale is active, reapply it with new octave
            if (activeButton == followScaleBtn) {
                applyCurrentScale();
            }
        }
    }
}
