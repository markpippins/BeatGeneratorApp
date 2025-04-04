package com.angrysurfer.beats.panel;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BackgroundPanel extends JPanel {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundPanel.class.getName());
    private BufferedImage backgroundImage;
    private float opacity = 0.15f; // Reduced opacity to make it more subtle

    public BackgroundPanel() {
        setOpaque(false); // Make panel transparent
        loadBackgroundImage();
    }

    private void loadBackgroundImage() {
        try {
            InputStream is = getClass().getClassLoader().getResourceAsStream("space cobra.jpg");
            if (is != null) {
                backgroundImage = ImageIO.read(is);
                logger.info("Background image loaded successfully");
            } else {
                logger.error("Could not find space cobra.jpg in resources");
            }
        } catch (Exception e) {
            logger.error("Error loading background image: " + e.getMessage());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        
        // Paint the background color of the parent
        if (getBackground() != null) {
            g2d.setColor(getBackground());
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
        
        if (backgroundImage != null) {
            // Enable better quality rendering
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Calculate scaling to maintain aspect ratio while filling the panel
            double scaleX = (double) getWidth() / backgroundImage.getWidth();
            double scaleY = (double) getHeight() / backgroundImage.getHeight();
            double scale = Math.max(scaleX, scaleY);

            int newWidth = (int) (backgroundImage.getWidth() * scale);
            int newHeight = (int) (backgroundImage.getHeight() * scale);

            // Center the image
            int x = (getWidth() - newWidth) / 2;
            int y = (getHeight() - newHeight) / 2;

            // Set opacity
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            
            // Draw the scaled image
            g2d.drawImage(backgroundImage, x, y, newWidth, newHeight, null);
        }
        
        g2d.dispose();
        
        // Paint children on top
        super.paintChildren(g);
    }

    public void setOpacity(float opacity) {
        this.opacity = Math.max(0f, Math.min(1f, opacity));
        repaint();
    }
}
