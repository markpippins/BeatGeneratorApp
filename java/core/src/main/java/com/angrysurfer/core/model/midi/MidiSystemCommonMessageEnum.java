package com.angrysurfer.core.model.midi;

import java.util.HashMap;
import java.util.Map;

public final class MidiSystemCommonMessageEnum {
    // Integer constants
    public static final int SYSEX = 0xF0;          // 240
    public static final int TIMECODE = 0xF1;       // 241
    public static final int SONG_POSITION = 0xF2;  // 242
    public static final int SONG_SELECT = 0xF3;    // 243
    public static final int TUNE_REQUEST = 0xF6;   // 246
    public static final int SYSEX_END = 0xF7;      // 247

    // String constants
    public static final String NAME_SYSEX = "sysex";
    public static final String NAME_TIMECODE = "timecode";
    public static final String NAME_SONG_POSITION = "songposition";
    public static final String NAME_SONG_SELECT = "songselect";
    public static final String NAME_TUNE_REQUEST = "tunerequest";
    public static final String NAME_SYSEX_END = "sysexend";

    private static final Map<String, Integer> NAME_TO_VALUE = new HashMap<>();
    private static final Map<Integer, String> VALUE_TO_NAME = new HashMap<>();

    static {
        register(NAME_SYSEX, SYSEX);
        register(NAME_TIMECODE, TIMECODE);
        register(NAME_SONG_POSITION, SONG_POSITION);
        register(NAME_SONG_SELECT, SONG_SELECT);
        register(NAME_TUNE_REQUEST, TUNE_REQUEST);
        register(NAME_SYSEX_END, SYSEX_END);
    }

    private static void register(String name, int value) {
        NAME_TO_VALUE.put(name, value);
        VALUE_TO_NAME.put(value, name);
    }

    public static int getDecimalValue(String name) {
        return NAME_TO_VALUE.getOrDefault(name.toLowerCase(), -1);
    }

    public static String getHexValue(String name) {
        int value = getDecimalValue(name);
        return value >= 0 ? String.format("0x%X", value) : null;
    }

    public static String getName(int value) {
        return VALUE_TO_NAME.getOrDefault(value, "unknown");
    }

    private MidiSystemCommonMessageEnum() {}
}
