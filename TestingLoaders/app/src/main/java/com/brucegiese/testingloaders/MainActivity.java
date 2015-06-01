package com.brucegiese.testingloaders;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;

import com.activeandroid.ActiveAndroid;

/**
 *  This is some investigation implementation based on this document:
 *  https://javacro.files.wordpress.com/2013/08/android-development-best-practices-ivan-gavran-calyx.pdf
 *
 *  Activity
 *  Fragment
 *  Loader
 *  SQLite Database (currently using ActiveAndroid)
 *  SyncService (basically an IntentService)
 *  REST Client
 *  Web Service
 *  Cloud data
 *
 */
public class MainActivity extends Activity
        implements PrePopulate.PrePopulateCallback,
                    LoaderFragment.OnFragmentInteractionListener {

    private static final String TAG = "testload.MainActivity";
    private PrePopulate mPrePopulate = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");

        setContentView(R.layout.activity_main);
        ActiveAndroid.initialize(this);

        FragmentManager fm = getFragmentManager();
        Fragment fragment = (Fragment) fm.findFragmentById(R.id.fragment_container);

        if( fragment == null ) {
            fragment = LoaderFragment.newInstance("This is a new fragment", 42);
            fm.beginTransaction().add(R.id.fragment_container, fragment).commit();
        }

        mPrePopulate = new PrePopulate(this, this);
    }


    protected void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "onDestroy()");
        mPrePopulate = null;
    }


    @Override
    public void onPrePopulateDone() {
        if( mPrePopulate != null ) {
            Log.d(TAG, "Done with PrePopulate");

        } else {
            Log.d(TAG, "Done with PrePopulate, but we're getting destroyed");
        }
    }


    /**
     *      This is simply a communication path from the fragment to the activity.
     * @param text      a string to print out to the log.
     */
    @Override
    public void whatsGoingOn(String text) {
        Log.i(TAG, "What's Going On: " + text);
    }

}
