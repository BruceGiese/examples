package com.brucegiese.perfectposture;


import android.util.Log;

        // TODO: Serialize this to JSON and write it out to (and read it from) external storage.
/**
 * This class represents the data collected about the user's posture.
 */
public class PostureResults {
    private static final String TAG = "com.brucegiese.postureresults";
    private static final int DEFAULT_BAD_THRESHOLD = 4;
    private static final int DEFAULT_GOOD_THRESHOLD = 4;
    private static final int DEFAULT_BAD_REMINDER_THRESHOLD = 10;

    int mGoodPostureSamples;            // total count of good posture samples
    int mBadPostureSamples;             // total count of bad posture samples
    boolean mCurrentPostureGood;        // current state of posture

    // These are internal state variables to determine how many consecutive good or bad samples
    // we've seen at this point.
    int mCurrentConsecutiveGoodSamples;
    int mCurrentConsecutiveBadSamples;



    // number of consecutive bad posture samples before we change the state to bad posture
    int mBadPostureCountThreshold = DEFAULT_BAD_THRESHOLD;
    // number of additional consecutive bad posture samples before we issue a reminder
    int mBadPostureReminderCountThreshold = DEFAULT_BAD_REMINDER_THRESHOLD;
    // number of consecutive good posture samples before we change the state to good posture
    int mGoodPostureCountThreshold = DEFAULT_GOOD_THRESHOLD;

    public void resetResults() {
        mGoodPostureSamples = 0;
        mBadPostureSamples = 0;
        mCurrentPostureGood = true;     // assume the best in people
        mCurrentConsecutiveGoodSamples = 0;
        mCurrentConsecutiveBadSamples = 0;
    }


    /**
     * Record a good posture sample.
     * @return true if this marks a transition from a bad to good posture state.
     */
    public boolean recordGoodSample() {
        boolean retValue = false;

        mGoodPostureSamples++;
        mCurrentConsecutiveBadSamples = 0;
        mCurrentConsecutiveGoodSamples++;
        if( mCurrentConsecutiveGoodSamples > mGoodPostureCountThreshold) {
            if( ! mCurrentPostureGood ) {
                // The posture state was bad, but now it's transitioned to good.
                mCurrentPostureGood = true;
                retValue = true;
            }
        }
        return retValue;
    }

    /**
     * Record a bad posture sample
     * @return true if this marks a transition from a good to bad posture state or time for
     * a reminder again.
     */
    public boolean recordBadSample() {
        boolean retValue = false;

        mBadPostureSamples++;
        mCurrentConsecutiveGoodSamples = 0;
        mCurrentConsecutiveBadSamples++;
        if( mCurrentConsecutiveBadSamples > mBadPostureCountThreshold) {
            if( mCurrentPostureGood ) {
                // The posture state was good, but now it's transitioned to bad.
                mCurrentPostureGood = false;
                retValue = true;
            } else {
                // look to see if the user needs a reminder
                if( (mCurrentConsecutiveBadSamples - mBadPostureCountThreshold)%mBadPostureReminderCountThreshold == 0) {
                    retValue = true;
                }
            }
        }
        return retValue;
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

    public int getBadPostureCountThreshold() {
        return mBadPostureCountThreshold;
    }
    public void setBadPostureCountThreshold(int badPostureCountThreshold) {
        mBadPostureCountThreshold = badPostureCountThreshold;
    }

    public int getBadPostureReminderCountThreshold() {
        return mBadPostureReminderCountThreshold;
    }

    public void setBadPostureReminderCountThreshold(int badPostureReminderCountThreshold) {
        mBadPostureReminderCountThreshold = badPostureReminderCountThreshold;
    }

    public int getGoodPostureCountThreshold() {
        return mGoodPostureCountThreshold;
    }

    public void setGoodPostureCountThreshold(int goodPostureCountThreshold) {
        mGoodPostureCountThreshold = goodPostureCountThreshold;
    }
}
