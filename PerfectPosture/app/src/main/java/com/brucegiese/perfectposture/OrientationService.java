package com.brucegiese.perfectposture;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

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

    public static final String NEW_DATA_POINT_INTENT = "com.brucegiese.perfectposture.sample";
    public static final String EXTRA_VALUE = "value";                // Z-axis posture value
    public static final String CHECK_STATUS_INTENT = "com.brucegiese.perfectposture.check";
    /**
     * Is the service running right now?  We need to effectively create a singleton object
     * with the service.  The OS cooperates by only calling the constructor once, even if there
     * are multiple calls to startService().
     */
    public static boolean sIsRunning = false;
    private static OrientationService sInstance = null;

    private static final int LOW_Z_AXIS_POS_THRESHOLD = 20;     // units of degrees
    private static final int LOW_Z_AXIS_NEG_THRESHOLD = -20;    // units of degrees
    private static final int MEDIUM_Z_AXIS_POS_THRESHOLD = 40;     // units of degrees
    private static final int MEDIUM_Z_AXIS_NEG_THRESHOLD = -40;    // units of degrees
    private static final int HIGH_Z_AXIS_POS_THRESHOLD = 50;     // units of degrees
    private static final int HIGH_Z_AXIS_NEG_THRESHOLD = -50;    // units of degrees
    private int mZAxisPosThreshold = MEDIUM_Z_AXIS_POS_THRESHOLD;  // sensitivity feature upgrade
    private int mZAxisNegThreshold = MEDIUM_Z_AXIS_NEG_THRESHOLD;  // sensitivity feature upgrade

    /*
    * Number of consecutive good/bad samples before we declare a change in posture.
    * This hysteresis is only for the user's benefit in giving alerts.  It's not saved in data.
    * */
    private static final int LOW_POSITIVE_HYSTERESIS = 2;
    private static final int LOW_NEGATIVE_HYSTERESIS = 10;
    private static final int MEDIUM_POSITIVE_HYSTERESIS = 4;
    private static final int MEDIUM_NEGATIVE_HYSTERESIS = 30;
    private static final int HIGH_POSITIVE_HYSTERESIS = 8;
    private static final int HIGH_NEGATIVE_HYSTERESIS = 60;
    private int mPositiveHysteresis = MEDIUM_POSITIVE_HYSTERESIS;
    private int mNegativeHysteresis = MEDIUM_NEGATIVE_HYSTERESIS;
    // number of additional consecutive bad posture samples before we issue a reminder
    private static final int DEFAULT_BAD_REMINDER_THRESHOLD = 30;
    private int mBadPostureReminderCountThreshold = DEFAULT_BAD_REMINDER_THRESHOLD; // sensitivity feature upgrade
    private static final int UPDATE_INTERVAL = 1;           // units of seconds

    private Orientation mOrientation = null;
    private ScheduledFuture mScheduledFuture;

    private boolean mCurrentPostureGood;
    private int mHysteresisCounter;
    private int mBadPostureReminderCounter;
    private int mChinTuckReminderCounter;
    private boolean mChinTuckReminderState;

    // Configuration settings sent from main activity.
    private final boolean DEFAULT_ALERT_NOTIFICATION = true;
    private boolean mAlertNotification = DEFAULT_ALERT_NOTIFICATION;
    private final boolean DEFAULT_ALERT_VIBRATION = true;
    private boolean mAlertVibration = DEFAULT_ALERT_VIBRATION;
    private final boolean DEFAULT_ALERT_LED = false;
    private boolean mAlertLed = DEFAULT_ALERT_LED;
    private final boolean DEFAULT_ALERT_CHIN_TUCK = true;
    private boolean mChinTuck = DEFAULT_ALERT_CHIN_TUCK;
    private Context mContext;

    private Vibrator mVibrator = null;
    private static final int SERVICE_NOTIFICATION_ID = 1;
    private static final int POSTURE_NOTIFICATION_ID = 2;
    private static final int CHIN_TUCK_NOTIFICATION_ID = 3;
    private static final String SERVICE_NOTIFICATION_TITLE = "serviceNotification";
    private static final String POSTURE_NOTIFICATION_TITLE = "postureNotification";
    private static final String CHIN_TUCK_NOTIFICATION_TITLE = "chinTuckNotification";
    public static final String TURN_ON_SERVICE_ACTION = "com.brucegiese.perfectposture.serviceon";
    public static final String TURN_OFF_SERVICE_ACTION = "com.brucegiese.perfectposture.serviceoff";
    private NotificationManager mNotificationManager;

    private enum NotificationType {
        SERVICE_RUNNING,
        BAD_POSTURE,
        CHIN_TUCK_REMINDER
    }

    private CommandReceiver mCommandReceiver;

    public OrientationService() {
        if (OrientationService.sInstance != null) {
            Log.e(TAG, "Our assumption that the OS treats service as a singleton is WRONG!");
        }
        OrientationService.sInstance = this;
        mCommandReceiver = new CommandReceiver();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (mOrientation == null) {
            mOrientation = new Orientation(this);
        }
        if (mVibrator == null) {
            mVibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        }

        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        loadSharedPreferences();
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        mPrefs.registerOnSharedPreferenceChangeListener(prefListener);      // listen for changes

        mContext = this;        // needed by Runnable below
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null) {
            switch (intent.getAction()) {
                case TURN_OFF_SERVICE_ACTION:
                    Log.d(TAG, "onStartCommand(): turning off the service");
                    stopChecking();
                    // Tell the Activity to check its status because this might be sent...
                    // ...by a notification, so the Activity might not know about it.
                    Intent bcastIntent = new Intent(CHECK_STATUS_INTENT);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(bcastIntent);
                    stopSelf();     // Completely shut down the service
                    break;

                case TURN_ON_SERVICE_ACTION:
                    Log.d(TAG, "onStartCommand(): turning on the service");
                    startChecking();
                    break;

                default:
                    Log.e(TAG, "onStartCommand(): unexpected action: " + intent.getAction());
                    break;
            }
        } else {
            Log.e(TAG, "Someone started the service without an action, we ignored it");
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't use binding, just start the service with an action.
        return null;
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mScheduledFuture != null) {
            Log.i(TAG, "onDestroy(): mScheduledFuture was not already null");
            mScheduledFuture.cancel(true);
            mScheduledFuture = null;
        }
        if (mOrientation != null) {
            // Just to be safe
            mOrientation.stopOrienting();
            mOrientation = null;
        } else {
            Log.e(TAG, "mOrientation was null in onDestroy().  That should never happen.");
        }

        mNotificationManager.cancel(SERVICE_NOTIFICATION_ID);
        // This object is a de-facto singleton
        OrientationService.sIsRunning = false;
        OrientationService.sInstance = null;
    }


    /**
     * Start monitoring the user's posture.
     */
    private void startChecking() {
        mChinTuckReminderCounter = 0;
        mCurrentPostureGood = true;     // start out assuming good posture
        mHysteresisCounter = 0;
        mBadPostureReminderCounter = 0;

        try {
            if (mOrientation.startOrienting()) {
                OrientationService.sIsRunning = true;

                if (mScheduledFuture == null) {
                    // We use an additional thread for the periodic execution task.

                    ScheduledExecutorService mScheduler = Executors.newScheduledThreadPool(1);
                    mScheduledFuture =
                            mScheduler.scheduleAtFixedRate(
                                    mDoPeriodicWork,
                                    UPDATE_INTERVAL,
                                    UPDATE_INTERVAL,
                                    TimeUnit.SECONDS);
                } else {
                    Log.e(TAG, "startChecking() was called when checking was already running");
                }
            } else {
                Toast.makeText(this, R.string.no_sensors, Toast.LENGTH_LONG).show();
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
     * Stop monitoring the user's posture.
     */
    private void stopChecking() {
        // remove any existing notifications
        sendNotification(NotificationType.BAD_POSTURE, false);
        sendNotification(NotificationType.CHIN_TUCK_REMINDER, false);

        OrientationService.sIsRunning = false;
        if (mScheduledFuture != null) {
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
     */
    private final Runnable mDoPeriodicWork = new Runnable() {   // must be executed in the UI thread
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

            //       Apply hysteresis to determine when to alert the user.
            if (measurePosture(z)) {                   // Good posture
                if (!mCurrentPostureGood) {
                    mHysteresisCounter++;
                    if (mHysteresisCounter >= mPositiveHysteresis) {
                        mCurrentPostureGood = true;     // posture has been good for long enough
                        mHysteresisCounter = 0;
                        goodPostureAlerts();
                    }
                } else {
                    mHysteresisCounter = 0;
                }

            } else {                                    // Bad posture
                if (mCurrentPostureGood) {
                    mHysteresisCounter++;
                    if (mHysteresisCounter >= mNegativeHysteresis) {
                        mCurrentPostureGood = false;    // posture has been bad for too long
                        mHysteresisCounter = 0;
                        badPostureAlerts();
                        mBadPostureReminderCounter = 0;
                    }
                } else {
                    mHysteresisCounter = 0;

                    // If posture stays bad for too long, remind the user
                    mBadPostureReminderCounter++;
                    if (mBadPostureReminderCounter >= mBadPostureReminderCountThreshold) {
                        mBadPostureReminderCounter = 0;
                        badPostureAlerts();
                    }
                }
            }

            // Chin tuck reminder functionality
            if (mChinTuck) {        // if the functionality is enabled
                // Note that the various types of notification may still be disabled.
                int CHIN_TUCK_REMINDER_TIME = 15 * 60 / UPDATE_INTERVAL;    // first number is minutes
                int CHIN_TUCK_REMINDER_DURATION = 1 * 60 / UPDATE_INTERVAL; // how long to leave the notification up
                mChinTuckReminderCounter++;
                if (!mChinTuckReminderState) {
                    // We're not currently reminding the user to do a chin tuck exercise
                    if (mChinTuckReminderCounter > CHIN_TUCK_REMINDER_TIME) {
                        mChinTuckReminderCounter = 0;
                        mChinTuckReminderState = true;
                        sendNotification(NotificationType.CHIN_TUCK_REMINDER, true);
                        vibrate(true);
                    }
                } else {
                    if (mChinTuckReminderCounter > CHIN_TUCK_REMINDER_DURATION) {
                        mChinTuckReminderCounter = 0;
                        mChinTuckReminderState = false;
                        sendNotification(NotificationType.CHIN_TUCK_REMINDER, false);
                    }
                }
            }
        }
    };

    /**
     * Determine whether the user currently has good posture or bad posture.
     *
     * @param angle angle of device Z-axis from the vertical
     * @return true if good posture
     */
    boolean measurePosture(int angle) {
        return !(angle > mZAxisPosThreshold || angle < mZAxisNegThreshold);
    }

    /**
     * Send out all alerts associated with a bad posture event
     */
    private void badPostureAlerts() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        // Only send out alerts if the screen is active.
        if (pm.isScreenOn()) {     // This was deprecated in API level 20
            Log.d(TAG, "Posture is bad!");
            vibrate(true);
            sendNotification(NotificationType.BAD_POSTURE, true);
        }
    }

    /**
     * Send out all alerts associated with a good posture event
     */
    private void goodPostureAlerts() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        // Only send out alerts if the screen is active.
        if (pm.isScreenOn()) {     // This was deprecated in API level 20
            Log.d(TAG, "Posture just got good!");
            vibrate(false);
            sendNotification(NotificationType.BAD_POSTURE, false);
        }
    }

    /**
     * Send or cancel a notification, subject to user settings
     *
     * @param n    The type of notification to send or cancel
     * @param send true if send, false if cancel
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
                // Add an action allowing the user to open the application
                mBuilder.addAction(R.drawable.ic_posture, getString(R.string.open_application), pIntent);

                // Add an action to turn off the service (needs to be a broadcast intent)
                Intent turnOffIntent = new Intent(this, OrientationService.class);
                turnOffIntent.setAction(TURN_OFF_SERVICE_ACTION);
                PendingIntent pendingIntentTurnOff =
                        PendingIntent.getService(this, 0, turnOffIntent, 0);
                // ic_action_halt icon is from Opoloo, covered by Attribution-ShareAlike 4.0 license
                // http://creativecommons.org/licenses/by-sa/4.0/
                // icons are at http://www.opoloo.com/
                mBuilder.addAction(R.drawable.ic_action_halt, getString(R.string.turn_off_service), pendingIntentTurnOff);

                mNotificationManager.notify(id, mBuilder.build());

            } else {
                // remove the notification
                mNotificationManager.cancel(id);
            }
        }
    }

    /**
     * Vibrate the device to signal a good or bad posture, subject to user config settings.
     *
     * @param longInterval if true, long vibration, otherwise short vibration time
     */
    private void vibrate(boolean longInterval) {
        if (mAlertVibration) {
            int vibrationTime = 30;         // short vibration time, units of milliseconds
            if (longInterval) {
                vibrationTime = 800;        // long vibration time, units of milliseconds
            }

            if (mVibrator != null) {
                mVibrator.vibrate(vibrationTime);
            }
        }
    }


    /**
     * Listen for changes in the application's shared preferences.
     * <p/>
     * This is the recommended way of implementing the listener for changes in shared preferences
     * Otherwise, the OS will garbage collect the listener.  This creates a strong reference.
     */
    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                    Log.d(TAG, "pref change, key = " + key);
                    setupPreference(key);
                }
            };

    private void setupPreference(String key) {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        Log.d(TAG, "getting preference for " + key);
        if (key.equals("PREF_NOTIFICATION")) {
            mAlertNotification = sharedPrefs.getBoolean(getString(R.string.pref_notification), DEFAULT_ALERT_NOTIFICATION);
            if (!mAlertNotification) {
                // remove all notifications that might be currently displayed
                sendNotification(NotificationType.CHIN_TUCK_REMINDER, false);
                sendNotification(NotificationType.BAD_POSTURE, false);
//                sendNotification(NotificationType.SERVICE_RUNNING, false);
            }

        } else if (key.equals("PREF_VIBRATION")) {
            mAlertVibration = sharedPrefs.getBoolean(getString(R.string.pref_vibrate), DEFAULT_ALERT_VIBRATION);

        } else if (key.equals("PREF_LED")) {
            mAlertLed = sharedPrefs.getBoolean(getString(R.string.pref_led), DEFAULT_ALERT_LED);

        } else if (key.equals("PREF_CHIN_TUCK")) {
            mChinTuck = sharedPrefs.getBoolean(getString(R.string.pref_chin_tuck), DEFAULT_ALERT_CHIN_TUCK);
            if (!mChinTuck) {
                // remove any chin tuck notification that might be currently displayed
                sendNotification(NotificationType.CHIN_TUCK_REMINDER, false);
            }
        } else if (key.equals("PREF_SENSITIVITY")) {
            Log.d(TAG, "selection is...");
            Log.d(TAG, "..." + sharedPrefs.getString("PREF_SENSITIVITY", "2"));
            switch (Integer.valueOf(sharedPrefs.getString("PREF_SENSITIVITY", "2"))) {

                case 1:
                    Log.d(TAG, "user selected low sensitivity setting");
                    mPositiveHysteresis = LOW_POSITIVE_HYSTERESIS;
                    mNegativeHysteresis = LOW_NEGATIVE_HYSTERESIS;
                    mZAxisPosThreshold = LOW_Z_AXIS_POS_THRESHOLD;
                    mZAxisNegThreshold = LOW_Z_AXIS_NEG_THRESHOLD;
                    break;

                case 2:
                    Log.d(TAG, "user selected medium sensitivity setting");
                    mPositiveHysteresis = MEDIUM_POSITIVE_HYSTERESIS;
                    mNegativeHysteresis = MEDIUM_NEGATIVE_HYSTERESIS;
                    mZAxisPosThreshold = MEDIUM_Z_AXIS_POS_THRESHOLD;
                    mZAxisNegThreshold = MEDIUM_Z_AXIS_NEG_THRESHOLD;
                    break;

                case 3:
                    Log.d(TAG, "user selected high sensitivity setting");
                    mPositiveHysteresis = HIGH_POSITIVE_HYSTERESIS;
                    mNegativeHysteresis = HIGH_NEGATIVE_HYSTERESIS;
                    mZAxisPosThreshold = HIGH_Z_AXIS_POS_THRESHOLD;
                    mZAxisNegThreshold = HIGH_Z_AXIS_NEG_THRESHOLD;
                    break;

                default:
                    Log.e(TAG, "invalid sensitivity setting");
                    break;
            }
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
        setupPreference(getString(R.string.pref_sensitivity));
    }

    /**
     * Get the user configured value for the Z-Axis positive threshold.
     * This is needed by the charting function to draw the dotted red limit lines.
     * @param c     Context
     * @return      Z-Axis positive threshold
     */
    public static int getZAxisPositiveThreshold(Context c) {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(c);
        switch (Integer.valueOf(sharedPrefs.getString("PREF_SENSITIVITY", "2"))) {

            case 1:
                Log.d(TAG, "user selected low sensitivity setting");
                return LOW_Z_AXIS_POS_THRESHOLD;

            case 2:
                return MEDIUM_Z_AXIS_POS_THRESHOLD;

            case 3:
                return HIGH_Z_AXIS_POS_THRESHOLD;

            default:
                Log.e(TAG, "invalid sensitivity setting");
                return MEDIUM_Z_AXIS_POS_THRESHOLD;     // just use the default
        }
    }

    /**
     * Get the user configured value for the Z-Axis negative threshold.
     * This is needed by the charting function to draw the dotted red limit lines.
     * @param c     Context
     * @return      Z-Axis negative threshold
     */
    public static int getZAxisNegativeThreshold(Context c) {
        SharedPreferences sharedPrefs =
                PreferenceManager.getDefaultSharedPreferences(c);
        switch (Integer.valueOf(sharedPrefs.getString("PREF_SENSITIVITY", "2"))) {

            case 1:
                Log.d(TAG, "user selected low sensitivity setting");
                return LOW_Z_AXIS_NEG_THRESHOLD;

            case 2:
                return MEDIUM_Z_AXIS_NEG_THRESHOLD;

            case 3:
                return HIGH_Z_AXIS_NEG_THRESHOLD;

            default:
                Log.e(TAG, "invalid sensitivity setting");
                return MEDIUM_Z_AXIS_NEG_THRESHOLD;     // just use the default
        }
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
