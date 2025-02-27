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

import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.spring.service.PlayerService;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class RuleController {
    static Logger logger = LoggerFactory.getLogger(RuleController.class);
    private final PlayerService service;

    public RuleController(PlayerService service) {
        this.service = service;
    }

    @GetMapping(Constants.RULES_FOR_PLAYER)
    public ResponseEntity<Set<Rule>> getRules(@RequestParam Long playerId) {
        logger.info("GET {}", Constants.RULES_FOR_PLAYER);
        return ResponseEntity.ok(service.getRules(playerId));
    }

    @PostMapping(Constants.ADD_RULE)
    public ResponseEntity<Rule> addRule(@RequestParam Long playerId) {
        logger.info("POST {}", Constants.ADD_RULE);
        Rule rule = service.addRule(playerId);
        return rule != null ? 
            ResponseEntity.status(HttpStatus.CREATED).body(rule) : 
            ResponseEntity.badRequest().build();
    }

    @DeleteMapping(Constants.REMOVE_RULE)
    public ResponseEntity<Void> removeRule(@RequestParam Long playerId, @RequestParam Long ruleId) {
        logger.info("DELETE {}", Constants.REMOVE_RULE);
        service.removeRule(playerId, ruleId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(Constants.SPECIFY_RULE)
    public ResponseEntity<Rule> specifyRule(
            @RequestParam Long playerId,
            @RequestParam int operator,
            @RequestParam int comparison,
            @RequestParam String value,
            @RequestParam int part) {
        logger.info("PUT {}", Constants.SPECIFY_RULE);
        Rule rule = service.addRule(playerId, operator, comparison, 
            Double.parseDouble(value), part);
        return rule != null ? 
            ResponseEntity.ok(rule) : 
            ResponseEntity.badRequest().build();
    }

    @PutMapping(Constants.UPDATE_RULE)
    public ResponseEntity<Rule> updateRule(
            @RequestParam Long ruleId, 
            @RequestParam int updateType, 
            @RequestParam int updateValue) {
        logger.info("PUT {}", Constants.UPDATE_RULE);
        Rule rule = service.updateRule(ruleId, updateType, updateValue);
        return rule != null ? 
            ResponseEntity.ok(rule) : 
            ResponseEntity.notFound().build();
    }
}

