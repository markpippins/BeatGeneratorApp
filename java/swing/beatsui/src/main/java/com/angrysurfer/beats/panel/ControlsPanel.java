package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.midi.ControlCode;
import com.angrysurfer.core.model.midi.ControlCodeCaption;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ControlsPanel extends JPanel implements IBusListener {
    private static final Logger logger = Logger.getLogger(ControlsPanel.class.getName());
    private JComboBox<Instrument> instrumentSelector;
    private final RedisService redisService;
    private final JPanel controlsContainer;
    private JToolBar toolBar;

    private boolean showToolbar = true;

    public ControlsPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(800, 600));
        this.redisService = RedisService.getInstance();

        // Initialize instrumentSelector first
        this.instrumentSelector = new JComboBox<>();

        // Create toolbar
        toolBar = new JToolBar();
        toolBar.setFloatable(false);

        // Add hamburger menu button
        JButton menuButton = new JButton("☰");
        menuButton.setFont(new Font("Dialog", Font.PLAIN, 16));
        menuButton.setToolTipText("Menu");

        // Create popup menu
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem saveConfigItem = new JMenuItem("Save Config...");
        saveConfigItem.addActionListener(e -> {
            Instrument currentInstrument = (Instrument) instrumentSelector.getSelectedItem();
            if (currentInstrument != null) {
                CommandBus.getInstance().publish(Commands.SAVE_CONFIG, this, currentInstrument);
            }
        });
        popupMenu.add(saveConfigItem);

        // Add popup trigger to button
        menuButton.addActionListener(e -> {
            popupMenu.show(menuButton, 0, menuButton.getHeight());
        });

        toolBar.add(menuButton);
        toolBar.addSeparator(new Dimension(10, 0));

        // Create and configure instrument selector
        instrumentSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Instrument) {
                    setText(((Instrument) value).getName());
                }
                return this;
            }
        });

        // Add refresh button to toolbar
        JButton refreshButton = new JButton("\u21BB"); // Unicode refresh symbol
        refreshButton.setToolTipText("Refresh Controls");
        refreshButton.addActionListener(e -> refreshControlsPanel());

        // Add send button after refresh
        JButton sendButton = new JButton("\u2192"); // Unicode right arrow
        sendButton.setToolTipText("Send All Controls");
        sendButton.addActionListener(e -> {
            Instrument current = (Instrument) instrumentSelector.getSelectedItem();
            if (current != null) {
                CommandBus.getInstance().publish(Commands.SEND_ALL_CONTROLS, this, current);
            }
        });

        // Add components to toolbar with spacing
        toolBar.add(new JLabel("Instrument: "));
        toolBar.add(instrumentSelector);
        toolBar.addSeparator(new Dimension(10, 0));
        toolBar.add(refreshButton);
        toolBar.addSeparator(new Dimension(5, 0));
        toolBar.add(sendButton);

        // Create scrollable container with proper layout
        controlsContainer = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(controlsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // Changed to AS_NEEDED
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED); // Added VERTICAL

        // Wrap controlsContainer in another panel to handle sizing
        JPanel containerWrapper = new JPanel(new BorderLayout());
        containerWrapper.add(controlsContainer, BorderLayout.NORTH);
        containerWrapper.add(Box.createVerticalGlue(), BorderLayout.CENTER);
        scrollPane.setViewportView(containerWrapper);

        // Add components to panel
        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // Load instruments
        refreshInstruments();

        // Add selection listener
        instrumentSelector.addActionListener(e -> {
            Instrument selected = (Instrument) instrumentSelector.getSelectedItem();
            if (selected != null) {
                logger.info("Selected instrument: " + selected.getName());
                updateControlsDisplay(selected);
            }
        });

        // Register for resize events
        CommandBus.getInstance().register(this);
    }

    public ControlsPanel(boolean showToolbar) {
        this();
        showToolbar(showToolbar);
    }

    @Override
    public void onAction(Command action) {
        if (Commands.WINDOW_RESIZED.equals(action.getCommand())) {
            Instrument currentInstrument = (Instrument) instrumentSelector.getSelectedItem();
            if (currentInstrument != null) {
                SwingUtilities.invokeLater(() -> updateControlsDisplay(currentInstrument));
            }
        }
    }

    private static class ControlGroup {
        String name;
        List<ControlGroup> subgroups = new ArrayList<>();
        List<ControlCode> controls = new ArrayList<>();

        ControlGroup(String name) {
            this.name = name;
        }
    }

    public void updateControlsDisplay() {
        Instrument selected = (Instrument) instrumentSelector.getSelectedItem();
        if (selected != null) {
            updateControlsDisplay(selected);
        }
    }

    private void updateControlsDisplay(Instrument instrument) {
        controlsContainer.removeAll();

        if (instrument == null || instrument.getControlCodes() == null || instrument.getControlCodes().isEmpty()) {
            // Add empty message with center alignment
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            JLabel emptyLabel = new JLabel("No controls available");
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            controlsContainer.add(emptyLabel, gbc);

            revalidate();
            repaint();
            return;
        }

        // Create hierarchical groups
        Map<String, ControlGroup> groups = new TreeMap<>();
        List<ControlCode> singleControls = new ArrayList<>();

        // Group controls by common prefixes
        for (ControlCode control : instrument.getControlCodes()) {
            String[] parts = control.getName().split(" ");
            String prefix = parts[0];

            // Special handling for numbered groups (Envelope N, Filter N, etc.)
            if (parts.length > 2 && isNumberedGroup(prefix)) {
                String groupKey = parts[0] + " " + parts[1]; // e.g., "Envelope 1", "Filter 2"
                ControlGroup group = groups.computeIfAbsent(groupKey, ControlGroup::new);
                group.controls.add(control);
                continue;
            }

            // Special handling for other prefixed groups (LFO, OSC, Wave)
            if (isPrefixedGroup(prefix)) {
                String restOfName = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));
                if (!restOfName.trim().isEmpty()) {
                    ControlGroup group = groups.computeIfAbsent(prefix, ControlGroup::new);
                    group.controls.add(control);
                    continue;
                }
            }

            // Check for X/Y pairs
            if (prefix.equals("X") || prefix.equals("Y")) {
                String key = "Position";
                ControlGroup group = groups.computeIfAbsent(key, ControlGroup::new);
                group.controls.add(control);
                continue;
            }

            // Standard grouping logic for other controls
            if (parts.length > 1) {
                String key = prefix;
                ControlGroup group = groups.computeIfAbsent(key, ControlGroup::new);
                group.controls.add(control);
            } else {
                singleControls.add(control);
            }
        }

        // Create outer wrapper panel with GridBagLayout
        JPanel outerWrapper = new JPanel(new GridBagLayout());
        GridBagConstraints outerGbc = new GridBagConstraints();
        outerGbc.gridx = 0;
        outerGbc.gridy = 0;
        outerGbc.weightx = 1.0;
        outerGbc.anchor = GridBagConstraints.CENTER;

        // Create inner panel that will contain rows
        JPanel rowsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints rowGbc = new GridBagConstraints();
        rowGbc.gridx = 0;
        rowGbc.fill = GridBagConstraints.HORIZONTAL;
        rowGbc.insets = new Insets(5, 5, 5, 5);

        // Calculate max width for wrapping
        int maxWidth = controlsContainer.getParent().getWidth() - 50;
        int currentRowWidth = 0;
        JPanel currentRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        int currentRowY = 0;

        // Add groups with wrapping
        for (ControlGroup group : groups.values()) {
            JPanel groupPanel = new JPanel(new GridBagLayout());
            groupPanel.setBorder(BorderFactory.createTitledBorder(group.name));
            layoutControlsInGroup(groupPanel, group);

            Dimension groupSize = groupPanel.getPreferredSize();

            if (currentRowWidth + groupSize.width > maxWidth && currentRowWidth > 0) {
                // Add current row and start new one
                rowGbc.gridy = currentRowY++;
                rowsPanel.add(currentRow, rowGbc);
                currentRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
                currentRowWidth = 0;
            }

            currentRow.add(groupPanel);
            currentRowWidth += groupSize.width + 10;
        }

        // Add last row if it has components
        if (currentRow.getComponentCount() > 0) {
            rowGbc.gridy = currentRowY;
            rowsPanel.add(currentRow, rowGbc);
        }

        // Add singles panel at the bottom if needed
        if (!singleControls.isEmpty()) {
            rowGbc.gridy = currentRowY + 1;
            JPanel singlesPanel = createSinglesPanel(singleControls, 6);
            rowsPanel.add(singlesPanel, rowGbc);
        }

        // Add rows panel to outer wrapper
        outerWrapper.add(rowsPanel, outerGbc);

        // Add outer wrapper to container
        GridBagConstraints containerGbc = new GridBagConstraints();
        containerGbc.anchor = GridBagConstraints.NORTH;
        containerGbc.fill = GridBagConstraints.BOTH;
        containerGbc.weightx = 1.0;
        containerGbc.weighty = 1.0;
        controlsContainer.add(outerWrapper, containerGbc);

        revalidate();
        repaint();
    }

    private boolean isNumberedGroup(String prefix) {
        return prefix.equals("LFO") ||
                prefix.equals("Envelope") ||
                prefix.equals("Filter") ||
                prefix.equals("Oscillator");
    }

    private boolean isPrefixedGroup(String prefix) {
        return prefix.equals("OSC") ||
                prefix.equals("Wave");
    }

    private boolean isEnvelopeControl(String name) {
        // Check if it's in an envelope-type group
        boolean isEnvelopeGroup = name.contains("Env") ||
                name.contains("Envelope") ||
                name.contains("Cycling");

        if (!isEnvelopeGroup)
            return false;

        // Check for specific envelope parameters
        String[] parts = name.split(" ");
        String paramName = parts[parts.length - 1];
        return paramName.equals("Rise") ||
                paramName.equals("Fall") ||
                paramName.equals("Amount") ||
                paramName.equals("Hold") ||
                paramName.equals("Attack") ||
                paramName.equals("Decay") ||
                paramName.equals("Sustain") ||
                paramName.equals("Release");
    }

    private JPanel createSinglesPanel(List<ControlCode> singleControls, int maxControlsPerRow) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);

        int col = 0;
        for (ControlCode control : singleControls) {
            gbc.gridx = col;
            addSingleControl(panel, control, gbc);
            col = (col + 1) % maxControlsPerRow; // Use maxControlsPerRow instead of CONTROLS_PER_ROW
        }

        return panel;
    }

    private void addSingleControl(JPanel panel, ControlCode control, GridBagConstraints gbc) {
        JPanel wrapper = new JPanel(new BorderLayout(5, 2));

        // Add label
        JLabel label = new JLabel(control.getName(), SwingConstants.CENTER);
        label.setPreferredSize(new Dimension(70, 20));
        wrapper.add(label, BorderLayout.NORTH);

        // Add control
        Component controlComponent = createControl(control);
        wrapper.add(controlComponent, BorderLayout.CENTER);

        panel.add(wrapper, gbc);
    }

    private String getShortName(String fullName) {
        // Get the last part of the name after the last space
        int lastSpace = fullName.lastIndexOf(' ');
        return lastSpace >= 0 ? fullName.substring(lastSpace + 1) : fullName;
    }

    private Component createControl(ControlCode controlCode) {
        int lowerBound = controlCode.getLowerBound() != null ? controlCode.getLowerBound() : 0;
        int upperBound = controlCode.getUpperBound() != null ? controlCode.getUpperBound() : 127;

        // Special handling for all envelope-type controls
        if (isEnvelopeControl(controlCode.getName())) {
            JPanel sliderPanel = new JPanel(new BorderLayout(2, 2));
            JSlider slider = new JSlider(JSlider.VERTICAL, lowerBound, upperBound, lowerBound);
            slider.setPreferredSize(new Dimension(40, 100));
            slider.setPaintTicks(false);
            slider.setPaintLabels(false);
            sliderPanel.add(slider, BorderLayout.CENTER);
            return sliderPanel;
        }

        if (controlCode.getCaptions() != null) {
            int captionCount = controlCode.getCaptions().size();

            if (captionCount == 2) {
                // Create a toggle switch instead of a toggle button
                ToggleSwitch toggle = new ToggleSwitch();

                // Sort captions by code to ensure consistent ordering
                List<ControlCodeCaption> sortedCaptions = new ArrayList<>(controlCode.getCaptions());
                sortedCaptions.sort((a, b) -> a.getCode().compareTo(b.getCode()));

                String offState = sortedCaptions.get(0).getDescription();
                String onState = sortedCaptions.get(1).getDescription();

                logger.info("Created toggle switch for " + controlCode.getName() +
                        " with states: " + offState + " / " + onState);

                // Set tool tip to show both states
                toggle.setToolTipText(offState + " / " + onState);
                toggle.setPreferredSize(new Dimension(60, 30));

                // Add label panel to show current state
                JPanel togglePanel = new JPanel(new BorderLayout(5, 2));
                JLabel stateLabel = new JLabel(offState, SwingConstants.CENTER);
                togglePanel.add(toggle, BorderLayout.CENTER);
                togglePanel.add(stateLabel, BorderLayout.SOUTH);

                toggle.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        stateLabel.setText(toggle.isSelected() ? onState : offState);
                    }
                });

                return togglePanel;
            } else if (captionCount > 2) {
                // Create a panel to hold the slider and labels
                JPanel sliderPanel = new JPanel(new BorderLayout(2, 2));

                // Create and configure the slider with wider dimensions
                JSlider slider = new JSlider(JSlider.VERTICAL, 0, captionCount - 1, 0);
                slider.setPreferredSize(new Dimension(80, 120)); // Changed from 60 to 80
                slider.setMajorTickSpacing(1);
                slider.setSnapToTicks(true);
                slider.setPaintTicks(true);

                // Sort captions by code for consistent ordering
                List<ControlCodeCaption> sortedCaptions = new ArrayList<>(controlCode.getCaptions());
                sortedCaptions.sort((a, b) -> a.getCode().compareTo(b.getCode()));

                // Create a label table for the captions
                Dictionary<Integer, JLabel> labelTable = new Hashtable<>();
                for (int i = 0; i < sortedCaptions.size(); i++) {
                    JLabel label = new JLabel(sortedCaptions.get(i).getDescription());
                    label.setFont(label.getFont().deriveFont(10.0f)); // Smaller font
                    labelTable.put(sortedCaptions.size() - 1 - i, label); // Reverse order for vertical slider
                }
                slider.setLabelTable(labelTable);
                slider.setPaintLabels(true);

                sliderPanel.add(slider, BorderLayout.CENTER);
                return sliderPanel;
            }
        }

        // Default to dial for continuous control
        logger.info("Creating dial for " + controlCode.getName());
        Dial dial = new Dial();
        dial.setMinimum(lowerBound);
        dial.setMaximum(upperBound);
        dial.setPreferredSize(new Dimension(50, 50));
        return dial;
    }

    public void refreshInstruments() {
        instrumentSelector.removeAllItems();
        List<Instrument> instruments = redisService.findAllInstruments();

        // Sort instruments by name
        instruments.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));

        for (Instrument instrument : instruments) {
            instrumentSelector.addItem(instrument);
        }

        logger.info("Loaded " + instruments.size() + " instruments into selector");
    }

    public void refreshControlsPanel() {
        // Store currently selected instrument
        Instrument currentInstrument = (Instrument) instrumentSelector.getSelectedItem();

        // Clear and reload instruments from database
        refreshInstruments();

        // If there was a previously selected instrument, try to reselect it
        if (currentInstrument != null) {
            for (int i = 0; i < instrumentSelector.getItemCount(); i++) {
                Instrument item = instrumentSelector.getItemAt(i);
                if (item.getId().equals(currentInstrument.getId())) {
                    instrumentSelector.setSelectedIndex(i);
                    break;
                }
            }
        }

        // Force update of controls display
        Instrument selected = (Instrument) instrumentSelector.getSelectedItem();
        if (selected != null) {
            updateControlsDisplay(selected);
        }

        logger.info("Controls panel refreshed");
    }

    public void showToolbar(boolean show) {
        toolBar.setVisible(show);
    }

    public void selectInstrument(Instrument instrument) {
        if (instrument != null) {
            for (int i = 0; i < instrumentSelector.getItemCount(); i++) {
                Instrument item = instrumentSelector.getItemAt(i);
                if (item.getId().equals(instrument.getId())) {
                    instrumentSelector.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void layoutControlsInGroup(JPanel groupPanel, ControlGroup group) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER; // Changed from WEST to CENTER

        // Calculate optimal layout
        int totalControls = group.controls.size();
        int maxColumns = Math.min(4, totalControls);
        int rows = (totalControls + maxColumns - 1) / maxColumns;

        // Center the controls grid within the panel
        int currentControl = 0;
        for (int row = 0; row < rows; row++) {
            // Calculate controls in this row
            int controlsInRow = Math.min(maxColumns, totalControls - (row * maxColumns));

            // Calculate starting X position to center the row
            int startX = (maxColumns - controlsInRow) / 2;

            for (int col = 0; col < controlsInRow; col++) {
                ControlCode control = group.controls.get(currentControl++);

                // Add label
                gbc.gridx = startX + col;
                gbc.gridy = row * 2;
                JLabel label = new JLabel(getShortName(control.getName()), SwingConstants.CENTER);
                label.setPreferredSize(new Dimension(70, 20));
                groupPanel.add(label, gbc);

                // Add control
                gbc.gridy = row * 2 + 1;
                Component controlComponent = createControl(control);
                groupPanel.add(controlComponent, gbc);
            }
        }
    }
}
