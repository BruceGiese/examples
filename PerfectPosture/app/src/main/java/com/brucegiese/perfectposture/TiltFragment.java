package com.brucegiese.perfectposture;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
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


            if( mOrientation.getZInt() > 4) {
                badPostureDetected();
            }

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


    /**
     * @hide
     *
     * Send a notification of bad posture to the user
     *
     */
    private void badPostureDetected() {
        // TODO: NOTES ON NEW FUNCTIONALITY:
        //      Notifications should NOT be used if the app is currently on screen
        //      Using LEDs to provide a specific notification might interfere with other stuff
        //      I should add a notification action so the user can return to the app
        // TODO: Remove this stuff
        NotificationManager nm = (NotificationManager) getActivity().getSystemService(getActivity().NOTIFICATION_SERVICE);
        NotificationCompat.Builder notif = new NotificationCompat.Builder(getActivity())
                .setContentTitle("TESTING")
                .setContentText("content text here")
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setLights(0xFFff0000, 200, 200);
        // Keep the default priority, PRIORITY_DEFAULT(0)

        // Use a fixed notification ID so we don't create endless notifications
        int LED_NOTIFICATION_ID = 1;
        Intent resultIntent = new Intent(getActivity(), PerfectPostureActivity.class);

        // Best Practice: ensure that all users can get to the functionality in the
        // Activity by having it start when the users click the notification.
        PendingIntent resultPendingIntent;

        // If the device supports preserving navigation when starting an Activity
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(getActivity());
            stackBuilder.addParentStack(PerfectPostureActivity.class);
            stackBuilder.addNextIntent(resultIntent);
            resultPendingIntent = stackBuilder.getPendingIntent(
                    0, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            // On older devices, don't bother preserving navigation
            resultPendingIntent =
                    PendingIntent.getActivity(getActivity(), 0, resultIntent, 0);
        }
        notif.setContentIntent(resultPendingIntent);
        nm.notify(LED_NOTIFICATION_ID, notif.build());
    }

}
