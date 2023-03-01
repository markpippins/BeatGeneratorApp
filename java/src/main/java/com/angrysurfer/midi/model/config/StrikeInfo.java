package com.angrysurfer.midi.model.config;

import com.angrysurfer.midi.model.Strike;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
@Setter
public class StrikeInfo extends PlayerInfo {
    static Logger logger = LoggerFactory.getLogger(StrikeInfo.class.getCanonicalName());
    public StrikeInfo() {
    }
    static StrikeInfo fromStrike(Strike strike) {
        StrikeInfo def = new StrikeInfo();
        copyValues(strike, def);
        return def;
    }

//    static DrumPad fromDrumPadDef(DrumPadDef def, IMidiInstrument instrument) {
//        DrumPad pad = new DrumPad();
//        pad.setAllowedControlMessages(pad.getAllowedControlMessages());
//        pad.setPreset(def.getPreset());
//        pad.setChannel(def.getChannel());
//        pad.setInstrument(instrument);
//        pad.setConditions(def.getRules());
//        pad.setMinVelocity(def.getMinVelocity());
//        pad.setMaxVelocity(def.getMaxVelocity());
//        pad.setNote(pad.getNote());
//
//        return pad;
//    }
}
