package cz.vitskalicky.lepsirozvrh

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Parcel
import androidx.annotation.PluralsRes
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parceler
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketAddress
import kotlin.math.roundToInt

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

    val FLAG_IMMUTABLE: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        PendingIntent.FLAG_IMMUTABLE
    } else {
        0
    }

    @Composable
    fun quantityStringResource(@PluralsRes id: Int, quantity: Int, vararg formatArgs: Any): String {
        return LocalContext.current.resources.getQuantityString(id, quantity, *formatArgs)
    }

    fun calculateWeekSwitchOffset(context: Context, optionIndex: Int): Int{
        return context.resources.getIntArray(R.array.switch_to_next_week_offsets)[optionIndex]
    }
    fun getWeekSwitchOffset(context: Context): Int {
        return calculateWeekSwitchOffset(context, context.prefs.int(PrefsConsts.SWITCH_TO_NEXT_WEEK_OPTION_INDEX) ?: 0)
    }

    /** Converts [Color] represented as its value (may be in various color spaced etc.) to ARGB int.*/
    @JvmStatic
    fun composeColorLongToARGB(value: Long): Int = Color(value = value.toULong()).toArgb()

    /** Converts DP into pixels*/
    @JvmStatic
    fun dpToPx(dpValue: Float, context: Context): Int = (dpValue * context.resources.displayMetrics.density).roundToInt()
    /** Converts SP into pixels*/
    @JvmStatic
    fun spToPx(spValue: Float, context: Context): Int = (spValue * context.resources.displayMetrics.scaledDensity).roundToInt()

    // composable shortcuts
    inline val Int.str: String
        @Composable get() = stringResource(this)
    inline val ImageVector.icon: @Composable () -> Unit get() = { Icon(this, null) }
}

/** Parceler for saving [Color] into bundles */
object ColorParceler : Parceler<Color> {
    override fun create(parcel: Parcel) = Color(parcel.readInt())

    override fun Color.write(parcel: Parcel, flags: Int) {
        parcel.writeInt(toArgb())
    }
}
