package com.angrysurfer.core.model.feature;

import javax.sound.midi.ShortMessage;
import java.util.HashMap;
import java.util.Map;

public interface MidiMessage {
    public static final Map<Integer, String> commands = new HashMap<>() {{
        put(ShortMessage.CONTROL_CHANGE, "Control Change");
        put(ShortMessage.NOTE_ON, "Note On");
        put(ShortMessage.NOTE_OFF, "Note Off");
        put(ShortMessage.PROGRAM_CHANGE, "Program Change");
        put(ShortMessage.PITCH_BEND, "Pitch Bend");
        put(ShortMessage.POLY_PRESSURE, "Poly Pressure");
        put(ShortMessage.CHANNEL_PRESSURE, "Channel Pressure");
        put(ShortMessage.SONG_SELECT, "Song Select");
        put(ShortMessage.START, "Start");
        put(ShortMessage.STOP, "Stop");
        put(ShortMessage.CONTINUE, "Continue");
    }};

    static String lookupCommand(int key) {
        return commands.getOrDefault(key, "unknown");
    }

}
