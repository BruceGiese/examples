package com.brucegiese.testingloaders;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.activeandroid.query.Select;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrePopulate {
    private static final String TAG = "prepop";

    private List<Foo> mFoos = null;
    private AtomicBoolean mStartedCreatingFoos = new AtomicBoolean(false);


    interface PrePopulateCallback {
        void onPrePopulateDone();
    }

    PrePopulateCallback mPrePopulateCallback;
    Context mContext;

    public PrePopulate(PrePopulateCallback callback, Context context) {
        mPrePopulateCallback = callback;
        mContext = context;

        tryAddingMoreStuff();
    }


    private void tryAddingMoreStuff() {

        if( !mStartedCreatingFoos.getAndSet(true) ) {
            new LoadFoos().execute();
        }
    }


    private class LoadFoos extends AsyncTask<Void, Void, List<Foo>> {

        protected List<Foo> doInBackground(Void... x) {
            return new Select()
                    .from(Foo.class)
                    .execute();
        }

        protected void onPostExecute(List<Foo> fooList) {

            if (fooList.size() == 0) {
                Log.d(TAG, "No Foos yet, creating some...");

                for (int i = 0; i < 20; i++) {

                    Foo f = new Foo(Integer.toString(i), i);
                    f.save();
                }
                new LoadFoos().execute();

            } else {
                Log.d(TAG, "List of Foos");
                for (int i = 0; i < fooList.size(); i++) {
                    Log.d(TAG, "... " + fooList.get(i).getA() + ", " + fooList.get(i).getB());
                }
            }

        }
    }

}
