package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.beats.UIUtils;

/**
 * Panel for displaying and editing audio samples
 */
public class SampleViewerPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SampleViewerPanel.class);
    
    // UI Components
    private WaveformPanel waveformPanel;
    private JButton playButton;
    private JButton stopButton;
    private JLabel durationLabel;
    private JLabel selectionLabel;
    private JLabel fileInfoLabel;
    private JSlider zoomSlider;
    
    // Audio playback
    private File audioFile;
    private Clip audioClip;
    private AudioFormat audioFormat;
    private byte[] audioData;
    private int[] waveformData;
    private int sampleRate;
    private int channels;
    private int sampleSizeInBits;
    private double duration;
    
    // Selection points (in frames)
    private int sampleStart = 0;
    private int sampleEnd = 0;
    private int loopStart = 0;
    private int loopEnd = 0;
    
    // Background loading
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    
    public SampleViewerPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Create waveform panel
        waveformPanel = new WaveformPanel();
        JScrollPane scrollPane = new JScrollPane(waveformPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);
        
        // Create controls panel
        JPanel controlsPanel = createControlsPanel();
        add(controlsPanel, BorderLayout.SOUTH);
        
        // Create info panel
        JPanel infoPanel = createInfoPanel();
        add(infoPanel, BorderLayout.NORTH);
        
        // Set minimum size
        setMinimumSize(new Dimension(400, 300));
        setPreferredSize(new Dimension(600, 400));
    }
    
    /**
     * Create panel with playback controls and zoom
     */
    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        // Play button
        playButton = new JButton("▶ Play");
        playButton.setEnabled(false);
        playButton.addActionListener(e -> playAudio());
        
        // Stop button
        stopButton = new JButton("⏹ Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopAudio());
        
        // Zoom slider
        zoomSlider = new JSlider(1, 20, 1);
        zoomSlider.setPreferredSize(new Dimension(150, 20));
        zoomSlider.setToolTipText("Zoom");
        zoomSlider.addChangeListener(e -> {
            waveformPanel.setZoomLevel(zoomSlider.getValue());
        });
        
        // Add buttons to panel
        buttonsPanel.add(playButton);
        buttonsPanel.add(stopButton);
        buttonsPanel.add(new JLabel("Zoom:"));
        buttonsPanel.add(zoomSlider);
        
        // Duration and selection labels
        durationLabel = new JLabel("Duration: --:--");
        selectionLabel = new JLabel("Selection: --:-- to --:--");
        
        JPanel labelsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        labelsPanel.add(durationLabel);
        labelsPanel.add(selectionLabel);
        
        // Add components to panel
        panel.add(buttonsPanel, BorderLayout.WEST);
        panel.add(labelsPanel, BorderLayout.EAST);
        
        return panel;
    }
    
    /**
     * Create panel with file information
     */
    private JPanel createInfoPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        fileInfoLabel = new JLabel("No file loaded");
        panel.add(fileInfoLabel, BorderLayout.WEST);
        
        return panel;
    }
    
    /**
     * Load audio file and display waveform
     */
    public void loadAudioFile(File file) throws IOException, 
            UnsupportedAudioFileException, LineUnavailableException {
        
        // Store file reference
        audioFile = file;
        
        // Update UI immediately to indicate loading
        fileInfoLabel.setText("Loading: " + file.getName() + "...");
        waveformPanel.resetWaveform();
        
        // Clear previous audio clip
        stopAndCloseAudio();
        
        // Load audio data in background
        executor.submit(() -> {
            try {
                // Get audio input stream
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
                audioFormat = audioInputStream.getFormat();
                
                // Get audio properties
                sampleRate = (int)audioFormat.getSampleRate();
                channels = audioFormat.getChannels();
                sampleSizeInBits = audioFormat.getSampleSizeInBits();
                
                // Calculate duration
                long frameLength = audioInputStream.getFrameLength();
                duration = frameLength / audioFormat.getFrameRate();
                
                // Initialize selection points
                sampleStart = 0;
                sampleEnd = (int)frameLength;
                loopStart = 0;
                loopEnd = (int)frameLength;
                
                // Read audio data
                int dataSize = (int)audioInputStream.getFrameLength() 
                        * audioFormat.getFrameSize();
                audioData = new byte[dataSize];
                int bytesRead = 0;
                int totalBytesRead = 0;
                byte[] buffer = new byte[4096];
                
                while ((bytesRead = audioInputStream.read(buffer, 0, buffer.length)) != -1) {
                    System.arraycopy(buffer, 0, audioData, totalBytesRead, bytesRead);
                    totalBytesRead += bytesRead;
                    
                    if (totalBytesRead >= dataSize) {
                        break;
                    }
                }
                
                // Close input stream
                audioInputStream.close();
                
                // Generate waveform data
                generateWaveformData();
                
                // Create audio clip for playback
                createAudioClip();
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    updateFileInfoLabel();
                    updateDurationLabel();
                    updateSelectionLabel();
                    
                    // Enable play button
                    playButton.setEnabled(true);
                    
                    // Update waveform display
                    waveformPanel.setWaveformData(waveformData);
                    waveformPanel.setAudioProperties(
                            (int)audioFormat.getSampleRate(),
                            audioFormat.getChannels(),
                            (int)audioInputStream.getFrameLength());
                    waveformPanel.repaint();
                });
                
            } catch (Exception e) {
                logger.error("Error loading audio file: {}", e.getMessage(), e);
                
                // Update UI on EDT
                SwingUtilities.invokeLater(() -> {
                    fileInfoLabel.setText("Error loading: " + file.getName());
                    playButton.setEnabled(false);
                });
            }
        });
    }
    
    /**
     * Generate waveform data from audio bytes
     */
    private void generateWaveformData() {
        if (audioData == null) return;
        
        int frameSize = audioFormat.getFrameSize();
        int numFrames = audioData.length / frameSize;
        
        // For visualization, we'll use fewer points to improve performance
        int downsampleFactor = Math.max(1, numFrames / 2000);
        int waveformSize = numFrames / downsampleFactor;
        waveformData = new int[waveformSize];
        
        // Process audio data based on format
        for (int i = 0; i < waveformSize; i++) {
            int frameIndex = i * downsampleFactor;
            int sampleValue = 0;
            
            // Calculate average value for this segment
            for (int j = 0; j < downsampleFactor && (frameIndex + j) * frameSize < audioData.length; j++) {
                int byteIndex = (frameIndex + j) * frameSize;
                
                // Handle different sample sizes and channels
                if (sampleSizeInBits == 16) {
                    // 16-bit audio (convert bytes to short)
                    for (int ch = 0; ch < channels; ch++) {
                        int chOffset = ch * (sampleSizeInBits / 8);
                        short sample = (short) ((audioData[byteIndex + chOffset] & 0xFF) |
                                ((audioData[byteIndex + chOffset + 1] & 0xFF) << 8));
                        sampleValue += Math.abs(sample);
                    }
                } else if (sampleSizeInBits == 8) {
                    // 8-bit audio
                    for (int ch = 0; ch < channels; ch++) {
                        int chOffset = ch * (sampleSizeInBits / 8);
                        byte sample = audioData[byteIndex + chOffset];
                        sampleValue += Math.abs(sample - 128);
                    }
                }
            }
            
            // Average across channels and samples
            sampleValue /= (downsampleFactor * channels);
            
            // Store value (normalized to 0-100 range for display)
            if (sampleSizeInBits == 16) {
                waveformData[i] = sampleValue * 100 / 32768;
            } else {
                waveformData[i] = sampleValue * 100 / 128;
            }
        }
    }
    
    /**
     * Create audio clip for playback
     */
    private void createAudioClip() throws LineUnavailableException, IOException {
        // Create clip for playback
        DataLine.Info info = new DataLine.Info(Clip.class, audioFormat);
        
        if (!AudioSystem.isLineSupported(info)) {
            throw new LineUnavailableException("Audio format not supported for playback");
        }
        
        // Get and open the clip
        audioClip = (Clip) AudioSystem.getLine(info);
        
        // Create new audio input stream from the data
        AudioInputStream ais = new AudioInputStream(
                new java.io.ByteArrayInputStream(audioData),
                audioFormat,
                audioData.length / audioFormat.getFrameSize());
        
        audioClip.open(ais);
        
        // Add listener to update UI when playback stops
        audioClip.addLineListener(event -> {
            if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                SwingUtilities.invokeLater(() -> {
                    stopButton.setEnabled(false);
                    playButton.setEnabled(true);
                });
            }
        });
    }
    
    /**
     * Start audio playback from current position
     */
    private void playAudio() {
        if (audioClip == null) return;
        
        try {
            // Set clip to current sample start position
            audioClip.setFramePosition(sampleStart);
            
            // Check if loop points are set
            if (loopStart < loopEnd && loopStart >= sampleStart && loopEnd <= sampleEnd) {
                audioClip.setLoopPoints(loopStart, loopEnd);
                audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                // Play once without looping
                audioClip.start();
            }
            
            // Update UI
            playButton.setEnabled(false);
            stopButton.setEnabled(true);
            
        } catch (Exception e) {
            logger.error("Error playing audio: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Stop audio playback
     */
    private void stopAudio() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
        }
        
        // Update UI
        playButton.setEnabled(true);
        stopButton.setEnabled(false);
    }
    
    /**
     * Stop playback and close audio resources
     */
    private void stopAndCloseAudio() {
        if (audioClip != null) {
            stopAudio();
            audioClip.close();
            audioClip = null;
        }
    }
    
    /**
     * Update file info label with audio properties
     */
    private void updateFileInfoLabel() {
        if (audioFile == null || audioFormat == null) {
            fileInfoLabel.setText("No file loaded");
            return;
        }
        
        // Format audio properties
        String info = String.format("%s (%d Hz, %d-bit, %s)",
                audioFile.getName(),
                (int)audioFormat.getSampleRate(),
                audioFormat.getSampleSizeInBits(),
                audioFormat.getChannels() == 1 ? "Mono" : "Stereo");
        
        fileInfoLabel.setText(info);
    }
    
    /**
     * Update duration label
     */
    private void updateDurationLabel() {
        if (audioFormat == null) {
            durationLabel.setText("Duration: --:--");
            return;
        }
        
        // Format duration as mm:ss.ms
        String formattedDuration = formatTime(duration);
        durationLabel.setText("Duration: " + formattedDuration);
    }
    
    /**
     * Update selection label
     */
    private void updateSelectionLabel() {
        if (audioFormat == null) {
            selectionLabel.setText("Selection: --:-- to --:--");
            return;
        }
        
        double startTime = sampleStart / audioFormat.getFrameRate();
        double endTime = sampleEnd / audioFormat.getFrameRate();
        
        String formattedStart = formatTime(startTime);
        String formattedEnd = formatTime(endTime);
        
        selectionLabel.setText("Selection: " + formattedStart + " to " + formattedEnd);
    }
    
    /**
     * Format time value as mm:ss.ms
     */
    private String formatTime(double seconds) {
        int mins = (int)(seconds / 60);
        int secs = (int)(seconds % 60);
        int ms = (int)((seconds - Math.floor(seconds)) * 1000);
        
        return String.format("%02d:%02d.%03d", mins, secs, ms);
    }
    
    /**
     * Refresh UI after theme change
     */
    public void refreshUI() {
        SwingUtilities.updateComponentTreeUI(this);
        waveformPanel.repaint();
        repaint();
    }
    
    /**
     * Panel for displaying the waveform with markers
     */
    private class WaveformPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        
        // Waveform data
        private int[] waveformData;
        private int sampleRate;
        private int channels;
        private int totalFrames;
        
        // Display properties
        private int zoomLevel = 1;
        private static final int MARKER_WIDTH = 3;
        private static final int TICK_HEIGHT = 5;
        private static final int TOP_MARGIN = 20; // Space for tick marks
        
        // Marker dragging state
        private boolean dragging = false;
        private int draggedMarker = -1; // 0=start, 1=end, 2=loopStart, 3=loopEnd
        
        public WaveformPanel() {
            setBackground(Color.BLACK);
            setBorder(BorderFactory.createEmptyBorder());
            
            // Add mouse listeners for marker interaction
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (waveformData == null) return;
                    
                    // Calculate position in frames
                    int x = e.getX();
                    int frame = xToFrame(x);
                    
                    // Check if clicking on a marker
                    Point p = e.getPoint();
                    
                    if (isNearMarker(p, sampleStart)) {
                        dragging = true;
                        draggedMarker = 0; // sampleStart
                    } else if (isNearMarker(p, sampleEnd)) {
                        dragging = true;
                        draggedMarker = 1; // sampleEnd
                    } else if (isNearMarker(p, loopStart)) {
                        dragging = true;
                        draggedMarker = 2; // loopStart
                    } else if (isNearMarker(p, loopEnd)) {
                        dragging = true;
                        draggedMarker = 3; // loopEnd
                    } else {
                        // If double-click, set selection point
                        if (e.getClickCount() == 2) {
                            if (e.isShiftDown()) {
                                sampleEnd = frame;
                            } else {
                                sampleStart = frame;
                            }
                            updateSelectionLabel();
                            repaint();
                        }
                    }
                }
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!dragging || waveformData == null) return;
                    
                    // Calculate position in frames
                    int x = Math.max(0, Math.min(e.getX(), getWidth() - 1));
                    int frame = xToFrame(x);
                    
                    // Update appropriate marker
                    switch (draggedMarker) {
                        case 0: // Sample start
                            sampleStart = Math.min(frame, sampleEnd - 1);
                            break;
                        case 1: // Sample end
                            sampleEnd = Math.max(frame, sampleStart + 1);
                            break;
                        case 2: // Loop start
                            loopStart = Math.min(frame, loopEnd - 1);
                            loopStart = Math.max(loopStart, sampleStart);
                            break;
                        case 3: // Loop end
                            loopEnd = Math.max(frame, loopStart + 1);
                            loopEnd = Math.min(loopEnd, sampleEnd);
                            break;
                    }
                    
                    // Update labels and repaint
                    updateSelectionLabel();
                    repaint();
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = false;
                    draggedMarker = -1;
                }
                
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (waveformData == null) return;
                    
                    // Change cursor if near a marker
                    Point p = e.getPoint();
                    if (isNearMarker(p, sampleStart) || 
                            isNearMarker(p, sampleEnd) || 
                            isNearMarker(p, loopStart) || 
                            isNearMarker(p, loopEnd)) {
                        setCursor(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }
                }
            };
            
            addMouseListener(mouseAdapter);
            addMouseMotionListener(mouseAdapter);
        }
        
        /**
         * Check if the point is near the specified marker position
         */
        private boolean isNearMarker(Point p, int markerFrame) {
            int markerX = frameToX(markerFrame);
            return Math.abs(p.x - markerX) <= MARKER_WIDTH * 2;
        }
        
        /**
         * Set waveform data
         */
        public void setWaveformData(int[] data) {
            this.waveformData = data;
            updateSize();
        }
        
        /**
         * Set audio properties
         */
        public void setAudioProperties(int sampleRate, int channels, int totalFrames) {
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.totalFrames = totalFrames;
            updateSize();
        }
        
        /**
         * Reset waveform
         */
        public void resetWaveform() {
            waveformData = null;
            totalFrames = 0;
            repaint();
        }
        
        /**
         * Set zoom level (1-20)
         */
        public void setZoomLevel(int level) {
            this.zoomLevel = level;
            updateSize();
            repaint();
        }
        
        /**
         * Update panel size based on zoom and data
         */
        private void updateSize() {
            if (waveformData == null) {
                setPreferredSize(new Dimension(400, 200));
                return;
            }
            
            // Calculate width based on zoom level
            int width = waveformData.length * zoomLevel;
            int height = 200;
            
            setPreferredSize(new Dimension(width, height));
            revalidate();
        }
        
        /**
         * Convert frame position to X coordinate
         */
        private int frameToX(int frame) {
            if (totalFrames <= 0) return 0;
            
            // Scale based on waveform data and zoom
            return (int)((long)frame * waveformData.length * zoomLevel / totalFrames);
        }
        
        /**
         * Convert X coordinate to frame position
         */
        private int xToFrame(int x) {
            if (waveformData == null || waveformData.length == 0) return 0;
            
            // Convert based on zoom level and waveform data
            int frame = (int)((long)x * totalFrames / (waveformData.length * zoomLevel));
            return Math.max(0, Math.min(frame, totalFrames - 1));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                    RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            // Draw background
            g2d.setColor(new Color(25, 25, 25));
            g2d.fillRect(0, 0, width, height);
            
            // Draw tick marks and time labels at the top
            drawTimeScale(g2d, width);
            
            // If no data, show message
            if (waveformData == null || waveformData.length == 0) {
                g2d.setColor(Color.WHITE);
                String msg = "No audio file loaded";
                g2d.drawString(msg, (width - g2d.getFontMetrics().stringWidth(msg)) / 2, 
                        height / 2);
                g2d.dispose();
                return;
            }
            
            // Draw waveform
            drawWaveform(g2d, width, height);
            
            // Draw selection region
            drawSelectionRegion(g2d, width, height);
            
            // Draw markers
            drawMarkers(g2d, height);
            
            g2d.dispose();
        }
        
        /**
         * Draw time scale with tick marks
         */
        private void drawTimeScale(Graphics2D g2d, int width) {
            if (audioFormat == null) return;
            
            g2d.setColor(new Color(100, 100, 100));
            
            // Calculate tick interval based on zoom level
            double secondsPerPixel = 1.0 / (audioFormat.getFrameRate() * zoomLevel / waveformData.length * totalFrames);
            double tickIntervalSec = calculateTickInterval(secondsPerPixel * width);
            
            // Draw ticks and labels
            for (double sec = 0; sec <= duration; sec += tickIntervalSec) {
                int frame = (int)(sec * audioFormat.getFrameRate());
                int x = frameToX(frame);
                
                if (x >= 0 && x < width) {
                    // Draw tick mark
                    g2d.drawLine(x, 0, x, TICK_HEIGHT);
                    
                    // Draw time label
                    String timeLabel = formatTimeShort(sec);
                    int labelWidth = g2d.getFontMetrics().stringWidth(timeLabel);
                    g2d.drawString(timeLabel, x - labelWidth / 2, TICK_HEIGHT + 12);
                }
            }
        }
        
        /**
         * Calculate appropriate tick interval based on view width
         */
        private double calculateTickInterval(double viewDurationSec) {
            double[] intervals = {0.001, 0.005, 0.01, 0.05, 0.1, 0.5, 1.0, 5.0, 10.0, 30.0, 60.0};
            
            // Target around 10-20 tick marks in view
            double targetInterval = viewDurationSec / 15;
            
            for (double interval : intervals) {
                if (interval >= targetInterval) {
                    return interval;
                }
            }
            
            return 60.0; // Default to 1 minute if view is very large
        }
        
        /**
         * Format time value for tick marks
         */
        private String formatTimeShort(double seconds) {
            if (seconds < 1) {
                return new DecimalFormat("0.###").format(seconds) + "s";
            } else if (seconds < 60) {
                return new DecimalFormat("#0.#").format(seconds) + "s";
            } else {
                int mins = (int)(seconds / 60);
                int secs = (int)(seconds % 60);
                return String.format("%d:%02d", mins, secs);
            }
        }
        
        /**
         * Draw the waveform
         */
        private void drawWaveform(Graphics2D g2d, int width, int height) {
            int dataLength = waveformData.length;
            int displayHeight = height - TOP_MARGIN;
            int centerY = TOP_MARGIN + displayHeight / 2;
            
            // Draw waveform
            g2d.setColor(new Color(0, 200, 100));
            
            for (int i = 0; i < dataLength - 1; i++) {
                int x1 = i * zoomLevel;
                int x2 = (i + 1) * zoomLevel;
                
                if (x2 >= width) break;
                
                int amplitude1 = waveformData[i];
                int amplitude2 = waveformData[i + 1];
                
                // Scale amplitude to half the display height
                int y1 = centerY - (amplitude1 * displayHeight / 200);
                int y2 = centerY - (amplitude2 * displayHeight / 200);
                
                // Draw waveform line
                g2d.drawLine(x1, y1, x2, y2);
                
                // Mirror for bottom half
                y1 = centerY + (amplitude1 * displayHeight / 200);
                y2 = centerY + (amplitude2 * displayHeight / 200);
                g2d.drawLine(x1, y1, x2, y2);
            }
        }
        
        /**
         * Draw selection and loop regions
         */
        private void drawSelectionRegion(Graphics2D g2d, int width, int height) {
            if (totalFrames <= 0) return;
            
            int displayHeight = height - TOP_MARGIN;
            
            // Draw sample selection region
            int startX = frameToX(sampleStart);
            int endX = frameToX(sampleEnd);
            
            // Selection region
            g2d.setColor(new Color(100, 100, 255, 40));
            g2d.fillRect(startX, TOP_MARGIN, endX - startX, displayHeight);
            
            // Loop region
            int loopStartX = frameToX(loopStart);
            int loopEndX = frameToX(loopEnd);
            
            g2d.setColor(new Color(255, 100, 100, 40));
            g2d.fillRect(loopStartX, TOP_MARGIN, loopEndX - loopStartX, displayHeight);
        }
        
        /**
         * Draw markers for selection and loop points
         */
        private void drawMarkers(Graphics2D g2d, int height) {
            if (totalFrames <= 0) return;
            
            int startX = frameToX(sampleStart);
            int endX = frameToX(sampleEnd);
            int loopStartX = frameToX(loopStart);
            int loopEndX = frameToX(loopEnd);
            
            // Draw sample start/end markers
            g2d.setColor(new Color(100, 100, 255));
            drawMarker(g2d, startX, height, true);
            drawMarker(g2d, endX, height, false);
            
            // Draw loop start/end markers
            g2d.setColor(new Color(255, 100, 100));
            drawLoopMarker(g2d, loopStartX, height, true);
            drawLoopMarker(g2d, loopEndX, height, false);
        }
        
        /**
         * Draw a marker line with handle
         */
        private void drawMarker(Graphics2D g2d, int x, int height, boolean isStart) {
            // Draw vertical line
            g2d.fillRect(x - MARKER_WIDTH / 2, TOP_MARGIN, MARKER_WIDTH, height - TOP_MARGIN);
            
            // Draw handle at the bottom
            int handleWidth = MARKER_WIDTH * 3;
            int handleHeight = MARKER_WIDTH * 3;
            int handleX = x - handleWidth / 2;
            int handleY = height - handleHeight - 5;
            
            g2d.fillRect(handleX, handleY, handleWidth, handleHeight);
            
            // Draw arrow inside handle to indicate direction
            g2d.setColor(Color.BLACK);
            int[] xPoints = new int[3];
            int[] yPoints = new int[3];
            
            if (isStart) {
                // Right-pointing triangle
                xPoints[0] = handleX + 2;
                xPoints[1] = handleX + handleWidth - 2;
                xPoints[2] = handleX + 2;
                
                yPoints[0] = handleY + 2;
                yPoints[1] = handleY + handleHeight / 2;
                yPoints[2] = handleY + handleHeight - 2;
            } else {
                // Left-pointing triangle
                xPoints[0] = handleX + handleWidth - 2;
                xPoints[1] = handleX + 2;
                xPoints[2] = handleX + handleWidth - 2;
                
                yPoints[0] = handleY + 2;
                yPoints[1] = handleY + handleHeight / 2;
                yPoints[2] = handleY + handleHeight - 2;
            }
            
            g2d.fillPolygon(xPoints, yPoints, 3);
        }
        
        /**
         * Draw a loop marker line with handle
         */
        private void drawLoopMarker(Graphics2D g2d, int x, int height, boolean isStart) {
            // Draw dashed vertical line
            float[] dash = {3.0f, 3.0f};
            java.awt.Stroke oldStroke = g2d.getStroke();
            g2d.setStroke(new java.awt.BasicStroke(
                    MARKER_WIDTH, 
                    java.awt.BasicStroke.CAP_BUTT,
                    java.awt.BasicStroke.JOIN_ROUND,
                    1.0f,
                    dash,
                    0.0f));
            
            g2d.drawLine(x, TOP_MARGIN, x, height);
            g2d.setStroke(oldStroke);
            
            // Draw loop handle at the top
            int handleWidth = MARKER_WIDTH * 3;
            int handleHeight = MARKER_WIDTH * 3;
            int handleX = x - handleWidth / 2;
            int handleY = TOP_MARGIN + 5;
            
            g2d.fillRect(handleX, handleY, handleWidth, handleHeight);
            
            // Draw loop symbol inside handle
            g2d.setColor(Color.BLACK);
            
            if (isStart) {
                // Loop start symbol (like "|>")
                g2d.fillRect(handleX + 2, handleY + 2, 2, handleHeight - 4);
                
                int[] xPoints = {handleX + 5, handleX + handleWidth - 2, handleX + 5};
                int[] yPoints = {handleY + 2, handleY + handleHeight / 2, handleY + handleHeight - 2};
                g2d.fillPolygon(xPoints, yPoints, 3);
            } else {
                // Loop end symbol (like "<|")
                g2d.fillRect(handleX + handleWidth - 4, handleY + 2, 2, handleHeight - 4);
                
                int[] xPoints = {handleX + handleWidth - 5, handleX + 2, handleX + handleWidth - 5};
                int[] yPoints = {handleY + 2, handleY + handleHeight / 2, handleY + handleHeight - 2};
                g2d.fillPolygon(xPoints, yPoints, 3);
            }
        }
    }
}