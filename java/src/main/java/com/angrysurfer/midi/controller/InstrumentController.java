package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.service.BeatGeneratorService;
import com.angrysurfer.midi.service.IMidiInstrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class InstrumentController {
    static Logger logger = LoggerFactory.getLogger(InstrumentController.class.getCanonicalName());
    private final BeatGeneratorService service;

    public InstrumentController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping(path = "/instruments/info")
    public @ResponseBody Map<String, IMidiInstrument> getInstruments() {
        logger.info("/instruments/info");
        return service.getInstruments();
    }

    @GetMapping(path = "/instrument/info")
    public @ResponseBody IMidiInstrument getInstrument(int channel) {
        logger.info("/instrument/info");
        return service.getInstrument(channel);
    }
}

