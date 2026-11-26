package com.cinetrack.hub

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SyncProgress(val running: Boolean = false, val done: Int = 0, val total: Int = 0, val found: Int = 0)
data class ImportResult(val items: List<MediaEntry>, val skipped: Int)
data class ArtHit(val poster: String, val backdrop: String, val overview: String)
data class ArtResult(val ok: Boolean, val hit: ArtHit?)

// Sirf poster/backdrop/overview bharta hai (jab khali ho). Ratings, status, speed, tags kabhi nahi chhuta.
fun applyArt(e: MediaEntry, h: ArtHit?): MediaEntry =
    if (h == null) e else e.copy(
        posterUrl = e.posterUrl.ifBlank { h.poster },
        backdropUrl = e.backdropUrl.ifBlank { h.backdrop },
        overview = e.overview.ifBlank { h.overview }
    )

object DupeGuard {
    private val junk = Regex("[^\\p{L}\\p{N}]+")
    private fun norm(s: String) = s.lowercase().replace(junk, "")
    private fun kind(t: String) = when (t.trim().lowercase()) {
        "anime" -> "anime"
        "series", "tv", "show", "tv series" -> "series"
        else -> "movie"
    }
    fun key(e: MediaEntry) = norm(e.title) + "|" + kind(e.type)

    fun merge(existing: List<MediaEntry>, incoming: List<MediaEntry>): ImportResult {
        val seen = HashSet<String>()
        existing.forEach { seen.add(key(it)) }
        val fresh = ArrayList<MediaEntry>()
        var skipped = 0
        for (e in incoming) { if (seen.add(key(e))) fresh.add(e) else skipped++ }
        return ImportResult(fresh, skipped)
    }
}

object TmdbSync {
    private fun get(url: String): String? {
        return try {
            val c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 8000
            c.readTimeout = 8000
            val body = if (c.responseCode == 200) c.inputStream.bufferedReader().use { it.readText() } else null
            c.disconnect()
            body
        } catch (e: Exception) { null }
    }

    private suspend fun findArt(apiKey: String, e: MediaEntry): ArtResult = withContext(Dispatchers.IO) {
        val q = e.title.replace(Regex("\\s*\\(.*?\\)"), "").trim().ifBlank { e.title.trim() }
        val url = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=" + URLEncoder.encode(q, "UTF-8")
        var body = get(url)
        if (body == null) { delay(1200); body = get(url) }
        if (body == null) return@withContext ArtResult(false, null)
        try {
            val arr = JSONObject(body).optJSONArray("results") ?: return@withContext ArtResult(true, null)
            val wantMovie = e.type.equals("Movie", true)
            var best: JSONObject? = null
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val mt = o.optString("media_type")
                if (mt != "movie" && mt != "tv") continue
                val pp = o.optString("poster_path")
                if (pp.isBlank() || pp == "null") continue
                if ((mt == "movie") == wantMovie) { best = o; break }
                if (best == null) best = o
            }
            val b = best ?: return@withContext ArtResult(true, null)
            fun img(k: String, size: String): String {
                val v = b.optString(k)
                return if (v.isBlank() || v == "null") "" else "https://image.tmdb.org/t/p/$size$v"
            }
            ArtResult(true, ArtHit(img("poster_path", "w500"), img("backdrop_path", "w780"), b.optString("overview", "")))
        } catch (ex: Exception) { ArtResult(true, null) }
    }

    // Saare missing posters, 4 parallel, koi 50 ki limit nahi. 12 lagatar fail (key/internet) par ruk jata hai.
    suspend fun syncMissing(
        apiKey: String,
        todo: List<MediaEntry>,
        onProgress: (Int, Int) -> Unit,
        onBatch: (Map<String, ArtHit>) -> Unit
    ): Int {
        var done = 0; var found = 0; var fails = 0
        val pending = HashMap<String, ArtHit>()
        for (chunk in todo.chunked(4)) {
            val res = coroutineScope { chunk.map { e -> async { e to findArt(apiKey, e) } }.awaitAll() }
            for ((e, r) in res) {
                done++
                if (r.ok) fails = 0 else fails++
                r.hit?.let { pending[e.id] = it; found++ }
            }
            if (pending.size >= 20) { onBatch(HashMap(pending)); pending.clear() }
            onProgress(done, found)
            if (fails >= 12) break
            delay(150)
        }
        if (pending.isNotEmpty()) onBatch(HashMap(pending))
        return found
    }
}
