package cz.vitskalicky.lepsirozvrh.update

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import retrofit2.http.GET
import retrofit2.http.Path

/** The slice of GitHub's release JSON this app cares about. */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubRelease @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("tag_name") val tagName: String = "",
    @JsonProperty("name") val name: String = "",
    @JsonProperty("body") val body: String = "",
    @JsonProperty("draft") val draft: Boolean = false,
    @JsonProperty("prerelease") val prerelease: Boolean = false,
    @JsonProperty("assets") val assets: List<GithubAsset> = emptyList()
) {
    /** The installable APK, if the release has one. */
    val apk: GithubAsset?
        get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class GithubAsset @JsonCreator(mode = JsonCreator.Mode.PROPERTIES) constructor(
    @JsonProperty("name") val name: String = "",
    @JsonProperty("size") val size: Long = 0,
    @JsonProperty("browser_download_url") val browserDownloadUrl: String = ""
)

interface UpdateWebservice {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): GithubRelease
}
