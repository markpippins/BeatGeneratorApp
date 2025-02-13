package com.angrysurfer.beatsui.surface;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import javax.swing.JComponent;
import javax.swing.Timer;

import org.reflections.Reflections;

import com.angrysurfer.beatsui.api.StatusConsumer;
import com.angrysurfer.beatsui.widget.GridButton;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Grid {

    private final JComponent parent;

    private GridButton[][] buttons;
    private Timer animationTimer;
    private VisualizationEnum currentVisualization = null; // Changed from TEXT to null

    private Random random = new Random();

    private boolean isVisualizationMode = false;
    private long lastInteraction;

    private Timer visualizationTimer;
    private Timer visualizationChangeTimer;

    private static final int VISUALIZATION_DELAY = 30; // 30 seconds
    private static final int VISUALIZATION_CHANGE_DELAY = 10000; // 10 seconds

    private StatusConsumer statusConsumer;

    private Map<VisualizationEnum, VisualizationHandler> visualizations = new HashMap<>();

    public Grid(JComponent parent, StatusConsumer statusConsumer, GridButton[][] buttons) {
        this.parent = parent;
        this.statusConsumer = statusConsumer;
        this.buttons = buttons;
        initializeVisualizations();
        setupTimers();
        setupAnimation();
    }

    private void initializeVisualizations() {
        visualizations = new HashMap<>();
        for (VisualizationEnum vizEnum : VisualizationEnum.values()) {
            try {
                VisualizationHandler handler = vizEnum.createHandler();
                visualizations.put(vizEnum, handler);
            } catch (Exception e) {
                System.err.println("Failed to initialize " + vizEnum.name() + ": " + e.getMessage());
            }
        }
    }

    private void setupTimers() {
        lastInteraction = System.currentTimeMillis();

        visualizationTimer = new Timer(1000, e -> checkScreensaver());
        visualizationTimer.start();

        visualizationChangeTimer = new Timer(VISUALIZATION_CHANGE_DELAY, e -> {
            if (isVisualizationMode) {
                setDisplayMode(VisualizationEnum.values()[random.nextInt(VisualizationEnum.values().length)]);
            }
        });
    }

    public void checkScreensaver() {
        long timeSinceLastInteraction = System.currentTimeMillis() - lastInteraction;
        if (!isVisualizationMode && timeSinceLastInteraction > VISUALIZATION_DELAY) {
            startScreensaver();
        }
    }

    public void startScreensaver() {
        isVisualizationMode = true;
        visualizationChangeTimer.start();
        setDisplayMode(VisualizationEnum.values()[random.nextInt(VisualizationEnum.values().length)]);
    }

    public void stopScreensaver() {
        isVisualizationMode = false;
        visualizationChangeTimer.stop();
        clearDisplay();
        currentVisualization = null; // Reset current mode
        lastInteraction = System.currentTimeMillis(); // Reset timer
    }

    private void setupAnimation() {
        animationTimer = new Timer(100, e -> updateDisplay());
        animationTimer.start();
    }

    private void setDisplayMode(VisualizationEnum mode) {
        currentVisualization = mode;
        clearDisplay();
    }

    private void clearDisplay() {
        for (int row = 0; row < buttons.length; row++) {
            for (int col = 0; col < buttons[0].length; col++) {
                buttons[row][col].reset();
            }
        }
    }

    public void updateDisplay() {
        if (!isVisualizationMode || currentVisualization == null)
            return;

        VisualizationHandler viz = visualizations.get(currentVisualization);
        if (viz != null) {
            if (statusConsumer != null) {
                statusConsumer.setSender(viz.getName());
            }
            try {
                viz.update(buttons);
            } catch (Exception e) {
                System.err.println(viz.getName() + " Error updating display: " + e.getMessage());
            }
        }
    }

    private void setSender(String string) {
        if (Objects.nonNull(statusConsumer))
            statusConsumer.setSender(string);
    }

    // Keep supporting classes and methods that are shared across visualizations

    // ... other necessary supporting classes ...
}
