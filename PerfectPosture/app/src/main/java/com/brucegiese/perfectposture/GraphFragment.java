package com.brucegiese.perfectposture;


import android.graphics.Color;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.achartengine.ChartFactory;
import org.achartengine.GraphicalView;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

public class GraphFragment extends Fragment {
    private static final String TAG = "com.brucegiese.graph";
    int index = 0;
    // Stores X,Y pairs
    private XYSeries mSeries;
    // Stores multiple X,Y objects and plots them
    private XYMultipleSeriesDataset mDataset = new XYMultipleSeriesDataset();
    // Renders a single X,Y series
    private XYSeriesRenderer mRenderer;
    // Renders all the X,Y points in the data set
    private XYMultipleSeriesRenderer mMultRenderer = new XYMultipleSeriesRenderer();
    // View object for displaying the graph
    private GraphicalView mChart;

    private static final int X_WINDOW_SIZE = 30;
    private int mXmin;              // create a sliding window of a fixed size
    private int mXmax;

    public GraphFragment() {
        Log.d(TAG, "GraphFragment constructor called");
        // TODO: use a resource for this string.
        mSeries = new XYSeries("Posture");
        mDataset.addSeries(mSeries);
        mRenderer = new XYSeriesRenderer();
        mRenderer.setColor(Color.BLUE);
        mRenderer.setLineWidth(4);
        mRenderer.setFillPoints(false);
        mMultRenderer.addSeriesRenderer(mRenderer);
        mMultRenderer.setBackgroundColor(Color.WHITE);
        mMultRenderer.setMarginsColor(Color.WHITE);
        mMultRenderer.setYTitle("Posture Angle (degrees)");
        mMultRenderer.setApplyBackgroundColor(true);
        mMultRenderer.setAxisTitleTextSize(40.0f);
        mMultRenderer.setChartTitleTextSize(40.0f);
        mMultRenderer.setLabelsTextSize(30.0f);
        mMultRenderer.setMargins(new int[]{50, 100, 40, 100});
        mMultRenderer.setZoomEnabled(false, false);
        mMultRenderer.setPanEnabled(false, false);
        mMultRenderer.setShowGridY(false);
        mMultRenderer.setShowGridX(true);
        mMultRenderer.setGridColor(Color.LTGRAY);
        mMultRenderer.setYAxisMin(-180);
        mMultRenderer.setYAxisMax(180);
        mMultRenderer.setBarSpacing(20);
        mMultRenderer.setXLabels(0);



        // TODO: use resources for these strings
        mMultRenderer.addYTextLabel(50, "bad");
        mMultRenderer.addYTextLabel(0, "good");
        mMultRenderer.addYTextLabel(-50, "bad");

        mXmin = 0;
        mXmax = X_WINDOW_SIZE;
        mMultRenderer.setXAxisMin( (double)mXmin);
        mMultRenderer.setXAxisMax( (double)mXmax);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "GraphFragment onCreateView() called");
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_graph, container, false);
        FrameLayout graph = (FrameLayout)v.findViewById(R.id.graph);
        graph.setFocusable(false);

        mChart = ChartFactory.getLineChartView(getActivity(), mDataset, mMultRenderer);
        graph.addView(mChart);
        return v;
    }


    public void addNewPoint(int value){

        // Get rid of any annotations immediately before we add the next point.
        if( mSeries.getAnnotationCount() > 0 ) {
            // The index here means the annotation's index, which is always zero
            // since we never have more than one on the screen.
            mSeries.removeAnnotation(0);
        }

        mSeries.add((double)index, (double)value);

        if( value > PostureResults.mZAxisPosThreshold) {
            Log.d(TAG, "addNewPoint(): above the range at " + value);
            mRenderer.setColor(Color.RED);
            // TODO: use a resource for this string
            mSeries.addAnnotation("HIGH", (double)(index), (double)value);

        } else if( value < PostureResults.mZAxisNegThreshold) {
            Log.d(TAG, "addNewPoint(): below the range at " + value);
            mRenderer.setColor(Color.RED);
            // TODO: use a resource for this string
            mSeries.addAnnotation("LOW", (double)(index), (double)value);

        } else {
            Log.d(TAG, "addNewPoint(): new value " + value);
            mRenderer.setColor(Color.GREEN);
        }

        // This implements a sliding window on the screen.
        if( (double)index >= X_WINDOW_SIZE) {
            mXmin++;
            mXmax++;
            mMultRenderer.setXAxisMin( (double)mXmin);
            mMultRenderer.setXAxisMax( (double)mXmax);
        }

        mChart.repaint();
        index++;
    }

}
