@file:Suppress("DEPRECATION")

package xyz.pisoj.holo1

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v4.view.ViewPager
import android.view.View
import android.widget.TabHost


class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val viewPager: ViewPager = findViewById(R.id.viewPager)
        viewPager.adapter = MainPagerAdapter(supportFragmentManager)

        val tabHost: TabHost = findViewById(R.id.tabHost)
        tabHost.setup()
        MainPages.entries.forEach { page ->
            tabHost.addTab(tabHost.newTabSpec(page.ordinal.toString()).setIndicator(page.title).setContent { View(this) })
        }
        tabHost.setOnTabChangedListener { tag ->
            viewPager.currentItem = tag.toInt()
        }

        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) = Unit

            override fun onPageScrollStateChanged(state: Int) = Unit

            override fun onPageSelected(position: Int) {
                tabHost.setCurrentTabByTag(position.toString())
            }
        })
    }
}