package com.angrysurfer.beatsui.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.angrysurfer.beatsui.RedisService;
import com.angrysurfer.beatsui.mock.Caption;
import com.angrysurfer.beatsui.mock.ControlCode;
import com.angrysurfer.beatsui.mock.Instrument;
import com.angrysurfer.core.config.DeviceConfig;
import com.angrysurfer.core.engine.MIDIEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BeatsUIConfig {
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private List<Instrument> instruments = new ArrayList<>();

    public static BeatsUIConfig loadDefaults(String filepath, RedisService redisService) throws IOException {
        BeatsUIConfig uiConfig = new BeatsUIConfig();
        
        // First, load the config file using DeviceConfig
        DeviceConfig deviceConfig = new DeviceConfig();
        DeviceConfig.loadDefaults(filepath, 
            () -> new ArrayList<>(), // findAllInstruments - empty list since we want fresh state
            (instrument) -> instrument, // saveInstrument - no-op
            (caption) -> caption, // saveCaption - no-op
            (controlCode) -> controlCode, // saveControlCode - no-op
            (pad) -> pad // savePad - no-op
        );

        // Now copy each instrument from deviceConfig to our system
        deviceConfig.getInstruments().forEach(sourceInstrument -> {
            Instrument newInstrument = new Instrument();
            // Copy basic properties
            newInstrument.setName(sourceInstrument.getName());
            newInstrument.setDeviceName(sourceInstrument.getDeviceName());
            newInstrument.setChannels(sourceInstrument.getChannels());
            newInstrument.setLowestNote(sourceInstrument.getLowestNote());
            newInstrument.setHighestNote(sourceInstrument.getHighestNote());
            newInstrument.setHighestPreset(sourceInstrument.getHighestPreset());
            newInstrument.setPreferredPreset(sourceInstrument.getPreferredPreset());
            newInstrument.setHasAssignments(sourceInstrument.getHasAssignments());
            newInstrument.setPlayerClassName(sourceInstrument.getPlayerClassName());
            newInstrument.setAvailable(sourceInstrument.getAvailable());

            // Copy and save control codes with their captions
            sourceInstrument.getControlCodes().forEach(sourceCC -> {
                ControlCode newCC = new ControlCode();
                newCC.setCode(sourceCC.getCode());
                newCC.setName(sourceCC.getName());
                newCC.setLowerBound(sourceCC.getLowerBound());
                newCC.setUpperBound(sourceCC.getUpperBound());
                newCC.setBinary(sourceCC.getBinary());

                // Handle captions
                sourceCC.getCaptions().forEach(sourceCap -> {
                    Caption newCap = new Caption();
                    newCap.setCode(sourceCap.getCode());
                    newCap.setDescription(sourceCap.getDescription());
                    // Save caption and add to control code
                    Caption savedCap = redisService.saveCaption(newCap);
                    newCC.getCaptions().add(savedCap);
                });

                // Save control code
                ControlCode savedCC = redisService.saveControlCode(newCC);
                newInstrument.getControlCodes().add(savedCC);

                // Update instrument mappings
                newInstrument.getAssignments().put(savedCC.getCode(), savedCC.getName());
                if (savedCC.getLowerBound() != null && savedCC.getUpperBound() != null) {
                    newInstrument.getBoundaries().put(savedCC.getCode(),
                            new Integer[] { savedCC.getLowerBound(), savedCC.getUpperBound() });
                }
            });

            // Copy pads
            sourceInstrument.getPads().forEach(sourcePad -> {
                // Create new mock.Pad (not core.Pad)
                com.angrysurfer.beatsui.mock.Pad newPad = new com.angrysurfer.beatsui.mock.Pad();
                if (sourcePad instanceof com.angrysurfer.core.model.Pad corePad) {
                    newPad.setNote(corePad.getNote());
                    newPad.setName(corePad.getName());
                    newPad.setControlCodes(new ArrayList<>(corePad.getControlCodes()));
                }
                
                // Save pad and add to instrument
                com.angrysurfer.beatsui.mock.Pad savedPad = redisService.savePad(newPad);
                newInstrument.getPads().add(savedPad);
            });

            // Save the complete instrument
            Instrument savedInstrument = redisService.saveInstrument(newInstrument);
            uiConfig.getInstruments().add(savedInstrument);
        });

        return uiConfig;
    }

    private static Instrument updateExistingInstrument(Instrument source, Instrument target) {
        target.setChannels(source.getChannels());
        target.setDeviceName(source.getDeviceName());
        target.setLowestNote(source.getLowestNote());
        target.setHighestNote(source.getHighestNote());
        target.setHighestPreset(source.getHighestPreset());
        target.setPreferredPreset(source.getPreferredPreset());
        return target;
    }

    private static void processControlCodes(Instrument dbInstrument,
            Instrument configInstrument, RedisService redisService) {
        dbInstrument.getControlCodes().clear();
        dbInstrument.getAssignments().clear();
        dbInstrument.getBoundaries().clear();

        configInstrument.getControlCodes().forEach(cc -> {
            // Reset ID for new assignment
            cc.setId(null);
            
            ControlCode controlCode = new ControlCode();
            controlCode.setCode(cc.getCode());
            controlCode.setName(cc.getName());
            controlCode.setLowerBound(cc.getLowerBound());
            controlCode.setUpperBound(cc.getUpperBound());
            controlCode.setBinary(cc.getBinary());

            final ControlCode controlCodeRef = controlCode;

            // Process captions if they exist
            if (!cc.getCaptions().isEmpty()) {
                cc.getCaptions().forEach(cap -> {
                    // Reset ID for new assignment
                    cap.setId(null);
                    Caption caption = new Caption();
                    caption.setCode(cap.getCode());
                    caption.setDescription(cap.getDescription());
                    caption = redisService.saveCaption(caption);
                    controlCodeRef.getCaptions().add(caption);
                });
            }

            controlCode = redisService.saveControlCode(controlCode);

            // Update instrument mappings
            dbInstrument.getAssignments().put(controlCode.getCode(), controlCode.getName());
            if (controlCode.getLowerBound() != null && controlCode.getUpperBound() != null) {
                dbInstrument.getBoundaries().put(controlCode.getCode(),
                        new Integer[] { controlCode.getLowerBound(), controlCode.getUpperBound() });
            }

            dbInstrument.getControlCodes().add(controlCode);
        });
    }

    public static void saveCurrentState(String filepath, RedisService redisService) throws IOException {
        BeatsUIConfig currentConfig = new BeatsUIConfig();
        currentConfig.setInstruments(redisService.findAllInstruments());

        // Write to file with pretty printing
        File outputFile = new File(filepath);
        mapper.writeValue(outputFile, currentConfig);

        // Also output to console for validation
        String jsonOutput = mapper.writeValueAsString(currentConfig);
        System.out.println("Current database state:");
        System.out.println(jsonOutput);
    }
}
