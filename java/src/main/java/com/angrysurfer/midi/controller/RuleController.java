package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.config.TickerInfo;
import com.angrysurfer.midi.service.BeatGeneratorService;
import com.angrysurfer.midi.service.IMidiInstrument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class RuleController {
    static Logger logger = LoggerFactory.getLogger(RuleController.class.getCanonicalName());

    private final BeatGeneratorService service;

    public RuleController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping("/player/rules")
    public List<Rule> getRules(@RequestParam Long playerId) {
        logger.info("/player/rules");
        return service.getRules(playerId);
    }

    @GetMapping("/rules/add")
    public Rule addRule(@RequestParam Long playerId) {
        logger.info("/rules/add");
        return service.addRule(playerId);
    }

    @GetMapping("/rules/remove")
    public void removeCondition(@RequestParam Long playerId, @RequestParam Long ruleId) {
        logger.info("/rules/remove");
        service.removeRule(playerId, ruleId);
    }

    @GetMapping("/rule/update")
    public void updateRule(@RequestParam Long playerId,
                           @RequestParam int ruleId,
                           @RequestParam int operatorId,
                           @RequestParam int comparisonId,
                           @RequestParam String newValue) {
        logger.info("/rules/update");
        service.updateRule(playerId, ruleId, operatorId, comparisonId, Double.parseDouble(newValue));
    }
}

