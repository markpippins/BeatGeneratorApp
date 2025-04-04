package com.angrysurfer.spring.service;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.model.Comparison;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Operator;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.util.update.RuleUpdateType;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class PlayerService {

    private static final Logger logger = LoggerFactory.getLogger(PlayerService.class);

    private PlayerManager playerManager = PlayerManager.getInstance();
    private SessionService sessionService;
    private InstrumentService instrumentService;
    private RedisService redisService;

    public PlayerService(SessionService sessionService,
            InstrumentService instrumentService,
            RedisService redisService) {
        this.sessionService = sessionService;
        this.instrumentService = instrumentService;
        this.redisService = redisService;
    }

    public Player addPlayer(String instrumentName) {
        InstrumentWrapper instrument = instrumentService.findByName(instrumentName);
        return addPlayer(instrument, getNoteForMidiInstrument(instrument));
    }

    public Player addPlayer(String instrumentName, int note) {
        InstrumentWrapper instrument = instrumentService.findByName(instrumentName);
        return addPlayer(instrument, note);
    }

    private int getNoteForMidiInstrument(InstrumentWrapper instrument) {
        Integer note = Objects.nonNull(instrument.getLowestNote()) ? instrument.getLowestNote() : 60;
        return note + getSession().getPlayers().size();
    }

    public Player addPlayer(InstrumentWrapper instrument, int note) {
        Player player = playerManager.addPlayer(getSession(), instrument, note);
        redisService.savePlayer(player);
        return player;
    }

    public Rule addRule(Long playerId) {
        logger.info("addRule() - playerId: {}", playerId);
        Player player = getSession().getPlayer(playerId);
        Rule rule = playerManager.addRule(player, Comparison.BEAT, Operator.EQUALS, 1.0, 0);
        redisService.saveRule(rule);
        return rule;
    }

    public Rule addRule(Long playerId, int operator, int comparison, double value, int part) {
        logger.info("addRule() - playerId: {}, operator: {}, comparison: {}, value: {}, part: {}",
                playerId, operator, comparison, value, part);
        Player player = getSession().getPlayer(playerId);
        Rule rule = playerManager.addRule(player, operator, comparison, value, part);
        redisService.saveRule(rule);
        return rule;
    }

    public void removeRule(Long playerId, Long ruleId) {
        logger.info("removeRule() - playerId: {}, ruleId: {}", playerId, ruleId);
        Player player = getSession().getPlayer(playerId);
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
        Session session = getSession();
        if (session != null) {
            Player player = playerManager.updatePlayer(session, playerId, updateType, updateValue);
            if (player != null) {
                redisService.savePlayer(player);
            }
            return player;
        }
        return null;
    }

    public Player mutePlayer(Long playerId) {
        logger.info("mutePlayer() - playerId: {}", playerId);
        Session session = getSession();
        if (session != null) {
            Player player = playerManager.mutePlayer(session, playerId);
            if (player != null) {
                redisService.savePlayer(player);
            }
            return player;
        }
        return null;
    }

    public Set<Rule> getRules(Long playerId) {
        logger.info("getRules() - playerId: {}", playerId);
        Player player = getSession().getPlayer(playerId);
        return player != null ? player.getRules() : new HashSet<>();
    }

    public Set<Player> removePlayer(Long playerId) {
        Set<Player> players = playerManager.removePlayer(getSession(), playerId);
        redisService.saveSession(getSession());
        return players;
    }

    public void clearPlayers() {
        playerManager.clearPlayers(getSession());
        redisService.saveSession(getSession());
    }

    public void clearPlayersWithNoRules() {
        logger.info("clearPlayersWithNoRules()");
        Session session = getSession();
        if (session != null) {
            playerManager.clearPlayersWithNoRules(session);
            redisService.saveSession(session);
        }
    }

    private Session getSession() {
        return sessionService.getSession();
    }

    public Set<Player> getPlayers() {
        return getSession().getPlayers();
    }

    // ... other necessary methods ...
}
