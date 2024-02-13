package xyz.pisoj.holo1

import android.annotation.SuppressLint
import android.content.Context
import android.widget.ListView
import xyz.pisoj.holo1.model.Host

@SuppressLint("ViewConstructor")
class HostListView(context: Context, hostList: List<Host>): ListView(context) {
    init {
        divider = null
        adapter = HostListAdapter(hostList)
    }
}