package com.brucegiese.perfectposture;

import java.util.ArrayList;
import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.DataSet;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;


public class GraphFragment extends Fragment {
    private static final String TAG = "com.brucegiese.graph";
    private static final String VALUES_KEY = "com.brucegiese.values";
    private static final int DATA_POINTS_TO_SHOW = 30;
    private static final int DARK_GREEN = 0xFF006600;
    private static final int DARK_RED = 0xFFB22222;

    private LineChart mLineChart;
    private int mIndex;
    ArrayList<Entry> mPostureSamples;
    LineData mLineData;
    LineDataSet mLineDataSet;
    private boolean mChartValid = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate() called");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView() called");
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

        Log.d(TAG, "Starting with new data");
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
        mLineChart.setData(mLineData);

        // See if we have any saved data to add
        if (savedInstanceState != null) {
            int[] values = savedInstanceState.getIntArray(VALUES_KEY);
            if (values != null) {
                Log.d(TAG, "We have saved data with " + values.length + " samples.");
                for (int i = 0; i < values.length; i++) {
                    mLineData.addXValue("");
                    mLineData.addEntry(new Entry((float) values[i], i), 0);
                }
                mIndex = values.length;
            }
        } else {
            Log.d(TAG, "no saved data");
        }

        mChartValid = true;
        mLineChart.invalidate();
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if( mLineData != null) {
            Log.d(TAG, "saving mLineData into Bundle");
            DataSet d = mLineData.getDataSetByIndex(0);
            int [] values = new int[d.getValueCount()];
            for( int i=0; i<d.getEntryCount(); i++) {
                values[i] = Math.round(d.getEntryForXIndex(i).getVal());
            }
            outState.putIntArray(VALUES_KEY, values);
        } else {
            Log.d(TAG, "can't save mLineData into Bundle");
        }
    }


    public void addNewPoint(int value) {
        if( value > 90 ) {                          // constrain to plus or minus 90 degrees.
            value = 90;
        }
        if( value < -90 ) {
            value = -90;
        }

        if( mChartValid) {                          // chart is not valid during rotations etc.
            Log.d(TAG, "addNewPoint() " + value);
            Entry point = new Entry((float) value, mIndex);
            mLineData.addXValue("");                // we don't want to show an X value
            mLineData.addEntry(point, 0);           // We have one dataset, it's index is 0
            mLineChart.notifyDataSetChanged();
            if (mIndex >= DATA_POINTS_TO_SHOW) {
                mLineChart.setVisibleXRange(DATA_POINTS_TO_SHOW);
                mLineChart.moveViewToX(mLineData.getXValCount() - DATA_POINTS_TO_SHOW);
            }
            mLineChart.invalidate();
            mIndex++;
        } else {
            Log.d(TAG, "addNewPoint() mChartValid is false");
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView() called");
        // Cleanup whatever we can... and hope garbage collection does the rest.
        mChartValid = false;
        mLineChart.clear();
        mLineChart.setEnabled(false);
        mLineChart.removeAllViews();        // Not sure if this is needed
        mLineChart = null;
        mLineData.removeDataSet(0);
        mLineData = null;
        mLineDataSet = null;
        mPostureSamples = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
    }
}
