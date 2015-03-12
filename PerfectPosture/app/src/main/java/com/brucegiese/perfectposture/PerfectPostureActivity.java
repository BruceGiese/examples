package com.brucegiese.perfectposture;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/**
 * This displays the orientation of the device with respect to gravity.
 * It can be enabled/disabled via a button and the values are displayed
 * from -10 to zero to +10 on the X, Y, and Z axes.  Despite the accuracy
 * of the math, it goes to 11 on some devices (e.g. mine).
 *
 * To create a posture detection application, this would presumably need to
 * run as a service, define acceptable ranges for posture, and alert the user
 * to bad posture and maybe consistently good posture.
 */
public class PerfectPostureActivity extends Activity {

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
}
