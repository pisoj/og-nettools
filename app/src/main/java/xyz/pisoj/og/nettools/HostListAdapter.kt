package xyz.pisoj.og.nettools

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import xyz.pisoj.og.nettools.model.Host
import xyz.pisoj.og.nettools.utils.formatLatencyMillis
import xyz.pisoj.og.nettools.utils.getColorForStatus


class HostListAdapter(var hosts: MutableList<Host>): BaseAdapter() {

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
            setLatencyText(host.time)
            setStatus(host.status)
        }
    }
}