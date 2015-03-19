package com.brucegiese.perfectposture;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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
    private DataSampleListener dataSampleListener;
    private View mView;
    private boolean mButtonState = false;
    private Messenger mToService;         // messenger for communicating to OrientationService
    private Messenger mFromService;       // messenger for getting msgs from OrientationService
    private boolean mServiceConnected = false;


    public TiltFragment() { }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            dataSampleListener = (DataSampleListener)activity;
        } catch( ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + "must implement onDataSampleReceived()");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The OS treats the service as a singleton and only calls the constructor once
        // no matter how many times we call startService().
        Intent intent = new Intent(getActivity(), OrientationService.class);
        // make it persistent by calling this before attempting to bind
        getActivity().startService(intent);
        getActivity().bindService(new Intent(getActivity(), OrientationService.class),
                    mConnection, getActivity().BIND_AUTO_CREATE);
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
                // We're already connected to the service, so send the message now
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
    public void onResume() {
        super.onResume();
        // In case of multiple instances running.
        checkAndSetButtonState();
    }

    @Override
    public void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mView = null;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(mConnection);
        // If the service is not doing orientation, then stop the whole service
        if( ! OrientationService.sInstance.checkIsRunning()) {
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
        mButtonState = OrientationService.sInstance.checkIsRunning();
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
        sendMessage(OrientationService.MSG_START_MONITORING);
    }

    private void stopOrientation() {
        sendMessage(OrientationService.MSG_STOP_MONITORING);
    }


    /**
     * This provides a means for getting a connection to the service.
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        /** this is called when the connection with the service has been established,
         * giving us the object we can use for the service.  We need a client-side
         * representation of the Messenger from the raw IBinder object
         */
        public void onServiceConnected(ComponentName className, IBinder service) {
            mServiceConnected = true;
            mToService = new Messenger(service);

            try {
                // Register this activity with the data sending service
                Message msg = Message.obtain(null, OrientationService.MSG_REGISTER_CLIENT);
                msg.replyTo = fromServiceMessenger;
                mToService.send(msg);
            } catch(RemoteException e) {
                Log.e(TAG, "Error registering Activity's messenger with service", e);
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // Called when the connection with the service was unexpectedly disconnected
            Log.e(TAG, "ServiceConnection: The OrientationService probably crashed, onServiceDisconnected() called");
            mToService = null;
            mServiceConnected = false;
        }
    };


    private void sendMessage(int message) { sendMessage(message, 0, 0); }
    private void sendMessage(int message, int arg1) { sendMessage(message, arg1, 0); }
    /**
     *
     * @param message
     * @param arg1 optional integer argument as part of normal messages
     * @param arg2 optional integer argument as part of normal messages
     */
    private void sendMessage(int message, int arg1, int arg2) {
        if (!mServiceConnected) {
            Log.e(TAG, "Service was not already running when we tried to send message " + message);
        } else {
            Message msg = Message.obtain(null, message, arg1, arg2);
            try {
                mToService.send(msg);
            } catch (Exception e) {
                Log.e(TAG, "Exception trying to send message " + message, e);
            }
        }
    }

    private class IncomingHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {

            switch( msg.what) {

                case OrientationService.MSG_DATA_SAMPLE:
                    dataSampleListener.onDataSampleReceived(msg.arg1);  // send to the Activity
                    break;

                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    }

    final Messenger fromServiceMessenger = new Messenger(new IncomingHandler());


    /**
     * The Activity must implement this interface in order to receive a stream
     * of data samples from this Fragment.  This data stream can be used for
     * a graphical display or whatever else is useful.
     */
    public interface DataSampleListener {
        public void onDataSampleReceived(int value);
    }

}
