package com.uhsl.flowmessage.flowmessagev2;

import android.content.Intent;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import com.imgtec.flow.MessagingEvent;
import com.imgtec.flow.client.users.Device;
import com.uhsl.flowmessage.flowmessagev2.flow.FlowController;
import com.uhsl.flowmessage.flowmessagev2.flow.SubscribedEventListener;
import com.uhsl.flowmessage.flowmessagev2.utils.ActivityController;
import com.uhsl.flowmessage.flowmessagev2.utils.AsyncCall;
import com.uhsl.flowmessage.flowmessagev2.utils.BackgroundTask;
import com.uhsl.flowmessage.flowmessagev2.utils.DeviceConnectionEvent;
import com.uhsl.flowmessage.flowmessagev2.utils.DeviceConnectionState;
import com.uhsl.flowmessage.flowmessagev2.utils.DeviceConnectionStateMachine;
import com.uhsl.flowmessage.flowmessagev2.utils.GlobalStatic;
import com.uhsl.flowmessage.flowmessagev2.utils.HeartbeatFormat;
import com.uhsl.flowmessage.flowmessagev2.utils.KVSValueFormat;
import com.uhsl.flowmessage.flowmessagev2.utils.StaticSettingStoreKeys;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;


public class ChooseDeviceActivity extends AppCompatActivity implements BackgroundTask.Callback<List<Device>> {

    private FlowController flowController;
    private Handler handler = new Handler();
    private Device selectedDevice;
    private String selectedDeviceID;

    private List<String[]> deviceList = new CopyOnWriteArrayList<>();
    private ArrayAdapter<String[]> deviceListAdapter;

    private SubscribedEventListener eventListener;
    private Map<String, DeviceConnectionStateMachine> connectionStateMachineMap = new HashMap<>();
    private Map<DeviceConnectionStateMachine, Date> lastHeartBeat = new HashMap<>();
    private Timer heartbeatTimeout;

    private Button connectBtn;
    private ListView listView;

    /*********************************************************************************************/
    /*********************************Overridden android methods**********************************/
    /*********************************************************************************************/


    /**
     * {@inheritDoc}
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_device);

        // Set the flow controller instance
        flowController = FlowController.getInstance(this);

        // using the custom app toolbar
        setSupportActionBar((Toolbar) findViewById(R.id.choose_device_toolbar));
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // grab the views we need
        connectBtn = (Button) findViewById(R.id.choose_device_connect_btn);
        connectBtn.setEnabled(false);

        listView = (ListView) findViewById(R.id.choose_device_listView);

        // fill the list of devices
        fillDeviceList();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onPause() {
        super.onPause();

        // cancel the heartbeat timeout, no need for another thread to keep running while this activity is paused
        heartbeatTimeout.cancel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onResume() {
        super.onResume();

        //Start the heartbeat timeout.
        timeoutStart();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        // inflate our custom toolbar menu
        getMenuInflater().inflate(R.menu.settings_logout_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // do the approproate actions depending on the item pressed
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

    /*********************************************************************************************/
    /*********************************View Action Callback Methods********************************/
    /*********************************************************************************************/

    /**
     * Connect to the selected device
     *
     * @param view The invoking view
     */
    public void doConnectToDevice(View view) {
        if (selectedDevice != null){
            flowController.setConnectedDevice(selectedDevice);
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(MainActivity.NEW_CONNECTION, true);
            this.startActivity(intent);
        }


    }

    /*********************************************************************************************/
    /*********************************Device List Methods*****************************************/
    /*********************************************************************************************/

    /**
     * Resets the previously selected item from before the ListView was redrawn, if there was one.
     */
    private void setPreviouslySelectedItem() {
        if (selectedDeviceID != null) {
            final int pos = ((ThreeLineOptionalHiddenArrayAdapter) listView.getAdapter())
                    .getItemPosition(null, selectedDeviceID, null);

            listView.setItemChecked(pos, true); //TODO doesn't work
        }
    }

