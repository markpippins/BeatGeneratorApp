package com.angrysurfer.beatsui.config;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.angrysurfer.beatsui.data.RedisService;
import com.angrysurfer.core.config.DeviceConfig;
import com.angrysurfer.core.proxy.ProxyCaption;
import com.angrysurfer.core.proxy.ProxyControlCode;
import com.angrysurfer.core.proxy.ProxyInstrument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserConfig {
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

    private List<ProxyInstrument> instruments = new ArrayList<>();

    public static UserConfig loadDefaults(String filepath, RedisService redisService) throws IOException {
        UserConfig uiConfig = new UserConfig();
        
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
            ProxyInstrument newInstrument = new ProxyInstrument();
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
                ProxyControlCode newCC = new ProxyControlCode();
                newCC.setCode(sourceCC.getCode());
                newCC.setName(sourceCC.getName());
                newCC.setLowerBound(sourceCC.getLowerBound());
                newCC.setUpperBound(sourceCC.getUpperBound());
                newCC.setBinary(sourceCC.getBinary());

                // Handle captions
                sourceCC.getCaptions().forEach(sourceCap -> {
                    ProxyCaption newCap = new ProxyCaption();
                    newCap.setCode(sourceCap.getCode());
                    newCap.setDescription(sourceCap.getDescription());
                    // Save caption and add to control code
                    ProxyCaption savedCap = redisService.saveCaption(newCap);
                    newCC.getCaptions().add(savedCap);
                });

                // Save control code
                ProxyControlCode savedCC = redisService.saveControlCode(newCC);
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
                com.angrysurfer.core.proxy.ProxyPad newPad = new com.angrysurfer.core.proxy.ProxyPad();
                if (sourcePad instanceof com.angrysurfer.core.model.Pad corePad) {
                    newPad.setNote(corePad.getNote());
                    newPad.setName(corePad.getName());
                    newPad.setControlCodes(new ArrayList<>(corePad.getControlCodes()));
                }
                
                // Save pad and add to instrument
                com.angrysurfer.core.proxy.ProxyPad savedPad = redisService.savePad(newPad);
                newInstrument.getPads().add(savedPad);
            });

            // Save the complete instrument
            redisService.saveInstrument(newInstrument);
            uiConfig.getInstruments().add(newInstrument);
        });

        return uiConfig;
    }

    private static ProxyInstrument updateExistingInstrument(ProxyInstrument source, ProxyInstrument target) {
        target.setChannels(source.getChannels());
        target.setDeviceName(source.getDeviceName());
        target.setLowestNote(source.getLowestNote());
        target.setHighestNote(source.getHighestNote());
        target.setHighestPreset(source.getHighestPreset());
        target.setPreferredPreset(source.getPreferredPreset());
        return target;
    }

    private static void processControlCodes(ProxyInstrument dbInstrument,
            ProxyInstrument configInstrument, RedisService redisService) {
        dbInstrument.getControlCodes().clear();
        dbInstrument.getAssignments().clear();
        dbInstrument.getBoundaries().clear();

        configInstrument.getControlCodes().forEach(cc -> {
            // Reset ID for new assignment
            cc.setId(null);
            
            ProxyControlCode controlCode = new ProxyControlCode();
            controlCode.setCode(cc.getCode());
            controlCode.setName(cc.getName());
            controlCode.setLowerBound(cc.getLowerBound());
            controlCode.setUpperBound(cc.getUpperBound());
            controlCode.setBinary(cc.getBinary());

            final ProxyControlCode controlCodeRef = controlCode;

            // Process captions if they exist
            if (!cc.getCaptions().isEmpty()) {
                cc.getCaptions().forEach(cap -> {
                    // Reset ID for new assignment
                    cap.setId(null);
                    ProxyCaption caption = new ProxyCaption();
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
        UserConfig currentConfig = new UserConfig();
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
