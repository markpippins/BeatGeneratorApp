package com.angrysurfer.beats.ui.visualization;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.Timer;

import com.angrysurfer.beats.ui.widget.GridButton;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.StatusConsumer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Visualizer implements CommandListener {

    private final JComponent parent;

    private GridButton[][] buttons;
    private Timer animationTimer;
    private IVisualizationHandler currentVisualization = null;

    private Random random = new Random();

    private boolean isVisualizationMode = false;
    private long lastInteraction;

    private Timer visualizationTimer;
    private Timer visualizationChangeTimer;

    private static final int VISUALIZATION_DELAY = 30000; // 30 seconds
    private static final int VISUALIZATION_CHANGE_DELAY = 10000; // 10 seconds

    private StatusConsumer statusConsumer;
    private final CommandBus commandBus = CommandBus.getInstance();

    private List<IVisualizationHandler> visualizations = new ArrayList<>();

    public Visualizer(JComponent parent, StatusConsumer statusConsumer, GridButton[][] buttons) {
        this.parent = parent;
        this.statusConsumer = statusConsumer;
        this.buttons = buttons;
        initializeVisualizations();
        setupTimers();
        setupAnimation();
        commandBus.register(this);
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null)
            return;

        switch (action.getCommand()) {
            case Commands.START_VISUALIZATION:
                startVisualizer();
                isVisualizationMode = true;

                break;
            case Commands.STOP_VISUALIZATION:
                stopVisualizer();
                isVisualizationMode = false;
                break;

            case Commands.VISUALIZATION_SELECTED:
                startVisualizer((IVisualizationHandler) action.getData());
                break;

        }
    }

    private List<IVisualizationHandler> getVisualizations() {
        List<IVisualizationHandler> visualizations = new ArrayList<>();
        String packageName = "com.angrysurfer.beats.ui.visualization.handler";

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            String path = packageName.replace('.', '/');
            java.net.URL resource = classLoader.getResource(path);

            if (resource == null) {
                System.err.println("Package not found: " + packageName);
                return visualizations;
            }

            java.io.File directory = new java.io.File(resource.getFile());
            if (directory.exists()) {
                String[] files = directory.list();
                for (String file : files) {
                    if (file.endsWith(".class")) {
                        String className = file.substring(0, file.length() - 6);
                        try {
                            Class<?> cls = Class.forName(packageName + "." + className);
                            if (IVisualizationHandler.class.isAssignableFrom(cls) &&
                                    !java.lang.reflect.Modifier.isAbstract(cls.getModifiers())) {
                                IVisualizationHandler handler = (IVisualizationHandler) cls.getDeclaredConstructor()
                                        .newInstance();
                                visualizations.add(handler);
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to load visualization: " + className + " - " + e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error scanning for visualizations: " + e.getMessage());
        }

        return visualizations;
    }

    private void initializeVisualizations() {
        visualizations = getVisualizations();
        // Register visualizations with command bus
        for (IVisualizationHandler handler : visualizations) {
            commandBus.publish(new Command(Commands.VISUALIZATION_REGISTERED, this, handler));
        }
    }

    private void setupTimers() {
        lastInteraction = System.currentTimeMillis();

        visualizationTimer = new Timer(1000, e -> checkVisualizer());
        visualizationTimer.start();

        visualizationChangeTimer = new Timer(VISUALIZATION_CHANGE_DELAY, e -> {
            if (isVisualizationMode) {
                setDisplayMode(getRandomVisualization());
            }
        });
    }

    private IVisualizationHandler getRandomVisualization() {
        return visualizations.get(random.nextInt(visualizations.size()));
    }

    public void checkVisualizer() {
        long timeSinceLastInteraction = System.currentTimeMillis() - lastInteraction;
        if (!isVisualizationMode && timeSinceLastInteraction > VISUALIZATION_DELAY) {
            startVisualizer();
        }
    }

    public void startVisualizer() {
        startVisualizer(getRandomVisualization());
    }

    public void startVisualizer(IVisualizationHandler handler) {
        isVisualizationMode = true;
        commandBus.publish(new Command(Commands.VISUALIZATION_STARTED, this, null));
        visualizationChangeTimer.start();
        setDisplayMode(handler);
    }

    public void stopVisualizer() {
        isVisualizationMode = false;
        commandBus.publish(new Command(Commands.VISUALIZATION_STOPPED, this, null));
        visualizationChangeTimer.stop();
        clearDisplay();
        currentVisualization = null; // Reset current mode
        lastInteraction = System.currentTimeMillis(); // Reset timer
    }

    private void setupAnimation() {
        animationTimer = new Timer(100, e -> updateDisplay());
        animationTimer.start();
    }

    private void setDisplayMode(IVisualizationHandler visualization) {
        currentVisualization = visualization;
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

        if (statusConsumer != null) {
            statusConsumer.setSender(currentVisualization.getName());
        }
        try {
            currentVisualization.update(buttons);
        } catch (Exception e) {
            System.err.println(currentVisualization.getName() + " Error updating display: " + e.getMessage());
        }
    }

    // ... other necessary supporting classes ...
}
