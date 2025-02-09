// package com.angrysurfer.core.config;

// import java.io.File;
// import java.io.IOException;
// import java.io.Serializable;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;
// import java.util.Objects;
// import java.util.function.Function;
// import java.util.stream.Collectors;
// import java.util.stream.IntStream;

// import com.angrysurfer.core.model.Pad;
// import com.angrysurfer.core.model.midi.ControlCode;
// import com.angrysurfer.core.model.midi.Instrument;
// import com.angrysurfer.core.model.player.Strike;
// import com.angrysurfer.core.model.ui.Caption;
// import com.angrysurfer.core.repo.Captions;
// import com.angrysurfer.core.repo.ControlCodes;
// import com.angrysurfer.core.repo.Instruments;
// import com.angrysurfer.core.repo.Pads;
// import com.fasterxml.jackson.databind.DeserializationFeature;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.fasterxml.jackson.databind.SerializationFeature;

// import lombok.Getter;
// import lombok.Setter;

// @Getter
// @Setter
// public class DeviceConfig implements Serializable {
//     static ObjectMapper mapper = new ObjectMapper()
//             .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//             .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false) // Add this line
//             .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false); // Add this line
//     List<Instrument> instruments = new ArrayList<>();

//     public DeviceConfig() {
//     }

//     public DeviceConfig(List<Instrument> instruments) {
//         this.instruments = instruments;
//     }

//     public static void loadDefaults(String filepath,
//             Instruments instruments,
//             ControlCodes controlCodes,
//             Captions captions,
//             Pads pads) throws IOException {

//         // Get existing instruments from DB as a map
//         Map<String, Instrument> existingInstruments = instruments.findAll().stream()
//                 .collect(Collectors.toMap(Instrument::getName, Function.identity()));

//         List<String> devices = new ArrayList<>();

//         MIDIService.getMidiOutDevices().forEach(device -> devices.add(device.getDeviceInfo().getName()));

//         existingInstruments.values().forEach(ins -> {

//             if (ins.getAvailable() != devices.contains(ins.getDeviceName())) {
//                 ins.setAvailable(devices.contains(ins.getDeviceName()));
//                 instruments.save(ins);
//             }
//         });

//         // Get available MIDI devices and create if not in DB
//         List<String> availableDevices = MIDIService.getMidiOutDevices().stream()
//                 .filter(d -> d.getMaxTransmitters() != -1)
//                 .map(d -> {
//                     String deviceName = d.getDeviceInfo().getName();
//                     if (!existingInstruments.containsKey(deviceName)) {
//                         Instrument instrument = new Instrument(deviceName, d);
//                         instrument.setAvailable(true);
//                         instrument = instruments.save(instrument);
//                         existingInstruments.put(deviceName, instrument);
//                     }
//                     return deviceName;
//                 })
//                 .collect(Collectors.toList());

//         // Load and process config file
//         File configFile = new File(filepath);
//         if (configFile.exists()) {
//             DeviceConfig config = mapper.readValue(configFile, DeviceConfig.class);

//             // Process each instrument from config
//             config.getInstruments().forEach(configInstrument -> {
//                 configInstrument.setAvailable(availableDevices.contains(configInstrument.getName()));

//                 // Update existing or create new
//                 Instrument dbInstrument = existingInstruments.containsKey(configInstrument.getName())
//                         ? updateExistingInstrument(configInstrument,
//                                 existingInstruments.get(configInstrument.getName()))
//                         : configInstrument;

//                 // Process control codes
//                 if (!dbInstrument.getControlCodes().isEmpty())
//                     processControlCodesCaptionsAssignmentsAndBoundaries(dbInstrument, configInstrument, controlCodes,
//                             captions);

//                 if (Objects.isNull(dbInstrument.getHighestNote()) || dbInstrument.getHighestNote() == 0)
//                     dbInstrument.setHighestNote(126);

//                 if (Objects.isNull(dbInstrument.getLowestNote()))
//                     dbInstrument.setLowestNote(0);

//                 // Process pads if needed
//                 if (dbInstrument.getHighestNote() - dbInstrument.getLowestNote() != dbInstrument.getHighestNote())
//                     addPadInfo(pads, dbInstrument);

//                 try {
//                     // Save instrument
//                     dbInstrument = instruments.save(dbInstrument);
//                 } catch (Exception e) {
//                     System.out.println("Failed to save instrument: " + dbInstrument.getName());
//                 }

