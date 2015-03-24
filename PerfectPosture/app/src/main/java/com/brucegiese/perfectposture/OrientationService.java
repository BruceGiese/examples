package com.brucegiese.perfectposture;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a Service for tracking the user device's orientation periodically.
 * It notifies the user when they are outside of the range parameters for tilt orientation.
 * It keeps track of metrics which can be fetched.  It allows for changing the various
 * configuration parameters while the service is running.
 *
 * This service also updates the database with posture data and sends out a broadcast each
 * time new data is added to the database.
 */
public class OrientationService extends Service {
    private static final String TAG = "com.brucegiese.service";

    // TODO: Refactor all this stuff!  There's too much stuff in one place.
    public static final String NEW_DATA_POINT_INTENT = "com.brucegiese.perfectposture.sample";
    public static final String EXTRA_VALUE = "value";                // Z-axis posture value
    public static final String CHECK_STATUS_INTENT = "com.brucegiese.perfectposture.check";
    /**
     * Is the service running right now?  We need to effectively create a singleton object
     * with the service.  The OS cooperates by only calling the constructor once, even if there
     * are multiple calls to startService().
     */
    private static boolean sIsRunning = false;
    public static OrientationService sInstance = null;

    private static final int DEFAULT_Z_AXIS_POS_THRESHOLD = 20;     // units of degrees
    private static final int DEFAULT_Z_AXIS_NEG_THRESHOLD = -20;    // units of degrees
    private int mZAxisPosThreshold = DEFAULT_Z_AXIS_POS_THRESHOLD;
    private int mZAxisNegThreshold = DEFAULT_Z_AXIS_NEG_THRESHOLD;

    /*
    * Number of consecutive good/bad samples before we declare a change in posture.
    * This hysteresis is only for the user's benefit in giving alerts.  It's not saved in data.
    * */
    private static final int POSITIVE_HYSTERESIS = 4;
    private static final int NEGATIVE_HYSTERESIS = 8;
    // number of additional consecutive bad posture samples before we issue a reminder
    private static final int DEFAULT_BAD_REMINDER_THRESHOLD = 10;
    int mBadPostureReminderCountThreshold = DEFAULT_BAD_REMINDER_THRESHOLD;
    private static final int DEFAULT_UPDATE_INTERVAL = 1;           // units of seconds

    // Messaging from whoever is running this service.
    public static final int MSG_START_MONITORING = 1;
    public static final int MSG_STOP_MONITORING = 2;

    private Orientation mOrientation = null;
    private ScheduledFuture mScheduledFuture;

    private int mUpdateInterval = DEFAULT_UPDATE_INTERVAL;

    private boolean mCurrentPostureGood;
    private int mHysteresisCounter;
    private int mBadPostureReminderCounter;
    private int mChinTuckReminderCounter;
    private boolean mChinTuckReminderState;

    // Configuration settings sent from main activity.
    private boolean DEFAULT_ALERT_NOTIFICATION = true;
    private boolean mAlertNotification = DEFAULT_ALERT_NOTIFICATION;
    private boolean DEFAULT_ALERT_VIBRATION = true;
    private boolean mAlertVibration = DEFAULT_ALERT_VIBRATION;
    private boolean DEFAULT_ALERT_LED = false;
    private boolean mAlertLed = DEFAULT_ALERT_LED;
    private boolean DEFAULT_ALERT_CHIN_TUCK = true;
    private boolean mChinTuck = DEFAULT_ALERT_CHIN_TUCK;
    private Context mContext;

    private Vibrator mVibrator = null;
    private int BAD_POSTURE_VIBRATION_TIME = 800;       // units of milliseconds
    private int GOOD_POSTURE_VIBRATION_TIME = 30;       // units of milliseconds
//    private int CHIN_TUCK_REMINDER_TIME = 15 *60/DEFAULT_UPDATE_INTERVAL;    // first number is minutes
    private int CHIN_TUCK_REMINDER_TIME = 10;           // TODO: This is for debugging, use the above definition
    private int CHIN_TUCK_REMINDER_DURATION = 5;        // how long to leave the notification up
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static final int POSTURE_NOTIFICATION_ID = 2;
    private static final int CHIN_TUCK_NOTIFICATION_ID = 3;
    private static final String SERVICE_NOTIFICATION_TITLE = "serviceNotification";
    private static final String POSTURE_NOTIFICATION_TITLE = "postureNotification";
    private static final String CHIN_TUCK_NOTIFICATION_TITLE = "chinTuckNotification";
    private static final String TURN_OFF_SERVICE_ACTION = "com.brucegiese.perfectposture.serviceoff";
    private NotificationManager mNotificationManager;
    private enum NotificationType {
        SERVICE_RUNNING,
        BAD_POSTURE,
        CHIN_TUCK_REMINDER
    }
    private CommandReceiver mCommandReceiver;

