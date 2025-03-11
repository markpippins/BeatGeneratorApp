package com.angrysurfer.spring.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.BusListener;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.PlayerManager;
import com.angrysurfer.core.service.SessionManager;
import com.angrysurfer.core.util.update.SessionUpdateType;
import com.angrysurfer.spring.dao.SessionStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Service
public class SessionService implements BusListener {

    static Logger logger = LoggerFactory.getLogger(SessionService.class.getCanonicalName());

    private RedisService redisService = RedisService.getInstance();
    private SongService songService;
    private InstrumentService instrumentService;

    public SessionService(SongService songService, InstrumentService instrumentService) {
        this.songService = songService;
        this.instrumentService = instrumentService;
        CommandBus.getInstance().register(this); // Register for command events
    }

    @Override
    public void onAction(Command action) {
        switch (action.getCommand()) {
            case Commands.TRANSPORT_PLAY -> play();
            case Commands.TRANSPORT_STOP -> stop();
            case Commands.TRANSPORT_PAUSE -> pause();
        }
    }

    public void play() {
        Session activeSession = SessionManager.getInstance().getActiveSession();


        getSongService().getSong().setBeatDuration(activeSession.getBeatDuration());
        getSongService().getSong().setTicksPerBeat(activeSession.getTicksPerBeat());

        SessionManager.getInstance().getActiveSession().play();
    }

    public SessionStatus getSessionStatus() {
        Session activeSession = SessionManager.getInstance().getActiveSession();
        return SessionStatus.from(activeSession, getSongService().getSong(), activeSession.isRunning());
    }

    public List<Session> getAllSessions() {
        return redisService.getAllSessionIds().stream()
                .map(id -> redisService.findSessionById(id))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Session getSession() {
        return SessionManager.getInstance().getActiveSession();
    }

    public Session updateSession(Long sessionId, int updateType, long updateValue) {
        Session session = redisService.findSessionById(sessionId);
        if (session != null) {
            switch (updateType) {
                case SessionUpdateType.PPQ -> session.setTicksPerBeat((int) updateValue);
                case SessionUpdateType.BEATS_PER_BAR -> session.setBeatsPerBar((int) updateValue);
                case SessionUpdateType.BPM -> {
                    session.setTempoInBPM(Float.valueOf(updateValue));
                }
                case SessionUpdateType.PARTS -> {
                    session.setParts((int) updateValue);
                    session.getPartCycler().reset();
                }
                case SessionUpdateType.BASE_NOTE_OFFSET ->
                    session.setNoteOffset(session.getNoteOffset() + updateValue);
                case SessionUpdateType.BARS -> session.setBars((int) updateValue);
                case SessionUpdateType.PART_LENGTH -> session.setPartLength(updateValue);
                case SessionUpdateType.MAX_TRACKS -> session.setMaxTracks((int) updateValue);
            }
            redisService.saveSession(session);
            return session;
        }
        return null;
    }

    public synchronized Session next(long currentSessionId) {
        if (SessionManager.getInstance().canMoveForward()) {
            SessionManager.getInstance().moveForward();
        }
        return SessionManager.getInstance().getActiveSession();
    }

    public synchronized Session previous(long currentSessionId) {
        if (SessionManager.getInstance().canMoveBack()) {
            SessionManager.getInstance().moveBack();
        }
        return SessionManager.getInstance().getActiveSession();
    }

    public void clearPlayers() {
        Session session = getSession();
        if (session != null) {
            PlayerManager.getInstance().removeAllPlayers(session);
        }
    }

    public Session loadSession(long sessionId) {
        Session session = redisService.findSessionById(sessionId);
        if (session != null) {
            SessionManager.getInstance().sessionSelected(session);
        }
        return session;
    }
    
    public void pause() {
        SessionManager.getInstance().getActiveSession().setPaused(true);
    }

    public void stop() {
        SessionManager.getInstance().getActiveSession().stop();
    }

}