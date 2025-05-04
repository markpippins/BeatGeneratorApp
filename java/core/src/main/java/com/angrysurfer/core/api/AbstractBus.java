package com.angrysurfer.core.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractBus implements IBusListener {
    private static final Logger logger = LoggerFactory.getLogger(AbstractBus.class);
    
    private final List<IBusListener> listeners = new CopyOnWriteArrayList<>();
    private final String instanceId;
    
    // Thread pool for asynchronous command processing
    private final ExecutorService commandExecutor;
    private final boolean asyncProcessing;

    protected AbstractBus() {
        this(true, Runtime.getRuntime().availableProcessors(), true);
    }

    public AbstractBus(boolean asyncProcessing, int threadPoolSize) {
        this(asyncProcessing, threadPoolSize, true);
    }
    
    public AbstractBus(boolean asyncProcessing, int threadPoolSize, boolean registerSelf) {
        // Create a unique ID for this instance
        instanceId = String.valueOf(System.identityHashCode(this));
        this.asyncProcessing = asyncProcessing;
        
        logger.info("{} instance initialized - ID: {}", getClass().getSimpleName(), instanceId);
        System.out.println(getClass().getSimpleName() + " instance initialized - ID: " + instanceId);


        
        // Create thread factory
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, getClass().getSimpleName() + "-" + 
                                   instanceId + "-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };

        // Create thread pool
        if (asyncProcessing) {
            commandExecutor = Executors.newFixedThreadPool(threadPoolSize, threadFactory);
            logger.info("Created async command bus with {} threads - Instance ID: {}", 
                      threadPoolSize, instanceId);
        } else {
            commandExecutor = null;
            logger.info("Created synchronous command bus - Instance ID: {}", instanceId);
        }

        // Register self only if requested
        if (registerSelf) {
            register(this);
        }
    }
    
    // Add an instance method to verify this instance
    public String getInstanceId() {
        return instanceId;
    }

    public void register(IBusListener listener) {
        if (listener != null && !listeners.contains(listener))
            listeners.add(listener);
    }

    public void unregister(IBusListener listener) {
        listeners.remove(listener);
    }

    public void publish(String command) {
        publish(new Command(command, this, this));
    }

    public void publish(String command, Object sender) {
        Command cmd = new Command(command, sender, null);
        publish(cmd);
    }

    public void publish(String command, Object sender, Object data) {
        Command cmd = new Command(command, sender, data);
        for (IBusListener listener : listeners) {
            if (listener != sender)
                listener.onAction(cmd);
        }
    }

    /**
     * Publishes a command to be processed by all registered listeners.
     * Depending on the bus configuration, processing will happen either
     * synchronously in the current thread or asynchronously in the thread pool.
     * 
     * @param action The command to publish
     */
    public void publish(Command action) {
        if (action == null) {
            logger.error("Attempted to publish null action");
            return;
        }

        if (asyncProcessing && commandExecutor != null) {
            commandExecutor.submit(() -> processCommand(action));
        } else {
            processCommand(action);
        }
    }

    /**
     * Publish a command with immediate execution regardless of async setting.
     * Use this for commands that must be processed immediately.
     * 
     * @param action The command to publish immediately
     */
    public void publishImmediate(Command action) {
        if (action == null) {
            logger.error("Attempted to publish null action");
            return;
        }

        processCommand(action);
    }

    /**
     * Process the command by notifying all listeners
     */
    private void processCommand(Command action) {
        listeners.forEach(listener -> {
            try {
                if (listener != action.getSender())
                    listener.onAction(action);
            } catch (Exception e) {
                logger.error("Error in listener {} handling command {}: {}", 
                             listener.getClass().getSimpleName(),
                             action.getCommand(),
                             e.getMessage(), e);
            }
        });
    }

    /**
     * Shutdown the command executor gracefully
     */
    public void shutdown() {
        if (commandExecutor != null && !commandExecutor.isShutdown()) {
            logger.info("Shutting down command executor");
            commandExecutor.shutdown();
            try {
                // Wait for existing tasks to terminate
                if (!commandExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warn("Command executor did not terminate in time, forcing shutdown");
                    commandExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                logger.error("Command executor shutdown was interrupted", e);
                commandExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void onAction(Command action) {
        // Handle logging commands
        if (action.getData() instanceof LogMessage msg) {
            switch (action.getCommand()) {
                case Commands.LOG_DEBUG -> logger.debug(msg.message());
                case Commands.LOG_INFO -> logger.info(msg.message());
                case Commands.LOG_WARN -> logger.warn(msg.message());
                case Commands.LOG_ERROR -> {
                    if (msg.throwable() != null) {
                        logger.error(msg.message(), msg.throwable());
                    } else {
                        logger.error(msg.message());
                    }
                }
            }
        }
    }

    // Helper record for log messages
    public record LogMessage(String source, String message, Throwable throwable) {
        public LogMessage(String source, String message) {
            this(source, message, null);
        }
    }

    // Helper methods to publish log messages
    public void debug(String source, String message) {
        publish(Commands.LOG_DEBUG, this, new LogMessage(source, message));
    }

    public void info(String source, String message) {
        publish(Commands.LOG_INFO, this, new LogMessage(source, message));
    }

    public void warn(String source, String message) {
        publish(Commands.LOG_WARN, this, new LogMessage(source, message));
    }

    public void error(String source, String message) {
        publish(Commands.LOG_ERROR, this, new LogMessage(source, message));
    }

    public void error(String source, String message, Throwable e) {
        publish(Commands.LOG_ERROR, this, new LogMessage(source, message, e));
    }
}
