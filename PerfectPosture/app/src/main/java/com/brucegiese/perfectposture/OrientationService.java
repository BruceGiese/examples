package com.brucegiese.perfectposture;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.os.IBinder;
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
public class OrientationService extends IntentService {
    private static final String TAG = "com.brucegiese.service";
    private static final int DEFAULT_UPDATE_INTERVAL = 1;    // units of seconds
    private static final int DEFAULT_Z_AXIS_THRESHOLD = 4;
    private Orientation mOrientation = null;
    private ScheduledExecutorService mScheduler;
    private ScheduledFuture mScheduledFuture;
    private int mUpdateInterval = DEFAULT_UPDATE_INTERVAL;
    private int mZAxisThreshold = DEFAULT_Z_AXIS_THRESHOLD;
    private int mPercentBadPosture;      // 0 to 100

    /*
    *   This uses the recommended method for implementing an IntentService.  A lot of the
    *   extra code comes from having to receive requests from the application and then
    *   sending that same information down to the background thread.  Fortunately, you
    *   can ignore most of this stuff and focus on the public methods below.
     */

    // Start the service (if it's not already started)
    private static final String ACTION_START = "com.brucegiese.perfectposture.action.START";
    // Stop the service
    private static final String ACTION_STOP = "com.brucegiese.perfectposture.action.STOP";
    // Get history, metrics, etc.
    private static final String ACTION_GET = "com.brucegiese.perfectposture.action.GET";
    // Set parameters
    private static final String ACTION_SET_PARAMS = "com.brucegiese.perfectposture.action.SET_PARAMS";

    private static final String EXTRA_UPDATE_INTERVAL
            = "com.brucegiese.perfectposture.extra.UPDATE_INTERVAL";
    private static final String EXTRA_Z_AXIS_THRESHOLD
            = "com.brucegiese.perfectposture.extra.Z_AXIS_THRESHOLD";


    // Broadcast receivers must create an Intent filter with this Action name to receive the results
    public static final String GET_ALL_RESULTS
            = "com.brucegiese.perfectposture.BROADCAST_TYPE_GET_ALL_RESULTS";
    // This is one of the results sent back to the broadcast receiver. % of overall time.
    public static final String PERCENT_BAD_POSTURE
            = "com.brucegiese.perfectposture.PERCENT_BAD_POSTURE";



    public OrientationService() {
        super("OrientationService");
        Log.d(TAG, "OrientationService created");
    }


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


    @Override
    public IBinder onBind(Intent intent) {
        // TODO: need to support stopping the service here.  Also getting results.
        return null;
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
            Log.d(TAG, "...mOrientation was not null");
            mOrientation.stopOrienting();
            mOrientation = null;
        }
    }



    /**
     * Start the background service running.  It will continue running after the app terminates.
     * This will immediately send out a notification showing the service is running.  If the
     * user clicks on the notification, it will re-open the application allowing the user to
     * stop the service, if desired.
     *
     * This service also sends another notification if they are holding the device outside
     * the range of allowed orientation.
     *
     * @param context
     */
    public static void startChecking(Context context) {
        Log.d(TAG, "OrientationService startChecking() called");

        Intent intent = new Intent(context, OrientationService.class);
        intent.setAction(ACTION_START);
        context.startService(intent);
    }

    /**
     * Stop the background service running.  There must always be a way for the user to call this!
     * @param context
     */
    public static void stopChecking(Context context) {
        Log.d(TAG, "OrientationService stopChecking() called");
        Intent intent = new Intent(context, OrientationService.class);
        intent.setAction(ACTION_STOP);
        context.startService(intent);
    }

    /**
     * Get any results and metrics back from the service sent to a broadcast receiver.
     *
      * @param context
     */
    public static void getResults(Context context) {
        Intent intent = new Intent(context, OrientationService.class);
        intent.setAction(ACTION_GET);
        context.startService(intent);
    }

    /**
     * Set the configurable parameters while the service is running.
     *
     * @param context
     * @param updateInterval    How often to process the data that's streaming in.
     * @param zAxisThreshold    How much tilt is allowed before we complain to the user
     */
    public static void setParams(Context context, int updateInterval, int zAxisThreshold) {
        Intent intent = new Intent(context, OrientationService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_UPDATE_INTERVAL, updateInterval);
        intent.putExtra(EXTRA_Z_AXIS_THRESHOLD, zAxisThreshold);
        context.startService(intent);
    }



    /*
    *       The stuff below runs on a background thread
     */

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                handleStartChecking();
            } else if (ACTION_STOP.equals(action)) {
                handleStopChecking();
            } else if (ACTION_GET.equals(action)) {
                handleGetResults();
            } else if (ACTION_SET_PARAMS.equals(action)) {
                final int updateInterval =
                        intent.getIntExtra(EXTRA_UPDATE_INTERVAL, DEFAULT_UPDATE_INTERVAL);
                final int zAxisThreshold =
                        intent.getIntExtra(EXTRA_Z_AXIS_THRESHOLD, DEFAULT_Z_AXIS_THRESHOLD);
                handleSetParams(updateInterval, zAxisThreshold);
            }
        }
    }

    private void handleStartChecking() {
        Log.d(TAG, "background service thread handleStartChecking()");
        mPercentBadPosture = 0;
        mOrientation.startOrienting();

        // We use an additional thread for the periodic execution task.
        mScheduler = Executors.newScheduledThreadPool(1);
        mScheduledFuture =
                mScheduler.scheduleAtFixedRate(
                        mDoPeriodicWork,
                        mUpdateInterval,
                        mUpdateInterval,
                        TimeUnit.SECONDS);
    }

    private void handleStopChecking() {
        Log.d(TAG, "background service thread handleStopChecking()");

        mScheduledFuture.cancel(true);
        mScheduledFuture = null;
        mOrientation.stopOrienting();
        mOrientation = null;
        stopSelf();

    }

    private void handleSetParams(int updateInterval, int zAxisThreshold) {
        Log.d(TAG, "background service thread handleSetParams");

        mUpdateInterval = updateInterval;
        mZAxisThreshold = zAxisThreshold;
    }

    private void handleGetResults() {
        // TODO:  https://guides.codepath.com/android/Starting-Background-Services
        // This guide shows the best way to do this.

        Log.d(TAG, "background service thread handleGetResults");
        Intent intent = new Intent();
        intent.setAction(GET_ALL_RESULTS);

        intent.putExtra(PERCENT_BAD_POSTURE, mPercentBadPosture);
        sendBroadcast(intent);
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
        }
    };






}
