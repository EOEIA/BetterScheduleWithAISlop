package cz.vitskalicky.lepsirozvrh.model

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import org.joda.time.DateTime

@Entity
data class Account(
    val serverUrl: String,
    val username: String,
    val accessToken: String,
    val refreshToken: String,
    /** When access token expires */
    val accessExpires: DateTime,

    val schoolName: String,
    val schoolId: String,
    val fullName: String,
    /** Example: parents */
    val userType: String,
    /** Example: Rodič */
    val userTypeText: String,
    val semesterEnd: DateTime?,
    val userUID: String,
    @Embedded(prefix = "class_")
    val clazz: Class,
    /** > Insert methods treat 0 as not-set while inserting the item.*/
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
){
    fun isAccessExpired(): Boolean = accessExpires.isBefore(DateTime.now().plusMinutes(1))
}

data class Class(
    val id: String,
    val abbrev: String,
    /** Has been spotted to be sometimes empty */
    val name: String
)
