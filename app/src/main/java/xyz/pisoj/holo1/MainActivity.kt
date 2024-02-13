@file:Suppress("DEPRECATION")

package xyz.pisoj.holo1

import android.app.Activity
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TabHost
import xyz.pisoj.holo1.model.Host
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val hostList = mutableListOf<Host>()
        val hostListView = HostListView(this, hostList)

        val tabHost: TabHost = findViewById(R.id.tabHost)
        tabHost.setup()
        tabHost.addTab(
            tabHost.newTabSpec("ping")
                .setIndicator("Ping")
                .setContent { hostListView }
        )

        findViewById<ImageButton>(R.id.query).setOnClickListener {
        }
    }
}