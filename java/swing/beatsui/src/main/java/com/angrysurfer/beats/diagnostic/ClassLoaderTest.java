package com.angrysurfer.beats.diagnostic;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassLoaderTest {
    public static void main(String[] args) {
        // Create a map of class name to set of classloaders that loaded them
        Map<String, Map<ClassLoader, AtomicInteger>> classLoadCount = new HashMap<>();
        
        // List of interesting classes to check
        String[] classesToCheck = {
            "com.angrysurfer.core.api.CommandBus",
            "com.angrysurfer.core.api.TimingBus",
            "com.angrysurfer.core.api.AbstractBus",
            "com.angrysurfer.beats.App"
        };
        
        // Dump all loaded classes from current classloader chain
        ClassLoader loader = ClassLoaderTest.class.getClassLoader();
        System.out.println("Classloader hierarchy:");
        while (loader != null) {
            System.out.println("- " + loader);
            loader = loader.getParent();
        }
        
        // Try to find and instantiate our key classes
        System.out.println("\nTrying to access key classes:");
        for (String className : classesToCheck) {
            try {
                Class<?> clazz = Class.forName(className);
                ClassLoader classLoader = clazz.getClassLoader();
                
                System.out.println(className + " loaded by: " + classLoader);
                
                // Count loadings
                classLoadCount.computeIfAbsent(className, k -> new HashMap<>())
                              .computeIfAbsent(classLoader, k -> new AtomicInteger())
                              .incrementAndGet();
                
                // Try to get instance if it has getInstance method
                try {
                    java.lang.reflect.Method getInstance = clazz.getMethod("getInstance");
                    Object instance1 = getInstance.invoke(null);
                    Object instance2 = getInstance.invoke(null);
                    
                    System.out.println("  - getInstance() returns same instance: " + 
                                     (instance1 == instance2) + 
                                     " (IDs: " + System.identityHashCode(instance1) + 
                                     ", " + System.identityHashCode(instance2) + ")");
                } catch (NoSuchMethodException e) {
                    // No getInstance method
                    System.out.println("  - No getInstance() method found");
                }
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                System.out.println("Error with " + className + ": " + sw.toString());
            }
        }
        
        System.out.println("\nClass loading summary:");
        for (Map.Entry<String, Map<ClassLoader, AtomicInteger>> entry : classLoadCount.entrySet()) {
            System.out.println(entry.getKey() + ":");
            for (Map.Entry<ClassLoader, AtomicInteger> loaderEntry : entry.getValue().entrySet()) {
                System.out.println("  - Loaded " + loaderEntry.getValue() + " times by " + loaderEntry.getKey());
            }
        }
    }
}