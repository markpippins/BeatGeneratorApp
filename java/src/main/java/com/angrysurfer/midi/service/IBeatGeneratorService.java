package com.angrysurfer.midi.service;

import com.angrysurfer.midi.controller.ConditionDTO;
import com.angrysurfer.midi.model.PlayerCondition;
import com.angrysurfer.midi.model.config.PlayerInfo;
import com.angrysurfer.midi.model.config.TickerInfo;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

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

    void updateCondition(String playerId,
                         String conditionId,
                         String newOperator,
                         String newComparison,
                         double newValue);
}
