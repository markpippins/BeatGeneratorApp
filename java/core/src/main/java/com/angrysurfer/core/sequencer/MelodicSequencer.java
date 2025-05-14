package com.angrysurfer.core.sequencer;

import com.angrysurfer.core.api.*;
import com.angrysurfer.core.event.NoteEvent;
import com.angrysurfer.core.event.PatternSwitchEvent;
import com.angrysurfer.core.event.StepUpdateEvent;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Note;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.midi.MidiDevice;
import javax.sound.midi.Receiver;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Getter
@Setter
public class MelodicSequencer implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(MelodicSequencer.class);
    private final javax.sound.midi.ShortMessage reuseableMessage = new javax.sound.midi.ShortMessage();
    private int currentStep = 0; // Current step in the pattern
    private long tickCounter = 0; // Tick counter
    private boolean isPlaying = false; // Flag indicating playback state
    private int bounceDirection = 1; // Direction for bounce mode
    private boolean isLooping = false;
    private Integer currentBar = null;
    private MelodicSequenceData sequenceData = new MelodicSequenceData();
    private Boolean[] scaleNotes; // Computed from root note and scale
    private Quantizer quantizer; // Computed from scale notes
    private Consumer<StepUpdateEvent> stepUpdateListener;
    private Consumer<NoteEvent> noteEventListener;
    private int masterTempo;
    private Integer id;
    private boolean latchEnabled = false;
    private Consumer<NoteEvent> noteEventPublisher;
    private Long nextPatternId = null;
    private long lastNoteTriggeredTime = 0;
    private Player player;
    private int currentTilt = 0;
    private boolean currentlyMuted = false;

    public MelodicSequencer(Integer id) {

        setId(id);
        //setChannel(SEQUENCER_CHANNELS[id]);
        initializePlayer(SequencerConstants.SEQUENCER_CHANNELS[id]);
        CommandBus.getInstance().register(this);
        TimingBus.getInstance().register(this);

        updateQuantizer();

        logger.info("MelodicSequencer initialized and registered with CommandBus");
    }

    public MelodicSequenceData getSequenceData() {
        return sequenceData;
    }

    public void setSequenceData(MelodicSequenceData data) {
        this.sequenceData = data;
        updateQuantizer();
    }

    public void updateQuantizer() {
        scaleNotes = sequenceData.createScaleArray(sequenceData.getRootNote(), sequenceData.getScale());
        quantizer = new Quantizer(scaleNotes);

        logger.info("Quantizer updated with root note {} and scale {}",
                sequenceData.getRootNote(), sequenceData.getScale());
    }

    public void incrementOctaveShift() {
        sequenceData.setOctaveShift(sequenceData.getOctaveShift() + 1);
        logger.info("Octave shift increased to {}", sequenceData.getOctaveShift());
    }

    public void decrementOctaveShift() {
        sequenceData.setOctaveShift(sequenceData.getOctaveShift() - 1);
        logger.info("Octave shift decreased to {}", sequenceData.getOctaveShift());
    }

    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate, int probability, int nudge) {
        sequenceData.setStepData(stepIndex, active, note, velocity, gate, probability, nudge);
    }

    public void setStepData(int stepIndex, boolean active, int note, int velocity, int gate) {
        sequenceData.setStepData(stepIndex, active, note, velocity, gate);
    }

    private void initializePlayer(Player player) {
        PlayerManager.getInstance().applyInstrumentPreset(player);

        // Add this explicit program change to ensure the preset is applied:
        if (player != null && player.getInstrument() != null) {
            try {
                // Force program change through both regular channel and direct MIDI
                InstrumentWrapper instrument = player.getInstrument();
                int channel = player.getChannel();
                int bankIndex = instrument.getBankIndex() != null ? instrument.getBankIndex() : 0;
                int preset = instrument.getPreset() != null ? instrument.getPreset() : 0;

                // Send explicit bank select and program change
                instrument.controlChange(0, (bankIndex >> 7) & 0x7F);  // Bank MSB
                instrument.controlChange(32, bankIndex & 0x7F);        // Bank LSB
                instrument.programChange(preset, 0);

                logger.info("Explicitly set instrument {} to bank {} program {} on channel {}",
                        instrument.getName(), bankIndex, preset, channel);
            } catch (Exception e) {
                logger.error("Error applying program change: {}", e.getMessage(), e);
            }
        }
    }

    public void start() {
        if (isPlaying) {
            return;
        }

        if (getHarmonicTiltValues() == null || getHarmonicTiltValues().isEmpty()) {
            logger.warn("No harmonic tilt values set, using default 0");
            setHarmonicTiltValues(Collections.singletonList(0));
        }
        currentTilt = getHarmonicTiltValues().get(0);
        if (currentStep >= getSequenceData().getPatternLength()) {
            reset();
        }

        initializePlayer(player);


        isPlaying = true;
        logger.info("Melodic sequencer {} started playback", id);
        CommandBus.getInstance().publish(Commands.SEQUENCER_STATE_CHANGED, this,
                Map.of("sequencerId", id, "state", "started"));
    }

    public void stop() {
        if (isPlaying) {
            isPlaying = false;
            logger.info("Melodic sequencer playback stopped");
        }
    }

    public void ensurePlayerHasInstrument() {
        if (player != null && player.getInstrument() == null) {
            logger.warn("Player {} has no instrument, initializing default", SequencerConstants.SEQUENCER_CHANNELS[id]);
            PlayerManager.getInstance().initializeInternalInstrument(player, true, player.getId().intValue());
        }
    }

    public void processTick(Long tick) {
        if (!isPlaying || tick == null) {
            return;
        }

        tickCounter = tick;

        int ticksForDivision = sequenceData.getTimingDivision().getTicksPerBeat();

        if (ticksForDivision <= 0) {
            ticksForDivision = 24; // Emergency fallback
        }

        if (tick % ticksForDivision == 0) {
            int prevStep = currentStep;

            calculateNextStep();

            if (stepUpdateListener != null) {
                stepUpdateListener.accept(new StepUpdateEvent(prevStep, currentStep));
            }

            triggerNote(currentStep);
        }
    }

    private void calculateNextStep() {
        int oldStep = currentStep;
        boolean patternCompleted = false;

        switch (sequenceData.getDirection()) {
            case FORWARD -> {
                currentStep++;

                // Check if we've reached the end of the pattern
                if (currentStep >= sequenceData.getPatternLength()) {
                    currentStep = 0;
                    patternCompleted = true;

                    // Handle pattern switching if enabled
                    handlePatternCompletion();

                    if (!sequenceData.isLooping()) {
                        isPlaying = false;
                    }
                }
            }

            case BACKWARD -> {
                currentStep--;

                if (currentStep < 0) {
                    currentStep = sequenceData.getPatternLength() - 1;
                    patternCompleted = true;

                    handlePatternCompletion();

                    if (!sequenceData.isLooping()) {
                        isPlaying = false;
                    }
                }
            }

            case BOUNCE -> {
                currentStep += bounceDirection;

                if (currentStep <= 0 || currentStep >= sequenceData.getPatternLength() - 1) {
                    bounceDirection *= -1;

                    if (currentStep <= 0 || currentStep >= sequenceData.getPatternLength() - 1) {
                        handlePatternCompletion();
                    }

                    if (!sequenceData.isLooping()) {
                        isPlaying = false;
                    }
                }
            }

            case RANDOM -> {
                int priorStep = currentStep;

                currentStep = (int) (Math.random() * sequenceData.getPatternLength());

                if (currentStep == 0 && priorStep != 0) {
                    handlePatternCompletion();
                }
            }
        }

        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(oldStep, currentStep));
        }
    }

    private void handlePatternCompletion() {
        if (latchEnabled) {
            int octaveRange = 2;
            int density = 50;
            generatePattern(octaveRange, density);
            logger.info("Latch mode: Generated new pattern at cycle end");
        }

        if (nextPatternId != null) {
            Long currentId = sequenceData.getId();
            if (RedisService.getInstance().findMelodicSequenceById(nextPatternId, id) != null) {
                boolean wasPlaying = isPlaying;

                RedisService.getInstance().applyMelodicSequenceToSequencer(
                        RedisService.getInstance().findMelodicSequenceById(nextPatternId, id),
                        this);

                isPlaying = wasPlaying;

                CommandBus.getInstance().publish(
                        Commands.MELODIC_PATTERN_SWITCHED,
                        this,
                        new PatternSwitchEvent(currentId, nextPatternId));

                nextPatternId = null;
            }
        }
    }

    public int quantizeNote(int noteValue) {
        if (!sequenceData.isQuantizeEnabled() || quantizer == null) {
            return noteValue;
        }

        // Apply octave shift
        int octaveOffset = sequenceData.getOctaveShift() * 12;
        int shiftedNote = noteValue + octaveOffset;

        // Apply quantization
        int quantizedNote = quantizer.quantizeNote(shiftedNote);

        logger.debug("Quantized note {} to {} (with octave shift {})",
                noteValue, quantizedNote, sequenceData.getOctaveShift());

        return quantizedNote;
    }

    public void reset() {
        isPlaying = false;

        currentStep = 0;

        tickCounter = 0;

        bounceDirection = 1;

        if (player != null && player.getInstrument() != null) {
            try {
                logger.debug("All notes off sent during reset");
            } catch (Exception e) {
                logger.error("Error sending all notes off: {}", e.getMessage(), e);
            }
        }

        if (stepUpdateListener != null) {
            stepUpdateListener.accept(new StepUpdateEvent(-1, currentStep));
        }

        logger.info("Sequencer reset");
    }

    public void triggerNote(int stepIndex) {
        // Skip if not playing or player not available
        if (!isPlaying || player == null) {
            return;
        }

        // Check if player is muted - immediately return if so
        if (player.getLevel() <= 0) {
            logger.debug("Player is muted (level: {}), skipping note trigger", player.getLevel());
            return;
        }

        // Check if step is active
        boolean stepActive = sequenceData.isStepActive(stepIndex);
        if (!stepActive) {
            return;
        }

        // Check probability
        int probability = sequenceData.getProbabilityValue(stepIndex);
        if (probability < 100) {
            int rand = (int) (Math.random() * 100);

            if (rand >= probability) {
                logger.debug("Step {} skipped due to probability ({} < {})",
                        stepIndex, rand, probability);
                return;
            }
        }

        int noteValue = sequenceData.getNoteValue(stepIndex);
        int velocity = sequenceData.getVelocityValue(stepIndex);
        int gate = sequenceData.getGateValue(stepIndex);

        if (sequenceData.isQuantizeEnabled()) {
            noteValue = quantizeNote(noteValue);
        }

        noteValue = noteValue + currentTilt;

        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastNoteTriggeredTime < SequencerConstants.MIN_NOTE_INTERVAL_MS) {
                logger.debug("Ignoring note trigger - too soon after last note ({} ms)",
                        currentTime - lastNoteTriggeredTime);
                return;
            }
            lastNoteTriggeredTime = currentTime;

            int noteOnVelocity = velocity;

            NoteEvent event = new NoteEvent(
                    noteValue,
                    noteOnVelocity,
                    gate
            );

            if (player != null) {
                player.noteOn(noteValue, noteOnVelocity, gate);
            }

            if (noteEventListener != null) {
                noteEventListener.accept(event);
            }

            CommandBus.getInstance().publish(
                    Commands.MELODIC_NOTE_TRIGGERED,
                    this,
                    event);

            logger.debug("Triggered step {} - note:{} vel:{} gate:{} tilt:{}",
                    stepIndex, noteValue, velocity, gate, currentTilt);

        } catch (Exception e) {
            logger.error("Error triggering note: {}", e.getMessage(), e);
        }
    }

    public void notifyPatternUpdated() {
        CommandBus.getInstance().publish(
                Commands.MELODIC_PATTERN_UPDATED,
                this,
                sequenceData);

        logger.debug("Pattern updated notification sent");
    }

    public void updateFromData(MelodicSequenceData sequenceData) {
        if (sequenceData == null) {
            logger.warn("Cannot update from null sequence data");
            return;
        }

        boolean wasPlaying = isPlaying;

        this.sequenceData = sequenceData;

        updateQuantizer();

        currentStep = 0;

        isPlaying = wasPlaying;

        logger.info("Sequencer updated from sequence data (ID: {})",
                sequenceData.getId());
    }

    private void initializePlayer(int playerChannel) {
        Session session = SessionManager.getInstance().getActiveSession();
        if (session == null) {
            logger.error("Cannot initialize player - no active session");
            return;
        }

        List<Note> ordered = UserConfigManager.getInstance().getNotesOrderedById();
        List<Note> notes = UserConfigManager.getInstance().getCurrentConfig().getDefaultNotes();

        Optional<Note> opt = notes.stream().filter(p -> p.getChannel().equals(playerChannel)).findFirst();

        if (opt.isPresent()) {

            player = opt.get();
            logger.info("Using existing player {} for sequencer {}", player.getId(), id);
            // if (player.getChannel() != playerChannel) {
            player.setDefaultChannel(playerChannel);
            player.setOwner(this);
            player.setMelodicPlayer(true);
            //  PlayerManager.getInstance().savePlayerProperties(player);
            //}
        } else {
            logger.info("Creating new player for melodic sequencer {}", id);
            // player = RedisService.getInstance().newNote();
            player = new Note();
            player.setId(RedisService.getInstance().getPlayerHelper().getNextPlayerId());
            player.setRules(new HashSet<>()); // Ensure rules are initialized
            player.setMinVelocity(60);
            player.setMaxVelocity(127);
            player.setLevel(100);
            player.setIsDefault(true);
            player.setName("Melo " + getId() + 1);
            player.setDefaultChannel(playerChannel);
        }

        //InstrumentWrapper instrument = UserConfigManager.getInstance().getCurrentConfig().getInstruments()
        //        .stream().filter(InstrumentWrapper::getIsDefault).toList().get(getId());


//        if (player.getInstrument() != null) {
//            InternalSynthManager.getInstance().initializeInstrumentState(player.getInstrument());
//        } else {
//            logger.warn("Could not create instrument for melodic sequencer {}", id);
//        }

        if (player.getInstrument() != null) {
            DeviceManager.getInstance();
            MidiDevice device = DeviceManager.getMidiDevice(player.getInstrument().getDeviceName());
            if (device != null) {
                player.getInstrument().setDevice(device);
                player.getInstrument().setAssignedToPlayer(true);
            } else PlayerManager.getInstance().initializeInternalInstrument(player, true, player.getId().intValue());
        }

        if (sequenceData != null) {
            applySequenceDataToInstrument();
        }

        initializePlayer(player);
        player.noteOn(player.getRootNote(), 100);

        session.getPlayers().add(player);
        PlayerManager.getInstance().savePlayerProperties(player);
        SessionManager.getInstance().saveSession(session);
        logger.info("Added new player to session {}: {}", session.getId(), player.getId());
    }

    private void applySequenceDataToInstrument() {
        if (sequenceData == null || player == null || player.getInstrument() == null) {
            return;
        }

        InstrumentWrapper instrument = player.getInstrument();
        if (!instrument.isInternalSynth() && instrument.getChannel() != SequencerConstants.MIDI_DRUM_CHANNEL) {

            if (sequenceData.getSoundbankName() != null) {
                instrument.setSoundbankName(sequenceData.getSoundbankName());
            }

            if (sequenceData.getBankIndex() != null) {
                instrument.setBankIndex(sequenceData.getBankIndex());
            }

            if (sequenceData.getPreset() != null) {
                instrument.setPreset(sequenceData.getPreset());
            }

            PlayerManager.getInstance().applyInstrumentPreset(player);
        }

        logger.debug("Applied sequence data settings to instrument: preset:{}, bank:{}, soundbank:{}",
                instrument.getPreset(), instrument.getBankIndex(), instrument.getSoundbankName());
    }

    /**
     * Find an existing player for the sequencer in the session.
     *
     * @param session The session to search in.
     * @return The existing player, or null if not found.
     */
    private Player findExistingPlayerForSequencer(Session session) {
        if (session == null || id == null) {
            return null;
        }

        for (Player p : session.getPlayers()) {
            if (p instanceof Note &&
                    p.getOwner() != null &&
                    p.getOwner() instanceof MelodicSequencer &&
                    ((MelodicSequencer) p.getOwner()).getId() != null &&
                    ((MelodicSequencer) p.getOwner()).getId().equals(id) &&
                    p.getChannel() == SequencerConstants.SEQUENCER_CHANNELS[id]) {
                // p.noteOn(p.getRootNote(), 100);
                return p;
            }
        }

        logger.info("No player found for sequencer {} and channel {}", id, SequencerConstants.SEQUENCER_CHANNELS[id]);
        return null;
    }

    /**
     * Generate a random pattern
     *
     * @param octaveRange The number of octaves to use (1-4)
     * @param density     The note density (0-100)
     * @return true if pattern was generated successfully
     */
    public boolean generatePattern(int octaveRange, int density) {
        try {
            if (sequenceData == null) {
                logger.error("Cannot generate pattern - sequence data is null");
                return false;
            }

            // Validate parameters
            if (octaveRange < 1) octaveRange = 1;
            if (octaveRange > 4) octaveRange = 4;
            if (density < 0) density = 0;
            if (density > 100) density = 100;

            logger.info("Generating pattern with octave range: {}, density: {}", octaveRange, density);

            // Clear current pattern
            for (int step = 0; step < sequenceData.getMaxSteps(); step++) {
                sequenceData.setStepActive(step, false);
                sequenceData.setNoteValue(step, 60); // Middle C default
                sequenceData.setVelocityValue(step, 64); // Medium velocity
                sequenceData.setGateValue(step, 75); // Medium gate
                sequenceData.setProbabilityValue(step, 100); // Full probability
            }

            // Calculate note range based on octave selection
            int baseNote = 60 - (12 * (octaveRange / 2)); // Center around middle C
            int noteRange = 12 * octaveRange;

            // Determine active steps based on density
            int stepsToActivate = (int) Math.round(sequenceData.getMaxSteps() * (density / 100.0));

            // Ensure we have at least one step if density > 0
            if (density > 0 && stepsToActivate == 0) {
                stepsToActivate = 1;
            }

            logger.debug("Will activate {} steps out of {}", stepsToActivate, sequenceData.getMaxSteps());

            // Generate steps
            java.util.Random random = new java.util.Random();
            for (int i = 0; i < stepsToActivate; i++) {
                // Choose a random step that's not already active
                int step;
                int attempts = 0;
                do {
                    step = random.nextInt(sequenceData.getMaxSteps());
                    attempts++;
                    // Prevent infinite loops
                    if (attempts > 100) break;
                } while (sequenceData.isStepActive(step) && attempts < 100);

                // Activate the step
                sequenceData.setStepActive(step, true);

                // Generate a random note in the selected range
                int noteOffset = random.nextInt(noteRange);
                int note = baseNote + noteOffset;

                // Quantize to scale if enabled
                if (sequenceData.isQuantizeEnabled() && quantizer != null) {
                    note = quantizeNote(note);
                }

                // Set the note
                sequenceData.setNoteValue(step, note);

                // Random velocity between 70-100
                int velocity = 70 + random.nextInt(31);
                sequenceData.setVelocityValue(step, velocity);

                // Random gate between 50-100
                int gate = 50 + random.nextInt(51);
                sequenceData.setGateValue(step, gate);

                // Sometimes add randomized probability
                if (random.nextDouble() < 0.3) { // 30% chance of partial probability
                    int probability = 50 + random.nextInt(51); // 50-100
                    sequenceData.setProbabilityValue(step, probability);
                }
            }

            // Generate harmonic tilt values
//            int[] tiltValues = new int[sequenceData.getMaxSteps()];
//            for (int i = 0; i < sequenceData.getMaxSteps(); i++) {
//                // Create small tilt values (-3 to +3)
//                tiltValues[i] = random.nextInt(7) - 3;
//            }
//            sequenceData.setHarmonicTiltValues(tiltValues);
//
//            // Update the internal harmonic tilt values list if it exists
//            if (harmonicTiltValues != null) {
//                harmonicTiltValues.clear();
//                for (int i = 0; i < sequenceData.getMaxSteps(); i++) {
//                    harmonicTiltValues.add(sequenceData.getTiltValue(i));
//                }
//            }

            logger.info("Successfully generated new pattern with {} active steps", stepsToActivate);
            return true;
        } catch (Exception e) {
            logger.error("Error generating pattern: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public void onAction(Command action) {
        if (action == null) {
            return;
        }

        if (Commands.REPAIR_MIDI_CONNECTIONS.equals(action.getCommand())) {
            repairMidiConnections();
            return;
        }

        switch (action.getCommand()) {
            case Commands.TIMING_UPDATE -> {
                if (isPlaying && action.getData() instanceof TimingUpdate update) {
                    if (update.tick() != null) {
                        processTick(update.tick());
                    }

                    if (update.bar() != null) {
                        int newBar = update.bar() - 1; // Adjust for 0-based index

                        // Only process if bar actually changed
                        if (currentBar == null || newBar != currentBar) {
                            currentBar = newBar;
                            logger.debug("Current bar updated to {}", currentBar);

                            // Process tilt values
                            if (getHarmonicTiltValues() != null && getHarmonicTiltValues().size() > currentBar) {
                                currentTilt = getHarmonicTiltValues().get(currentBar);
                                logger.debug("Current tilt value for bar {}: {}", currentBar, currentTilt);
                            }

                            // Process mute values
                            if (sequenceData.getMuteValues() != null && sequenceData.getMuteValues().size() > currentBar) {
                                int muteValue = sequenceData.getMuteValue(currentBar);
                                boolean shouldMute = muteValue > 0;

                                // Only update if mute state changes
                                if (shouldMute != currentlyMuted) {
                                    currentlyMuted = shouldMute;

                                    // Apply mute state if player exists
                                    if (player != null) {
                                        int originalLevel = player.getOriginalLevel() > 0 ?
                                                player.getOriginalLevel() : 100;

                                        player.setLevel(shouldMute ? 0 : originalLevel);

                                        logger.debug("Bar {}: Player {} {}",
                                                currentBar, player.getName(),
                                                shouldMute ? "muted" : "unmuted");

                                        // Notify system of level change
                                        CommandBus.getInstance().publish(
                                                Commands.PLAYER_UPDATED,
                                                this,
                                                player);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            case Commands.TRANSPORT_START -> {
                logger.info("Received TRANSPORT_START command");
                masterTempo = SessionManager.getInstance().getActiveSession().getTicksPerBeat();
                logger.info("Master tempo set to {} ticks per beat", masterTempo);
                start();
            }

            case Commands.TRANSPORT_STOP -> {
                logger.info("Received TRANSPORT_STOP command");
                stop();
            }

            case Commands.REFRESH_ALL_INSTRUMENTS -> {
                logger.info("Refreshing instrument preset for melodic sequencer {}", id);
                if (player != null && player.getInstrument() != null) {
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                }
            }

            case Commands.REFRESH_PLAYER_INSTRUMENT -> {
                if (action.getData() instanceof Long playerId &&
                        player != null &&
                        player.getId().equals(playerId)) {

                    logger.info("Explicit refresh requested for player instrument: {}", playerId);
                    PlayerManager.getInstance().applyInstrumentPreset(player);
                }
            }
        }
    }

    public List<Integer> getHarmonicTiltValues() {
        if (sequenceData == null) {
            logger.error("sequenceData is null in getHarmonicTiltValues()");
            return new ArrayList<>();
        }

        int[] rawValues = sequenceData.getHarmonicTiltValuesRaw();
        if (rawValues == null) {
            logger.error("Raw harmonic tilt values array is null in sequencer");
            return new ArrayList<>();
        }

        List<Integer> result = Arrays.stream(rawValues).boxed().collect(Collectors.toList());
        logger.debug("getHarmonicTiltValues(): returning {} values from raw array of length {}",
                result.size(), rawValues.length);

        return result;
    }

    public void setHarmonicTiltValues(List<Integer> tiltValues) {
        if (tiltValues == null || tiltValues.isEmpty()) {
            logger.warn("Attempted to set null or empty tilt values");
            return;
        }

        int[] tiltArray = new int[Math.max(sequenceData.getPatternLength(), tiltValues.size())];

        for (int i = 0; i < tiltValues.size(); i++) {
            tiltArray[i] = tiltValues.get(i);
        }

        sequenceData.setHarmonicTiltValues(tiltArray);

        logger.info("Set {} harmonic tilt values in sequencer", tiltValues.size());
    }

    public List<Integer> getMuteValues() {
        if (sequenceData == null) {
            logger.error("sequenceData is null in getMuteValues()");
            return new ArrayList<>();
        }

        return sequenceData.getMuteValues();
    }

    /**
     * Set mute values from a list
     */
    public void setMuteValues(List<Integer> muteValues) {
        if (sequenceData == null || muteValues == null) {
            logger.error("Cannot set mute values: sequenceData or muteValues is null");
            return;
        }

        int[] muteArray = new int[Math.max(sequenceData.getPatternLength(), muteValues.size())];
        for (int i = 0; i < muteValues.size(); i++) {
            muteArray[i] = muteValues.get(i);
        }

        sequenceData.setMuteValues(muteArray);
        logger.info("Set {} mute values in sequencer", muteValues.size());
    }

    public void repairMidiConnections() {
        logger.info("Attempting to repair MIDI connections for melodic sequencer {}", id);

        try {
            if (player == null) {
                logger.warn("No player for sequencer {}, creating", id);
                initializePlayer(SequencerConstants.SEQUENCER_CHANNELS[id]);
                return;
            }

            if (player.getInstrument() == null) {
                logger.warn("Player has no instrument, initializing...");
                PlayerManager.getInstance().initializeInternalInstrument(player, true, player.getId().intValue());
            }

            InstrumentWrapper instrument = player.getInstrument();
            if (instrument == null) {
                logger.error("Failed to initialize instrument");
                return;
            }

            String deviceName = instrument.getDeviceName();
            if (deviceName == null || deviceName.isEmpty()) {
                deviceName = SequencerConstants.GERVILL; // Default to Gervill
                instrument.setDeviceName(deviceName);
            }

            MidiDevice device = DeviceManager.getMidiDevice(deviceName);
            if (device == null) {
                device = DeviceManager.getInstance().getDefaultOutputDevice();
                if (device != null) {
                    deviceName = device.getDeviceInfo().getName();
                    instrument.setDeviceName(deviceName);
                }
            }

            if (device != null) {
                if (!device.isOpen()) {
                    device.open();
                }
                instrument.setDevice(device);

                Receiver receiver = ReceiverManager.getInstance().getOrCreateReceiver(deviceName, device);
                if (receiver != null) {
                    logger.info("Successfully reconnected sequencer {} to device {}", id, deviceName);

                    PlayerManager.getInstance().applyInstrumentPreset(player);
                } else {
                    logger.warn("Failed to get receiver for sequencer {}", id);
                }
            } else {
                logger.warn("Could not get device for sequencer {}", id);
            }

            PlayerManager.getInstance().savePlayerProperties(player);
        } catch (Exception e) {
            logger.error("Error repairing melodic sequencer {}: {}", id, e.getMessage());
        }
    }

    public void updateInstrumentSettingsInSequenceData() {
        if (player != null && player.getInstrument() != null) {
            InstrumentWrapper instrument = player.getInstrument();

            sequenceData.setSoundbankName(instrument.getSoundbankName());
            sequenceData.setBankIndex(instrument.getBankIndex());
            sequenceData.setPreset(instrument.getPreset());
            sequenceData.setDeviceName(instrument.getDeviceName());
            sequenceData.setInstrumentId(instrument.getId());
            sequenceData.setInstrumentName(instrument.getName());

            logger.debug("Updated sequence data with instrument settings - preset:{}, bank:{}, soundbank:{}",
                    instrument.getPreset(), instrument.getBankIndex(), instrument.getSoundbankName());
        }
    }
}

