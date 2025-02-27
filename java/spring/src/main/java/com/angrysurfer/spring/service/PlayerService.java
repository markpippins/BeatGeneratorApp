package com.angrysurfer.spring.service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.util.Comparison;
import com.angrysurfer.core.util.Operator;
import com.angrysurfer.core.util.update.RuleUpdateType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    private PlayerManager playerManager = PlayerManager.getInstance();
    private TickerService tickerService;
    private InstrumentService instrumentService;
    private RedisService redisService;

    public PlayerService(TickerService tickerService,
            InstrumentService instrumentService,
            RedisService redisService) {
        this.tickerService = tickerService;
        this.instrumentService = instrumentService;
        this.redisService = redisService;
    }

    public Player addPlayer(String instrumentName) {
        Instrument instrument = instrumentService.findByName(instrumentName);
        return addPlayer(instrument, getNoteForMidiInstrument(instrument));
    }

    public Player addPlayer(String instrumentName, long note) {
        Instrument instrument = instrumentService.findByName(instrumentName);
        return addPlayer(instrument, note);
    }

    private long getNoteForMidiInstrument(Instrument instrument) {
        Long note = Objects.nonNull(instrument.getLowestNote()) ? instrument.getLowestNote() : 60L;
        return note + getTicker().getPlayers().size();
    }

    public Player addPlayer(Instrument instrument, long note) {
        Player player = playerManager.addPlayer(getTicker(), instrument, note);
        redisService.savePlayer(player);
        return player;
    }

    public Rule addRule(Long playerId) {
        logger.info("addRule() - playerId: {}", playerId);
        Player player = getTicker().getPlayer(playerId);
        Rule rule = playerManager.addRule(player, Operator.BEAT, Comparison.EQUALS, 1.0, 0);
        redisService.saveRule(rule);
        return rule;
    }

    public Rule addRule(Long playerId, int operator, int comparison, double value, int part) {
        logger.info("addRule() - playerId: {}, operator: {}, comparison: {}, value: {}, part: {}",
                playerId, operator, comparison, value, part);
        Player player = getTicker().getPlayer(playerId);
        Rule rule = playerManager.addRule(player, operator, comparison, value, part);
        redisService.saveRule(rule);
        return rule;
    }

    public void removeRule(Long playerId, Long ruleId) {
        logger.info("removeRule() - playerId: {}, ruleId: {}", playerId, ruleId);
        Player player = getTicker().getPlayer(playerId);
        playerManager.removeRule(player, ruleId);
        redisService.savePlayer(player);
    }

    public Rule updateRule(Long ruleId, int updateType, int updateValue) {
        logger.info("updateRule() - ruleId: {}, updateType: {}, updateValue: {}",
                ruleId, updateType, updateValue);
        Rule rule = redisService.findRuleById(ruleId);

        if (rule != null) {
            switch (updateType) {
                case RuleUpdateType.OPERATOR -> rule.setOperator(updateValue);
                case RuleUpdateType.COMPARISON -> rule.setComparison(updateValue);
                case RuleUpdateType.VALUE -> rule.setValue((double) updateValue);
                case RuleUpdateType.PART -> rule.setPart(updateValue);
            }
            redisService.saveRule(rule);
            return rule;
        }
        return null;
    }

    public Player updatePlayer(Long playerId, int updateType, int updateValue) {
        logger.info("updatePlayer() - playerId: {}, updateType: {}, updateValue: {}",
                playerId, updateType, updateValue);
        Ticker ticker = getTicker();
        if (ticker != null) {
            Player player = playerManager.updatePlayer(ticker, playerId, updateType, updateValue);
            if (player != null) {
                redisService.savePlayer(player);
            }
            return player;
        }
        return null;
    }

    public Player mutePlayer(Long playerId) {
        logger.info("mutePlayer() - playerId: {}", playerId);
        Ticker ticker = getTicker();
        if (ticker != null) {
            Player player = playerManager.mutePlayer(ticker, playerId);
            if (player != null) {
                redisService.savePlayer(player);
            }
            return player;
        }
        return null;
    }

    public Set<Rule> getRules(Long playerId) {
        logger.info("getRules() - playerId: {}", playerId);
        Player player = getTicker().getPlayer(playerId);
        return player != null ? player.getRules() : new HashSet<>();
    }

    public Set<Player> removePlayer(Long playerId) {
        Set<Player> players = playerManager.removePlayer(getTicker(), playerId);
        redisService.saveTicker(getTicker());
        return players;
    }

    public void clearPlayers() {
        playerManager.clearPlayers(getTicker());
        redisService.saveTicker(getTicker());
    }

    public void clearPlayersWithNoRules() {
        logger.info("clearPlayersWithNoRules()");
        Ticker ticker = getTicker();
        if (ticker != null) {
            playerManager.clearPlayersWithNoRules(ticker);
            redisService.saveTicker(ticker);
        }
    }

    private Ticker getTicker() {
        return tickerService.getTicker();
    }

    public Set<Player> getPlayers() {
        return getTicker().getPlayers();
    }

    // ... other necessary methods ...
}
