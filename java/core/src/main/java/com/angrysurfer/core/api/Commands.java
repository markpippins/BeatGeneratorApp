package com.angrysurfer.core.api;

public class Commands {
    // Transport Commands
    public static final String REWIND = "REWIND";
    public static final String PAUSE = "PAUSE";
    public static final String RECORD = "RECORD";
    public static final String STOP = "STOP";
    public static final String PLAY = "PLAY";
    public static final String FORWARD = "FORWARD";

    // Transport commands
    public static final String TRANSPORT_REWIND = "TRANSPORT_REWIND";
    public static final String TRANSPORT_PAUSE = "TRANSPORT_PAUSE";
    public static final String TRANSPORT_RECORD = "TRANSPORT_RECORD";
    public static final String TRANSPORT_STOP = "TRANSPORT_STOP";
    public static final String TRANSPORT_PLAY = "TRANSPORT_PLAY";
    public static final String TRANSPORT_FORWARD = "TRANSPORT_FORWARD";

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
    public static final String PLAYER = "PLAYER";

    public static final String PLAYER_SELECTED = "PLAYER_SELECTED";
    public static final String PLAYER_UNSELECTED = "PLAYER_UNSELECTED";
    public static final String PLAYER_ADDED = "PLAYER_ADDED";
    public static final String PLAYER_UPDATED = "PLAYER_UPDATED";

    // Player CRUD commands
    public static final String PLAYER_ADD_REQUEST = "PLAYER_ADD_REQUEST";
    public static final String PLAYER_EDIT_REQUEST = "PLAYER_EDIT_REQUEST";
    public static final String PLAYER_DELETE_REQUEST = "PLAYER_DELETE_REQUEST";
    public static final String PLAYER_EDIT_CANCELLED = "PLAYER_EDIT_CANCELLED";
    public static final String EDIT_PLAYER_PARAMETERS = "EDIT_PLAYER_PARAMETERS";  // Add this line

    // Player navigation commands
    public static final String PLAYER_ROW_INDEX_REQUEST = "PLAYER_ROW_INDEX_REQUEST";
    public static final String PLAYER_ROW_INDEX_RESPONSE = "PLAYER_ROW_INDEX_RESPONSE";

    // Rule selection commands
    public static final String RULE = "RULE";

    public static final String RULE_SELECTED = "RULE_SELECTED";
    public static final String RULE_UNSELECTED = "RULE_UNSELECTED";
    public static final String RULE_ADDED = "RULE_ADDED";

    // Rule CRUD commands
    public static final String RULE_ADD_REQUEST = "RULE_ADD_REQUEST";
    public static final String RULE_EDIT_REQUEST = "RULE_EDIT_REQUEST";
    public static final String RULE_DELETE_REQUEST = "RULE_DELETE_REQUEST";
    public static final String RULE_UPDATED = "RULE_UPDATED";

    // Additional Rule events
    public static final String RULE_LOADED = "RULE_LOADED";
    public static final String RULES_CLEARED = "RULES_CLEARED";

    // Rule editor commands
    public static final String SHOW_RULE_EDITOR = "SHOW_RULE_EDITOR";
    public static final String SHOW_RULE_EDITOR_OK = "SHOW_RULE_EDITOR_OK";
    public static final String SHOW_RULE_EDITOR_CANCEL = "SHOW_RULE_EDITOR_CANCEL";

    // Control code and caption commands
    public static final String CONTROL_CODE_SELECTED = "CONTROL_CODE_SELECTED";
    public static final String CONTROL_CODE_UNSELECTED = "CONTROL_CODE_UNSELECTED";
    public static final String CONTROL_CODE_ADDED = "CONTROL_CODE_ADDED";
    public static final String CONTROL_CODE_UPDATED = "CONTROL_CODE_UPDATED";
    public static final String CONTROL_CODE_DELETED = "CONTROL_CODE_DELETED";
    public static final String CAPTION_ADDED = "CAPTION_ADDED";
    public static final String CAPTION_UPDATED = "CAPTION_UPDATED";
    public static final String CAPTION_DELETED = "CAPTION_DELETED";

    // Control sending commands
    public static final String SEND_ALL_CONTROLS = "SEND_ALL_CONTROLS";

    // Ticker selection commands
    public static final String TICKER = "TICKER";
    public static final String TICKER_SELECTED = "TICKER_SELECTED";
    public static final String TICKER_UPDATED = "TICKER_UPDATED";

    // Ticker state commands
    public static final String TICKER_UNSELECTED = "TICKER_UNSELECTED";
    public static final String TICKER_DELETED = "TICKER_DELETED";
    public static final String TICKER_CREATED = "TICKER_CREATED";  // Add this line

    // Ticker-Player-Rule relationship commands
    public static final String TICKER_REQUEST = "TICKER_REQUEST";
    public static final String TICKER_LOADED = "TICKER_LOADED";
    public static final String TICKER_CHANGED = "TICKER_CHANGED";

