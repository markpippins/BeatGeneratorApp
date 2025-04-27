package com.angrysurfer.core.model.feature;

import java.io.File;
import java.util.ArrayList;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Clip;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.angrysurfer.core.model.Player;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;
import com.angrysurfer.core.sequencer.TimingUpdate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sample extends Player {

    private boolean started = false;
    private File audioFile;
    private Clip audioClip;
    private AudioFormat audioFormat;
    private byte[] audioData;
    private int[] waveformData;
    private int sampleRate;
    private int channels;
    private int sampleSizeInBits;
//    private double duration;

    // Selection points (in frames)
    private int sampleStart = 0;
    private int sampleEnd = 0;
    private int loopStart = 0;
    private int loopEnd = 0;
    
    private int fadeInDuration = 0;
    private int fadeOutDuration = 0;
    private boolean loopEnabled = false;
    private boolean reverseEnabled = false;
    private boolean fadeInEnabled = false;
    private boolean fadeOutEnabled = false;
    private boolean normalizeEnabled = false;
    private boolean autoTrimEnabled = false;
    
    private boolean autoGainEnabled = false;
    private boolean autoPitchEnabled = false;
    private boolean autoTempoEnabled = false;
    private boolean autoReverseEnabled = false;
    private boolean autoLoopEnabled = false;
    private boolean autoFadeInEnabled = false;
    private boolean autoFadeOutEnabled = false;
    private boolean autoNormalizeEnabled = false;


    public Sample() {
        super();
    }

    @Override
    public void onTick(TimingUpdate timingUpdate) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'onTick'");
    }

}
