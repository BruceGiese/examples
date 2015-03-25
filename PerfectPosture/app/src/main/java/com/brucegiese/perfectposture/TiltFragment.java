package com.brucegiese.perfectposture;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * This fragment allows the user to start and stop the posture measurement service.
 */
public class TiltFragment extends Fragment {
    private static final String TAG = "com.brucegiese.tilt";
    private View mView;
    private Messenger mService;         // messenger for communicating to OrientationService
    private boolean mServiceConnected = false;
    private CheckStatusReceiver mCheckStatusReceiver;


    public TiltFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCheckStatusReceiver = new CheckStatusReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_tilt, container, false);
        checkAndSetButtonState();

        Button button = (Button) mView.findViewById(R.id.start_stop_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), OrientationService.class);
                if (OrientationService.sIsRunning) {
                    intent.setAction(OrientationService.TURN_OFF_SERVICE_ACTION);
                    ((Button) v).setText(R.string.start_tilt_detection);

                } else {
                    intent.setAction(OrientationService.TURN_ON_SERVICE_ACTION);
                    ((Button) v).setText(R.string.stop_tilt_detection);
                }
                getActivity().startService(intent);
            }
        });

        // Register the broadcast receiver
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(OrientationService.CHECK_STATUS_INTENT);
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mCheckStatusReceiver, iFilter);
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // In case of multiple instances running and other complex scenarios.
        checkAndSetButtonState();
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Un-register the broadcast receiver
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mCheckStatusReceiver);

        mView = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // If the service is not doing orientation, then stop the whole service
        if( ! OrientationService.sIsRunning) {
            Log.d(TAG, "onDestroy(): Stopping the service");
            Intent intent = new Intent(getActivity(), OrientationService.class);
            // make it persistent by calling this before attempting to bind
            getActivity().stopService(intent);
        }
    }


    /**
     * Check the global state of the service to see if it's running.  This is the most
     * reliable way of keeping track of the service without going into AIDL.
     */
    private void checkAndSetButtonState() {
        if( mView != null ) {
            Button button = (Button) mView.findViewById(R.id.start_stop_button);
            if( OrientationService.sIsRunning) {
                button.setText(R.string.stop_tilt_detection);
            } else {
                button.setText(R.string.start_tilt_detection);
            }
        }
    }


    /**
     * The service can be stopped by other means, so it will tell us when that happens.
     */
    class CheckStatusReceiver extends BroadcastReceiver {

        public CheckStatusReceiver() { }

        @Override
        public void onReceive(Context c, Intent i) {
            if( i.getAction().equals(OrientationService.CHECK_STATUS_INTENT)) {
                Log.d(TAG, "Rechecking the button state based on a broadcast from the service");
                checkAndSetButtonState();
            } else {
                Log.e(TAG, "Received an unexpected broadcast intent");
            }

        }
    }
}
