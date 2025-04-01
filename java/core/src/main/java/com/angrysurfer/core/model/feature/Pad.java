package com.angrysurfer.core.model.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.angrysurfer.core.model.InstrumentWrapper;
import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

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
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class Pad {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private Integer note;

    @ElementCollection
    private List<Integer> controlCodes = new ArrayList<>();

    private String name;

    @JsonIgnore  // Ignore this field during serialization
    @ManyToMany(mappedBy = "pads")
    private Set<InstrumentWrapper> instruments = new HashSet<>();

    public Pad() {

    }

    public Pad(Integer note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return String.format("Pad{id=%d, note=%d, name='%s', controlCodes=%s}",
                id, note, name, controlCodes);
    }
}
