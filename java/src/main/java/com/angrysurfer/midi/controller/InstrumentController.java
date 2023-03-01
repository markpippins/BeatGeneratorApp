package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.service.BeatGeneratorService;
import com.angrysurfer.midi.service.IMidiInstrument;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class InstrumentController {

    private final BeatGeneratorService service;

    public InstrumentController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping(path = "/instruments/info")
    public @ResponseBody Map<String, IMidiInstrument> getInstruments() {
        return service.getInstruments();
    }

    @GetMapping(path = "/instrument/info")
    public @ResponseBody IMidiInstrument getInstrument(int channel) {
        return service.getInstrument(channel);
    }
}

