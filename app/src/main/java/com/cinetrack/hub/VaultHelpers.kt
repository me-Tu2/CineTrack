package com.cinetrack.hub

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.UUID

data class MediaEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String = "Movie",
    val status: String = "Watched",
    val userRating: Double = 9.0,
    val imdbRating: Double = 8.5,
    val runtimeMinutes: Int = 120,
    val speed: Double = 1.5,
    val tags: List<String> = emptyList()
)

fun formatHoursMins(totalMins: Int): String {
    val h = totalMins / 60
    val m = totalMins % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

object StorageHelper {
    private const val FILE_NAME = "cinetrack_vault.json"

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
                    tags = (0 until tArr.length()).map { tArr.getString(it) }
                )
            }
        } catch (e: Exception) { starterList() }
    }

    fun saveEntries(context: Context, list: List<MediaEntry>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id); put("title", item.title); put("type", item.type)
                put("status", item.status); put("userRating", item.userRating)
                put("imdbRating", item.imdbRating); put("runtimeMinutes", item.runtimeMinutes)
                put("speed", item.speed); put("tags", JSONArray(item.tags))
            })
        }
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use { it.write(arr.toString().toByteArray()) }
    }

    private fun starterList() = listOf(
        MediaEntry(title = "LOTR: Return of the King (Extended)", type = "Movie", status = "Watched", userRating = 10.0, imdbRating = 9.0, runtimeMinutes = 263, speed = 1.35, tags = listOf("1.0x Dialogue", "Rewatch")),
        MediaEntry(title = "Bleach: Thousand-Year Blood War", type = "Anime", status = "Watched", userRating = 9.6, imdbRating = 9.0, runtimeMinutes = 624, speed = 1.5, tags = listOf("Original Audio")),
        MediaEntry(title = "True Detective (Season 1)", type = "Series", status = "Watched", userRating = 9.8, imdbRating = 8.9, runtimeMinutes = 460, speed = 1.35, tags = listOf("1.0x Dialogue")),
        MediaEntry(title = "Lord of Mysteries", type = "Anime", status = "Wishlist", userRating = 9.5, imdbRating = 8.8, runtimeMinutes = 300, speed = 1.5, tags = listOf("Hindi Dub"))
    )
}

object CsvHelper {
    fun importFromUri(context: Context, uri: Uri): List<MediaEntry> {
        val out = mutableListOf<MediaEntry>()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream))
            val headers = parseLine(reader.readLine() ?: return emptyList()).map { it.trim().lowercase(Locale.US) }
            val tIdx = headers.indexOfFirst { it in listOf("title", "name", "show", "movie") }.coerceAtLeast(0)
            val uIdx = headers.indexOfFirst { it in listOf("userrating", "user_rating", "rating", "score") }
            val iIdx = headers.indexOfFirst { it in listOf("imdbrating", "imdb_rating", "imdb", "vote_average") }
            val typeIdx = headers.indexOfFirst { it in listOf("type", "media_type") }
            val stIdx = headers.indexOfFirst { it in listOf("status", "watched") }
            val minIdx = headers.indexOfFirst { it in listOf("runtimeminutes", "runtime", "minutes") }
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val c = parseLine(line!!)
                val title = c.getOrNull(tIdx)?.trim().orEmpty()
                if (title.isBlank()) continue
                var u = c.getOrNull(uIdx)?.toDoubleOrNull() ?: 9.0
                if (u in 0.5..5.0) u *= 2.0
                out.add(MediaEntry(
                    title = title,
                    type = c.getOrNull(typeIdx)?.trim().orEmpty().ifBlank { "Movie" },
                    status = if (c.getOrNull(stIdx)?.equals("Wishlist", true) == true) "Wishlist" else "Watched",
                    userRating = u.coerceIn(0.0, 10.0),
                    imdbRating = (c.getOrNull(iIdx)?.toDoubleOrNull() ?: 8.0).coerceIn(0.0, 10.0),
                    runtimeMinutes = (c.getOrNull(minIdx)?.toIntOrNull() ?: 120).coerceAtLeast(1)
                ))
            }
        }
        return out
    }

    fun exportToUri(context: Context, uri: Uri, list: List<MediaEntry>) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            val sb = StringBuilder("Title,Type,Status,UserRating,IMDbRating,RuntimeMinutes,Speed,Tags\n")
            list.forEach {
                sb.appendLine("\"${it.title.replace("\"", "\"\"")}\",${it.type},${it.status},${it.userRating},${it.imdbRating},${it.runtimeMinutes},${it.speed},\"${it.tags.joinToString("|")}\"")
            }
            out.write(sb.toString().toByteArray())
        }
    }

    private fun parseLine(line: String): List<String> {
        val res = mutableListOf<String>(); val cur = StringBuilder(); var q = false
        for (ch in line) {
            when { ch == '"' -> q = !q; ch == ',' && !q -> { res.add(cur.toString()); cur.setLength(0) }; else -> cur.append(ch) }
        }
        res.add(cur.toString()); return res
    }
}
