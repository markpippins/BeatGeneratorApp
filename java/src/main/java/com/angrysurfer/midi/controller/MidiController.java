package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.MidiDeviceInfo;
import com.angrysurfer.midi.service.MIDIService;
import com.angrysurfer.midi.util.Constants;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

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
    public @ResponseBody List<MidiDeviceInfo> getDeviceInfo() {
        return MIDIService.getMidiDeviceInfos();
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

    // @GetMapping(Constants.SEND_MESSAGE)
    // public void sendMessage(@RequestParam int channel, @RequestParam int messageType, @RequestParam int data1, @RequestParam int data2) {
    //     // logger.info("/messages/send");
    //     service.sendMessageToChannel(channel, messageType, data1, data2);
    // }
}
