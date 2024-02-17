package xyz.pisoj.og.nettools

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import xyz.pisoj.og.nettools.model.DnsRecord


class DnsListAdapter(var records: MutableList<DnsRecord>): BaseAdapter() {
    override fun getCount(): Int {
        return records.size
    }

    override fun getItem(position: Int): Any {
        return records[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView
            ?: LayoutInflater.from(parent.context)
                .inflate(R.layout.item_dns, parent, false)
                .apply { tag = ViewHolder(this) }

        val viewHolder = view.tag as ViewHolder
        viewHolder.setDns(records[position])

        return view
    }

    private class ViewHolder(root: View) {
        private val name = root.findViewById<TextView>(R.id.name)
        private val data = root.findViewById<TextView>(R.id.data)
        private val ttl = root.findViewById<TextView>(R.id.ttl)
        private val recordType = root.findViewById<TextView>(R.id.recordType)

        fun setDns(record: DnsRecord) {
            name.text = record.name
            data.text = record.data
            @SuppressLint("SetTextI18n")
            ttl.text = "TTL: ${record.ttl}"
            recordType.text = record.type.name
        }
    }
}