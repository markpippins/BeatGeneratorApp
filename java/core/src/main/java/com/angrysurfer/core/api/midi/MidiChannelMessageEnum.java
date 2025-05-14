package com.angrysurfer.core.api.midi;

import java.util.HashMap;
import java.util.Map;

public final class MidiChannelMessageEnum {
    // Integer constants
    public static final int NOTE_OFF = 0x8;
    public static final int NOTE_ON = 0x9;
    public static final int KEY_AFTERTOUCH = 0xA;
    public static final int CONTROL_CHANGE = 0xB;
    public static final int PROGRAM_CHANGE = 0xC;
    public static final int CHANNEL_AFTERTOUCH = 0xD;
    public static final int PITCH_BEND = 0xE;

    // String constants
    public static final String NAME_NOTE_OFF = "noteoff";
    public static final String NAME_NOTE_ON = "noteon";
    public static final String NAME_KEY_AFTERTOUCH = "keyaftertouch";
    public static final String NAME_CONTROL_CHANGE = "controlchange";
    public static final String NAME_PROGRAM_CHANGE = "programchange";
    public static final String NAME_CHANNEL_AFTERTOUCH = "channelaftertouch";
    public static final String NAME_PITCH_BEND = "pitchbend";

    private static final Map<String, Integer> NAME_TO_VALUE = new HashMap<>();
    private static final Map<Integer, String> VALUE_TO_NAME = new HashMap<>();

    static {
        register(NAME_NOTE_OFF, NOTE_OFF);
        register(NAME_NOTE_ON, NOTE_ON);
        register(NAME_KEY_AFTERTOUCH, KEY_AFTERTOUCH);
        register(NAME_CONTROL_CHANGE, CONTROL_CHANGE);
        register(NAME_PROGRAM_CHANGE, PROGRAM_CHANGE);
        register(NAME_CHANNEL_AFTERTOUCH, CHANNEL_AFTERTOUCH);
        register(NAME_PITCH_BEND, PITCH_BEND);
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

    private MidiChannelMessageEnum() {
        // Prevent instantiation
    }
}
