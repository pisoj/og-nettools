package xyz.pisoj.og.nettools.utils

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.UnknownHostException


@Throws(UnknownHostException::class, IOException::class)
fun whois(domain: String, whoisServer: String, whoisPort: Int): String {
    val query = domain + "\r\n"
    val socket = Socket(whoisServer, whoisPort)
    val writer = OutputStreamWriter(socket.getOutputStream())
    writer.write(query)
    writer.flush()
    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
    var line: String?
    var complete = ""
    while (reader.readLine().also { line = it } != null) {
        complete += line + "\n"
    }
    reader.close()
    writer.close()
    socket.close()
    return complete
}