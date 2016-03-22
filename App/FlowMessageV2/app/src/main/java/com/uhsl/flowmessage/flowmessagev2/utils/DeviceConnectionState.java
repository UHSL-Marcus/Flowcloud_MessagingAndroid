package com.uhsl.flowmessage.flowmessagev2.utils;

import android.util.Pair;

/**
 * Created by Marcus on 11/03/2016.
 */
public enum DeviceConnectionState {
    GOT_CONNECTION,LOSING_CONNECTION,LOST_CONNECTION,NO_CONNECTION;

    private Pair[] validTransitions;
    static {
        GOT_CONNECTION.validTransitions = new Pair[] {
                Pair.create(DeviceConnectionEvent.MISSED_HEARTBEAT, DeviceConnectionState.LOSING_CONNECTION),
                Pair.create(DeviceConnectionEvent.DISCONNECTED, DeviceConnectionState.NO_CONNECTION)
        };
        LOSING_CONNECTION.validTransitions = new Pair[] {
                Pair.create(DeviceConnectionEvent.MISSED_HEARTBEAT, DeviceConnectionState.LOST_CONNECTION),
                Pair.create(DeviceConnectionEvent.DISCONNECTED,DeviceConnectionState.NO_CONNECTION),
                Pair.create(DeviceConnectionEvent.CONNECTED, DeviceConnectionState.GOT_CONNECTION)
        };
        LOST_CONNECTION.validTransitions = new Pair[] {
                Pair.create(DeviceConnectionEvent.DISCONNECTED, DeviceConnectionState.NO_CONNECTION),
                Pair.create(DeviceConnectionEvent.CONNECTED, DeviceConnectionState.GOT_CONNECTION)
        };
        NO_CONNECTION.validTransitions = new Pair[] {
                Pair.create(DeviceConnectionEvent.CONNECTED, DeviceConnectionState.GOT_CONNECTION)
        };
    }

    /**
     * Use the transition table to return the next valid state depending on the event
     *
     * @param e The event being queried
     * @return The new state enum
     */
    public DeviceConnectionState getNextState(DeviceConnectionEvent e) {
        for (Pair p : validTransitions) {
            if (p.first.equals(e)) return (DeviceConnectionState)p.second;
        }
        return this;
    }

}
