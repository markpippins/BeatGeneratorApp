package com.angrysurfer.sequencer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.angrysurfer.sequencer.config.SystemConfig;
import com.angrysurfer.sequencer.model.Pad;
import com.angrysurfer.sequencer.model.midi.MidiInstrument;
import com.angrysurfer.sequencer.model.player.Strike;
import com.angrysurfer.sequencer.repo.CaptionRepo;
import com.angrysurfer.sequencer.repo.ControlCodeRepo;
import com.angrysurfer.sequencer.repo.MidiInstrumentRepo;
import com.angrysurfer.sequencer.repo.PadRepo;
import com.angrysurfer.sequencer.service.MIDIService;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication
public class Application {

    private MidiInstrumentRepo midiInstrumentRepo;
    private PadRepo padRepo;

    static ObjectMapper mapper = new ObjectMapper();

    static String filepath = "C:/Users/MarkP/dev/BeatGeneratorApp/java/resources/config/midi.json";
    // this.getClass().getResource("resources/config/midi.json").getPath();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    CommandLineRunner beatGeneratorSetup(MidiInstrumentRepo midiInstrumentRepo,
            PadRepo padRepo,
            ControlCodeRepo controlCodeRepo,
            CaptionRepo captionRepo) {

        this.midiInstrumentRepo = midiInstrumentRepo;
        this.padRepo = padRepo;

        return args -> {
            if (!midiInstrumentRepo.findAll().isEmpty())
                return;

            List<String> instrumentNames = new ArrayList<>();
            MIDIService.findMidiDevices(true, false).forEach(device -> {
                if (device.getDeviceInfo().getName().contains("MIDI"))
                    return;

                MidiInstrument instrument = new MidiInstrument(device.getDeviceInfo().getName(), device, 5);
                instrument.setAvailable(true);
                midiInstrumentRepo.save(instrument);
                instrumentNames.add(instrument.getName());
            });

            File ini = new File(filepath);
            if (ini.exists() || !ini.isDirectory())
                SystemConfig.newInstance(filepath,
                        instrumentNames.stream().collect(Collectors.joining(",")),
                        midiInstrumentRepo, controlCodeRepo, captionRepo).getInstruments()
                        .forEach(instrument -> {
                            addPadInfo(instrument);
                        });

            // add ticker, player, rules
        };
    }

    private void addPadInfo(MidiInstrument instrumentInfo) {
        int padCount = instrumentInfo.getHighestNote() - instrumentInfo.getLowestNote();
        if (padCount == 8) {
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
