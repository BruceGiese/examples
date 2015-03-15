package com.brucegiese.perfectposture;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.preference.PreferenceManager;
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
    private static boolean sIsRunning = false;
    public static OrientationService sInstance = null;

    /** Warning: These strings MUST match up with the strings in preferences.xml */
    private static final String PREF_NOTIFICATION = "pref_notifications";
    private static final String PREF_VIBRATE = "pref_vibrate";
    private static final String PREF_LED = "pref_led";
    private static final boolean DEFAULT_CHECKBOX = true;    // default for checkboxes is true

    // Number of consecutive good/bad samples before we declare a change in posture.
    // This hysteresis is only for the user's benefit in giving alerts.  It's not saved in data.
    // That's why this functionality is here and not in PostureResults.
    private static final int POSITIVE_HYSTERESIS = 4;
    private static final int NEGATIVE_HYSTERESIS = 8;
    // number of additional consecutive bad posture samples before we issue a reminder
    private static final int DEFAULT_BAD_REMINDER_THRESHOLD = 10;
    int mBadPostureReminderCountThreshold = DEFAULT_BAD_REMINDER_THRESHOLD;

    public static final int MSG_START_MONITORING = 1;
    public static final int MSG_STOP_MONITORING = 2;
    public static final int MSG_REGISTER_CLIENT = 3;
    public static final int MSG_UNREGISTER_CLIENT = 4;
    public static final int MSG_DATA_SAMPLE = 101;                  // this is outgoing

    private static final int DEFAULT_UPDATE_INTERVAL = 1;           // units of seconds

    private Orientation mOrientation = null;
    private ScheduledExecutorService mScheduler;
    private ScheduledFuture mScheduledFuture;
    private Messenger mToClientMessenger;

//
// We want to support multiple Activities running simultaneously, but it crashes when we add to the ArrayList
//    private ArrayList<Messenger> mClientMessengerArrayList;               // Activities wanting to RX real-time data


    private int mUpdateInterval = DEFAULT_UPDATE_INTERVAL;

    private PostureResults mResults = null;
    private boolean mCurrentPostureGood = true;        // start out assuming good posture
    private int mHysteresisCounter = 0;
    private int mBadPostureReminderCounter = 0;

    // Configuration settings sent from main activity.
    private boolean mAlertNotification = DEFAULT_CHECKBOX;
    private boolean mAlertVibration = DEFAULT_CHECKBOX;
    private boolean mAlertLed = DEFAULT_CHECKBOX;
    private SharedPreferences mPrefs;

    private Vibrator mVibrator = null;
    private int BAD_POSTURE_VIBRATION_TIME = 800;        // units of milliseconds
    private int GOOD_POSTURE_VIBRATION_TIME = 30;        // units of milliseconds
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static final int POSTURE_NOTIFICATION_ID = 2;
    private static final String SERVICE_NOTIFICATION_TITLE = "serviceNotification";
    private static final String POSTURE_NOTIFICATION_TITLE = "postureNotification";
    private static final String TURN_OFF_SERVICE_ACTION = "turnOffService";
    private static final int TURN_OFF_SERVICE_REQUEST_CODE = 12345;
    private NotificationManager mNotificationManager;
    private enum NotificationType {
        SERVICE_RUNNING,
        BAD_POSTURE
    }


    /**
     * Is the service running right now?  We need to effectively create a singleton object
     * with the service.  The OS cooperates by only calling the constructor once, even if there
     * are multiple calls to startService().
     * @return true if the service is running
     */
    public static boolean checkIsRunning() {
        return sIsRunning;
    }

    public OrientationService() {
        if( OrientationService.sInstance != null ) {
            Log.e(TAG, "Our assumption that the OS treats service as a singleton is WRONG!");
        }
        OrientationService.sInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if( mOrientation == null) {
            mOrientation = new Orientation(this);
        }
        if( mVibrator == null) {
            mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);
        }

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        loadSharedPreferences();        // read the existing shared preferences
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(prefListener);      // listen for changes
    }


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
                    Log.d(TAG, "handleMessage(): Start monitoring");
                    startChecking();
                    break;

                case MSG_STOP_MONITORING:
                    Log.d(TAG, "handleMessage(): Stop monitoring");
                    stopChecking();
                    break;

                case MSG_REGISTER_CLIENT:
                    Log.d(TAG, "An Activity registered a handler with the service");
                    if( msg.replyTo != null) {
//                        mClientMessengerArrayList.add(msg.replyTo);
                        mToClientMessenger = msg.replyTo;
                    } else {
                        Log.e(TAG, "We got a null replyTo messenger from an Activity");
                    }
                    break;

                case MSG_UNREGISTER_CLIENT:
                    Log.d(TAG, "An Activity unregistered a handler with the service");
