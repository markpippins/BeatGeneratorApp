package com.angrysurfer.beats.panel;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.event.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;

/**
 * A panel for editing and visualizing Euclidean rhythm patterns
 */
public class EuclideanPatternPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(EuclideanPatternPanel.class);
    
    // Pattern parameters
    private int totalSteps = 16;
    private int filledSteps = 4;
    private int rotation = 0;
    
    // UI components
    private CircleDisplay circleDisplay;
    private JSlider stepsSlider;
    private JSlider fillsSlider;
    private Dial rotationDial;
    
    public EuclideanPatternPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create the circular display component
        circleDisplay = new CircleDisplay();
        circleDisplay.setPreferredSize(new Dimension(400, 400));
        
        // Create control panel
        JPanel controlPanel = createControlPanel();
        
        // Add components to the panel
        add(circleDisplay, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);
        
        // Update the pattern initially
        updatePattern();
    }
    
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(10, 0, 0, 0),
            BorderFactory.createTitledBorder("Euclidean Pattern Controls")
        ));
        
        // Create steps slider
        JPanel stepsPanel = new JPanel(new BorderLayout(5, 0));
        stepsPanel.add(new JLabel("Total Steps:"), BorderLayout.WEST);
        
        stepsSlider = new JSlider(JSlider.HORIZONTAL, 1, 32, totalSteps);
        stepsSlider.setMajorTickSpacing(8);
        stepsSlider.setMinorTickSpacing(1);
        stepsSlider.setPaintTicks(true);
        stepsSlider.setPaintLabels(true);
        stepsSlider.addChangeListener(e -> {
            totalSteps = stepsSlider.getValue();
            
            // Ensure filled steps doesn't exceed total steps
            if (filledSteps > totalSteps) {
                filledSteps = totalSteps;
                fillsSlider.setValue(filledSteps);
            }
            
            // Update fills slider maximum
            fillsSlider.setMaximum(totalSteps);
            
            updatePattern();
            logger.debug("Total steps changed to: {}", totalSteps);
        });
        stepsPanel.add(stepsSlider, BorderLayout.CENTER);
        
        // Create fills slider
        JPanel fillsPanel = new JPanel(new BorderLayout(5, 0));
        fillsPanel.add(new JLabel("Filled Steps:"), BorderLayout.WEST);
        
        fillsSlider = new JSlider(JSlider.HORIZONTAL, 0, totalSteps, filledSteps);
        fillsSlider.setMajorTickSpacing(4);
        fillsSlider.setMinorTickSpacing(1);
        fillsSlider.setPaintTicks(true);
        fillsSlider.setPaintLabels(true);
        fillsSlider.addChangeListener(e -> {
            filledSteps = fillsSlider.getValue();
            updatePattern();
            logger.debug("Filled steps changed to: {}", filledSteps);
        });
        fillsPanel.add(fillsSlider, BorderLayout.CENTER);
        
        // Create rotation dial panel
        JPanel rotationPanel = new JPanel();
        rotationPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        rotationPanel.add(new JLabel("Rotation:"));
        
        rotationDial = new Dial();
        rotationDial.setMinimum(0);
        rotationDial.setValue(0);
        rotationDial.setMaximum(totalSteps-1);
        rotationDial.setSize(80, 80);
        rotationDial.setPreferredSize(new Dimension(80, 80));
        rotationDial.addChangeListener(e -> {
            rotation = rotationDial.getValue();
            updatePattern();
            logger.debug("Rotation changed to: {}", rotation);
        });
        rotationPanel.add(rotationDial);
        
        // Create a sub-panel for the sliders to keep them aligned
        JPanel sliderPanel = new JPanel();
        sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.Y_AXIS));
        sliderPanel.add(stepsPanel);
        sliderPanel.add(Box.createVerticalStrut(10));
        sliderPanel.add(fillsPanel);
        
        // Add panels to the main control panel
        panel.add(sliderPanel);
        panel.add(Box.createVerticalStrut(10));
        panel.add(rotationPanel);
        
        return panel;
    }
    
    /**
     * Updates the visual pattern based on current parameters
     */
    private void updatePattern() {
        // Update rotation dial's max value based on total steps
        rotationDial.setMaximum(totalSteps > 0 ? totalSteps-1 : 0);
        
        // Ensure rotation is within bounds
        if (rotation >= totalSteps) {
            rotation = 0;
            rotationDial.setValue(0);
        }
        
        // Generate the Euclidean pattern
        boolean[] pattern = generateEuclideanPattern(totalSteps, filledSteps, rotation);
        
        // Update the visual component
        circleDisplay.setPattern(pattern);
        circleDisplay.repaint();
    }
    
    /**
     * Generates an Euclidean pattern with the given parameters
     * 
     * @param steps Total number of steps
     * @param fills Number of fills (active steps)
     * @param rotation Pattern rotation
     * @return Array representing the pattern
     */
    private boolean[] generateEuclideanPattern(int steps, int fills, int rotation) {
        if (steps <= 0) return new boolean[0];
        if (fills <= 0) return new boolean[steps]; // All false
        if (fills >= steps) {
            // All true
            boolean[] allTrue = new boolean[steps];
            for (int i = 0; i < steps; i++) {
                allTrue[i] = true;
            }
            return allTrue;
        }
        
        boolean[] pattern = new boolean[steps];
        
        // Bresenham's line algorithm adapted for even distribution
        int increment = steps / fills;
        int error = steps % fills;
        int position = 0;
        
        for (int i = 0; i < fills; i++) {
            pattern[(position + rotation) % steps] = true;
            position += increment;
            
            // Distribute the remainder evenly
            if (error > 0) {
                position++;
                error--;
            }
        }
        
        return pattern;
    }
    
    /**
     * Custom component for drawing the circular pattern display
     */
    private class CircleDisplay extends JPanel {
        private boolean[] pattern;
        
        public CircleDisplay() {
            setBackground(new Color(30, 30, 30));
            setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 2));
            pattern = new boolean[totalSteps];
        }
        
        public void setPattern(boolean[] pattern) {
            this.pattern = pattern;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Calculate dimensions
            int width = getWidth();
            int height = getHeight();
            int size = Math.min(width, height) - 40; // Leave some margin
            
            // Center the circle
            int centerX = width / 2;
            int centerY = height / 2;
            
            // Draw outer circle
            g2d.setColor(new Color(60, 60, 60));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawOval(centerX - size/2, centerY - size/2, size, size);
            
            // Draw steps - only if we have a valid pattern
            if (pattern != null && pattern.length > 0) {
                int radius = size / 2;
                int dotRadius = Math.max(5, Math.min(radius / 10, 12));
                
                // Arrays for polygon points (only filled steps)
                int[] polygonX = new int[filledSteps];
                int[] polygonY = new int[filledSteps];
                int filledCount = 0;
                
                // Draw each step position at equal angular intervals
                for (int i = 0; i < pattern.length; i++) {
                    // Calculate angle: start at top (270 degrees) and move clockwise
                    // Convert to radians and adjust for Java's coordinate system
                    double angleInRadians = Math.toRadians(270 + (360.0 * i / pattern.length));
                    
                    // Calculate position on the circle
                    int x = centerX + (int)(radius * Math.cos(angleInRadians));
                    int y = centerY + (int)(radius * Math.sin(angleInRadians));
                    
                    // Draw step number for all positions
                    g2d.setColor(Color.GRAY);
                    g2d.setFont(new Font("Sans", Font.PLAIN, 10));
                    String stepText = String.valueOf(i + 1);
                    FontMetrics fm = g2d.getFontMetrics();
                    int textWidth = fm.stringWidth(stepText);
                    int textHeight = fm.getHeight();
                    
                    // Position text slightly outside the circle
                    double textAngle = angleInRadians;
                    int textRadius = radius + 20;
                    int textX = centerX + (int)(textRadius * Math.cos(textAngle));
                    int textY = centerY + (int)(textRadius * Math.sin(textAngle));
                    
                    g2d.drawString(stepText, textX - textWidth/2, textY + textHeight/4);
                    
                    // Draw step dot - styled according to whether it's active or not
                    if (pattern[i]) {
                        // Store coordinates for the polygon (filled steps only)
                        if (filledCount < polygonX.length) {
                            polygonX[filledCount] = x;
                            polygonY[filledCount] = y;
                            filledCount++;
                        }
                        
                        // Filled step
                        g2d.setColor(new Color(120, 200, 255));
                        g2d.fillOval(x - dotRadius, y - dotRadius, dotRadius*2, dotRadius*2);
                        g2d.setColor(Color.WHITE);
                        g2d.drawOval(x - dotRadius, y - dotRadius, dotRadius*2, dotRadius*2);
                    } else {
                        // Empty step
                        g2d.setColor(new Color(80, 80, 80));
                        g2d.drawOval(x - dotRadius, y - dotRadius, dotRadius*2, dotRadius*2);
                        g2d.setColor(new Color(50, 50, 50));
                        g2d.fillOval(x - dotRadius, y - dotRadius, dotRadius*2, dotRadius*2);
                    }
                }
                
                // Draw connecting lines between all steps to show the circular structure
                g2d.setColor(new Color(80, 80, 80));
                g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 
                                            0, new float[]{1.0f, 2.0f}, 0));
                
                for (int i = 0; i < pattern.length; i++) {
                    // Calculate positions for current and next point
                    double angle1 = Math.toRadians(270 + (360.0 * i / pattern.length));
                    double angle2 = Math.toRadians(270 + (360.0 * ((i + 1) % pattern.length) / pattern.length));
                    
                    int x1 = centerX + (int)(radius * Math.cos(angle1));
                    int y1 = centerY + (int)(radius * Math.sin(angle1));
                    int x2 = centerX + (int)(radius * Math.cos(angle2));
                    int y2 = centerY + (int)(radius * Math.sin(angle2));
                    
                    g2d.drawLine(x1, y1, x2, y2);
                }
                
                // Draw the polygon connecting filled steps
                if (filledCount >= 2) {
                    g2d.setColor(new Color(120, 200, 255, 60));
                    g2d.setStroke(new BasicStroke(2));
                    g2d.drawPolygon(polygonX, polygonY, filledCount);
                    g2d.setColor(new Color(120, 200, 255, 30));
                    g2d.fillPolygon(polygonX, polygonY, filledCount);
                }
            }
            
            g2d.dispose();
        }
    }
}