package com.angrysurfer.midi.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

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

    private double stepSize = 1.0;

    private double stepValue = 0;

    private List<CyclerListener> listeners = new ArrayList<>();

    public Cycler() {

    }

    public Cycler(int length) {
        this.length = length;
    }
    
    public Long advance() {
        if (position.get() == length)
            position.set(0);

        if (stepSize < 1 && stepSize > 0) {
            stepValue = stepValue + stepSize;
            if (stepValue == 1) {
                stepValue = 0;
                return incrementPosition();
            }
            else return position.get() == 0 ? 1 : position.get() ;
        }
        
        return incrementPosition();
    }

    private Long incrementPosition() {
        position.incrementAndGet();
        notifyAll(position.get());
        cycleComplete();
        return position.get(); 
    }

    public Long get() {
        return position.get() == 0 ? 1 : position.get();
    }

    public Long zeroBasedGet() {
        if (position.get() == 0)
            return 0L;

        return position.get() - 1;
    }

    public void reset() {
        position.set(0);
    }

    public boolean atStart() {
        return get() == 1;
    }
    
    public boolean atEnd() {
        return getLength() == get();
    }

    public void addListener(CyclerListener listener) {
        getListeners().add(listener);
    }

    public void removeListener(CyclerListener listener) {
        getListeners().remove(listener);
    }

    public void notifyAll(long position) {
        getListeners().forEach(l -> l.notify(position));
    }

    public void cycleComplete() {
        getListeners().forEach(l -> l.cycleComplete());
    }

    public static void main(String[] args) {
        Cycler cycler = new Cycler(16);
        IntStream.range(0, 32).forEach(i -> 
            logger.info(String.format("%s - %s", cycler.advance(), cycler.getPosition())));


        Cycler cycler2 = new Cycler(16);
        cycler2.stepSize = .5;
        IntStream.range(0, 16).forEach(i -> 
            logger.info(String.format("%s - %s", cycler2.advance(), cycler2.getPosition())));

        Cycler cycler3 = new Cycler(16);
        cycler3.stepSize = .25;
        IntStream.range(0, 64).forEach(i -> 
            logger.info(String.format("%s - %s", cycler3.advance(), cycler3.get())));
    }

}
