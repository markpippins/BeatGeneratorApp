package com.angrysurfer.sequencer.config;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.angrysurfer.sequencer.model.Pad;
import com.angrysurfer.sequencer.model.midi.ControlCode;
import com.angrysurfer.sequencer.model.midi.Instrument;
import com.angrysurfer.sequencer.model.player.Strike;
import com.angrysurfer.sequencer.model.ui.Caption;
import com.angrysurfer.sequencer.repo.CaptionRepo;
import com.angrysurfer.sequencer.repo.ControlCodeRepo;
import com.angrysurfer.sequencer.repo.MidiInstrumentRepo;
import com.angrysurfer.sequencer.repo.PadRepo;
import com.angrysurfer.sequencer.service.MIDIService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;

import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@Getter
@Setter
public class SystemConfig implements Serializable {
    static ObjectMapper mapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    List<Instrument> instruments = new ArrayList<>();

    public SystemConfig() {
    }

    public SystemConfig(List<Instrument> instruments) {
        this.instruments = instruments;
    }

    public static SystemConfig newInstance(File ini, String instrumentNames,
            MidiInstrumentRepo midiInstrumentRepo,
            ControlCodeRepo controlCodeRepo, CaptionRepo captionRepo) throws IOException {

        SystemConfig config = mapper.readValue(ini, SystemConfig.class);
        config.getInstruments().forEach(instrument -> {
            instrument.setAvailable(instrumentNames.contains(instrument.getName()));
            instrument = midiInstrumentRepo.save(instrument);
            Instrument finalInstrumentDef = instrument;
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

    public static void startupRoutine(String filepath,
            MidiInstrumentRepo midiInstrumentRepo,
            ControlCodeRepo controlCodeRepo, 
            CaptionRepo captionRepo, 
            PadRepo padRepo) throws IOException {

        if (!midiInstrumentRepo.findAll().isEmpty())
            return;

        List<String> instrumentNames = new ArrayList<>();
        MIDIService.getMidiOutDevices().forEach(device -> {
            if (device.getMaxTransmitters() == -1)
                return;

            Instrument instrument = new Instrument(device.getDeviceInfo().getName(), device);
            instrument.setAvailable(true);
            midiInstrumentRepo.save(instrument);
            instrumentNames.add(instrument.getName());
        });

        File ini = new File(filepath);
        if (ini.exists() || !ini.isDirectory())
            SystemConfig.newInstance(ini,
                    instrumentNames.stream().collect(Collectors.joining(",")),
                    midiInstrumentRepo, controlCodeRepo, captionRepo).getInstruments()
                    .forEach(instrument -> {
                        addPadInfo(midiInstrumentRepo, padRepo, instrument);
                    });
    }

    private static Instrument updateExistingInstrument(Instrument source, Instrument target) {
        // Update fields but preserve ID and relationships
        target.setChannels(source.getChannels());
        target.setDeviceName(source.getDeviceName());
        target.setLowestNote(source.getLowestNote());
        target.setHighestNote(source.getHighestNote());
        target.setHighestPreset(source.getHighestPreset());
        target.setPreferredPreset(source.getPreferredPreset());
        target.setAssignments(source.getAssignments());
        target.setBoundaries(source.getBoundaries());
        target.setCaptions(source.getCaptions());
        return target;
    }

    private static void processControlCodes(Instrument instrument, 
            ControlCodeRepo controlCodeRepo, 
            CaptionRepo captionRepo) {
        instrument.getAssignments().keySet().forEach(code -> {
            ControlCode controlCode = new ControlCode();
            controlCode.setCode(code);
            controlCode.setName(instrument.getAssignments().get(code));
            
            if (instrument.getBoundaries().containsKey(code)) {
                controlCode.setLowerBound(instrument.getBoundaries().get(code)[0]);
                controlCode.setUpperBound(instrument.getBoundaries().get(code)[1]);

                if (instrument.getCaptions().containsKey(code)) {
                    controlCode.setCaptions(instrument.getCaptions().get(code)
                        .entrySet().stream()
                        .map(es -> {
                            Caption caption = new Caption();
                            caption.setCode(es.getKey());
                            caption.setDescription(es.getValue().strip());
                            return captionRepo.save(caption);
                        })
                        .collect(Collectors.toSet()));
                }
            }
            controlCode = controlCodeRepo.save(controlCode);
            instrument.getControlCodes().add(controlCode);
        });
    }

    public static void saveCurrentStateToFile(String filepath, 
            MidiInstrumentRepo midiInstrumentRepo,
            ResourceLoader resourceLoader) throws IOException {
        SystemConfig currentConfig = new SystemConfig();
        currentConfig.setInstruments(midiInstrumentRepo.findAll());
        
        // Configure mapper for pretty printing
        ObjectMapper prettyMapper = new ObjectMapper();
        prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        // Get actual file path for writing
        Resource resource = resourceLoader.getResource(filepath);
        File outputFile = resource.getFile();
        prettyMapper.writeValue(outputFile, currentConfig);
    }

    static void addPadInfo(MidiInstrumentRepo midiInstrumentRepo, PadRepo padRepo, Instrument instrumentInfo) {
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
