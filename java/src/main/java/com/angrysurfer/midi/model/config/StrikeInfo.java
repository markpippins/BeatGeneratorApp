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
    static StrikeInfo fromDrumPad(Strike pad) {
        StrikeInfo def = new StrikeInfo();
        def.setAllowedControlMessages(pad.getAllowedControlMessages());
        def.setPreset(pad.getPreset());
        def.setChannel(pad.getChannel());
        def.setInstrument(pad.getInstrumentName());
        def.setConditions(pad.getConditions());
        def.setMinVelocity(pad.getMinVelocity());
        def.setMaxVelocity(pad.getMaxVelocity());
        def.setNote(pad.getNote());

        return def;
    }

//    static DrumPad fromDrumPadDef(DrumPadDef def, IMidiInstrument instrument) {
//        DrumPad pad = new DrumPad();
//        pad.setAllowedControlMessages(pad.getAllowedControlMessages());
//        pad.setPreset(def.getPreset());
//        pad.setChannel(def.getChannel());
//        pad.setInstrument(instrument);
//        pad.setConditions(def.getConditions());
//        pad.setMinVelocity(def.getMinVelocity());
//        pad.setMaxVelocity(def.getMaxVelocity());
//        pad.setNote(pad.getNote());
//
//        return pad;
//    }
}
