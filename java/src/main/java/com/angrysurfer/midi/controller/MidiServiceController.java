package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.service.IMIDIService;
import com.angrysurfer.midi.service.MIDIService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.sound.midi.MidiDevice;
import java.util.List;

@CrossOrigin("*")
@RequestMapping(path = "/api")
@Controller
@RestController
public class MidiServiceController {
    private IMIDIService midiService;

    public MidiServiceController(IMIDIService midiService) {
        this.midiService = midiService;
    }

    @GetMapping(path = MIDIService.DEVICES_INFO)
    public @ResponseBody List<MidiDevice.Info> getDeviceInfo() {
        return midiService.getMidiDevices().stream().map(MidiDevice::getDeviceInfo).toList();
    }

    @GetMapping(path = MIDIService.DEVICE_NAMES)
    public @ResponseBody List<String> getDeviceNames() {
        return midiService.getMidiDevices().stream().map(d -> d.getDeviceInfo().getName()).toList();
    }

    @PostMapping(path = MIDIService.SERVICE_RESET)
    public void reset() {
        midiService.reset();
    }

    @PostMapping(path = MIDIService.SERVICE_SELECT)
    public @ResponseBody boolean select(String name) {
        return midiService.select(name);
    }
}
