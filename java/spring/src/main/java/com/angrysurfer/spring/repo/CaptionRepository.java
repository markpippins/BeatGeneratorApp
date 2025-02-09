package com.angrysurfer.spring.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.angrysurfer.core.model.ui.Caption;

@Repository
public interface CaptionRepository extends JpaRepository<Caption, Long> {
    Iterable<Caption> findByCode(Long code);
}
