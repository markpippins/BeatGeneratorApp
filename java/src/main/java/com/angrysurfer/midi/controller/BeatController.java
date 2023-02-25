package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.config.PlayerInfo;
import com.angrysurfer.midi.model.config.TickerInfo;
import com.angrysurfer.midi.service.BeatGeneratorService;
import com.angrysurfer.midi.service.IMidiInstrument;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class BeatController {

    private final BeatGeneratorService service;

    public BeatController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping(path = "/ticker/start")
    public void start() {
        service.start();
    }

    @GetMapping(path = "/ticker/pause")
    public void pause() {
        service.pause();
    }

    @GetMapping(path = "/ticker/stop")
    public void stop() {
        service.stop();
    }

    @GetMapping(path = "/ticker/next")
    public void next() {
        service.next();
    }

    @GetMapping(path = "/beat/save")
    public void saveBeat() {
        service.saveBeat();
    }

    @GetMapping(path = "/ticker/info")
    public @ResponseBody TickerInfo getTickerInfo() {
        return service.getTickerInfo();
    }

    @GetMapping(path = "/players/info")
    public @ResponseBody List<PlayerInfo> getPlayers() {
        return service.getPlayers();
    }

    @GetMapping(path = "/instruments/info")
    public @ResponseBody Map<String, IMidiInstrument> getInstruments() {
        return service.getInstruments();
    }

    @GetMapping(path = "/instrument/info")
    public @ResponseBody IMidiInstrument getInstrument(int channel) {
        return service.getInstrument(channel);
    }

    @GetMapping("/drums/note")
    public void playDrumNote(@RequestParam String instrument, @RequestParam int channel, @RequestParam int note) {
        service.playDrumNote(instrument, channel, note);
    }

    @GetMapping("/messages/send")
    public void sendMessage(@RequestParam int messageType, @RequestParam int channel, @RequestParam int data1, @RequestParam int data2) {
        service.sendMessage(messageType, channel, data1, data2);
    }
    @GetMapping("/players/add")
    public PlayerInfo addPlayer(@RequestParam String instrument) {
        return service.addPlayer(instrument);
    }

    @GetMapping("/players/remove")
    public PlayerInfo removePlayer(@RequestParam int playerId) {
        return service.removePlayer(playerId);
    }

    @GetMapping("/players/mute")
    public PlayerInfo mutePlayer(@RequestParam int playerId) {
        return service.mutePlayer(playerId);
    }

    @GetMapping("/player/conditions")
    public List<Condition> getConditions(@RequestParam int playerId) {
        return service.getConditions(playerId);
    }

    @GetMapping("/player/update")
    public void updatePlayer(@RequestParam int playerId, @RequestParam int updateType, @RequestParam int updateValue) {
        service.updatePlayer(playerId, updateType, updateValue);
    }

    @GetMapping("/players/clear")
    public void clearPlayers() {
        service.clearPlayers();
    }

    @GetMapping("/condition/update")
    public void updateCondition(@RequestParam int playerId,
                                @RequestParam int conditionId,
                                @RequestParam String newOperator,
                                @RequestParam String newComparison,
                                @RequestParam String newValue) {
        service.updateCondition(playerId, conditionId, newOperator, newComparison, Double.valueOf(newValue));
    }
}

