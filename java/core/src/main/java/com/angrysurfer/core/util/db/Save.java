package com.angrysurfer.core.util.db;

public interface Save<T> {
    
    public T save(T unsaved);
}
