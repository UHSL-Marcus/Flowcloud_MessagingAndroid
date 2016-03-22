package com.uhsl.flowmessage.flowmessagev2;

import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.widget.Toolbar;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.imgtec.flow.MessagingEvent;
import com.imgtec.flow.client.users.Device;
import com.uhsl.flowmessage.flowmessagev2.flow.AsyncMessageListener;
import com.uhsl.flowmessage.flowmessagev2.flow.FlowController;
import com.uhsl.flowmessage.flowmessagev2.flow.SubscribedEventListener;
import com.uhsl.flowmessage.flowmessagev2.utils.ActivityController;
import com.uhsl.flowmessage.flowmessagev2.utils.AsyncRun;
import com.uhsl.flowmessage.flowmessagev2.utils.BackgroundTask;
import com.uhsl.flowmessage.flowmessagev2.utils.DeviceConnectionEvent;
import com.uhsl.flowmessage.flowmessagev2.utils.DeviceConnectionState;
import com.uhsl.flowmessage.flowmessagev2.utils.DeviceConnectionStateMachine;
import com.uhsl.flowmessage.flowmessagev2.utils.EventFormat;
import com.uhsl.flowmessage.flowmessagev2.utils.EventTypes;
import com.uhsl.flowmessage.flowmessagev2.utils.GlobalStatic;
import com.uhsl.flowmessage.flowmessagev2.utils.HeartbeatFormat;
import com.uhsl.flowmessage.flowmessagev2.utils.KVSValueFormat;
import com.uhsl.flowmessage.flowmessagev2.utils.MessageFormat;
import com.uhsl.flowmessage.flowmessagev2.utils.MessageTypes;
import com.uhsl.flowmessage.flowmessagev2.utils.SenderTypes;
import com.uhsl.flowmessage.flowmessagev2.utils.StaticSettingStoreKeys;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity implements AsyncMessageListener {

    public static String NEW_CONNECTION = "MainActivity_NewConnection";
    private boolean newConnection = false;

    private FlowController flowController;
    private ThreeLineOptionalHiddenArrayAdapter messageListAdapter;
    private List<String[]> messageList = new CopyOnWriteArrayList<>(); //[0]Body, [1]MessageID, [2]Status
    private ListView messagesListView;
    private Handler handler = new Handler();

    // Fields saved inside the retained fragment
    private MainActivityDataFragment savedState;

    private SubscribedEventListener eventListener;

    private Timer heartbeatTimeout;

    private DeviceConnectionStateMachine connectionStateMachine;

    // View fields
    private TextView connection_textView;
    private Button send_Button;


    /**********************************************************************/
    /*******************Overridden Android Methods*************************/
    /**********************************************************************/

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check the intent, see if this a new connection
        newConnection = getIntent().getBooleanExtra(NEW_CONNECTION, false);
        getIntent().removeExtra(NEW_CONNECTION);

        // set up the flow controller instance and listen for messages
        flowController = FlowController.getInstance(this);
        flowController.setAsyncMessageListener(this);

        // using the custom support actionbar rather than the built in one
        setSupportActionBar((Toolbar) findViewById(R.id.main_toolbar));

        // grab the views we need
        connection_textView = (TextView) findViewById(R.id.main_connection_info_textView);
        send_Button = (Button) findViewById(R.id.main_send_message_btn);
        messagesListView = (ListView) findViewById(R.id.main_message_listView);

        messageList.add(new String[] {"Line1", "line2", "line3"});

        // set up the adapter for the message ListView
        messageListAdapter = new ThreeLineOptionalHiddenArrayAdapter(this, messageList, false, true, false);

        // retrieve or create the fragment holding this activities state
        FragmentManager fm = getSupportFragmentManager();
        savedState = (MainActivityDataFragment) fm.findFragmentByTag("MainActivityState");

        if (savedState == null) {
            savedState = new MainActivityDataFragment();
            fm.beginTransaction().add(savedState, "MainActivityState").commit();
        }

        // set the adapter
        messagesListView.setAdapter(messageListAdapter);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();

        // import the state from the fragment
        importState();

        System.out.println("new: " + newConnection);

        // initialise the connection state machine if this is a new connection
        if (newConnection)
            initialConnectionState();

        // start the connection timeout if there is a device connected
        if (flowController.getConnectedDevice() != null)
            timeoutStart();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();

        // save the current state to the fragment
        saveState();

        // make sure this is falsified, unless the activity is started from someplace else,
        // the next create won't involve a new connection
        newConnection = false;

        // cancel the heartbeat timer if needed
        if (heartbeatTimeout != null)
            heartbeatTimeout.cancel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onStop() {
        super.onStop();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate our custom toolbar menu
        getMenuInflater().inflate(R.menu.settings_logout_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // check which item was selected and work accordingly
        if (item.getItemId() == R.id.toolbar_settings_item) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else if(item.getItemId() == R.id.toolbar_logout_item) {
            flowController.logoutUser(true);
            if (!flowController.isUserLoggedIn()) {
                ActivityController.changeActivity(this, new Intent(this, LoginActivity.class));
            } else {
                System.out.println("logout failed");
            }
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    /**********************************************************************/
    /*******************View Callback Methods******************************/
    /**********************************************************************/

    /**
     * Choose device button callback, open the choose device activity
     *
     * @param view The calling view
     */
    public void doChooseDeviceActivity(View view) {
        this.startActivity(new Intent(this, ChooseDeviceActivity.class));
    }

    /**
     * Send a message to the connected device through flow
     * Networking
     *
     * @param view The view which called the method
     */
    public void doSendMessage(View view) {

        BackgroundTask.run(new AsyncRun() {
            @Override
            public void run() {
                try {
                    // create the message format holding the info
                    MessageFormat newMessage = new MessageFormat(
                            UUID.randomUUID().toString(),
                            flowController.getUserID(), SenderTypes.USER, MessageTypes.TEXT_MESSAGE,
                            ((EditText) findViewById(R.id.main_input_message_editText)).getText().toString());

                    // build the message
                    String msg = MessageFormat.buildMessage(newMessage);
                    String recipient = flowController.getConnectedDevice().getFlowMessagingAddress().getAddress();

                    //Send
                    if (msg != null && flowController.sendAsyncMessage(recipient, msg))
                        addMessage("Me", msg, "Status: Sending...");
                    else addMessage("Me", buildFailedMessage(), "Status: Done");
                } catch (Exception e) {
                    System.out.println("Send message exception: " + e.toString() + " -> " + e.getMessage());
                }

            }
        });
    }

    /**********************************************************************/
    /*******************Activity State Control*****************************/
    /**********************************************************************/

    /**
     * Import the state from the fragment storing it
     */
    private void importState() {
        eventListener = savedState.getEventListener();
        connectionStateMachine = savedState.getConnectionStateMachine();

        if (!newConnection)
            messageListAdapter.importCollection(savedState.getMessageList());
    }

    /**
     * Save the state to the storage fragment
     */
    private void saveState() {
        savedState.setConnectionStateMachine(connectionStateMachine);
        savedState.setEventListener(eventListener);
        savedState.setMessageList(messageList);
    }

    /**********************************************************************/
    /*******************Connection State Control***************************/
    /**********************************************************************/

    /**
     * Set the display of the current connection state,
     * Set the message shown and the availability of the send button
     *
     * @param text Text to display
     * @param enabled send button enabled state
     */
    private void setConnectionDisplayState(final String text, final boolean enabled) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                connection_textView.setText(text);
                send_Button.setEnabled(enabled);
            }
        });
    }

    /**
     * Creates the state machine controlling the device connected state and links a callback runnable to each state.
     */
    private void initialConnectionState() {
        final Device device = flowController.getConnectedDevice();

        connectionStateMachine = new DeviceConnectionStateMachine();
        connectionStateMachine.setStateCallback(DeviceConnectionState.GOT_CONNECTION, new Runnable() {
            @Override
            public void run() {
                setConnectionDisplayState(device.getDeviceName() + "\n" +
                        "Connected", true);
            }
        });
        connectionStateMachine.setStateCallback(DeviceConnectionState.LOSING_CONNECTION, new Runnable() {
            @Override
            public void run() {
                setConnectionDisplayState(device.getDeviceName() + "\n" +
                        "Losing Connection", true);
            }
        });
        connectionStateMachine.setStateCallback(DeviceConnectionState.LOST_CONNECTION, new Runnable() {
            @Override
            public void run() {
                setConnectionDisplayState(device.getDeviceName() + "\n" +
                        "Connection Lost", false);
            }
        });
        connectionStateMachine.setStateCallback(DeviceConnectionState.NO_CONNECTION, new Runnable() {
            @Override
            public void run() {
                setConnectionDisplayState("No Device Connected", false);
            }
        });

        // fire off the initial state
        connectionStateMachine.start();

        // if a device is actually connected, fire that event
        if (device != null) {
            connectionStateMachine.doEvent(DeviceConnectionEvent.CONNECTED);
        }
    }

    /**
     * Start the connection timeout timer, each interval compare the last saved heartbeat for this
     * device (in UTC) with the current UTC time, call events on the state machine as needed.
     */
    private void timeoutStart() {
        // make sure only one timer is running
        if (heartbeatTimeout != null)
            heartbeatTimeout.cancel();

        heartbeatTimeout = new Timer();

        heartbeatTimeout.schedule(new TimerTask() {
            @Override
            public void run() {
                //System.out.println("heartbeatTimer MN");

                // grab the latest heartbeat
                String lastHeartBeat = flowController.retrieveDeviceKeyValue(flowController.getConnectedDevice(),
                        StaticSettingStoreKeys.HEARTBEAT);

                if (lastHeartBeat != null) {
                    try {
                        Date heartbeatTimestamp = HeartbeatFormat.parseHeartbeat(
                                KVSValueFormat.parseValue(lastHeartBeat).body).timestamp;

                        //System.out.println("time: " + ((new Date()).getTime() - heartbeatTimestamp.getTime()));

                        // compare and fire off events
                        if ((new Date()).getTime() - heartbeatTimestamp.getTime() > GlobalStatic.HEARTBEAT_TIMEOUT)
                            connectionStateMachine.doEvent(DeviceConnectionEvent.MISSED_HEARTBEAT);
                        else
                            connectionStateMachine.doEvent(DeviceConnectionEvent.CONNECTED);


                    } catch (NullPointerException e) {
                        System.out.println("Malformed heartbeat value");
                    }
                }
            }

        }, 0, GlobalStatic.HEARTBEAT_CHECK); // Check right away initially, then every set interval
    }

    /**********************************************************************/
    /*******************Message Methods************************************/
    /**********************************************************************/

    /**
     * Adds a message to the list view of received messages
     *
     * @param sender Message sender
     * @param content Message content
     * @param status Status of the message (sent, failed, sending, received)
     */
    private void addMessage(final String sender, final String content, final String status) {

        System.out.println("*****New message to add*****\n\nSender: " + sender + "\n\nContent: " +
                content + "\n\nStatus: " + status + "\n\n**********");

        try {
            final MessageFormat message = MessageFormat.parseMessage(content);
            System.out.println("*****Message parsed*****" +
                    "\n\nSenderID: " + message.senderID +
                    "\n\nSenderType: " + message.senderType +
                    "\n\nType: " + message.type +
                    "\n\nBody: " + message.body +
                    "\n\n**********");

            // update the message list and message adapter on the UI thread.
            handler.post(new Runnable() {
                @Override
                public void run() {
                    messageList.add(new String[]{sender + ": " + message.body, message.messageID, status});
                    messageListAdapter.notifyDataSetChanged();
                    messagesListView.smoothScrollToPosition(messageListAdapter.getCount() - 1);
                }
            });
        } catch (Exception e) {
            System.out.println("malformed message");
        }

    }

    /**
     * Handler for received messages from flow
     *
     * @param messagingEvent Holds the message information
     */
    @Override
    public void onMessageReceived(MessagingEvent messagingEvent) {
        System.out.println("RECEIVED MESSAGE");
        addMessage(messagingEvent.sender, messagingEvent.content, "");
    }

    /**
     * Handler for sent message auto responses
     * @param response Holds the response information
     */
    @Override
    public void onResponseReceived(MessagingEvent response) {
        System.out.println("received response: " + response.messageResponse);
        String status;
        // resolve the response
        switch (response.messageResponse) {
            case SEND_SUCCESS:
                status = "Received"; break;
            case SENT_BUT_NOT_DELIVERED:
                status = "Sent"; break;
            case SEND_BUFFER_FULL:
                status = "Device Buffer Full"; break;
            case SEND_FAILED:
                status = "Failed"; break;
            default: status = ""; break;
        }

        try {
            MessageFormat newMessage = MessageFormat.parseMessage(response.content);

            // find the affected message in the list and update
            for (String[] message : messageList) {
                if (message[1].equals(newMessage.messageID)) {
                    message[2] = "Status: " + status;
                    break;
                }
            }
            messageListAdapter.notifyDataSetChanged();

            System.out.println("Received response: " + response.sender + " : " + response.content
                    + " : " + status);
        } catch (Exception e) {
            System.out.println("malformed message: response callback");
            System.out.println(e.toString() + " -> " + e.getMessage());
        }
    }

    /**
     * Build the standard failed message
     * @return The message
     */
    private String buildFailedMessage() {
        MessageFormat newMessage = new MessageFormat("0", "0", "0", "0", "Message Failed");

        return MessageFormat.buildMessage(newMessage);
    }

    /**********************************************************************/
    /*******************Received Event Methods************************/
    /**********************************************************************/


    /**
     * Evaluate the received event
     *
     * @param device Device from whom the event originates
     */
    private void eventListen(Device device) {
        if (eventListener == null) {
            System.out.println("listener is null");
            eventListener = new SubscribedEventListener(flowController) {
                @Override
                public void onEventReceived(MessagingEvent messagingEvent) {
                    System.out.println("GOT THE EVENT");
                    if (messagingEvent.content != null) {
                        try {
                            EventFormat event = EventFormat.parseMessage(messagingEvent.content);
                            if (event.type.equals(EventTypes.HEARTBEAT))
                                connectionStateMachine.doEvent(DeviceConnectionEvent.CONNECTED);

                        } catch (Exception e) {
                            System.out.println("unrecognised event");
                        }
                    }
                }
            };
        }
    }


}
