package edu.dartmouth.cs.racetraq.Adapters;


import android.support.v4.app.FragmentManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class BottomNavigationViewPagerAdapter extends FragmentPagerAdapter {
    private ArrayList<Fragment> fragments;

    public BottomNavigationViewPagerAdapter(FragmentManager fm, ArrayList<Fragment> fragments){
        super(fm);
        this.fragments = fragments;
    }
    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}

