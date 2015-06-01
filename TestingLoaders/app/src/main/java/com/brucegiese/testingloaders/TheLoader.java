package com.brucegiese.testingloaders;


import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.activeandroid.query.Select;

import java.util.List;
import java.util.Random;


public class TheLoader extends AsyncTaskLoader<MixedData> {
    private static final String TAG = "testload.loader";
    private int mMaxFoo;
    private int mMaxBar;

    public TheLoader( Context context, int maxFoo, int maxBar ) {
        super(context);
        Log.d(TAG, "constructor called: maxFoo=" + maxFoo + ", maxBar=" + maxBar);
        mMaxFoo = maxFoo;
        mMaxBar = maxBar;
    }


    @Override
    protected void onStartLoading() {
        Log.d(TAG, "onStartLoading() called");


        forceLoad();
    }


    @Override
    public MixedData loadInBackground() {
        Log.d(TAG, "loadInBackground() called");


        // Do we really want to put serial database fetches here?
        List<Foo> fooList = new Select()
                .from(Foo.class)
                .where("Foo.b <= ?", mMaxFoo)
                .execute();


        List<Bar> barList = new Select()
                .from(Bar.class)
                .where("Bar.y <= ?", mMaxBar)
                .execute();


        // Add a random string to the two lists just to add a non-database originated part
        Random rn = new Random();
        return new MixedData(fooList, barList, Integer.toString(rn.nextInt(10000)) );
    }


    @Override
    protected void onReset() {
        super.onReset();

        Log.d(TAG, "onReset() called");
    }

}