    /**
     * Sets the state of the connection button based on the connection state of the selected device
     */
    private void setConnectBtnState() {
        // grab the position of the selected device in the list adapter
        int pos = ((ThreeLineOptionalHiddenArrayAdapter) listView.getAdapter())
                .getItemPosition(null, selectedDeviceID, null);
        // get the device's view from the list
        View view = listView.getChildAt(pos);

        // default to false
        connectBtn.setEnabled(false);

        if (view != null) {
            // get the ViewHolder containing the current state of this view
            ThreeLineOptionalHiddenArrayAdapter.ViewHolder viewHolder =
                    (ThreeLineOptionalHiddenArrayAdapter.ViewHolder) view.getTag();

            // enable if needed
            if (viewHolder.active)
                connectBtn.setEnabled(true);
        }

    }
    /**
     * Start the task to fill the device list
     */
    private void fillDeviceList() {
        selectedDevice = null;

        // placeholder text in case nothing happens
        deviceList.add(new String[]{"No Devices", "", ""});

        // initialise the adapter
        deviceListAdapter = new ThreeLineOptionalHiddenArrayAdapter
                (ChooseDeviceActivity.this, deviceList, false, true, false);
        listView.setAdapter(deviceListAdapter);

        BackgroundTask.call(new AsyncCall<List<Device>>() {
            @Override
            public List<Device> call() {
                // request devices in a background thread
                return flowController.requestDevices(true);
            }
        }, this, 3);
    }

