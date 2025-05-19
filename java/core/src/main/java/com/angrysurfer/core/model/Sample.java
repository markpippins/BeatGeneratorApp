package com.angrysurfer.core.model;

import com.angrysurfer.core.api.Command;
import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.Commands;
import com.angrysurfer.core.api.IBusListener;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Setter
public class Sample implements IBusListener {

    private static final Logger logger = LoggerFactory.getLogger(Sample.class);

    private static final float DEFAULT_SAMPLE_RATE = 44100.0f;
    Long id;
    // Store active clips for note control
    private Map<Integer, ActiveSample> activeClips = new ConcurrentHashMap<>();
    private boolean started = false;
    private File audioFile;
    private Clip audioClip;
    private AudioFormat audioFormat;
    private byte[] audioData;
    private int[] waveformData;
    private int sampleRate;
    private int channels;
    private int sampleSizeInBits;

    // Playback controls
    private boolean loopEnabled = false;
    private boolean reverseEnabled = false;
    private int fadeInDuration = 0;
    private int fadeOutDuration = 0;
    private boolean normalizeEnabled = false;

    // Selection points (in frames)
    private int sampleStart = 0;
    private int sampleEnd = 0;
    private int loopStart = 0;
    private int loopEnd = 0;

    private boolean fadeInEnabled;
    private boolean fadeOutEnabled;

    // Auto processing options
    private boolean autoTrimEnabled = false;
    private boolean autoGainEnabled = false;
    private boolean autoPitchEnabled = true; // Default to pitch shifting enabled
    private boolean autoTempoEnabled = false;
    private boolean autoReverseEnabled = false;
    private boolean autoLoopEnabled = false;
    private boolean autoFadeInEnabled = false;
    private boolean autoFadeOutEnabled = false;
    private boolean autoNormalizeEnabled = false;

    // Reference note (plays original sample)
    private int referenceNote = 60; // Middle C

    private String name;
    private Float duration;

    @JsonIgnore
    private boolean playing;

    /**
     * Default constructor
     */
    public Sample() {
        super();
    }

    /**
     * Constructor with audio file
     *
     * @param name      Sample name
     * @param audioFile Audio file to load
     */
    public Sample(String name, File audioFile) {
        super();
        setName(name);
        setAudioFile(audioFile);
        loadAudioFile(audioFile);
    }

    /**
     * Load audio data from a file
     *
     * @param file The WAV file to load
     * @return true if successful, false otherwise
     */
    public boolean loadAudioFile(File file) {
        if (file == null || !file.exists()) {
            logger.error("Audio file is null or doesn't exist");
            return false;
        }

        try {
            // Load the audio file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);
            audioFormat = audioInputStream.getFormat();

            // Store audio format properties
            sampleRate = (int) audioFormat.getSampleRate();
            channels = audioFormat.getChannels();
            sampleSizeInBits = audioFormat.getSampleSizeInBits();

            // Calculate duration in seconds
            long frameLength = audioInputStream.getFrameLength();
            setDuration(frameLength / audioFormat.getFrameRate());

            // Read the entire audio data into a byte array
            audioData = new byte[(int) (frameLength * audioFormat.getFrameSize())];
            audioInputStream.read(audioData);

            // Generate waveform data for visualization (downsampled)
            generateWaveformData();

            // Set end frame to full length by default
            sampleEnd = (int) frameLength;

            // Store the audio file reference
            this.audioFile = file;

            // Set the name if not already set
            if (getName() == null || getName().isEmpty()) {
                setName(file.getName().replace(".wav", ""));
            }

            logger.info("Loaded audio file: {}, duration: {:.2f}s, rate: {}Hz, channels: {}",
                    file.getName(), getDuration(), sampleRate, channels);

            return true;

        } catch (UnsupportedAudioFileException e) {
            logger.error("Unsupported audio format: {}", e.getMessage());
        } catch (IOException e) {
            logger.error("Error reading audio file: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error loading audio file: {}", e.getMessage());
        }

        return false;
    }

