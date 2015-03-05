package com.brucegiese.stuff;

import com.google.gson.annotations.Expose;
import java.io.Serializable;

public class Step implements Serializable {
    private static final String TAG = "com.brucegiese.step_list";

    @Expose
    private int mId;
    @Expose
    private String mTitle;
    @Expose
    private String mDescription;
    @Expose
    private boolean mHasTimer;

    public Step() {  }

    public Step( String title, String description, boolean hasTimer) {
        mTitle = title;
        mDescription = description;
        mHasTimer = hasTimer;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }


    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }


    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }


    public boolean isHasTimer() {
        return mHasTimer;
    }

    public void setHasTimer(boolean hasTimer) {
        mHasTimer = hasTimer;
    }
}

