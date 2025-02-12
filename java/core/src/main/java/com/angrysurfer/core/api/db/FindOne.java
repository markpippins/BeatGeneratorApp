package com.angrysurfer.core.api.db;

import java.util.Optional;

public interface FindOne<T> {

    Optional<T> find(Long id);
}
