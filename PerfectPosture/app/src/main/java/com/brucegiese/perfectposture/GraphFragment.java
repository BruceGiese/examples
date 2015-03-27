package com.brucegiese.perfectposture;

import java.util.ArrayList;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.activeandroid.query.Delete;
import com.activeandroid.query.Select;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


/**
 * This fragment represents the graph of the Z-axis posture data.
 * It uses a broadcast receiver to get the data in real-time and it uses the database
 * to get any previous data.  It uses MPAndroidChart to plot the data.  A previous
 * pre-beta version used aChartEngine.
 */
public class GraphFragment extends Fragment {
    private static final String TAG = "com.brucegiese.graph";
    private static final int DATA_POINTS_TO_SHOW = 100;

    private LineChart mLineChart;
    private int mIndex;
    private ArrayList<Entry> mPostureSamples;
    private LineData mLineData;
    private DataReceiver mDataReceiver;
    private boolean mChartValid = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDataReceiver = new DataReceiver();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_graph, container, false);

        Button button = (Button) v.findViewById(R.id.clear_data_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear all the data from the database (and also the chart)
                Log.d(TAG, "clearing data");
                new Delete().from(Sample.class).execute();      // delete all records!!!
                setupData();                                    // delete data in the chart
            }
        });

        /*
        *       Set up the chart
         */
        mLineChart = (LineChart) v.findViewById(R.id.chart);
        mLineChart.setBackgroundColor(getResources().getColor(R.color.neutral_main_color));
        mLineChart.setDescription("");
        mLineChart.setNoDataText(getString(R.string.no_chart_data));
        mLineChart.setDrawGridBackground(true);
        mLineChart.setGridBackgroundColor(getResources().getColor(R.color.chart_background));
        mLineChart.setDrawBorders(false);



        /*
        *       Y-Axis stuff
         */
        YAxis yAxisRight = mLineChart.getAxisRight();
        yAxisRight.setEnabled(false);

        YAxis yAxis = mLineChart.getAxisLeft();
        yAxis.setTextColor(getResources().getColor(R.color.light_secondary_color));
        yAxis.setEnabled(true);
        yAxis.setDrawAxisLine(true);
        yAxis.setDrawGridLines(true);
        yAxis.setDrawLabels(true);
        yAxis.setDrawTopYLabelEntry(true);
        yAxis.setLabelCount(17);
        yAxis.setStartAtZero(false);
        yAxis.setAxisMaxValue(95);
        yAxis.setAxisMinValue(-95);
        yAxis.setSpaceTop(0.0f);       // leave this much percent space above max value
        yAxis.setSpaceBottom(0.0f);    // leave this much percent space below min value

        LimitLine upperLimit = new LimitLine(20.0f, getString(R.string.max_forward_tilt));
        upperLimit.setLineColor(getResources().getColor(R.color.chart_red));
        upperLimit.setLineWidth(2f);
        upperLimit.setTextColor(getResources().getColor(R.color.chart_red));
        upperLimit.setTextSize(10f);
        upperLimit.enableDashedLine(10, 10, 0);
        upperLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);

        yAxis.addLimitLine(upperLimit);
        LimitLine lowerLimit = new LimitLine(-20.0f, getString(R.string.max_backward_tilt));
        lowerLimit.setLineColor(getResources().getColor(R.color.chart_red));
        lowerLimit.setLineWidth(2f);
        lowerLimit.setTextColor(getResources().getColor(R.color.chart_red));
        lowerLimit.setTextSize(10f);
        lowerLimit.enableDashedLine(10, 10, 0);
        lowerLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);
        yAxis.addLimitLine(lowerLimit);

        LimitLine zeroLimit = new LimitLine(0f, getString(R.string.best_posture));
        zeroLimit.setLineColor(getResources().getColor(R.color.chart_green));
        zeroLimit.setLineWidth(2f);
        zeroLimit.setTextColor(getResources().getColor(R.color.chart_green));
        zeroLimit.setTextSize(10f);
        zeroLimit.enableDashedLine(10, 10, 0);
        zeroLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);
        yAxis.addLimitLine(zeroLimit);

        /*
        *       X-Axis stuff
         */
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setDrawLabels(false);
        xAxis.setEnabled(false);

        /*
        *       Data setup
         */
        setupData();

        /*
        *       Can't set up legend until the data is set up.
         */
        Legend legend = mLineChart.getLegend();
        legend.setEnabled(false);
        // When clearing data, the legend disappears and the chart re-sizes, looks odd, so don't use

        /*
        *       Chart interactions
         */
        mLineChart.setDragEnabled(true);
        mLineChart.setPinchZoom(false);

        // Register the broadcast receiver
        IntentFilter iFilter = new IntentFilter();
        iFilter.addAction(OrientationService.NEW_DATA_POINT_INTENT);
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mDataReceiver, iFilter);

        return v;
    }

    /**
     * Set up the data in the fragment.  This can be called to refresh the data if
     * the user clears out the database, for instance.
     */
    private void setupData() {
        LineDataSet mLineDataSet;
        mIndex = 0;
        mPostureSamples = new ArrayList<Entry>();
        mLineDataSet = new LineDataSet(mPostureSamples, getString(R.string.posture_readings));
        mLineDataSet.setValueTextSize(0);            // Can't find any other way to disable txt
        mLineDataSet.setCircleSize(2);
        mLineDataSet.setDrawCircleHole(true);
        mLineDataSet.setLineWidth(2);
        mLineDataSet.setColor(getResources().getColor(R.color.chart_data_line));
        mLineDataSet.setCircleColor(getResources().getColor(R.color.chart_data_line));
        mLineDataSet.setDrawCircleHole(true);

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(mLineDataSet);
        ArrayList<String> xVals = new ArrayList<String>();

        mLineData = new LineData(xVals, dataSets);      // LineData is a subclass of ChartData
        mLineChart.setVisibleXRange(DATA_POINTS_TO_SHOW);
        mLineChart.setData(mLineData);

        new LoadFromDatabase().execute();
        mChartValid = true;
        mLineChart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDataReceiver);
        mIndex = 0;
        // Cleanup whatever we can... and hope garbage collection does the rest.
        mChartValid = false;
        mLineChart.clear();
        mLineChart = null;
        mPostureSamples = null;
    }

    /**
     * This adds one point of data to the chart.
     * @param value  Z-axis posture value from vertical, Positive means device leaning backward.
     */
    private void addPoint( int value ) {
        if (mChartValid) {                          // chart is not valid during rotations etc.
            Entry point = new Entry((float) value, mIndex);
            mLineData.addXValue("");                // we don't want to show an X value
            mLineData.addEntry(point, 0);           // We have one dataset, it's index is 0
            mLineChart.notifyDataSetChanged();
            if (mIndex >= DATA_POINTS_TO_SHOW) {
                mLineChart.setVisibleXRange(DATA_POINTS_TO_SHOW);
                mLineChart.moveViewToX(mLineData.getXValCount() - DATA_POINTS_TO_SHOW);
            }
            View chart = getActivity().findViewById(R.id.chart);
            // This is for both testing and accessibility
            chart.setContentDescription("index is" + mIndex + ", value is" + point.getVal());
            mIndex++;
            mLineChart.invalidate();
        } else {
            Log.d(TAG, "addNewPoint() mChartValid is false");
        }
    }

    /**
     * This AsyncTask loads previous data points from the database.
     */
    private class LoadFromDatabase extends AsyncTask<Void, Void, List<Sample>> {

        /**
         * Get all the points from the database.
         * Unfortunately, "WHERE ROWNUM <= ?" won't work in the ORM we're using.
         * @return                  List of all the Sample objects from the database
         */
        protected List<Sample> doInBackground(Void... x) {
            return new Select()
                    .from(Sample.class)
                    .orderBy("_ID ASC")
                    .execute();
        }

        protected void onPostExecute(List<Sample> values) {
            for(Sample s : values) {
                addPoint(s.value);
            }
        }
    }


    /**
     * Receive new data points as they're being added to the database.
     */
    class DataReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context c, Intent i) {
            if( i.getAction().equals(OrientationService.NEW_DATA_POINT_INTENT)) {
                    int value = i.getIntExtra(OrientationService.EXTRA_VALUE, Orientation.IMPOSSIBLE_INTEGER);
                    addPoint(value);
            } else {
                Log.e(TAG, "Received an unexpected broadcast intent");
            }

        }
    }
}
