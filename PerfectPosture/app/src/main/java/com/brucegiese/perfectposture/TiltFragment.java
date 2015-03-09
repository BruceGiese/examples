package com.brucegiese.perfectposture;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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
    private static final String SAVED_BUTTON_STATE = "savedButtonState";
    private static final String SAVED_ORIENTATION_SERVICE = "savedOrientationService";
    private View mView;
    private boolean mButtonState = false;
    private OrientationService mOrientationService;
    private IBinder mBinder;

    public TiltFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if( savedInstanceState != null) {
            mButtonState = savedInstanceState.getBoolean(SAVED_BUTTON_STATE, false);
        }

        mOrientationService = new OrientationService();
        // TODO: Add the parameter handling here and link it to actual configuration
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_tilt, container, false);

        if( mButtonState ) {        // This could be on after a configuration change
            mOrientationService.startChecking(getActivity());
            Button button = (Button)mView.findViewById(R.id.start_stop_button);
            button.setText(R.string.stop_tilt_detection);
        }

        Button button = (Button)mView.findViewById(R.id.start_stop_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
           public void onClick(View v) {
                if( mButtonState ) {
                    mOrientationService.stopChecking(getActivity());
                    ((Button)v).setText(R.string.start_tilt_detection);
                    mButtonState = false;
                } else {
                    mOrientationService.startChecking(getActivity());
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


    /*
    *       Service Related Stuff
     */

    private class OrientationConnection implements ServiceConnection {

        public void onServiceConnected( ComponentName name, IBinder service) {
            mBinder = service;
        }

        public void onServiceDisconnected( ComponentName name) {
            // We really don't care about this
        }
    }



}
