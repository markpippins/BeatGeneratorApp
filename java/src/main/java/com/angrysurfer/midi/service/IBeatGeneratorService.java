package com.angrysurfer.midi.service;

import com.angrysurfer.midi.model.config.PlayerInfo;
import com.angrysurfer.midi.model.config.TickerInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;

public interface IBeatGeneratorService {
    boolean start();

    boolean stop();

    boolean pause();

    boolean skipAhead();

    boolean skipBack();

    TickerInfo getTickerInfo();

    Map<String, IMidiInstrument> getInstruments();

    List<PlayerInfo> getPlayers();

    IMidiInstrument getInstrument(int channel);

    void sendMessage(int messageType, int channel, int data1, int data2);

    void clearPlayers();

    PlayerInfo addPlayer(String instrument);

    void updateCondition(Long playerId,
                         int conditionId,
                         int operatorId,
                         int comparisonId,
                         double newValue);

    PlayerInfo removePlayer(Long playerId);

    PlayerInfo mutePlayer(Long playerId);

    List<Condition> getConditions(Long playerId);

    void next();

    void save();

    void saveBeat();

    void updatePlayer(Long playerId, int updateType, int updateValue);

    void addCondition(Long playerId);

    public void removeCondition(Long playerId, Long conditionId);
}
