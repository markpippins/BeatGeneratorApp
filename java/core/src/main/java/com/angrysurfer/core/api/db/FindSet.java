package com.angrysurfer.core.api.db;

import java.util.Set;

public interface FindSet<T> {

    Set<T> find(Long id);
}