//             });
//         }
//     }

//     private static Instrument updateExistingInstrument(Instrument source, Instrument target) {
//         // Update fields but preserve ID and relationships
//         target.setChannels(source.getChannels());
//         target.setDeviceName(source.getDeviceName());
//         target.setLowestNote(source.getLowestNote());
//         target.setHighestNote(source.getHighestNote());
//         target.setHighestPreset(source.getHighestPreset());
//         target.setPreferredPreset(source.getPreferredPreset());
//         // target.setAssignments(source.getAssignments());
//         // target.setBoundaries(source.getBoundaries());
//         // target.setCaptions(source.getCaptions());
//         return target;
//     }

//     private static void processControlCodesCaptionsAssignmentsAndBoundaries(Instrument dbInstrument,
//             Instrument configInstrument,
//             ControlCodes controlCodes,
//             Captions captions) {

//         dbInstrument.getControlCodes().clear(); // Clear existing control codes for
//         dbInstrument.getAssignments().clear();
//         dbInstrument.getBoundaries().clear();
//         dbInstrument.getCaptions().clear();

//         // instrument.getAssignments().keySet().forEach(code -> {
//         configInstrument.getControlCodes().forEach(cc -> {
//             Integer code = cc.getCode();
//             ControlCode controlCode = new ControlCode();
//             controlCode.setCode(code);
//             controlCode.setName(cc.getName());
//             controlCode.setLowerBound(cc.getLowerBound());
//             controlCode.setUpperBound(cc.getUpperBound());
//             // controlCode.setPad(cc.getPad());
//             controlCode.setBinary(cc.getBinary());
//             if (!cc.getCaptions().isEmpty()) {
//                 List<Caption> newCaptions = new ArrayList<>();
//                 cc.getCaptions().forEach(cap -> {
//                     Caption caption = new Caption();
//                     caption.setCode(cap.getCode());
//                     caption.setDescription(cap.getDescription());
//                     caption = captions.save(caption);
//                     newCaptions.add(caption);
//                 });
//                 controlCode.getCaptions().addAll(newCaptions);
//             }
//             controlCode = controlCodes.save(controlCode);

//             dbInstrument.getAssignments().put(controlCode.getCode(), controlCode.getName());
//             dbInstrument.getBoundaries().put(controlCode.getCode(),
//                     new Integer[] { controlCode.getLowerBound(), controlCode.getUpperBound() });

//             if (!controlCode.getCaptions().isEmpty())
//                 dbInstrument.getCaptions().put(controlCode.getCode(),
//                         controlCode.getCaptions().stream()
//                                 .collect(Collectors.toMap(Caption::getCode, Caption::getDescription)));

//             dbInstrument.getControlCodes().add(controlCode);
//         });
//     }

//     public static void saveCurrentStateToFile(String filepath,
//             Instruments instruments) throws IOException {
//         DeviceConfig currentConfig = new DeviceConfig();

//         // Create clean copies of instruments without circular references
//         List<Instrument> cleanInstruments = instruments.findAll().stream()
//                 .map(instrument -> {
//                     Instrument clean = new Instrument();
//                     clean.setName(instrument.getName());
//                     clean.setDeviceName(instrument.getDeviceName());
//                     clean.setChannels(instrument.getChannels());
//                     clean.setLowestNote(instrument.getLowestNote());
//                     clean.setHighestNote(instrument.getHighestNote());
//                     clean.setHighestPreset(instrument.getHighestPreset());
//                     clean.setPreferredPreset(instrument.getPreferredPreset());
//                     clean.setHasAssignments(instrument.getHasAssignments());
//                     clean.setPlayerClassName(instrument.getPlayerClassName());
//                     clean.setAvailable(instrument.getAvailable());

