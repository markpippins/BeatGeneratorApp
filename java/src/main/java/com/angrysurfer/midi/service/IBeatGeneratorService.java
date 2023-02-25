package com.angrysurfer.midi.service;

import com.angrysurfer.midi.controller.PlayerUpdateDTO;
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

    void updateCondition(int playerId,
                         int conditionId,
                         String newOperator,
                         String newComparison,
                         double newValue);

    PlayerInfo removePlayer(int playerId);

    PlayerInfo mutePlayer(int playerId);

    List<Condition> getConditions(int playerId);

    void next();

    void save();

    void saveBeat();

    void updatePlayer(int playerId, int updateType, int updateValue);
}
