package com.angrysurfer.midi.model;


import com.angrysurfer.midi.model.config.MidiInstrumentInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ControlCode {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    private String name;
    private Integer controlCode;
    private Integer lowerBound;
    private Integer upperBound;

}
