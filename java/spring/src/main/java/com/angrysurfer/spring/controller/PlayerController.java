package com.angrysurfer.spring.controller;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.spring.service.PlayerService;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class PlayerController {
    static Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private final PlayerService service;

    public PlayerController(PlayerService service) {
        this.service = service;
    }

    @GetMapping(Constants.ALL_PLAYERS)
    public ResponseEntity<Set<Player>> getPlayers() {
        logger.info("GET {}", Constants.ALL_PLAYERS);
        return ResponseEntity.ok(service.getPlayers());
    }

    @DeleteMapping(Constants.CLEAR_PLAYERS)
    public ResponseEntity<Void> clearPlayers() {
        logger.info("DELETE {}", Constants.CLEAR_PLAYERS);
        service.clearPlayers();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping(Constants.CLEAR_PLAYERS_WITH_NO_RULES)
    public ResponseEntity<Void> clearPlayersWithNoRules() {
        logger.info("DELETE {}", Constants.CLEAR_PLAYERS_WITH_NO_RULES);
        service.clearPlayersWithNoRules();
        return ResponseEntity.noContent().build();
    }

    @PostMapping(Constants.ADD_PLAYER)
    public ResponseEntity<Player> addPlayer(@RequestParam String instrument) {
        logger.info("POST {} - instrument: {}", Constants.ADD_PLAYER, instrument);
        Player player = service.addPlayer(instrument);
        return player != null ? 
            ResponseEntity.status(HttpStatus.CREATED).body(player) : 
            ResponseEntity.badRequest().build();
    }

    @PostMapping(Constants.ADD_PLAYER_FOR_NOTE)
    public ResponseEntity<Player> addPlayer(
            @RequestParam String instrument, 
            @RequestParam Long note) {
        logger.info("POST {} - instrument: {}, note: {}", 
            Constants.ADD_PLAYER_FOR_NOTE, instrument, note);
        Player player = service.addPlayer(instrument, note);
        return player != null ? 
            ResponseEntity.status(HttpStatus.CREATED).body(player) : 
            ResponseEntity.badRequest().build();
    }

    @DeleteMapping(Constants.REMOVE_PLAYER)
    public ResponseEntity<Set<Player>> removePlayer(@RequestParam Long playerId) {
        logger.info("DELETE {} - playerId: {}", Constants.REMOVE_PLAYER, playerId);
        Set<Player> players = service.removePlayer(playerId);
        return ResponseEntity.ok(players);
    }

    @PutMapping(Constants.MUTE_PLAYER)
    public ResponseEntity<Player> mutePlayer(@RequestParam Long playerId) {
        logger.info("PUT {} - playerId: {}", Constants.MUTE_PLAYER, playerId);
        Player player = service.mutePlayer(playerId);
        return player != null ? 
            ResponseEntity.ok(player) : 
            ResponseEntity.notFound().build();
    }

    @PutMapping(Constants.UPDATE_PLAYER)
    public ResponseEntity<Player> updatePlayer(
            @RequestParam Long playerId, 
            @RequestParam int updateType,
            @RequestParam int updateValue) {
        logger.info("PUT {} - playerId: {}, updateType: {}, updateValue: {}", 
            Constants.UPDATE_PLAYER, playerId, updateType, updateValue);
        Player player = service.updatePlayer(playerId, updateType, updateValue);
        return player != null ? 
            ResponseEntity.ok(player) : 
            ResponseEntity.notFound().build();
    }
}
