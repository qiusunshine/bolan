package com.pngcui.skyworth.dlna.datastore;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.hd.tvpro.R;

public class LocalConfigSharePreference {

    public static final String preference_name = "LocalConfigSharePreference";
    public static final String dev_name = "dev_name";

    public static boolean commintDevName(Context context, String devName) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(preference_name, 0);
        Editor editor = sharedPreferences.edit();
        editor.putString(dev_name, devName);
        editor.commit();
        return true;
    }

    public static String getDevName(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(preference_name, 0);
        return sharedPreferences.getString(dev_name, context.getResources().getString(R.string.app_name));
    }

}
