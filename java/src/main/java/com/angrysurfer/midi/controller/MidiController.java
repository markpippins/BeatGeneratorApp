package com.angrysurfer.midi.controller;

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

    @GetMapping(path = MIDIService.DEVICES_INFO)
    public @ResponseBody List<MidiDevice.Info> getDeviceInfo() {
        return service.getMidiDevices().stream().map(MidiDevice::getDeviceInfo).toList();
    }

    @GetMapping(path = MIDIService.DEVICE_NAMES)
    public @ResponseBody List<String> getDeviceNames() {
        return service.getMidiDevices().stream().map(d -> d.getDeviceInfo().getName()).toList();
    }

    @PostMapping(path = MIDIService.SERVICE_RESET)
    public void reset() {
        service.reset();
    }

    @PostMapping(path = MIDIService.SERVICE_SELECT)
    public @ResponseBody boolean select(String name) {
        return service.select(name);
    }

    
    @GetMapping("/messages/send")
    public void sendMessage(@RequestParam int messageType, @RequestParam int channel, @RequestParam int data1, @RequestParam int data2) {
        // logger.info("/messages/send");
        service.sendMessage(messageType, channel, data1, data2);
    }

        // @GetMapping(path = "/instruments/info")
    // public @ResponseBody Map<String, MidiInstrument> getInstruments() {
    //     logger.info("/instruments/info");
    //     return beatGeneratorService.getInstrumentMap();
    // }

    @GetMapping(path = "/instrument/info")
    public @ResponseBody MidiInstrument getInstrument(int channel) {
        // logger.info("/instrument/info");
        return service.getInstrument(channel);
    }
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

}
