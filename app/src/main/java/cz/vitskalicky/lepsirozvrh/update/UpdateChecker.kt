package cz.vitskalicky.lepsirozvrh.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import cz.vitskalicky.lepsirozvrh.BuildConfig
import cz.vitskalicky.lepsirozvrh.MainApplication
import cz.vitskalicky.lepsirozvrh.PrefsConsts
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.io.File

/**
 * Checks this fork's GitHub releases and installs the APK from one, so updates don't have to be
 * fetched from the releases page by hand.
 *
 * Sideloading, not Play: the APK is downloaded and handed to the system package installer, which
 * only accepts it because it is signed with the same key as the installed app. Release builds are
 * signed with the project keystore (see CLAUDE.md); a debug build has a different applicationId
 * entirely, so it deliberately never offers to update itself into the release app.
 */
object UpdateChecker {
    private const val TAG = "UpdateChecker"
    const val OWNER = "EOEIA"
    const val REPO = "BetterScheduleWithAISlop"
    private const val APK_NAME = "update.apk"

    private val webservice: UpdateWebservice by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(JacksonConverterFactory.create(MainApplication.objectMapper))
            .build()
            .create(UpdateWebservice::class.java)
    }

    /** A debug build is a separate app - updating it into the release package is not possible. */
    val isSupported: Boolean get() = !BuildConfig.APPLICATION_ID.endsWith(".debug")

    /**
     * Compares dotted version numbers ("2.0.28" vs "2.0.9") segment by segment, so it does not fall
     * into the string-comparison trap where "2.0.9" looks newer than "2.0.28". Anything after the
     * numbers (a `-debug_abc123` suffix, say) is ignored.
     */
    fun isNewer(candidate: String, current: String): Boolean {
        fun parse(v: String): List<Int> = v.trimStart('v', 'V')
            .split('.')
            .map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }
        val a = parse(candidate)
        val b = parse(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }

    /** Returns the latest release when it is newer than what is running, else `null`. */
    suspend fun checkForUpdate(context: Context): GithubRelease? {
        if (!isSupported) return null
        return try {
            val release = withContext(Dispatchers.IO) { webservice.getLatestRelease(OWNER, REPO) }
            context.prefs.putOne(PrefsConsts.LAST_UPDATE_CHECK, System.currentTimeMillis())
            if (release.draft || release.prerelease || release.apk == null) return null
            if (isNewer(release.tagName, BuildConfig.VERSION_NAME)) release else null
        } catch (e: Exception) {
            Log.w(TAG, "Could not check for updates", e)
            null
        }
    }

    /**
     * Downloads the release APK, reporting 0..1 progress. Returns the file, or `null` on failure.
     * Written into cacheDir so the system can clear it and a failed download leaves nothing behind.
     */
    suspend fun download(
        context: Context,
        release: GithubRelease,
        onProgress: (Float) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        val asset = release.apk ?: return@withContext null
        val target = File(context.cacheDir, APK_NAME)
        try {
            val response = OkHttpClient.Builder().build()
                .newCall(Request.Builder().url(asset.browserDownloadUrl).build())
                .execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Download failed: HTTP ${response.code}")
                return@withContext null
            }
            val total = response.body?.contentLength()?.takeIf { it > 0 } ?: asset.size
            response.body?.byteStream()?.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var written = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        written += read
                        if (total > 0) onProgress((written.toFloat() / total).coerceIn(0f, 1f))
                    }
                }
            } ?: return@withContext null
            target
        } catch (e: Exception) {
            Log.w(TAG, "Could not download the update", e)
            target.delete()
            null
        }
    }

    /** Whether the user has granted this app permission to install APKs (needed from API 26). */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    /** Opens the system screen where "install unknown apps" is granted for this app. */
    fun requestInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    /** Hands the downloaded APK to the system installer. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
