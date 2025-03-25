package com.angrysurfer.core.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cycler {

    static Logger logger = LoggerFactory.getLogger(Cycler.class.getCanonicalName());

    private Long length = 1L;

    private AtomicLong position = new AtomicLong(1);

    private Double stepSize = 1.0;






    

    private Double stepValue = 0.0;

    private List<CyclerListener> listeners = new ArrayList<>();

    public Cycler() {

    }

    public Cycler(long length) {
        this.length = length;
    }
    
    public Long advance() {
        logger.debug("advance() - current position: {}, length: {}", position.get(), length);
        if (position.get() == length && length > 1)
            position.set(0);
        
        if (getLength() == 1)
            return 1L;

        if (stepSize < 1 && stepSize > 0) {
            stepValue = stepValue + stepSize;
            if (stepValue == 1.0) {
                stepValue = 0.0;
                return incrementPosition();
            }
            else return position.get() == 0 ? 1 : position.get() ;
        }
        
        return incrementPosition();
    }

    private Long incrementPosition() {
        position.incrementAndGet();
        logger.debug("incrementPosition() - new position: {}", position.get());
        notifyAll(position.get());
        if (position.get() > getLength()) {
            logger.debug("Cycle complete at position: {}", position.get());
            cycleComplete();
        }
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
        logger.debug("reset() - resetting position to 0");
        position.set(0);
    }

    public boolean atStart() {
        return get() == 1;
    }
    
    public boolean atEnd() {
        return getLength() == get();
    }

    public void addListener(CyclerListener listener) {
        logger.debug("addListener() - adding listener: {}", listener.getClass().getSimpleName());
        getListeners().add(listener);
    }

    public void removeListener(CyclerListener listener) {
        logger.debug("removeListener() - removing listener: {}", listener.getClass().getSimpleName());
        getListeners().remove(listener);
    }

    public void notifyAll(long position) {
        logger.debug("notifyAll() - notifying {} listeners of position: {}", listeners.size(), position);
        getListeners().forEach(l -> l.advanced(position));
    }

    public void cycleComplete() {
        getListeners().forEach(l -> l.cycleComplete());
    }


}
