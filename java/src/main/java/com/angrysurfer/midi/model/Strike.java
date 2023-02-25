package com.angrysurfer.midi.model;

import com.angrysurfer.midi.model.condition.Comparison;
import com.angrysurfer.midi.model.condition.Condition;
import com.angrysurfer.midi.model.condition.Operator;
import com.angrysurfer.midi.service.IMidiInstrument;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

@Getter
@Setter
@Entity
public class Strike extends Player {
    static final Random rand = new Random();
    static Logger logger = LoggerFactory.getLogger(Strike.class.getCanonicalName());

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

//    List<Ratchet> ratchets = new ArrayList<>();

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

    public Strike addCondition(Operator operator, Comparison comparison, Double value) {
        getConditions().add(new Condition(operator, comparison, value));
        getConditions().get(getConditions().size() - 1).setId((long) getConditions().size());
        return this;
    }

    @Override
    public void onTick(int tick, int bar) {
        drumNoteOn(getNote(), rand.nextInt(getMinVelocity(), getMaxVelocity()));
    }
}
