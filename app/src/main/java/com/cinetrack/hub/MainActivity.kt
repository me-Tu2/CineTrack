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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.roundToInt

private val OledBlack = Color(0xFF000000)
private val CardSurface = Color(0xFF0A0A0A)
private val DividerLine = Color(0xFF242424)
private val NeonGreen = Color(0xFFB6FF3C)
private val SecondaryGreen = Color(0xFF72C94A)
private val ImdbGold = Color(0xFFF5C518)
private val TextWhite = Color(0xFFF5F5F5)
private val TextMuted = Color(0xFF777777)

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
    var showAdd by remember { mutableStateOf(false) }
    fun saveAll(list: List<MediaEntry>) { entries = list; StorageHelper.saveEntries(ctx, list) }

    Scaffold(
        containerColor = OledBlack,
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp, 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("CINETRACK", color = NeonGreen, fontSize = 20.sp, fontWeight = FontWeight.Black, letterSpacing = 1.8.sp)
                    Text("${entries.size} TITLES IN VAULT", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
            Card(shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, NeonGreen.copy(0.45f)), colors = CardDefaults.cardColors(containerColor = CardSurface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("⚡ SPEED-WATCH SAVED", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(NeonGreen.copy(0.15f)).padding(8.dp, 2.dp)) {
                            Text("OLED MODE", color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        Column {
                            Text(formatHoursMins(saved), color = NeonGreen, fontSize = 30.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                            Text("Saved at 1.35x–1.75x speed", color = TextMuted, fontSize = 12.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Content: ${formatHoursMins(origMins)}", color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Text("Actual: ${formatHoursMins(actMins)}", color = SecondaryGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
        item { OutlinedTextField(value = q, onValueChange = { q = it }, placeholder = { Text("Search title or tag...", color = TextMuted) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = NeonGreen) }, singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Watched", "Wishlist", "Anime", "Movie", "Series")) { f ->
                    val sel = filter == f
                    FilterChip(selected = sel, onClick = { filter = f }, label = { Text(f, fontSize = 12.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonGreen, selectedLabelColor = OledBlack, containerColor = CardSurface, labelColor = TextMuted))
                }
            }
        }
        items(list, key = { it.id }) { item ->
            val sMins = if (item.status == "Watched") (item.runtimeMinutes - (item.runtimeMinutes / item.speed).roundToInt()).coerceAtLeast(0) else 0
            Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardSurface), border = BorderStroke(1.dp, DividerLine), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(Modifier.clip(RoundedCornerShape(4.dp)).background(NeonGreen.copy(0.15f)).padding(6.dp, 2.dp)) { Text(item.type.uppercase(), color = NeonGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                            Spacer(Modifier.width(8.dp))
                            Text(item.title, color = TextWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = { onDel(item) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Default.Delete, null, tint = TextMuted, modifier = Modifier.size(16.dp)) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(NeonGreen.copy(0.14f)).padding(8.dp, 4.dp)) { Text("You: ${String.format(Locale.US, "%.1f", item.userRating)}", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(ImdbGold.copy(0.15f)).padding(8.dp, 4.dp)) { Text("IMDb: ${String.format(Locale.US, "%.1f", item.imdbRating)}", color = ImdbGold, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFF161616)).padding(8.dp, 4.dp)) { Text("${item.speed}x", color = SecondaryGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
                        }
                        val w = item.status == "Watched"
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (w) NeonGreen else Color(0xFF1A1A1A)).clickable { onToggle(item) }.padding(10.dp, 4.dp)) {
                            Text(if (w) "✓ Watched" else "+ Wishlist", color = if (w) OledBlack else ImdbGold, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Runtime: ${formatHoursMins(item.runtimeMinutes)}" + if (sMins > 0) " • Saved ${sMins}m" else "", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    if (item.tags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { item.tags.forEach { t -> Box(Modifier.clip(RoundedCornerShape(4.dp)).border(1.dp, DividerLine, RoundedCornerShape(4.dp)).padding(8.dp, 3.dp)) { Text(t, color = TextMuted, fontSize = 10.sp) } } }
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
        Text("PERSONAL TASTE DNA", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
        listOf(
            Triple("Your Avg Rating", "${String.format(Locale.US, "%.2f", avgU)} / 10", NeonGreen),
            Triple("IMDb Avg Rating", "${String.format(Locale.US, "%.2f", avgI)} / 10", ImdbGold),
            Triple("Watched vs Wishlist", "${w.size} Watched • ${entries.size - w.size} Wishlist", SecondaryGreen),
            Triple("Rated Higher Than IMDb", "${w.count { it.userRating > it.imdbRating }} titles", TextWhite)
        ).forEach { (k, v, c) ->
            Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = CardSurface), border = BorderStroke(1.dp, DividerLine), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(k, color = TextMuted); Text(v, color = c, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) }
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
        Text("1-SECOND CSV VAULT", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 1.sp)
        Button(onClick = { imp.launch(arrayOf("text/*", "text/csv")) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Import CSV (Letterboxd / Trakt / Custom)", fontWeight = FontWeight.ExtraBold) }
        OutlinedButton(onClick = { exp.launch("cinetrack_backup.csv") }, border = BorderStroke(1.dp, DividerLine), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Export Clean CSV Backup", color = NeonGreen, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun AddTitleDialog(onDismiss: () -> Unit, onSave: (MediaEntry) -> Unit) {
    var title by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Movie") }; var status by remember { mutableStateOf("Watched") }
    var uRat by remember { mutableStateOf("9.5") }; var iRat by remember { mutableStateOf("8.5") }; var mins by remember { mutableStateOf("140") }
    var spd by remember { mutableDoubleStateOf(1.5) }; val tags = remember { mutableStateListOf<String>() }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardSurface, title = { Text("Add Title", fontWeight = FontWeight.Bold, color = NeonGreen) },
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
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onSave(MediaEntry(title = title.trim(), type = type, status = status, userRating = uRat.toDoubleOrNull() ?: 9.0, imdbRating = iRat.toDoubleOrNull() ?: 8.0, runtimeMinutes = mins.toIntOrNull() ?: 120, speed = spd, tags = tags.toList())) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack)) { Text("Save", fontWeight = FontWeight.ExtraBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } }
    )
}
