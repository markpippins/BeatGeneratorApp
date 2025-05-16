package com.angrysurfer.core.service;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service to handle playback of sample-based instruments
 */
public class SamplePlaybackService implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(SamplePlaybackService.class);
    private static SamplePlaybackService instance;

    private final ExecutorService executor = Executors.newFixedThreadPool(4);
    private final Map<String, Clip> activeClips = new HashMap<>();

    private SamplePlaybackService() {
        // Register with command bus for note events
        CommandBus.getInstance().register(this, new String[]{
                Commands.TRANSPORT_STOP,
                Commands.ALL_NOTES_OFF,
                Commands.PLAYER_DELETED,
                Commands.TIMING_UPDATE
        });
    }

    public static synchronized SamplePlaybackService getInstance() {
        if (instance == null) {
            instance = new SamplePlaybackService();
        }
        return instance;
    }

    /**
     * Play a sample-based note
     */
    public void playSampleNote(Player player, int velocity, int duration) {
        if (player == null || player.getInstrument() == null) {
            return;
        }

        InstrumentWrapper instrument = player.getInstrument();
        Map<String, Object> properties = instrument.getProperties();

        // Check if this is a sample-based instrument
        if (!"sample".equals(properties.get("type"))) {
            return;
        }

        // Get sample data
        byte[] sampleData = (byte[]) properties.get("sampleData");
        AudioFormat format = (AudioFormat) properties.get("sampleFormat");

        if (sampleData == null || format == null) {
            logger.warn("Missing sample data or format for instrument: {}", instrument.getName());
            return;
        }

        // Calculate start/end frames based on player properties
        Map<String, Object> playerProps = player.getProperties();
        int startFrame;
        int endFrame = sampleData.length / format.getFrameSize();

        if (playerProps.containsKey("sampleStart")) {
            startFrame = (int) playerProps.get("sampleStart");
        } else {
            startFrame = 0;
        }

        if (playerProps.containsKey("sampleEnd")) {
            endFrame = (int) playerProps.get("sampleEnd");
        }

        // Calculate start/end positions in bytes
        int startPos = startFrame * format.getFrameSize();
        int endPos = endFrame * format.getFrameSize();
        int length = endPos - startPos;

        // Ensure valid range
        if (startPos < 0) startPos = 0;
        if (endPos > sampleData.length) endPos = sampleData.length;
        if (length <= 0) {
            logger.warn("Invalid sample length: {}", length);
            return;
        }

        // Extract the desired portion of the sample
        byte[] playData = new byte[length];
        System.arraycopy(sampleData, startPos, playData, 0, length);

        // Store player ID outside the lambda for use in the listener
        final String playerIdString = player.getId().toString();

        // Play the sample asynchronously
        int finalEndFrame = endFrame;
        executor.submit(() -> {
            try {
                // Create audio stream from sample data
                ByteArrayInputStream bais = new ByteArrayInputStream(playData);
                AudioInputStream ais = new AudioInputStream(bais, format, length / format.getFrameSize());

                // Get a clip to play the sample
                Clip clip = AudioSystem.getClip();
                clip.open(ais);

                // Scale volume based on velocity
                float volume = (float) velocity / 127.0f;
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = (float) (Math.log10(volume) * 20.0f);
                gainControl.setValue(dB);

                // Create a final clipId for use in the listener
                final String clipId = playerIdString + "_" + System.currentTimeMillis();

                synchronized (activeClips) {
                    activeClips.put(clipId, clip);
                }

                // Add listener to remove from map when done
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        synchronized (activeClips) {
                            activeClips.remove(clipId); // Now using final clipId
                        }
                        clip.close();
                    }
                });

                // Check for looping
                boolean loopEnabled = (boolean) playerProps.getOrDefault("loopEnabled", false);
                if (loopEnabled) {
                    // Create final copies of loop points for use in conditional logic
                    final int loopStartPos = (int) playerProps.getOrDefault("loopStart", 0) - startFrame;
                    final int loopEndPos = (int) playerProps.getOrDefault("loopEnd", finalEndFrame) - startFrame;
                    final int sampleLength = length / format.getFrameSize();

                    if (loopStartPos >= 0 && loopEndPos <= sampleLength && loopStartPos < loopEndPos) {
                        clip.setLoopPoints(loopStartPos, loopEndPos);
                        clip.loop(Clip.LOOP_CONTINUOUSLY);
                    } else {
                        clip.start();
                    }
                } else {
                    clip.start();
                }

                // If duration is specified, schedule a stop
                if (duration > 0) {
                    Thread.sleep(duration);
                    clip.stop();
                }

            } catch (Exception e) {
                logger.error("Error playing sample: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Stop all active sample playback
     */
    public void stopAllSamples() {
        synchronized (activeClips) {
            for (Clip clip : activeClips.values()) {
                try {
                    clip.stop();
                    clip.close();
                } catch (Exception e) {
                    logger.warn("Error stopping clip: {}", e.getMessage());
                }
            }
            activeClips.clear();
        }
    }

    @Override
    public void onAction(Command action) {
        if (action.getCommand() == null) return;

        switch (action.getCommand()) {
            case Commands.TRANSPORT_STOP:
            case Commands.ALL_NOTES_OFF:
                stopAllSamples();
                break;

            case Commands.PLAYER_DELETED:
                if (action.getData() instanceof Player player) {
                    // Stop any samples from this player
                    String playerId = player.getId().toString();
                    synchronized (activeClips) {
                        for (String clipId : new HashMap<>(activeClips).keySet()) {
                            if (clipId.startsWith(playerId + "_")) {
                                Clip clip = activeClips.get(clipId);
                                clip.stop();
                                clip.close();
                                activeClips.remove(clipId);
                            }
                        }
                    }
                }
                break;
        }
    }

    /**
     * Clean up resources
     */
    public void shutdown() {
        stopAllSamples();
        executor.shutdown();
    }
}