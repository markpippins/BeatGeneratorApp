package com.angrysurfer.beats.widget;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Scale;

import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Synthesizer;

public class NoteSelectionDial extends Dial {

    private static final Logger logger = LoggerFactory.getLogger(NoteSelectionDial.class.getName());

    private static final String[] NOTE_NAMES = { "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B" };
    private static final int DETENT_COUNT = 12;
    private static final double SNAP_THRESHOLD = 0.2;
    private static final double START_ANGLE = -90; // Start at top (-90 degrees)
    private static final double TOTAL_ARC = 360; // Full circle
    private static final int NOTES_PER_OCTAVE = 12;
    private static final double DEGREES_PER_DETENT = 360.0 / DETENT_COUNT;

    // Store both the note position (0-11) and the full MIDI note value
    private int currentDetent = 0; // Note within octave (0-11)
    private int midiNote = 60; // Full MIDI note (0-127)
    private int octave = 4; // Current octave (default to middle C = C4)

    private boolean isDragging = false;
    private double startAngle = 0;
    private boolean infiniteTurn = true;

    private Synthesizer synthesizer;

    public NoteSelectionDial() {
        super();
        setMinimum(0);
        setMaximum(127); // Full MIDI range
        setValue(60, false); // Default to middle C (MIDI note 60)

        setPreferredSize(new Dimension(90, 90));
        setMinimumSize(new Dimension(90, 90));

        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!isEnabled())
                    return;
                isDragging = true;
                Point center = new Point(getWidth() / 2, getHeight() / 2);
                startAngle = Math.atan2(e.getY() - center.y, e.getX() - center.x);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                isDragging = false;
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (!isEnabled() || !isDragging)
                    return;

                Point center = new Point(getWidth() / 2, getHeight() / 2);
                double currentAngle = Math.atan2(e.getY() - center.y, e.getX() - center.x);

                double angleDelta = currentAngle - startAngle;
                if (angleDelta > Math.PI)
                    angleDelta -= 2 * Math.PI;
                if (angleDelta < -Math.PI)
                    angleDelta += 2 * Math.PI;

                double angleDegrees = Math.toDegrees(angleDelta);
                int detentDelta = (int) Math.round(angleDegrees / DEGREES_PER_DETENT);

                if (detentDelta != 0) {
                    // Calculate new note position (0-11) within the octave
                    int newDetent = (currentDetent + detentDelta) % NOTES_PER_OCTAVE;
                    if (newDetent < 0)
                        newDetent += NOTES_PER_OCTAVE;

                    // Only update if the note changes
                    if (newDetent != currentDetent) {
                        currentDetent = newDetent;

                        // Calculate new MIDI note preserving octave
                        int newMidiNote = Scale.getMidiNote(NOTE_NAMES[currentDetent], octave);

                        // Store old value for change detection
                        int oldValue = midiNote;
                        midiNote = newMidiNote;

                        // Update the visual representation
                        startAngle = currentAngle;
                        repaint();

                        // Fire change events if value changed
                        if (oldValue != newMidiNote) {
                            // Change this line:
                            // super.setValue(newMidiNote, true);

                            // To this:
                            NoteSelectionDial.this.setValue(newMidiNote, true);

                            logger.debug("Note changed: {} (MIDI {})", NOTE_NAMES[currentDetent] + octave, newMidiNote);
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void updateSize() {
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - 20;
        int margin = size / 8;

        // Center coordinates
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double radius = (size - 2 * margin) / 2.0;

        // Draw dial background
        g2d.setColor(ColorUtils.warmMustard);
        g2d.fillOval(x + margin, y + margin, size - 2 * margin, size - 2 * margin);

        // Draw outer ring
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x + margin, y + margin, size - 2 * margin, size - 2 * margin);

        // Draw detent markers and labels
        Font font = new Font("SansSerif", Font.PLAIN, size / 10);
        g2d.setFont(font);

        for (int i = 0; i < NOTES_PER_OCTAVE; i++) {
            double angle = Math.toRadians(START_ANGLE + (i * DEGREES_PER_DETENT));

            // Calculate marker points
            Point2D p1 = new Point2D.Double(centerX + Math.cos(angle) * (radius - margin),
                    centerY + Math.sin(angle) * (radius - margin));
            Point2D p2 = new Point2D.Double(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius);

            // Highlight current note
            if (i == currentDetent) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2f));
            } else {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.setStroke(new BasicStroke(1f));
            }

            // Draw marker line
            g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());

            // Draw note name
            Point2D labelPos = new Point2D.Double(centerX + Math.cos(angle) * (radius + margin * 1.2),
                    centerY + Math.sin(angle) * (radius + margin * 1.2));

