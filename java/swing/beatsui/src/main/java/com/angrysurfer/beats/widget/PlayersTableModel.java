package com.angrysurfer.beats.widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Vector;
import java.util.stream.Collectors;

import javax.swing.table.DefaultTableModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;

public class PlayersTableModel extends DefaultTableModel {
    private static final Logger logger = LoggerFactory.getLogger(PlayersTableModel.class.getName());

    // Column name constants
    public static final String COL_ID = "ID";
    public static final String COL_NAME = "Name";
    public static final String COL_INSTRUMENT = "Instrument";
    public static final String COL_CHANNEL = "Channel";
    public static final String COL_SWING = "Swing";
    public static final String COL_LEVEL = "Level";
    public static final String COL_MUTE = "Mute";
    public static final String COL_NOTE = "Note";
    public static final String COL_MIN_VEL = "Min Vel";
    public static final String COL_MAX_VEL = "Max Vel";
    public static final String COL_PRESET = "Preset";
    public static final String COL_STICKY = "Sticky";
    public static final String COL_PROBABILITY = "Prob";
    public static final String COL_RANDOM = "Random";
    public static final String COL_RATCHET_COUNT = "Ratchet #";
    public static final String COL_RATCHET_INTERVAL = "Ratchet Int";
    public static final String COL_INT_BEATS = "Int Beats";
    public static final String COL_INT_BARS = "Int Bars";
    public static final String COL_PAN = "Pan";
    public static final String COL_PRESERVE = "Preserve";
    public static final String COL_SPARSE = "Sparse";

    public static final Set<String> COLUMNS = new LinkedHashSet<>(Arrays.asList(
            COL_ID, COL_NAME, COL_INSTRUMENT, COL_CHANNEL, COL_PRESET, COL_NOTE, COL_LEVEL, COL_MUTE,
            COL_PAN, COL_MIN_VEL, COL_MAX_VEL, COL_SWING,
            COL_PROBABILITY, COL_RANDOM, COL_SPARSE,
            COL_RATCHET_COUNT, COL_RATCHET_INTERVAL, COL_INT_BEATS,
            COL_INT_BARS, COL_STICKY, COL_PRESERVE));

    // IMPORTANT: Make the ID column hidden
    private static final Set<String> HIDDEN_COLUMN_NAMES = Set.of(COL_ID);

    private static final Set<String> STRING_COLUMN_NAMES = Set.of(COL_ID, COL_NAME, COL_INSTRUMENT);
    private static final Set<String> BOOLEAN_COLUMN_NAMES = Set.of(
            COL_MUTE, COL_STICKY, COL_PRESERVE);

    private static final int[] BOOLEAN_COLUMNS = BOOLEAN_COLUMN_NAMES.stream()
            .mapToInt(name -> new ArrayList<>(COLUMNS).indexOf(name))
            .toArray();

    private static final int[] STRING_COLUMNS = STRING_COLUMN_NAMES.stream()
            .mapToInt(name -> new ArrayList<>(COLUMNS).indexOf(name))
            .toArray();

    private static final int[] NUMERIC_COLUMNS = initNumericColumns();

    private static int[] initNumericColumns() {
        Set<Integer> booleanCols = Arrays.stream(BOOLEAN_COLUMNS).boxed().collect(Collectors.toSet());
        Set<Integer> stringCols = Arrays.stream(STRING_COLUMNS).boxed().collect(Collectors.toSet());
        List<Integer> numericCols = new ArrayList<>();
        
        for (int i = 0; i < getColumnNames().length; i++) {
            if (!booleanCols.contains(i) && !stringCols.contains(i)) {
                numericCols.add(i);
            }
        }
        
        return numericCols.stream().mapToInt(Integer::intValue).toArray();
    }

    public PlayersTableModel() {
        super(getColumnNames(), 0);
    }

