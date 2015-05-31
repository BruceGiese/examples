package com.brucegiese.testingloaders;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.activeandroid.content.ContentProvider;

public class LoaderFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "loader.fragment";
    private static final String ARG_PARAM_A = "paramA";
    private static final String ARG_PARAM_B = "paramB";

    private String mParamA;
    private int mParamB;

    private View mView;
    private TextView mFragmentText;

    private OnFragmentInteractionListener mListener;

    interface OnFragmentInteractionListener {
        void whatsGoingOn(String whatsup );
    }



    /**
     *
     * @param paramA
     * @param paramB
     * @return A new instance of fragment LoaderFragment.
     *
     */
    public static LoaderFragment newInstance(String paramA, int paramB) {

        LoaderFragment fragment = new LoaderFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM_A, paramA);
        args.putInt(ARG_PARAM_B, paramB);
        fragment.setArguments(args);

        return fragment;
    }

    public LoaderFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParamA = getArguments().getString(ARG_PARAM_A);
            mParamB = getArguments().getInt(ARG_PARAM_B);
        }
        mListener.whatsGoingOn("onCreate in the fragment");
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.fragment_loader, container, false);
        mFragmentText = (TextView) mView.findViewById(R.id.fragment_text);

        return mView;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mListener = (OnFragmentInteractionListener) activity;
        mListener.whatsGoingOn("attached to activity");
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "onDestroyView()");
        mListener.whatsGoingOn("destroying the view");
        mView = null;

    }


    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }


    /**
     *      Loader Callbacks
     *
     */
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Configure a loader and return it.

        String select = "( Foo )";

        CursorLoader cursorLoader = new CursorLoader(getActivity(),
                ContentProvider.createUri(Foo.class, null),
                null, select, null, null);

        return cursorLoader;
    }

    @Override
    public void onLoadFinished( Loader<Cursor> loader, Cursor data ) {
        // NOTE: Can't commit fragment transactions, etc. because this can happen after Activity
        // state has been saved.

        // Remove all use of old data (which is about to be released by the loader).
        // For cursors, just swap the cursor, don't close it.

        // This gets called when there is new data.

        // Don't tell the cursor to FLAG_AUTO_REQUERY or FLAG_REGISTER_CONTENT_OBSERVER since
        // the loader takes care of updating the data, not the cursor.  (so flags = 0 in cursor
        // adapter constructor).



    }

    @Override
    public void onLoaderReset( Loader<Cursor> loader ) {

        // Remove all references to the data.  Not sure what we do with the cursor here.

    }


}
