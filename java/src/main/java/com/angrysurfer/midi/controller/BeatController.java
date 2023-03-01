package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.service.BeatGeneratorService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class BeatController {

    private final BeatGeneratorService service;

    public BeatController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping(path = "/beat/save")
    public void saveBeat() {
        service.saveBeat();
    }

    @GetMapping("/drums/note")
    public void playDrumNote(@RequestParam String instrument, @RequestParam int channel, @RequestParam int note) {
        service.playDrumNote(instrument, channel, note);
    }

    @GetMapping("/messages/send")
    public void sendMessage(@RequestParam int messageType, @RequestParam int channel, @RequestParam int data1, @RequestParam int data2) {
        service.sendMessage(messageType, channel, data1, data2);
    }
}

