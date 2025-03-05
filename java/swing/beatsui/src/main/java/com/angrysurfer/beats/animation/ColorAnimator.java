package com.angrysurfer.beats.animation;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import javax.swing.Timer;

import com.angrysurfer.beats.ColorUtils;

public class ColorAnimator {
    private final List<Color> colors;
    private int currentIndex = 0;
    private int nextIndex = 1;
    private float interpolation = 0.0f;
    private final float stepSize = 0.005f; // Controls animation speed
    private Timer timer;
    private Runnable onColorUpdate;

    public ColorAnimator() {
        this.colors = new ArrayList<>();
        colors.add(ColorUtils.charcoalGray);
        colors.add(ColorUtils.slateGray);
        colors.add(ColorUtils.deepNavy);
        colors.add(ColorUtils.mutedOlive);
        colors.add(ColorUtils.fadedLime);
        colors.add(ColorUtils.dustyAmber);
        colors.add(ColorUtils.warmMustard);
        colors.add(ColorUtils.deepOrange);
        colors.add(ColorUtils.agedOffWhite);
        colors.add(ColorUtils.deepTeal);

        colors.add(Color.cyan);
        colors.add(Color.magenta);
        colors.add(Color.yellow);
        colors.add(Color.red);
        colors.add(Color.pink);

        timer = new Timer(15, e -> updateColor()); // Slow down animation to 20 FPS
    }

    public void start() {
        timer.start();
    }

    public void stop() {
        timer.stop();
    }

    public void setOnColorUpdate(Runnable callback) {
        this.onColorUpdate = callback;
    }

    public Color getCurrentColor() {
        Color c1 = colors.get(currentIndex);
        Color c2 = colors.get(nextIndex);
        
        return interpolateColor(c1, c2, interpolation);
    }

    private void updateColor() {
        interpolation += stepSize;
        
        if (interpolation >= 1.0f) {
            interpolation = 0.0f;
            currentIndex = nextIndex;
            nextIndex = (nextIndex + 1) % colors.size();
        }

        if (onColorUpdate != null) {
            onColorUpdate.run();
        }
    }

    private Color interpolateColor(Color c1, Color c2, float ratio) {
        float inv = 1.0f - ratio;
        
        int red = Math.round(c1.getRed() * inv + c2.getRed() * ratio);
        int green = Math.round(c1.getGreen() * inv + c2.getGreen() * ratio);
        int blue = Math.round(c1.getBlue() * inv + c2.getBlue() * ratio);
        
        return new Color(red, green, blue);
    }
}
