package com.cinetrack.hub

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

private val BgObsidian = Color(0xFF090D16)
private val GlassCard = Color(0xFF131B2E)
private val GlassBorder = Color(0xFF25324D)
private val ImdbGold = Color(0xFFF5C518)
private val SpeedCyan = Color(0xFF00E5FF)
private val NeonPurple = Color(0xFF9D6BFF)
private val TextPrimary = Color(0xFFF3F6FC)
private val TextMuted = Color(0xFF8E9BB5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = BgObsidian, surface = GlassCard, primary = SpeedCyan, onBackground = TextPrimary, onSurface = TextPrimary)) {
                Surface(modifier = Modifier.fillMaxSize(), color = BgObsidian) { CineTrackApp() }
            }
        }
    }
}

@Composable
fun CineTrackApp() {
    val ctx = LocalContext.current
    var entries by remember { mutableStateOf(StorageHelper.loadEntries(ctx)) }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    fun saveAll(list: List<MediaEntry>) { entries = list; StorageHelper.saveEntries(ctx, list) }

    Scaffold(
        containerColor = BgObsidian,
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CINETRACK", color = SpeedCyan, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.5.sp)
                    Text("${entries.size} titles in vault", color = TextMuted, fontSize = 12.sp)
                }
                Button(onClick = { showAdd = true }, colors = ButtonDefaults.buttonColors(containerColor = SpeedCyan, contentColor = BgObsidian), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF0E1422)) {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Library") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.Star, null) }, label = { Text("Taste DNA") })
                NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Share, null) }, label = { Text("CSV Vault") })
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> LibraryScreen(entries, { item -> saveAll(entries.map { if (it.id == item.id) it.copy(status = if (it.status == "Watched") "Wishlist" else "Watched") else it }) }, { item -> saveAll(entries.filterNot { it.id == item.id }) })
                1 -> StatsScreen(entries)
                2 -> CsvVaultScreen(entries) { imported -> saveAll(imported + entries); Toast.makeText(ctx, "Imported ${imported.size}!", Toast.LENGTH_SHORT).show() }
            }
        }
    }
    if (showAdd) AddTitleDialog({ showAdd = false }) { saveAll(listOf(it) + entries); showAdd = false }
}

