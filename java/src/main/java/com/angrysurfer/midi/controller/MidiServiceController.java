package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.service.IMIDIService;
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

    @GetMapping(path = "/devices/info")
    public @ResponseBody List<MidiDevice.Info> getDeviceInfo() {
        return midiService.getMidiDevices().stream().map(MidiDevice::getDeviceInfo).toList();
    }

    @GetMapping(path = "/devices/names")
    public @ResponseBody List<String> getDeviceNames() {
        return midiService.getMidiDevices().stream().map(d -> d.getDeviceInfo().getName()).toList();
    }

    @PostMapping(path = "/service/reset")
    public void reset() {
        midiService.reset();
    }

    @PostMapping(path = "/service/select")
    public @ResponseBody boolean select(String name) {
        return midiService.select(name);
    }
}
