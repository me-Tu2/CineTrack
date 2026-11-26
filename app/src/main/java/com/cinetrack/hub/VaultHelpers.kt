package com.cinetrack.hub

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

data class MediaEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String = "Movie",
    val status: String = "Watched",
    val userRating: Double = 9.0,
    val imdbRating: Double = 8.5,
    val runtimeMinutes: Int = 120,
    val speed: Double = 1.5,
    val posterUrl: String = "",
    val backdropUrl: String = "",
    val overview: String = "",
    val totalEpisodes: Int = 1,
    val watchedEpisodes: Int = 1,
    val tags: List<String> = emptyList()
)

fun formatHoursMins(totalMins: Int): String {
    val h = totalMins / 60; val m = totalMins % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun launchMpvRx(context: Context, title: String) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage("is.xyz.mpv")
            ?: context.packageManager.getLaunchIntentForPackage("is.xyz.mpv.ytdl")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(context, "Opening $title in mpvRx...", Toast.LENGTH_SHORT).show()
        } else {
            val pick = Intent(Intent.ACTION_VIEW).apply { type = "video/*"; addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(Intent.createChooser(pick, "Play $title with"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "mpvRx not found on device", Toast.LENGTH_SHORT).show()
    }
}

object StorageHelper {
    private const val FILE_NAME = "cinetrack_v2.json"
    private const val PREFS = "cinetrack_prefs"

    fun getTmdbKey(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("tmdb_key", "") ?: ""
    fun saveTmdbKey(context: Context, key: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString("tmdb_key", key.trim()).apply()

    fun loadEntries(context: Context): List<MediaEntry> {
        val file = context.getFileStreamPath(FILE_NAME)
        if (!file.exists()) return starterList()
        return try {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val tArr = o.optJSONArray("tags") ?: JSONArray()
                MediaEntry(
                    id = o.optString("id", UUID.randomUUID().toString()),
                    title = o.getString("title"),
                    type = o.optString("type", "Movie"),
                    status = o.optString("status", "Watched"),
                    userRating = o.optDouble("userRating", 9.0),
                    imdbRating = o.optDouble("imdbRating", 8.0),
                    runtimeMinutes = o.optInt("runtimeMinutes", 120),
                    speed = o.optDouble("speed", 1.5),
                    posterUrl = o.optString("posterUrl", ""),
                    backdropUrl = o.optString("backdropUrl", ""),
                    overview = o.optString("overview", ""),
                    totalEpisodes = o.optInt("totalEpisodes", 1),
                    watchedEpisodes = o.optInt("watchedEpisodes", 1),
                    tags = (0 until tArr.length()).map { tArr.getString(it) }
                )
            }
        } catch (e: Exception) { starterList() }
    }

    fun saveEntries(context: Context, list: List<MediaEntry>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("title", item.title); put("type", item.type); put("status", item.status)
                put("userRating", item.userRating); put("imdbRating", item.imdbRating); put("runtimeMinutes", item.runtimeMinutes)
                put("speed", item.speed); put("posterUrl", item.posterUrl); put("backdropUrl", item.backdropUrl)
                put("overview", item.overview); put("totalEpisodes", item.totalEpisodes); put("watchedEpisodes", item.watchedEpisodes)
                put("tags", JSONArray(item.tags))
            })
        }
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { it.write(arr.toString().toByteArray()) }
    }

    private fun starterList() = listOf(
        MediaEntry(
            title = "Dune: Part Two", type = "Movie", status = "Watched", userRating = 9.6, imdbRating = 8.5, runtimeMinutes = 166, speed = 1.35,
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg",
            overview = "Paul Atreides unites with Chani and the Fremen while seeking revenge against the conspirators who destroyed his family.",
            tags = listOf("1.0x Dialogue", "Rewatch")
        ),
        MediaEntry(
            title = "Bleach: Thousand-Year Blood War", type = "Anime", status = "Watched", userRating = 9.7, imdbRating = 9.0, runtimeMinutes = 624, speed = 1.5,
            posterUrl = "https://image.tmdb.org/t/p/w500/2EewmxXe72ogD0EaWM8gqa0ccIw.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/o0NsbcIvsllg6CJX0FBFY8wWwrn.jpg",
            overview = "The peace is suddenly broken when warning sirens blare through the Soul Society as the Quincy Wandenreich attack.",
            totalEpisodes = 26, watchedEpisodes = 26, tags = listOf("Original Audio")
        ),
        MediaEntry(
            title = "LOTR: The Return of the King", type = "Movie", status = "Watched", userRating = 10.0, imdbRating = 9.0, runtimeMinutes = 263, speed = 1.35,
            posterUrl = "https://image.tmdb.org/t/p/w500/rCzpDGLbOoPwLjy3OAm5NUPOTrC.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/2u7zbn8EudG6kLlBzUYqP8RyFU4.jpg",
            overview = "Aragorn is revealed as the heir to the ancient kings as he, Gandalf and the other members of the broken fellowship struggle to save Gondor.",
            tags = listOf("Extended Cut", "Rewatch")
        ),
        MediaEntry(
            title = "True Detective (Season 1)", type = "Series", status = "Watched", userRating = 9.8, imdbRating = 8.9, runtimeMinutes = 460, speed = 1.35,
            posterUrl = "https://image.tmdb.org/t/p/w500/cuV2O5ZyDLHSOWzg3nLVljp1ubw.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/2Ahm0YjLNvYv2nPEaYgUGvk8Sj2.jpg",
            overview = "Detectives Rust Cohle and Martin Hart's lives collide and entwine during a seventeen-year hunt for a killer in Louisiana.",
            totalEpisodes = 8, watchedEpisodes = 8, tags = listOf("1.0x Dialogue")
        ),
        MediaEntry(
            title = "Jujutsu Kaisen", type = "Anime", status = "Wishlist", userRating = 9.4, imdbRating = 8.6, runtimeMinutes = 576, speed = 1.5,
            posterUrl = "https://image.tmdb.org/t/p/w500/hFWP5HkbVEe40hrXgtCeQxoccHE.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/gmECX1DvFgdUPjtio2zaL8BPZvu.jpg",
            overview = "Yuji Itadori swallows a cursed talisman—the finger of a demon—and becomes cursed himself.",
            totalEpisodes = 24, watchedEpisodes = 12, tags = listOf("Hindi Dub")
        )
    )
}

