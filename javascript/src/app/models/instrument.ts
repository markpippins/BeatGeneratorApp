import {ControlCode} from "./control-code";

export interface Instrument {
  name: string;
  channel: number;
  lowestNote: number;
  highestNote: number;
  highestPreset: number;
  preferredPreset: number;
  assignments: Map<number, string>;
  boundaries: Map<number, number[]>;
  hasAssignments: boolean;
  pads: number
  controlCodes: ControlCode[]
}
