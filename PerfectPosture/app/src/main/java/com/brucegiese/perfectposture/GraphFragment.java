package com.brucegiese.perfectposture;


import android.app.Activity;
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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;

public class GraphFragment extends Fragment {
    private static final String TAG = "com.brucegiese.graph";
    private static final int DATA_POINTS_TO_SHOW = 30;
    private Activity mActivity;
    private LineChart mLineChart;
    private int mIndex = 0;
    ArrayList<Entry> mPostureSamples;
    LineData mLineData;
    LineDataSet mPostureDataSet;

    public GraphFragment() {
        Log.d(TAG, "GraphFragment constructor called");
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mActivity = activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "GraphFragment onCreateView() called");
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_graph, container, false);
        mLineChart = (LineChart)v.findViewById(R.id.chart);

        mLineChart.setBackgroundColor(Color.WHITE);
        // TODO: Change these strings to resources
        mLineChart.setDescription("");
        mLineChart.setNoDataText("Start posture detection to create a chart!");
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
        yAxis.setLabelCount(9);
        yAxis.setStartAtZero(false);
        yAxis.setAxisMaxValue(199);
        yAxis.setAxisMinValue(-199);
        yAxis.setSpaceTop(4.0f);       // leave this much percent space above max value
        yAxis.setSpaceBottom(4.0f);    // leave this much percent space below min value
        // TODO: Change this string to a resource
        LimitLine upperLimit = new LimitLine(20.0f, "Max forward tilt");
        upperLimit.setLineColor(Color.RED);
        upperLimit.setLineWidth(.5f);
        upperLimit.setTextColor(Color.RED);
        upperLimit.setTextSize(8f);
        upperLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);
        yAxis.addLimitLine(upperLimit);
        // TODO: Change this string to a resource
        LimitLine lowerLimit = new LimitLine(-20.0f, "Max backward tilt");
        lowerLimit.setLineColor(Color.RED);
        lowerLimit.setLineWidth(.5f);
        lowerLimit.setTextColor(Color.RED);
        lowerLimit.setTextSize(8f);
        lowerLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);
        yAxis.addLimitLine(lowerLimit);
        // TODO: Change this string to a resource
        LimitLine zeroLimit = new LimitLine(0f, "Best posture");
        zeroLimit.setLineColor(Color.GREEN);
        zeroLimit.setLineWidth(1f);
        zeroLimit.setTextColor(Color.GREEN);
        zeroLimit.setTextSize(8f);
        yAxis.addLimitLine(zeroLimit);

        /*
        *       X-Axis stuff
         */
        XAxis xAxis = mLineChart.getXAxis();
        xAxis.setDrawLabels(false);
        xAxis.setEnabled(false);

        /*
        *       Data
         */
        mPostureSamples = new ArrayList<Entry>();
        // TODO: Change this string to a resource
        mPostureDataSet = new LineDataSet(mPostureSamples, "Posture Readings");
        mPostureDataSet.setValueTextSize(0);            // Can't find any other way to disable txt
        mPostureDataSet.setCircleSize(2);
        mPostureDataSet.setLineWidth(2);
        mPostureDataSet.setColor(Color.BLACK);
        mPostureDataSet.setCircleColor(Color.BLACK);

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(mPostureDataSet);
        ArrayList<String> xVals = new ArrayList<String>();
        xVals.add("");                                  // just to be safe

        // Don't use any X-axis labels
        mLineData = new LineData(xVals, dataSets);      // LineData is a subclass of ChartData

        mLineChart.setGridBackgroundColor(Color.WHITE);
        mLineChart.setData(mLineData);
        mLineChart.invalidate();
        return v;
    }


    public void addNewPoint(int value) {
        if( value > 180 ) {                     // constrain to plus or minus 180 degrees.
            value = 180;
        }
        if( value < -180 ) {
            value = -180;
        }
        Log.d(TAG, "addNewPoint() index=" + mIndex);
        Entry point = new Entry((float) value, mIndex);

//        mPostureDataSet.addEntry(point);
        mLineData.addXValue("");                // we don't want to show an X value
        mLineData.addEntry(point, 0);           // We have one dataset, it's index is 0
        mLineChart.notifyDataSetChanged();
        if( mIndex >= DATA_POINTS_TO_SHOW) {
            mLineChart.setVisibleXRange(DATA_POINTS_TO_SHOW);
            mLineChart.moveViewToX(mLineData.getXValCount() - DATA_POINTS_TO_SHOW);
        }
        mLineChart.invalidate();
        mIndex++;
    }

}
