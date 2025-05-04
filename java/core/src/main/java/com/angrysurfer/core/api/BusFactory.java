package com.angrysurfer.core.api;

import java.util.ServiceLoader;

public class BusFactory {
    private static IBusProvider provider;
    
    static {
        ServiceLoader<IBusProvider> loader = ServiceLoader.load(IBusProvider.class);
        for (IBusProvider p : loader) {
            provider = p;
            break;
        }
        
        // Fallback to default if no provider found
        if (provider == null) {
            provider = new DefaultBusProvider();
        }
        
        System.out.println("BusFactory initialized with provider: " + provider.getClass().getName());
    }
    
    public static CommandBus getCommandBus() {
        return provider.getCommandBus();
    }
    
    public static TimingBus getTimingBus() {
        return provider.getTimingBus();
    }
}