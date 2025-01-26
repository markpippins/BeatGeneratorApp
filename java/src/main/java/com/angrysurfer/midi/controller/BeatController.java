package com.angrysurfer.midi.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import com.angrysurfer.midi.model.Pattern;
import com.angrysurfer.midi.service.PlayerService;
import com.angrysurfer.midi.util.Constants;

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

    @GetMapping(path = Constants.SAVE_BEAT)
    public void saveBeat() {
        logger.info(Constants.SAVE_BEAT);
        service.saveBeat();
    }

    @GetMapping(Constants.PLAY_DRUM_NOTE)
    public void playDrumNote(@RequestParam String instrument, @RequestParam int channel, @RequestParam int note) {
        logger.info(Constants.PLAY_DRUM_NOTE);
        service.playDrumNote(instrument, channel, note);
    }
    @PostMapping(path = Constants.PLAY_SEQUENCE)
    public void setSteps(@RequestBody List<Pattern> steps) {
        logger.info(Constants.PLAY_SEQUENCE);
        service.setSteps(steps);
    }

    @GetMapping(path = Constants.SAVE_CONFIG)
    public void saveConfig() {
        logger.info(Constants.SAVE_CONFIG);
        service.saveConfig();
    }

}

