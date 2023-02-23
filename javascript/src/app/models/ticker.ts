export interface Ticker {
  tick: number;
  done: boolean;
  bar: number;
  barLengthInTicks: number;
  beat: number;
  beatsPerBar: number;
  beatDivider: number;
  delay: number;
  partLength: number;
  maxTracks: number;
  songLength: number;
  swing: number;
  playing: boolean;
  stopped: boolean;
  // muteGroups:
}
