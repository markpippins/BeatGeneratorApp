package com.angrysurfer.core.util.db;

import java.util.List;

public interface FindList<T> {

    List<T> find(Long id);
}
