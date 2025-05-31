package com.angrysurfer.beats;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Symbols {
    public static final Map<String, String> SYMBOLS;

    public static final String PLAY = "play";
    public static final String STOP = "stop";
    public static final String PAUSE = "pause";
    public static final String RECORD = "record";
    public static final String REWIND = "rewind";
    public static final String FAST_FORWARD = "fast_forward";
    public static final String PANIC = "panic";
    public static final String EDIT = "edit";
    public static final String GRID = "grid";
    public static final String SETTINGS = "settings";
    public static final String RESTART = "restart";
    public static final String SAVE = "save";
    public static final String LOAD = "load";
    public static final String DICE = "dice";
    public static final String METRONOME = "metronome";
    // public static final String TRIGGER = "trigger";
    public static final String CYCLE = "cycle";
    public static final String LOOP = "loop";
    public static final String ALL_NOTES_OFF = "all_notes_off";
    public static final String MUTE = "mute";
    public static final String UNMUTE = "unmute";

    public static final String TRIGGER = "trigger";
    public static final String FOR = "for"; // For the "⟳" symbol
    public static final String TRIGGER_FOR = "trigger_for"; // For the "⟳" symbol

    public static final String NEW = "new";
    public static final String OPEN = "open";
    public static final String SAVE_AS = "save_as";
    public static final String EXPORT = "export";
    public static final String IMPORT = "import";
    public static final String CLOSE = "close";
    public static final String EXIT = "exit";
    public static final String HELP = "help";
    public static final String ABOUT = "about";
    public static final String PREFERENCES = "preferences";
    public static final String UNDO = "undo";
    public static final String REDO = "redo";
    public static final String CUT = "cut";
    public static final String COPY = "copy";
    public static final String PASTE = "paste";
    public static final String DELETE = "delete";
    public static final String SELECT_ALL = "select_all";
    public static final String FIND = "find";
    public static final String FIND_NEXT = "find_next";
    public static final String FIND_PREVIOUS = "find_previous";
    public static final String REPLACE = "replace";
    public static final String REPLACE_ALL = "replace_all";

    public static final String SNAPSHOT = "snapshot";

    public static final String SELECT = "select";
    public static final String DESELECT = "deselect";
    public static final String HIGHLIGHT = "highlight";

    public static final String MIX = "mix";
    public static final String PAN = "pan";
    public static final String EQ = "eq";
    public static final String COMPRESS = "compress";
    public static final String LIMIT = "limit";
    public static final String EXPAND = "expand";
    public static final String REVERB = "reverb";
    public static final String DELAY = "delay";
    public static final String DISTORTION = "distortion";
    public static final String FLANGER = "flanger";
    public static final String PHASER = "phaser";
    public static final String CHORUS = "chorus";
    public static final String TREMOLO = "tremolo";
    public static final String FILTER = "filter";
    public static final String NOISE_GATE = "noise_gate";
    public static final String SPECTRUM_ANALYZER = "spectrum_analyzer";
    public static final String OSCILLOSCOPE = "oscilloscope";
    public static final String SPECTROGRAM = "spectrogram";
    public static final String MIDI = "midi";
    public static final String AUDIO = "audio";
    public static final String MIDI_LEARN = "midi_learn";
    public static final String MIDI_MAP = "midi_map";
    public static final String MIDI_CC = "midi_cc";
    public static final String MIDI_NOTE = "midi_note";
    public static final String MIDI_CHANNEL = "midi_channel";
    public static final String MIDI_IN = "midi_in";
    public static final String MIDI_OUT = "midi_out";
    public static final String MIDI_THROTTLE = "midi_throttle";
    public static final String MIDI_MONITOR = "midi_monitor";
    public static final String MIDI_MONITOR_IN = "midi_monitor_in";
    public static final String MIDI_MONITOR_OUT = "midi_monitor_out";
    public static final String MIDI_MONITOR_CC = "midi_monitor_cc";
    public static final String MIDI_MONITOR_NOTE = "midi_monitor_note";
    public static final String MIDI_MONITOR_CHANNEL = "midi_monitor_channel";
    public static final String MIDI_MONITOR_IN_CC = "midi_monitor_in_cc";
    public static final String MIDI_MONITOR_IN_NOTE = "midi_monitor_in_note";
    public static final String MIDI_MONITOR_IN_CHANNEL = "midi_monitor_in_channel";
    public static final String MIDI_MONITOR_OUT_CC = "midi_monitor_out_cc";
    public static final String MIDI_MONITOR_OUT_NOTE = "midi_monitor_out_note";
    public static final String MIDI_MONITOR_OUT_CHANNEL = "midi_monitor_out_channel";
    public static final String MIDI_MONITOR_IN_OUT_CC = "midi_monitor_in_out_cc";
    public static final String MIDI_MONITOR_IN_OUT_NOTE = "midi_monitor_in_out_note";
    public static final String MIDI_MONITOR_IN_OUT_CHANNEL = "midi_monitor_in_out_channel";
    public static final String MIDI_MONITOR_IN_OUT = "midi_monitor_in_out";
    public static final String MIDI_MONITOR_IN_OUT_CC_NOTE = "midi_monitor_in_out_cc_note";
    public static final String REFRESH = "refresh";

    static {
        Map<String, String> map = new HashMap<>();
        map.put(PLAY, "▶");
        map.put(STOP, "■");
        map.put(MUTE, "🔇");
        map.put(UNMUTE, "🔊");
        // map.put(MUTE, "🔈");
        // map.put(UNMUTE, "🔉");
        // map.put(MUTE, "🔊");
        // map.put(UNMUTE, "🔊")
        // map.put(MUTE, "🔈")

        // map.put(MUTE, "🔇");
        // map.put(UNMUTE, "🔊");
        map.put(PAUSE, "⏸");
        map.put(RECORD, "⏺");
        map.put(REWIND, "⏪");
        map.put(FAST_FORWARD, "⏩");

        map.put(SNAPSHOT, "\uD83D\uDCF7"); // 📷 Camera

        map.put(PANIC, "💥");
        map.put(EDIT, "✏️");
        map.put(GRID, "☷");
        map.put(SETTINGS, "⚙");
        map.put(RESTART, "↺");
        map.put(SAVE, "💾");
        map.put(LOAD, "📂");
        map.put(DICE, "🎲");
        map.put(METRONOME, "🕰️");
        // map.put(TRIGGER, "🔔");
        map.put(ALL_NOTES_OFF, "🔕");
        map.put(CYCLE, "🔄");
        map.put(LOOP, "🔁");
        map.put(REFRESH, "🔄");

        map.put(NEW, "🆕");
        map.put(OPEN, "📂");
        map.put(SAVE, "💾");
        map.put(SAVE_AS, "💾");
        map.put(EXPORT, "📤");
        map.put(IMPORT, "📥");
        map.put(CLOSE, "❌");
        map.put(EXIT, "🚪");
        map.put(HELP, "❓");
        map.put(ABOUT, "ℹ️");
        map.put(PREFERENCES, "⚙️");
        map.put(UNDO, "↩️");
        map.put(REDO, "↪️");
        map.put(CUT, "✂️");
        map.put(COPY, "⎘");
        map.put(PASTE, "📋");
        map.put(DELETE, "🗑️");
        map.put(SELECT_ALL, "🔲");
        map.put(FIND, "🔍");
        map.put(FIND_NEXT, "🔜");
        map.put(FIND_PREVIOUS, "🔙");
        map.put(REPLACE, "🔄");
        map.put(REPLACE_ALL, "🔄");
        map.put(SELECT, "✅");

        map.put(DESELECT, "❌");
        map.put(HIGHLIGHT, "🔆");
        map.put(MIX, "🎚️");
        map.put(PAN, "🔊");
        map.put(EQ, "🎛️");
        map.put(COMPRESS, "🔊");
        map.put(LIMIT, "🔊");
        map.put(EXPAND, "🔊");
        map.put(REVERB, "🔊");
        map.put(DELAY, "🔊");
        map.put(DISTORTION, "🔊");
        map.put(FLANGER, "🔊");
        map.put(PHASER, "🔊");
        map.put(CHORUS, "🔊");
        map.put(TREMOLO, "🔊");
        map.put(FILTER, "🔊");
        map.put(NOISE_GATE, "🔊");
        map.put(SPECTRUM_ANALYZER, "🔊");
        map.put(OSCILLOSCOPE, "🔊");
        map.put(SPECTROGRAM, "🔊");
        map.put(MIDI, "🎹");
        map.put(AUDIO, "🔊");


        SYMBOLS = Collections.unmodifiableMap(map);
    }

    public static String get(String key) {
        return SYMBOLS.getOrDefault(key, "❓"); // Return a default symbol if not found
    }

    public static String get(String key, String defaultSymbol) {
        return SYMBOLS.getOrDefault(key, defaultSymbol); // Return a custom default symbol if not found
    }
}
