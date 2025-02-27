package com.angrysurfer.core.model.midi;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "control_code")
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
    @JoinTable(name = "control_code_caption", joinColumns = @JoinColumn(name = "caption_id"), inverseJoinColumns = @JoinColumn(name = "control_code_id"))
    private Set<ControlCodeCaption> captions = new HashSet<>();

    @Override
    public String toString() {
        return String.format("ControlCode{id=%d, name='%s', code=%d, bounds=[%d,%d], pad=%d, binary=%b, captions=%s}",
                id, name, code, lowerBound, upperBound, pad, binary, 
                captions.stream().map(ControlCodeCaption::toString).toList());
    }
}
