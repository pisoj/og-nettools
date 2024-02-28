@file:Suppress("DEPRECATION")

package xyz.pisoj.og.nettools

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ListView
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.SlidingDrawer
import android.widget.Spinner
import android.widget.TabHost
import android.widget.TextView
import android.widget.Toast
import kotlinx.parcelize.Parcelize
import org.json.JSONObject
import xyz.pisoj.og.nettools.model.DnsRecord
import xyz.pisoj.og.nettools.model.DnsRecordType
import xyz.pisoj.og.nettools.model.Host
import xyz.pisoj.og.nettools.model.toHostStatus
import xyz.pisoj.og.nettools.utils.dpToPixels
import xyz.pisoj.og.nettools.utils.formatLatencyMillis
import xyz.pisoj.og.nettools.utils.whois
import java.net.InetAddress
import java.net.URL
import java.net.URLEncoder
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
                state = state.copy(pingDelay = progress + 300)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        pingDelay.progress = state.pingDelay

        val useAlternativePingMethod = findViewById<CheckBox>(R.id.alternativePingMethod)
        useAlternativePingMethod.setOnCheckedChangeListener { _, isChecked ->
            state = state.copy(useAlternativePingMethod = isChecked)
        }
        useAlternativePingMethod.isChecked = state.useAlternativePingMethod

        val dnsRecordType = findViewById<Spinner>(R.id.dnsRecordType)
        ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item).apply {
            addAll(DnsRecordType.entries.map { it.name })
            dnsRecordType.adapter = this
        }
        dnsRecordType.setSelection(state.dnsRecordType.ordinal)
        dnsRecordType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                state = state.copy(dnsRecordType = DnsRecordType.entries[position])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
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
        val pingTimeout = findViewById<EditText>(R.id.pingTimeoutMillis)
        val whoisServer = findViewById<EditText>(R.id.whoisServer)
        val whoisPort = findViewById<EditText>(R.id.whoisPort)
        queryButton.setOnClickListener {
            state = state.copy(
                host = hostEditText.text.toString(),
                pingTimeout = pingTimeout.text.toString().ifBlank { "3000" }.toInt(),
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
        val pingTimeout: Int = 3000,
        val useAlternativePingMethod: Boolean = false,
        val dnsRecordType: DnsRecordType = DnsRecordType.ANY,
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
                if (mainActivity.state.useAlternativePingMethod) {
                    inetPing(mainActivity) { host ->
                        mainActivity.runOnUiThread {
                            adapter.hosts.add(index = 0, host)
                            adapter.notifyDataSetChanged()
                        }
                    }
                } else {
                    terminalPing(mainActivity) { host ->
                        mainActivity.runOnUiThread {
                            adapter.hosts.add(index = 0, host)
                            adapter.notifyDataSetChanged()
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

            private fun inetPing(mainActivity: MainActivity, onNewPing: (host: Host) -> Unit) {
                thread {
                    try {
                        val hostInet = InetAddress.getByName(mainActivity.state.host)
                        while (mainActivity.state.isOperationActive) {
                            val start = System.currentTimeMillis()
                            val status = hostInet.isReachable(mainActivity.state.pingTimeout).toHostStatus()
                            val latency = System.currentTimeMillis() - start
                            onNewPing(
                                Host(
                                    host = mainActivity.state.host,
                                    time = if(status == Host.Status.Unavailable) null else formatLatencyMillis(latency),
                                    status = status
                                )
                            )

                            (1..mainActivity.state.pingDelay / 100).forEach { _ ->
                                if (!mainActivity.state.isOperationActive) return@thread
                                Thread.sleep(100)
                            }
                        }
                    } catch (e: UnknownHostException) {
                        mainActivity.runOnUiThread {
                            AlertDialog.Builder(mainActivity)
                                .setTitle("Failed to resolve the host name")
                                .setPositiveButton("Close") { _, _ -> }
                                .show()
                            mainActivity.updateQueryButton()
                        }
                    } catch (e: Exception) {
                        AlertDialog.Builder(mainActivity)
                            .setTitle("Failed to perform a ping")
                            .setMessage(e.message)
                            .setPositiveButton("Close") { _, _ -> }
                            .show()
                        e.printStackTrace()
                    } finally {
                        mainActivity.state = mainActivity.state.copy(isOperationActive = false)
                    }
                }
            }

            private fun terminalPing(mainActivity: MainActivity, onNewPing: (host: Host) -> Unit) {
                val pingProcess = Runtime.getRuntime().exec(
                    arrayOf(
                        "/system/bin/ping",
                        "-t","${mainActivity.state.pingTimeout}",
                        "-W", "${mainActivity.state.pingDelay}",
                        mainActivity.state.host
                    )
                )
                thread {
                    pingProcess.inputStream.bufferedReader().use { reader ->
                        var line: String? = reader.readLine()
                        var isFirstLine = true
                        while (mainActivity.state.isOperationActive) {
                            if(line == null) {
                                Thread.sleep(mainActivity.state.pingDelay / 2L)
                                line = reader.readLine()
                                continue
                            }
                            // The first line is always some kind of header if you will
                            if (isFirstLine) {
                                isFirstLine = false
                                continue
                            }

                            val time =  Regex("time=([0-9.]+\\sms)").find(line)?.groupValues?.get(1)
                            onNewPing(
                                Host(
                                    host = mainActivity.state.host,
                                    time = time,
                                    status = if (time == null) Host.Status.Unavailable else Host.Status.Available
                                )
                            )

                            line = reader.readLine()
                        }
                    }
                }
            }
        },

        Dns(title = "Dns") {
            private val adapter by lazy { DnsListAdapter(mutableListOf()) }

            override fun invoke(mainActivity: MainActivity, shouldResetState: Boolean) {
                adapter.records = mutableListOf()
                adapter.notifyDataSetChanged()
                thread {
                    try {
                        val resultText = URL("https://dns.google/resolve?name=${URLEncoder.encode(mainActivity.state.host, "UTF-8")}&type=${mainActivity.state.dnsRecordType.typeId}").readText()
                        val records = JSONObject(resultText).getJSONArray("Answer")
                        for(i in 0 ..< records.length()) {
                            records.getJSONObject(i).apply {
                                adapter.records.add(
                                    DnsRecord(
                                        name = getString("name"),
                                        data = getString("data"),
                                        typeId = getInt("type"),
                                        ttl = getInt("TTL"),
                                    )
                                )
                            }
                        }
                        mainActivity.runOnUiThread {
                            adapter.notifyDataSetChanged()
                        }
                    } catch (e: Exception) {
                        mainActivity.runOnUiThread {
                            AlertDialog.Builder(mainActivity)
                                .setTitle("Failed to query dns.google")
                                .setMessage("${e::class.qualifiedName}: ${e.message.orEmpty()}")
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
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager =
                        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                    vibratorManager.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(VIBRATOR_SERVICE) as Vibrator
                }

                return ListView(context).apply {
                    divider = null
                    adapter = this@Dns.adapter
                    onItemLongClickListener =
                        AdapterView.OnItemLongClickListener { _, _, position, _ ->
                            val dnsRecord = getItemAtPosition(position) as DnsRecord
                            clipboard.setPrimaryClip(ClipData.newPlainText(dnsRecord.data, dnsRecord.data))
                            Toast.makeText(context, "Data copied to clipboard", Toast.LENGTH_SHORT).show()
                            if (Build.VERSION.SDK_INT >= 26) {
                                vibrator.vibrate(VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(60)
                            }
                            true
                        }
                }
            }
        },

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
                                .setMessage("Please check you whois server configuration.\n${e::class.qualifiedName}: ${e.message.orEmpty()}")
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