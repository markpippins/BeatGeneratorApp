package com.angrysurfer.beats.panel.modulation.oscillator;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Path2D;

/**
 * Panel for visualizing the live waveform
 */
public class LiveWaveformPanel extends JPanel {
    private static final int HISTORY_SIZE = 500; // Number of points to keep in history
    private final java.util.Deque<Double> valueHistory = new java.util.ArrayDeque<>(HISTORY_SIZE);

    public LiveWaveformPanel() {
        setBackground(Color.BLACK);
        // Initialize with zeros
        for (int i = 0; i < HISTORY_SIZE; i++) {
            valueHistory.addLast(0.0);
        }
    }

    public void addValue(double value) {
        // Add new value and remove oldest if full
        valueHistory.addLast(value);
        if (valueHistory.size() > HISTORY_SIZE) {
            valueHistory.removeFirst();
        }
        repaint(); // Trigger redraw with new data
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        int width = getWidth();
        int height = getHeight();
        int midY = height / 2;

        // Anti-aliasing for smoother lines
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw center line
        g2d.setColor(new Color(40, 40, 40));
        g2d.drawLine(0, midY, width, midY);

        // Draw horizontal grid lines at 25%, 50%, 75%
        g2d.setColor(new Color(30, 30, 30));
        g2d.drawLine(0, midY - height / 4, width, midY - height / 4);
        g2d.drawLine(0, midY + height / 4, width, midY + height / 4);

        // Draw waveform from history
        g2d.setColor(new Color(255, 100, 100)); // Different color for live waveform
        g2d.setStroke(new BasicStroke(2f));

        Path2D.Double path = new Path2D.Double();
        boolean first = true;

        // Convert history to array for easier indexing
        Double[] values = valueHistory.toArray(new Double[0]);

        for (int i = 0; i < values.length; i++) {
            // Calculate x position (newest values on the right)
            double x = ((double) i / values.length) * width;

            // Calculate y position (invert since Y grows downward)
            double y = midY - (values[i] * height / 2);

            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }

        g2d.draw(path);
        g2d.dispose();
    }
}
