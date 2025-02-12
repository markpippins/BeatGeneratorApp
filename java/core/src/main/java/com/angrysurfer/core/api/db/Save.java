package com.angrysurfer.core.api.db;

public interface Save<T> {
    
    public T save(T unsaved);
}
