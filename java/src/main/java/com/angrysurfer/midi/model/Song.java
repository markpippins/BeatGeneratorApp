package com.angrysurfer.midi.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    private String name;
    
    @OneToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "song_patterns", joinColumns = { @JoinColumn(name = "song_id") }, inverseJoinColumns = {
			@JoinColumn(name = "pattern_id") })
	private List<Pattern> patterns = new ArrayList<>();
}