@Composable
fun LibraryScreen(entries: List<MediaEntry>, onToggle: (MediaEntry) -> Unit, onDel: (MediaEntry) -> Unit) {
    var q by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    val watched = entries.filter { it.status == "Watched" }
    val origMins = watched.sumOf { it.runtimeMinutes }
    val actMins = watched.sumOf { (it.runtimeMinutes / it.speed).roundToInt() }
    val saved = (origMins - actMins).coerceAtLeast(0)
    val list = entries.filter { (it.title.contains(q, true) || it.tags.any { t -> t.contains(q, true) }) && (filter == "All" || it.status == filter || it.type.equals(filter, true)) }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Card(shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, SpeedCyan.copy(0.35f)), colors = CardDefaults.cardColors(containerColor = Color.Transparent)) {
                Box(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF152238), Color(0xFF0E1626)))).padding(16.dp)) {
                    Column {
                        Text("⚡ SPEED-WATCH TIME SAVED", color = SpeedCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                            Column { Text(formatHoursMins(saved), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold); Text("Saved at 1.35x–1.75x", color = TextMuted, fontSize = 12.sp) }
                            Column(horizontalAlignment = Alignment.End) { Text("Total: ${formatHoursMins(origMins)}", color = TextPrimary, fontSize = 12.sp); Text("Actual: ${formatHoursMins(actMins)}", color = SpeedCyan, fontSize = 12.sp) }
                        }
                    }
                }
            }
        }
        item { OutlinedTextField(value = q, onValueChange = { q = it }, placeholder = { Text("Search title or tag...", color = TextMuted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) }, singleLine = true, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) }
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(listOf("All", "Watched", "Wishlist", "Anime", "Movie", "Series")) { f -> FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f, fontSize = 12.sp) }) } } }
        items(list, key = { it.id }) { item ->
            val sMins = if (item.status == "Watched") (item.runtimeMinutes - (item.runtimeMinutes / item.speed).roundToInt()).coerceAtLeast(0) else 0
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = GlassCard), border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(NeonPurple.copy(0.2f)).padding(7.dp, 2.dp)) { Text(item.type.uppercase(), color = NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(8.dp))
                            Text(item.title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onDel(item) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SpeedCyan.copy(0.15f)).padding(8.dp, 4.dp)) { Text("You: ${String.format(Locale.US, "%.1f", item.userRating)}/10", color = SpeedCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(ImdbGold.copy(0.18f)).padding(8.dp, 4.dp)) { Text("IMDb: ${String.format(Locale.US, "%.1f", item.imdbRating)}", color = ImdbGold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        }
                        val w = item.status == "Watched"
                        Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (w) Color(0xFF153B2E) else Color(0xFF362A14)).clickable { onToggle(item) }.padding(10.dp, 4.dp)) {
                            Text(if (w) "✓ Watched" else "+ Wishlist", color = if (w) Color(0xFF4ADE80) else ImdbGold, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Runtime: ${formatHoursMins(item.runtimeMinutes)} • ${item.speed}x" + if (sMins > 0) " (Saved ${sMins}m)" else "", color = TextMuted, fontSize = 11.sp)
                    if (item.tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { item.tags.forEach { t -> Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF1D273D)).padding(8.dp, 3.dp)) { Text(t, color = TextMuted, fontSize = 10.sp) } } }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsScreen(entries: List<MediaEntry>) {
    val w = entries.filter { it.status == "Watched" }
    val avgU = if (w.isNotEmpty()) w.map { it.userRating }.average() else 0.0
    val avgI = if (w.isNotEmpty()) w.map { it.imdbRating }.average() else 0.0
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("PERSONAL TASTE DNA", color = SpeedCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        listOf(
            Triple("Your Avg Rating", "${String.format(Locale.US, "%.2f", avgU)} / 10", SpeedCyan),
            Triple("IMDb Avg Rating", "${String.format(Locale.US, "%.2f", avgI)} / 10", ImdbGold),
            Triple("Watched vs Wishlist", "${w.size} Watched • ${entries.size - w.size} Wishlist", Color(0xFF4ADE80)),
            Triple("Rated Higher Than IMDb", "${w.count { it.userRating > it.imdbRating }} titles", NeonPurple)
        ).forEach { (k, v, c) ->
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = GlassCard), border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(k, color = TextMuted); Text(v, color = c, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun CsvVaultScreen(entries: List<MediaEntry>, onImport: (List<MediaEntry>) -> Unit) {
    val ctx = LocalContext.current
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) onImport(CsvHelper.importFromUri(ctx, uri)) }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? -> if (uri != null) { CsvHelper.exportToUri(ctx, uri, entries); Toast.makeText(ctx, "Exported!", Toast.LENGTH_SHORT).show() } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("1-SECOND CSV VAULT", color = SpeedCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Button(onClick = { imp.launch(arrayOf("text/*", "text/csv")) }, colors = ButtonDefaults.buttonColors(containerColor = SpeedCyan, contentColor = BgObsidian), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Import CSV (Letterboxd / Trakt / Custom)", fontWeight = FontWeight.Bold) }
        OutlinedButton(onClick = { exp.launch("cinetrack_backup.csv") }, border = BorderStroke(1.dp, ImdbGold), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Export Clean CSV Backup", color = ImdbGold, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun AddTitleDialog(onDismiss: () -> Unit, onSave: (MediaEntry) -> Unit) {
    var title by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Movie") }; var status by remember { mutableStateOf("Watched") }
    var uRat by remember { mutableStateOf("9.5") }; var iRat by remember { mutableStateOf("8.5") }; var mins by remember { mutableStateOf("140") }
    var spd by remember { mutableDoubleStateOf(1.5) }; val tags = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = GlassCard, title = { Text("Add Title", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Movie", "Series", "Anime").forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 11.sp) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Watched", "Wishlist").forEach { s -> FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s, fontSize = 11.sp) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uRat, onValueChange = { uRat = it }, label = { Text("You") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = iRat, onValueChange = { iRat = it }, label = { Text("IMDb") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = mins, onValueChange = { mins = it }, label = { Text("Runtime (mins)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(1.0, 1.35, 1.5, 1.75).forEach { sp -> FilterChip(selected = spd == sp, onClick = { spd = sp }, label = { Text("${sp}x", fontSize = 11.sp) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                    listOf("Hindi Dub", "Original Audio", "1.0x Dialogue", "Rewatch").forEach { tg -> FilterChip(selected = tags.contains(tg), onClick = { if (tags.contains(tg)) tags.remove(tg) else tags.add(tg) }, label = { Text(tg, fontSize = 11.sp) }) }
                }
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onSave(MediaEntry(title = title.trim(), type = type, status = status, userRating = uRat.toDoubleOrNull() ?: 9.0, imdbRating = iRat.toDoubleOrNull() ?: 8.0, runtimeMinutes = mins.toIntOrNull() ?: 120, speed = spd, tags = tags.toList())) }, colors = ButtonDefaults.buttonColors(containerColor = SpeedCyan, contentColor = BgObsidian)) { Text("Save", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } }
    )
}
