package com.cinetrack.hub

import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DetailScreen(item: MediaEntry, onBack: () -> Unit, onUpdate: (MediaEntry) -> Unit, onDelete: (MediaEntry) -> Unit) {
    val ctx = LocalContext.current
    val finishMins = (item.runtimeMinutes / item.speed).roundToInt().coerceAtLeast(1)
    val savedMins = (item.runtimeMinutes - finishMins).coerceAtLeast(0)
    val isWatched = item.status == "Watched"

    Column(Modifier.fillMaxSize().background(OledBlack).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(235.dp)) {
            if (item.backdropUrl.isNotBlank() || item.posterUrl.isNotBlank()) {
                AsyncImage(model = item.backdropUrl.ifBlank { item.posterUrl }, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF214212), OledBlack))))
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x66000000), Color(0xBB000000), OledBlack))))
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(8.dp).clip(CircleShape).background(Color(0x99000000))) {
                Icon(Icons.Default.ArrowBack, null, tint = NeonLime)
            }
            Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom) {
                PosterBox(url = item.posterUrl, title = item.title, modifier = Modifier.width(88.dp).height(130.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, NeonLime.copy(0.5f), RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(item.type.uppercase(), color = NeonLime, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    Text(item.title, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("You: ${String.format(Locale.US, "%.1f", item.userRating)}/10", color = NeonLime, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("IMDb: ${if (item.imdbRating > 0.0) String.format(Locale.US, "%.1f", item.imdbRating) else "—"}", color = ImdbGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { launchMpvRx(ctx, item.title) }, colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = OledBlack), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Play (${item.speed}x)", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Button(
                    onClick = {
                        val next = if (isWatched) "Wishlist" else "Watched"
                        onUpdate(item.copy(status = next))
                        Toast.makeText(ctx, if (next == "Watched") "Marked Watched ✓" else "Moved to Watchlist ⏳", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isWatched) CardDark else Color(0xFF1D3326), contentColor = if (isWatched) ImdbGold else NeonLime),
                    border = BorderStroke(1.dp, if (isWatched) ImdbGold.copy(0.5f) else NeonLime.copy(0.6f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Text(if (isWatched) "⏳ To Watchlist" else "✓ Mark Watched", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }

            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardDark), border = BorderStroke(1.dp, NeonLime.copy(0.35f))) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("⏱️ FINISH TIME CALCULATOR", color = NeonLime, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.clip(CircleShape).background(GlassDark).clickable { onUpdate(item.copy(runtimeMinutes = (item.runtimeMinutes - 15).coerceAtLeast(15))) }.padding(8.dp, 3.dp)) { Text("-15m", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                            Box(Modifier.clip(CircleShape).background(GlassDark).clickable { onUpdate(item.copy(runtimeMinutes = item.runtimeMinutes + 15)) }.padding(8.dp, 3.dp)) { Text("+15m", color = TextWhite, fontSize = 10.sp, fontFamily = FontFamily.Monospace) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text("ORIGINAL", color = TextMuted, fontSize = 9.sp); Text(formatHoursMins(item.runtimeMinutes), color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("FINISHES IN", color = NeonLime, fontSize = 9.sp, fontWeight = FontWeight.Bold); Text(formatHoursMins(finishMins), color = NeonLime, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace) }
                        Column(horizontalAlignment = Alignment.End) { Text("YOU SAVE", color = MintTeal, fontSize = 9.sp); Text("${savedMins}m", color = MintTeal, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1.0, 1.35, 1.5, 1.75).forEach { sp ->
                            val sel = item.speed == sp
                            Box(Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(if (sel) NeonLime else GlassDark).clickable { onUpdate(item.copy(speed = sp)) }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                Text("${sp}x", color = if (sel) OledBlack else TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }

            if (item.overview.isNotBlank()) Text(item.overview, color = TextMuted, fontSize = 13.sp, lineHeight = 18.sp)

            OutlinedButton(onClick = { onDelete(item) }, border = BorderStroke(1.dp, Color(0xFFFF453A).copy(0.5f)), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF453A), modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Remove from Vault", color = Color(0xFFFF453A), fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun StatsScreen(entries: List<MediaEntry>) {
    val ctx = LocalContext.current
    val watched = entries.filter { it.status == "Watched" }
    val wishlist = entries.filter { it.status != "Watched" }
    val savedMins = watched.sumOf { (it.runtimeMinutes - (it.runtimeMinutes / it.speed).roundToInt()).coerceAtLeast(0) }
    val savedHrs = savedMins / 60
    val rankTitle = when {
        savedHrs >= 800 -> "🏆 CHRONOS GRANDMASTER"
        savedHrs >= 300 -> "⚡ SPEED-WATCH WARLORD"
        savedHrs >= 100 -> "🎬 BINGE TACTICIAN"
        else -> "🍿 CINEMA EXPLORER"
    }

    var roulettePool by remember { mutableStateOf(if (wishlist.isNotEmpty()) "Wishlist" else "9.5+ Gems") }
    var picked by remember { mutableStateOf<MediaEntry?>(null) }

    val avgYou = if (watched.isNotEmpty()) watched.map { it.userRating }.average() else 0.0
    val imdbList = watched.filter { it.imdbRating > 0.0 }
    val avgImdb = if (imdbList.isNotEmpty()) imdbList.map { it.imdbRating }.average() else 0.0
    val hotTakes = imdbList.sortedByDescending { it.userRating - it.imdbRating }.take(4)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = NeonLime)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("YOUR CINEMA RANK", color = OledBlack.copy(0.75f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
                Text(rankTitle, color = OledBlack, fontSize = 20.sp, fontWeight = FontWeight.Black)
                Text("${savedHrs}h+ saved across ${watched.size} titles • Avg You ${String.format(Locale.US, "%.2f", avgYou)} vs IMDb ${String.format(Locale.US, "%.2f", avgImdb)}", color = OledBlack.copy(0.85f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardDark), border = BorderStroke(1.dp, NeonLime.copy(0.4f))) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("🎲 KYA DEKHUN? ROULETTE", color = NeonLime, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Wishlist", "9.5+ Gems").forEach { mode ->
                            val sel = roulettePool == mode
                            Box(Modifier.clip(CircleShape).background(if (sel) NeonLime else GlassDark).clickable { roulettePool = mode }.padding(9.dp, 4.dp)) {
                                Text(mode, color = if (sel) OledBlack else TextWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Button(
                    onClick = {
                        val pool = if (roulettePool == "Wishlist") wishlist else entries.filter { it.userRating >= 9.5 }
                        picked = (pool.ifEmpty { entries }).randomOrNull()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassDark, contentColor = NeonLime),
                    border = BorderStroke(1.dp, NeonLime.copy(0.5f)),
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("🎲 Spin Random Title", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }

                picked?.let { m ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(GlassDark).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        PosterBox(url = m.posterUrl, title = m.title, modifier = Modifier.width(56.dp).height(82.dp).clip(RoundedCornerShape(8.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(m.title, color = TextWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("You ${m.userRating} • IMDb ${m.imdbRating} • ${formatHoursMins((m.runtimeMinutes / m.speed).roundToInt())}", color = NeonLime, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Box(Modifier.clip(CircleShape).background(NeonLime).clickable { launchMpvRx(ctx, m.title) }.padding(12.dp, 4.dp)) {
                                Text("Play in mpvRx ▶", color = OledBlack, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        if (hotTakes.isNotEmpty()) {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = CardDark), border = BorderStroke(1.dp, BorderGlass)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🔥 YOUR HOT TAKES (YOU VS IMDB)", color = ImdbGold, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    hotTakes.forEach { m ->
                        val diff = m.userRating - m.imdbRating
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(m.title, color = TextWhite, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            Text("${String.format(Locale.US, "%+.1f", diff)} (${m.userRating} vs ${m.imdbRating})", color = if (diff >= 0) NeonLime else Color(0xFFFF6B6B), fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTitleDialog(onDismiss: () -> Unit, onAdd: (MediaEntry) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiKey = remember { StorageHelper.getTmdbKey(ctx) }
    var query by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Wishlist") }
    var results by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardDark,
        title = { Text("Add to Vault", color = NeonLime, fontWeight = FontWeight.Black) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = query, onValueChange = { query = it }, placeholder = { Text("Movie, Series, Anime...", color = TextMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Wishlist", "Watched").forEach { st ->
                        FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonLime, selectedLabelColor = OledBlack))
                    }
                }
                if (apiKey.isNotBlank()) {
                    Button(onClick = { searching = true; scope.launch { results = TmdbHelper.searchTmdb(apiKey, query); searching = false } }, colors = ButtonDefaults.buttonColors(containerColor = GlassDark, contentColor = NeonLime), modifier = Modifier.fillMaxWidth()) {
                        Text(if (searching) "Searching TMDB..." else "Search TMDB Posters")
                    }
                }
                results.take(4).forEach { hit ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(GlassDark).clickable { onAdd(hit.copy(status = status)) }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PosterBox(url = hit.posterUrl, title = hit.title, modifier = Modifier.width(36.dp).height(52.dp).clip(RoundedCornerShape(6.dp)))
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(hit.title, color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text("${hit.type} • IMDb ${hit.imdbRating}", color = ImdbGold, fontSize = 11.sp)
                        }
                        Text("+ Add", color = NeonLime, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (query.isNotBlank()) onAdd(MediaEntry(title = query.trim(), status = status)) }, colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = OledBlack)) {
                Text("Add Manual", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } }
    )
}
