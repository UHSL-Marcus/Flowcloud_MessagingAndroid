package com.uhsl.flowmessage.flowmessagev2.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Marcus on 19/02/2016.
 */
public class ConfigSettings {

    public static final String SETTINGS = "SETTINGS";
    public static final String DEFAULT_VALUE = "EMPTY";

    public static final String SERVER = "SERVER";
    public static final String OAUTH_KEY = "OAUTH_KEY";
    public static final String OAUTH_SECRET = "OAUTH_SECRET";

    /**
     * Cleat the config
     * @param context Context that the method is called in, usually an activity
     */
    public static void clearAll(Context context) {
        context.getSharedPreferences(ConfigSettings.SETTINGS, Context.MODE_PRIVATE).edit().clear().commit();
    }

    /**
     * Save the server config settings
     * @param context Context that the method is called in, usually an activity
     * @param sever HTTP REST server url
     * @param oAuthKey Server oAuth key
     * @param oAuthSecret Server oAuth secret
     */
    public static void saveServerSettings(Context context, String sever, String oAuthKey, String oAuthSecret) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(ConfigSettings.SETTINGS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ConfigSettings.OAUTH_KEY, oAuthKey);
        editor.putString(ConfigSettings.OAUTH_SECRET, oAuthSecret);
        editor.putString(ConfigSettings.SERVER, sever);
        editor.apply();
    }

    /**
     * Check if all the server setting are present
     * @param context Context that the method is called in, usually an activity
     * @return boolean Settings present
     */
    public static boolean checkServerSettings(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(ConfigSettings.SETTINGS, Context.MODE_PRIVATE);
        return sharedPreferences.contains(ConfigSettings.OAUTH_KEY) &&
            sharedPreferences.contains(ConfigSettings.OAUTH_SECRET) &&
            sharedPreferences.contains(ConfigSettings.SERVER);
    }

    /**
     * Get a string setting from the config
     * @param context Context that the method is called in, usually an activity
     * @param key The setting key
     * @return String the setting value
     */
    public static String getStringSetting(Context context, String key) {
        return context.getSharedPreferences(ConfigSettings.SETTINGS, Context.MODE_PRIVATE)
                .getString(key, ConfigSettings.DEFAULT_VALUE);
    }



}
