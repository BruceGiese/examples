package com.brucegiese.perfectposture;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This fragment holds a dynamic display showing the device's orientation relative to gravity.
 * It includes a start/stop button and shows data as X,Y,Z axis values.
 */
public class TiltFragment extends Fragment {
    private static final String TAG = "com.brucegiese.tiltdetector.tiltfragment";
    private static final int REPEAT_INTERVAL = 1000;    // units of milliseconds
    private static final String SAVED_BUTTON_STATE = "savedButtonState";
    private Orientation mOrientation = null;
    private View mView;
    private Handler mHandler;
    private boolean mButtonState = false;

    public TiltFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( savedInstanceState != null) {
            mButtonState = savedInstanceState.getBoolean(SAVED_BUTTON_STATE, false);
        }

        setUpOrientation();
        mHandler = new Handler();   // Created on the UI thread, so it runs on the UI thread.
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_tilt, container, false);

        if( mButtonState ) {        // This could be on after a configuration change
            turnOnOrientation();
            Button button = (Button)mView.findViewById(R.id.start_stop_button);
            button.setText(R.string.stop_tilt_detection);
        }

        Button button = (Button)mView.findViewById(R.id.start_stop_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
           public void onClick(View v) {
                if( mButtonState ) {
                    turnOffOrientation();
                    doRepeatingWork();  // clear out the data on the UI
                    ((Button)v).setText(R.string.start_tilt_detection);
                    mButtonState = false;
                } else {
                    turnOnOrientation();
                    ((Button)v).setText(R.string.stop_tilt_detection);
                    mButtonState = true;
                }
            }
        });
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if( mButtonState ) {
            turnOnOrientation();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if( mButtonState ) {
            turnOffOrientation();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_BUTTON_STATE, mButtonState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mOrientation = null;        // we create these for every configuration change
    }


    /**
    * @hide
    * This creates a periodic timer which makes a UI thread level call.
    */
    Runnable mDoPeriodicUiWork = new Runnable() {   // must be executed in the UI thread
        @Override
        public void run() {
            doRepeatingWork();
            mHandler.postDelayed(mDoPeriodicUiWork, REPEAT_INTERVAL);   // keep repeating
        }
    };

    private void doRepeatingWork() {
        if( mView != null) {
            TextView t;
            t = (TextView)mView.findViewById(R.id.x_axis_value);
            t.setText(mOrientation.getX());

            t = (TextView)mView.findViewById(R.id.y_axis_value);
            t.setText(mOrientation.getY());

            t = (TextView)mView.findViewById(R.id.z_axis_value);
            t.setText(mOrientation.getZ());

            t = (TextView)mView.findViewById(R.id.temperature);
            t.setText(mOrientation.getTemp());

            t = (TextView)mView.findViewById(R.id.humidity);
            t.setText(mOrientation.getHumidity());

            // TODO: check the ranges and send a notification for bad posture. Use a Posture object?

        } else {
            // Throw an exception to stop it
            throw new RuntimeException("Repeating work was left running after onDestroyView");
        }
    }


    /*
    *   Orientation Methods
    */
    private void setUpOrientation() {
        if( mOrientation == null) {
            mOrientation = new Orientation(getActivity().getApplicationContext());
        }
    }

    private void turnOnOrientation() {
        if( mOrientation.startOrienting() ) {
            mDoPeriodicUiWork.run();        // start repeated work
        } else {
            Log.i(TAG, "No gravity sensor on this device");
            Toast.makeText(getActivity(), R.string.no_gravity_sensor, Toast.LENGTH_SHORT );
        }
    }

    private void turnOffOrientation() {
        mHandler.removeCallbacks(mDoPeriodicUiWork);
        mOrientation.stopOrienting();
    }

}
