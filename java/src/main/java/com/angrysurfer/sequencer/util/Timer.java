package com.angrysurfer.sequencer.util;

public interface Timer  extends Runnable{

    void addTickListener(Runnable listener);

    void addBeatListener(Runnable listener);

    void stop();

    void setBpm(int newBpm);

    void setPpq(int newPpq);

    int getBpm();

    int getPpq();

    long getCurrentTick();

}