    /**
     * Generate downsampled waveform data for visualization
     */
    private void generateWaveformData() {
        if (audioData == null || audioFormat == null) {
            return;
        }

        // Create a downsampled representation for visualization
        // Aim for around 1000 points for the waveform
        int frameSize = audioFormat.getFrameSize();
        int totalFrames = audioData.length / frameSize;
        int pointCount = Math.min(totalFrames, 1000);
        int samplesPerPoint = totalFrames / pointCount;

        waveformData = new int[pointCount];

        for (int i = 0; i < pointCount; i++) {
            int startFrame = i * samplesPerPoint;
            int endFrame = Math.min((i + 1) * samplesPerPoint, totalFrames);

            // Find peak amplitude in this segment
            int maxAmplitude = 0;

            for (int frame = startFrame; frame < endFrame; frame++) {
                int sampleIndex = frame * frameSize;

                // Handle different bit depths and formats
                int amplitude = 0;
                if (audioFormat.getSampleSizeInBits() == 16) {
                    // 16-bit samples
                    if (audioFormat.isBigEndian()) {
                        amplitude = ((audioData[sampleIndex] & 0xFF) << 8) |
                                (audioData[sampleIndex + 1] & 0xFF);
                    } else {
                        amplitude = ((audioData[sampleIndex + 1] & 0xFF) << 8) |
                                (audioData[sampleIndex] & 0xFF);
                    }
                    // Convert signed value to absolute
                    amplitude = Math.abs(amplitude);
                } else if (audioFormat.getSampleSizeInBits() == 8) {
                    // 8-bit samples are typically unsigned (0-255)
                    amplitude = Math.abs(audioData[sampleIndex] - 128);
                }

                // Track maximum amplitude
                maxAmplitude = Math.max(maxAmplitude, amplitude);
            }

            waveformData[i] = maxAmplitude;
        }
    }

//    @Override
//    public void onTick(TimingUpdate timingUpdate) {
//        // Check if we should play based on timing rules
//        boolean shouldPlayResult = shouldPlay(timingUpdate);
//
//        if (shouldPlayResult) {
//            try {
//                // Get the note from root note (may be modified by scale/rules)
//                int noteToPlay = getRootNote() + (Objects.nonNull(getSession()) ? getSession().getNoteOffset() : 0);
//
//                // Play the sample
//                noteOn(noteToPlay, getLevel());
//
//                // Log the playback
//                logger.debug("Sample.onTick playing sample at note: {}", noteToPlay);
//            } catch (Exception e) {
//                logger.error("Error in Sample.onTick: {}", e.getMessage(), e);
//            }
//        }
//    }

    /**
     * Play the sample at a specific pitch
     */
    public void noteOn(int note, int velocity) {
        if (audioData == null || audioFormat == null) {
            logger.warn("Cannot play sample - no audio data loaded");
            return;
        }

        try {
            // Calculate pitch shift ratio from MIDI note
            // Each semitone is a factor of 2^(1/12) in frequency
            float ratio = (float) Math.pow(2.0, (note - referenceNote) / 12.0);

            // Skip pitch shifting if not enabled or if it's the reference note
            if (!autoPitchEnabled || note == referenceNote) {
                ratio = 1.0f;
            }

            // Create a new AudioFormat with the adjusted sample rate
            float newSampleRate = audioFormat.getSampleRate() * ratio;

            AudioFormat newFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    newSampleRate,
                    audioFormat.getSampleSizeInBits(),
                    audioFormat.getChannels(),
                    audioFormat.getFrameSize(),
                    newSampleRate,
                    audioFormat.isBigEndian()
            );

            // Create a new audio input stream with the adjusted format
            AudioInputStream pitchedStream = new AudioInputStream(
                    new java.io.ByteArrayInputStream(audioData),
                    newFormat,
                    audioData.length / audioFormat.getFrameSize()
            );

            // Create a new clip for this note
            Clip clip = AudioSystem.getClip();
            clip.open(pitchedStream);

            // Apply volume based on velocity (0-127)
            float gainLevel = velocity / 127.0f;
            if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                // Convert linear gain to dB gain (-40dB to 0dB range)
                float dBGain = 20 * (float) Math.log10(gainLevel);
                // Clamp to control's range
                dBGain = Math.max(gainControl.getMinimum(), Math.min(dBGain, gainControl.getMaximum()));
                gainControl.setValue(dBGain);
            }

