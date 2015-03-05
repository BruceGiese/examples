package com.brucegiese.doliststuff;


import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Steps {
    private static final String TAG = "com.brucegiese.steps";
    private static final String JSON_ASSET_FILE_NAME = "steps.json";
    private static Steps sInstance = null;
    private ArrayList<Step> mStepArrayList = null;
    private Context mAppContext;


    private Steps(Context applicationContext) {
        mAppContext = applicationContext;
        mStepArrayList = new ArrayList<Step>();

        Step[] steps = null;

        // Get the list of steps from a JSON asset file
        AssetManager assetManager = mAppContext.getAssets();
        String jsonString = null;
        try {
            // TODO: probably not the best way to do this, using a buffered reader
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(assetManager.open(JSON_ASSET_FILE_NAME)));
            StringBuilder sb = new StringBuilder();
            String s = null;
            while((s = reader.readLine()) != null) {
                sb.append(s).append("\n");
            }
            reader.close();

            jsonString = sb.toString();

            Gson gson = new Gson();
            steps = gson.fromJson(jsonString, Step[].class);

            Log.d(TAG, "Sanity check here: step_list 0 title is " + steps[0].getTitle());
            Log.d(TAG, "...and step_list 0 description is: " + steps[0].getDescription());
            Log.d(TAG, "...and step_list 0 hasTimer value is " + steps[0].isHasTimer());

        } catch(IOException e) {
            Log.e(TAG,"data file error reading JSON file");
            Toast.makeText(mAppContext, R.string.toast_data_file_error, Toast.LENGTH_SHORT);
        } catch(JsonIOException e) {
            Log.e(TAG,"JsonIOException parsing the JSON file");
            Toast.makeText(mAppContext, R.string.toast_json_error, Toast.LENGTH_SHORT);
        } catch(JsonSyntaxException e) {        // I can't believe multi-catches aren't supported!
            Log.e(TAG,"JsonSyntaxException parsing the JSON file");
            Toast.makeText(mAppContext, R.string.toast_json_error, Toast.LENGTH_SHORT);
        }

        for(int i=0; i<steps.length; i++) {
            mStepArrayList.add(steps[i]);
        }

    }


    public static Steps getInstance(Context context) {
        if( sInstance == null) {
            sInstance = new Steps(context.getApplicationContext());
        }
        return sInstance;
    }

    public ArrayList<Step> getSteps() {
        return mStepArrayList;
    }

    public Step getStep(int id) {
        for( Step s : mStepArrayList) {
            if (s.getId() == id) {      // these are primitives... for now.
                return s;
            }
        }
        return null;
    }

    public void addStep(Step s) {
        mStepArrayList.add(s);
    }

    public boolean removeStep(int id) {
        for( Step s : mStepArrayList) {
            if( s.getId() == id) {      // these are primitives... for now.
                mStepArrayList.remove(s);
                return true;
            }
        }
        return false;
    }
}
