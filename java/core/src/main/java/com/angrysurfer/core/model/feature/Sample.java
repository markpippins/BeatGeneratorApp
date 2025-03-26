package com.angrysurfer.core.model.feature;

import java.util.ArrayList;

import com.angrysurfer.core.model.Instrument;
import com.angrysurfer.core.model.Session;
import com.angrysurfer.core.model.Strike;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Sample extends Strike {

    private boolean started = false;

    public Sample(String name, Session session, Instrument instrument, int low, int high) {
        super(name, session, instrument, low, new ArrayList<>(instrument.getAssignments().keySet()));
        // setHighNote(high);
    }

}
