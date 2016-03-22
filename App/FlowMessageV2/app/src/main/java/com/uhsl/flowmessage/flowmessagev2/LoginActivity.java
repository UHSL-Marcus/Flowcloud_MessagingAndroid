package com.uhsl.flowmessage.flowmessagev2;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.uhsl.flowmessage.flowmessagev2.flow.FlowController;
import com.uhsl.flowmessage.flowmessagev2.utils.ActivityController;
import com.uhsl.flowmessage.flowmessagev2.utils.AsyncCall;
import com.uhsl.flowmessage.flowmessagev2.utils.AsyncRun;
import com.uhsl.flowmessage.flowmessagev2.utils.BackgroundTask;
import com.uhsl.flowmessage.flowmessagev2.utils.ConfigSettings;

public class LoginActivity extends AppCompatActivity implements BackgroundTask.Callback<Boolean> {

    private Handler handler = new Handler();
    private FlowController flowController;

    private EditText username_editText;
    private EditText password_editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        flowController = FlowController.getInstance(this);

        setSupportActionBar((Toolbar) findViewById(R.id.login_toolbar));
        ActionBar actionBar = getSupportActionBar();

        username_editText = (EditText) findViewById(R.id.login_username_editText);
        password_editText = (EditText) findViewById(R.id.login_password_editText);

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (true) { //TODO: check internet connectivity
            if (ConfigSettings.checkServerSettings(this))
                initialiseFlow(this);
            else {
                doSettingsSnackbar();
            }
        } else {
            //TODO: Snackbar -> no internet
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.settings_only_toolbar, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.toolbar_settings_item) {
            this.startActivity(new Intent(this, SettingsActivity.class));
            return true;
        } else
            return super.onOptionsItemSelected(item);
    }

    /**
     * Initialise the flow connection
     * @param activity This activity
     */
    private void initialiseFlow(final Activity activity) {

        BackgroundTask.run(new AsyncRun() {
            @Override
            public void run() {
                try {
                    final FlowController flowController = FlowController.getInstance(activity);
                    flowController.initFlowIfNot(activity);

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("flow is init " + flowController.isFlowInit());
                            if (flowController.isFlowInit()) {
                                System.out.println("userlogged in: " + flowController.isUserLoggedIn());
                                System.out.println("has saved: " + flowController.hasSavedCredentials());


                                if (flowController.isUserLoggedIn()) //TODO: maybe new connection? test
                                    ActivityController.changeActivity(activity, new Intent(activity, MainActivity.class));

                                if (flowController.hasSavedCredentials())
                                    flowLogin(true);


                            } else {
                                doSettingsSnackbar();
                            }

                        }
                    });


                } catch (Exception e) {
                    System.out.println("Exception: " + e.toString() + " -> " + e.getMessage());
                    //TODO: Something else has gone wrong
                }
            }

        });

    }

    /**
     * Display the snack bar prompting the user to go to server settings
     */
    private void doSettingsSnackbar() {

        ActivityController.showSnackbar(findViewById(R.id.login_coordinator_layout), "edit settings", "Settings",
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LoginActivity.this.startActivity(new Intent(LoginActivity.this,
                                SettingsActivity.class));
                    }
                }, handler);
    }


    /**
     * Start login
     * @param view Calling view
     */
    public void doLogin(View view) {
        if (flowController.isFlowInit())
            flowLogin(false);
        else
            doSettingsSnackbar();
    }

    /**
     * Login Actual
     * @param useSaved Use saved credentials or not
     */
    private void flowLogin(final boolean useSaved) {
        System.out.println("flow login: " + useSaved);
        //todo: validate, timeout using post delayed. cancel future. remove callbacks, logging in visual
        BackgroundTask.call(new AsyncCall<Boolean>() {
            @Override
            public Boolean call() {
                if (useSaved)
                    return flowController.userReLogin();

                return flowController.userLogin(username_editText.getText().toString(), password_editText.getText().toString());
            }
        }, this, 2);
    }

    /**
     * Called when BackgroundTask.call() finishes
     * @param result Result of the task
     * @param task Task ID, to identify the correct result
     */
    public void onBackGroundTaskResult(final Boolean result, final int task) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (task == 2) {
                    if (result) {
                        //todo: timeout callback remove
                        System.out.println("login good");
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.putExtra(MainActivity.NEW_CONNECTION, true);
                        ActivityController.changeActivity(LoginActivity.this,
                                intent);
                    } else {
                        System.out.println("bad login: " + flowController.getLastFlowError().toString());
                    }
                }
            }
        });
    }
}
