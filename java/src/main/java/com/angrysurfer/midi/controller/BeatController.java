package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Step;
import com.angrysurfer.midi.service.PlayerService;
import com.angrysurfer.midi.service.MIDIService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class BeatController {
    static Logger logger = LoggerFactory.getLogger(BeatController.class.getCanonicalName());

    private final PlayerService service;

    public BeatController(PlayerService service) {
        this.service = service;
    }

    @GetMapping(path = "/beat/save")
    public void saveBeat() {
        logger.info("/beat/save");
        service.saveBeat();
    }

    @GetMapping("/drums/note")
    public void playDrumNote(@RequestParam String instrument, @RequestParam int channel, @RequestParam int note) {
        logger.info("/drum/note");
        service.playDrumNote(instrument, channel, note);
    }
    @PostMapping(path = "/sequence/play")
    public void setSteps(@RequestBody List<Step> steps) {
        service.setSteps(steps);
    }

    @GetMapping(path = MIDIService.SAVE_CONFIG)
    public void saveConfig() {
        service.saveConfig();
    }

}

