package com.cinetrack.hub

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import java.util.Locale
import kotlin.math.roundToInt

val OledBlack = Color(0xFF000000)
val CardSurface = Color(0xFF0D0D0F)
val DividerLine = Color(0xFF242428)
val NeonGreen = Color(0xFFB6FF3C)
val SecondaryGreen = Color(0xFF72C94A)
val ImdbGold = Color(0xFFF5C518)
val TextWhite = Color(0xFFF5F5F5)
val TextMuted = Color(0xFF8E8E93)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = OledBlack, surface = CardSurface, primary = NeonGreen, onBackground = TextWhite, onSurface = TextWhite)) {
                Surface(modifier = Modifier.fillMaxSize(), color = OledBlack) { CineTrackApp() }
            }
        }
    }
}

@Composable
fun CineTrackApp() {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf(StorageHelper.loadEntries(ctx)) }
    var tab by remember { mutableIntStateOf(0) }
    var selectedItem by remember { mutableStateOf<MediaEntry?>(null) }
    var showAdd by remember { mutableStateOf(false) }

    fun saveAll(list: List<MediaEntry>) { entries = list; StorageHelper.saveEntries(ctx, list) }

    if (selectedItem != null) {
        DetailScreen(
            item = selectedItem!!,
            onBack = { selectedItem = null },
            onUpdate = { updated ->
                selectedItem = updated
                saveAll(entries.map { if (it.id == updated.id) updated else it })
            },
            onDelete = { del ->
                saveAll(entries.filterNot { it.id == del.id })
                selectedItem = null
            }
        )
        return
    }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp, 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CINETRACK", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
                    Text("${entries.size} TITLES • MOVIE PARADISE x OLED", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add", fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = DividerLine, thickness = 1.dp)
                NavigationBar(containerColor = OledBlack) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Library") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = OledBlack, indicatorColor = NeonGreen, selectedTextColor = NeonGreen))
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Star, null) }, label = { Text("Taste DNA") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = OledBlack, indicatorColor = NeonGreen, selectedTextColor = NeonGreen))
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Share, null) }, label = { Text("CSV Vault") }, colors = NavigationBarItemDefaults.colors(selectedIconColor = OledBlack, indicatorColor = NeonGreen, selectedTextColor = NeonGreen))
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> LibraryScreen(
                    entries = entries,
                    onOpenDetail = { selectedItem = it },
                    onToggle = { item -> saveAll(entries.map { if (it.id == item.id) it.copy(status = if (it.status == "Watched") "Wishlist" else "Watched") else it }) }
                )
                1 -> StatsScreen(entries)
                2 -> CsvVaultScreen(entries) { imported -> saveAll(imported + entries); Toast.makeText(ctx, "Imported ${imported.size}!", Toast.LENGTH_SHORT).show() }
            }
        }
    }
    if (showAdd) AddTitleDialog({ showAdd = false }) { saveAll(listOf(it) + entries); showAdd = false }
}

