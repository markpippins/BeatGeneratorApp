package com.angrysurfer.core.model.midi;

import java.util.HashMap;
import java.util.Map;

public final class MidiControlMessageEnum {
    // Integer constants
    public static final int ALL_SOUND_OFF = 0x78;        // 120
    public static final int RESET_ALL_CONTROLLERS = 0x79; // 121
    public static final int LOCAL_CONTROL = 0x7A;         // 122
    public static final int ALL_NOTES_OFF = 0x7B;         // 123
    public static final int OMNI_MODE_OFF = 0x7C;         // 124
    public static final int OMNI_MODE_ON = 0x7D;          // 125
    public static final int MONO_MODE_ON = 0x7E;          // 126
    public static final int POLY_MODE_ON = 0x7F;          // 127

    // String constants
    public static final String NAME_ALL_SOUND_OFF = "allsoundoff";
    public static final String NAME_RESET_ALL_CONTROLLERS = "resetallcontrollers";
    public static final String NAME_LOCAL_CONTROL = "localcontrol";
    public static final String NAME_ALL_NOTES_OFF = "allnotesoff";
    public static final String NAME_OMNI_MODE_OFF = "omnimodeoff";
    public static final String NAME_OMNI_MODE_ON = "omnimodeon";
    public static final String NAME_MONO_MODE_ON = "monomodeon";
    public static final String NAME_POLY_MODE_ON = "polymodeon";

    private static final Map<String, Integer> NAME_TO_VALUE = new HashMap<>();
    private static final Map<Integer, String> VALUE_TO_NAME = new HashMap<>();

    static {
        register(NAME_ALL_SOUND_OFF, ALL_SOUND_OFF);
        register(NAME_RESET_ALL_CONTROLLERS, RESET_ALL_CONTROLLERS);
        register(NAME_LOCAL_CONTROL, LOCAL_CONTROL);
        register(NAME_ALL_NOTES_OFF, ALL_NOTES_OFF);
        register(NAME_OMNI_MODE_OFF, OMNI_MODE_OFF);
        register(NAME_OMNI_MODE_ON, OMNI_MODE_ON);
        register(NAME_MONO_MODE_ON, MONO_MODE_ON);
        register(NAME_POLY_MODE_ON, POLY_MODE_ON);
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

    private MidiControlMessageEnum() {
        // Prevent instantiation
    }
}
