package com.angrysurfer.sequencer.model;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import com.angrysurfer.sequencer.model.midi.MidiInstrument;
import com.angrysurfer.sequencer.util.Constants;
import com.angrysurfer.sequencer.util.Cycler;
import com.angrysurfer.sequencer.util.Quantizer;
import com.angrysurfer.sequencer.util.Scale;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Pattern {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private String name;

    private Integer position = 0;

    private Integer channel = 0;

    private Integer scale = 0;

    private Boolean active = false;

    private Boolean quantize = false;

    private Integer direction = 1;

    private Integer length = 16;

    private Integer firstStep = 1;

    private Integer lastStep = 16;

    private Integer speed = 1;

    @Column(name = "rand")
    private Integer random = 0;

    private Integer rootNote = 60;

    private Integer transpose = 0;

    private Integer repeats = 99;

    private Integer swing = 50;

    private Integer gate = 50;

    private Integer preset = 0;

    private Boolean muted = true;

    @Column(name = "auto_repeat")
    private Boolean loop = true;

    private Integer beatDivider = Constants.DEFAULT_BEAT_DIVIDER;

    @JsonIgnore
    @Transient
    private Quantizer quantizer = new Quantizer(Scale.getScale("C", "Chromatic"));

    @JsonIgnore
    @Transient
    private Stack<Integer> playingNote = new Stack<>();

    @Column(name = "delay_bars")
    private Integer delay = 0;

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "song_id")
    private Song song;

    @Transient
    Set<Step> steps = new HashSet<>();

    // @JsonIgnore
    @Transient
    @JoinColumn(name = "instrument_id")
    private MidiInstrument instrument;

    @JsonIgnore
    @Transient
    private Cycler stepCycler = new Cycler();

    public Pattern() {

    }

}
