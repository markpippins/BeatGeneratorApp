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
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.Getter;
import lombok.Setter;

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

    public static void loadDefaults(String filepath,
            MidiInstrumentRepo midiInstrumentRepo,
            ControlCodeRepo controlCodeRepo,
            CaptionRepo captionRepo,
            PadRepo padRepo) throws IOException {

        // Get existing instruments from DB as a map
        Map<String, Instrument> existingInstruments = midiInstrumentRepo.findAll().stream()
                .collect(Collectors.toMap(Instrument::getName, Function.identity()));

        // Get available MIDI devices and create if not in DB
        List<String> availableDevices = MIDIService.getMidiOutDevices().stream()
                .filter(d -> d.getMaxTransmitters() != -1)
                .map(d -> {
                    String deviceName = d.getDeviceInfo().getName();
                    if (!existingInstruments.containsKey(deviceName)) {
                        Instrument instrument = new Instrument(deviceName, d);
                        instrument.setAvailable(true);
                        instrument = midiInstrumentRepo.save(instrument);
                        existingInstruments.put(deviceName, instrument);
                    }
                    return deviceName;
                })
                .collect(Collectors.toList());

        // Load and process config file
        File configFile = new File(filepath);
        if (configFile.exists()) {
            SystemConfig config = mapper.readValue(configFile, SystemConfig.class);

            // Process each instrument from config
            config.getInstruments().forEach(configInstrument -> {
                configInstrument.setAvailable(availableDevices.contains(configInstrument.getName()));

                // Update existing or create new
                Instrument dbInstrument = existingInstruments.containsKey(configInstrument.getName())
                        ? updateExistingInstrument(configInstrument,
                                existingInstruments.get(configInstrument.getName()))
                        : configInstrument;

                // Process control codes
                if (!dbInstrument.getControlCodes().isEmpty())
                    processControlCodesCaptionsAssignmentsAndBoundaries(dbInstrument, controlCodeRepo, captionRepo,
                            config);

                try {
                    // Save instrument
                    dbInstrument = midiInstrumentRepo.save(dbInstrument);
                } catch (Exception e) {
                    System.out.println("Failed to save instrument: " + dbInstrument.getName());
                }

                // Process pads if needed
                addPadInfo(midiInstrumentRepo, padRepo, dbInstrument);
            });
        }

        // Save current state back to file
        saveCurrentStateToFile(filepath, midiInstrumentRepo);
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

    // private static void processControlCodes(Instrument instrument,
    // ControlCodeRepo controlCodeRepo,
    // CaptionRepo captionRepo) {
    // instrument.getAssignments().keySet().forEach(code -> {
    // try {
    // // Check if control code already exists
    // ControlCode controlCode = controlCodeRepo.findById(code)
    // .orElseGet(ControlCode::new);

    // // Update or set new values
    // controlCode.setCode(code);
    // controlCode.setName(instrument.getAssignments().get(code));

    // if (instrument.getBoundaries().containsKey(code)) {
    // controlCode.setLowerBound(instrument.getBoundaries().get(code)[0]);
    // controlCode.setUpperBound(instrument.getBoundaries().get(code)[1]);

    // if (instrument.getCaptions().containsKey(code)) {
    // try {
    // controlCode.setCaptions(instrument.getCaptions().get(code)
    // .entrySet().stream()
    // .map(es -> {
    // Caption caption = new Caption();
    // caption.setCode(es.getKey());
    // caption.setDescription(es.getValue().strip());
    // return captionRepo.save(caption);
    // })
    // .collect(Collectors.toSet()));
    // } catch (Exception e) {
    // log.warn("Failed to process captions for control code {}: {}", code,
    // e.getMessage());
    // }
    // }
    // }

    // // Save control code and add to instrument
    // controlCode = controlCodeRepo.save(controlCode);
    // instrument.getControlCodes().add(controlCode);

    // } catch (Exception e) {
    // log.error("Failed to process control code {}: {}", code, e.getMessage());
    // }
    // });
    // }

    private static void processControlCodesCaptionsAssignmentsAndBoundaries(Instrument instrument,
            ControlCodeRepo controlCodeRepo,
            CaptionRepo captionRepo, SystemConfig config) {

        List<ControlCode> controlCodes = new ArrayList<>();
        controlCodes.addAll(instrument.getControlCodes());

        instrument.getControlCodes().clear(); // Clear existing control codes for
        instrument.getAssignments().clear();
        instrument.getBoundaries().clear();
        instrument.getCaptions().clear();
        
        // instrument.getAssignments().keySet().forEach(code -> {
        controlCodes.forEach(cc -> {
            Integer code = cc.getCode();
            ControlCode controlCode = new ControlCode();
            controlCode.setCode(code);
            controlCode.setName(cc.getName());
            controlCode.setLowerBound(cc.getLowerBound());
            controlCode.setUpperBound(cc.getUpperBound());
            controlCode.setCaptions(cc.getCaptions());
            controlCode.setPad(cc.getPad());
            controlCode.setBinary(cc.getBinary());

            instrument.getAssignments().put(cc.getCode(), cc.getName());
            instrument.getBoundaries().put(cc.getCode(), new Integer[]{cc.getLowerBound(), cc.getUpperBound()});
            instrument.getCaptions().put(cc.getCode(), cc.getCaptions().stream().collect(Collectors.toMap(Caption::getCode, Caption::getDescription))); 

            // if (instrument.getAssignments().containsKey(code)) {
            //     controlCode.setName(instrument.getAssignments().get(code));
            // }
            // controlCode.setName(instrument.getAssignments().get(code));

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
            MidiInstrumentRepo midiInstrumentRepo) throws IOException {
        SystemConfig currentConfig = new SystemConfig();
        currentConfig.setInstruments(midiInstrumentRepo.findAll());

        // Configure mapper for pretty printing
        ObjectMapper prettyMapper = new ObjectMapper();
        prettyMapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Get actual file path for writing
        File outputFile = new File(filepath);
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

    public ControlCode copy(ControlCode controlCode) {
        ControlCode copy = new ControlCode();
        copy.setCode(controlCode.getCode());
        copy.setName(controlCode.getName());
        copy.setLowerBound(controlCode.getLowerBound());
        copy.setUpperBound(controlCode.getUpperBound());
        copy.setCaptions(controlCode.getCaptions());
        return copy;
    }

    public Caption copy(Caption caption) {
        Caption copy = new Caption();
        copy.setCode(caption.getCode());
        copy.setDescription(caption.getDescription());
        return copy;
    }

    // public Boundary copy(Boundary boundary) {
    // Boundary copy = new Boundary();
    // copy.setLowerBound(boundary.getLowerBound());
    // copy.setUpperBound(boundary.getUpperBound());
    // return copy;
    // }
}
