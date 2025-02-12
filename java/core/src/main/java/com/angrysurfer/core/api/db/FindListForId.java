package com.angrysurfer.core.api.db;

import java.util.List;

public interface FindListForId<T> {

    List<T> find(Long id);
}
