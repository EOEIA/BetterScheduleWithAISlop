package cz.vitskalicky.lepsirozvrh.schoolsDatabase

import androidx.room.Entity
import androidx.room.Fts4
import java.text.Normalizer

@Fts4
@Entity(tableName = "schools")
data class SchoolInfo(
        val id: String,
        val name: String,
        val url: String,
        /**
         * only letters and digits from 'name', lowercase (accents are removed)
         */
        val search_text: String? = "$name $url".simplified()
)

/**
 * Removes all accents, converts punctuation to spaces, deletes all non-alphanumerical characters and removes duplicate spaces
 */
fun CharSequence.simplified():String =
        Normalizer.normalize(this, Normalizer.Form.NFD) //converts letters with accents to letter without an accent and combining character, which gets removed 3 lines later.
                .toLowerCase()
                .replace(Regex("\\p{Punct}"), " ")
                .replace(Regex("[^\\p{Alnum}\\s]"), "")
                .split(" ").filter{ it.isNotBlank()}.joinToString(" ")