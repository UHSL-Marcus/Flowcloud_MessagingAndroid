package com.uhsl.flowmessage.flowmessagev2.flow;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import com.imgtec.flow.ErrorType;
import com.imgtec.flow.MessagingEvent;
import com.imgtec.flow.client.core.Core;
import com.imgtec.flow.client.core.NetworkException;
import com.imgtec.flow.client.users.Device;
import com.imgtec.flow.client.users.User;
import com.imgtec.flow.client.users.UserHelper;
import com.uhsl.flowmessage.flowmessagev2.utils.ConfigSettings;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Marcus on 19/02/2016.
 */
public class FlowController {

    private static volatile FlowController instance;

    private static boolean flowInit = false;
    private FlowConnection flowConnection;
    //private SharedPreferences sharedPreferences;
    Context context;

    /**
     * Get the instance of this singleton.
     * @param activity The activity calling the instance
     * @return Singleton instance
     */
    public static FlowController getInstance(Activity activity) {
        if (instance == null) {
            synchronized (FlowController.class) {
                if (instance == null) {
                    instance = new FlowController(activity);
                }
            }
        }
        return instance;
    }

    /**
     * Private contructor
     * @param activity Activity that called the creation of the instance
     */
    private FlowController(Activity activity) {
        flowConnection = FlowConnection.getInstance(activity);
        context = activity.getApplicationContext();
        //sharedPreferences = context.getSharedPreferences(ConfigSettings.SETTINGS, Context.MODE_PRIVATE);
    }

    /**
     * Initiaise flow if it is not all ready initialised.
     * @param activity The activity that called the method
     * @return boolean Success
     */
    public boolean initFlowIfNot(Activity activity) {
        boolean result = false;
        if (!flowInit) {
            result = flowInit(activity);
            if (result && Core.getDefaultClient().getAPI().hasSettings())
                flowInit = true;
        }

        return result;
    }

    /**
     * Reinitialise flow
     * @param activity The activity calling the method
     * @return boolean Success
     */
    public boolean reinitialiseFlow(Activity activity) {
        return shutdownFlow(false) && initFlowIfNot(activity);
    }

    /**
     * Start the flow shutdown process
     * @param clearSession true if this is a final shutdown, false if session data is still needed (like in a reinitialisation)
     * @return boolean Success
     */
    public boolean shutdownFlow(boolean clearSession) {

        if (logoutUser(clearSession) && flowConnection.getFlowInstance().shutdown())
            flowInit = false;

        return !flowInit;
    }

    /**
     * Log the current user out of flow
     * @param clearSession true to removed all session data, false to keep
     * @return boolean Success
     */
    public boolean logoutUser(boolean clearSession) {
        if (clearSession)
            flowConnection.clearUserCredentials();

        return flowConnection.getFlowInstance().logOut(flowConnection.getUserFlowHandler());
    }

    /**
     * Initialise flow
     * @param activity The calling activity
     * @return boolean Success
     */
    public boolean flowInit(Activity activity)  {
        String server = ConfigSettings.getStringSetting(activity, ConfigSettings.SERVER);
        return flowConnection.testServerUrl(server) &&
                flowConnection.getFlowInstance().init(
                    flowConnection.getInitXML(
                            server,
                            ConfigSettings.getStringSetting(activity, ConfigSettings.OAUTH_KEY),
                            ConfigSettings.getStringSetting(activity, ConfigSettings.OAUTH_SECRET)
                    )
                );
    }

    /**
     * Check if this session has saved user credentials
     * @return boolean true: has credentials, false: does not
     */
    public boolean hasSavedCredentials() {
        return flowConnection.getUsername() != null && flowConnection.getPassword() != null;
    }

    /**
     * Log a user into flow
     * @param username Username
     * @param password Password
     * @return boolean Success
     */
    public boolean userLogin(String username, String password) {
        if (!isUserLoggedIn()) {
            User user = UserHelper.newUser(Core.getDefaultClient());
            boolean loggedIn = flowConnection.getFlowInstance().userLogin(username, password, user, flowConnection.getUserFlowHandler());
            if (loggedIn) {
                flowConnection.setUserCredentials(username, password);
                flowConnection.subscribeAsyncMessages();
            }

            return loggedIn;
        }
        return true;
    }

