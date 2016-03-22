package com.uhsl.flowmessage.flowmessagev2.flow;

import android.content.Context;

import com.imgtec.flow.MessagingEvent;
import com.imgtec.flow.MessagingEvent.MessagingEventCategory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;



/**
 * Created by Marcus on 10/03/2016.
 */
public abstract class SubscribedEventListener {

    private Map<String, List<MessagingEventCategory>> deviceEventMap = new HashMap<>();
    private FlowController flowController;

    /**
     * Constructor
     *
     * @param flowController The FlowController Instance
     */
    public SubscribedEventListener(FlowController flowController) {
        this.flowController = flowController;
    }

    /**
     * To be extended when used, this method is called when an event is received
     *
     * @param messagingEvent Data from the event
     */
    public abstract void onEventReceived(MessagingEvent messagingEvent);

    /**
     * Adds a device/event pair to this listener object.
     *
     * @param AoR Messaging address of the device
     * @param event The type of event to listen for
     */
    public void AddDeviceEvent(String AoR, MessagingEventCategory event) {
        AddDeviceEvents(AoR, new MessagingEventCategory[] {event});
    }

    /**
     * Adds an array of events to a single device
     *
     * @param AoR Messaging address for the device
     * @param events Array of event types to add
     */
    public void AddDeviceEvents(String AoR, MessagingEventCategory[] events) {
        if (!deviceEventMap.containsKey(AoR))
            deviceEventMap.put(AoR, new ArrayList<MessagingEventCategory>());

        for (MessagingEventCategory event : events) {
            deviceEventMap.get(AoR).add(event);

            flowController.addEventSubscription(this, AoR, event);
        }
    }

    /**
     * Remove a device from this listener
     *
     * @param AoR Messaging address of the device
     */
    public void removeDevice(String AoR) {
        if (deviceEventMap.containsKey(AoR))
            deviceEventMap.remove(AoR);
    }

    /**
     * Remove an event from a certain device in this listener
     *
     * @param AoR Messaging address of device
     * @param event The event type to remove
     */
    public void removeEventFromDevice(String AoR, MessagingEventCategory event) {
        if (deviceEventMap.containsKey(AoR)) {
            List<MessagingEventCategory> list = deviceEventMap.get(AoR);
            if (list.contains(event)) {
                list.remove(event);
            }
        }
    }

    /**
     * Returns whether this listener is registered to listen for a certain device publishing a certain event type
     *
     * @param AoR Messaging address of the device
     * @param event The event type
     * @return Is device/event pair registered
     */
    public boolean isSubscribedToDeviceEvent(String AoR, MessagingEventCategory event) {
        return deviceEventMap.containsKey(AoR) && deviceEventMap.get(AoR).contains(event);
    }
}
