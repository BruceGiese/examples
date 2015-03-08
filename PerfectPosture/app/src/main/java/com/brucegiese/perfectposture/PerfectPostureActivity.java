package com.brucegiese.perfectposture;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
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
public class PerfectPostureActivity extends ActionBarActivity {
    private static final String TAG = "com.brucegiese.activity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfect_posture);

        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentById(R.id.fragment_container);

        if( f == null ) {
            TiltFragment tiltFragment = new TiltFragment();
            fm.beginTransaction().add(R.id.fragment_container, tiltFragment).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.perfect_posture_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();
        if (id == R.id.update_interval) {
            Log.d(TAG, "Menu item update_interval selected");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "The menu is about to be shown");

        return true;
    }
}
