package com.angrysurfer.beats.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;

import com.angrysurfer.beats.widget.Dial;
import com.angrysurfer.beats.widget.ToggleSwitch;
import com.angrysurfer.core.proxy.ProxyCaption;
import com.angrysurfer.core.proxy.ProxyControlCode;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.angrysurfer.core.service.RedisService;

public class ControlsPanel extends JPanel {
    private static final Logger logger = Logger.getLogger(ControlsPanel.class.getName());
    private final JComboBox<ProxyInstrument> instrumentSelector;
    private final RedisService redisService;
    private final JPanel controlsContainer;
    private static final int CONTROLS_PER_ROW = 8; // Changed from 6 to 12
    private static final int MIN_CONTROLS_PER_ROW = 3;

    public ControlsPanel() {
        setLayout(new BorderLayout());
        this.redisService = RedisService.getInstance();
        
        // Create toolbar
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        // Create and configure instrument selector
        instrumentSelector = new JComboBox<>();
        instrumentSelector.setRenderer(new DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(
                    JList<?> list, Object value, int index, 
                    boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ProxyInstrument) {
                    setText(((ProxyInstrument) value).getName());
                }
                return this;
            }
        });
        
        // Add refresh button to toolbar
        JButton refreshButton = new JButton("\u21BB"); // Unicode refresh symbol
        refreshButton.setToolTipText("Refresh Controls");
        refreshButton.addActionListener(e -> refreshControlsPanel());
        
        // Add components to toolbar with spacing
        toolBar.add(new JLabel("Instrument: "));
        toolBar.add(instrumentSelector);
        toolBar.addSeparator(new Dimension(10, 0));
        toolBar.add(refreshButton);
        
        // Create scrollable container with proper layout
        controlsContainer = new JPanel(new GridBagLayout());
        JScrollPane scrollPane = new JScrollPane(controlsContainer);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED); // Changed to AS_NEEDED
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);     // Added VERTICAL
        
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
            ProxyInstrument selected = (ProxyInstrument) instrumentSelector.getSelectedItem();
            if (selected != null) {
                logger.info("Selected instrument: " + selected.getName());
                updateControlsDisplay(selected);
            }
        });
    }

    private static class ControlGroup {
        String name;
        List<ControlGroup> subgroups = new ArrayList<>();
        List<ProxyControlCode> controls = new ArrayList<>();
        
        ControlGroup(String name) {
            this.name = name;
        }
    }

    private void updateControlsDisplay(ProxyInstrument instrument) {
        controlsContainer.removeAll();
        
        if (instrument.getControlCodes() == null || instrument.getControlCodes().isEmpty()) {
            controlsContainer.add(new JLabel("No controls available"));
            return;
        }

        // Create hierarchical groups
        Map<String, ControlGroup> groups = new TreeMap<>();
        List<ProxyControlCode> singleControls = new ArrayList<>();
        
        // Group controls by common prefixes
        for (ProxyControlCode control : instrument.getControlCodes()) {
            String[] parts = control.getName().split(" ");
            String prefix = parts[0];
            
            // Special handling for numbered groups (Envelope N, Filter N, etc.)
            if (parts.length > 2 && isNumberedGroup(prefix)) {
                String groupKey = parts[0] + " " + parts[1];  // e.g., "Envelope 1", "Filter 2"
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

        // Create wrapper panel with GridBagLayout for better control
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        GridBagConstraints wrapperGbc = new GridBagConstraints();
        wrapperGbc.insets = new Insets(5, 5, 5, 5);
        wrapperGbc.fill = GridBagConstraints.NONE;
        wrapperGbc.anchor = GridBagConstraints.NORTHWEST;
        
        // Track current row and column
        int row = 0;
        int col = 0;
        int maxWidth = Math.max(800, controlsContainer.getParent().getWidth() - 50); // Minimum width
        int currentRowWidth = 0;

        // Add groups with proper wrapping
        for (ControlGroup group : groups.values()) {
            // Create and setup the group panel
            JPanel groupPanel = new JPanel(new GridBagLayout());
            groupPanel.setBorder(BorderFactory.createTitledBorder(group.name));
            layoutControlsInGroup(groupPanel, group);

            // Get preferred size before adding
            Dimension prefSize = groupPanel.getPreferredSize();

            // Check if we need to wrap to next row
            if (col > 0 && (currentRowWidth + prefSize.width > maxWidth)) {
                row++;
                col = 0;
                currentRowWidth = 0;
            }

            // Position the panel
            wrapperGbc.gridx = col++;
            wrapperGbc.gridy = row;
            wrapperPanel.add(groupPanel, wrapperGbc);

            currentRowWidth += prefSize.width + wrapperGbc.insets.left + wrapperGbc.insets.right;
        }

        // Add singles at the bottom if any
        if (!singleControls.isEmpty()) {
            wrapperGbc.gridx = 0;
            wrapperGbc.gridy = row + 1;
            wrapperGbc.gridwidth = GridBagConstraints.REMAINDER;
            JPanel singlesPanel = createSinglesPanel(singleControls, 6);
            wrapperPanel.add(singlesPanel, wrapperGbc);
        }

        // Add the wrapper to a constraint that allows proper scrolling
        GridBagConstraints containerGbc = new GridBagConstraints();
        containerGbc.anchor = GridBagConstraints.NORTHWEST;
        containerGbc.weightx = 1.0;
        containerGbc.weighty = 1.0;
        containerGbc.fill = GridBagConstraints.BOTH;
        controlsContainer.add(wrapperPanel, containerGbc);

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

    private void layoutSingleControls(JPanel panel, List<ProxyControlCode> controls) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.CENTER;
        
        int col = 0;
        int row = 0;
        
        for (ProxyControlCode control : controls) {
            gbc.gridx = col;
            gbc.gridy = row * 2;
            
            // Add label
            JLabel label = new JLabel(control.getName(), SwingConstants.CENTER);
            label.setPreferredSize(new Dimension(70, 20));
            panel.add(label, gbc);
            
            // Add control
            gbc.gridy = row * 2 + 1;
            Component controlComponent = createControl(control);
            panel.add(controlComponent, gbc);
            
            col++;
            if (col >= 6) {  // 6 controls per row for singles
                col = 0;
                row++;
            }
        }
    }

    private int estimateGroupWidth(ControlGroup group) {
        int controlWidth = 90; // Base width for a control
        int controls = group.controls.size() + group.subgroups.size();
        return Math.min(controls * controlWidth, 600); // Cap at reasonable maximum
    }

    private void layoutRow(JPanel container, List<ControlGroup> groups, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0 / groups.size(); // Equal weight for all groups in row
        
        for (int i = 0; i < groups.size(); i++) {
            gbc.gridx = i;
            JPanel groupPanel = createGroupPanel(groups.get(i));
            container.add(groupPanel, gbc);
        }
    }

    private JPanel createGroupPanel(ControlGroup group) {
        // Similar to existing addGroupToPanel but returns the panel instead of adding it
        JPanel groupPanel = new JPanel(new GridBagLayout());
        groupPanel.setBorder(BorderFactory.createTitledBorder(group.name));
        
        // ... existing group panel creation code ...
        
        return groupPanel;
    }

    private JPanel createSinglesPanel(List<ProxyControlCode> singleControls, int maxControlsPerRow) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        
        int col = 0;
        for (ProxyControlCode control : singleControls) {
            gbc.gridx = col;
            addSingleControl(panel, control, gbc);
            col = (col + 1) % maxControlsPerRow;  // Use maxControlsPerRow instead of CONTROLS_PER_ROW
        }
        
        return panel;
    }

    private void addSingleControl(JPanel panel, ProxyControlCode control, GridBagConstraints gbc) {
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

    private void addGroupToPanel(JPanel parent, ControlGroup group, GridBagConstraints gbc) {
        // Special handling for Position group (X/Y controls)
        if (group.name.equals("Position")) {
            addPositionControls(parent, group, gbc);
            return;
        }

        // Create direct grid layout for controls with border
        JPanel groupPanel = new JPanel(new GridBagLayout());
        groupPanel.setBorder(BorderFactory.createTitledBorder(group.name));
        
        GridBagConstraints innerGbc = new GridBagConstraints();
        innerGbc.insets = new Insets(2, 2, 2, 2);
        innerGbc.fill = GridBagConstraints.NONE;
        innerGbc.anchor = GridBagConstraints.WEST;
        
        // Calculate optimal columns based on number of controls
        int totalControls = group.controls.size();
        int optimalColumns = Math.min(totalControls, 4); // Max 4 controls per row
        
        // Layout controls directly in the group panel
        int currentRow = 0;
        int currentCol = 0;
        
        for (ProxyControlCode control : group.controls) {
            // Add label
            innerGbc.gridx = currentCol;
            innerGbc.gridy = currentRow * 2;
            innerGbc.gridheight = 1;
            JLabel label = new JLabel(getShortName(control.getName()), SwingConstants.LEFT);
            label.setPreferredSize(new Dimension(70, 20));
            groupPanel.add(label, innerGbc);
            
            // Add control
            innerGbc.gridy = currentRow * 2 + 1;
            Component controlComponent = createControl(control);
            groupPanel.add(controlComponent, innerGbc);
            
            currentCol++;
            if (currentCol >= optimalColumns) {
                currentCol = 0;
                currentRow++;
            }
        }

        // Add panel directly to parent
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.0;
        parent.add(groupPanel, gbc);
    }

    private void addPositionControls(JPanel parent, ControlGroup group, GridBagConstraints gbc) {
        JPanel positionPanel = new JPanel(new GridBagLayout());
        positionPanel.setBorder(BorderFactory.createTitledBorder("Position"));
        
        GridBagConstraints innerGbc = new GridBagConstraints();
        innerGbc.insets = new Insets(2, 2, 2, 2);
        
        // Sort controls to ensure X comes before Y
        group.controls.sort((a, b) -> a.getName().compareTo(b.getName()));
        
        // Add controls side by side
        int col = 0;
        for (ProxyControlCode control : group.controls) {
            innerGbc.gridx = col++;
            addSingleControl(positionPanel, control, innerGbc);
        }
        
        parent.add(positionPanel, gbc);
    }

    private void addControlToPanel(JPanel panel, ProxyControlCode control, int col, int row, GridBagConstraints gbc) {
        // Add label
        gbc.gridx = col;
        gbc.gridy = row * 2;
        gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.WEST; // Changed from CENTER to WEST
        JLabel label = new JLabel(getShortName(control.getName()), SwingConstants.LEFT); // Changed to LEFT
        label.setPreferredSize(new Dimension(70, 20));
        panel.add(label, gbc);
        
        // Add control
        gbc.gridy = row * 2 + 1;
        Component controlComponent = createControl(control);
        panel.add(controlComponent, gbc);
    }

    private String getShortName(String fullName) {
        // Get the last part of the name after the last space
        int lastSpace = fullName.lastIndexOf(' ');
        return lastSpace >= 0 ? fullName.substring(lastSpace + 1) : fullName;
    }

    private Component createControl(ProxyControlCode controlCode) {
        int lowerBound = controlCode.getLowerBound() != null ? controlCode.getLowerBound() : 0;
        int upperBound = controlCode.getUpperBound() != null ? controlCode.getUpperBound() : 127;
        
        if (controlCode.getCaptions() != null) {
            int captionCount = controlCode.getCaptions().size();
            logger.info("Creating control for " + controlCode.getName() + 
                       " with " + captionCount + " captions");
            
            if (captionCount == 2) {
                // Create a toggle switch instead of a toggle button
                ToggleSwitch toggle = new ToggleSwitch();
                
                // Sort captions by code to ensure consistent ordering
                List<ProxyCaption> sortedCaptions = new ArrayList<>(controlCode.getCaptions());
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
                // Create slider for more than 2 captions
                logger.info("Creating slider for " + controlCode.getName() + 
                          " with " + captionCount + " captions");
                JSlider slider = new JSlider(JSlider.VERTICAL, lowerBound, upperBound, lowerBound);
                slider.setPaintTicks(true);
                slider.setPaintLabels(true);
                slider.setMajorTickSpacing((upperBound - lowerBound) / 4);
                slider.setPreferredSize(new Dimension(50, 120));
                return slider;
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
        List<ProxyInstrument> instruments = redisService.findAllInstruments();
        
        // Sort instruments by name
        instruments.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        
        for (ProxyInstrument instrument : instruments) {
            instrumentSelector.addItem(instrument);
        }
        
        logger.info("Loaded " + instruments.size() + " instruments into selector");
    }

    private void refreshControlsPanel() {
        // Store currently selected instrument
        ProxyInstrument currentInstrument = (ProxyInstrument) instrumentSelector.getSelectedItem();
        
        // Clear and reload instruments from database
        refreshInstruments();
        
        // If there was a previously selected instrument, try to reselect it
        if (currentInstrument != null) {
            for (int i = 0; i < instrumentSelector.getItemCount(); i++) {
                ProxyInstrument item = instrumentSelector.getItemAt(i);
                if (item.getId().equals(currentInstrument.getId())) {
                    instrumentSelector.setSelectedIndex(i);
                    break;
                }
            }
        }
        
        // Force update of controls display
        ProxyInstrument selected = (ProxyInstrument) instrumentSelector.getSelectedItem();
        if (selected != null) {
            updateControlsDisplay(selected);
        }
        
        logger.info("Controls panel refreshed");
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
                ProxyControlCode control = group.controls.get(currentControl++);
                
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
