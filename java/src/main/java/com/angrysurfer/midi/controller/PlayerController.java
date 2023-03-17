package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.PlayerInfo;
import com.angrysurfer.midi.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class PlayerController {
    static Logger logger = LoggerFactory.getLogger(PlayerController.class.getCanonicalName());
    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping(path = "/players/info")
    public @ResponseBody List<PlayerInfo> getPlayers() {
        logger.info("/players/info");
        return service.getPlayers();
    }

    @GetMapping("/players/clear")
    public void clearPlayers() {
        logger.info("/players/clear");
        service.clearPlayers();
    }

    @GetMapping("/players/new")
    public PlayerInfo addPlayer(@RequestParam Long instrumentId) {
        logger.info("/players/new");
        return service.addPlayer(instrumentId);
    }
        @GetMapping("/players/add")
    public PlayerInfo addPlayer(@RequestParam String instrument) {
        logger.info("/players/add");
        return service.addPlayer(instrument);
    }

    @GetMapping("/players/remove")
    public List<PlayerInfo> removePlayer(@RequestParam Long playerId) {
        logger.info("/players/remove");
        return service.removePlayer(playerId);
    }

    @GetMapping("/players/mute")
    public PlayerInfo mutePlayer(@RequestParam Long playerId) {
        logger.info("/players/mute");
        return service.mutePlayer(playerId);
    }

    @GetMapping("/player/update")
    public void updatePlayer(@RequestParam Long playerId, @RequestParam int updateType, @RequestParam int updateValue) {
        logger.info("/players/update");
        service.updatePlayer(playerId, updateType, updateValue);
    }

}

