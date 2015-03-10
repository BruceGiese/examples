package com.brucegiese.perfectposture;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a Service for tracking the user device's orientation periodically.
 * It notifies the user when they are outside of the range parameters for tilt orientation.
 * It keeps track of metrics which can be fetched.  It allows for changing the various
 * configuration parameters while the service is running.
 */
public class OrientationService extends Service {
    private static final String TAG = "com.brucegiese.service";
    public static final int MSG_START_MONITORING = 1;
    public static final int MSG_STOP_MONITORING = 2;
    public static final int MSG_DO_NOTHING = 3;

    private static final int DEFAULT_UPDATE_INTERVAL = 1;    // units of seconds
    private static final int DEFAULT_Z_AXIS_THRESHOLD = 4;

    private Orientation mOrientation = null;
    private ScheduledExecutorService mScheduler;
    private ScheduledFuture mScheduledFuture;

    private int mUpdateInterval = DEFAULT_UPDATE_INTERVAL;
    private int mZAxisThreshold = DEFAULT_Z_AXIS_THRESHOLD;
    private int mPercentBadPosture;      // 0 to 100

    private int debugCounter = 0;



    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OrientationService onCreate() called");
        if( mOrientation == null) {
            mOrientation = new Orientation(this);
        }
    }

    // Documentation says don't Override onStartCommand().  If you do, make sure to
    // return super.onStartCommand() with the same args.
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");
        return super.onStartCommand(intent, flags, startId);
    }


    /**
     * Handler of incoming messages from different instantiations of the application
     */
    class IncomingHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch( msg.what ) {

                case MSG_START_MONITORING:
                    Log.d(TAG, "got message: MSG_START_MONITORING");
                    startChecking();
                    break;

                case MSG_STOP_MONITORING:
                    Log.d(TAG, "got message: MSG_STOP_MONITORING");
                    stopChecking();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * Target we publish for the client to send messages to IncomingHandler
     */
    public final Messenger mMessenger = new Messenger( new IncomingHandler());

    /**
     * When binding to the service, we return an interface to our messenger for sending
     * messsages to the service
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called");
        return mMessenger.getBinder();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "OrientationService onDestroy() called");

        // Make absolutely sure that we've stopped everything and cleaned up.
        // This is guaranteed to be the last thing called.
        if( mScheduledFuture != null ) {
            Log.d(TAG, "...mScheduledFuture was not null");
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
        }
        if( mOrientation != null ) {
            mOrientation.stopOrienting();
            mOrientation = null;
        } else {
            Log.e(TAG, "mOrientation was null in onDestroy().  That should never happen.");
        }
    }



    /**
     *  For robustness purposes, this should be able to handle being called
     *  when the checking is already running.  Worst case, the results get reset.
     */
    private void startChecking() {
        try {
            mPercentBadPosture = 0;
            mOrientation.startOrienting();

            if (mScheduledFuture == null) {
                // We use an additional thread for the periodic execution task.

                mScheduler = Executors.newScheduledThreadPool(1);
                mScheduledFuture =
                        mScheduler.scheduleAtFixedRate(
                                mDoPeriodicWork,
                                mUpdateInterval,
                                mUpdateInterval,
                                TimeUnit.SECONDS);
            } else {
                Log.i(TAG, "startChecking() was called when checking was already running");
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception when starting orientation and scheduler: ", e);
        }
    }

    /**
     *  For robustness purposes, this should be able to handle being called
     *  when the checking has already been stopped.
     */
    private void stopChecking() {
        if( mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
            mOrientation.stopOrienting();
        } else {
            Log.i(TAG, "stopChecking() was called when checking wasn't running.");
        }
    }



    /**
     * @hide
     * This runs periodically in the background (yet another thread).
     * NOTE: This does NOT run on the same thread as the handleXXX stuff above!
     */
    Runnable mDoPeriodicWork = new Runnable() {   // must be executed in the UI thread
        @Override
        public void run() {
            int x = mOrientation.getX();
            int y = mOrientation.getY();
            int z = mOrientation.getZ();

            Log.d(TAG, "x=" + x + ", y=" + y + ", z=" + z);

            // TODO: REMOVE THIS: this stops the service after 15 seconds so we don't get stuck.
            if( debugCounter++ > 15) {
                debugCounter = 0;
                Log.d(TAG, "STOPPING THE SERVICE due to the debugCounter");
                stopChecking();
            }
        }
    };


}
