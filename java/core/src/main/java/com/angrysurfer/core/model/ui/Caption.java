package com.angrysurfer.core.model.ui;

import java.io.Serializable;

import com.angrysurfer.core.api.ICaption;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Caption implements Serializable, ICaption {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    private Long code;
    private String description;
}
