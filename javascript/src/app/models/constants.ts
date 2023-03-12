import { Player } from "./player";

export class Constants {

  static CONDITION_COLUMNS = ['operator', 'comparison', 'value']

  static PLAYER_COLUMNS = [
    'Device',
    'Preset',
    'Pitch',
    'Probability',
    'Min. Vel.',
    'Max. Vel.'
  ]

  static DUMMY_PLAYER: Player = {
    id: 0,
    maxVelocity: 0,
    minVelocity: 0,
    note: 0,
    preset: 0,
    probability: 0,
    rules: [],
    allowedControlMessages: [],
    instrument: {
      "id": 0,
      "name": "",
      "channel": 0,
      "lowestNote": 0,
      "highestNote": 0,
      "highestPreset": 0,
      "preferredPreset": 0,
      "assignments": new Map() ,
      "boundaries": new Map() ,
      "hasAssignments": false,
      "pads": 0,
      "controlCodes": []
    }
  }

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

  static TICKER_SELECTED = 0
  static STEP_UPDATED = 10
}
