package com.angrysurfer.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Rule;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.midi.Instrument;
import com.angrysurfer.core.redis.RedisService;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SessionManager {

    private static final Logger logger = Logger.getLogger(SessionManager.class.getName());
    private static SessionManager instance;

    private final CommandBus commandBus = CommandBus.getInstance();
    private final RedisService redisService = RedisService.getInstance();
    private final SequencerManager sequencerManager = SequencerManager.getInstance();

    private final Map<Long, Instrument> instrumentCache = new HashMap<>();

    // Directly store activeSession instead of using SessionManager
    private Session activeSession;

    private SongEngine songEngine;

    private Player[] activePlayers[];
    private Rule[] selectedRules;

    private boolean isRecording = false;

    public static SessionManager getInstance() {
        if (instance == null) {
            synchronized (SessionManager.class) {
                if (instance == null) {
                    instance = new SessionManager();
                }
            }
        }
        return instance;
    }

    public void addPlayerToSession(Session currentSession, Player updatedPlayer) {
        redisService.addPlayerToSession(currentSession, updatedPlayer);
    }

    void handleSessionRequest() {
        if (Objects.nonNull(getActiveSession()))
            commandBus.publish(Commands.SESSION_SELECTED, this, getActiveSession());
    }

    // Direct getter instead of delegating
    public Session getActiveSession() {
        return activeSession;
    }

    // Direct setter instead of delegating
    public void setActiveSession(Session session) {
        if (session != null && !session.equals(this.activeSession)) {
            this.activeSession = session;
            commandBus.publish(Commands.SESSION_SELECTED, this, session);
            logger.info("Session selected: " + session.getId());
        }
    }

    public void initialize() {
        logger.info("Initializing session manager");

        List<Instrument> instruments = UserConfigManager.getInstance().getCurrentConfig().getInstruments();

        // Instead of creating SessionManager, directly load session
        loadActiveSession();
        logSessionState(getActiveSession());

        songEngine = new SongEngine();

        commandBus.register(new IBusListener() {
            @Override
            public void onAction(Command action) {
                if (action == null || action.getCommand() == null)
                    return;

                String cmd = action.getCommand();

                try {
                    switch (cmd) {

                        case Commands.SAVE_SESSION -> handleSessionSaveRequest();
                        case Commands.SESSION_REQUEST -> handleSessionRequest();
                        case Commands.TRANSPORT_REWIND -> moveBack();
                        case Commands.TRANSPORT_FORWARD -> moveForward();
                        case Commands.TRANSPORT_PLAY -> {
                            // Direct control of transport
                            if (activeSession != null) {
                                // Make session.initializeDevices() public, not private!
                                activeSession.initializeDevices(); // Call this first to ensure devices are ready

                                // Set this session as the active session in SequencerManager
                                sequencerManager.setActiveSession(activeSession);

                                // Start the sequencer directly
                                sequencerManager.start();
                            }
                        }
                        case Commands.TRANSPORT_STOP -> {
                            // Direct control of transport
                            if (activeSession != null) {
                                sequencerManager.stop();
                            }
                            // Also stop recording when transport stops
                            if (isRecording()) {
                                setRecording(false);
                                CommandBus.getInstance().publish(Commands.RECORDING_STOPPED, this);
                            }
                        }
                        case Commands.TRANSPORT_RECORD -> {
                            if (activeSession != null) {
                                redisService.saveSession(activeSession);
                            }
                        }
                        case Commands.SHOW_PLAYER_EDITOR_OK -> processPlayerEdit((Player) action.getData());
                        case Commands.SHOW_RULE_EDITOR_OK -> processRuleEdit((Rule) action.getData());
                        case Commands.PLAYER_DELETE_REQUEST -> {
                            if (action.getData() instanceof Long[] playerIds) {
                                processPlayerDeleteByIds(playerIds);
                            } else if (action.getData() instanceof Player[] players) {
                                // Support legacy code that sends Player[] instead of Long[]
                                processPlayerDelete(players);
                            }
                        }
                        // Handle recording commands
                        case Commands.TRANSPORT_RECORD_START -> {
                            setRecording(true);
                            // Optionally notify UI or start recording-specific behaviors
                            CommandBus.getInstance().publish(Commands.RECORDING_STARTED, this);
                        }
                        case Commands.TRANSPORT_RECORD_STOP -> {
                            setRecording(false);
                            // Optionally finalize recording or perform cleanup
                            CommandBus.getInstance().publish(Commands.RECORDING_STOPPED, this);
                        }
                    }
                } catch (Exception e) {
                    // logger. error("Error processing command {}: {}", cmd, e.getMessage());
                }
            }

            private Object handleSessionSaveRequest() {
                if (activeSession != null) {

                    getActiveSession().getPlayers().forEach(player -> {
                        if (player != null) {
                            redisService.savePlayer(player);
                        }
                    });
                    redisService.saveSession(activeSession);
                    logger.info("Session saved: " + activeSession.getId());
                }
                return null;
            }

            private void processPlayerDelete(Player[] data) {
                for (Player player : data) {
                    player.setEnabled(false);
                    logger.info("Deleting player: " + player.getId());
                    if (getActiveSession().getPlayers().remove(player)) {
                        redisService.deletePlayer(player);
                        logger.info("Player deleted: " + player.getId());
                        commandBus.publish(Commands.PLAYER_DELETED, this);
                    }
                }
            }
        });
    }

    // Moved from SessionManager
    public boolean canMoveBack() {
        Long minId = redisService.getMinimumSessionId();
        return (Objects.nonNull(getActiveSession()) && Objects.nonNull(minId) && getActiveSession().getId() > minId);
    }

    // Moved from SessionManager
    public boolean canMoveForward() {
        return Objects.nonNull(getActiveSession()) && getActiveSession().isValid();
    }

    // Moved from SessionManager
    public void moveBack() {
        Long prevId = redisService.getPreviousSessionId(activeSession);
        if (prevId != null) {
            setActiveSession(redisService.findSessionById(prevId));
            if (activeSession != null) {
                logger.info("Moved back to session: " + activeSession.getId());
            }
        }
    }

    // Moved from SessionManager
    public void moveForward() {
        Long maxId = redisService.getMaximumSessionId();

        if (activeSession != null && maxId != null && activeSession.getId().equals(maxId)) {
            // Only create a new session if current one is valid and has active rules
            if (activeSession.isValid() && !activeSession.getPlayers().isEmpty() &&
                    activeSession.getPlayers().stream()
                            .map(p -> p)
                            .anyMatch(p -> p.getRules() != null && !p.getRules().isEmpty())) {

                Session newSession = redisService.newSession();
                setActiveSession(newSession);
                logger.info("Created new session and moved forward to it: " + newSession.getId());
            }
        } else {
            // Otherwise, move to the next existing session
            Long nextId = redisService.getNextSessionId(activeSession);
            if (nextId != null) {
                setActiveSession(redisService.findSessionById(nextId));
                if (activeSession != null) {
                    logger.info("Moved forward to session: " + activeSession.getId());
                }
            }
        }
    }

    // Moved from SessionManager
    public void loadActiveSession() {
        logger.info("Loading session");
        Long minId = redisService.getMinimumSessionId();
        Long maxId = redisService.getMaximumSessionId();

        logger.info("Minimum session ID: " + minId);
        logger.info("Maximum session ID: " + maxId);

        Session session = null;

        // If we have existing sessions, try to load the first valid one
        if (minId != null && maxId != null) {
            for (Long id = minId; id <= maxId; id++) {
                session = redisService.findSessionById(id);
                if (session != null) {
                    logger.info("Found valid session " + session.getId());
                    break;
                }
            }
        }

        // If no valid session found or no sessions exist, create a new one
        if (session == null) {
            logger.info("No valid session found, creating new session");
            session = redisService.newSession();
            redisService.saveSession(session);
            logger.info("Created new session with ID: " + session.getId());
        }

        setActiveSession(session);
    }

    // Moved from SessionManager
    public void deleteAllSessions() {
        logger.info("Deleting sessions");
        redisService.getAllSessionIds().forEach(id -> {
            Session session = redisService.findSessionById(id);
            if (session != null) {
                logger.info(String.format("Loading session {}", id));
                redisService.deleteSession(id);
                commandBus.publish(Commands.SESSION_DELETED, this, id);
            }
        });
    }

    public void sessionSelected(Session session) {
        setActiveSession(session);
    }

    private void processPlayerEdit(Player player) {
        logger.info("Processing player edit/add: " + player.getName());

        RedisService redis = RedisService.getInstance();

        if (player.getId() == null) {
            redis.savePlayer(player);

            // Add to session
            getActiveSession().getPlayers().add(player);

            // TODO: don't save session here

            redis.saveSession(getActiveSession());

            // Get fresh session state
            // activeSession = redis.findSessionById(activeSession.getId());

            logger.info("Added new player and updated session");
        } else {
            // TODO: don't save player here, just update the active session

            // Existing player update
            redis.savePlayer(player);
            // activeSession = redis.findSessionById(activeSession.getId());
            redis.saveSession(getActiveSession());
            logger.info("Updated existing player and session");
        }

        // Notify UI
        commandBus.publish(Commands.SESSION_UPDATED, this, getActiveSession());
    }

    private void processRuleEdit(Rule data) {
        // Publish rule event
        if (Objects.nonNull(PlayerManager.getInstance().getActivePlayer())) {
            commandBus.publish(Commands.RULE_EDITED, this, PlayerManager.getInstance().getActivePlayer());
        }
    }

    private void logSessionState(Session session) {
        if (session != null) {
            logger.info("Session state:");
            logger.info("  ID: " + session.getId());
            logger.info("  BPM: " + session.getTempoInBPM());
            logger.info("  Ticks per beat: " + session.getTicksPerBeat());
            logger.info("  Beats per bar: " + session.getBeatsPerBar());
            logger.info("  Bars: " + session.getBars());
            logger.info("  Parts: " + session.getParts());

            if (session.getPlayers() != null) {
                logger.info("  Players: " + session.getPlayers().size());
                session.getPlayers().forEach(this::logPlayerState);
            }
        }
    }

    private void logPlayerState(Player player) {
        logger.info("    Player: " + player.getId() + " - " + player.getName());
        if (player.getRules() != null) {
            logger.info("      Rules: " + player.getRules().size());
            player.getRules().forEach(r -> logger.info("        Rule: " + r.getId() +
                    " - Op: " + r.getOperator() +
                    ", Comp: " + r.getComparison() +
                    ", Value: " + r.getValue() +
                    ", Part: " + r.getPart()));
        }
    }

    public void saveSession(Session currentSession) {
        RedisService.getInstance().saveSession(currentSession);
    }

    /**
     * Updates a player in the active session and saves changes to Redis
     * 
     * @param player The player to update
     */
    public void updatePlayer(Player player) {
        if (player != null && activeSession != null) {
            // Update player in active session
            activeSession.updatePlayer(player);

            // Save player to Redis
            RedisService.getInstance().savePlayer(player);

            // Save updated session to Redis
            RedisService.getInstance().saveSession(activeSession);

            // Publish update events
            commandBus.publish(Commands.PLAYER_UPDATED, this, player);
            commandBus.publish(Commands.SESSION_UPDATED, this, activeSession);
        }
    }

    /**
     * Gets the current recording state
     * @return true if recording is active, false otherwise
     */
    public boolean isRecording() {
        return isRecording;
    }

    /**
     * Sets the recording state
     * @param recording the new recording state
     */
    public void setRecording(boolean recording) {
        this.isRecording = recording;
        // logger.debug("Recording state set to: {}", recording);
    }

    private void processPlayerDeleteByIds(Long[] playerIds) {
        if (playerIds == null || playerIds.length == 0 || activeSession == null) {
            return;
        }
        
        logger.info("Processing deletion of " + playerIds.length + " players by ID");
        
        // Track players we find and successfully delete
        int deletedCount = 0;
        
        for (Long playerId : playerIds) {
            // Find player by ID
            Player playerToDelete = null;
            for (Player p : activeSession.getPlayers()) {
                if (p.getId().equals(playerId)) {
                    playerToDelete = p;
                    break;
                }
            }
            
            if (playerToDelete != null) {
                // Mark as disabled
                playerToDelete.setEnabled(false);
                logger.info("Deleting player: " + playerId);
                
                // Remove from session's collection
                if (activeSession.getPlayers().remove(playerToDelete)) {
                    // Delete from Redis
                    redisService.deletePlayer(playerToDelete);
                    logger.info("Player deleted: " + playerId);
                    deletedCount++;
                } else {
                    logger.warning("Failed to remove player " + playerId + " from session");
                }
            } else {
                logger.warning("Player not found for deletion: " + playerId);
            }
        }
        
        // Only publish event if we actually deleted players
        if (deletedCount > 0) {
            // Save the session to persist changes
            redisService.saveSession(activeSession);
            
            // Notify listeners about the deletions
            commandBus.publish(Commands.PLAYER_DELETED, this);
            logger.info("Successfully deleted " + deletedCount + " players");
        }
    }
}