    /**
     * Log in a user using saved credentials
     * @return boolean Success
     */
    public boolean userReLogin() {
        System.out.println("saved detils: " + flowConnection.getUsername() + " : " + flowConnection.getPassword());
        return userLogin(flowConnection.getUsername(), flowConnection.getPassword());
    }

    /**
     * Get the last flow error
     * @return ErrorType The last flow error
     */
    public ErrorType getLastFlowError() {
        return flowConnection.getFlowInstance().getLastError();
    }

    /**
     * Check if flow is set as initialised
     * @return boolean Flow initialised
     */
    public boolean isFlowInit() {
        return flowInit;
    }

    /**
     * Check if a user is logged in
     * Networking
     * @return boolean User logged in
     */
    public boolean isUserLoggedIn () {
        return Core.getDefaultClient().isUserLoggedIn();
    }

    /**
     * Request a list of devices owned/reachable by this user
     * Networking if refreshed is true
     * @param refreshed Refresh cached store with live list (uses networking if true)
     * @return List<Device> List of devices
     */
    public List<Device> requestDevices(boolean refreshed) {
        if (refreshed)
            return flowConnection.getUserDevices();

        return flowConnection.getDeviceCache();
    }

    /**
     * Set the connected device
     * @param device Device to set
     */
    public void setConnectedDevice(Device device) {
        flowConnection.setCurrentDevice(device);
    }

    /**
     * Get the currently connected device
     * @return Device Connected device
     */
    public Device getConnectedDevice() {
        return flowConnection.getCurrentDevice();
    }

    /**
     * Get the AoR of the currently connected device
     * @return AoR of device
     */
    public String getConnectedDeviceAoR() { return flowConnection.getCurrentDevice()
            .getFlowMessagingAddress().getAddress(); }

    /**
     * Set the listener for incoming messages
     * @param asyncMessageListener Class implementing AsyncMessageListener to act as the listener
     */
    public void setAsyncMessageListener(AsyncMessageListener asyncMessageListener) {
        flowConnection.setAsyncMessageListener(asyncMessageListener);
    }

    /**
     * Add an subscribe to a list of events for a specific device
     * @param listener The class which extends the SubscribedEventListener abstract class, to listen for these events
     * @param AoR The device address to listen to
     * @param event The event to listen for
     */
    public void addEventSubscription(SubscribedEventListener listener, String AoR, MessagingEvent.MessagingEventCategory event) {
        flowConnection.addEventListener(listener);
        flowConnection.subscribeToDeviceEvent(event, AoR);
    }

    /**
     * Remove an event listener which is no longer used
     * @param listener Listener to remove
     */
    public void removeEventListener(SubscribedEventListener listener) {
        flowConnection.removeEventListener(listener);
    }

    /**
     * Send a new message to another flow user or device
     * @param recipient The ID of the message recipient
     * @param message The message to send
     * @return boolean Success
     */
    public boolean sendAsyncMessage(String recipient, String message) {
        return flowConnection.getFlowInstance().sendAsyncMessage(flowConnection.getUserFlowHandler(),
                new String[]{recipient}, message);
    }

    /**
     * Get the flow messaging ID of the logged in user
     * @return String Messaging ID
     */
    public String getUserMessagingID() {
        return flowConnection.getUserAOR();
    }

    /**
     * Get the Flow User ID of the logged in user
     * @return String Flow ID
     */
    public String getUserID() {
        return flowConnection.getUserID();
    }

    /**
     * Retrieves data from the key value store for a device
     * @param device Device to whom the store belongs
     * @param key Value key
     * @return String The value or null if no value exists
     */
    public String retrieveDeviceKeyValue(Device device, String key) {
        if (device.hasSettings()) {
            return device.getSetting(key).getValue();
        }
        return null;
    }

}
