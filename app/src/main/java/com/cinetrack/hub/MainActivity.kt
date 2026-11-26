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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

val OledBlack = Color(0xFF000000)
val CardDark = Color(0xFF111315)
val GlassDark = Color(0xD91A1E21)
val BorderGlass = Color(0xFF2A2F33)
val NeonLime = Color(0xFFB6FF3C)
val MintTeal = Color(0xFF2CE6B8)
val ImdbGold = Color(0xFFF5C518)
val TextWhite = Color(0xFFF5F5F7)
val TextMuted = Color(0xFF8E949A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = OledBlack, surface = CardDark, primary = NeonLime, onBackground = TextWhite, onSurface = TextWhite)) {
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
    var query by remember { mutableStateOf("") }
    var wallType by remember { mutableStateOf("Movie") }
    var wallStatus by remember { mutableStateOf("Watched") }
    var triggerSearch by remember { mutableStateOf(false) }

    fun saveAll(list: List<MediaEntry>) { entries = list; StorageHelper.saveEntries(ctx, list) }

    if (selectedItem != null) {
        DetailScreen(
            item = selectedItem!!,
            onBack = { selectedItem = null },
            onUpdate = { updated -> selectedItem = updated; saveAll(entries.map { if (it.id == updated.id) updated else it }) },
            onDelete = { del -> saveAll(entries.filterNot { it.id == del.id }); selectedItem = null }
        )
        return
    }

    Scaffold(
        containerColor = OledBlack,
        bottomBar = {
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.weight(1f).height(62.dp).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xE622282B), Color(0xF5101315))))
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.28f), NeonLime.copy(0.24f))), CircleShape)
                        .padding(horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        listOf(
                            Triple(0, "Home", Icons.Default.Home),
                            Triple(1, "Movies", Icons.Default.PlayArrow),
                            Triple(2, "Shows", Icons.Default.List),
                            Triple(3, "Vault", Icons.Default.Star)
                        ).forEach { (idx, label, icon) ->
                            val sel = tab == idx
                            Column(
                                Modifier.clip(RoundedCornerShape(16.dp)).background(if (sel) MintTeal.copy(0.22f) else Color.Transparent)
                                    .clickable {
                                        tab = idx
                                        if (idx == 1) wallType = "Movie"
                                        if (idx == 2) wallType = "Series"
                                    }.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(icon, null, tint = if (sel) MintTeal else TextWhite.copy(0.8f), modifier = Modifier.size(19.dp))
                                Text(label, color = if (sel) MintTeal else TextMuted, fontSize = 10.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium)
                            }
                        }
                    }
                }
                Box(
                    Modifier.size(62.dp).clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(Color(0xE6242A2D), Color(0xF5111416))))
                        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(0.30f), NeonLime.copy(0.35f))), CircleShape)
                        .clickable { wallType = "All"; tab = 1; triggerSearch = !triggerSearch },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Search, null, tint = TextWhite, modifier = Modifier.size(23.dp))
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> HomeDashboardScreen(
                    entries = entries,
                    onOpenAdd = { showAdd = true },
                    onJumpToWall = { status, type -> wallStatus = status; wallType = type; tab = 1 },
                    onOpenDetail = { selectedItem = it },
                    onSaveAll = { saveAll(it) }
                )
                1, 2 -> PosterWallScreen(
                    entries = entries,
                    typeFilter = wallType,
                    onTypeChange = { wallType = it },
                    statusFilter = wallStatus,
                    onStatusChange = { wallStatus = it },
                    query = query,
                    onQueryChange = { query = it },
                    triggerSearch = triggerSearch,
                    onBackHome = { tab = 0 },
                    onOpenAdd = { showAdd = true },
                    onOpenDetail = { selectedItem = it },
                    onToggle = { item -> saveAll(entries.map { if (it.id == item.id) it.copy(status = if (it.status == "Watched") "Wishlist" else "Watched") else it }) },
                    onSaveAll = { saveAll(it) }
                )
                3 -> VaultHubScreen(entries) { imported ->
                    val existing = entries.map { it.title.trim().lowercase(Locale.US) }.toSet()
                    val clean = imported.distinctBy { it.title.trim().lowercase(Locale.US) }.filter { it.title.trim().lowercase(Locale.US) !in existing }
                    saveAll(clean + entries)
                    Toast.makeText(ctx, "Added ${clean.size} new (${imported.size - clean.size} duplicates skipped)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    if (showAdd) AddTitleDialog({ showAdd = false }) { saveAll(listOf(it) + entries); showAdd = false }
}

@Composable
fun HomeDashboardScreen(
    entries: List<MediaEntry>,
    onOpenAdd: () -> Unit,
    onJumpToWall: (status: String, type: String) -> Unit,
    onOpenDetail: (MediaEntry) -> Unit,
    onSaveAll: (List<MediaEntry>) -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var previewSpeed by remember { mutableDoubleStateOf(1.5) }
    var homeFilter by remember { mutableStateOf("All") }
    var syncText by remember { mutableStateOf("") }

    val scoped = if (homeFilter == "All") entries else entries.filter { it.type.equals(homeFilter, true) }
    val watched = scoped.filter { it.status == "Watched" }
    val wishCount = scoped.count { it.status != "Watched" }
    val origMins = watched.sumOf { it.runtimeMinutes }
    val actMins = watched.sumOf { (it.runtimeMinutes / it.speed).roundToInt() }
    val saved = (origMins - actMins).coerceAtLeast(0)
    val avgYou = if (watched.isNotEmpty()) watched.map { it.userRating }.average() else 0.0
    val imdbValid = watched.filter { it.imdbRating > 0.0 }
    val avgImdb = if (imdbValid.isNotEmpty()) imdbValid.map { it.imdbRating }.average() else 0.0
    val missingPosters = entries.count { it.posterUrl.isBlank() }
    val apiKey = remember { StorageHelper.getTmdbKey(ctx) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 15.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Hello Tu2", color = TextWhite, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Welcome back • ${entries.size} titles in vault", color = TextMuted, fontSize = 11.sp)
                }
                Row(Modifier.clip(CircleShape).background(CardDark).border(1.dp, BorderGlass, CircleShape).padding(10.dp, 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tu2", color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Box(Modifier.size(22.dp).clip(CircleShape).background(NeonLime), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Person, null, tint = OledBlack, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        item {
            Button(onClick = onOpenAdd, colors = ButtonDefaults.buttonColors(containerColor = NeonLime, contentColor = OledBlack), shape = CircleShape, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Default.Add, null, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text("Add new title", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            }
        }

        item {
            Card(shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, NeonLime.copy(0.35f)), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(Brush.radialGradient(listOf(Color(0xFF7ED926), Color(0xFF26540E), Color(0xFF0A1208)), center = Offset(650f, 90f), radius = 520f))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Speed-Watch Saved", color = TextWhite.copy(0.9f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Box(Modifier.size(28.dp).clip(CircleShape).background(NeonLime), contentAlignment = Alignment.Center) {
                                Text("⚡", color = OledBlack, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            }
                        }
                        Text(formatHoursMins(saved), color = TextWhite, fontSize = 36.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total: ${formatHoursMins(origMins)}", color = TextWhite.copy(0.75f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("Actual: ${formatHoursMins(actMins)}", color = NeonLime, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf(1.0, 1.35, 1.5, 1.75).forEach { sp ->
                                val act = previewSpeed == sp
                                Box(Modifier.weight(1f).clip(CircleShape).background(if (act) NeonLime else Color(0x88000000)).clickable { previewSpeed = sp }.padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                                    Text("${sp}x", color = if (act) OledBlack else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Movie", "Series", "Anime").forEach { f ->
                    val sel = homeFilter == f
                    Box(Modifier.weight(1f).clip(CircleShape).background(if (sel) NeonLime else CardDark).border(1.dp, if (sel) NeonLime else BorderGlass, CircleShape).clickable { homeFilter = f }.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(f, color = if (sel) OledBlack else TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, BorderGlass), colors = CardDefaults.cardColors(containerColor = CardDark), modifier = Modifier.weight(1f).clickable { onJumpToWall("Watched", homeFilter) }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF1F2326)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Check, null, tint = NeonLime, modifier = Modifier.size(15.dp)) }
                            Text("${watched.size}", color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Text("Watched", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = NeonLime), modifier = Modifier.weight(1f).clickable { onJumpToWall("Watched", homeFilter) }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(OledBlack.copy(0.14f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = OledBlack, modifier = Modifier.size(15.dp)) }
                            Text(String.format(Locale.US, "%.2f", avgYou), color = OledBlack, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Text("You avg", color = OledBlack.copy(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = NeonLime), modifier = Modifier.weight(1f).clickable { onJumpToWall("Watched", homeFilter) }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(OledBlack.copy(0.14f)), contentAlignment = Alignment.Center) { Text("IM", color = OledBlack, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                            Text(if (avgImdb > 0.0) String.format(Locale.US, "%.2f", avgImdb) else "—", color = OledBlack, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Text("IMDb avg", color = OledBlack.copy(0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Card(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, BorderGlass), colors = CardDefaults.cardColors(containerColor = CardDark), modifier = Modifier.weight(1f).clickable { onJumpToWall("Wishlist", homeFilter) }) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF1F2326)), contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = TextWhite, modifier = Modifier.size(15.dp)) }
                            Text("$wishCount", color = TextWhite, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Text("Wishlist", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        item {
            val bars = listOf(
                "MOV" to entries.count { it.type == "Movie" },
                "SER" to entries.count { it.type == "Series" },
                "ANI" to entries.count { it.type == "Anime" },
                "1.35x" to entries.count { it.speed == 1.35 },
                "1.5x" to entries.count { it.speed == 1.5 },
                "1.75x" to entries.count { it.speed == 1.75 },
                "9+" to entries.count { it.userRating >= 9.0 }
            )
            val maxV = (bars.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
            Card(shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, BorderGlass), colors = CardDefaults.cardColors(containerColor = CardDark)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Vault Breakdown", color = TextWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Live Stats", color = NeonLime, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                        bars.forEachIndexed { i, (lbl, count) ->
                            val ratio = (count.toFloat() / maxV).coerceIn(0.18f, 1f)
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Box(Modifier.fillMaxWidth().height((52 * ratio).dp).clip(RoundedCornerShape(8.dp)).background(if (i >= 3) NeonLime else Color(0xFF23272A)))
                                Text(lbl, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        if (apiKey.isNotBlank() && (missingPosters > 0 || syncText.isNotEmpty())) {
            item {
                Button(
                    onClick = {
                        if (syncText.isEmpty()) {
                            syncText = "Syncing..."
                            scope.launch {
                                val mutable = entries.toMutableList()
                                val missing = mutable.indices.filter { mutable[it].posterUrl.isBlank() }
                                var fetched = 0
                                missing.forEachIndexed { step, idx ->
                                    val cur = mutable[idx]
                                    val hit = TmdbHelper.searchTmdb(apiKey, cur.title).firstOrNull()
                                    if (hit != null && hit.posterUrl.isNotBlank()) {
                                        mutable[idx] = cur.copy(posterUrl = hit.posterUrl, backdropUrl = hit.backdropUrl.ifBlank { cur.backdropUrl }, imdbRating = if (cur.imdbRating > 0.0) cur.imdbRating else hit.imdbRating, overview = cur.overview.ifBlank { hit.overview })
                                        fetched++
                                    }
                                    syncText = "Syncing HD Posters: ${step + 1} / ${missing.size}"
                                    if ((step + 1) % 5 == 0 || step + 1 == missing.size) onSaveAll(mutable.toList())
                                    delay(90)
                                }
                                syncText = ""
                                Toast.makeText(ctx, "Synced $fetched HD posters (IMDb preserved)!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GlassDark, contentColor = NeonLime),
                    border = BorderStroke(1.dp, NeonLime.copy(0.45f)), shape = CircleShape, modifier = Modifier.fillMaxWidth()
                ) { Text(syncText.ifEmpty { "⚡ Sync All $missingPosters HD Posters (Keep IMDb Scores)" }, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun PosterWallScreen(
    entries: List<MediaEntry>,
    typeFilter: String,
    onTypeChange: (String) -> Unit,
    statusFilter: String,
    onStatusChange: (String) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    triggerSearch: Boolean,
    onBackHome: () -> Unit,
    onOpenAdd: () -> Unit,
    onOpenDetail: (MediaEntry) -> Unit,
    onToggle: (MediaEntry) -> Unit,
    onSaveAll: (List<MediaEntry>) -> Unit
) {
    val ctx = LocalContext.current
    val focusReq = remember { FocusRequester() }
    var cardMode by remember { mutableStateOf(false) }
    var showTypeBar by remember { mutableStateOf(false) }

    LaunchedEffect(triggerSearch) { if (triggerSearch) runCatching { focusReq.requestFocus() } }

    val watchedCount = entries.count { it.status == "Watched" && (typeFilter == "All" || it.type.equals(typeFilter, true)) }
    val wishCount = entries.count { it.status != "Watched" && (typeFilter == "All" || it.type.equals(typeFilter, true)) }

    val filtered = entries.filter { item ->
        val matchQ = query.isBlank() || item.title.contains(query, true) || item.tags.any { t -> t.contains(query, true) }
        val matchStatus = when (statusFilter) { "Watched" -> item.status == "Watched"; "Wishlist" -> item.status != "Watched"; else -> true }
        val matchType = typeFilter == "All" || item.type.equals(typeFilter, true) || (typeFilter == "Series" && item.type.equals("Anime", true))
        matchQ && matchStatus && matchType
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 20.dp)) {
        item {
            Row(Modifier.fillMaxWidth().statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(GlassDark).border(1.dp, BorderGlass, CircleShape).clickable { onBackHome() }, contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, null, tint = TextWhite, modifier = Modifier.size(18.dp))
                    }
                    Text(if (typeFilter == "All") "All Titles" else "${typeFilter}s", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                }
                Row(Modifier.clip(CircleShape).background(GlassDark).border(1.dp, BorderGlass, CircleShape).padding(4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.clip(CircleShape).clickable { cardMode = !cardMode }.padding(10.dp, 6.dp)) {
                        Text(if (cardMode) "3-Wall" else "Cards", color = MintTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(Modifier.clip(CircleShape).background(NeonLime).clickable { onOpenAdd() }.padding(10.dp, 6.dp)) {
                        Text("+ Add", color = OledBlack, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search your movies by title", color = TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextWhite.copy(0.85f)) },
                trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Close, null, tint = TextMuted) } },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = GlassDark, unfocusedContainerColor = GlassDark, focusedBorderColor = MintTeal, unfocusedBorderColor = BorderGlass),
                modifier = Modifier.fillMaxWidth().focusRequester(focusReq)
            )
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.weight(1f).clip(CircleShape).background(GlassDark).border(1.dp, BorderGlass, CircleShape).padding(4.dp)) {
                    listOf("Wishlist" to "⏳ Watchlist - $wishCount", "Watched" to "✓ Watched - $watchedCount").forEach { (key, label) ->
                        val sel = statusFilter == key
                        Box(
                            Modifier.weight(1f).clip(CircleShape).background(if (sel) Color(0xFF284338) else Color.Transparent)
                                .border(1.dp, if (sel) MintTeal.copy(0.65f) else Color.Transparent, CircleShape)
                                .clickable { onStatusChange(if (statusFilter == key) "All" else key) }.padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (sel) NeonLime else TextWhite.copy(0.8f), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        }
                    }
                }
                Box(Modifier.size(40.dp).clip(CircleShape).background(GlassDark).border(1.dp, BorderGlass, CircleShape).clickable { showTypeBar = !showTypeBar }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Menu, null, tint = TextWhite, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (showTypeBar) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Movie", "Series", "Anime")) { t ->
                        val sel = typeFilter == t
                        FilterChip(selected = sel, onClick = { onTypeChange(t) }, label = { Text(t, fontSize = 11.sp, fontWeight = FontWeight.Bold) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonLime, selectedLabelColor = OledBlack, containerColor = GlassDark, labelColor = TextWhite))
                    }
                }
            }
        }

        if (!cardMode) {
            items(filtered.chunked(3)) { rowItems ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    rowItems.forEach { item ->
                        val isW = item.status == "Watched"
                        Box(Modifier.weight(1f).aspectRatio(0.67f).clip(RoundedCornerShape(13.dp)).background(CardDark).clickable { onOpenDetail(item) }) {
                            PosterBox(url = item.posterUrl, title = item.title, modifier = Modifier.fillMaxSize())
                            Box(
                                Modifier.align(Alignment.TopEnd).padding(6.dp).size(26.dp).clip(CircleShape)
                                    .background(if (isW) Color(0xCC1E2622) else Color(0x88000000))
                                    .border(1.dp, if (isW) NeonLime.copy(0.8f) else Color.White.copy(0.4f), CircleShape)
                                    .clickable { onToggle(item) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = if (isW) NeonLime else Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                val isW = item.status == "Watched"
                val savedM = if (isW) (item.runtimeMinutes - (item.runtimeMinutes / item.speed).roundToInt()).coerceAtLeast(0) else 0
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = CardDark), border = BorderStroke(1.dp, BorderGlass), modifier = Modifier.fillMaxWidth().clickable { onOpenDetail(item) }) {
                    Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
                        PosterBox(url = item.posterUrl, title = item.title, modifier = Modifier.width(84.dp).height(124.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFF1A2A3A)).padding(7.dp, 2.dp)) {
                                Text(item.type.uppercase(), color = Color(0xFF66B2FF), fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                            Text("You: ${String.format(Locale.US, "%.1f", item.userRating)}/10 • IMDb: ${if (item.imdbRating > 0.0) String.format(Locale.US, "%.1f", item.imdbRating) else "—"}", color = TextWhite.copy(0.85f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("${item.speed}x • $savedM min saved", color = ImdbGold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.clip(CircleShape).background(if (isW) Color(0xFF173322) else GlassDark).clickable { onToggle(item) }.padding(10.dp, 4.dp)) {
                                    Text(if (isW) "Watched" else "+ Wishlist", color = if (isW) NeonLime else ImdbGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Box(Modifier.clip(CircleShape).background(Color(0xFF162836)).clickable { launchMpvRx(ctx, item.title) }.padding(10.dp, 4.dp)) {
                                    Text("mpvRx ▶", color = MintTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VaultHubScreen(entries: List<MediaEntry>, onImport: (List<MediaEntry>) -> Unit) {
    var subTab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(0 to "Taste DNA", 1 to "CSV & TMDB Vault").forEach { (idx, label) ->
                val sel = subTab == idx
                Box(Modifier.weight(1f).clip(CircleShape).background(if (sel) NeonLime else CardDark).border(1.dp, if (sel) NeonLime else BorderGlass, CircleShape).clickable { subTab = idx }.padding(vertical = 10.dp), contentAlignment = Alignment.Center) {
                    Text(label, color = if (sel) OledBlack else TextWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (subTab == 0) StatsScreen(entries) else CsvVaultScreen(entries, onImport)
    }
}

@Composable
fun PosterBox(url: String, title: String, modifier: Modifier = Modifier) {
    Box(modifier.background(GlassDark), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(6.dp)) {
            Text(title.take(2).uppercase(), color = NeonLime.copy(0.55f), fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(title, color = TextMuted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (url.isNotBlank()) AsyncImage(model = url, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}
