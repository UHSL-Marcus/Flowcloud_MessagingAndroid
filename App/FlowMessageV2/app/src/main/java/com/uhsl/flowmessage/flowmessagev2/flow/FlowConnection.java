package com.uhsl.flowmessage.flowmessagev2.flow;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.imgtec.flow.Flow;
import com.imgtec.flow.FlowHandler;
import com.imgtec.flow.MessagingEvent;
import com.imgtec.flow.client.core.Core;
import com.imgtec.flow.client.core.Setting;
import com.imgtec.flow.client.users.Device;
import com.imgtec.flow.client.users.Devices;
import com.imgtec.flow.client.users.User;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by Marcus on 19/02/2016.
 */
public class FlowConnection {

    private static volatile FlowConnection instance;

    private final Flow flowInstance;
    private final FlowHandler userFlowHandler;
    private String username = "mail+flow@marcuslee1.co.uk";
    private String password = "Sm@rtlab1234";
    private final List<Device> deviceCache = new CopyOnWriteArrayList<>();
    private Device currentDevice;
    private String userAOR;

    private Handler asyncMessageHandler;
    private Handler asyncResponseHandler;
    private AsyncMessageListener asyncMessageListener;

    private Handler eventReceivedHandler;
    private final List<SubscribedEventListener> eventListeners = new CopyOnWriteArrayList<>();

    /**
     * Private constructor
     * @param context Context of call, often an activity
     */
    private FlowConnection(Context context) {
        flowInstance = Flow.getInstance();
        flowInstance.setAppContext(context);
        userFlowHandler = new FlowHandler();
        initAsyncMessageHandlers();
    }

    /**
     * Get or create the instance of this singleton
     * @param context Context of call, often an activity
     * @return FlowConnection Singleton Instance
     */
    public static FlowConnection getInstance(Context context) {
        if (instance == null) {
            synchronized (FlowConnection.class) {
                if (instance == null) {
                    instance = new FlowConnection(context);
                }
            }
        }
        return instance;
    }

