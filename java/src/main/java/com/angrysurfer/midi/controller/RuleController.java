package com.angrysurfer.midi.controller;

import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.config.TickerInfo;
import com.angrysurfer.midi.service.BeatGeneratorService;
import com.angrysurfer.midi.service.IMidiInstrument;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin("*")
@RestController
@RequestMapping("/api")
public class RuleController {

    private final BeatGeneratorService service;

    public RuleController(BeatGeneratorService service) {
        this.service = service;
    }

    @GetMapping("/player/rules")
    public List<Rule> getRules(@RequestParam Long playerId) {
        return service.getRules(playerId);
    }

    @GetMapping("/rules/add")
    public Rule addRule(@RequestParam Long playerId) {
        return service.addRule(playerId);
    }

    @GetMapping("/rules/remove")
    public void removeCondition(@RequestParam Long playerId, @RequestParam Long ruleId) {
        service.removeRule(playerId, ruleId);
    }

    @GetMapping("/rule/update")
    public void updateRule(@RequestParam Long playerId,
                           @RequestParam int ruleId,
                           @RequestParam int operatorId,
                           @RequestParam int comparisonId,
                           @RequestParam String newValue) {
        service.updateRule(playerId, ruleId, operatorId, comparisonId, Double.parseDouble(newValue));
    }
}

