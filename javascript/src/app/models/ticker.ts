export interface Ticker {
  tick: number;
  done: boolean;
  bar: number;
  ticksPerBeat: number;
  beat: number;
  beatsPerBar: number;
  beatDivider: number;
  tempoInBPM: number;
  partLength: number;
  maxTracks: number;
  songLength: number;
  swing: number;
  playing: boolean;
  stopped: boolean;
  // muteGroups:
}