            // Configure playback
            if (loopEnabled) {
                clip.setLoopPoints(loopStart, loopEnd);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }

            // Save the active clip for this note
            activeClips.put(note, new ActiveSample(clip, note, ratio));

            // Start playback
            clip.start();

            // Update player state
            setPlaying(true);

            // Publish event to notify UI
            CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, this);

            logger.debug("Playing sample at note: {}, rate: {}x", note, ratio);
        } catch (Exception e) {
            logger.error("Error playing sample: {}", e.getMessage(), e);
        }
    }

    public void noteOff(int note, int velocity) {
        // Find and stop the specific note
        ActiveSample activeSample = activeClips.remove(note);
        if (activeSample != null) {
            try {
                activeSample.stop();
                logger.debug("Stopped sample playback for note: {}", note);

                // If no more active clips, update player state
                if (activeClips.isEmpty()) {
                    setPlaying(false);
                    CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, this);
                }
            } catch (Exception e) {
                logger.error("Error stopping sample: {}", e.getMessage(), e);
            }
        }
    }

    public void allNotesOff() {
        // Stop all active samples
        for (ActiveSample sample : activeClips.values()) {
            try {
                sample.stop();
            } catch (Exception e) {
                logger.error("Error stopping sample during allNotesOff: {}", e.getMessage());
            }
        }

        // Clear the active clips map
        activeClips.clear();

        // Update player state
        setPlaying(false);
        CommandBus.getInstance().publish(Commands.PLAYER_ROW_REFRESH, this, this);

        logger.debug("All sample playback stopped");
    }

    @Override
    public void onAction(Command action) {
        // Handle standard player commands
        // super.onAction(action);

        // Handle sample-specific commands
        if (action == null || action.getCommand() == null) {
            return;
        }

        switch (action.getCommand()) {
            case Commands.SAMPLE_LOOP_POINTS_CHANGED:
                if (action.getData() instanceof Object[] data && data.length >= 3) {
                    Long id = (Long) data[0];
                    if (id.equals(getId())) {
                        Integer start = (Integer) data[1];
                        Integer end = (Integer) data[2];
                        setLoopStart(start);
                        setLoopEnd(end);
                        logger.debug("Updated loop points: {}-{}", start, end);
                    }
                }
                break;

            case Commands.SAMPLE_SELECTION_CHANGED:
                if (action.getData() instanceof Object[] data && data.length >= 3) {
                    Long id = (Long) data[0];
                    if (id.equals(getId())) {
                        Integer start = (Integer) data[1];
                        Integer end = (Integer) data[2];
                        setSampleStart(start);
                        setSampleEnd(end);
                        logger.debug("Updated sample selection: {}-{}", start, end);
                    }
                }
                break;
        }
    }

    /**
     * Gets information about this sample as a diagnostic string
     */
    public String getSampleInfo() {
        if (audioFormat == null) {
            return "No sample loaded";
        }

        return String.format("Sample Rate: %d Hz, Bits: %d, Channels: %d, Duration: %.2f sec, Size: %.2f KB",
                sampleRate, sampleSizeInBits, channels, getDuration(), audioData.length / 1024.0);
    }

    /**
     * Class to hold currently playing sample instances
     */
    private class ActiveSample {
        private final Clip clip;
        private final int note;
        private final float rate;

        public ActiveSample(Clip clip, int note, float rate) {
            this.clip = clip;
            this.note = note;
            this.rate = rate;
        }

        public void stop() {
            if (clip.isRunning()) {
                clip.stop();
            }
            clip.close();
        }
    }
}