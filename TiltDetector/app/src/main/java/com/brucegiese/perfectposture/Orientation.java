package com.brucegiese.perfectposture;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * This class supports gravity based orientation, initially for the purpose of evaluating
 * how the device is being held.
 */
public class Orientation implements SensorEventListener {
    private static final String TAG = "com.brucegiese.tiltdetector.orientation";
    private static final float G_FORCE = 9.78f;         // gravity in meters per second squared
    private static final float MAX_INTEGER = 10f;       // maximum integer value to return
    private static final float ALMOST_HALF = .4999f;    // need to round up
    Context mContext;
    private SensorManager mSensorManager;
    private Sensor mGravitySensor;
    private float[] mGravity = null;

    public Orientation(Context context) {
        mContext = context;
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);

        // Note that if there is no gravity sensor implemented, then this will return null.
        mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
    }

    /**
     * Begin sensing the orientation of the device.
     * Can only be called once after creation or stopping orientation.
     * @return whether the device has the proper sensor.
     */
    public boolean startOrienting() {
        if (mGravitySensor != null) {
            // Use the slowest rate possible, although the Mgr just uses this as a suggestion.
            mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            // No gravity sensor on this device!
            return false;
        }
        return true;
    }

    /**
     * Stop sensing the orientation of the device.
     * Technically you can call this repeatedly without any bad effects.
     */
    public void stopOrienting() {
        // Best practice: Make sure to call this when the Activity is paused to save battery life.
        mSensorManager.unregisterListener(this);
        mGravity = null;
    }

    /**
    *  @hide
    *   Sensor Event Listener Interface Methods
    */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * @hide
     * @param event see sensor listener documentation
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        /*
        *   Best practice: Do as little as possible in onSensorChanged().
        *   Therefore, we need to have the UI code call the result methods
        *   based on some sort of timer.  Otherwise, the app will crash.
        */
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            mGravity = event.values;
        }
    }

    /**
     * Get the current value of the X-axis orientation data.
     * This can be called from the UI thread.
     * @return an integer from -MAX_INTEGER (upside down) to +MAX_INTEGER (right side up)
     */
    public String getX() {
        if( mGravity != null) {
            int x = Math.round(mGravity[0] * (MAX_INTEGER/G_FORCE) + ALMOST_HALF);
            return String.valueOf(x);
        } else {
            return mContext.getString(R.string.no_data);
        }
    }

    /**
     * Get the current value of the Y-axis orientation data.
     * This can be called from the UI thread.
     * @return an integer from -MAX_INTEGER (upside down) to +MAX_INTEGER (right side up)
     */
    public String getY() {
        if( mGravity != null) {
            int y = Math.round(mGravity[1] * (MAX_INTEGER/G_FORCE) + ALMOST_HALF);
            return String.valueOf(y);
        } else {
            return mContext.getString(R.string.no_data);
        }
    }

    /**
     * Get the current value of the Z-axis orientation data.
     * This can be called from the UI thread.
     * @return an integer from -MAX_INTEGER (upside down) to +MAX_INTEGER (right side up)
     */
    public String getZ() {
        if( mGravity != null) {
            int z = Math.round(mGravity[2] * (MAX_INTEGER/G_FORCE) + ALMOST_HALF);
            return String.valueOf(z);
        } else {
            return mContext.getString(R.string.no_data);
        }
    }
}
