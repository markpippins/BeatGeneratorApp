package com.angrysurfer.spring.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.angrysurfer.core.model.Pattern;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.spring.service.PlayerService;

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
        logger.info("GET " + Constants.SAVE_BEAT);
        // service.saveBeat();
    }

    @GetMapping(Constants.PLAY_DRUM_NOTE)
    public void playDrumNote(@RequestParam String instrument, @RequestParam int channel, @RequestParam int note) {
        logger.info("GET " + Constants.PLAY_DRUM_NOTE + " - instrument: {}, channel: {}, note: {}", instrument, channel, note);
        // service.playDrumNote(instrument, channel, note);
    }
    @PostMapping(path = Constants.PLAY_SEQUENCE)
    public void setSteps(@RequestBody List<Pattern> steps) {
        logger.info("POST " + Constants.PLAY_SEQUENCE + " - steps size: {}", steps.size());
        // service.setSteps(steps);
    }

    @GetMapping(path = Constants.SAVE_CONFIG)
    public void saveConfig() {
        logger.info("GET " + Constants.SAVE_CONFIG);
        // service.saveConfig();
        logger.info("Save Called");
    }

}

