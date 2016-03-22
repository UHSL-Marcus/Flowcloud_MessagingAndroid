package com.uhsl.flowmessage.flowmessagev2.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.view.View;

/**
 * Created by Marcus on 22/02/2016.
 */
public class ActivityController {

    /**
     * Starts a new activity and finishes the previous one.
     * Used when there is no requirement to use the back key
     * @param activity Previous Activity
     * @param intent New Intent
     */
    public static void changeActivity(Activity activity, Intent intent) {
        activity.startActivity(intent);
        activity.finish();
    }

    /**
     * Show a Snackbar with no actions
     * @param view The view to tie the snackbar to (often a coordinated layout)
     * @param message The message to display
     * @param handler The UI thread handler for the calling activity (for when the Snackbar is called form another thread)
     */
    public static void showSnackbarNoAction(final View view, final String message, Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Show Snack bar with an action
     * @param view The view to tie the snackbar to (often a coordinated layout)
     * @param message The message to display
     * @param actionMessage The text for the action button
     * @param listener Listener method to handle the action
     * @param handler The UI thread handler for the calling activity (for when the Snackbar is called form another thread)
     */
    public static void showSnackbar(final View view, final String message, final String actionMessage,
                                    final View.OnClickListener listener, Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
                snackbar.setAction(actionMessage, listener);
                snackbar.show();
            }
        });
    }


}
