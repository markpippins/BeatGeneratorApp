package com.angrysurfer.sequencer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.angrysurfer.sequencer.model.midi.MidiDeviceInfo;
import com.angrysurfer.sequencer.service.MIDIService;
import com.angrysurfer.sequencer.util.Constants;

import java.util.List;

import javax.sound.midi.MidiUnavailableException;

@CrossOrigin("*")
@RequestMapping(path = "/api")
@Controller
@RestController
public class MidiController {

    @GetMapping(path = Constants.DEVICES_INFO)
    public @ResponseBody List<MidiDeviceInfo> getDeviceInfo() {
        return MIDIService.getMidiDeviceInfos();
    }

    @GetMapping(path = Constants.DEVICE_NAMES)
    public @ResponseBody List<String> getDeviceNames() {
        return MIDIService.getMidiOutDevices().stream().map(d -> d.getDeviceInfo().getName()).toList();
    }

    @PostMapping(path = Constants.SERVICE_RESET)
    public void reset() {
        MIDIService.reset();
    }

    @PostMapping(path = Constants.SERVICE_SELECT)
    public @ResponseBody boolean select(String name) {
        try {
            return MIDIService.select(name);
        } catch (MidiUnavailableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return false;
    }

    // @GetMapping(Constants.SEND_MESSAGE)
    // public void sendMessage(@RequestParam int channel, @RequestParam int
    // messageType, @RequestParam int data1, @RequestParam int data2) {
    // // logger.info("/messages/send");
    // service.sendMessageToChannel(channel, messageType, data1, data2);
    // }
}
