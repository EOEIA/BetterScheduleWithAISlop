package cz.vitskalicky.lepsirozvrh.schoolPicker

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Fts4
import kotlinx.parcelize.Parcelize
import java.text.Normalizer

@Fts4
@Entity(tableName = "schools")
@Parcelize
data class SchoolInfo(
        val id: String,
        val name: String,
        val url: String,
        val isManual: Boolean = false,
        /**
         * only letters and digits from 'name', lowercase (accents are removed)
         */
        val search_text: String? = "$name $url".simplified()
) : Parcelable

/**
 * Removes all accents, converts punctuation to spaces, deletes all non-alphanumerical characters and removes duplicate spaces
 */
fun String.simplified():String {
    val normalized = Normalizer.normalize(this.lowercase(), Normalizer.Form.NFD).toCharArray() //converts letters with accents to letter without an accent and combining character, which gets removed 3 lines later.
    val sb = StringBuilder()
    var last: Char = ' ';
    var i = 0;
    while (i < normalized.size){
        var item:Char = normalized[i]
        var category = item.category
        if (item.isLetterOrDigit()){
            sb.append(item)
            last = item
        } else {
            if (category == CharCategory.OTHER_PUNCTUATION){
                item = ' '
                category = item.category
            }
            if (category == CharCategory.SPACE_SEPARATOR && last != ' '){
                sb.append(' ')
                last = ' '
            }
        }
        i++
    }
    return sb.toString()
}