    public static boolean checkIsRunning() {
        return sIsRunning;
    }


    public OrientationService() {
        if( OrientationService.sInstance != null ) {
            Log.e(TAG, "Our assumption that the OS treats service as a singleton is WRONG!");
        }
        OrientationService.sInstance = this;
        mCommandReceiver = new CommandReceiver();
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
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(prefListener);      // listen for changes

        mContext = this;        // needed by Runnable below
    }

    /*
     * This gets called when the messaging service starts up.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() called");

        String action = intent.getAction();
        if( action != null) {
            switch (action) {
                case TURN_OFF_SERVICE_ACTION:
                    Log.d(TAG, "onStartCommand(): turning off the service");
                    stopChecking();
                    // Tell the Activity to check its status, which just changed.
                    Intent bcastIntent = new Intent(CHECK_STATUS_INTENT);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(bcastIntent);
                    break;

                default:
                    Log.e(TAG, "onStartCommand(): unexpected action: " + action);
                    break;
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * Target we publish for the client to send messages to Handler
     */
    final Messenger fromClientMessenger = new Messenger( new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch( msg.what ) {

                // TODO: Should this be refactored to use onStartCommand????
                case MSG_START_MONITORING:
                    Log.d(TAG, "handleMessage(): Start monitoring");
                    startChecking();
                    break;

                case MSG_STOP_MONITORING:
                    Log.d(TAG, "handleMessage(): Stop monitoring");
                    stopChecking();
                    break;

                default:
                    super.handleMessage(msg);
            }
        }

    });

    @Override
    public IBinder onBind(Intent intent) {
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
        mChinTuckReminderCounter = 0;
        mCurrentPostureGood = true;     // start out assuming good posture
        mHysteresisCounter = 0;
        mBadPostureReminderCounter = 0;

        try {
            mOrientation.startOrienting();
            // This object is essentially a singleton
            OrientationService.sIsRunning = true;

            if (mScheduledFuture == null) {
                // We use an additional thread for the periodic execution task.

                ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
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

        // Register the broadcast receiver
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(TURN_OFF_SERVICE_ACTION);
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(mCommandReceiver, iFilter);
    }

    /**
     *  For robustness purposes, this should be able to handle being called
     *  when the checking has already been stopped.
     */
    private void stopChecking() {
        // remove any bad posture notifications
        sendNotification(NotificationType.BAD_POSTURE, false);
        sendNotification(NotificationType.CHIN_TUCK_REMINDER, false);

        OrientationService.sIsRunning = false;
        if( mScheduledFuture != null) {
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
            mOrientation.stopOrienting();
        } else {
            Log.e(TAG, "stopChecking() was called when checking wasn't running.");
        }

        // Un-register the broadcast receiver
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mCommandReceiver);
    }


    /**
     * This runs periodically in the background (yet another thread).
     * NOTE: This does NOT run on the same thread as the handleXXX stuff above!
     */
    Runnable mDoPeriodicWork = new Runnable() {   // must be executed in the UI thread
        @Override
        public void run() {
            int z = mOrientation.getZ();

            //      Enter the data point into the database.
            Sample sample = new Sample(z, new Date(), measurePosture(z));
            sample.save();

            // Broadcast the data point
            Intent bcastIntent = new Intent(NEW_DATA_POINT_INTENT);
            bcastIntent.putExtra(EXTRA_VALUE, z);
            // Don't bother adding the date or goodPosture value
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(bcastIntent);


            // TODO: move this code into a separate object.
            //       Apply hysteresis to determine when to alert the user.
            if( measurePosture(z) ) {        // Good posture
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

            // Chin tuck reminder functionality
            if( mChinTuck) {
                // If this feature is enabled
                // Note that the various types of notification may still be disabled.
                mChinTuckReminderCounter++;
                if (mChinTuckReminderState) {
                    // We're currently reminding the user to do a chin tuck exercise
                    if (mChinTuckReminderCounter > CHIN_TUCK_REMINDER_DURATION) {
                        mChinTuckReminderCounter = 0;
                        mChinTuckReminderState = false;
                        sendNotification(NotificationType.CHIN_TUCK_REMINDER, false);
                    }
                } else {
                    if (mChinTuckReminderCounter > CHIN_TUCK_REMINDER_TIME) {
                        mChinTuckReminderCounter = 0;
                        mChinTuckReminderState = true;
                        sendNotification(NotificationType.CHIN_TUCK_REMINDER, true);
                        vibrate(true);      // TODO: Vibrate function needs enum for more than 2 types
                    }
                }
            }
        }
    };

    /**
     * Determine whether it is good posture or bad posture.
     * @param angle  angle of device Z-axis from the vertical
     * @return  true if good posture
     */
    public boolean measurePosture(int angle) {
        return !(angle > mZAxisPosThreshold || angle < mZAxisNegThreshold);
    }

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
            Log.d(TAG, "badPostureAlerts(): Screen is not interactive right now");
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
            Log.d(TAG, "goodPostureAlerts(): Screen is not interactive right now");
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

                // Right now, we're not sending any SERVICE_RUNNING notifications.
                // If we do, then we need two different icons for service running vs bad posture.
                case SERVICE_RUNNING:
                    title = SERVICE_NOTIFICATION_TITLE;
                    text = getResources().getString(R.string.service_notification_text);
                    id = SERVICE_NOTIFICATION_ID;
                    icon = R.drawable.ic_posture_notif;
                    break;


                case BAD_POSTURE:
                    title = POSTURE_NOTIFICATION_TITLE;
                    text = getResources().getString(R.string.posture_notification_text);
                    id = POSTURE_NOTIFICATION_ID;
                    icon = R.drawable.ic_posture_notif;
                    break;

                case CHIN_TUCK_REMINDER:
                    title = CHIN_TUCK_NOTIFICATION_TITLE;
                    text = getResources().getString(R.string.chin_tuck_notification_text);
                    id = CHIN_TUCK_NOTIFICATION_ID;
                    icon = R.drawable.ic_chin_tuck_notif;
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
                                .setContentTitle(title)
                                .setContentText(text);
                // Add an action to open the application
                mBuilder.addAction(R.drawable.ic_posture, getString(R.string.open_application), pIntent);

                // Add an action to turn off the service (needs to be a broadcast intent)
                Intent turnOffIntent = new Intent(this,OrientationService.class);
                turnOffIntent.setAction(TURN_OFF_SERVICE_ACTION);
                PendingIntent pendingIntentTurnOff =
                        PendingIntent.getService(this, 0, turnOffIntent, 0);
                // ic_action_halt icon is from Opoloo, covered by Attribution-ShareAlike 4.0 license
                // http://creativecommons.org/licenses/by-sa/4.0/
                // icons are at http://www.opoloo.com/
                mBuilder.addAction(R.drawable.ic_action_halt, getString(R.string.turn_off_service), pendingIntentTurnOff);

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
                    Log.d(TAG, "Preference change, key is " + key);
                }
            };

    private void setupPreference(String key) {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        if( key.equals(getString(R.string.pref_notification))) {
            mAlertNotification = sharedPrefs.getBoolean(getString(R.string.pref_notification), DEFAULT_ALERT_NOTIFICATION);
            Log.d(TAG, "Notification is " + mAlertNotification);

        } else if( key.equals(getString(R.string.pref_vibrate))) {
            mAlertVibration = sharedPrefs.getBoolean(getString(R.string.pref_vibrate), DEFAULT_ALERT_VIBRATION);
            Log.d(TAG, "Vibration is " + mAlertVibration);

        } else if( key.equals(getString(R.string.pref_led))) {
            mAlertLed = sharedPrefs.getBoolean(getString(R.string.pref_led), DEFAULT_ALERT_LED);
            Log.d(TAG, "LED is " + mAlertLed);

        } else if( key.equals(getString(R.string.pref_chin_tuck))) {
            mChinTuck = sharedPrefs.getBoolean(getString(R.string.pref_chin_tuck), DEFAULT_ALERT_CHIN_TUCK);
            Log.d(TAG, "Chin Tuck is " + mChinTuck);
        }

    }

    /**
     * Read in the various configuration settings via the shared preferences.
     */
    private void loadSharedPreferences() {
        // the getAll() method isn't going to work with the support library ArrayMap.
        // so just grab each value one-by-one.
        setupPreference(getString(R.string.pref_notification));
        setupPreference(getString(R.string.pref_vibrate));
        setupPreference(getString(R.string.pref_led));
        setupPreference(getString(R.string.pref_chin_tuck));
    }


    /**
     * Receive intents telling us to stop the service (these are actions within notifications)
     */
    class CommandReceiver extends BroadcastReceiver {
        public CommandReceiver() { }

        @Override
        public void onReceive(Context c, Intent i) {
            if( i.getAction().equals(TURN_OFF_SERVICE_ACTION)) {
                Log.d(TAG, "received an Intent telling us to stop the service");
                stopChecking();
            } else {
                Log.e(TAG, "Received an unexpected broadcast intent");
            }
        }
    }
}
