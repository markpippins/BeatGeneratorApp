package com.angrysurfer.midi.model;

import com.angrysurfer.midi.service.IMidiInstrument;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
@Setter
public class Strike extends Player {
    static final Random rand = new Random();
    static Logger logger = LoggerFactory.getLogger(Strike.class.getCanonicalName());
    List<Ratchet> ratchets = new ArrayList<>();

    public Strike() {
    }

    public Strike(String name, Ticker ticker, IMidiInstrument instrument, int note, List<Integer> allowedControlMessages) {
        super(name, ticker, instrument, allowedControlMessages);
        setNote(note);
    }

    public Strike(String name, Ticker ticker, IMidiInstrument instrument, int note,
                  List<Integer> allowableControlMessages, int minVelocity, int maxVelocity) {
        super(name, ticker, instrument, allowableControlMessages);
        setName(name);
        setNote(note);
        setMinVelocity(minVelocity);
        setMaxVelocity(maxVelocity);
    }

    public Strike addCondition(Eval.Operator operator, Eval.Comparison comparison, Double value) {
        Eval e = new Eval(operator, comparison, value);
        getConditions().put(e.toString(), e);
        return this;
    }

    @Override
    public void onTick(int tick, int bar) {
        drumNoteOn(getNote(), rand.nextInt(getMinVelocity(), getMaxVelocity()));
    }
}
