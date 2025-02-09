package com.angrysurfer.core.util;

public interface Timer extends Runnable {
    void addTickCallback(Runnable callback);  // Single callback for tick events
    void addBeatCallback(Runnable callback);  // Single callback for beat events
    void stop();
    void setBpm(int newBpm);
    void setPpq(int newPpq);
    int getBpm();
    int getPpq();
    long getCurrentTick();
    boolean isRunning();
}