object TmdbHelper {
    suspend fun searchTmdb(apiKey: String, query: String): List<MediaEntry> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || query.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val json = JSONObject(URL("https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=$encoded").readText())
            val results = json.optJSONArray("results") ?: return@withContext emptyList()
            (0 until minOf(results.length(), 8)).mapNotNull { i ->
                val o = results.getJSONObject(i)
                val mt = o.optString("media_type")
                if (mt != "movie" && mt != "tv") return@mapNotNull null
                val title = o.optString("title").ifBlank { o.optString("name") }
                if (title.isBlank()) return@mapNotNull null
                val p = o.optString("poster_path").let { if (it.isNotBlank() && it != "null") "https://image.tmdb.org/t/p/w500$it" else "" }
                val b = o.optString("backdrop_path").let { if (it.isNotBlank() && it != "null") "https://image.tmdb.org/t/p/w780$it" else "" }
                val isAnime = mt == "tv" && o.optJSONArray("genre_ids")?.toString()?.contains("16") == true
                MediaEntry(
                    title = title, type = when { isAnime -> "Anime"; mt == "tv" -> "Series"; else -> "Movie" },
                    status = "Wishlist", userRating = 9.0, imdbRating = (o.optDouble("vote_average", 8.0) * 10).roundToInt() / 10.0,
                    runtimeMinutes = if (mt == "tv") 288 else 135, speed = 1.5, posterUrl = p, backdropUrl = b,
                    overview = o.optString("overview", ""), totalEpisodes = if (mt == "tv") 12 else 1, watchedEpisodes = 0
                )
            }
        } catch (e: Exception) { emptyList() }
    }
}

