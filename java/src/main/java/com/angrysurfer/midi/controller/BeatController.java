package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.StepData;
import com.angrysurfer.midi.service.BeatGeneratorService;
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

    private final BeatGeneratorService service;

    public BeatController(BeatGeneratorService service) {
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
    public void setSteps(@RequestBody List<StepData> steps) {
        service.setSteps(steps);
    }

    @PostMapping(path = "/steps/update")
    public void addTrack(@RequestBody StepData step) {

    }

    @GetMapping("/messages/send")
    public void sendMessage(@RequestParam int messageType, @RequestParam int channel, @RequestParam int data1, @RequestParam int data2) {
        logger.info("/messages/send");
        service.sendMessage(messageType, channel, data1, data2);
    }

    
    @GetMapping(path = MIDIService.SAVE_CONFIG)
    public void saveConfig() {
        service.saveConfig();
    }

}

