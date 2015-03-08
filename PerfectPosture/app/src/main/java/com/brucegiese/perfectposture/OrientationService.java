package com.brucegiese.perfectposture;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
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
    private Orientation mOrientation = null;
    private ScheduledExecutorService mScheduler;
    private ScheduledFuture mScheduledFuture;
    private int mUpdateInterval;
    private int mZAxisThreshold;
    private int mPercentBadPosture;      // 0 to 100

    /*
    *   This uses the recommended method for implementing an IntentService.  A lot of the
    *   extra code comes from having to receive requests from the application and then
    *   send that same information down to the background thread.  Fortunately, you
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
    // This is one of the results sent back to the broadcast receiver.
    public static final String PERCENT_BAD_POSTURE
            = "com.brucegiese.perfectposture.PERCENT_BAD_POSTURE";


    @Override
    public void onCreate() {
        // TODO: move the constructor stuff into here
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

    }

    @Override
    public void IBinder onBind(Intent intent) {

    }

    @Override
    public void onDestroy() {

    }


    /**
     * Start the background service running.  It will continue running after the app terminates.
     * This will send out a notification.  If the user clicks on the notification, it will
     * re-open the application allowing the user to stop the service.
     *
     * This service also sends another notification if they are holding the device outside
     * the range of allowed orientation.
     *
     * @param context
     * @param updateInterval    How often to process the data that's streaming in.
     * @param zAxisThreshold    How much tilt is allowed before we complain to the user
     */
    public static void startChecking(Context context, String updateInterval, String zAxisThreshold) {
        Intent intent = new Intent(context, OrientationService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_UPDATE_INTERVAL, updateInterval);
        intent.putExtra(EXTRA_Z_AXIS_THRESHOLD, zAxisThreshold);
        context.startService(intent);
    }

    /**
     * Stop the background service running.  There must always be a way for the user to call this!
     * @param context
     */
    public static void stopChecking(Context context) {
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
    public static void setParams(Context context, String updateInterval, String zAxisThreshold) {
        Intent intent = new Intent(context, OrientationService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(EXTRA_UPDATE_INTERVAL, updateInterval);
        intent.putExtra(EXTRA_Z_AXIS_THRESHOLD, zAxisThreshold);
        context.startService(intent);
    }


    public OrientationService() {
        super("OrientationService");
        if( mOrientation == null) {
            mOrientation = new Orientation(this);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                final String updateInterval = intent.getStringExtra(EXTRA_UPDATE_INTERVAL);
                final String zAxisThreshold = intent.getStringExtra(EXTRA_Z_AXIS_THRESHOLD);
                handleStartChecking(this, updateInterval, zAxisThreshold);
            } else if (ACTION_STOP.equals(action)) {
                handleStopChecking();
            } else if (ACTION_GET.equals(action)) {
                handleGetResults();
            } else if (ACTION_SET_PARAMS.equals(action)) {
                final String updateInterval = intent.getStringExtra(EXTRA_UPDATE_INTERVAL);
                final String zAxisThreshold = intent.getStringExtra(EXTRA_Z_AXIS_THRESHOLD);
                handleSetParams(updateInterval, zAxisThreshold);
            }
        }
    }


    private void handleStartChecking(Context context, String updateInterval, String zAxisThreshold) {
        mPercentBadPosture = 0;
        mUpdateInterval = Integer.valueOf(updateInterval);
        mZAxisThreshold = Integer.valueOf(zAxisThreshold);
        mOrientation.startOrienting();

        // We use an additional thread for the periodic execution task.
        mScheduler = Executors.newScheduledThreadPool(1);
        mScheduledFuture =
                mScheduler.scheduleAtFixedRate(
                        mDoPeriodicWork,
                        mUpdateInterval,
                        mUpdateInterval,
                        TimeUnit.SECONDS);
        Log.d(TAG, "background service thread handleStartChecking()");
    }

    private void handleStopChecking() {
        Log.d(TAG, "background service thread handleStopChecking()");

        mScheduledFuture.cancel(true);
        mOrientation.stopOrienting();
        mOrientation = null;
        stopSelf();             // turn off this service

    }

    private void handleSetParams(String updateInterval, String zAxisThreshold) {
        Log.d(TAG, "background service thread handleSetParams");

        Log.e(TAG, "handleSetParams is not implemented yet");
        mUpdateInterval = Integer.valueOf(updateInterval);
        // TODO: change the parameters to these values on the fly

    }


    private void handleGetResults() {
        Log.d(TAG, "background service thread handleGetResults");
        Intent intent = new Intent();
        intent.setAction(GET_ALL_RESULTS);

        intent.putExtra(PERCENT_BAD_POSTURE, mPercentBadPosture);
        sendBroadcast(intent);
    }

    /**
     * @hide
     * This runs periodically in the background (yet another thread).
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