//                     // Clean copy of control codes
//                     clean.setControlCodes(instrument.getControlCodes().stream()
//                             .map(cc -> {
//                                 ControlCode cleanCC = new ControlCode();
//                                 cleanCC.setCode(cc.getCode());
//                                 cleanCC.setName(cc.getName());
//                                 cleanCC.setLowerBound(cc.getLowerBound());
//                                 cleanCC.setUpperBound(cc.getUpperBound());
//                                 cleanCC.setBinary(cc.getBinary());
//                                 // Create clean copies of captions
//                                 cleanCC.setCaptions(cc.getCaptions().stream()
//                                         .map(cap -> {
//                                             Caption cleanCap = new Caption();
//                                             cleanCap.setCode(cap.getCode());
//                                             cleanCap.setDescription(cap.getDescription());
//                                             return cleanCap;
//                                         })
//                                         .collect(Collectors.toSet()));
//                                 return cleanCC;
//                             })
//                             .collect(Collectors.toList()));

//                     return clean;
//                 })
//                 .collect(Collectors.toList());

//         currentConfig.setInstruments(cleanInstruments);

//         // Configure mapper for pretty printing
//         ObjectMapper prettyMapper = new ObjectMapper()
//                 .configure(SerializationFeature.INDENT_OUTPUT, true)
//                 .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
//                 .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

//         // Write to file
//         File outputFile = new File(filepath);
//         prettyMapper.writeValue(outputFile, currentConfig);

//         System.out.println("config saved");
//     }

//     static void addPadInfo(Pads padRepo, Instrument instrument) {

//         List<Pad> pads = new ArrayList<>(IntStream.range(instrument.getLowestNote(), instrument.getHighestNote())
//                 .mapToObj(note -> new Pad(note)).toList());

//         instrument.getControlCodes().forEach(cc -> {
//             if (pads.size() > 0 && Strike.kickParams.contains(cc.getCode()))
//                 pads.get(0).getControlCodes().add(cc.getCode());

//             if (pads.size() > 1 && Strike.snarePrams.contains(cc.getCode()))
//                 pads.get(1).getControlCodes().add(cc.getCode());

//             if (pads.size() > 2 && cc.getCode() > 23 && cc.getCode() < 29)
//                 pads.get(2).getControlCodes().add(cc.getCode());

//             if (pads.size() > 3 && cc.getCode() > 28 && cc.getCode() < 32)
//                 pads.get(3).getControlCodes().add(cc.getCode());

//             if (pads.size() > 4 && cc.getCode() > 31 && cc.getCode() < 40)
//                 pads.get(4).getControlCodes().add(cc.getCode());

//             if (pads.size() > 5 && cc.getCode() > 39 && cc.getCode() < 45)
//                 pads.get(5).getControlCodes().add(cc.getCode());

//             if (pads.size() > 6 && cc.getCode() > 44 && cc.getCode() < 56)
//                 pads.get(6).getControlCodes().add(cc.getCode());

//             if (pads.size() > 7 && cc.getCode() > 55 && cc.getCode() < 64)
//                 pads.get(7).getControlCodes().add(cc.getCode());

//             if (pads.size() > 8 && cc.getCode() > 63 && cc.getCode() < 72)
//                 pads.get(8).getControlCodes().add(cc.getCode());
//         });

//         pads.forEach(pad -> {
//             var index = pads.indexOf(pad);
//             switch (index) {
//                 case 0:
//                     pad.setName("Kick");
//                     break;
//                 case 1:
//                     pad.setName("Snare");
//                     break;
//                 case 2:
//                     pad.setName("Closed Hi-Hat");
//                     break;
//                 case 3:
//                     pad.setName("Open Hi-Hat");
//                     break;
//                 case 4:
//                     pad.setName("Crash");
//                     break;
//                 case 5:
//                     pad.setName("Low Tom");
//                     break;
//                 case 6:
//                     pad.setName("Md Tom");
//                     break;
//                 case 7:
//                     pad.setName("High Tom");
//                     break;
//                 default:
//                     pad.setName(pad.getNote().toString());
//             }
//         });

//         pads.forEach(pad -> instrument.getPads().add(padRepo.save(pad)));
//     }

//     public ControlCode copy(ControlCode controlCode) {
//         ControlCode copy = new ControlCode();
//         copy.setCode(controlCode.getCode());
//         copy.setName(controlCode.getName());
//         copy.setLowerBound(controlCode.getLowerBound());
//         copy.setUpperBound(controlCode.getUpperBound());
//         // copy.setCaptions(controlCode.getCaptions());
//         return copy;
//     }

//     public Caption copy(Caption caption) {
//         Caption copy = new Caption();
//         copy.setCode(caption.getCode());
//         copy.setDescription(caption.getDescription());
//         return copy;
//     }

// }
