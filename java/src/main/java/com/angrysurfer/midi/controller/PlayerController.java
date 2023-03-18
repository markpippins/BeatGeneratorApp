package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Constants;
import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.model.Strike;
import com.angrysurfer.midi.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class PlayerController {
    static Logger logger = LoggerFactory.getLogger(PlayerController.class.getCanonicalName());
    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping(path = Constants.ALL_PLAYERS)
    public @ResponseBody Set<Strike> getPlayers() {
        logger.info(Constants.ALL_PLAYERS);
        return service.getPlayers();
    }

    @GetMapping(Constants.CLEAR_PLAYERS)
    public void clearPlayers() {
        logger.info(Constants.CLEAR_PLAYERS);
        service.clearPlayers();
    }

    @GetMapping(Constants.ADD_PLAYER)
    public Player addPlayer(@RequestParam String instrument) {
        logger.info(Constants.ADD_PLAYER);
        return service.addPlayer(instrument);
    }

    @GetMapping(Constants.REMOVE_PLAYER)
    public Set<Strike> removePlayer(@RequestParam Long playerId) {
        logger.info(Constants.REMOVE_PLAYER);
        return service.removePlayer(playerId);
    }

    @GetMapping(Constants.MUTE_PLAYER)
    public Player mutePlayer(@RequestParam Long playerId) {
        logger.info(Constants.MUTE_PLAYER);
        return service.mutePlayer(playerId);
    }

    @GetMapping(Constants.UPDATE_PLAYER)
    public void updatePlayer(@RequestParam Long playerId, @RequestParam int updateType, @RequestParam int updateValue) {
        logger.info(Constants.UPDATE_PLAYER);
        service.updatePlayer(playerId, updateType, updateValue);
    }

}

