package com.brucegiese.perfectposture;

import android.os.Bundle;
import android.preference.PreferenceFragment;


// TODO: add a "clear data" button, which must be outside the PreferenceFragment, unfortunately

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);
    }

}