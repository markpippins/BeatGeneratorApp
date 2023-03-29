import { Player } from "./player";

export class Constants {

  static CONDITION_COLUMNS = ['operator', 'comparison', 'value']

  static PLAYER_COLUMNS = [
    // 'Id',
    'Device',
    'Preset',
    'Pitch',
    '',
    'Subs',
    'Swing',
    'Prob',
    'Level',
    'Min. V',
    'Max. V',
    'Ratchet',
    'Interval'
  ]

  static TICK_DIV = 0;
  static BEAT_DIV = 1;
  static BAR_DIV = 2;
  static PART_DIV = 3;

  static INSTRUMENT = 0;
  static NOTE = 1;
  static PROBABILITY = 2;
  static MIN_VELOCITY = 3;
  static MAX_VELOCITY = 4;
  static MUTE = 5;
  static PART = 6;
  static LEVEL = 7;
  static SWING = 8;
  static PRESET = 9;
  static RATCHET_COUNT = 10;
  static RATCHET_INTERVAL = 11;
  static CHANNEL = 12;
  static SUBS = 13;

  static STEP_ACTIVE = 0;
  static STEP_GATE = 1;
  static STEP_PITCH = 2;
  static STEP_VELOCITY = 3;
  static STEP_PROBABILITY = 4;

  static PPQ = 0
  static BPM = 1
  static BEATS_PER_BAR = 2
  static PART_LENGTH = 3
  static MAX_TRACKS = 4
  static BARS = 5
  static PARTS = 6

  static ERROR = 0
  static CONNECTED = 1
  static DISCONNECTED = 2
  static STATUS = 3
  static TICKER_SELECTED = 10
  static STEP_UPDATED = 11

  static PATTERN_UPDATE_CHANNEL = 0
  static PATTERN_UPDATE_ACTIVE = 1
  static PATTERN_UPDATE_BASE_NOTE = 2
  static PATTERN_UPDATE_LAST_STEP = 3
  static PATTERN_UPDATE_DIRECTION = 4
  static PATTERN_UPDATE_PROBABILITY = 5
  static PATTERN_UPDATE_RANDOM = 6
  static PATTERN_UPDATE_TRANSPOSE = 7
  static PATTERN_UPDATE_LENGTH = 8
  static PATTERN_UPDATE_GATE = 9
  static PATTERN_UPDATE_REPEATS = 10
  static PATTERN_UPDATE_SWING = 11
  static PATTERN_UPDATE_DEVICE = 12
  static PATTERN_UPDATE_PRESET = 13
}