            FontMetrics fm = g2d.getFontMetrics();
            String label = NOTE_NAMES[i];
            int labelW = fm.stringWidth(label);
            int labelH = fm.getHeight();

            g2d.drawString(label, (int) (labelPos.getX() - labelW / 2), (int) (labelPos.getY() + labelH / 4));
        }

        // Draw pointer
        double pointerAngle = Math.toRadians(START_ANGLE + (currentDetent * DEGREES_PER_DETENT));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(isEnabled() ? Color.RED : Color.GRAY);
        g2d.drawLine((int) centerX, (int) centerY, (int) (centerX + Math.cos(pointerAngle) * (radius - margin / 2)),
                (int) (centerY + Math.sin(pointerAngle) * (radius - margin / 2)));

        // Draw octave indicator in center
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, size / 6));
        String octaveText = String.valueOf(octave);
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(octaveText, (int) (centerX - fm.stringWidth(octaveText) / 2),
                (int) (centerY + fm.getHeight() / 4));

        g2d.dispose();
    }

    @Override
    public int getValue() {
        return midiNote; // Return the full MIDI note value
    }

    public int getNoteInOctave() {
        return currentDetent;
    }

    public int getOctave() {
        return octave;
    }

    @Override
    public void setValue(int midiNoteValue, boolean notify) {
        // Ensure value is within MIDI range
        midiNoteValue = Math.max(0, Math.min(127, midiNoteValue));

        // Extract octave and note information
        octave = Scale.getOctave(midiNoteValue);
        currentDetent = midiNoteValue % NOTES_PER_OCTAVE;
        midiNote = midiNoteValue;

        // Set the base class value
        super.setValue(midiNoteValue, notify);

        logger.debug("Set note: {} (MIDI {}, octave {}, position {})", NOTE_NAMES[currentDetent] + octave, midiNote,
                octave, currentDetent);

        repaint();
    }

    /**
     * Changes just the note without changing the octave
     */
    public void setNoteOnly(String noteName, boolean notify) {
        // Find the index of the note name
        int noteIndex = -1;
        for (int i = 0; i < NOTE_NAMES.length; i++) {
            if (NOTE_NAMES[i].equals(noteName)) {
                noteIndex = i;
                break;
            }
        }

        if (noteIndex != -1) {
            // Calculate new MIDI note preserving octave
            int newMidiNote = Scale.getMidiNote(noteName, octave);
            setValue(newMidiNote, notify);
        }
    }

    /**
     * Changes just the octave without changing the note
     */
    public void setOctaveOnly(int newOctave, boolean notify) {
        // Ensure octave is in valid range (typically -1 to 9 for MIDI)
        newOctave = Math.max(-1, Math.min(9, newOctave));

        // Calculate new MIDI note with same note but new octave
        int newMidiNote = Scale.getMidiNote(NOTE_NAMES[currentDetent], newOctave);
        setValue(newMidiNote, notify);
    }

    /**
     * Gets the note name with octave (e.g., "C4")
     */
    public String getNoteWithOctave() {
        return NOTE_NAMES[currentDetent] + octave;
    }

    /**
     * Gets the current note name without octave (e.g., "C#")
     */
    public String getCurrentNoteName() {
        return NOTE_NAMES[currentDetent];
    }

    public void setInfiniteTurn(boolean infinite) {
        this.infiniteTurn = infinite;
    }

    public boolean isInfiniteTurn() {
        return infiniteTurn;
    }

    /**
     * Play a note on the X0X synthesizer
     * 
     * @param note MIDI note number (0-127)
     * @param velocity Velocity (0-127)
     * @param durationMs Duration in milliseconds
     */
    public void playNote(int note, int velocity, int durationMs) {
        if (synthesizer != null && synthesizer.isOpen()) {
            try {
                // Play on channel 16 (index 15)
                MidiChannel channel = synthesizer.getChannels()[15];
                
                if (channel != null) {
                    // Start the note
                    channel.noteOn(note, velocity);
                    
                    // Schedule note off
                    java.util.Timer timer = new java.util.Timer();
                    timer.schedule(new java.util.TimerTask() {
                        @Override
                        public void run() {
                            channel.noteOff(note);
                            timer.cancel();
                        }
                    }, durationMs);
                }
            } catch (Exception e) {
                System.err.println("Error playing note: " + e.getMessage());
            }
        }
    }

    /**
     * Clean up resources
     */
    public void dispose() {
        // Close synthesizer if open
        if (synthesizer != null && synthesizer.isOpen()) {
            synthesizer.close();
            System.out.println("Closed synthesizer");
        }
        
    }
}