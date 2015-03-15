package com.brucegiese.perfectposture;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This displays the orientation of the device with respect to gravity.
 * It can be enabled/disabled via a button and has configurable
 * parameters as implemented in TiltFragment.
 *
 * This activity receives a data stream of posture values from the
 * TiltFragment and sends them to the GraphFragment for display.
 * The data stream is implemented as the DataSampleListener interface.
 */
public class PerfectPostureActivity extends Activity
        implements TiltFragment.DataSampleListener {
    private final static String TAG = "com.brucegiese.perfpost";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfect_posture);

        FragmentManager fm = getFragmentManager();
        Fragment f = fm.findFragmentById(R.id.fragment_container);
        if( f == null ) {
            TiltFragment tiltFragment = new TiltFragment();
            fm.beginTransaction().add(R.id.fragment_container, tiltFragment).commit();
        }
        Fragment fs = fm.findFragmentById(R.id.pref_container);
        if( fs == null ) {
            SettingsFragment settingsFragment = new SettingsFragment();
            fm.beginTransaction().add(R.id.pref_container, settingsFragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tilt, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * This implements the DataSampleListener interface in the TiltFragment.
     * This receives one data sample representing the current state of the user's
     * posture.
     *
     * @param value  a value representing the current posture.
     */
    @Override
    public void onDataSampleReceived(int value) {
        Log.d(TAG, "received data sample: " + value);
    }

}
