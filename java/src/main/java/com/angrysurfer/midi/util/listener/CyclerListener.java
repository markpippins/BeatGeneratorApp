package com.angrysurfer.midi.util.listener;

public interface CyclerListener {

    public void advanced(long position);

    public void cycleComplete();

    public void starting();
    
}
  