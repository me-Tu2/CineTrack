package com.cinetrack.hub

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.util.Locale

@Composable
fun DetailScreen(item: MediaEntry, onBack: () -> Unit, onUpdate: (MediaEntry) -> Unit, onDelete: (MediaEntry) -> Unit) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().background(OledBlack).verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(270.dp)) {
            val bg = item.backdropUrl.ifBlank { item.posterUrl }
            if (bg.isNotBlank()) AsyncImage(model = bg, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x44000000), OledBlack))))
            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(12.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xAA000000))) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = NeonGreen)
            }
            Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom) {
                PosterBox(url = item.posterUrl, title = item.title, modifier = Modifier.width(86.dp).height(126.dp).clip(RoundedCornerShape(12.dp)).border(1.dp, NeonGreen.copy(0.5f), RoundedCornerShape(12.dp)))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.type.uppercase(), color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    Text(item.title, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(NeonGreen.copy(0.18f)).padding(8.dp, 4.dp)) { Text("You: ${String.format(Locale.US, "%.1f", item.userRating)}/10", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(ImdbGold.copy(0.18f)).padding(8.dp, 4.dp)) { Text("IMDb: ${String.format(Locale.US, "%.1f", item.imdbRating)}", color = ImdbGold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Button(onClick = { launchMpvRx(ctx, item.title) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Play in mpvRx (${item.speed}x)", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }

            if (item.overview.isNotBlank()) {
                Text("OVERVIEW", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(item.overview, color = TextWhite, fontSize = 13.sp, lineHeight = 19.sp)
            }

            Text("PLAYBACK SPEED", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.0, 1.35, 1.5, 1.75).forEach { sp ->
                    FilterChip(selected = item.speed == sp, onClick = { onUpdate(item.copy(speed = sp)) }, label = { Text("${sp}x", fontWeight = FontWeight.Bold) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NeonGreen, selectedLabelColor = OledBlack))
                }
            }

            if (item.type != "Movie" && item.totalEpisodes > 1) {
                Text("EPISODE TRACKER (${item.watchedEpisodes}/${item.totalEpisodes})", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                (1..minOf(item.totalEpisodes, 30)).forEach { ep ->
                    val done = ep <= item.watchedEpisodes
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(CardSurface).clickable { onUpdate(item.copy(watchedEpisodes = if (done) ep - 1 else ep, status = if (ep >= item.totalEpisodes) "Watched" else item.status)) }.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Episode $ep", color = TextWhite, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(if (done) "✓ Watched" else "Mark", color = if (done) NeonGreen else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { launchMpvRx(ctx, "${item.title} E$ep") }, modifier = Modifier.size(26.dp)) { Icon(Icons.Default.PlayArrow, null, tint = NeonGreen) }
                        }
                    }
                }
            }

            OutlinedButton(onClick = { onDelete(item) }, border = BorderStroke(1.dp, Color(0xFF552222)), modifier = Modifier.fillMaxWidth()) {
                Text("Remove from Vault", color = Color(0xFFFF6B6B))
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
    var apiKey by remember { mutableStateOf(StorageHelper.getTmdbKey(ctx)) }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? -> if (uri != null) onImport(CsvHelper.importFromUri(ctx, uri)) }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? -> if (uri != null) { CsvHelper.exportToUri(ctx, uri, entries); Toast.makeText(ctx, "Exported!", Toast.LENGTH_SHORT).show() } }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("TMDB API KEY (AUTO POSTERS & BACKDROPS)", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        OutlinedTextField(value = apiKey, onValueChange = { apiKey = it; StorageHelper.saveTmdbKey(ctx, it) }, placeholder = { Text("Paste free TMDB v3 API key...", color = TextMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        HorizontalDivider(color = DividerLine)
        Text("1-SECOND CSV VAULT", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Button(onClick = { imp.launch(arrayOf("text/*", "text/csv")) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Import CSV (Letterboxd / Trakt / Custom)", fontWeight = FontWeight.ExtraBold) }
        OutlinedButton(onClick = { exp.launch("cinetrack_backup.csv") }, border = BorderStroke(1.dp, DividerLine), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) { Text("Export Clean CSV Backup", color = NeonGreen, fontWeight = FontWeight.Bold) }
    }
}

@Composable
fun AddTitleDialog(onDismiss: () -> Unit, onSave: (MediaEntry) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val apiKey = remember { StorageHelper.getTmdbKey(ctx) }
    var title by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Movie") }; var status by remember { mutableStateOf("Watched") }
    var uRat by remember { mutableStateOf("9.5") }; var iRat by remember { mutableStateOf("8.5") }; var mins by remember { mutableStateOf("140") }
    var poster by remember { mutableStateOf("") }; var backdrop by remember { mutableStateOf("") }; var overview by remember { mutableStateOf("") }
    var spd by remember { mutableDoubleStateOf(1.5) }; var results by remember { mutableStateOf<List<MediaEntry>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss, containerColor = CardSurface, title = { Text("Add Movie / Anime", fontWeight = FontWeight.Bold, color = NeonGreen) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.weight(1f))
                    if (apiKey.isNotBlank()) {
                        Button(onClick = { scope.launch { results = TmdbHelper.searchTmdb(apiKey, title) } }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack), contentPadding = PaddingValues(10.dp)) {
                            Icon(Icons.Default.Search, null)
                        }
                    }
                }
                results.forEach { hit ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF18181D)).clickable {
                        title = hit.title; type = hit.type; iRat = hit.imdbRating.toString(); mins = hit.runtimeMinutes.toString()
                        poster = hit.posterUrl; backdrop = hit.backdropUrl; overview = hit.overview; results = emptyList()
                    }.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PosterBox(url = hit.posterUrl, title = hit.title, modifier = Modifier.width(34.dp).height(50.dp).clip(RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(8.dp))
                        Text("${hit.title} (${hit.type} • ★${hit.imdbRating})", color = TextWhite, fontSize = 12.sp, maxLines = 2)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Movie", "Series", "Anime").forEach { t -> FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t, fontSize = 11.sp) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf("Watched", "Wishlist").forEach { s -> FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s, fontSize = 11.sp) }) } }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = uRat, onValueChange = { uRat = it }, label = { Text("You") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = iRat, onValueChange = { iRat = it }, label = { Text("IMDb") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = mins, onValueChange = { mins = it }, label = { Text("Runtime (mins)") }, singleLine = true)
                OutlinedTextField(value = poster, onValueChange = { poster = it }, label = { Text("Poster Image URL (optional)") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { listOf(1.0, 1.35, 1.5, 1.75).forEach { sp -> FilterChip(selected = spd == sp, onClick = { spd = sp }, label = { Text("${sp}x", fontSize = 11.sp) }) } }
            }
        },
        confirmButton = { Button(onClick = { if (title.isNotBlank()) onSave(MediaEntry(title = title.trim(), type = type, status = status, userRating = uRat.toDoubleOrNull() ?: 9.0, imdbRating = iRat.toDoubleOrNull() ?: 8.0, runtimeMinutes = mins.toIntOrNull() ?: 120, speed = spd, posterUrl = poster.trim(), backdropUrl = backdrop.trim(), overview = overview)) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack)) { Text("Save", fontWeight = FontWeight.ExtraBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextMuted) } }
    )
}
