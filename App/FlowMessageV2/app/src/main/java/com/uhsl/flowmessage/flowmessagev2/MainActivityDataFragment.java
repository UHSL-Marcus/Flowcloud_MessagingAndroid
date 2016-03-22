package com.uhsl.flowmessage.flowmessagev2;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.uhsl.flowmessage.flowmessagev2.flow.SubscribedEventListener;
import com.uhsl.flowmessage.flowmessagev2.utils.DeviceConnectionStateMachine;

import java.util.List;
import java.util.Timer;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivityDataFragment extends Fragment {

    private SubscribedEventListener eventListener;

    private List<String[]> messageList = new CopyOnWriteArrayList<>(); //[0]Body, [1]MessageID, [2]Status

    private DeviceConnectionStateMachine connectionStateMachine;

    /**
     * {@inheritDoc}
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
    }

    public List<String[]> getMessageList() {
        return messageList;
    }

    public void setMessageList(List<String[]> messageList) {
        this.messageList = messageList;
    }

    public SubscribedEventListener getEventListener() {
        return eventListener;
    }

    public void setEventListener(SubscribedEventListener eventListener) {
        this.eventListener = eventListener;
    }

    public DeviceConnectionStateMachine getConnectionStateMachine() {
        return connectionStateMachine;
    }

    public void setConnectionStateMachine(DeviceConnectionStateMachine connectionStateMachine) {
        this.connectionStateMachine = connectionStateMachine;
    }
}
