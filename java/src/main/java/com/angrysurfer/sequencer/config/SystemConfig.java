package com.angrysurfer.sequencer.config;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.angrysurfer.sequencer.model.midi.ControlCode;
import com.angrysurfer.sequencer.model.midi.MidiInstrument;
import com.angrysurfer.sequencer.model.ui.Caption;
import com.angrysurfer.sequencer.repo.CaptionRepo;
import com.angrysurfer.sequencer.repo.ControlCodeRepo;
import com.angrysurfer.sequencer.repo.MidiInstrumentRepo;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfig implements Serializable {
    static ObjectMapper mapper = new ObjectMapper();
    List<MidiInstrument> instruments = new ArrayList<>();

    public SystemConfig() {
    }

    public SystemConfig(List<MidiInstrument> instruments) {
        this.instruments = instruments;
    }

    public static SystemConfig newInstance(String filepath, String instrumentNames,
            MidiInstrumentRepo midiInstrumentRepo,
            ControlCodeRepo controlCodeRepo, CaptionRepo captionRepo) throws IOException {

        File ini = new File(filepath);

        SystemConfig config = mapper.readValue(ini, SystemConfig.class);
        config.getInstruments().forEach(instrument -> {
            instrument.setAvailable(instrumentNames.contains(instrument.getName()));
            instrument = midiInstrumentRepo.save(instrument);
            MidiInstrument finalInstrumentDef = instrument;
            instrument.getAssignments().keySet().forEach(code -> {
                ControlCode controlCode = new ControlCode();
                controlCode.setCode(code);
                controlCode.setName(finalInstrumentDef.getAssignments().get(code));
                if (finalInstrumentDef.getBoundaries().containsKey(code)) {
                    controlCode.setLowerBound(finalInstrumentDef.getBoundaries().get(code)[0]);
                    controlCode.setUpperBound(finalInstrumentDef.getBoundaries().get(code)[1]);

                    if (finalInstrumentDef.getCaptions().containsKey(code))
                        controlCode.setCaptions(finalInstrumentDef.getCaptions().get(code)
                                .entrySet().stream().map(es -> {
                                    Caption caption = new Caption();
                                    caption.setCode(es.getKey());
                                    caption.setDescription(es.getValue().strip());
                                    caption = captionRepo.save(caption);
                                    return caption;
                                }).collect(Collectors.toSet()));
                }
                controlCode = controlCodeRepo.save(controlCode);
                finalInstrumentDef.getControlCodes().add(controlCode);
            });
            instrument = midiInstrumentRepo.save(finalInstrumentDef);
            // addPadInfo(instrument);
        });

        return config;
    }
}
