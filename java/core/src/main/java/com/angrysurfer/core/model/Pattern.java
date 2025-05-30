package com.angrysurfer.core.model;

import com.angrysurfer.core.sequencer.Quantizer;
import com.angrysurfer.core.sequencer.Scale;
import com.angrysurfer.core.sequencer.SequencerConstants;
import com.angrysurfer.core.util.Cycler;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

@Entity
@Getter
@Setter
@Table(name = "pattern")
public class Pattern {

    private static final Logger logger = LoggerFactory.getLogger(Pattern.class);

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

    private Boolean muted = false;

    @Column(name = "auto_repeat")
    private Boolean loop = true;

    private Integer beatDivider = SequencerConstants.DEFAULT_BEAT_DIVIDER;

    @JsonIgnore
    @Transient
    private Quantizer quantizer = new Quantizer(Scale.getScale("C", Scale.SCALE_CHROMATIC));

    @JsonIgnore
    @Transient
    private Stack<Integer> playingNote = new Stack<>();

    @Column(name = "delay_bars")
    private Integer delay = 0;

    @JsonIgnore
    @ManyToOne()
    @JoinColumn(name = "song_id")
    private Song song;

    @OneToMany(mappedBy = "pattern", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Step> steps = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id")
    private InstrumentWrapper instrument;

    @JsonIgnore
    @Column(name = "step_cycler")
    private Integer stepCyclerPosition = 0;

    @Transient
    @JsonIgnore
    private Cycler stepCycler = new Cycler();

    public Pattern() {
        logger.debug("Creating new Pattern");
    }

    public void setQuantize(Boolean quantize) {
        logger.debug("setQuantize() - value: {}", quantize);
        this.quantize = quantize;
    }

    public void setDirection(Integer direction) {
        logger.debug("setDirection() - value: {}", direction);
        this.direction = direction;
    }

    public void setSpeed(Integer speed) {
        logger.debug("setSpeed() - value: {}", speed);
        this.speed = speed;
    }

}
