package com.angrysurfer.midi.util;

import java.util.concurrent.atomic.AtomicInteger;
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

    private double stepSize = 1.0;

    private double stepValue = 0;

    private AtomicInteger position = new AtomicInteger(0);

    public Cycler() {

    }

    public Cycler(int length) {
        this.length = length;
    }
    
    public int advance() {

        if (position.get() > length)
            throw new RuntimeException("Invalid Configuration");

        if (position.get() == length)
            position.set(0);
        

        if (stepSize < 1 && stepSize > 0) {
            stepValue = stepValue + stepSize;
            if (stepValue == 1) {
                stepValue = 0;
                return position.incrementAndGet();
            }
            else return position.get() == 0 ? 1 : position.get() ;
        }

        return position.incrementAndGet();
    }

    public int get() {
        return position.get() == 0 ? 1 : position.get();
    }

    public void reset() {
        position.set(0);
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
            logger.info(String.format("%s - %s", cycler3.advance(), cycler3.getPosition())));
    }
}