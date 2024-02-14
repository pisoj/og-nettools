@file:Suppress("DEPRECATION")

package xyz.pisoj.holo1

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.SeekBar
import android.widget.SlidingDrawer
import android.widget.TabHost
import kotlinx.parcelize.Parcelize
import xyz.pisoj.holo1.model.Host
import xyz.pisoj.holo1.model.toHostStatus
import java.net.InetAddress
import kotlin.concurrent.thread


class MainActivity : Activity() {

    private lateinit var state: State
    private lateinit var tabHost: TabHost
    private lateinit var drawer: SlidingDrawer
    private lateinit var mask: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        state = savedInstanceState?.getParcelable("state") ?: State()

        setupTabHost()
        setupQueryButton()
        setupDrawer()
        setupPingDelaySeekBar()
        if(state.isOperationActive) {
            state.operation(this, resetState = false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("state", state)
    }

    override fun onDestroy() {
        super.onDestroy()
        state = state.copy(isOperationActive = false)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawer.isOpened) {
            closeDrawer()
        } else {
            super.onBackPressed()
        }
    }

    private fun setupTabHost() {
        tabHost = findViewById(R.id.tabHost)
        tabHost.setup()
        Operation.entries.forEach { operation ->
            tabHost.addTab(
                tabHost.newTabSpec(operation.name)
                    .setIndicator(operation.title)
                    .setContent {
                        operation.createContent(this)
                    }
            )
        }
        tabHost.setOnTabChangedListener {
            state = state.copy(operation = Operation.valueOf(it))
        }
    }

    private fun setupDrawer() {
        mask = findViewById(R.id.mask)
        drawer = findViewById(R.id.drawer)
        findViewById<ImageButton>(R.id.preferences).setOnClickListener { openDrawer() }
        findViewById<ImageButton>(R.id.drawerBack).setOnClickListener { closeDrawer() }
    }

    private fun openDrawer() {
        mask.visibility = View.VISIBLE
        mask.startAnimation(AlphaAnimation(0f, 1f).apply {
            duration = 200
            fillAfter = true
        })
        drawer.animateOpen()
    }

    private fun closeDrawer() {
        mask.startAnimation(AlphaAnimation(1f, 0f).apply {
            duration = 200
            fillAfter = true
        })
        drawer.animateClose()
        mask.visibility = View.GONE
    }

    private fun setupQueryButton() {
        val hostEditText = findViewById<EditText>(R.id.host)
        val queryButton = findViewById<ImageButton>(R.id.query)
        queryButton.setImageResource(
            if(state.isOperationActive) R.drawable.ic_stop else R.drawable.ic_query
        )
        queryButton.setOnClickListener {
            state = state.copy(
                host = hostEditText.text.toString(),
                isOperationActive = !state.isOperationActive
            )
            (it as ImageButton).setImageResource(
                if(state.isOperationActive) R.drawable.ic_stop else R.drawable.ic_query
            )
            if(state.isOperationActive) {
                state.operation(this, resetState = true)
            }
        }
    }

    private fun setupPingDelaySeekBar() {
        val pingDelay = findViewById<SeekBar>(R.id.pingDelay)
        pingDelay.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                state = state.copy(pingDelay = progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        pingDelay.progress = state.pingDelay
    }

    @Parcelize
    private data class State(
        val operation: Operation = Operation.Ping,
        val isOperationActive: Boolean = false,
        val host: String = "",
        val pingDelay: Int = 1000,
    ) : Parcelable

    /**
     * @param title User visible name of the operation
     */
    private enum class Operation(val title: String) {

        Ping(title = "Ping") {
            private val adapter by lazy { HostListAdapter(mutableListOf()) }

            override fun createContent(context: Context): ListView {
                return ListView(context).apply {
                    divider = null
                    adapter =  this@Ping.adapter
                }
            }

            override operator fun invoke(mainActivity: MainActivity, resetState: Boolean) {
                if(resetState) {
                    adapter.hosts = mutableListOf()
                    adapter.notifyDataSetChanged()
                }
                thread {
                    ping(mainActivity) { host ->
                        mainActivity.runOnUiThread {
                            adapter.hosts.add(index = 0, host)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }

            private fun ping(mainActivity: MainActivity, onNewPing: (host: Host) -> Unit) {
                val hostInet = InetAddress.getByName(mainActivity.state.host)
                while (mainActivity.state.isOperationActive) {
                    val start = System.currentTimeMillis()
                    val status = hostInet.isReachable(1500).toHostStatus()
                    val latency = System.currentTimeMillis() - start
                    onNewPing(Host(mainActivity.state.host, if(status == Host.Status.Unavailable) null else latency, status))

                    (1..mainActivity.state.pingDelay / 100).forEach { _ ->
                        if (!mainActivity.state.isOperationActive) return
                        Thread.sleep(100)
                    }
                }
            }
        };

        abstract operator fun invoke(mainActivity: MainActivity, resetState: Boolean)

        abstract fun createContent(context: Context): View
    }
}