package xyz.pisoj.holo1

import android.support.v4.app.Fragment
import xyz.pisoj.holo1.ping.PingFragment

enum class MainPages(val fragment: Fragment, val title: String) {
    Mama(PingFragment(), "Mama"),
    Mia(PingFragment(), "Mia"),
    Zmija(PingFragment(), "Zmija"),
}