package com.brucegiese.stuff;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;


public class StepActivity extends ActionBarActivity {
    private static final String TAG = "com.brucegiese.stepactivity";
    public static final String EXTRA_STEP_ID = "step_list";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step);

        int id = getIntent().getIntExtra(EXTRA_STEP_ID,-1);
        Step step = Steps.getInstance(this).getStep(id);
        Log.d(TAG, "StepActivity onCreate() with step_list title: " + step.getTitle());

        FragmentManager fm = getSupportFragmentManager();
        Fragment f = fm.findFragmentById(R.id.step_fragment_container);

        if( f == null) {
            Log.d(TAG, "StepActivity adding new StepFragment");
            StepFragment stepFragment = StepFragment.newInstance(step);
            fm.beginTransaction().add(R.id.step_fragment_container, stepFragment).commit();
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
