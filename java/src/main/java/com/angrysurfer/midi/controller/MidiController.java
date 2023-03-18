package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Constants;
import com.angrysurfer.midi.model.MidiInstrument;
import com.angrysurfer.midi.service.MIDIService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.sound.midi.MidiDevice;
import java.util.List;

@CrossOrigin("*")
@RequestMapping(path = "/api")
@Controller
@RestController
public class MidiController {
    private MIDIService service;

    public MidiController(MIDIService midiService) {
        this.service = midiService;
    }

    @GetMapping(path = Constants.DEVICES_INFO)
    public @ResponseBody List<MidiDevice.Info> getDeviceInfo() {
        return MIDIService.getMidiDevices().stream().map(MidiDevice::getDeviceInfo).toList();
    }

    @GetMapping(path = Constants.DEVICE_NAMES)
    public @ResponseBody List<String> getDeviceNames() {
        return MIDIService.getMidiDevices().stream().map(d -> d.getDeviceInfo().getName()).toList();
    }

    @PostMapping(path = Constants.SERVICE_RESET)
    public void reset() {
        MIDIService.reset();
    }

    @PostMapping(path = Constants.SERVICE_SELECT)
    public @ResponseBody boolean select(String name) {
        return MIDIService.select(name);
    }

    @GetMapping(Constants.SEND_MESSAGE)
    public void sendMessage(@RequestParam int messageType, @RequestParam int channel, @RequestParam int data1, @RequestParam int data2) {
        // logger.info("/messages/send");
        service.sendMessage(messageType, channel, data1, data2);
    }

    @GetMapping(path = Constants.GET_INSTRUMENT_BY_CHANNEL)
    public @ResponseBody MidiInstrument getInstrumentByChannel(int channel) {
        // logger.info("/instrument/info");
        return service.getInstrumentByChannel(channel);
    }

    @GetMapping(path = Constants.GET_INSTRUMENT_BY_ID)
    public @ResponseBody MidiInstrument getInstrumentById(Long id) {
        // logger.info("/instrument/info");
        return service.getInstrumentById(id);
    }
}


// @GetMapping(path = "/instruments/info")
// public @ResponseBody Map<String, MidiInstrument> getInstruments() {
//     logger.info("/instruments/info");
//     return beatGeneratorService.getInstrumentMap();
// }

//    @PostMapping(path = "/tracks/add")
//    public void addTrack(@RequestBody List<StepData> steps) {
//
//    }
//
//    @PostMapping(path = "/sequence/play")
//    public void playSequence(@RequestBody List<StepData> steps) {
//        midiService.playSequence(steps);
//    }
//
//    @PostMapping(path = "/steps/update")
//    public void addTrack(@RequestBody StepData step) {
//
//    }


