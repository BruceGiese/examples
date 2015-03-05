package com.brucegiese.stuff;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * A fragment representing a list of Items.
 * <p />
 * Large screen devices (such as tablets) are supported by replacing the ListView
 * with a GridView.
 */
public class StepListFragment extends ListFragment {
    private static final String TAG = "com.brucegiese.steplistfragment";


    /**
     * The fragment's ListView/GridView.
     */
    private AbsListView mListView;



    public StepListFragment() {  }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Steps steps = Steps.getInstance(getActivity());
        StepAdapter adapter = new StepAdapter(steps.getSteps());
        setListAdapter(adapter);
    }


    @Override
    public void onListItemClick(ListView l, View view, int position, long id) {
        Step step = ((StepAdapter)getListAdapter()).getItem(position);
        Log.d(TAG, "user pressed step_list with title: " + step.getTitle());

        Intent intent = new Intent(getActivity(), StepActivity.class);
        intent.putExtra(StepActivity.EXTRA_STEP_ID, step.getId());
        startActivity(intent);
    }



    /**
     * The default content for this Fragment has a TextView that is shown when
     * the list is empty. If you would like to change the text, call this method
     * to supply the text it should use.
     */
    public void setEmptyText(CharSequence emptyText) {
        View emptyView = mListView.getEmptyView();

        if (emptyText instanceof TextView) {
            ((TextView) emptyView).setText(emptyText);
        }
    }

    private class StepAdapter extends ArrayAdapter<Step> {
        public StepAdapter(ArrayList<Step> steps) {
            super(getActivity(), 0, steps);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if( convertView == null) {
                // Get the generic fragment_item, which is either a fragment_item_list or fragment_item_grid
                convertView = getActivity().getLayoutInflater().inflate(R.layout.fragment_item, null);
            }

            Step step = getItem(position);
            TextView t = (TextView)convertView.findViewById(R.id.list_item_title);
            t.setText(step.getTitle());

            return convertView;
        }
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        Log.d(TAG, "StepListFragment() is inflating options menu");
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.step_list, menu);
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
