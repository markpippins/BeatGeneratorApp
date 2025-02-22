package com.angrysurfer.beatsui.api;

public class Commands {
    // Transport Commands
    public static final String REWIND = "REWIND";
    public static final String PAUSE = "PAUSE";
    public static final String RECORD = "RECORD";
    public static final String STOP = "STOP";
    public static final String PLAY = "PLAY";
    public static final String FORWARD = "FORWARD";

    // File Commands
    public static final String NEW_FILE = "NEW_FILE";
    public static final String OPEN_FILE = "OPEN_FILE";
    public static final String SAVE_FILE = "SAVE_FILE";
    public static final String SAVE_AS = "SAVE_AS";
    public static final String EXIT = "EXIT";

    // Edit Commands
    public static final String CUT = "CUT";
    public static final String COPY = "COPY";
    public static final String PASTE = "PASTE";

    // Theme Commands
    public static final String CHANGE_THEME = "CHANGE_THEME";

    // Player selection commands
    public static final String PLAYER_SELECTED = "PLAYER_SELECTED";
    public static final String PLAYER_UNSELECTED = "PLAYER_UNSELECTED";

    // Control code and caption commands
    public static final String CONTROL_CODE_SELECTED = "CONTROL_CODE_SELECTED";
    public static final String CONTROL_CODE_UNSELECTED = "CONTROL_CODE_UNSELECTED";
    public static final String CONTROL_CODE_ADDED = "CONTROL_CODE_ADDED";
    public static final String CONTROL_CODE_UPDATED = "CONTROL_CODE_UPDATED";
    public static final String CONTROL_CODE_DELETED = "CONTROL_CODE_DELETED";
    public static final String CAPTION_ADDED = "CAPTION_ADDED";
    public static final String CAPTION_UPDATED = "CAPTION_UPDATED";
    public static final String CAPTION_DELETED = "CAPTION_DELETED";

    // Ticker selection commands
    public static final String TICKER_SELECTED = "TICKER_SELECTED";
    public static final String TICKER_UPDATED = "TICKER_UPDATED";

    // Piano key commands
    public static final String KEY_PRESSED = "KEY_PRESSED";
    public static final String KEY_HELD = "KEY_HELD";
    public static final String KEY_RELEASED = "KEY_RELEASED";

    // Visualization Commands
    public static final String START_VISUALIZATION = "START_VISUALIZATION";
    public static final String STOP_VISUALIZATION = "STOP_VISUALIZATION";

    // Visualization Commands
    public static final String VISUALIZATION_SELECTED = "VISUALIZATION_SELECTED";
    public static final String VISUALIZATION_STARTED = "VISUALIZATION_STARTED";
    public static final String VISUALIZATION_STOPPED = "VISUALIZATION_STOPPED";
    public static final String VISUALIZATION_REGISTERED = "VISUALIZATION_REGISTERED";

}
