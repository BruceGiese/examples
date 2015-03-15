package com.brucegiese.perfectposture;


import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

// TODO: Serialize this to JSON and write it out to (and read it from) external storage.
// TODO: Make sure the configuration values are taken from the previously used instance.
/**
 * This class represents the data collected about the user's posture.
 * Results are grouped by beginning and ending of the posture monitoring service.
 * To reset the displayed parameters, just start using a new PostureResults object.
 *
 * There are two sets of data associated with the object.  For the duration of the object
 * we save the actual angles recorded.  However, for persistent data, we only save the
 * run length encoded good/bad data.  This throws away the actual angle and only keeps
 * whether the data point was good or bad posture.
 */
public class PostureResults {
    private static final String TAG = "com.brucegiese.results";

    private static final int DEFAULT_Z_AXIS_POS_THRESHOLD = 20;     // units of degrees
    private static final int DEFAULT_Z_AXIS_NEG_THRESHOLD = -20;    // units of degrees

    // These thresholds are used throughout the app, so they need to be static
    public static int mZAxisPosThreshold = DEFAULT_Z_AXIS_POS_THRESHOLD;
    public static int mZAxisNegThreshold = DEFAULT_Z_AXIS_NEG_THRESHOLD;

    private ArrayList<Integer> mPersistentSeq;  // run length encoded good/bad data (small data set)

    int mGoodPostureSamples;            // total count of good posture samples
    int mBadPostureSamples;             // total count of bad posture samples
    boolean mCurrentPostureGood;        // current state of posture
    Date mStartDate;                    // when this sequence of samples started
    boolean mObjectJustCreated;         // needed so we always start with good sample count.

    // These are internal state variables to determine how many consecutive good or bad samples
    // we've seen at this point.
    int mCurrentConsecutiveGoodSamples;
    int mCurrentConsecutiveBadSamples;

    public PostureResults() {
        mPersistentSeq = new ArrayList<Integer>();  // the integers here are good/bad run counts
        mStartDate = new Date();        // Assume that we're going to start sampling right away
        mCurrentPostureGood = true;     // sampling begins with number of good samples, possibly 0
        mObjectJustCreated = true;      // this is needed to know if we just starting sampling
    }


    /**
     * Record a posture sample and determine if it's good or bad posture.
     * @return true if the posture sample represents good posture, false if bad posture.
     */
    public boolean recordSample(int angle) {

        if( angle > mZAxisPosThreshold || angle < mZAxisNegThreshold ) {
            // bad posture
            mBadPostureSamples++;
            mCurrentConsecutiveBadSamples++;        // This is only used for run length encoding
            // This is a special case for the first set of samples
            if( mObjectJustCreated ) {
                mObjectJustCreated = false;
                if( mCurrentConsecutiveGoodSamples != 0) {
                    Log.e(TAG, "Internal logic error regarding newly created object and good count");
                    mCurrentConsecutiveGoodSamples = 0;
                }
                // We always begin the sequence with the number of good samples, possibly zero
                mPersistentSeq.add(new Integer(0));
            }

            if( mCurrentConsecutiveGoodSamples != 0) {
                // We're ending a series of good samples, save the persistent data
                mPersistentSeq.add(new Integer(mCurrentConsecutiveGoodSamples));
                mCurrentConsecutiveGoodSamples = 0;
            }
            return false;

        } else {
            // good posture
            mObjectJustCreated = false;
            mGoodPostureSamples++;
            mCurrentConsecutiveGoodSamples++;
            if( mCurrentConsecutiveBadSamples != 0) {
                // We're ending a series of bad samples, save the persistent data
                mPersistentSeq.add(new Integer(mCurrentConsecutiveBadSamples));
                mCurrentConsecutiveBadSamples = 0;
            }
            return true;
        }
    }

    /**
     * Call this routine when sampling has ended.  This method does any wrap-up needed.
     */
    public void endSampling() {
        // Record the run-length value for the current sequence of good or bad posture points.
        if( mCurrentConsecutiveGoodSamples != 0) {
            mPersistentSeq.add(new Integer(mCurrentConsecutiveGoodSamples));
            mCurrentConsecutiveGoodSamples = 0;
        }
        if( mCurrentConsecutiveBadSamples != 0) {
            mPersistentSeq.add(new Integer(mCurrentConsecutiveBadSamples));
            mCurrentConsecutiveBadSamples = 0;
        }
        mObjectJustCreated = false;
    }

    /**
     * Get the percentage of time user has had bad posture
     * @return  integer percentage number
     */
    public int percentBadPosture() {
        if(mGoodPostureSamples + mBadPostureSamples != 0) {
            return (mBadPostureSamples * 100) / (mGoodPostureSamples + mBadPostureSamples);
        }
        return 0;       // Assume the best until proven otherwise
    }

    /**
     * Print out the current result sequence to the log.
     */
    public void logResultSequence() {
        boolean good = true;    // first sequence number is always good posture number
        Log.i(TAG, "Run Length Results:");
        for( Integer i : mPersistentSeq) {
            if( good ) {
                good = false;
                Log.i(TAG, "Good: " + i.toString());
            } else {
                good = true;
                Log.i(TAG, "Bad:  " + i.toString());
            }
        }
    }
}