    @Override
    public Class<?> getColumnClass(int column) {
        // Return Boolean.class for boolean columns
        for (int booleanColumn : BOOLEAN_COLUMNS) {
            if (column == booleanColumn) {
                return Boolean.class;
            }
        }
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    public static String[] getColumnNames() {
        return COLUMNS.toArray(new String[0]);
    }

    public int getColumnIndex(String columnName) {
        return new ArrayList<>(COLUMNS).indexOf(columnName);
    }

    public void updateInstrumentCell(Vector rowData, int columnIndex, Player player) {
        String instrumentName = "";

        try {
            if (player.getInstrumentId() != null) {
                Instrument instrument = InstrumentManager.getInstance()
                        .getInstrumentFromCache(player.getInstrumentId());
                if (instrument != null) {
                    instrumentName = instrument.getName();

                    // Initialize device if needed
                    if (Objects.isNull(instrument.getDevice()) && Objects.nonNull(instrument.getDeviceName())) {
                        try {
                            javax.sound.midi.MidiDevice device = DeviceManager
                                    .getMidiDevice(instrument.getDeviceName());

                            if (device != null) {
                                instrument.setDevice(device);
                                logger.info("Initialized device for instrument: " + instrument.getName() +
                                        " with device: " + device.getDeviceInfo().getName());
                            } else {
                                logger.error("Device not found: " + instrument.getDeviceName() +
                                        " for instrument: " + instrument.getName());
                            }
                        } catch (Exception e) {
                            logger.error("Error initializing device for instrument " +
                                    instrument.getName() + ": " + e.getMessage());
                        }
                    }

                    player.setInstrument(instrument);
                }
            }
            rowData.set(columnIndex, instrumentName);
        } catch (Exception e) {
            logger.error("Error updating instrument cell: " + e.getMessage());
            rowData.set(columnIndex, "Error");
        }
    }

    public void updatePlayerRow(Player player, int modelRow) {
        if (player == null) return;

        try {
            setValueAt(player.getName(), modelRow, getColumnIndex(COL_NAME));
            setValueAt(player.getNote(), modelRow, getColumnIndex(COL_NOTE));
            setValueAt(player.getLevel(), modelRow, getColumnIndex(COL_LEVEL));
            setValueAt(player.isMuted(), modelRow, getColumnIndex(COL_MUTE));
            setValueAt(player.getProbability(), modelRow, getColumnIndex(COL_PROBABILITY));
            setValueAt(player.getSparse(), modelRow, getColumnIndex(COL_SPARSE));
            setValueAt(player.getSwing(), modelRow, getColumnIndex(COL_SWING));
            setValueAt(player.getRandomDegree(), modelRow, getColumnIndex(COL_RANDOM));
            setValueAt(player.getMinVelocity(), modelRow, getColumnIndex(COL_MIN_VEL));
            setValueAt(player.getMaxVelocity(), modelRow, getColumnIndex(COL_MAX_VEL));
            setValueAt(player.getPreset(), modelRow, getColumnIndex(COL_PRESET));
            setValueAt(player.getPanPosition(), modelRow, getColumnIndex(COL_PAN));

            // Special handling for instrument column
            updateInstrumentCell(getDataVector().get(modelRow), getColumnIndex(COL_INSTRUMENT), player);

            fireTableRowsUpdated(modelRow, modelRow);
        } catch (Exception e) {
            logger.error("Error updating player row: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addPlayerRow(Player player) {
        Object[] rowData = new Object[COLUMNS.size()];

        rowData[getColumnIndex(COL_ID)] = player.getId();  // Store player ID
        rowData[getColumnIndex(COL_NAME)] = player.getName();
        rowData[getColumnIndex(COL_CHANNEL)] = player.getChannel();
        rowData[getColumnIndex(COL_SWING)] = player.getSwing();
        rowData[getColumnIndex(COL_LEVEL)] = player.getLevel();
        rowData[getColumnIndex(COL_MUTE)] = player.isMuted();
        rowData[getColumnIndex(COL_NOTE)] = player.getNote();
        rowData[getColumnIndex(COL_MIN_VEL)] = player.getMinVelocity();
        rowData[getColumnIndex(COL_MAX_VEL)] = player.getMaxVelocity();
        rowData[getColumnIndex(COL_PRESET)] = player.getPreset();
        rowData[getColumnIndex(COL_STICKY)] = player.getStickyPreset();
        rowData[getColumnIndex(COL_PROBABILITY)] = player.getProbability();
        rowData[getColumnIndex(COL_RANDOM)] = player.getRandomDegree();
        rowData[getColumnIndex(COL_RATCHET_COUNT)] = player.getRatchetCount();
        rowData[getColumnIndex(COL_RATCHET_INTERVAL)] = player.getRatchetInterval();
        rowData[getColumnIndex(COL_INT_BEATS)] = player.getInternalBeats();
        rowData[getColumnIndex(COL_INT_BARS)] = player.getInternalBars();
        rowData[getColumnIndex(COL_PAN)] = player.getPanPosition();
        rowData[getColumnIndex(COL_PRESERVE)] = player.getPreserveOnPurge();
        rowData[getColumnIndex(COL_SPARSE)] = player.getSparse();

        // Handle instrument separately
        Vector<Object> vectorData = new Vector<>(Arrays.asList(rowData));
        updateInstrumentCell(vectorData, getColumnIndex(COL_INSTRUMENT), player);
        addRow(vectorData);
    }

    public static int[] getBooleanColumns() {
        return BOOLEAN_COLUMNS;
    }

    public static int[] getNumericColumns() {
        return NUMERIC_COLUMNS;
    }

    public static int[] getStringColumns() {
        return STRING_COLUMNS;
    }

    // Add method to get hidden columns
    public static Set<String> getHiddenColumns() {
        return HIDDEN_COLUMN_NAMES;
    }
}