package cz.vitskalicky.lepsirozvrh.notification

import android.content.Context
import cz.vitskalicky.lepsirozvrh.prefs
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class ChangeHistoryEntry(
    val timestamp: Long,
    val monday: String,
    val lines: List<String>
)

object ChangeHistory {
    private const val PREFS_KEY = "change-history-json"
    private const val MAX_ENTRIES = 60
    private val json = Json { ignoreUnknownKeys = true }

    fun getEntries(context: Context): List<ChangeHistoryEntry> {
        val raw = context.prefs.string(PREFS_KEY) ?: return emptyList()
        return try {
            json.decodeFromString<List<ChangeHistoryEntry>>(raw).sortedByDescending { it.timestamp }
        } catch (_: Exception) { emptyList() }
    }

    fun addEntry(context: Context, entry: ChangeHistoryEntry) {
        val existing = getEntries(context).toMutableList()
        existing.add(0, entry)
        val trimmed = existing.take(MAX_ENTRIES)
        context.prefs.putOne(PREFS_KEY, json.encodeToString(trimmed))
    }
}
