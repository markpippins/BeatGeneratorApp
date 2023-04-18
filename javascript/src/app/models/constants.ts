export class Constants {
  static RULE_COLUMNS = ['operator', 'comparison', 'value'];

  static PLAYER_COLUMNS = [
    // 'Id',
    // 'Class',
    'Device',
    'Preset',
    'Pitch',
    '',
    '',
    'Frac',
    'Divs',
    'Skip',
    'Swing',
    'Prob',
    'Random',
    'Level',
    'VMIN',
    'VMAX',
    'Ratchet',
    'Interval',
    'IFade',
    'OFade',
  ];

  static TICK_DIV = 0;
  static BEAT_DIV = 1;
  static BAR_DIV = 2;
  static PART_DIV = 3;

  static STEP_ACTIVE = 0;
  static STEP_GATE = 1;
  static STEP_PITCH = 2;
  static STEP_VELOCITY = 3;
  static STEP_PROBABILITY = 4;

  static ERROR = 0;
  static CONNECTED = 1;
  static DISCONNECTED = 2;
  static STATUS = 3;
  static TICKER_SELECTED = 10;
  static STEP_UPDATED = 11;
  static PLAYER_UPDATED = 12;
  static COMMAND = 13;
  static TICKER_UPDATED = 14;
  static PLAYER_SELECTED = 15;
  static INSTRUMENT_SELECTED = 16;
  static TICKER_STARTED = 17;

  static CLICK = 0;
  static NUDGE_RIGHT = 1;
  static NUDGE_LEFT = 2;
  static EXTEND = 3;
  static SHORTEN = 4;
  static CLEAR = 5;

  static C_MAJOR_SCALE: boolean[] = [
    true,
    false,
    true,
    false,
    true,
    true,
    false,
    true,
    false,
    true,
    false,
    true,
  ];

  static C_MINOR_SCALE: boolean[] = [
    true,
    false,
    true,
    true,
    false,
    true,
    false,
    true,
    true,
    true,
    true,
    false,
  ];

  static SCALE_NOTES: string[] = [
    'C',
    'C♯/D♭',
    'D',
    'D♯/E♭',
    'E',
    'F',
    'F♯/G♭',
    'G',
    'G♯/A♭',
    'A',
    'A♯/B♭',
    'B',
  ];
}
