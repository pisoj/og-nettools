package xyz.pisoj.holo1.ping

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import kotlinx.parcelize.Parcelize
import xyz.pisoj.holo1.R

class PingFragment: Fragment() {

    private lateinit var state: PingFragmentState

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        state = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            savedInstanceState?.getParcelable("state", PingFragmentState::class.java) ?: PingFragmentState()
        } else {
            @Suppress("DEPRECATION")
            savedInstanceState?.getParcelable("state") ?: PingFragmentState()
        }

        return inflater.inflate(R.layout.fragment_ping, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val button: Button = view.findViewById(R.id.button)
        val tv: TextView = view.findViewById(R.id.textView)
        tv.text = state.count.toString()
        button.setOnClickListener {
            state.count++
            tv.text = state.count.toString()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("state", state)
    }

    @Parcelize
    private data class PingFragmentState(
        var count: Int = 0
    ) : Parcelable
}