package com.uhsl.flowmessage.flowmessagev2.flow;

import android.os.Message;

import com.imgtec.flow.MessagingEvent;

/**
 * Created by Marcus on 26/02/2016.
 */
public interface AsyncMessageListener {
    /**
     * Called when a new message arrives
     * @param messagingEvent Holds the message info
     */
    void onMessageReceived(MessagingEvent messagingEvent);

    /**
     * Called when a new auto response is arrives
     * @param response Holds the response info
     */
    void onResponseReceived(MessagingEvent response);
}
