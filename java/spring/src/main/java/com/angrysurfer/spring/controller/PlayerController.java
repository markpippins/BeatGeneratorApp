package com.angrysurfer.spring.controller;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.angrysurfer.core.model.player.IPlayer;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.spring.service.PlayerService;

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
    public @ResponseBody Set<IPlayer> getPlayers() {
        logger.info("GET " + Constants.ALL_PLAYERS);
        Set<IPlayer> players = (service.getPlayers());
        return players;
    }

    @GetMapping(Constants.CLEAR_PLAYERS)
    public void clearPlayers() {
        logger.info("GET " + Constants.CLEAR_PLAYERS);
        service.clearPlayers();
    }

    @GetMapping(Constants.CLEAR_PLAYERS_WITH_NO_RULES)
    public void clearPlayersWithNoRules() {
        logger.info("GET " + Constants.CLEAR_PLAYERS_WITH_NO_RULES);
        service.clearPlayersWithNoRules();
    }

    @GetMapping(Constants.ADD_PLAYER)
    public ResponseEntity<IPlayer> addPlayer(@RequestParam String instrument) {
        logger.info("GET " + Constants.ADD_PLAYER + " - instrument: {}", instrument);
        return new ResponseEntity<IPlayer>(service.addPlayer(instrument), HttpStatus.OK);
    }

    @GetMapping(Constants.ADD_PLAYER_FOR_NOTE)
    public ResponseEntity<IPlayer> addPlayer(@RequestParam String instrument, @RequestParam Long note) {
        logger.info("GET " + Constants.ADD_PLAYER_FOR_NOTE + " - instrument: {}, note: {}", instrument, note);
        return new ResponseEntity<IPlayer>(service.addPlayer(instrument, note), HttpStatus.OK);
    }

    @GetMapping(Constants.REMOVE_PLAYER)
    public Set<IPlayer> removePlayer(@RequestParam Long playerId) {
        logger.info("GET " + Constants.REMOVE_PLAYER + " - playerId: {}", playerId);
        return service.removePlayer(playerId);
    }

    @GetMapping(Constants.MUTE_PLAYER)
    public ResponseEntity<IPlayer> mutePlayer(@RequestParam Long playerId) {
        logger.info("GET " + Constants.MUTE_PLAYER + " - playerId: {}", playerId);
        return new ResponseEntity<IPlayer>(service.mutePlayer(playerId), HttpStatus.OK);
    }

    @GetMapping(Constants.UPDATE_PLAYER)
    public ResponseEntity<IPlayer> updatePlayer(@RequestParam Long playerId, @RequestParam int updateType,
            @RequestParam int updateValue) {
        logger.info("GET " + Constants.UPDATE_PLAYER + " - playerId: {}, updateType: {}, updateValue: {}", playerId,
                updateType, updateValue);
        return new ResponseEntity<IPlayer>(service.updatePlayer(playerId, updateType, updateValue),
                HttpStatus.OK);
    }

}
