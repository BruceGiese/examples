package com.brucegiese.testingloaders;


import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.activeandroid.query.Select;

import java.util.List;


 public class TheLoader extends AsyncTaskLoader<List<Foo>> {
    private static final String TAG = "testload.loader";
    private int mMaxFoo;

    public TheLoader( Context context, int maxFoo) {
        super(context);
        Log.d(TAG, "constructor called: maxFoo=" + maxFoo);
        mMaxFoo = maxFoo;
    }


    @Override
    protected void onStartLoading() {
        Log.d(TAG, "onStartLoading() called");
        forceLoad();
    }


    @Override
    public List<Foo> loadInBackground() {
        Log.d(TAG, "loadInBackground() called");

        List<Foo> fooList = new Select()
                .from(Foo.class)
                .where("Foo.b <= ?", mMaxFoo)
                .execute();

        return fooList;
    }


    @Override
    protected void onReset() {
        super.onReset();

        Log.d(TAG, "onReset() called");
    }

}
