package cz.vitskalicky.lepsirozvrh

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress

/** TODO refactor [Utils] to kotlin */
object KotlinUtils {
    // TCP/HTTP/DNS (depending on the port, 53=DNS, 80=HTTP, etc.)
    /**
     * opens and closes socket to Google's DNS nameserver to check internet connectivity
     * */
    suspend fun isOnline(): Boolean {
        return withContext(Dispatchers.IO){
            try {
                val timeoutMs = 1500
                val sock = Socket()
                val sockaddr: SocketAddress = InetSocketAddress("8.8.8.8", 53)
                sock.connect(sockaddr, timeoutMs)
                sock.close()
                true
            } catch (e: IOException) {
                false
            }
        }
    }
}