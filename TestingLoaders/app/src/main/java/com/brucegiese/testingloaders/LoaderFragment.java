package com.brucegiese.testingloaders;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;
import java.util.Random;

public class LoaderFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<List<Foo>> {

    private static final String TAG = "testload.fragment";
    private static final int LOADER_NUMBER = 12;


    private View mView;
    private List<Foo> mData;

    private OnFragmentInteractionListener mListener;

    interface OnFragmentInteractionListener {
        void whatsGoingOn(String whatsup );
    }


    public LoaderFragment() {
        // Required empty public constructor
        Log.d(TAG, "LoaderFragment() constructor called");
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (OnFragmentInteractionListener) activity;
        mListener.whatsGoingOn("attached to activity");
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mListener.whatsGoingOn("onCreate in the fragment");

        Log.d(TAG, "calling initLoader with number " + LOADER_NUMBER);
        getLoaderManager().initLoader( LOADER_NUMBER, null, this);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_loader, container, false);

        mListener.whatsGoingOn("onCreateView() in the fragment");
        return mView;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "onDestroyView()");
        mListener.whatsGoingOn("destroying the view in the fragment");
        mView = null;

    }


    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach() called");
        mListener = null;
    }


    /**
     *      Loader Callbacks
     *
     */
    @Override
    public Loader<List<Foo>> onCreateLoader(int id, Bundle args) {
        // Configure a loader and return it.
        Log.d(TAG, "onCreateLoader() called, ID=" + id);
        Random rn = new Random();
        int maxFoo = rn.nextInt(12);
        Log.d(TAG, "... setting up a loader with maxFoo=" + maxFoo);

        TheLoader loader = new TheLoader( getActivity(), maxFoo);

        return loader;
    }


    @Override
    public void onLoadFinished( Loader<List<Foo>> loader, List<Foo> data ) {
        // NOTE: Can't commit fragment transactions, etc. because this can happen after Activity
        // state has been saved.

        // Remove all use of old data (which is about to be released by the loader).
        // For cursors, just swap the cursor, don't close it.

        // This gets called when there is new data.

        // Don't tell the cursor to FLAG_AUTO_REQUERY or FLAG_REGISTER_CONTENT_OBSERVER since
        // the loader takes care of updating the data, not the cursor.  (so flags = 0 in cursor
        // adapter constructor).

        mData = data;
        Log.d(TAG, "onLoadFinished() called");
        if( loader == null ) {
            Log.d(TAG, "...loader is null");
        }

        if( data == null ) {
            Log.d(TAG, "...data is null");
        } else {
            for( Foo foo : data ) {
                Log.d(TAG, "... foo: " + foo.getA() );
            }
        }

    }


    @Override
    public void onLoaderReset( Loader<List<Foo>> loader ) {
        // Remove all references to the data.  Not sure what we do with the cursor here.
        Log.d(TAG, "onLoaderReset() called");

        mData = null;
    }


}
