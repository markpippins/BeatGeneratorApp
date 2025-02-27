package com.angrysurfer.core.service;

import static com.angrysurfer.core.util.update.PlayerUpdateType.BEAT_FRACTION;
import static com.angrysurfer.core.util.update.PlayerUpdateType.CHANNEL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.FADE_IN;
import static com.angrysurfer.core.util.update.PlayerUpdateType.FADE_OUT;
import static com.angrysurfer.core.util.update.PlayerUpdateType.LEVEL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MAX_VELOCITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MIN_VELOCITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.MUTE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.NOTE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.PRESET;
import static com.angrysurfer.core.util.update.PlayerUpdateType.PROBABILITY;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RANDOM_DEGREE;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RATCHET_COUNT;
import static com.angrysurfer.core.util.update.PlayerUpdateType.RATCHET_INTERVAL;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SKIPS;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SOLO;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SUBDIVISIONS;
import static com.angrysurfer.core.util.update.PlayerUpdateType.SWING;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.CommandListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.model.Ticker;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerManager {
    private static final Logger logger = LoggerFactory.getLogger(PlayerManager.class);
    private static PlayerManager instance;
    private CommandBus actionBus = CommandBus.getInstance();
    private Player activePlayer;
    private final RedisService redisService;

    private PlayerManager() {
        setupCommandBusListener();
        redisService = RedisService.getInstance();
    }

    public static PlayerManager getInstance() {
        if (instance == null) {
            instance = new PlayerManager();
        }
        return instance;
    }

    private void setupCommandBusListener() {
        actionBus.register(new CommandListener() {
            @Override
            public void onAction(Command action) {
                switch (action.getCommand()) {
                    case Commands.PLAYER_SELECTED -> {
                        if (action.getData() instanceof Player) {
                            playerSelected((Player) action.getData());
                        }
                    }
                    case Commands.PLAYER_UPDATED -> {
                        if (action.getData() instanceof Player) {
                            playerUpdated((Player) action.getData());
                        }
                    }
                    case Commands.RULE_DELETE_REQUEST -> {
                        if (action.getData() instanceof Rule[] rules) {
                            logger.info("Processing rule delete request for {} rules", rules.length);

                            // Get player for first rule (all rules should be from same player)
                            Player player = rules[0].getPlayer();
                            if (player != null) {
                                // Remove rules from player
                                for (Rule rule : rules) {
                                    player.getRules().remove(rule);
                                    redisService.deleteRule(rule.getId());
                                    logger.info("Deleted rule: {}", rule.getId());
                                }

                                // Save updated player
                                redisService.savePlayer(player);

                                // Get fresh player state
                                Player refreshedPlayer = redisService.findPlayerById(player.getId());

                                // Notify UI
                                actionBus.publish(Commands.PLAYER_UPDATED, this, refreshedPlayer);
                                actionBus.publish(Commands.PLAYER_SELECTED, this, refreshedPlayer);

                                logger.info("Player {} updated after rule deletion", player.getId());
                            }
                        }
                    }
                }
            }
        });
    }

    public void playerSelected(Player player) {
        if (!Objects.equals(activePlayer, player)) {
            this.activePlayer = player;
            actionBus.publish(Commands.PLAYER_SELECTED, this, player);
        }
    }

    public void playerUpdated(Player player) {
        if (Objects.equals(activePlayer.getId(), player.getId())) {
            this.activePlayer = player;
            actionBus.publish(Commands.PLAYER_UPDATED, this, player);
        }
    }

    public Player addPlayer(Ticker ticker, Instrument instrument, long note) {
        String name = instrument.getName() + ticker.getPlayers().size();
        Player player = new Strike(name, ticker, instrument, note,
                instrument.getControlCodes().stream().map(cc -> cc.getCode()).toList());
        player.setTicker(ticker);
        ticker.getPlayers().add(player);
        return player;
    }

    public Rule addRule(Player player, int operator, int comparison, double value, int part) {
        Rule rule = new Rule(operator, comparison, value, part, true);
        if (player.getRules().stream().noneMatch(r -> r.isEqualTo(rule))) {
            rule.setPlayer(player);
            player.getRules().add(rule);
            actionBus.publish(Commands.RULE_ADDED, this, player);
            actionBus.publish(Commands.PLAYER_SELECTED, this, player);

            return rule;
        }
        return null;
    }

    public void removeRule(Player player, Long ruleId) {
        Rule rule = player.getRule(ruleId);
        player.getRules().remove(rule);
        rule.setPlayer(null);
    }

    public Set<Player> removePlayer(Ticker ticker, Long playerId) {
        Player player = ticker.getPlayer(playerId);
        ticker.getPlayers().remove(player);
        return ticker.getPlayers();
    }

    public void clearPlayers(Ticker ticker) {
        Set<Player> players = ticker.getPlayers();
        players.stream()
                .filter(p -> p.getRules().isEmpty())
                .forEach(p -> {
                    ticker.getPlayers().remove(p);
                    p.setTicker(null);
                });
    }

    public Player updatePlayer(Ticker ticker, Long playerId, int updateType, long updateValue) {
        Player player = ticker.getPlayer(playerId);
        if (player == null)
            return null;

        switch (updateType) {
            case CHANNEL -> {
                player.noteOff(0, 0);
                player.setChannel((int) updateValue);
            }
            case NOTE -> player.setNote(updateValue);
            case PRESET -> handlePresetChange(player, updateValue);
            case PROBABILITY -> player.setProbability(updateValue);
            case MIN_VELOCITY -> player.setMinVelocity(updateValue);
            case MAX_VELOCITY -> player.setMaxVelocity(updateValue);
            case RATCHET_COUNT -> player.setRatchetCount(updateValue);
            case RATCHET_INTERVAL -> player.setRatchetInterval(updateValue);
            case MUTE -> player.setMuted(updateValue > 0);
            case SOLO -> player.setSolo(updateValue > 0);
            case SKIPS -> {
                player.setSkips(updateValue);
                player.getSkipCycler().setLength(updateValue);
                player.getSkipCycler().reset();
            }
            case RANDOM_DEGREE -> player.setRandomDegree(updateValue);
            case BEAT_FRACTION -> player.setBeatFraction(updateValue);
            case SUBDIVISIONS -> {
                player.setSubDivisions(updateValue);
                player.getSubCycler().setLength(updateValue);
                player.getSubCycler().reset();
            }
            case SWING -> player.setSwing(updateValue);
            case LEVEL -> player.setLevel(updateValue);
            case FADE_IN -> player.setFadeIn(updateValue);
            case FADE_OUT -> player.setFadeOut(updateValue);
        }

        return player;
    }

    private void handlePresetChange(Player player, long updateValue) {
        try {
            player.noteOff(0, 0);
            player.setPreset(updateValue);
            player.getInstrument().setDevice(MIDIDeviceManager.getMidiDevice(player.getInstrument().getDeviceName()));
            player.getInstrument().programChange(player.getChannel(), updateValue, 0);
        } catch (InvalidMidiDataException | MidiUnavailableException e) {
            // logger.error(e.getMessage(), e);
        }
    }

    public Player mutePlayer(Ticker ticker, Long playerId) {
        Player player = ticker.getPlayer(playerId);
        if (player != null) {
            player.setMuted(!player.isMuted());
        }
        return player;
    }

    public void clearPlayersWithNoRules(Ticker ticker) {
        ticker.getPlayers().stream()
                .filter(p -> p.getRules().isEmpty())
                .forEach(p -> {
                    ticker.getPlayers().remove(p);
                    p.setTicker(null);
                });
    }

    // Player property update methods
    public void updatePlayerLevel(Player player, int level) {
        player.setLevel((long) level);
    }

    public void updatePlayerSparse(Player player, int sparse) {
        player.setSparse(sparse / 100.0);
    }

    public void updatePlayerPan(Player player, int pan) {
        player.setPanPosition((long) pan);
    }

    public void updatePlayerRandom(Player player, int random) {
        player.setRandomDegree((long) random);
    }

    public void updatePlayerVelocityMax(Player player, int velocityMax) {
        player.setMaxVelocity((long) velocityMax);
    }

    public void updatePlayerVelocityMin(Player player, int velocityMin) {
        player.setMinVelocity((long) velocityMin);
    }

    public void updatePlayerProbability(Player player, int probability) {
        player.setProbability((long) probability);
    }

    public void updatePlayerSwing(Player player, int swing) {
        player.setSwing((long) swing);
    }

    public void updatePlayerNote(Player player, int note) {
        player.setNote((long) note);
    }

    public void savePlayerProperties(Player player) {
        if (player != null) {
            RedisService.getInstance().savePlayer(player);
            actionBus.publish(Commands.PLAYER_UPDATED, this, player);
        }
    }

    // Rule management methods
    public void clearRules(Player player) {
        player.getRules().clear();
        actionBus.publish(Commands.RULES_CLEARED, this, player);
    }

    public void deleteRule(Rule rule) {
        Player player = rule.getPlayer();
        player.getRules().remove(rule);
        actionBus.publish(Commands.RULE_DELETED, this, player);
    }

    public void updatePlayer(Player player) {
        actionBus.publish(Commands.PLAYER_UPDATED, this, player);
    }

    public void addPlayer(Player player) {
        actionBus.publish(Commands.PLAYER_ADDED, this, player);
    }

    public void removeAllPlayers(Ticker ticker) {
        logger.info("Removing all players from ticker: {}", ticker.getId());

        Set<Player> players = ticker.getPlayers();

        ticker.setPlayers(new HashSet<>());
        redisService.saveTicker(ticker);

        for (Player player : players)
            redisService.deletePlayer(player);
    }
}