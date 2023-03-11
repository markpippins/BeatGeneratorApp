package com.angrysurfer.midi.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.OneToMany;

@Entity
public class Pattern {
    
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = "id", nullable = false)
    private Long id;

    @OneToMany(fetch = FetchType.EAGER)
	@JoinTable(name = "pattern_steps", joinColumns = { @JoinColumn(name = "query_id") }, inverseJoinColumns = {
			@JoinColumn(name = "step_id") })
	private List<StepData> steps = new ArrayList();

    private int length;

    private int baseNote;

    private int tempo;
    
}
