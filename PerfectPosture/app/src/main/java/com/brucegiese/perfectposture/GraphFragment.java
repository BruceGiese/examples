package com.brucegiese.perfectposture;

import java.util.ArrayList;
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


public class GraphFragment extends Fragment {
    private static final String TAG = "com.brucegiese.graph";
    private static final int DATA_POINTS_TO_SHOW = 30;

    private LineChart mLineChart;
    private int mIndex;
    ArrayList<Entry> mPostureSamples;
    LineData mLineData;
    LineDataSet mLineDataSet;
    private boolean mChartValid = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_graph, container, false);
        mLineChart = (LineChart)v.findViewById(R.id.chart);

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
        yAxis.setLabelCount(9);
        yAxis.setStartAtZero(false);
        yAxis.setAxisMaxValue(90);
        yAxis.setAxisMinValue(-90);
        yAxis.setSpaceTop(4.0f);       // leave this much percent space above max value
        yAxis.setSpaceBottom(4.0f);    // leave this much percent space below min value

        LimitLine upperLimit = new LimitLine(20.0f, getString(R.string.max_forward_tilt));
        upperLimit.setLineColor(Color.RED);
        upperLimit.setLineWidth(2f);
        upperLimit.setTextColor(Color.RED);
        upperLimit.setTextSize(10f);
        upperLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);

        yAxis.addLimitLine(upperLimit);
        LimitLine lowerLimit = new LimitLine(-20.0f, getString(R.string.max_backward_tilt));
        lowerLimit.setLineColor(Color.RED);
        lowerLimit.setLineWidth(2f);
        lowerLimit.setTextColor(Color.RED);
        lowerLimit.setTextSize(10f);
        lowerLimit.setLabelPosition(LimitLine.LimitLabelPosition.POS_LEFT);
        yAxis.addLimitLine(lowerLimit);

        LimitLine zeroLimit = new LimitLine(0f, getString(R.string.best_posture));
        zeroLimit.setLineColor(Color.GREEN);
        zeroLimit.setLineWidth(2f);
        zeroLimit.setTextColor(Color.GREEN);
        zeroLimit.setTextSize(10f);
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
        xVals.add("");                                  // just to be safe

        mLineData = new LineData(xVals, dataSets);      // LineData is a subclass of ChartData
        mLineChart.setGridBackgroundColor(Color.WHITE);
        mLineChart.setData(mLineData);
        mChartValid = true;
        mLineChart.invalidate();
        return v;
    }


    public void addNewPoint(int value) {
        if( value > 90 ) {                     // constrain to plus or minus 180 degrees.
            value = 90;
        }
        if( value < -90 ) {
            value = -90;
        }

        if( mChartValid) {                      // The chart is not valid during rotations, etc.
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
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
}
