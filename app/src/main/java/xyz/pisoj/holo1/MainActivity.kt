@file:Suppress("DEPRECATION")

package xyz.pisoj.holo1

import android.app.Activity
import android.os.Bundle
import android.os.Parcelable
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.TabHost
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import xyz.pisoj.holo1.model.Host
import xyz.pisoj.holo1.model.toHostStatus
import java.net.InetAddress
import kotlin.concurrent.thread


class MainActivity : Activity() {

    private lateinit var state: State

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        state = savedInstanceState?.getParcelable("state") ?: State()

        val hostListView = ListView(this)
        val hostListAdapter = HostListAdapter(state.hostList)
        hostListView.apply {
            divider = null
            adapter = hostListAdapter
        }
        startOperation(hostListAdapter)

        val tabHost: TabHost = findViewById(R.id.tabHost)
        tabHost.setup()
        Operation.entries.forEach { operation ->
            if(operation.title == null) return@forEach
            tabHost.addTab(
                tabHost.newTabSpec(operation.name)
                    .setIndicator(operation.title)
                    .setContent { hostListView }
            )
        }

        val hostEditText = findViewById<EditText>(R.id.host)
        val queryButton = findViewById<ImageButton>(R.id.query)
        queryButton.setImageResource(
            if(state.operation != Operation.Idle) R.drawable.ic_stop else R.drawable.ic_query
        )
        queryButton.setOnClickListener {
            state = state.copy(
                host = hostEditText.text.toString(),
                operation = if(state.operation == Operation.Idle) Operation.valueOf(tabHost.currentTabTag!!) else Operation.Idle
            )
            (it as ImageButton).setImageResource(
                if(state.operation != Operation.Idle) R.drawable.ic_stop else R.drawable.ic_query
            )
            startOperation(hostListAdapter)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("state", state)
    }

    override fun onDestroy() {
        super.onDestroy()
        state = state.copy(operation = Operation.Idle)
    }

    private fun startOperation(hostListAdapter: HostListAdapter) {
        when(state.operation) {
            Operation.Ping -> {
                state = state.copy(hostList = mutableListOf())
                hostListAdapter.hosts = state.hostList
                hostListAdapter.notifyDataSetChanged()
                thread {
                    ping(1000) { host ->
                        runOnUiThread {
                            state.hostList.add(index = 0, host)
                            hostListAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    private fun ping(speedMillis: Int, onNewPing: (host: Host) -> Unit) {
        val hostInet = InetAddress.getByName(state.host)
        while (true) {
            val start = System.currentTimeMillis()
            val status = hostInet.isReachable(1500).toHostStatus()
            val latency = System.currentTimeMillis() - start
            onNewPing(Host(state.host, if(status == Host.Status.Unavailable) null else latency, status))

            (1..speedMillis / 100).forEach { _ ->
                Thread.sleep(100)
                if (state.operation != Operation.Ping) return
            }
        }
    }

    @Parcelize
    private data class State(
        val operation: Operation = Operation.Idle,
        val host: String = "",
        val hostList: @RawValue MutableList<Host> = mutableListOf(),
    ) : Parcelable

    /**
     * @param title User visible name of an operation
     */
    private enum class Operation(val title: String? = null) {
        Idle,
        Ping("Ping"),
    }
}