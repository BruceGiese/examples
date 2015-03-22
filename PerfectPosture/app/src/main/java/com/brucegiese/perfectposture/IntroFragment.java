package com.brucegiese.perfectposture;

import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class IntroFragment extends Fragment {

    /**
     * This is the Introduction screen for the application.  It contains the start/stop button.
     * This is done to make it easy for the user to open the app and start/stop.
     */
    public IntroFragment() {  }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_intro, container, false);
    }
}
