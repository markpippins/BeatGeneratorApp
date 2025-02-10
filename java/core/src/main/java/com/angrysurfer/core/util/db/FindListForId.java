package com.angrysurfer.core.util.db;

import java.util.List;

public interface FindListForId<T> {

    List<T> find(Long id);
}
