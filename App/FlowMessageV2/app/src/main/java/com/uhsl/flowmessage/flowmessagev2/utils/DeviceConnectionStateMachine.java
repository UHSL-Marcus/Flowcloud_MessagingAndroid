package com.uhsl.flowmessage.flowmessagev2.utils;

import android.util.Pair;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Marcus on 11/03/2016.
 */
public class DeviceConnectionStateMachine {
    private DeviceConnectionState currentDeviceConnectionState = DeviceConnectionState.NO_CONNECTION;
    private Map<DeviceConnectionState, Runnable> stateInvokableMethods = new HashMap<>();

    /**
     * Fire a new event in the state machine
     *
     * @param event The event being actioned
     */
    public void doEvent(DeviceConnectionEvent event) {
        DeviceConnectionState nextState = currentDeviceConnectionState.getNextState(event);
        if (nextState != currentDeviceConnectionState && stateInvokableMethods.containsKey(nextState))
            stateInvokableMethods.get(nextState).run();

        currentDeviceConnectionState = nextState;
    }

    /**
     * Set a runnable method to be called whenever moving to a state
     *
     * @param state The state in question
     * @param callback The runnable method to be called
     */
    public void setStateCallback(DeviceConnectionState state, Runnable callback) {
        if (!stateInvokableMethods.containsKey(state)) {
            stateInvokableMethods.put(state, callback);
        }
    }

    /**
     * Start the state machine and call the initial state's runnable method
     */
    public void start() {
        if (stateInvokableMethods.containsKey(currentDeviceConnectionState))
            stateInvokableMethods.get(currentDeviceConnectionState).run();
    }
}
