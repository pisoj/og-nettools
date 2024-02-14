@file:Suppress("DEPRECATION")

package xyz.pisoj.holo1

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SlidingDrawer
import android.widget.TabHost
import android.widget.TextView
import kotlinx.parcelize.Parcelize
import xyz.pisoj.holo1.model.Host
import xyz.pisoj.holo1.model.toHostStatus
import xyz.pisoj.holo1.utils.dpToPixels
import xyz.pisoj.holo1.utils.whois
import java.net.InetAddress
import java.net.UnknownHostException
import kotlin.concurrent.thread


class MainActivity : Activity() {

    private lateinit var state: State
    private lateinit var tabHost: TabHost
    private lateinit var drawer: SlidingDrawer
    private lateinit var mask: View
    private lateinit var queryButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        state = savedInstanceState?.getParcelable("state") ?: State()

        setupTabHost()
        setupQueryButton()
        setupPreferences()
        if(state.isOperationActive) {
            state.operation(this, shouldResetState = false)
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
        tabHost.setCurrentTabByTag(state.operation.name)
        tabHost.setOnTabChangedListener {
            state = state.copy(operation = Operation.valueOf(it), isOperationActive = false)
            updateQueryButton()
        }
    }

    private fun setupPreferences() {
        mask = findViewById(R.id.mask)
        drawer = findViewById(R.id.drawer)
        findViewById<ImageButton>(R.id.preferences).setOnClickListener { openDrawer() }
        findViewById<ImageButton>(R.id.drawerBack).setOnClickListener { closeDrawer() }
        if (state.isDrawerOpened) openDrawer()

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

    private fun openDrawer() {
        mask.visibility = View.VISIBLE
        mask.startAnimation(AlphaAnimation(0f, 1f).apply {
            duration = 200
            fillAfter = true
        })
        drawer.animateOpen()
        state = state.copy(isDrawerOpened = true)
    }

    private fun closeDrawer() {
        mask.startAnimation(AlphaAnimation(1f, 0f).apply {
            duration = 200
            fillAfter = true
        })
        drawer.animateClose()
        mask.visibility = View.GONE
        state = state.copy(isDrawerOpened = false)
    }

    private fun setupQueryButton() {
        val hostEditText = findViewById<EditText>(R.id.host)
        queryButton = findViewById(R.id.query)
        updateQueryButton()
        val whoisServer = findViewById<EditText>(R.id.whoisServer)
        val whoisPort = findViewById<EditText>(R.id.whoisPort)
        queryButton.setOnClickListener {
            state = state.copy(
                host = hostEditText.text.toString(),
                whoisServer = whoisServer.text.toString().ifBlank { "whois.iana.org" },
                whoisPort = whoisPort.text.toString().ifBlank { "43" }.toInt(),
                isOperationActive = !state.isOperationActive
            )
            updateQueryButton()
            if(state.isOperationActive) {
                state.operation(this, shouldResetState = true)
            }
        }
    }

    private fun updateQueryButton() {
        queryButton.setImageResource(
            if(state.isOperationActive) R.drawable.ic_stop else R.drawable.ic_query
        )
    }

    @Parcelize
    private data class State(
        val operation: Operation = Operation.Ping,
        val isOperationActive: Boolean = false,
        val isDrawerOpened: Boolean = false,
        val host: String = "",
        val pingDelay: Int = 1000,
        val whoisServer: String = "whois.iana.org",
        val whoisPort: Int = 43,
    ) : Parcelable

    /**
     * @param title User visible name of the operation
     */
    private enum class Operation(val title: String) {

        Ping(title = "Ping") {
            private val adapter by lazy { HostListAdapter(mutableListOf()) }

            override operator fun invoke(mainActivity: MainActivity, shouldResetState: Boolean) {
                if(shouldResetState) {
                    adapter.hosts = mutableListOf()
                    adapter.notifyDataSetChanged()
                }
                thread {
                    try {
                        ping(mainActivity) { host ->
                            mainActivity.runOnUiThread {
                                adapter.hosts.add(index = 0, host)
                                adapter.notifyDataSetChanged()
                            }
                        }
                    } catch (e: UnknownHostException) {
                        mainActivity.runOnUiThread {
                            mainActivity.state = mainActivity.state.copy(isOperationActive = false)
                            AlertDialog.Builder(mainActivity)
                                .setTitle("Failed to resolve the host name")
                                .setPositiveButton("Close") { _, _ -> }
                                .show()
                            mainActivity.updateQueryButton()
                        }
                    }
                }
            }

            override fun createContent(context: Context): View {
                return ListView(context).apply {
                    divider = null
                    adapter =  this@Ping.adapter
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
        },

        /*Dns(title = "Dns") {
            private lateinit var textView: TextView

            override fun invoke(mainActivity: MainActivity, shouldResetState: Boolean) {
                thread {
                    try {
                        val output = InetAddress.getAllByName(mainActivity.state.host)
                            .ifEmpty { throw UnknownHostException() }.joinToString("\n") { it.hostAddress!! }
                        mainActivity.runOnUiThread {
                            textView.text = output
                            mainActivity.state = mainActivity.state.copy(isOperationActive = false)
                            mainActivity.updateQueryButton()
                        }
                    } catch (e: UnknownHostException) {
                        mainActivity.runOnUiThread {
                            AlertDialog.Builder(mainActivity)
                                .setTitle("Cannot look up the domain")
                                .setPositiveButton("Close") { _, _ -> }
                                .show()
                            mainActivity.state = mainActivity.state.copy(isOperationActive = false)
                            mainActivity.updateQueryButton()
                        }
                    }
                }
            }

            override fun createContent(context: Context): View {
                textView = TextView(context).apply {
                    val paddingInPixels = context.dpToPixels(16)
                    setPadding(paddingInPixels, paddingInPixels, paddingInPixels, 0)
                }
                return textView
            }
        },*/

        Whois(title = "Whois") {
            private lateinit var textView: TextView

            override fun invoke(mainActivity: MainActivity, shouldResetState: Boolean) {
                thread {
                    try {
                        val output = whois(
                            domain = mainActivity.state.host,
                            whoisServer = mainActivity.state.whoisServer,
                            whoisPort = mainActivity.state.whoisPort
                        )
                        mainActivity.runOnUiThread {
                            textView.text = output
                        }
                    } catch (e: UnknownHostException) {
                        mainActivity.runOnUiThread {
                            AlertDialog.Builder(mainActivity)
                                .setTitle("Failed to resolve the host name of the whois server")
                                .setMessage("Please check you internet connection and whois server configuration.")
                                .setPositiveButton("Close") { _, _ -> }
                                .show()
                        }
                    } catch (e: Exception) {
                        mainActivity.runOnUiThread {
                            AlertDialog.Builder(mainActivity)
                                .setTitle("Failed to query the whois server")
                                .setMessage("Please check you whois server configuration. ${e.message.orEmpty()}")
                                .setPositiveButton("Close") { _, _ -> }
                                .show()
                        }
                        e.printStackTrace()
                    } finally {
                        mainActivity.runOnUiThread {
                            mainActivity.state = mainActivity.state.copy(isOperationActive = false)
                            mainActivity.updateQueryButton()
                        }
                    }
                }
            }

            override fun createContent(context: Context): View {
                val paddingInPixels = context.dpToPixels(16)
                textView = TextView(context).apply {
                    setPadding(paddingInPixels, paddingInPixels, paddingInPixels, paddingInPixels)
                    setTextIsSelectable(true)
                }
                return ScrollView(context).apply { addView(textView) }
            }
        };

        /**
         * @param shouldResetState Should the operation reset the output of a previous run or should
         * it just append the new output to the previous one. It's okay to ignore this when implementing a new operation if it doesn't make sense.
         */
        abstract operator fun invoke(mainActivity: MainActivity, shouldResetState: Boolean)

        abstract fun createContent(context: Context): View
    }
}