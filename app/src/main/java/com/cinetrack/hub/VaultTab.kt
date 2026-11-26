package com.cinetrack.hub

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
private fun VaultCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = CardSurface), border = BorderStroke(1.dp, DividerLine), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            content()
        }
    }
}

@Composable
fun VaultTab(entries: List<MediaEntry>, sync: SyncProgress, onSync: () -> Unit, onImport: (ImportResult) -> Unit) {
    val ctx = LocalContext.current
    var apiKey by remember { mutableStateOf(StorageHelper.getTmdbKey(ctx)) }
    val missing = entries.count { it.posterUrl.isBlank() }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) onImport(DupeGuard.merge(entries, CsvHelper.importFromUri(ctx, uri)))
    }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri: Uri? ->
        if (uri != null) { CsvHelper.exportToUri(ctx, uri, entries); Toast.makeText(ctx, "Exported ${entries.size} titles", Toast.LENGTH_SHORT).show() }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        VaultCard("TMDB API KEY") {
            OutlinedTextField(value = apiKey, onValueChange = { apiKey = it; StorageHelper.saveTmdbKey(ctx, it) }, placeholder = { Text("Paste free TMDB v3 API key...", color = TextMuted) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        VaultCard("POSTER SYNC") {
            Text(if (missing == 0) "Saare ${entries.size} titles ke posters ready hain" else "$missing titles ka poster missing hai. Sync sirf poster/backdrop/overview bharta hai, ratings nahi chhuta.", color = TextMuted, fontSize = 12.sp)
            if (sync.running || sync.total > 0) {
                LinearProgressIndicator(progress = { if (sync.total == 0) 0f else sync.done.toFloat() / sync.total }, color = NeonGreen, trackColor = DividerLine, modifier = Modifier.fillMaxWidth())
                Text("${sync.done}/${sync.total} checked • ${sync.found} posters mile", color = TextWhite, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
            Button(onClick = onSync, enabled = !sync.running && missing > 0, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack, disabledContainerColor = DividerLine, disabledContentColor = TextMuted), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text(if (sync.running) "Syncing..." else "Sync all missing posters", fontWeight = FontWeight.ExtraBold)
            }
        }
        VaultCard("CSV VAULT") {
            Text("Letterboxd / Trakt / Movie Paradise / Custom CSV. Library aur same file ke duplicate titles auto-skip hote hain.", color = TextMuted, fontSize = 12.sp)
            Button(onClick = { imp.launch(arrayOf("text/*", "text/csv")) }, colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = OledBlack), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Import CSV", fontWeight = FontWeight.ExtraBold)
            }
            OutlinedButton(onClick = { exp.launch("cinetrack_backup.csv") }, border = BorderStroke(1.dp, DividerLine), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Export Clean CSV Backup", color = NeonGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}
