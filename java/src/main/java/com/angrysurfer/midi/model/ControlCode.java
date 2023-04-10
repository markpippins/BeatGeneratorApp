package com.angrysurfer.midi.model;


import java.io.Serializable;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ControlCode implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    private String name;
    private Integer code;
    private Integer lowerBound;
    private Integer upperBound;
    private Integer pad;
    @Column(name = "is_binary")
    private Boolean binary;
}
