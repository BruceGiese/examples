package com.brucegiese.perfectposture;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
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

    private static final int DEFAULT_UPDATE_INTERVAL = 1;           // units of seconds
    private static final int DEFAULT_Z_AXIS_POS_THRESHOLD = 20;    // units of degrees
    private static final int DEFAULT_Z_AXIS_NEG_THRESHOLD = 20;     // units of degrees

    private Orientation mOrientation = null;
    private ScheduledExecutorService mScheduler;
    private ScheduledFuture mScheduledFuture;

    private int mUpdateInterval = DEFAULT_UPDATE_INTERVAL;
    private int mZAxisPosThreshold = DEFAULT_Z_AXIS_POS_THRESHOLD;
    private int mZAxisNegThreshold = DEFAULT_Z_AXIS_NEG_THRESHOLD;
    private PostureResults mResults = null;

    private Vibrator mVibrator = null;
    private int VIBRATION_TIME = 200;        // units of milliseconds
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static final int POSTURE_NOTIFICATION_ID = 2;
    private static final String SERVICE_NOTIFICATION_TITLE = "serviceNotification";
    private static final String POSTURE_NOTIFICATION_TITLE = "postureNotification";
    private NotificationManager mNotificationManager;
    private enum NotificationType {
        SERVICE_RUNNING,
        BAD_POSTURE
    }

    private int debugCounter = 0;



    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "OrientationService onCreate() called");
        if( mOrientation == null) {
            mOrientation = new Orientation(this);
        }
        if( mResults == null) {
            mResults = new PostureResults();
            mResults.resetResults();
        }
        if( mVibrator == null) {
            mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        }

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
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

        mNotificationManager.cancel(SERVICE_NOTIFICATION_ID);
    }



    /**
     *  For robustness purposes, this should be able to handle being called
     *  when the checking is already running.  Worst case, the results get reset.
     */
    private void startChecking() {
        try {
            mOrientation.startOrienting();
            debugCounter = 0;

            if (mScheduledFuture == null) {
                // We use an additional thread for the periodic execution task.

                mScheduler = Executors.newScheduledThreadPool(1);
                mScheduledFuture =
                        mScheduler.scheduleAtFixedRate(
                                mDoPeriodicWork,
                                mUpdateInterval,
                                mUpdateInterval,
                                TimeUnit.SECONDS);

                sendNotification(NotificationType.SERVICE_RUNNING, true);

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

            if( (z > mZAxisPosThreshold) || (z < -mZAxisNegThreshold)) {
                if( mResults.recordBadSample() ) {
                    badPostureAlerts();
                }
            } else {
                if( mResults.recordGoodSample() ) {
                    goodPostureAlerts();
                }
            }


            // TODO: REMOVE THIS: this stops the service after 5 minutes so we don't get stuck.
            if( debugCounter++ > 300) {
                debugCounter = 0;
                Log.d(TAG, "STOPPING THE SERVICE due to the debugCounter");
                stopChecking();
            }
        }
    };

    /**
     * Send out all alerts associated with a bad posture event
     */
    private void badPostureAlerts() {
        Log.d(TAG, "Posture is bad!");
        if( mVibrator != null) {
            Log.d(TAG,"vibrate");
            mVibrator.vibrate(800);

        } else {
            Log.d(TAG, "no vibrate on this device");
        }

        sendNotification(NotificationType.BAD_POSTURE, true);
    }

    /**
     * Send out all alerts associated with a good posture event
     */
    private void goodPostureAlerts() {
        Log.d(TAG, "Posture just got good!");
        if( mVibrator != null) {
            Log.d(TAG,"vibrate");
            mVibrator.vibrate(30);
        } else {
            Log.d(TAG, "no vibrate on this device");
        }

        sendNotification(NotificationType.BAD_POSTURE, false);

    }

    /**
     * Send or cancel a notification
     * @param n The type of notification to send or cancel
     * @param send  true if send, false if cancel
     */
    private void sendNotification(NotificationType n, boolean send) {
        String title;
        String text;
        int id;
        int icon;

        switch( n ) {

            case SERVICE_RUNNING:
                title = SERVICE_NOTIFICATION_TITLE;
                text = getResources().getString(R.string.service_notification_text);
                id = SERVICE_NOTIFICATION_ID;
                icon = R.drawable.ic_posture;
                break;


            case BAD_POSTURE:
                title = POSTURE_NOTIFICATION_TITLE;
                text = getResources().getString(R.string.posture_notification_text);
                id = POSTURE_NOTIFICATION_ID;
                icon = R.drawable.ic_posture_alert;
                break;

            default:
                Log.e(TAG, "Unknown type given to sendNotification");
                return;

        }

        if( send ) {
            Intent resultIntent = new Intent(this, PerfectPostureActivity.class);
            PendingIntent pIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(this)
                            .setSmallIcon(icon)
                            .setContentIntent(pIntent)
                            .setContentTitle(title)
                            .setContentText(text);
            mNotificationManager.notify(id, mBuilder.build());
        } else {
            mNotificationManager.cancel(id);
        }
    }
}
