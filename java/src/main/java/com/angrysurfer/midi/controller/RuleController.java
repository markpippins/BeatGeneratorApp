package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.service.PlayerService;
import com.angrysurfer.midi.util.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class RuleController {
    static Logger logger = LoggerFactory.getLogger(RuleController.class.getCanonicalName());

    private final PlayerService service;

    public RuleController(PlayerService service) {
        this.service = service;
    }

    @GetMapping(Constants.RULES_FOR_PLAYER)
    public Set<Rule> getRules(@RequestParam Long playerId) {
        logger.info(Constants.RULES_FOR_PLAYER);
        return service.getRules(playerId);
    }

    @GetMapping(Constants.ADD_RULE)
    public Rule addRule(@RequestParam Long playerId) {
        logger.info(Constants.ADD_RULE);
        return service.addRule(playerId);
    }

    @GetMapping(Constants.REMOVE_RULE)
    public void removeCondition(@RequestParam Long playerId, @RequestParam Long ruleId) {
        logger.info(Constants.REMOVE_RULE);
        service.removeRule(playerId, ruleId);
    }

    @GetMapping(Constants.UPDATE_RULE)
    public void updateRule(@RequestParam Long playerId,
                           @RequestParam Long ruleId,
                           @RequestParam int operatorId,
                           @RequestParam int comparisonId,
                           @RequestParam String newValue,
                           @RequestParam int part) {
        logger.info(Constants.UPDATE_RULE);
        service.updateRule(playerId, ruleId, operatorId, comparisonId, Double.parseDouble(newValue), part);
    }
}

