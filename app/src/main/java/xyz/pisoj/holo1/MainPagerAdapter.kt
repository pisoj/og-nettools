package xyz.pisoj.holo1

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class MainPagerAdapter(fragmentManager: FragmentManager): FragmentStatePagerAdapter(fragmentManager) {

    override fun getCount(): Int {
        return MainPages.entries.size
    }

    override fun getItem(position: Int): Fragment {
        return MainPages.entries[position].fragment
    }
}