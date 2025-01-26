package com.angrysurfer.sequencer.model.midi;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.angrysurfer.sequencer.model.ui.Caption;

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

    @ManyToMany
    @JoinTable(
            name = "control_code_caption",
            joinColumns = @JoinColumn(name = "caption_id"),
            inverseJoinColumns = @JoinColumn(name = "control_code_id"))
    private Set<Caption> captions = new HashSet<>();
}
