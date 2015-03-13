package com.brucegiese.perfectposture;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;



public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

    @Override
    public void onResume() {
        super.onResume();

        // TODO: check the settings to see if they've changed.  Another instance can change them.
        // Probably need to save the View from onCreateView() and then check the individual
        // settings here manually, one-by-one.

    }

}