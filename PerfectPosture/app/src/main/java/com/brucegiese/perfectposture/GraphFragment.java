package com.brucegiese.perfectposture;

import java.util.ArrayList;
import java.util.List;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.activeandroid.query.Select;
import com.github.mikephil.charting.charts.LineChart;
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
    private static final int DARK_GREEN = 0xFF006600;
    private static final int DARK_RED = 0xFFB22222;

    private LineChart mLineChart;
    private int mIndex;
    ArrayList<Entry> mPostureSamples;
    LineData mLineData;
    LineDataSet mLineDataSet;
    DataReceiver mDataReceiver;
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
        mLineChart = (LineChart) v.findViewById(R.id.chart);

        mLineChart.setBackgroundColor(Color.WHITE);
        mLineChart.setDescription("");
        mLineChart.setNoDataText(getString(R.string.no_chart_data));
        mLineChart.setDrawGridBackground(true);
        mLineChart.setGridBackgroundColor(Color.LTGRAY);
        mLineChart.setDrawBorders(false);

        /*
        *       Y-Axis stuff
         */
        YAxis yAxisRight = mLineChart.getAxisRight();
        yAxisRight.setEnabled(false);

        YAxis yAxis = mLineChart.getAxisLeft();
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
        upperLimit.setLineColor(DARK_RED);
        upperLimit.setLineWidth(2f);
        upperLimit.setTextColor(DARK_RED);
        upperLimit.setTextSize(10f);
        upperLimit.enableDashedLine(10, 10, 0);
        upperLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);

        yAxis.addLimitLine(upperLimit);
        LimitLine lowerLimit = new LimitLine(-20.0f, getString(R.string.max_backward_tilt));
        lowerLimit.setLineColor(DARK_RED);
        lowerLimit.setLineWidth(2f);
        lowerLimit.setTextColor(DARK_RED);
        lowerLimit.setTextSize(10f);
        lowerLimit.enableDashedLine(10, 10, 0);
        lowerLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);
        yAxis.addLimitLine(lowerLimit);

        LimitLine zeroLimit = new LimitLine(0f, getString(R.string.best_posture));
        zeroLimit.setLineColor(DARK_GREEN);
        zeroLimit.setLineWidth(2f);
        zeroLimit.setTextColor(DARK_GREEN);
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
        mIndex = 0;
        mPostureSamples = new ArrayList<Entry>();
        mLineDataSet = new LineDataSet(mPostureSamples, getString(R.string.posture_readings));
        mLineDataSet.setValueTextSize(0);            // Can't find any other way to disable txt
        mLineDataSet.setCircleSize(2);
        mLineDataSet.setDrawCircleHole(true);
        mLineDataSet.setLineWidth(2);
        mLineDataSet.setColor(Color.BLACK);
        mLineDataSet.setCircleColor(Color.BLACK);

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(mLineDataSet);
        ArrayList<String> xVals = new ArrayList<String>();

        mLineData = new LineData(xVals, dataSets);      // LineData is a subclass of ChartData
        mLineChart.setGridBackgroundColor(Color.WHITE);
        mLineChart.setVisibleXRange(DATA_POINTS_TO_SHOW);
        mLineChart.setData(mLineData);

        new LoadFromDatabase().execute();
        mChartValid = true;
        mLineChart.invalidate();

        /*
        *       Chart interactions
         */
        mLineChart.setDragEnabled(true);
        mLineChart.setPinchZoom(false);

        // Register the broadcast receiver
        LocalBroadcastManager.getInstance(getActivity())
                .registerReceiver(mDataReceiver, new IntentFilter(OrientationService.DATA_INTENT));

        return v;
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
            List<Sample> list = new Select()
                    .from(Sample.class)
                    .orderBy("_ID ASC")
                    .execute();
            return list;
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

        public DataReceiver() { }

        @Override
        public void onReceive(Context c, Intent i) {
            int value = i.getIntExtra(OrientationService.EXTRA_VALUE, Orientation.IMPOSSIBLE_INTEGER);
            addPoint(value);
        }
    }
}
