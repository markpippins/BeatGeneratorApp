package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Player;
import com.angrysurfer.midi.service.PlayerService;
import com.angrysurfer.midi.util.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public @ResponseBody Set<Player> getPlayers() {
        logger.info(Constants.ALL_PLAYERS);
        Set<Player> players = (service.getPlayers());
        return players;
    }

    @GetMapping(Constants.CLEAR_PLAYERS)
    public void clearPlayers() {
        logger.info(Constants.CLEAR_PLAYERS);
        service.clearPlayers();
    }

    @GetMapping(Constants.CLEAR_PLAYERS_WITH_NO_RULES)
    public void clearPlayersWithNoRules() {
        logger.info(Constants.CLEAR_PLAYERS_WITH_NO_RULES);
        service.clearPlayersWithNoRules();
    }

    @GetMapping(Constants.ADD_PLAYER)
    public ResponseEntity<Player> addPlayer(@RequestParam String instrument) {
        logger.info(Constants.ADD_PLAYER);
        return new ResponseEntity<Player>(service.addPlayer(instrument), HttpStatus.OK);
    }

    @GetMapping(Constants.ADD_PLAYER_FOR_NOTE)
    public ResponseEntity<Player> addPlayer(@RequestParam String instrument, @RequestParam Long note) {
        logger.info(Constants.ADD_PLAYER_FOR_NOTE);
        return new ResponseEntity<Player>(service.addPlayer(instrument, note), HttpStatus.OK);
    }

    @GetMapping(Constants.REMOVE_PLAYER)
    public Set<Player> removePlayer(@RequestParam Long playerId) {
        logger.info(Constants.REMOVE_PLAYER);
        return service.removePlayer(playerId);
    }

    @GetMapping(Constants.MUTE_PLAYER)
    public ResponseEntity<Player> mutePlayer(@RequestParam Long playerId) {
        logger.info(Constants.MUTE_PLAYER);
        return new ResponseEntity<Player>(service.mutePlayer(playerId), HttpStatus.OK);
    }

    @GetMapping(Constants.UPDATE_PLAYER)
    public ResponseEntity<Player> updatePlayer(@RequestParam Long playerId, @RequestParam int updateType, @RequestParam int updateValue) {
        logger.info(Constants.UPDATE_PLAYER);
        return new ResponseEntity<Player>(service.updatePlayer(playerId, updateType, updateValue), HttpStatus.OK);
    }

}

