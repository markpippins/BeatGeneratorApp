package com.angrysurfer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.angrysurfer.midi.model.ControlCode;
import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.model.Pad;
import com.angrysurfer.midi.model.Strike;
import com.angrysurfer.midi.repo.ControlCodeRepo;
import com.angrysurfer.midi.repo.MidiInstrumentRepo;
import com.angrysurfer.midi.repo.PadRepo;
import com.angrysurfer.midi.util.MidiInstrumentList;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class BeatGeneratorApplication {

    MidiInstrumentRepo midiInstrumentRepo;
    PadRepo padRepo;
    ControlCodeRepo controlCodeRepo;

     static ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        SpringApplication.run(BeatGeneratorApplication.class, args);
    }
    
	@Bean
	CommandLineRunner beatGeneratorSetup(MidiInstrumentRepo midiInstrumentRepo,
    PadRepo padRepo, 
    ControlCodeRepo controlCodeRepo) {

        this.midiInstrumentRepo = midiInstrumentRepo;
        this.padRepo = padRepo;
        this.controlCodeRepo = controlCodeRepo;

		return args -> {
            // load data
            if (midiInstrumentRepo.findAll().isEmpty())
                try {
                    String filepath = "C:/Users/MarkP/IdeaProjects/BeatGeneratorApp/java/resources/config/midi.json";
                    // String filepath = this.getClass().getResource("resources/config/midi.json").getPath();
                    MidiInstrumentList config = mapper.readValue(new File(filepath), MidiInstrumentList.class);

                    config.getInstruments().forEach(instrument -> {
                        instrument = midiInstrumentRepo.save(instrument);
                        MidiInstrument finalInstrumentDef = instrument;
                        instrument.getAssignments().keySet().forEach(code -> {
                            ControlCode controlCode = new ControlCode();
                            controlCode.setCode(code);
                            controlCode.setName(finalInstrumentDef.getAssignments().get(code));
                            if (finalInstrumentDef.getBoundaries().containsKey(code)) {
                                controlCode.setLowerBound(finalInstrumentDef.getBoundaries().get(code)[0]);
                                controlCode.setUpperBound(finalInstrumentDef.getBoundaries().get(code)[1]);
                                controlCode.setOptionLabels(finalInstrumentDef.getOptionLabels());
                            }
                            controlCode = controlCodeRepo.save(controlCode);
                            finalInstrumentDef.getControlCodes().add(controlCode);
                        });
                        instrument = midiInstrumentRepo.save(finalInstrumentDef);
                        addPadInfo(instrument);
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            // add ticker, player, rules
		};
	}

    private void addPadInfo(MidiInstrument instrumentInfo) {
        int padCount = instrumentInfo.getHighestNote() - instrumentInfo.getLowestNote();
        if (padCount == 7) {
            List<Pad> pads = new ArrayList<>(IntStream.range(0, 8).mapToObj(i -> new Pad()).toList());
            instrumentInfo.getControlCodes().forEach(cc -> {
                if (Strike.kickParams.contains(cc.getCode()))
                    pads.get(0).getControlCodes().add(cc.getCode());
                    
                if (Strike.snarePrams.contains(cc.getCode()))
                    pads.get(1).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 23 && cc.getCode() < 29)
                    pads.get(2).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 28 && cc.getCode() < 32)
                    pads.get(3).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 31 && cc.getCode() < 40)
                    pads.get(4).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 39 && cc.getCode() < 45)
                    pads.get(3).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 44 && cc.getCode() < 56)
                    pads.get(5).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 55 && cc.getCode() < 64)
                    pads.get(6).getControlCodes().add(cc.getCode());

                if (cc.getCode() > 63 && cc.getCode() < 72)
                    pads.get(7).getControlCodes().add(cc.getCode());
            });

            pads.get(0).setName("Kick");
            pads.get(1).setName("Snare");
            pads.get(2).setName("Hi-Hat Closed");
            pads.get(3).setName("Hi-Hat Open");
            pads.get(4).setName("Ride");
            pads.get(5).setName("Low Tom");
            pads.get(6).setName("Mid Tom");
            pads.get(7).setName("Hi Tom");

            pads.forEach(pad -> instrumentInfo.getPads().add(padRepo.save(pad)));
            midiInstrumentRepo.save(instrumentInfo);
        }
    }


}
