package com.brucegiese.perfectposture;


import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * This fragment allows the user to start and stop the posture measurement service.
 */
public class TiltFragment extends Fragment {
    private static final String TAG = "com.brucegiese.tiltfrag";
    private View mView;
    private boolean mButtonState = false;
    private Messenger mService;         // service for communicating with OrientationService
    OrientationService mOrientationService = null;
    boolean mBound = false;             // are we currently bound to the OrientationService


    public TiltFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                // This stuff is here in onClick() because we don't want to do much unless the
                // user actually initiates starting or stopping.
                if( mOrientationService == null ) {
                    // We need to connect to the service before we start or stop it
                    mOrientationService = new OrientationService();
                    Intent intent = new Intent(getActivity(), OrientationService.class);
                    // make it persistent by calling this first, then do the binding
                    getActivity().startService(intent);
                    getActivity().bindService(new Intent(getActivity(), OrientationService.class),
                            mConnection, getActivity().BIND_AUTO_CREATE);
                    if (mButtonState) {
                        Log.d(TAG, "we're shutting down");
                        ((Button) v).setText(R.string.start_tilt_detection);
                        mButtonState = false;
                    } else {
                        Log.d(TAG, "we're starting up");
                        ((Button) v).setText(R.string.stop_tilt_detection);
                        mButtonState = true;
                    }
                    // Wait until the ServiceConnection is established below, then send the message.

                } else {
                    // We're already connected to the service, so send the message now
                    if (mButtonState) {
                        Log.d(TAG, "already connected, shutting down");
                        stopOrientation();
                        ((Button) v).setText(R.string.start_tilt_detection);
                        mButtonState = false;

                    } else {
                        Log.d(TAG, "already connected, starting up");
                        startOrientation();
                        ((Button) v).setText(R.string.stop_tilt_detection);
                        mButtonState = true;
                    }
                }

            }
        });
        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        // In case of multiple instances running.
        checkAndSetButtonState();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mView = null;
    }


    /**
     * Check the global state of the service to see if it's running.  This is the most
     * reliable way of keeping track of the service without going into AIDL.
     */
    private void checkAndSetButtonState() {
        mButtonState = OrientationService.sCheckingIsRunning;
        if( mView != null ) {
            Button button = (Button) mView.findViewById(R.id.start_stop_button);
            if( mButtonState ) {
                button.setText(R.string.stop_tilt_detection);
            } else {
                button.setText(R.string.start_tilt_detection);
            }
        }
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
            Log.d(TAG, "ServiceConnection: established connection with the service");
            // this is called when the connection with the service has been established,
            // giving us the object we can use for the service.  We need a client-side
            // representation of the Messenger from the raw IBinder object

            mService = new Messenger(service);
            mBound = true;
            // We determine what to do based on the state of the button

            if( mButtonState ) {
                // start the monitoring
                startOrientation();

            } else {
                // shut down the monitoring
                stopOrientation();
                Log.d(TAG, "shutting down the service");
                Intent intent = new Intent(getActivity(), OrientationService.class);
                getActivity().stopService(intent);
                mOrientationService = null;
                mBound = false;
            }

        }

        public void onServiceDisconnected(ComponentName className) {
            // Called when the connection with the service was unexpectedly disconnected
            Log.e(TAG, "ServiceConnection: The OrientationService probably crashed, onServiceDisconnected() called");
            mService = null;
            mBound = false;
        }
    };
}
