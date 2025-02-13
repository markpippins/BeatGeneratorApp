package com.angrysurfer.spring.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.angrysurfer.core.api.IRule;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.util.Constants;
import com.angrysurfer.spring.service.PlayerService;

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
    public Set<IRule> getRules(@RequestParam Long playerId) {
        logger.info(Constants.RULES_FOR_PLAYER);
        return service.getRules(playerId);
    }

    @GetMapping(Constants.ADD_RULE)
    public IRule addRule(@RequestParam Long playerId) {
        logger.info(Constants.ADD_RULE);
        return service.addRule(playerId);
    }

    @GetMapping(Constants.REMOVE_RULE)
    public void removeCondition(@RequestParam Long playerId, @RequestParam Long ruleId) {
        logger.info(Constants.REMOVE_RULE);
        service.removeRule(playerId, ruleId);
    }

    @GetMapping(Constants.SPECIFY_RULE)
    public void updateRule(@RequestParam Long playerId,
            @RequestParam int operator,
            @RequestParam int comparison,
            @RequestParam String value,
            @RequestParam int part) {
        logger.info(Constants.UPDATE_RULE);
        service.addRule(playerId, operator, comparison, Double.parseDouble(value), part);
    }

    @GetMapping(Constants.UPDATE_RULE)
    public ResponseEntity<Rule> updateRule(@RequestParam Long ruleId, @RequestParam int updateType,
            @RequestParam int updateValue) {
        logger.info(Constants.UPDATE_RULE);
        return new ResponseEntity<Rule>(service.updateRule(ruleId, updateType, updateValue), HttpStatus.OK);
    }
}
