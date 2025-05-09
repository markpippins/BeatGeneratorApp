package com.angrysurfer.core.service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class for audio file operations
 */
public class AudioFileManager {
    private static final Logger logger = LoggerFactory.getLogger(AudioFileManager.class);
    private static AudioFileManager instance;
    
    private Map<String, AudioInfo> audioCache = new HashMap<>();
    
    private AudioFileManager() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance
     */
    public static synchronized AudioFileManager getInstance() {
        if (instance == null) {
            instance = new AudioFileManager();
        }
        return instance;
    }
    
    /**
     * Get audio info for a file (cached)
     */
    public AudioInfo getAudioInfo(File file) throws IOException, UnsupportedAudioFileException {
        String path = file.getAbsolutePath();
        
        if (audioCache.containsKey(path)) {
            return audioCache.get(path);
        }
        
        AudioInfo info = loadAudioInfo(file);
        audioCache.put(path, info);
        return info;
    }
    
    /**
     * Load audio information from a file
     */
    private AudioInfo loadAudioInfo(File file) throws IOException, UnsupportedAudioFileException {
        try (AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file)) {
            AudioFormat format = audioInputStream.getFormat();
            long frameLength = audioInputStream.getFrameLength();
            float frameRate = format.getFrameRate();
            
            double durationSeconds = frameLength / frameRate;
            
            return new AudioInfo(
                    format,
                    (int)frameLength,
                    durationSeconds,
                    format.getSampleRate(),
                    format.getChannels(),
                    format.getSampleSizeInBits()
            );
        }
    }
    
    /**
     * Clear the audio cache
     */
    public void clearCache() {
        audioCache.clear();
    }
    
    /**
     * Class to hold audio file information
     */
    public static class AudioInfo {
        private final AudioFormat format;
        private final int frameLength;
        private final double durationSeconds;
        private final float sampleRate;
        private final int channels;
        private final int sampleSizeInBits;
        
        public AudioInfo(AudioFormat format, int frameLength, double durationSeconds,
                float sampleRate, int channels, int sampleSizeInBits) {
            this.format = format;
            this.frameLength = frameLength;
            this.durationSeconds = durationSeconds;
            this.sampleRate = sampleRate;
            this.channels = channels;
            this.sampleSizeInBits = sampleSizeInBits;
        }
        
        public AudioFormat getFormat() {
            return format;
        }
        
        public int getFrameLength() {
            return frameLength;
        }
        
        public double getDurationSeconds() {
            return durationSeconds;
        }
        
        public float getSampleRate() {
            return sampleRate;
        }
        
        public int getChannels() {
            return channels;
        }
        
        public int getSampleSizeInBits() {
            return sampleSizeInBits;
        }
    }
}