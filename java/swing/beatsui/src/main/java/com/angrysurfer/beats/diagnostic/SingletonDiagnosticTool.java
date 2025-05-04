package com.angrysurfer.beats.diagnostic;

import com.angrysurfer.core.api.CommandBus;
import com.angrysurfer.core.api.TimingBus;
import com.angrysurfer.core.redis.RedisService;
import com.angrysurfer.core.service.DeviceManager;
import com.angrysurfer.core.service.InstrumentManager;
import com.angrysurfer.core.service.InternalSynthManager;
import com.angrysurfer.core.service.SessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Enhanced utility class to verify singleton integrity in the application
 */
public class SingletonDiagnosticTool {
    
    public static void main(String[] args) {
        // Run basic singleton test
        runBasicTest();
        
        // Run advanced test with multiple threads
        runAdvancedTest();
        
        // Run specific tests for bus singletons
        System.out.println("\nRunning specific tests for bus singletons:");
        CommandBus.testSingleton();
        TimingBus.testSingleton();
        
        // Print class loader information
        System.out.println("\nClass Loader Information:");
        System.out.println("  CommandBus loaded by: " + CommandBus.class.getClassLoader());
        System.out.println("  TimingBus loaded by: " + TimingBus.class.getClassLoader());
        System.out.println("  Current class loaded by: " + SingletonDiagnosticTool.class.getClassLoader());
        
        System.out.println("\nSingleton testing completed.");
    }
    
    private static void runBasicTest() {
        // Map of singleton name -> supplier function
        Map<String, Supplier<?>> singletons = new HashMap<>();
        
        // Add all your singletons here
        singletons.put("CommandBus", CommandBus::getInstance);
        singletons.put("TimingBus", TimingBus::getInstance);
        singletons.put("RedisService", RedisService::getInstance);
        singletons.put("DeviceManager", DeviceManager::getInstance);
        singletons.put("InstrumentManager", InstrumentManager::getInstance);
        singletons.put("InternalSynthManager", InternalSynthManager::getInstance);
        singletons.put("SessionManager", SessionManager::getInstance);
        
        // Test each singleton multiple times
        System.out.println("Basic singleton integrity test:");
        System.out.println("==============================");
        
        for (Map.Entry<String, Supplier<?>> entry : singletons.entrySet()) {
            String name = entry.getKey();
            Supplier<?> supplier = entry.getValue();
            
            System.out.println("\nTesting " + name + ":");
            
            // Get the first instance
            Object instance1 = supplier.get();
            int hash1 = System.identityHashCode(instance1);
            System.out.println("  First instance hash: " + hash1);
            
            // Get a second instance - should be the same object
            Object instance2 = supplier.get();
            int hash2 = System.identityHashCode(instance2);
            System.out.println("  Second instance hash: " + hash2);
            
            // Check if the singletons are the same object
            boolean passed = (hash1 == hash2);
            System.out.println("  Basic test result: " + (passed ? "PASSED" : "FAILED"));
            
            if (!passed) {
                System.out.println("  WARNING: " + name + " is not a proper singleton!");
            }
        }
    }
    
    private static void runAdvancedTest() {
        // Map of singleton name -> supplier function
        Map<String, Supplier<?>> singletons = new HashMap<>();
        
        // Add all your singletons here
        singletons.put("CommandBus", CommandBus::getInstance);
        singletons.put("TimingBus", TimingBus::getInstance);
        
        System.out.println("\nAdvanced singleton integrity test (multi-threaded):");
        System.out.println("==================================================");
        
        ExecutorService executor = Executors.newFixedThreadPool(10);
        
        for (Map.Entry<String, Supplier<?>> entry : singletons.entrySet()) {
            String name = entry.getKey();
            Supplier<?> supplier = entry.getValue();
            
            System.out.println("\nTesting " + name + " with 10 concurrent threads:");
            
            try {
                // First get an instance on the main thread
                Object mainThreadInstance = supplier.get();
                int mainThreadHash = System.identityHashCode(mainThreadInstance);
                System.out.println("  Main thread instance hash: " + mainThreadHash);
                
                // Create tasks for 10 threads
                List<Callable<Integer>> tasks = new ArrayList<>();
                for (int i = 0; i < 10; i++) {
                    final int threadNum = i;
                    tasks.add(() -> {
                        Object threadInstance = supplier.get();
                        int hash = System.identityHashCode(threadInstance);
                        System.out.println("  Thread " + threadNum + " instance hash: " + hash);
                        return hash;
                    });
                }
                
                // Execute all tasks and collect results
                List<Future<Integer>> results = executor.invokeAll(tasks);
                
                // Check all results
                boolean allPassed = true;
                for (int i = 0; i < results.size(); i++) {
                    int hash = results.get(i).get();
                    boolean thisThreadPassed = (hash == mainThreadHash);
                    System.out.println("  Thread " + i + " test: " + 
                                     (thisThreadPassed ? "PASSED" : "FAILED"));
                    allPassed = allPassed && thisThreadPassed;
                }
                
                System.out.println("  Advanced test result: " + 
                                 (allPassed ? "PASSED" : "FAILED"));
                
                if (!allPassed) {
                    System.out.println("  WARNING: " + name + 
                                     " is not thread-safe or has multiple instances!");
                }
                
            } catch (Exception e) {
                System.out.println("  Error during test: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        executor.shutdown();
    }
}