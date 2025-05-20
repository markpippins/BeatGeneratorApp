package com.angrysurfer.core.api;

public class DefaultBusListener implements IBusListener {
    // IBusListener implementation
    @Override
    public void onAction(Command action) {
        // Let the interface try to handle it with our registered handlers
        if (!tryHandleCommand(action)) {
            // Handle any commands not registered with lambdas
            System.out.println("Unhandled command: " + action.getCommand());
        }
    }

}
