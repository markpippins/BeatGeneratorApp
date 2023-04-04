import { Player } from './player'

export class Constants {
  static CONDITION_COLUMNS = ['operator', 'comparison', 'value']

  static PLAYER_COLUMNS = [
    // 'Id',
    // 'Class',
    'Device',
    'Preset',
    'Pitch',
    '',
    '',
    'Div 1',
    'Div 2',
    'Skip',
    'Swing',
    'Prob',
    'Random',
    'Level',
    'Min. V',
    'Max. V',
    'Ratchet',
    'Interval',
    'Fade In',
    '/ Out',
  ]

  static TICK_DIV = 0
  static BEAT_DIV = 1
  static BAR_DIV = 2
  static PART_DIV = 3

  static STEP_ACTIVE = 0
  static STEP_GATE = 1
  static STEP_PITCH = 2
  static STEP_VELOCITY = 3
  static STEP_PROBABILITY = 4

  static ERROR = 0
  static CONNECTED = 1
  static DISCONNECTED = 2
  static STATUS = 3
  static TICKER_SELECTED = 10
  static STEP_UPDATED = 11
  static PLAYER_UPDATED = 12
  static COMMAND = 13
  static TICKER_UPDATED = 14
  static PLAYER_SELECTED = 15

  static CHROMATIC_SCALE = [
    'C',
    'C#',
    'D',
    'D#',
    'E',
    'F',
    'F#',
    'G',
    'G#',
    'A',
    'A#',
    'B',
  ]
}
