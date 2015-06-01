package com.brucegiese.testingloaders;

import java.util.List;

/**
 *      This just represents a composite data type combining two supposedly independent
 *      database fields and another independent String.  The purpose is to demonstrate
 *      handling data that is not simply the result of a single database query or a single
 *      REST API GET.
 *
 */
public class MixedData {
    public List<Foo> mFooList;
    public List<Bar> mBarList;
    public String mOther;

    public MixedData(List<Foo> fooList, List<Bar> barList, String other) {
        mFooList = fooList;
        mBarList = barList;
        mOther = other;
    }


    /**
     *      Basic getters and setters
     *
     */
    public List<Foo> getFooList() {
        return mFooList;
    }

    public void setFooList(List<Foo> mFooList) {
        this.mFooList = mFooList;
    }

    public List<Bar> getBarList() {
        return mBarList;
    }

    public void semBarList(List<Bar> mBarList) {
        this.mBarList = mBarList;
    }

    public String getOther() {
        return mOther;
    }

    public void setOther(String mOther) {
        this.mOther = mOther;
    }
}
