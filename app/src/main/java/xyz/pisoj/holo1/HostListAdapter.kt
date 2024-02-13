package xyz.pisoj.holo1

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import xyz.pisoj.holo1.model.Host
import xyz.pisoj.holo1.utils.formatLatencyMillis
import xyz.pisoj.holo1.utils.getColorForStatus

class HostListAdapter(private val hosts: List<Host>): BaseAdapter() {
    override fun getCount(): Int {
        return hosts.size
    }

    override fun getItem(position: Int): Any {
        return hosts[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater
                .from(parent.context)
                .inflate(R.layout.item_ping, parent, false)
                .apply { tag = ViewHolder(this) }

        val viewHolder = view.tag as ViewHolder
        viewHolder.setHost(hosts[position])

        return view
    }

    private class ViewHolder(private val root: View) {

        private val hostTextView: TextView = root.findViewById(R.id.host)
        private val latencyTextView: TextView = root.findViewById(R.id.latency)
        private val statusView: ImageView = root.findViewById(R.id.status)

        init {
            println("new ViewHolder")
        }

        fun setHostText(value: String) {
            hostTextView.text = value
        }

        fun setLatencyText(latency: String?) {
            if (latency == null) {
                latencyTextView.visibility = View.GONE
                return
            }
            latencyTextView.visibility = View.VISIBLE
            latencyTextView.text = latency
        }

        fun setStatus(status: Host.Status) {
            statusView.setColorFilter(
                root.context.getColorForStatus(status),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
        }

        fun setHost(host: Host) {
            setHostText(host.host)
            setLatencyText(host.latencyMillis?.let { formatLatencyMillis(it) })
            setStatus(host.status)
        }
    }
}