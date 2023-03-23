import {Player} from "./player";

export interface Ticker {
  id: number;
  tick: number;
  done: boolean;
  bar: number;
  part: number
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
  activePlayerIds: number[]
}
