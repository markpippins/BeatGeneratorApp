import { Player } from "./player";

export class Constants {

  static CONDITION_COLUMNS = ['operator', 'comparison', 'value']

  static PLAYER_COLUMNS = [
    'Device',
    'Preset',
    'Part',
    'Pitch',
    'Probability',
    'Min. Vel.',
    'Max. Vel.'
  ]

  static INSTRUMENT = 0;
  static NOTE = 1;
  static PROBABILITY = 2;
  static MIN_VELOCITY = 3;
  static MAX_VELOCITY = 4;
  static MUTE = 5;

  static PPQ = 0
  static BPM = 1
  static BEATS_PER_BAR = 2
  static PART_LENGTH = 3
  static MAX_TRACKS = 4

  static ERROR = 0
  static CONNECTED = 1
  static DISCONNECTED = 2
  static STATUS = 3
  static TICKER_SELECTED = 10
  static STEP_UPDATED = 11
}
