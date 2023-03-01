package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.config.PlayerInfo;
import com.angrysurfer.midi.service.BeatGeneratorService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class PlayerController {

    private final BeatGeneratorService service;

    public PlayerController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping(path = "/players/info")
    public @ResponseBody List<PlayerInfo> getPlayers() {
        return service.getPlayers();
    }

    @GetMapping("/players/add")
    public PlayerInfo addPlayer(@RequestParam String instrument) {
        return service.addPlayer(instrument);
    }

    @GetMapping("/players/remove")
    public List<PlayerInfo> removePlayer(@RequestParam Long playerId) {
        return service.removePlayer(playerId);
    }

    @GetMapping("/players/mute")
    public PlayerInfo mutePlayer(@RequestParam Long playerId) {
        return service.mutePlayer(playerId);
    }

    @GetMapping("/player/rules")
    public List<Rule> getRules(@RequestParam Long playerId) {
        return service.getRules(playerId);
    }

    @GetMapping("/player/update")
    public void updatePlayer(@RequestParam Long playerId, @RequestParam int updateType, @RequestParam int updateValue) {
        service.updatePlayer(playerId, updateType, updateValue);
    }

    @GetMapping("/players/clear")
    public void clearPlayers() {
        service.clearPlayers();
    }
}

