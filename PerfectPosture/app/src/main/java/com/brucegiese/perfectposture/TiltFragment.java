package com.brucegiese.perfectposture;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
    private static final String TAG = "com.brucegiese.tiltfrag";
    private static final String SAVED_BUTTON_STATE = "savedButtonState";
    private View mView;
    private boolean mButtonState = false;
    private Messenger mService;
    boolean mBound = false;
    OrientationService mOrientationService = null;


    public TiltFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            // TODO: this could actually be wrong if we start the app while the service is running
            mButtonState = savedInstanceState.getBoolean(SAVED_BUTTON_STATE, false);
        }

        if( mOrientationService == null ) {
            mOrientationService = new OrientationService();
            Intent intent = new Intent(getActivity(), OrientationService.class);
            getActivity().startService(intent);     // make it persistent by calling this first
            getActivity().bindService(new Intent(getActivity(),
                    OrientationService.class), mConnection, getActivity().BIND_AUTO_CREATE);
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        mView = inflater.inflate(R.layout.fragment_tilt, container, false);
        Button button = (Button) mView.findViewById(R.id.start_stop_button);

        if (mButtonState) {        // This could be on after a configuration change
            button.setText(R.string.stop_tilt_detection);
        }

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mButtonState) {
                    stopOrientation();
                    ((Button) v).setText(R.string.start_tilt_detection);
                    mButtonState = false;
                } else {
                    startOrientation();
                    ((Button) v).setText(R.string.stop_tilt_detection);
                    mButtonState = true;
                }
            }
        });
        return mView;
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
        if( mBound ) {
            Log.d(TAG, "unbinding from the service");
            getActivity().unbindService(mConnection);
            Log.d(TAG, "done unbinding");
        }
        // We leave the service running, but we unbind from it.
        // TODO: If we start/stop the app, do we get multiple instances of the service object?
    }


    /*
    *       Orientation Related Stuff
    *
     */
    private void startOrientation() {
        if( !mBound ) {
            Log.e(TAG, "We never got the service running or never got connected to it");
        } else {
            Message msg = Message.obtain(null, OrientationService.MSG_START_MONITORING, 0, 0);
            try {
                mService.send(msg);
            } catch (Exception e) {
                Log.e(TAG, "Exception trying to send message to start orienting: ", e);
            }
        }
    }

    private void stopOrientation() {
        if( !mBound ) {
            Log.e(TAG, "Service was not already running when we went to stop it");
        } else {
            Message msg = Message.obtain(null, OrientationService.MSG_STOP_MONITORING, 0, 0);
            try {
                mService.send(msg);
            } catch( Exception e) {
                Log.e(TAG, "Exception trying to send message to stop orienting: ", e);
            }
        }
    }


    /**
     * This provides a means for getting a connection to the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // this is called when the connection with the service has been established,
            // giving us the object we can use for the service.  We need a client-side
            // representation of the Messenger from the raw IBinder object
            Log.d(TAG, "ServiceConnection: established connection with the service");
            mService = new Messenger(service);
            mBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            // Called when the connection with the service was unexpectedly disconnected
            Log.e(TAG, "ServiceConnection: The OrientationService probably crashed, onServiceDisconnected() called");
            mService = null;
            mBound = false;
        }
    };

}
