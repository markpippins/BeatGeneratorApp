package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.Rule;
import com.angrysurfer.midi.model.config.PlayerInfo;
import com.angrysurfer.midi.model.config.TickerInfo;

import java.util.List;
import java.util.Map;

public interface IBeatGeneratorService {
    boolean play();
    boolean stop();
    void next();
    boolean pause();
    boolean previous();

    TickerInfo getTickerInfo();

    Map<String, IMidiInstrument> getInstruments();

    List<PlayerInfo> getPlayers();

    IMidiInstrument getInstrument(int channel);

    void sendMessage(int messageType, int channel, int data1, int data2);

    void clearPlayers();

    PlayerInfo addPlayer(String instrument);

    void updateRule(Long playerId,
                    int conditionId,
                    int operatorId,
                    int comparisonId,
                    double newValue);

    List<PlayerInfo> removePlayer(Long playerId);

    PlayerInfo mutePlayer(Long playerId);

    List<Rule> getRules(Long playerId);

    void save();

    void saveBeat();

    void updatePlayer(Long playerId, int updateType, int updateValue);

    Rule addRule(Long playerId);

    public void removeRule(Long playerId, Long conditionId);

    void loadTicker(long tickerId);
}
