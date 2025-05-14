package com.angrysurfer.core;

public class Constants {
    public static final Integer MIDI_DRUM_CHANNEL = 9;
    // Constants
    public static final int DRUM_PAD_COUNT = 16; // Number of drum pads
    // Button dimensions
    public static final int DRUM_PAD_SIZE = 28; // Standard drum pad button size


    public static boolean DEBUG = true;

    public static final double DEFAULT_BEAT_OFFSET = 1.0;
    public static int DEFAULT_PPQ = 48; // Update the default PPQ to 48 to work with both sequencers
    public static int DEFAULT_BAR_COUNT = 4;
    public static int DEFAULT_BEATS_PER_BAR = 4;
    public static int DEFAULT_BEAT_DIVIDER = 16;
    public static float DEFAULT_BPM = 88;
    public static int DEFAULT_PART_LENGTH = 1;
    public static int DEFAULT_MAX_TRACKS = 16;
    public static long DEFAULT_SONG_LENGTH = Long.MAX_VALUE;
    public static int DEFAULT_SWING = 25;

    public static final int DEFAULT_LOOP_COUNT = 0;
    public static final int DEFAULT_PART_COUNT = 1;

    public static final String INSTRUMENT = "/instrument";
    public static final String INSTRUMENT_LIST = "/instruments/all";
    public static final String INSTRUMENT_INFO = "/instrument/info";
    public static final String INSTRUMENT_LOOKUP = "/instruments/lookup";
    public static final String INSTRUMENT_NAMES = "/instruments/names";

    public static final String SAVE_CONFIG = "/instruments/save";

    public static final String DEVICES_INFO = "/devices/info";
    public static final String DEVICE_NAMES = "/devices/names";

    public static final String SERVICE_RESET = "/service/reset";
    public static final String SERVICE_SELECT = "/service/select";

    public static final String RULES_FOR_PLAYER = "/player/rules";

    public static final String ALL_PLAYERS = "/players/info";
    public static final String CLEAR_PLAYERS = "/players/clear";
    public static final String CLEAR_PLAYERS_WITH_NO_RULES = "/players/clearnorules";
    public static final String ADD_PLAYER = "/players/add";
    public static final String ADD_PLAYER_FOR_NOTE = "/players/note";
    public static final String REMOVE_PLAYER = "/players/remove";
    public static final String UPDATE_PLAYER = "/player/update";
    public static final String MUTE_PLAYER = "/players/mute";

    public static final String ADD_STEP = "/steps/add";
    public static final String REMOVE_STEP = "/steps/remove";
    public static final String UPDATE_STEP = "/steps/update";
    public static final String MUTE_STEP = "/steps/mute";

    public static final String SONG_INFO = "/song/info";
    public static final String ADD_SONG = "/songs/new";
    public static final String DELETE_SONG = "/songs/remove";
    public static final String PREV_SONG = "/songs/previous";
    public static final String NEXT_SONG = "/songs/next";
    public static final String UPDATE_SONG = "/song/update";
    public static final String LOAD_SONG = "/song/load";
    public static final String NEW_SONG = "/song/new";
    public static final String REMOVE_SONG = "/song/remove";

    public static final String ADD_SESSION = "/session/new";
    public static final String DELETE_SESSION = "/rules/remove";
    public static final String PREV_SESSION = "/session/previous";
    public static final String NEXT_SESSION = "/session/next";
    public static final String UPDATE_SESSION = "/session/update";

    public static final String START_SESSION = "/session/start";
    public static final String STOP_SESSION = "/session/stop";
    public static final String PAUSE_SESSION = "/session/pause";
    public static final String LOAD_SESSION = "/session/load";
    public static final String SESSION_STATUS = "/session/status";
    public static final String SESSION_INFO = "/session/info";
    public static final String SESSION_LOG = "/session/log";

    public static final String ADD_RULE = "/rules/add";
    public static final String REMOVE_RULE = "/rules/remove";
    public static final String UPDATE_RULE = "/rule/update";
    public static final String GET_INSTRUMENT_BY_CHANNEL = "/midi/instrument";
    public static final String GET_INSTRUMENT_BY_ID = "/instrument";

    public static final String SEND_MESSAGE = "/messages/send";
    public static final String SAVE_BEAT = "/session/save";
    public static final String SPECIFY_RULE = "/rules/specify";

    public static final String PLAY_DRUM_NOTE = "/drums/note";
    public static final String PLAY_SEQUENCE = "/sequence/play";

    public static final int DEFAULT_XOX_TRACKS = 16;
    public static final int DEFAULT_XOX_PATTERN_LENGTH = 16;
    public static final float DEFAULT_TEMPO = 130.0F;
    public static final float DEFAULT_TEMPO_FACTOR = 1.0F;
    public static final float DEFAULT_TEMPO_IN_MPQ = 0;

    public static final String APPLICATION_FRAME = "APPLICATION_FRAME";
    public static final String PLAYER = "PLAYER";
}