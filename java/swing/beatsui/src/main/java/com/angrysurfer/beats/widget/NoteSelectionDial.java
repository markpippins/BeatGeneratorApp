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

import com.angrysurfer.beats.ColorUtils;

public class NoteSelectionDial extends Dial {
    private static final String[] NOTE_NAMES = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"};
    private static final int DETENT_COUNT = 12;
    private static final double SNAP_THRESHOLD = 0.2; // Radians
    private static final double START_ANGLE = -90; // Start at top (-90 degrees)
    private static final double TOTAL_ARC = 300;    // Degrees
    private static final int NOTES_PER_OCTAVE = 12;
    private static final double DEGREES_PER_DETENT = 360.0 / DETENT_COUNT; // 30 degrees per note
    
    private int currentDetent = 0;
    private boolean snapping = false;
    private int baseOctave = 0;  // Store the octave
    private boolean infiniteTurn = true; // Allow wrapping around

    // Add drag starting position
    private double startAngle = 0;
    private boolean isDragging = false;

    public NoteSelectionDial() {
        super();
        setMinimum(0);
        setMaximum(11);
        setValue(0, false);
        
        // Set preferred size to ensure room for labels
        setPreferredSize(new Dimension(120, 120));
        setMinimumSize(new Dimension(100, 100));

        // Add mouse pressed listener to capture start position
        addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (!isEnabled()) return;
                isDragging = true;
                Point center = new Point(getWidth() / 2, getHeight() / 2);
                startAngle = Math.atan2(e.getY() - center.y, e.getX() - center.x);
            }

            public void mouseReleased(java.awt.event.MouseEvent e) {
                isDragging = false;
            }
        });

        // Modified mouse motion listener
        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent e) {
                if (!isEnabled() || !isDragging) return;
                
                Point center = new Point(getWidth() / 2, getHeight() / 2);
                double currentAngle = Math.atan2(e.getY() - center.y, e.getX() - center.x);
                
                // Calculate angle change
                double angleDelta = currentAngle - startAngle;
                if (angleDelta > Math.PI) angleDelta -= 2 * Math.PI;
                if (angleDelta < -Math.PI) angleDelta += 2 * Math.PI;
                
                // Convert to degrees and calculate detent position
                double angleDegrees = Math.toDegrees(angleDelta);
                int detentDelta = (int) Math.round(angleDegrees / DEGREES_PER_DETENT);
                
                // Calculate new detent position
                int newDetent = currentDetent + detentDelta;
                
                if (infiniteTurn) {
                    // Wrap around within octave
                    newDetent = ((newDetent % DETENT_COUNT) + DETENT_COUNT) % DETENT_COUNT;
                } else {
                    // Constrain to range
                    newDetent = Math.min(Math.max(newDetent, 0), DETENT_COUNT - 1);
                }
                
                if (newDetent != currentDetent) {
                    int oldValue = getValue();
                    currentDetent = newDetent;
                    
                    // Calculate new MIDI note while preserving octave
                    int newNote = (baseOctave * NOTES_PER_OCTAVE) + currentDetent;
                    
                    // Use fireStateChanged() instead of directly accessing changeListener
                    if (oldValue != newNote) {
                        fireStateChanged();
                    }
                    
                    startAngle = currentAngle; // Update start angle for next movement
                    repaint();
                }
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h) - 20; // Add padding for labels
        int margin = size / 8;
        
        // Center the dial
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        
        // Draw base dial
        g2d.setColor(ColorUtils.warmMustard);
        g2d.fillOval(x + margin, y + margin, size - 2*margin, size - 2*margin);
        
        // Draw outer ring
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(x + margin, y + margin, size - 2*margin, size - 2*margin);
        
        // Draw detent markers and labels
        g2d.setColor(Color.LIGHT_GRAY);
        Font font = new Font("SansSerif", Font.PLAIN, size / 10);
        g2d.setFont(font);
        
        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double radius = (size - 2*margin) / 2.0;
        
        for (int i = 0; i < DETENT_COUNT; i++) {
            // Calculate evenly spaced angles
            double angle = Math.toRadians(START_ANGLE + (i * DEGREES_PER_DETENT));
            
            // Draw detent marker
            Point2D p1 = new Point2D.Double(
                centerX + Math.cos(angle) * (radius - margin),
                centerY + Math.sin(angle) * (radius - margin)
            );
            Point2D p2 = new Point2D.Double(
                centerX + Math.cos(angle) * radius,
                centerY + Math.sin(angle) * radius
            );
            
            // Highlight current detent
            if (i == currentDetent) {
                g2d.setColor(Color.YELLOW);
                g2d.setStroke(new BasicStroke(2f));
            } else {
                g2d.setColor(Color.LIGHT_GRAY);
                g2d.setStroke(new BasicStroke(1f));
            }
            
            // Fix: Add missing Y coordinate for p2
            g2d.drawLine(
                (int)p1.getX(), 
                (int)p1.getY(), 
                (int)p2.getX(), 
                (int)p2.getY()
            );
            
            // Draw note label
            Point2D labelPos = new Point2D.Double(
                centerX + Math.cos(angle) * (radius + margin * 1.2),
                centerY + Math.sin(angle) * (radius + margin * 1.2)
            );
            
            FontMetrics fm = g2d.getFontMetrics();
            String label = NOTE_NAMES[i];
            int labelW = fm.stringWidth(label);
            int labelH = fm.getHeight();
            
            g2d.drawString(label, 
                (int)(labelPos.getX() - labelW/2), 
                (int)(labelPos.getY() + labelH/4));
        }
        
        // Update pointer angle calculation
        double pointerAngle = Math.toRadians(START_ANGLE + (currentDetent * DEGREES_PER_DETENT));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(isEnabled() ? Color.RED : Color.GRAY);
        g2d.drawLine(
            (int)centerX, 
            (int)centerY,
            (int)(centerX + Math.cos(pointerAngle) * (radius - margin/2)),
            (int)(centerY + Math.sin(pointerAngle) * (radius - margin/2))
        );
        
        g2d.dispose();
    }

    @Override
    public int getValue() {
        // Return the absolute MIDI note number
        return (baseOctave * NOTES_PER_OCTAVE) + super.getValue();
    }

    // Add method to get just the note within the octave
    public int getNoteInOctave() {
        return super.getValue();
    }

    // Add method to get current octave
    public int getOctave() {
        return baseOctave;
    }

    // Helper method to get note name with octave
    public String getFullNoteName() {
        return String.format("%s%d", NOTE_NAMES[getNoteInOctave()], baseOctave);
    }

    @Override
    public void setValue(int absoluteNote, boolean notify) {
        // Calculate octave and note within octave
        baseOctave = absoluteNote / NOTES_PER_OCTAVE;
        int noteInOctave = absoluteNote % NOTES_PER_OCTAVE;
        
        // Set the dial position to the note within the octave
        super.setValue(noteInOctave, notify);
        
        // logger.info(String.format("Set note %d (octave %d, note %d)", 
        //     absoluteNote, baseOctave, noteInOctave));
    }

    public String getCurrentNoteName() {
        return NOTE_NAMES[currentDetent];
    }

    public void setInfiniteTurn(boolean infinite) {
        this.infiniteTurn = infinite;
    }

    public boolean isInfiniteTurn() {
        return infiniteTurn;
    }
}