//                    mClientMessengerArrayList.remove(msg.replyTo);
                    mToClientMessenger = null;
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     * This provides a means for getting a connection to the service.
     */
    private ServiceConnection mToClientConnection = new ServiceConnection() {

        /** this is called when the connection with the service has been established,
         * giving us the object we can use for the service.  We need a client-side
         * representation of the Messenger from the raw IBinder object
         */
        public void onServiceConnected(ComponentName className, IBinder service) {
            mToClientMessenger = new Messenger(service);
        }

        public void onServiceDisconnected(ComponentName className) {
            // Called when the connection with the service was unexpectedly disconnected
            Log.e(TAG, "ServiceConnection: Activity probably crashed, lost messenger connection");
            mToClientMessenger = null;
        }
    };

    /**
     * Target we publish for the client to send messages to IncomingHandler
     */
    final Messenger fromClientMessenger = new Messenger( new IncomingHandler());


    /**
     * When binding to the service, we return an interface to our messenger for sending
     * messages to the service
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind called");
        return fromClientMessenger.getBinder();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        // Make absolutely sure that we've stopped everything and cleaned up.
        // This is guaranteed to be the last thing called.
        if( mScheduledFuture != null ) {
            Log.i(TAG, "onDestroy(): mScheduledFuture was not already null");
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
        }
        if( mOrientation != null ) {
            // Just to be safe
            mOrientation.stopOrienting();
            mOrientation = null;
        } else {
            Log.e(TAG, "mOrientation was null in onDestroy().  That should never happen.");
        }

        mNotificationManager.cancel(SERVICE_NOTIFICATION_ID);
        // This object is a de-facto singleton
        OrientationService.sIsRunning = false;
        OrientationService.sInstance = null;        // The OS will destroy the object now.
    }



    /**
     *  For robustness purposes, this should be able to handle being called
     *  when the checking is already running.  Worst case, the results get reset.
     */
    private void startChecking() {
        try {
            mResults = new PostureResults();        // Set up the data collection for this session.

            mOrientation.startOrienting();
            // This object is essentially a singleton
            OrientationService.sIsRunning = true;

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
                Log.e(TAG, "startChecking() was called when checking was already running");
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
        // remove any bad posture notifications
        sendNotification(NotificationType.BAD_POSTURE, false);

        if( mResults != null) {     // wrap-up the data collection
            mResults.endSampling();
            // TODO: Save the current session out to external storage.
            mResults.logResultSequence();
            mResults = null;        // for now, let garbage collection destroy the data.
        } else {
            Log.e(TAG, "Internal logic error: there was no results object when stopping");
        }
        OrientationService.sIsRunning = false;
        if( mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
            mOrientation.stopOrienting();
        } else {
            Log.e(TAG, "stopChecking() was called when checking wasn't running.");
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
            int z = mOrientation.getZ();

            // Send the data point to the Activity to plot it.
            if( mToClientMessenger != null) {
                try {
                    Message msg = Message.obtain(null, MSG_DATA_SAMPLE, z, 0);
                    msg.replyTo = fromClientMessenger;
                    mToClientMessenger.send(msg);
                } catch (RemoteException e) {
                    Log.e(TAG, "Remote Exception when sending a data point: ", e);
                }
            }

            if( mResults.recordSample(z) ) {        // Good posture
                if( ! mCurrentPostureGood ) {
                    mHysteresisCounter++;
                    if( mHysteresisCounter >= POSITIVE_HYSTERESIS ) {
                        mCurrentPostureGood = true;
                        mHysteresisCounter = 0;
                        goodPostureAlerts();
                    }
                } else {
                    mHysteresisCounter = 0;
                }

            } else {                                // Bad posture
                if( mCurrentPostureGood) {
                    mHysteresisCounter++;
                    if( mHysteresisCounter >= NEGATIVE_HYSTERESIS ) {
                        mCurrentPostureGood = false;
                        mHysteresisCounter = 0;
                        badPostureAlerts();
                        mBadPostureReminderCounter = 0;
                    }
                } else {
                    mHysteresisCounter = 0;

                    mBadPostureReminderCounter++;
                    if( mBadPostureReminderCounter >= mBadPostureReminderCountThreshold) {
                        mBadPostureReminderCounter = 0;
                        badPostureAlerts();     // Send a reminder
                    }
                }
            }
        }
    };

    /**
     * Send out all alerts associated with a bad posture event
     */
    private void badPostureAlerts() {
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        if( pm.isScreenOn() ) {     // This was deprecated in API level 20
            Log.d(TAG, "Posture is bad!");
            vibrate(true);
            sendNotification(NotificationType.BAD_POSTURE, true);
        } else {
            Log.d(TAG, "badPostureAlerts() Screen is not interactive right now");
        }
    }

    /**
     * Send out all alerts associated with a good posture event
     */
    private void goodPostureAlerts() {
        PowerManager pm = (PowerManager)getSystemService(POWER_SERVICE);
        if( pm.isScreenOn() ) {     // This was deprecated in API level 20
            Log.d(TAG, "Posture just got good!");
            vibrate(false);
            sendNotification(NotificationType.BAD_POSTURE, false);
        } else {
            Log.d(TAG, "goodPostureAlerts() Screen is not interactive right now");
        }

    }

    /**
     * Send or cancel a notification, subject to user settings
     *
     * @param n The type of notification to send or cancel
     * @param send  true if send, false if cancel
     */
    private void sendNotification(NotificationType n, boolean send) {
        String title;
        String text;
        int id;
        int icon;

        if (mAlertNotification || !send) {      // always attempt to cancel pending notifications
            switch (n) {

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

            if (send) {
                Intent resultIntent = new Intent(this, PerfectPostureActivity.class);
                PendingIntent pIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(icon)
                                .setContentIntent(pIntent)
                                .setContentTitle(title)
                                .setContentText(text);

                // Add an action to turn off the service
                Intent turnOffIntent = new Intent();
                turnOffIntent.setAction(TURN_OFF_SERVICE_ACTION);
                PendingIntent pendingIntentTurnOff =
                        PendingIntent.getBroadcast(this,
                                TURN_OFF_SERVICE_REQUEST_CODE,
                                turnOffIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);
                mBuilder.addAction(R.drawable.ic_posture, getString(R.string.turn_off_service), pendingIntentTurnOff);

                mNotificationManager.notify(id, mBuilder.build());

            } else {
                mNotificationManager.cancel(id);
            }
        }
    }

    /**
     * Vibrate the device to signal a good or bad posture, subject to user config settings.
     *
     * @param badPosture if true, this is a bad posture signal.  False sends a good posture signal.
     */
    private void vibrate( boolean badPosture ) {
        if( mAlertVibration ) {
            int vibrationTime = GOOD_POSTURE_VIBRATION_TIME;
            if (badPosture) {
                vibrationTime = BAD_POSTURE_VIBRATION_TIME;
            }

            if (mVibrator != null) {
                mVibrator.vibrate(vibrationTime);
            }
        }
    }


    /**
     * Listen for changes in the application's shared preferences.
     *
     * This is the recommended way of implementing the listener for changes in shared preferences
     * Otherwise, the OS will garbage collect the listener.  This creates a strong reference.
     */
    SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs,
                                                      String key) {
                    setupPreference(key);
                }
            };

    private void setupPreference(String key) {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if( key.equals(PREF_NOTIFICATION) ) {
            mAlertNotification = sharedPrefs.getBoolean(PREF_NOTIFICATION, DEFAULT_CHECKBOX);
        } else if( key.equals(PREF_VIBRATE) ) {
            mAlertVibration = sharedPrefs.getBoolean(PREF_VIBRATE, DEFAULT_CHECKBOX);
        } else if( key.equals(PREF_LED ) ) {
            mAlertLed = sharedPrefs.getBoolean(PREF_LED, DEFAULT_CHECKBOX);
        }
    }

    /**
     * Read in the various configuration settings via the shared preferences.
     */
    private void loadSharedPreferences() {
        // the getAll() method isn't going to work with the support library ArrayMap.
        // so just grab each value one-by-one.
        setupPreference(PREF_NOTIFICATION);
        setupPreference(PREF_VIBRATE);
        setupPreference(PREF_LED);
    }
}
