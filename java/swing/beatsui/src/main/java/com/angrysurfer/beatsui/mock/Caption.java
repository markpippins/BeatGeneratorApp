package com.angrysurfer.beatsui.mock;

import java.io.Serializable;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
public class Caption implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;
    private Long code;
    private String description;

    @Override
    public String toString() {
        return String.format("Caption{id=%d, code=%d, description='%s'}", id, code, description);
    }
}