    public static final String PLAYER_ADDED_TO_TICKER = "PLAYER_ADDED_TO_TICKER";
    public static final String PLAYER_REMOVED_FROM_TICKER = "PLAYER_REMOVED_FROM_TICKER";
    public static final String RULE_ADDED_TO_PLAYER = "RULE_ADDED_TO_PLAYER";
    public static final String RULE_REMOVED_FROM_PLAYER = "RULE_REMOVED_FROM_PLAYER";

    // Piano key commands
    public static final String KEY_PRESSED = "KEY_PRESSED";
    public static final String KEY_HELD = "KEY_HELD";
    public static final String KEY_RELEASED = "KEY_RELEASED";

    // Visualization Commands
    public static final String START_VISUALIZATION = "START_VISUALIZATION";
    public static final String STOP_VISUALIZATION = "STOP_VISUALIZATION";
    public static final String LOCK_CURRENT_VISUALIZATION = "LOCK_CURRENT_VISUALIZATION";
    public static final String UNLOCK_CURRENT_VISUALIZATION = "UNLOCK_CURRENT_VISUALIZATION";
    public static final String VISUALIZATION_LOCKED = "VISUALIZATION_LOCKED";
    public static final String VISUALIZATION_UNLOCKED = "VISUALIZATION_UNLOCKED";

    // Visualization Commands
    public static final String VISUALIZATION_SELECTED = "VISUALIZATION_SELECTED";
    public static final String VISUALIZATION_STARTED = "VISUALIZATION_STARTED";
    public static final String VISUALIZATION_STOPPED = "VISUALIZATION_STOPPED";
    public static final String VISUALIZATION_REGISTERED = "VISUALIZATION_REGISTERED";
    public static final String VISUALIZATION_HANDLER_REFRESH_REQUESTED = "VISUALIZATION_HANDLER_REFRESH_REQUESTED";

    // Logging commands
    public static final String LOG_DEBUG = "LOG_DEBUG";
    public static final String LOG_INFO = "LOG_INFO";
    public static final String LOG_WARN = "LOG_WARN";
    public static final String LOG_ERROR = "LOG_ERROR";

    // Database commands
    public static final String CLEAR_DATABASE = "CLEAR_DATABASE";
    public static final String CLEAR_INVALID_TICKERS = "CLEAR_INVALID_TICKERS";
    public static final String DATABASE_RESET = "DATABASE_RESET";
    public static final String LOAD_INSTRUMENTS_FROM_FILE = "LOAD_INSTRUMENTS_FROM_FILE";

    // Player editor commands
    public static final String SHOW_PLAYER_EDITOR = "SHOW_PLAYER_EDITOR";
    public static final String SHOW_PLAYER_EDITOR_OK = "SHOW_PLAYER_EDITOR_OK";
    public static final String SHOW_PLAYER_EDITOR_CANCEL = "SHOW_PLAYER_EDITOR_CANCEL";

    // New commands for each dial
    public static final String NEW_VALUE_LEVEL = "newValueLevel";
    public static final String NEW_VALUE_NOTE = "newValueNote";
    public static final String NEW_VALUE_SWING = "newValueSwing";
    public static final String NEW_VALUE_PROBABILITY = "newValueProbability";
    public static final String NEW_VALUE_VELOCITY_MIN = "newValueVelocityMin";
    public static final String NEW_VALUE_VELOCITY_MAX = "newValueVelocityMax";
    public static final String NEW_VALUE_RANDOM = "newValueRandom";
    public static final String NEW_VALUE_PAN = "newValuePan";
    public static final String NEW_VALUE_SPARSE = "newValueSparse";
    public static final String PLAYER_DELETED = "PLAYER_DELETED";

    public static final String TRANSPOSE_UP = "TRANSPOSE_UP";
    public static final String TRANSPOSE_DOWN = "TRANSPOSE_DOWN";

    public static String RULE_DELETED = "RULE_DELETED";

    public static final String ROOT_NOTE_SELECTED = "ROOT_NOTE_SELECTED";

    public static final String SCALE_SELECTED = "SCALE_SELECTED";
    public static final String FIRST_SCALE_SELECTED = "FIRST_SCALE_SELECTED";
    public static final String LAST_SCALE_SELECTED = "LAST_SCALE_SELECTED";
    public static final String NEXT_SCALE_SELECTED = "NEXT__SCALE_SELECTED";
    public static final String PREV_SCALE_SELECTED = "PREV_SCALE_SELECTED";

    public static final String WINDOW_CLOSING = "WINDOW_CLOSING";
    public static final String WINDOW_RESIZED = "WINDOW_RESIZED";

    public static final String INSTRUMENT_UPDATED = "INSTRUMENT_UPDATED";
}