object CsvHelper {
    fun importFromUri(context: Context, uri: Uri): List<MediaEntry> {
        val out = mutableListOf<MediaEntry>()
        val seen = mutableSetOf<String>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val r = BufferedReader(InputStreamReader(stream))
            val h = parseLine(r.readLine() ?: return emptyList()).map { it.trim().lowercase(Locale.US) }
            val tIdx = h.indexOfFirst { it in listOf("title", "name", "show", "movie") }.coerceAtLeast(0)
            val uIdx = h.indexOfFirst { it in listOf("your rating", "userrating", "user_rating", "rating", "score") }
            val iIdx = h.indexOfFirst { it in listOf("imdb rating", "imdbrating", "imdb_rating", "imdb", "vote_average") }
            val typeIdx = h.indexOfFirst { it in listOf("title type", "type", "media_type") }
            val stIdx = h.indexOfFirst { it in listOf("status", "watched") }
            val minIdx = h.indexOfFirst { it in listOf("runtime (mins)", "runtimeminutes", "runtime", "minutes") }
            val posIdx = h.indexOf("position")
            val genIdx = h.indexOf("genres")
            val constIdx = h.indexOf("const")
            val yrIdx = h.indexOf("year")
            var line: String?
            while (r.readLine().also { line = it } != null) {
                val c = parseLine(line!!)
                val title = c.getOrNull(tIdx)?.trim().orEmpty()
                val key = c.getOrNull(constIdx)?.trim()?.takeIf { it.isNotBlank() }
                    ?: "${title.lowercase(Locale.US)}_${c.getOrNull(yrIdx)?.trim().orEmpty()}"
                if (title.isBlank() || !seen.add(key)) continue
                val rawUser = c.getOrNull(uIdx)?.trim().orEmpty()
                val rawSt = c.getOrNull(stIdx)?.trim().orEmpty()
                val isWish = rawSt.equals("Wishlist", true) || rawSt.equals("Watchlist", true) || (posIdx >= 0 && rawUser.isBlank())
                var u = rawUser.toDoubleOrNull() ?: if (isWish) 0.0 else 9.0
                if (u in 0.5..5.0) u *= 2.0
                val rawT = c.getOrNull(typeIdx)?.trim().orEmpty().lowercase(Locale.US)
                val gen = c.getOrNull(genIdx)?.trim().orEmpty().lowercase(Locale.US)
                val mType = when {
                    rawT.contains("anime") || (gen.contains("animation") && (rawT.contains("series") || rawT.contains("tv"))) -> "Anime"
                    rawT.contains("series") || rawT.contains("tv") || rawT.contains("episode") -> "Series"
                    else -> "Movie"
                }
                out.add(MediaEntry(title = title, type = mType, status = if (isWish) "Wishlist" else "Watched", userRating = u.coerceIn(0.0, 10.0), imdbRating = (c.getOrNull(iIdx)?.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 10.0), runtimeMinutes = (c.getOrNull(minIdx)?.toIntOrNull() ?: 120).coerceAtLeast(1)))
            }
        }
        return out
    }
    fun exportToUri(context: Context, uri: Uri, list: List<MediaEntry>) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val sb = StringBuilder("Title,Type,Status,UserRating,IMDbRating,RuntimeMinutes,Speed,Tags\n")
            list.forEach { sb.appendLine("\"${it.title.replace("\"", "\"\"")}\",${it.type},${it.status},${it.userRating},${it.imdbRating},${it.runtimeMinutes},${it.speed},\"${it.tags.joinToString("|")}\"") }
            out.write(sb.toString().toByteArray())
        }
    }
    private fun parseLine(line: String): List<String> {
        val res = mutableListOf<String>(); val cur = StringBuilder(); var q = false
        for (ch in line) { when { ch == '"' -> q = !q; ch == ',' && !q -> { res.add(cur.toString()); cur.setLength(0) }; else -> cur.append(ch) } }
        res.add(cur.toString()); return res
    }
}