@Composable
fun LibraryScreen(entries: List<MediaEntry>, onOpenDetail: (MediaEntry) -> Unit, onToggle: (MediaEntry) -> Unit) {
    val ctx = LocalContext.current
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val watched = entries.filter { it.status == "Watched" }
    val origMins = watched.sumOf { it.runtimeMinutes }
    val actMins = watched.sumOf { (it.runtimeMinutes / it.speed).roundToInt() }
    val saved = (origMins - actMins).coerceAtLeast(0)
    val heroBackdrop = entries.firstOrNull { it.backdropUrl.isNotBlank() }?.backdropUrl ?: ""
    val list = entries.filter { (it.title.contains(q, true) || it.tags.any { t -> t.contains(q, true) }) && (filter == "All" || it.status == filter || it.type.equals(filter, true)) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            // Full-Bleed Cinematic Backdrop Hero Card
            Card(shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, NeonGreen.copy(0.45f)), colors = CardDefaults.cardColors(containerColor = CardSurface)) {
                Box(Modifier.fillMaxWidth().height(165.dp)) {
                    if (heroBackdrop.isNotBlank()) {
                        AsyncImage(model = heroBackdrop, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    }
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x55000000), Color(0xEE000000)))))
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("⚡ YOUR CINEMA JOURNEY", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(NeonGreen).padding(8.dp, 2.dp)) {
                                Text("SPEED HUB", color = OledBlack, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column {
                                Text(formatHoursMins(saved), color = NeonGreen, fontSize = 32.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                                Text("Time saved at 1.35x–1.75x", color = TextWhite, fontSize = 12.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Watched: ${formatHoursMins(origMins)}", color = TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Text("Actual: ${formatHoursMins(actMins)}", color = SecondaryGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Horizontal Poster Carousel (Movie Paradise Style)
        if (entries.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("POSTER WALL", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(entries, key = { "car_${it.id}" }) { item ->
                            Box(
                                Modifier.width(104.dp).height(154.dp).clip(RoundedCornerShape(12.dp))
                                    .background(CardSurface).border(1.dp, DividerLine, RoundedCornerShape(12.dp))
                                    .clickable { onOpenDetail(item) }
                            ) {
                                PosterBox(url = item.posterUrl, title = item.title, modifier = Modifier.fillMaxSize())
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xDD000000)))))
                                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xCC000000)).padding(5.dp, 2.dp)) {
                                    Text("★ ${String.format(Locale.US, "%.1f", item.userRating)}", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Text(item.title, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomStart).padding(7.dp))
                            }
                        }
                    }
                }
            }
        }

        item { OutlinedTextField(value = q, onValueChange = { q = it }, placeholder = { Text("Search movies, anime, tags...", color = TextMuted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = NeonGreen) }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Watched", "Wishlist", "Anime", "Movie", "Series")) { f ->
                    val sel = filter == f
                    FilterChip(selected = sel, onClick = { filter = f }, label = { Text(f, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonGreen, selectedLabelColor = OledBlack, containerColor = CardSurface, labelColor = TextMuted))
                }
            }
        }

        // Poster-Left + Details-Right Cards
        items(list, key = { it.id }) { item ->
            val sMins = if (item.status == "Watched") (item.runtimeMinutes - (item.runtimeMinutes / item.speed).roundToInt()).coerceAtLeast(0) else 0
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardSurface), border = BorderStroke(1.dp, DividerLine), modifier = Modifier.fillMaxWidth().clickable { onOpenDetail(item) }) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PosterBox(url = item.posterUrl, title = item.title, modifier = Modifier.width(78.dp).height(116.dp).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(NeonGreen.copy(0.16f)).padding(6.dp, 2.dp)) { Text(item.type.uppercase(), color = NeonGreen, fontSize = 9.sp, fontWeight = FontWeight.Black) }
                            Spacer(Modifier.width(6.dp))
                            Text(item.title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(NeonGreen.copy(0.14f)).padding(6.dp, 3.dp)) { Text("You: ${String.format(Locale.US, "%.1f", item.userRating)}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(ImdbGold.copy(0.16f)).padding(6.dp, 3.dp)) { Text("IMDb: ${String.format(Locale.US, "%.1f", item.imdbRating)}", color = ImdbGold, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                        }
                        Text("${item.speed}x • ${formatHoursMins(item.runtimeMinutes)}" + if (sMins > 0) " (Saved ${sMins}m)" else "", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            val w = item.status == "Watched"
                            Box(Modifier.clip(RoundedCornerShape(7.dp)).background(if (w) NeonGreen else Color(0xFF1E1E22)).clickable { onToggle(item) }.padding(9.dp, 4.dp)) {
                                Text(if (w) "✓ Watched" else "+ Wishlist", color = if (w) OledBlack else ImdbGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Box(Modifier.clip(RoundedCornerShape(7.dp)).border(1.dp, NeonGreen.copy(0.6f), RoundedCornerShape(7.dp)).clickable { launchMpvRx(ctx, item.title) }.padding(9.dp, 4.dp)) {
                                Text("mpvRx ▶", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PosterBox(url: String, title: String, modifier: Modifier = Modifier) {
    Box(modifier.background(Color(0xFF17171C)), contentAlignment = Alignment.Center) {
        Text(title.take(2).uppercase(), color = NeonGreen.copy(0.5f), fontWeight = FontWeight.Black, fontSize = 20.sp)
        if (url.isNotBlank()) {
            AsyncImage(model = url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    }
}
