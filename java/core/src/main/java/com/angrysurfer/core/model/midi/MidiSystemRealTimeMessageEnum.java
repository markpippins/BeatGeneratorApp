package com.angrysurfer.core.model.midi;

import java.util.HashMap;
import java.util.Map;

public final class MidiSystemRealTimeMessageEnum {
    // Integer constants
    public static final int CLOCK = 0xF8;          // 248
    public static final int START = 0xFA;          // 250
    public static final int CONTINUE = 0xFB;       // 251
    public static final int STOP = 0xFC;           // 252
    public static final int ACTIVE_SENSING = 0xFE; // 254
    public static final int RESET = 0xFF;          // 255

    // String constants
    public static final String NAME_CLOCK = "clock";
    public static final String NAME_START = "start";
    public static final String NAME_CONTINUE = "continue";
    public static final String NAME_STOP = "stop";
    public static final String NAME_ACTIVE_SENSING = "activesensing";
    public static final String NAME_RESET = "reset";

    private static final Map<String, Integer> NAME_TO_VALUE = new HashMap<>();
    private static final Map<Integer, String> VALUE_TO_NAME = new HashMap<>();

    static {
        register(NAME_CLOCK, CLOCK);
        register(NAME_START, START);
        register(NAME_CONTINUE, CONTINUE);
        register(NAME_STOP, STOP);
        register(NAME_ACTIVE_SENSING, ACTIVE_SENSING);
        register(NAME_RESET, RESET);
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

    private MidiSystemRealTimeMessageEnum() {}
}