    /**
     * Result of the background task execution
     *
     * @param result Result of the async execution
     * @param task Task ID
     */
    public void onBackGroundTaskResult(final List<Device> result, int task) {
        if (task == 3) {

            try {
                // build the ArrayList of devices from result
                deviceList.clear();
                for (Device device : result) {
                    deviceList.add(new String[]{device.getDeviceName(),
                            device.getFlowMessagingAddress().getAddress(), "Offline"});

                    // initialise the state machine for this device
                    deviceConnectionState(device);

                }

                // set the list adapter to show the new info
                deviceListAdapter.notifyDataSetChanged();
                setPreviouslySelectedItem();

            } catch (Exception e) {
                System.out.println("get device exception: " + e.toString() + " -> " + e.getMessage());
            }




            // Set the onclick listener for the ListView
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {

                    ThreeLineOptionalHiddenArrayAdapter.ViewHolder viewHolder =
                            (ThreeLineOptionalHiddenArrayAdapter.ViewHolder) view.getTag();

                        // uses networking, hence staying on the background thread
                        for (Device device : result) {
                            String AoR = device.getFlowMessagingAddress().getAddress();
                            if (AoR.equals(viewHolder.lineTwo.getText())) {
                                selectedDevice = device;
                                selectedDeviceID = AoR;
                                break;
                            }
                        }

                        // change state of the connect button on the UI Thread
                        if (selectedDevice != null) {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setConnectBtnState();
                                }
                            });
                        }
                }

            });

        }

    }

    /*********************************************************************************************/
    /*********************************Device State Control****************************************/
    /*********************************************************************************************/

    /**
     * Set the device connection status display
     *
     * @param AoR ID of the device
     * @param status The status display string
     * @param enabled Is the device active
     */
    private void setDeviceStatus(String AoR, String status, final boolean enabled) {
        // get the device position in the adapter
        final int pos = ((ThreeLineOptionalHiddenArrayAdapter) listView.getAdapter())
                .getItemPosition(null, AoR, null);


        // grab the device view in the ListView
        View view = listView.getChildAt(pos);

        // get the ViewHolder containing the view state
        ThreeLineOptionalHiddenArrayAdapter.ViewHolder viewHolder =
                (ThreeLineOptionalHiddenArrayAdapter.ViewHolder) view.getTag();

        // set whether this device is active
        viewHolder.active = enabled;

        // find the current device in the list of devices and update its status string
        for (String[] entry : deviceList) {
            if (entry[1].equals(AoR)) {
                entry[2] = status;
            }
        }

        // update the ListView, adapter and connect button on UI thread
        handler.post(new Runnable() {
            @Override
            public void run() {
                //listView.getChildAt(pos).setEnabled(enabled);
                deviceListAdapter.notifyDataSetChanged();
                setConnectBtnState();
            }
        });
    }

    /**
     * Sets the connection state machine and the runnable methods for each state for the passed device
     *
     * @param device The device to whom the state machine belongs
     */
    private void deviceConnectionState(Device device) {

        //create the state machine
        final DeviceConnectionStateMachine connectionStateMachine = new DeviceConnectionStateMachine();

        // find the device address/ID
        final String deviceAoR = device.getFlowMessagingAddress().getAddress();

        // Set the callback runnable methods
        connectionStateMachine.setStateCallback(DeviceConnectionState.GOT_CONNECTION, new Runnable() {
            @Override
            public void run() {
                setDeviceStatus(deviceAoR, "Online", true);
                System.out.println("CONNECTION STATE");
            }
        });
        connectionStateMachine.setStateCallback(DeviceConnectionState.LOSING_CONNECTION, new Runnable() {
            @Override
            public void run() {
                setDeviceStatus(deviceAoR, "Losing Connection", true);
                System.out.println("LOSING CONNECTION STATE");
            }
        });
        connectionStateMachine.setStateCallback(DeviceConnectionState.LOST_CONNECTION, new Runnable() {
            @Override
            public void run() {
                setDeviceStatus(deviceAoR, "Offline", false);
                System.out.println("LOST CONNECTION STATE");
            }
        });
        connectionStateMachine.setStateCallback(DeviceConnectionState.NO_CONNECTION, new Runnable() {
            @Override
            public void run() {
                setDeviceStatus(deviceAoR, "Offline", false);
                System.out.println("NO CONNECTION STATE");
            }
        });

        // Store the state machine in the map
        connectionStateMachineMap.put(deviceAoR, connectionStateMachine);

    }

    /**
     * Start the connection timeout timer, each interval compare the last saved heartbeat for each
     * device (in UTC) with the current UTC time, call events on the state machines as needed.
     */
    private void timeoutStart() {
        // make sure only one timer is running
        if (heartbeatTimeout != null)
            heartbeatTimeout.cancel();

        heartbeatTimeout = new Timer();

        heartbeatTimeout.schedule(new TimerTask() {
            @Override
            public void run() {
                //System.out.println("heartbeatTimer CD");

                // loop each device
                List<Device> devices = flowController.requestDevices(false);
                for (Device device : devices) {

                    // grab the latest heartbeat
                    String lastHeartBeat = flowController.retrieveDeviceKeyValue(device,
                            StaticSettingStoreKeys.HEARTBEAT);

                    if (lastHeartBeat != null) {
                        try {
                            Date heartbeatTimestamp = HeartbeatFormat.parseHeartbeat(
                                    KVSValueFormat.parseValue(lastHeartBeat).body).timestamp;

                            DeviceConnectionStateMachine stateMachine = connectionStateMachineMap
                                    .get(device.getFlowMessagingAddress().getAddress());


                            // compare and fire off events
                            if ((new Date()).getTime() - heartbeatTimestamp.getTime() > GlobalStatic.HEARTBEAT_TIMEOUT)
                                stateMachine.doEvent(DeviceConnectionEvent.MISSED_HEARTBEAT);
                            else
                                stateMachine.doEvent(DeviceConnectionEvent.CONNECTED);


                        } catch (NullPointerException e) {
                            System.out.println("Malformed heartbeat value");
                        }
                    }

                }
            }
        }, 0, GlobalStatic.HEARTBEAT_CHECK);
    }

    /*********************************************************************************************/
    /*********************************Event handling Methods**************************************/
    /*********************************************************************************************/

    /**
     * Evaluate the received event
     */
    private void eventListen() {
        if (eventListener == null) {
            System.out.println("listener is null");
            eventListener = new SubscribedEventListener(flowController) {
                @Override
                public void onEventReceived(MessagingEvent messagingEvent) {
                    System.out.println("GOT AN EVENT");

                }
            };
        }

    }

}
