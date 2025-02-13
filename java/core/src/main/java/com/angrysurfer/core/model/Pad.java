package com.angrysurfer.core.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.angrysurfer.core.api.IInstrument;
import com.angrysurfer.core.api.IPad;
import com.angrysurfer.core.model.midi.Instrument;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
public class Pad implements IPad {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private Integer note;

    @ElementCollection
    List<Integer> controlCodes = new ArrayList<>();

    private String name;

    @JsonBackReference
    @ManyToMany(mappedBy = "pads")
    private Set<IInstrument> instruments;

    public Pad() {

    }

    public Pad(Integer note) {
        this.note = note;
    }
}
