@file:UseSerializers(ThemeExchangeData.ColorAsStringSerializer::class)
@file:OptIn(ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)

package cz.vitskalicky.lepsirozvrh.theme

import android.os.Parcelable
import android.util.Base64
import android.util.Base64InputStream
import android.util.Base64OutputStream
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.fasterxml.jackson.databind.ObjectMapper
import cz.vitskalicky.lepsirozvrh.ColorParceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import kotlinx.serialization.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


/** For parsing/writing theme to json for export/import. Keeps (almost) same format as old version making it backwards-compatible. Don't forget to set decoder to ignore unknown keys.*/
@Parcelize
@TypeParceler<Color, ColorParceler>()
@Serializable()
data class ThemeExchangeData(
    // fields from pre-1.9 versions
    val cyaneaTheme: CyaneaTheme,
    val cEmptyBg: Color,
    val cABg: Color,
    val cHBg: Color,
    val cChngBg: Color,
    val cHeaderBg: Color,
    val cDivider: Color,
    val dpDividerWidth: Float,
    val cHighlight: Color,
    val dpHighlightWidth: Float,
    val cHPrimaryText: Color,
    val cHRoomText: Color,
    val cHSecondaryText: Color,
    val cChngPrimaryText: Color,
    val cChngRoomText: Color,
    val cChngSecondaryText: Color,
    val cAPrimaryText: Color,
    val cARoomText: Color,
    val cASecondaryText: Color,
    val cHeaderPrimaryText: Color,
    val cHeaderSecondaryText: Color,
    val spPrimaryText: Float,
    val spSecondaryText: Float,
    val dpPaddingLeft: Float,
    val dpPaddingTop: Float,
    val dpPaddingRight: Float,
    val dpPaddingBottom: Float,
    val dpTextPadding: Float,
    val cInfolineBg: Color,
    val cInfolineText: Color,
    val spInfolineTextSize: Float,
    val cError: Color,
    val cHomework: Color,
    val dpHomework: Float,

    // added in 1.9
    val isLight: Boolean = false,
    val customizationLevel: Int = 3,

): Parcelable  {

    @Parcelize
    @TypeParceler<Color, ColorParceler>()
    @Serializable
    data class CyaneaTheme(
        val primary: Color,
        val accent: Color,
        val background: Color
    ): Parcelable

    object ColorAsStringSerializer : KSerializer<Color> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: Color) {
            val string = value.toArgb().toUInt().toString(16).padStart(8, '0')
            encoder.encodeString(string)
        }

        override fun deserialize(decoder: Decoder): Color {
            val string = decoder.decodeString()
            return if (string.length == 6){
                // short string is without alpha, so we have to add it
                Color((0xff000000u or string.toUInt(16)).toInt())
            }else{
                Color(string.toUInt(16).toLong())
            }
        }
    }

    fun toRozvrhTheme(): RozvrhTheme{
        return RozvrhTheme(
            isLight = isLight,
            cPrimary = cyaneaTheme.primary,
            cSecondary = cyaneaTheme.accent,
            cSurface = cyaneaTheme.background,
            cEmptyBg = cEmptyBg,
            cABg = cABg,
            cHBg = cHBg,
            cChngBg = cChngBg,
            cHeaderBg = cHeaderBg,
            cDivider = cDivider,
            dpDividerWidth = dpDividerWidth,
            cHighlight = cHighlight,
            dpHighlightWidth = dpHighlightWidth,
            cHPrimaryText = cHPrimaryText,
            cHRoomText = cHRoomText,
            cHSecondaryText = cHSecondaryText,
            cChngPrimaryText = cChngPrimaryText,
            cChngRoomText = cChngRoomText,
            cChngSecondaryText = cChngSecondaryText,
            cAPrimaryText = cAPrimaryText,
            cARoomText = cARoomText,
            cASecondaryText = cASecondaryText,
            cHeaderPrimaryText = cHeaderPrimaryText,
            cHeaderSecondaryText = cHeaderSecondaryText,
            spPrimaryText = spPrimaryText,
            spSecondaryText = spSecondaryText,
            dpPaddingLeft = dpPaddingLeft,
            dpPaddingTop = dpPaddingTop,
            dpPaddingRight = dpPaddingRight,
            dpPaddingBottom = dpPaddingBottom,
            dpTextPadding = dpTextPadding,
            cInfolineBg = cInfolineBg,
            cInfolineText = cInfolineText,
            spInfolineTextSize = spInfolineTextSize,
            cError = cError,
            cHomework = cHomework,
            dpHomework = dpHomework,
            customizationLevel = customizationLevel,
        )
    }

    companion object{
        fun fromRozvrhTheme(rozvrhTheme: RozvrhTheme): ThemeExchangeData{
            return ThemeExchangeData(
                isLight = rozvrhTheme.isLight,
                cyaneaTheme = CyaneaTheme(
                    primary = rozvrhTheme.cPrimary,
                    accent = rozvrhTheme.cSecondary,
                    background = rozvrhTheme.cSurface,
                ),
                cEmptyBg = rozvrhTheme.cEmptyBg,
                cABg = rozvrhTheme.cABg,
                cHBg = rozvrhTheme.cHBg,
                cChngBg = rozvrhTheme.cChngBg,
                cHeaderBg = rozvrhTheme.cHeaderBg,
                cDivider = rozvrhTheme.cDivider,
                dpDividerWidth = rozvrhTheme.dpDividerWidth,
                cHighlight = rozvrhTheme.cHighlight,
                dpHighlightWidth = rozvrhTheme.dpHighlightWidth,
                cHPrimaryText = rozvrhTheme.cHPrimaryText,
                cHRoomText = rozvrhTheme.cHRoomText,
                cHSecondaryText = rozvrhTheme.cHSecondaryText,
                cChngPrimaryText = rozvrhTheme.cChngPrimaryText,
                cChngRoomText = rozvrhTheme.cChngRoomText,
                cChngSecondaryText = rozvrhTheme.cChngSecondaryText,
                cAPrimaryText = rozvrhTheme.cAPrimaryText,
                cARoomText = rozvrhTheme.cARoomText,
                cASecondaryText = rozvrhTheme.cASecondaryText,
                cHeaderPrimaryText = rozvrhTheme.cHeaderPrimaryText,
                cHeaderSecondaryText = rozvrhTheme.cHeaderSecondaryText,
                spPrimaryText = rozvrhTheme.spPrimaryText,
                spSecondaryText = rozvrhTheme.spSecondaryText,
                dpPaddingLeft = rozvrhTheme.dpPaddingLeft,
                dpPaddingTop = rozvrhTheme.dpPaddingTop,
                dpPaddingRight = rozvrhTheme.dpPaddingRight,
                dpPaddingBottom = rozvrhTheme.dpPaddingBottom,
                dpTextPadding = rozvrhTheme.dpTextPadding,
                cInfolineBg = rozvrhTheme.cInfolineBg,
                cInfolineText = rozvrhTheme.cInfolineText,
                spInfolineTextSize = rozvrhTheme.spInfolineTextSize,
                cError = rozvrhTheme.cError,
                cHomework = rozvrhTheme.cHomework,
                dpHomework = rozvrhTheme.dpHomework,
                customizationLevel = rozvrhTheme.customizationLevel,
            )
        }


        /**
         * Deserializes a [ThemeData] from JSON
         * @param is Input stream to read from.
         * @throws IOException if anything fails
         * @return Deserialized ThemeData
         */
        @Throws(IOException::class)
        fun parseJson(ins: InputStream): ThemeExchangeData {
            return try {
                val format = Json { ignoreUnknownKeys = true }
                format.decodeFromStream(ins)
            } catch (e: IOException) {
                throw IOException("Failed to parse theme", e)
            }
        }

        /**
         * Deserializes a [ThemeData] from JSON
         * @param s Json string
         * @throws IOException if parsing fails
         * @return Deserialized ThemeData
         */
        @Throws(IOException::class)
        fun parseJson(s: String): ThemeExchangeData {
            return try {
                val format = Json { ignoreUnknownKeys = true }
                format.decodeFromString(s)
            } catch (e: IOException) {
                throw IOException("Failed to parse theme from this string:$s", e)
            }
        }

        /**
         * Deserializes a [ThemeData] from base64 encoded (no wrap), gzipped, JSON.
         * @param is Input stream to read from.
         * @throws IOException if anything fails
         * @return Deserialized ThemeData
         */
        @Throws(IOException::class)
        fun parseZipped(`is`: InputStream): ThemeExchangeData {
            val bis = Base64InputStream(`is`, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
            val gzis = GZIPInputStream(bis)
            return parseJson(gzis)
        }

        /**
         * Deserializes a [ThemeData] from base64 encoded (no wrap), gzipped, JSON.
         * @param s base64 encoded (no wrap), gzipped, JSON
         * @throws IOException if anything fails
         * @return Deserialized ThemeData
         */
        @Throws(IOException::class)
        fun parseZipped(s: String): ThemeExchangeData {
            val bais = ByteArrayInputStream(s.toByteArray())
            return parseZipped(bais)
        }
    }


    /**
     * Serializes this object using JSON and writes it to the given output stream.
     * @throws IOException if it fails
     */
    @OptIn(ExperimentalSerializationApi::class)
    @Throws(IOException::class)
    fun toJsonString(os: OutputStream) {
        try {
            Json.encodeToStream(this, os);
        } catch (e: IOException) {
            throw IOException("Failed to write ThemeData.", e)
        }
    }

    /**
     * Serializes this object using JSON.
     * @return JSON string or `null` if it fails (it shouldn't)
     */
    fun toJsonString(): String? {
        val mapper = ObjectMapper()
        return try {
            Json.encodeToString(this)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Serializes this object using JSON, compresses using gzip, encodes in base64 and writes it to the given output stream.
     * @throws IOException if it fails
     */
    @Throws(IOException::class)
    fun toZippedString(os: OutputStream) {
        val bos = Base64OutputStream(os, Base64.NO_WRAP or Base64.NO_PADDING or Base64.URL_SAFE)
        val gzos = GZIPOutputStream(bos)
        toJsonString(gzos)
        gzos.finish()
    }

    /**
     * Serializes this object using JSON, compresses using gzip and encodes in base64
     * @return JSON string or `null` if it fails (it shouldn't)
     */
    fun toZippedString(): String? {
        return try {
            val baos = ByteArrayOutputStream()
            toZippedString(baos)
            baos.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}
