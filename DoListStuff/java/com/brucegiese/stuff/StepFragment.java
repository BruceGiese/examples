package com.brucegiese.stuff;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;


public class StepFragment extends Fragment {
    private static final String TAG = "com.brucegiese.stepfragment";
    private static final String ARG_STEP_ID = "step_argument";

    private Step mStep;

    public static StepFragment newInstance(Step step) {
        StepFragment fragment = new StepFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_STEP_ID, step.getId());
        fragment.setArguments(args);
        return fragment;
    }
    public StepFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            int id = getArguments().getInt(ARG_STEP_ID);
            mStep = Steps.getInstance(getActivity()).getStep(id);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup parent,
                             Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_step, parent, false);
        TextView title = (TextView)v.findViewById(R.id.step_title);
        title.setText(mStep.getTitle());

        TextView description = (TextView)v.findViewById(R.id.step_description);
        description.setText(mStep.getDescription());

        CheckBox checkBox = (CheckBox)v.findViewById(R.id.step_timer_checkbox);
        checkBox.setChecked(mStep.isHasTimer());
        checkBox.setOnClickListener(new CheckBox.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Step fragment checkbox is checked?  " + ((CheckBox)v).isChecked() );
                mStep.setHasTimer(((CheckBox)v).isChecked());
            }
        });

        return v;
    }

}
