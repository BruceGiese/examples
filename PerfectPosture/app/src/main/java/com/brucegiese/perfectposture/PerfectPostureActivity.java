package com.brucegiese.perfectposture;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.PagerTabStrip;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;


/**
 * This displays the orientation of the device with respect to gravity.
 * It can be enabled/disabled via a big_checkbox and has configurable
 * parameters as implemented in TiltFragment.
 *
 */
public class PerfectPostureActivity extends Activity {
    private final static String TAG = "com.brucegiese.perfpost";
    private final static int INTRO_CONTROL_PAGE = 0;
    private final static int DATA_PAGE = 1;
    private final static int SETTINGS_PAGE = 2;
    private final static int LAST_PAGE_NUM = SETTINGS_PAGE;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;
    private int mPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_perfect_posture);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setOffscreenPageLimit(LAST_PAGE_NUM+1);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        mViewPager.setOnPageChangeListener( new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected( int position) {
                if( mPosition == 3 && position == 2 ) {
                    // We changed from settings to the chart page.  Settings may have changed
                    // TODO: replace the existing GraphFragment with a new one (or some other solution)
                }
            }

            @Override public void onPageScrollStateChanged(int arg0){
            }
            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

        });

        final PagerTabStrip pts = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        pts.setTextColor(getResources().getColor(R.color.medium_secondary_color));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSectionsPagerAdapter = null;
    }

    /**
     * Pager adapter for main activity which is just a ViewPager for the small
     * number of pages we need for things like configuration, the chart, saved
     * data.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch( position ) {

                case INTRO_CONTROL_PAGE:
                    return new TiltFragment();

                case DATA_PAGE:
                    return new GraphFragment();

                case SETTINGS_PAGE:
                    return new SettingsFragment();

                default:
                    Log.e(TAG, "Internal error: getItem() position out of range: " + position);
                    return null;
            }
        }

        @Override
        public int getCount() {
            return LAST_PAGE_NUM + 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case INTRO_CONTROL_PAGE:
                    return getString(R.string.page_title_intro).toUpperCase();

                case DATA_PAGE:
                    return getString(R.string.page_title_data).toUpperCase();

                case SETTINGS_PAGE:
                    return getString(R.string.page_title_settings).toUpperCase();

                default:
                    return "INTERNAL ERROR";

            }
        }

    }
}