    /**
     * Initialise the message and event handlers
     * incoming messages, events and incoming auto response
     */
    private void initAsyncMessageHandlers() {
        asyncMessageHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                MessagingEvent messagingEvent = (MessagingEvent)msg.obj;
                asyncMessageListener.onMessageReceived(messagingEvent);
            }
        };

        asyncResponseHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                MessagingEvent messagingEvent = (MessagingEvent)msg.obj;
                asyncMessageListener.onResponseReceived(messagingEvent);
            }
        };

        eventReceivedHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                MessagingEvent messagingEvent = (MessagingEvent)msg.obj;

                System.out.println("Event received from: " + messagingEvent.sender);
                System.out.println("category: " + messagingEvent.category);
                if (messagingEvent.getContent() != null)
                    System.out.println("Content: " + messagingEvent.getContent());


                for (SubscribedEventListener listener : eventListeners) {
                    if (listener.isSubscribedToDeviceEvent(messagingEvent.sender, messagingEvent.category)) {
                        listener.onEventReceived(messagingEvent);
                    }
                }

            }
        };
    }

    /**
     * Subscribe the message handler to their corresponding events
     * Networking (I think?)
     */
    public void subscribeAsyncMessages() {
        flowInstance.subscribe(getUserFlowHandler(), getUserAOR(),
                MessagingEvent.MessagingEventCategory.FLOW_MESSAGING_EVENTCATEGORY_ASYNC_MESSAGE, "",
                1200, asyncMessageHandler);

        flowInstance.subscribe(getUserFlowHandler(), getUserAOR(),
                MessagingEvent.MessagingEventCategory.FLOW_MESSAGING_EVENTCATEGORY_ASYNC_MESSAGE_RESPONSE, "",
                1200, asyncResponseHandler);

    }

    /**
     * Subscribe a to a specific event for a specific device
     * Networking (I think?)
     * @param event Event to subscribe to
     * @param AoR Address of the device to subscribe to
     */
    public void subscribeToDeviceEvent(MessagingEvent.MessagingEventCategory event, String AoR) {
        System.out.println("Subscribing: " + event.toString());
        flowInstance.subscribe(getUserFlowHandler(), AoR, event, "", 1200, eventReceivedHandler);
    }

    /**
     * Add an object that will act as an event listener
     * @param listener Object to add
     */
    public void addEventListener(SubscribedEventListener listener) {
        if (!eventListeners.contains(listener))
            eventListeners.add(listener);
    }

    /**
     * Remove an event listening object
     * @param listener Object to be removed
     */
    public void removeEventListener(SubscribedEventListener listener) {
        System.out.println("Removed listener");
        if (eventListeners.contains(listener))
            eventListeners.remove(listener);
    }

    /**
     * Get the logged in users FlowMessaging ID
     * Networking
     * @return String FlowMessaging ID
     */
    public String getUserAOR() {
        User user = Core.getDefaultClient().getLoggedInUser();
        return user.getFlowMessagingAddress().getAddress();
    }

    /**
     * Get the logged in users Flow ID
     * Networking
     * @return String FlowID
     */
    public String getUserID() {
        User user = Core.getDefaultClient().getLoggedInUser();
        return user.getUserID();
    }

    /**
     * Get the current instance of Flow
     * @return Flow Flow Instance
     */
    public Flow getFlowInstance(){
        return flowInstance;
    }

    /**
     * Get the Flow User handler
     * @return FlowHandler User Handler
     */
    public FlowHandler getUserFlowHandler() {
        return userFlowHandler;
    }

    /**
     * Set the sessions user credentials
     * @param username Username
     * @param password Password
     */
    public void setUserCredentials(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Clear the sessions user credentials
     */
    public void clearUserCredentials() {
        username = null;
        password = null;
    }

    /**
     * Get the sessions saved username
     * @return String Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get the sessions saved password
     * @return String password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Build the XML used to initialise the flow server connection
     * @param server HTTP REST server URL
     * @param oAuthKey Server oAuth key
     * @param oAuthSecret Server oAuth secret
     * @return String The built XML
     */
    public String getInitXML(String server, String oAuthKey, String oAuthSecret) {
        return "<?xml version=\"1.0\"?>" +
                "<Settings>" +
                "<Setting>" +
                "<Name>restApiRoot</Name>" +
                "<Value>" + server + "</Value>" +
                "</Setting>" +
                "<Setting>" +
                "<Name>licenseeKey</Name>" +
                "<Value>" + oAuthKey + "</Value>" +
                "</Setting>" +
                "<Setting>" +
                "<Name>licenseeSecret</Name>" +
                "<Value>" + oAuthSecret + "</Value>" +
                "</Setting>" +
                "<Setting>" +
                "<Name>configDirectory</Name>" +
                "<Value>/mnt/img_messagingtest/outlinux/bin/config</Value>" +
                "</Setting>" +
                "</Settings>";
    }

    /**
     * Test the server url, make sure it exists/is online
     * Networking
     * @param server Url to test
     * @return boolean Success
     */
    public boolean testServerUrl(String server) {
        try {
            URL url = new URL(server);
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.connect();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the live list of devices owned/reachable by this user
     * Networking
     * @return List<Device> List of devices
     */
    public List<Device> getUserDevices(){
        Devices devices = Core.getDefaultClient().getLoggedInUser().getOwnedDevices();
        refreshDeviceCache(devices);
        return deviceCache;
    }

    /**
     * Update the device cache to represent the passed collection
     * @param devices Collection of devices
     */
    private void refreshDeviceCache(Devices devices){
        for (Device device : deviceCache) {
            if (!devices.contains(device))
                deviceCache.remove(device);
        }

        for (Device device : devices) {
            if (!deviceCache.contains(device))
                deviceCache.add(device);
        }
    }

    /**
     * Get the cached list of devices
     * @return List<Device> Cached list of devices
     */
    public List<Device> getDeviceCache(){
        return deviceCache;
    }

    /**
     * Set the currently connected device
     * @param currentDevice Device to set
     */
    public void setCurrentDevice(Device currentDevice) {
        this.currentDevice = currentDevice;
    }

    /**
     * Get the currently connected device
     * @return Device Connected device
     */
    public Device getCurrentDevice() {
        return currentDevice;
    }

    /**
     * Set the class to be used as the message listener
     * @param asyncMessageListener Class that implements AsyncMessageListener to be used
     */
    public void setAsyncMessageListener(AsyncMessageListener asyncMessageListener) {
        this.asyncMessageListener = asyncMessageListener;
    }

}
