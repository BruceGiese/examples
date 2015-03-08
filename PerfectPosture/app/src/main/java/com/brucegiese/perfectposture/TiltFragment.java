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
    private static final int DEFAULT_REPEAT_INTERVAL = 1000;    // units of milliseconds
    private static final int DEFAULT_MAX_Z_VALUE = 4;
    private static final String SAVED_BUTTON_STATE = "savedButtonState";
    private View mView;
    private boolean mButtonState = false;

    public TiltFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( savedInstanceState != null) {
            mButtonState = savedInstanceState.getBoolean(SAVED_BUTTON_STATE, false);
        }
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
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_BUTTON_STATE, mButtonState);
        // TODO: I think this is going to stop working now (after moving to use a service)
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }



}
