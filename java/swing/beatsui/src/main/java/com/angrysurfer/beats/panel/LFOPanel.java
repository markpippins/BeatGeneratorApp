package com.angrysurfer.beats.panel;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.DoubleDial;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;

/**
 * A panel that implements a Low Frequency Oscillator with various waveform
 * types
 * and visualization.
 */
public class LFOPanel extends JPanel implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(LFOPanel.class.getName());

    // LFO parameters
    private double frequency = 1.0; // Hz
    private double amplitude = 1.0; // 0-1 range
    private double offset = 0.0; // -1 to 1 range (center position)
    private double phase = 0.0; // 0-1 range (0-360 degrees)
    private double pulseWidth = 0.5; // 0-1 range (duty cycle for pulse waves)

    // UI components
    private WaveformPanel waveformPanel;
    private LiveWaveformPanel liveWaveformPanel;
    private JComboBox<String> waveformCombo;
    private DoubleDial freqDial;
    private DoubleDial ampDial;
    private DoubleDial offsetDial;
    private DoubleDial phaseDial;
    private DoubleDial pulseWidthDial;
    private JLabel valueLabel;
    private JToggleButton runButton;
    private JSlider bipolarSlider; // Shows current value as a slider

    // Update thread
    private ScheduledExecutorService executor;
    private boolean running = false;
    private long startTimeMs = 0;

    // Current waveform type and value
    private WaveformType currentWaveform = WaveformType.SINE;
    private double currentValue = 0.0;

    // Value change listener
    private Consumer<Double> valueChangeListener;

    // Waveform types
    public enum WaveformType {
        SINE("Sine"),
        TRIANGLE("Triangle"),
        SAWTOOTH("Sawtooth"),
        SQUARE("Square"),
        PULSE("Pulse"),
        RANDOM("Random");

        private final String displayName;

        WaveformType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    /**
     * Creates a new LFO Panel
     */
    public LFOPanel() {
        super(new BorderLayout(10, 10));
        // setBorder(new EmptyBorder(10, 10, 10, 10));
        setBorder(BorderFactory.createTitledBorder("LFO"));

        // Anonymous LFO implementation
        final LFO lfo = new LFO() {
            @Override
            public double getValue(double timeInSeconds) {
                // Calculate the oscillator value based on time and current parameters
                switch (currentWaveform) {
                    case SINE:
                        return offset
                                + amplitude * Math.sin(2 * Math.PI * frequency * timeInSeconds + phase * 2 * Math.PI);

                    case TRIANGLE:
                        double triPhase = (frequency * timeInSeconds + phase) % 1.0;
                        return offset + amplitude * (triPhase < 0.5 ? 4 * triPhase - 1 : 3 - 4 * triPhase);

                    case SAWTOOTH:
                        double sawPhase = (frequency * timeInSeconds + phase) % 1.0;
                        return offset + amplitude * (2 * sawPhase - 1);

                    case SQUARE:
                        double sqrPhase = (frequency * timeInSeconds + phase) % 1.0;
                        return offset + amplitude * (sqrPhase < 0.5 ? 1 : -1);

                    case PULSE:
                        double pulsePhase = (frequency * timeInSeconds + phase) % 1.0;
                        return offset + amplitude * (pulsePhase < pulseWidth ? 1 : -1);

                    case RANDOM:
                        // Step random - changes at frequency rate
                        double randomPhase = Math.floor(frequency * timeInSeconds + phase);
                        // Use the phase as seed for deterministic randomness
                        return offset + amplitude * (2 * ((Math.sin(randomPhase * 12345.67) + 1) % 1.0) - 1);

                    default:
                        return 0.0;
                }
            }
        };

        initializeUI();
        startLFO(lfo);
    }

    private void initializeUI() {
        // Create main panels
        JPanel controlPanel = new JPanel(new BorderLayout(5, 5));
        JPanel waveformPanelContainer = new JPanel(new BorderLayout(5, 5));
        JPanel topControlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JPanel dialPanel = new JPanel(new GridLayout(1, 5, 5, 5));

        // Create waveform visualization panel - MAKE IT SMALLER
        waveformPanel = new WaveformPanel();
        waveformPanel.setPreferredSize(new Dimension(600, 120)); // Reduced height to ~1/4
        waveformPanelContainer.setBorder(BorderFactory.createTitledBorder("Waveform Shape"));
        waveformPanelContainer.add(waveformPanel, BorderLayout.CENTER);

        // Create live waveform panel to replace the value display
        liveWaveformPanel = new LiveWaveformPanel();
        liveWaveformPanel.setPreferredSize(new Dimension(600, 100));
        JPanel liveWaveformContainer = new JPanel(new BorderLayout(5, 5));
        liveWaveformContainer.setBorder(BorderFactory.createTitledBorder("Live Output"));
        liveWaveformContainer.add(liveWaveformPanel, BorderLayout.CENTER);

        // Create compact value display
        JPanel valueDisplayPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        valueLabel = new JLabel("0.00");
        valueLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        valueDisplayPanel.add(new JLabel("Current:"));
        valueDisplayPanel.add(valueLabel);
        liveWaveformContainer.add(valueDisplayPanel, BorderLayout.SOUTH);

        // Create waveform selector
        String[] waveforms = { "Sine", "Triangle", "Sawtooth", "Square", "Pulse", "Random" };
        waveformCombo = new JComboBox<>(waveforms);
        waveformCombo.setSelectedIndex(0);
        waveformCombo.addActionListener(e -> {
            currentWaveform = WaveformType.values()[waveformCombo.getSelectedIndex()];
            updateWaveformDisplay();
            logger.info("Waveform changed to: " + currentWaveform);
        });

        // Create run button
        runButton = new JToggleButton("Run");
        runButton.setSelected(true);
        runButton.addActionListener(e -> {
            running = runButton.isSelected();
            if (running) {
                startTimeMs = System.currentTimeMillis();
            }
            runButton.setText(running ? "Stop" : "Run");
        });

        // Create control dials with enhanced panels and tooltips - UPDATED STEP SIZE VALUES
        JPanel freqPanel = createDialPanel(
            "Frequency", 
            "Controls the oscillation rate in Hertz (cycles per second)",
            0.001, 20.0, frequency, 0.1, // Keep 0.01 step for frequency
            val -> {
                frequency = val;
                updateWaveformDisplay();
            });
        freqDial = findDialInPanel(freqPanel);

        JPanel ampPanel = createDialPanel(
            "Amplitude", 
            "Controls the height/strength of the waveform (0-1)",
            0.0, 1.0, amplitude, 0.01, // ULTRA-FINE STEP for finer control
            val -> {
                amplitude = val;
                updateWaveformDisplay();
            });
        ampDial = findDialInPanel(ampPanel);

        JPanel offsetPanel = createDialPanel(
            "Offset", 
            "Shifts the center position of the waveform up or down",
            -1.0, 1.0, offset, 0.01, // ULTRA-FINE STEP for finer control
            val -> {
                offset = val;
                updateWaveformDisplay();
            });
        offsetDial = findDialInPanel(offsetPanel);

        JPanel phasePanel = createDialPanel(
            "Phase", 
            "Shifts the starting position within the waveform cycle (0-360°)",
            0.0, 1.0, phase, 0.01, // SMALLER STEP for finer control
            val -> {
                phase = val;
                updateWaveformDisplay();
            });
        phaseDial = findDialInPanel(phasePanel);

        JPanel pwPanel = createDialPanel(
            "Pulse Width", 
            "Controls the duty cycle of pulse waveforms (ratio of high to low time)",
            0.01, 0.99, pulseWidth, 0.001, // ULTRA-FINE STEP for finer control
            val -> {
                pulseWidth = val;
                updateWaveformDisplay();
            });
        pulseWidthDial = findDialInPanel(pwPanel);
        
        // Add dial panels (not dials) to the dial panel
        dialPanel.add(freqPanel);
        dialPanel.add(ampPanel);
        dialPanel.add(offsetPanel);
        dialPanel.add(phasePanel);
        dialPanel.add(pwPanel);

        // Build top control panel
        topControlPanel.add(new JLabel("Waveform:"));
        topControlPanel.add(waveformCombo);
        topControlPanel.add(Box.createHorizontalStrut(20));
        topControlPanel.add(runButton);

        // Add components to main panels
        controlPanel.add(topControlPanel, BorderLayout.NORTH);
        controlPanel.add(dialPanel, BorderLayout.CENTER);
        controlPanel.add(liveWaveformContainer, BorderLayout.SOUTH); // Use the live waveform instead

        // Add components to main panel
        add(waveformPanelContainer, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        // Update waveform display initially
        updateWaveformDisplay();
    }

    /**
     * Helper method to find and extract the DoubleDial component from a panel
     */
    private DoubleDial findDialInPanel(JPanel panel) {
        for (Component c : panel.getComponents()) {
            if (c instanceof DoubleDial) {
                return (DoubleDial)c;
            } else if (c instanceof JPanel) {
                DoubleDial found = findDialInPanel((JPanel)c);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Creates a Dial with label and value display
     */
    private DoubleDial createDial(String name, double min, double max, double initial, double step,
            Consumer<Double> changeListener) {
        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.setBorder(BorderFactory.createTitledBorder(name));

        DoubleDial dial = new DoubleDial();
        dial.setMinimum(min);
        dial.setMaximum(max);
        dial.setValue(initial);
        dial.setStepSize(step);

        JLabel valueLabel = new JLabel(String.format("%.2f", initial));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        dial.addChangeListener(e -> {
            double value = dial.getValue();
            valueLabel.setText(String.format("%.2f", value));
            changeListener.accept(value);
        });

        panel.add(dial, BorderLayout.CENTER);
        panel.add(valueLabel, BorderLayout.SOUTH);

        return dial;
    }

    /**
     * Creates a Dial with proper label, value display, and tooltip
     * Updated to center the dial within its container
     */
    private JPanel createDialPanel(String name, String tooltip, double min, double max, double initial, double step,
            Consumer<Double> changeListener) {
        // Create panel with border layout
        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(name),
                BorderFactory.createEmptyBorder(5, 5, 5, 5))); // Increased padding
        
        // Create the dial
        DoubleDial dial = new DoubleDial();
        dial.setMinimum(min);
        dial.setMaximum(max);
        dial.setValue(initial);
        dial.setStepSize(step);
        dial.setToolTipText(tooltip);
        
        // Format pattern based on step size - show more decimals for finer steps
        int decimals = step < 0.01 ? 3 : 2; 
        String formatPattern = "%." + decimals + "f";
        
        // Create value label with formatted current value
        JLabel valueLabel = new JLabel(String.format(formatPattern, initial));
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        valueLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        valueLabel.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        
        // Add change listener to update value label
        dial.addChangeListener(e -> {
            double value = dial.getValue();
            valueLabel.setText(String.format(formatPattern, value));
            changeListener.accept(value);
        });
        
        // Create units label if appropriate
        JLabel unitsLabel = null;
        if (name.equals("Frequency")) {
            unitsLabel = new JLabel("Hz");
            unitsLabel.setHorizontalAlignment(SwingConstants.CENTER);
            unitsLabel.setFont(new Font("Dialog", Font.PLAIN, 10));
        }
        
        // Create center panel to hold the dial with proper centering
        JPanel dialCenterPanel = new JPanel(new GridBagLayout()); // GridBagLayout centers components
        dialCenterPanel.add(dial); // This will center the dial in the panel
        
        // Layout components
        JPanel labelPanel = new JPanel(new BorderLayout(2, 0));
        labelPanel.add(valueLabel, BorderLayout.CENTER);
        if (unitsLabel != null) {
            labelPanel.add(unitsLabel, BorderLayout.EAST);
        }
        
        // Add components to panel
        panel.add(dialCenterPanel, BorderLayout.CENTER); // Centered dial
        panel.add(labelPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    /**
     * Update the waveform display based on current parameters
     */
    private void updateWaveformDisplay() {
        waveformPanel.repaint();
    }

    /**
     * Start the LFO calculation thread
     */
    private void startLFO(LFO lfo) {
        running = true;
        startTimeMs = System.currentTimeMillis();

        // Create a scheduled executor to update the LFO value
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            if (running) {
                double timeInSeconds = (System.currentTimeMillis() - startTimeMs) / 1000.0;
                double[] newValue = { lfo.getValue(timeInSeconds) };
                // Clamp the value between -1 and 1
                newValue[0] = Math.max(-1.0, Math.min(1.0, newValue[0]));

                // Update UI on the EDT
                SwingUtilities.invokeLater(() -> {
                    currentValue = newValue[0];
                    valueLabel.setText(String.format("%.2f", newValue[0]));
                    
                    // Update the live waveform panel with the new value
                    liveWaveformPanel.addValue(newValue[0]);
                    
                    // Notify listeners if attached
                    if (valueChangeListener != null) {
                        valueChangeListener.accept(newValue[0]);
                    }

                    // Publish to command bus
                    CommandBus.getInstance().publish(
                            Commands.LFO_VALUE_CHANGED,
                            this,
                            newValue[0]);
                });
            }
        }, 0, 16, TimeUnit.MILLISECONDS); // ~60 Hz update rate
    }

    /**
     * Set a listener to be notified when the LFO value changes
     */
    public void setValueChangeListener(Consumer<Double> listener) {
        this.valueChangeListener = listener;
    }

    /**
     * Get the current LFO value
     */
    public double getCurrentValue() {
        return currentValue;
    }

    @Override
    public void close() {
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Interface for LFO calculations
     */
    private interface LFO {
        double getValue(double timeInSeconds);
    }

    /**
     * Panel for visualizing the waveform
     */
    private class WaveformPanel extends JPanel {
        private static final int SAMPLES = 500;

        public WaveformPanel() {
            setBackground(Color.BLACK);
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

            // Draw grid lines
            g2d.setColor(new Color(30, 30, 30));
            // Horizontal grid lines at 25%, 50%, 75%
            g2d.drawLine(0, midY - height / 4, width, midY - height / 4);
            g2d.drawLine(0, midY + height / 4, width, midY + height / 4);

            // Vertical grid lines at 25%, 50%, 75%
            g2d.drawLine(width / 4, 0, width / 4, height);
            g2d.drawLine(width / 2, 0, width / 2, height);
            g2d.drawLine(3 * width / 4, 0, 3 * width / 4, height);

            // Draw waveform
            g2d.setColor(new Color(0, 200, 120));
            g2d.setStroke(new BasicStroke(2f));

            Path2D.Double path = new Path2D.Double();
            boolean first = true;

            // To ensure we draw exactly one cycle
            double timeScale = 1.0 / frequency;

            for (int i = 0; i < SAMPLES; i++) {
                double time = (i / (double) SAMPLES) * timeScale;
                double value = 0;

                // Use the same calculations as in the LFO anonymous class
                switch (currentWaveform) {
                    case SINE:
                        value = offset + amplitude * Math.sin(2 * Math.PI * frequency * time + phase * 2 * Math.PI);
                        break;

                    case TRIANGLE:
                        double triPhase = (frequency * time + phase) % 1.0;
                        value = offset + amplitude * (triPhase < 0.5 ? 4 * triPhase - 1 : 3 - 4 * triPhase);
                        break;

                    case SAWTOOTH:
                        double sawPhase = (frequency * time + phase) % 1.0;
                        value = offset + amplitude * (2 * sawPhase - 1);
                        break;

                    case SQUARE:
                        double sqrPhase = (frequency * time + phase) % 1.0;
                        value = offset + amplitude * (sqrPhase < 0.5 ? 1 : -1);
                        break;

                    case PULSE:
                        double pulsePhase = (frequency * time + phase) % 1.0;
                        value = offset + amplitude * (pulsePhase < pulseWidth ? 1 : -1);
                        break;

                    case RANDOM:
                        // For visualization, use a step function
                        int steps = 16; // number of steps to show
                        double randomPhase = Math.floor(steps * (frequency * time + phase)) / steps;
                        // Use phase as seed for deterministic randomness
                        value = offset + amplitude * (2 * ((Math.sin(randomPhase * 12345.67) + 1) % 1.0) - 1);
                        break;
                }

                // Clamp value to -1 to 1 range
                value = Math.max(-1.0, Math.min(1.0, value));

                // Map to Y coordinate (invert because Y grows downward)
                double x = (i / (double) SAMPLES) * width;
                double y = midY - (value * height / 2);

                if (first) {
                    path.moveTo(x, y);
                    first = false;
                } else {
                    path.lineTo(x, y);
                }
            }

            g2d.draw(path);

            // Draw value marker (current position in the waveform)
            if (running) {
                double currentPhase = (System.currentTimeMillis() - startTimeMs) / 1000.0 * frequency;
                currentPhase = currentPhase % 1.0;

                int markerX = (int) (currentPhase * width);
                g2d.setColor(Color.WHITE);
                g2d.drawLine(markerX, 0, markerX, height);
            }

            g2d.dispose();
        }
    }

    /**
     * Panel for visualizing the live waveform
     */
    private class LiveWaveformPanel extends JPanel {
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
}