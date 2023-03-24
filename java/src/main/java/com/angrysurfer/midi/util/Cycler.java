package com.angrysurfer.midi.util;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Cycler {

    static Logger logger = LoggerFactory.getLogger(Cycler.class.getCanonicalName());

    private int length;

    private AtomicLong position = new AtomicLong(1);

    public Cycler() {

    }

    public Cycler(int length) {
        this.length = length;
    }
    
    public Long advance() {
        if (position.get() == length)
            position.set(0);
        return position.incrementAndGet();
    }

    public Long get() {
        return position.get();
    }

    public Long zeroBasedGet() {
        if (position.get() == 0)
            return 0L;

        return position.get() - 1;
    }

    public void reset() {
        position.set(0);
    }

}
