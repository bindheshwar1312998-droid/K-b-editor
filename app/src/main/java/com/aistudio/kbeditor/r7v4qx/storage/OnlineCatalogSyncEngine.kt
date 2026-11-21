package com.aistudio.kbeditor.r7v4qx.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class OnlineCatalogPack(
    val version: Int = 1,
    val bannerMessage: String = "",
    val featuredFxNames: List<String> = emptyList(),
    val musicTracks: List<Pair<String, String>> = emptyList()
)

/**
 * Fetches dynamic Effects / Music / Template JSON from GitHub Raw or Cloudflare R2
 * without requiring a new APK build. Safe offline fallback via SharedPreferences cache.
 */
class OnlineCatalogSyncEngine(context: Context) {

    private val prefs = context.getSharedPreferences("kb_online_catalog", Context.MODE_PRIVATE)

    suspend fun fetchCatalog(
        remoteUrl: String = DEFAULT_GITHUB_CATALOG_URL
    ): OnlineCatalogPack = withContext(Dispatchers.IO) {
        val rawJson = runCatching {
            val conn = (URL(remoteUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 5000
                readTimeout = 5000
                requestMethod = "GET"
            }
            if (conn.responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }.also { body ->
                    prefs.edit().putString(KEY_CACHED_JSON, body).apply()
                }
            } else null
        }.getOrNull() ?: prefs.getString(KEY_CACHED_JSON, null)

        parseCatalog(rawJson)
    }

    private fun parseCatalog(raw: String?): OnlineCatalogPack {
        if (raw.isNullOrBlank()) return OnlineCatalogPack()
        return runCatching {
            val obj = JSONObject(raw)
            val fxArr = obj.optJSONArray("featuredFx")
            val fxList = buildList {
                if (fxArr != null) {
                    for (i in 0 until fxArr.length()) {
                        fxArr.optString(i).takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                }
            }
            val musicArr = obj.optJSONArray("musicTracks")
            val musicList = buildList {
                if (musicArr != null) {
                    for (i in 0 until musicArr.length()) {
                        val item = musicArr.optJSONObject(i) ?: continue
                        val title = item.optString("title")
                        val url = item.optString("url")
                        if (title.isNotBlank() && url.isNotBlank()) add(title to url)
                    }
                }
            }
            OnlineCatalogPack(
                version = obj.optInt("version", 1),
                bannerMessage = obj.optString("banner", ""),
                featuredFxNames = fxList,
                musicTracks = musicList
            )
        }.getOrDefault(OnlineCatalogPack())
    }

    companion object {
        const val DEFAULT_GITHUB_CATALOG_URL =
            "https://raw.githubusercontent.com/bindheshwar1312998-droid/K-b-editor/main/online_catalog.json"
        private const val KEY_CACHED_JSON = "cached_json"
    }
}
