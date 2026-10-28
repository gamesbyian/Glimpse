package io.github.gamesbyian.glimpse

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.gamesbyian.glimpse.data.LocalOriginalIntegritySummary
import io.github.gamesbyian.glimpse.data.LocalStorageInventory
import io.github.gamesbyian.glimpse.data.inspectLocalStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offline, source-faithful M1 help/privacy entry. No fabricated Play channel,
 * account sync, exact five-minute alarm or wallpaper readback is presented.
 */
@Composable
internal fun GlimpseSupportScreen(
    model: GlimpseModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentState by model.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val auditScope = rememberCoroutineScope()
    var archiveOpenError by remember { mutableStateOf<String?>(null) }
    val restorePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) model.restoreCollectionZip(uri)
    }
    var integrityReadout by remember { mutableStateOf<IntegrityReadout>(IntegrityReadout.NotChecked) }
    val packageInfo = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }.getOrNull()
    }
    val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    var externalError by remember { mutableStateOf<String?>(null) }
    // User-edited report text stays in Compose RAM and is never SavedState-persisted.
    var diagnosticsVisible by remember { mutableStateOf(false) }
    var diagnosticDraft by remember { mutableStateOf("") }
    val inventory by produceState<StorageReadout>(StorageReadout.Loading, context.filesDir) {
        value = withContext(Dispatchers.IO) {
            val result = inspectLocalStorage(File(context.filesDir, "originals"))
            if (result == null) StorageReadout.Unavailable else StorageReadout.Ready(result)
        }
    }
    val storageSummary = when (val current = inventory) {
        StorageReadout.Loading -> "Calculating local storage usage…"
        StorageReadout.Unavailable ->
            "Storage inspection is unavailable. No files were changed."
        is StorageReadout.Ready ->
            "${current.snapshot.originalFileCount} stored original files · " +
                Formatter.formatShortFileSize(context, current.snapshot.originalBytes) +
                " of image data. " +
                "${current.snapshot.stagingFileCount} temporary import fragments · " +
                Formatter.formatShortFileSize(context, current.snapshot.stagingBytes) + "."
    }

    Column(
        modifier = modifier.fillMaxSize().background(Color(0xFFF6F0E6))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TextButton(onClick = onBack) { Text("Back to Home") }
        Text(
            "GOOD TO KNOW",
            color = Color(0xFF97684D),
            style = MaterialTheme.typography.labelLarge,
            letterSpacing = 1.5.sp
        )
        Text(
            "Help & privacy.",
            fontFamily = GlimpseType.editorial,
            fontSize = 36.sp,
            color = Color(0xFF193B3F)
        )

        SupportSection(
            "GETTING STARTED",
            "Create or choose a collection, then tap Add photos. The Android Photo Picker " +
                "lets you select images from supported providers. Glimpse verifies and " +
                "keeps an offline private copy of each imported original.",
            "Browse photo reel opens the physical disc and stationary preview. Swipe " +
                "the disc or use Previous photo / Next photo; View all · Grid is an " +
                "equivalent alternative. Browsing never applies a wallpaper."
        )
        SupportSection(
            "APPLY & SHUFFLE",
            "Set Home wallpaper now is the explicit apply action. Glimpse shows the " +
                "last Android-acknowledged photo but cannot reliably read back the " +
                "current wallpaper after another app changes it.",
            "Start shuffle uses the named collection selected at that moment. " +
                "Five-minute eligibility is checked only while Glimpse is visible. " +
                "Background timing, exact alarms, Lock and Both targeting are not " +
                "available in this build. Review uncertain attempts before restarting."
        )
        SupportSection(
            "SHARING & ORIGINALS",
            "Share original photo opens Android's Sharesheet after verifying the " +
                "private original. It may contain camera, time or location EXIF data. " +
                "Glimpse does not guarantee delivery to another app or strip metadata. " +
                "Save original copy opens Android's document picker for one verified file.",
            "Android Share can send images into Glimpse. Confirm the receiving " +
                "collection and tap Import shared images; an unchanged private copy " +
                "is saved. Paste image reads clipboard only when explicitly tapped. " +
                "Photo Detail can preview crop edges and 90-degree rotations and save a " +
                "separate edited PNG with a 3072-pixel edge limit and stripped original " +
                "EXIF. Durable edit recipes and applying edited wallpapers are pending. " +
                "Batch outbound sharing is limited to 25 originals. Cloud-album " +
                "synchronization is not implemented; temporary provider grants can " +
                "expire but completed offline originals remain available."
        )
        SupportSection(
            "PRIVACY & STORAGE",
            "Glimpse has no account, advertising SDK, proprietary photo cloud or " +
                "automatic diagnostic upload. Photos and collection records are kept " +
                "in the app's private storage. Android app auto-backup is disabled.",
            "Uninstalling Glimpse or clearing its app storage can permanently remove " +
                "its private imported copies and collections. Export collection ZIP " +
                "can save the unchanged photos and a manifest to an Android-selected " +
                "document destination, but it does not export wallpaper settings " +
                "or support in-app restore. Verify the destination before uninstalling. " +
                "Exports include original EXIF data and the collection title."
        )
        SupportSection(
            "LOCAL STORAGE · READ ONLY",
            storageSummary,
            "Counts are physical SHA-named files and file lengths, not the number " +
                "of photos assigned to a collection or a fresh integrity check. " +
                "The same original may belong to multiple collections.",
            "This screen never deletes files. Temporary fragments may be active or " +
                "awaiting cleanup; do not manually remove imported originals because " +
                "that can break collection access and sharing."
        )
        OutlinedButton(
            enabled = integrityReadout != IntegrityReadout.Checking,
            onClick = {
                integrityReadout = IntegrityReadout.Checking
                auditScope.launch {
                    integrityReadout = try {
                        IntegrityReadout.Ready(model.auditPrivateOriginals())
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        IntegrityReadout.Unavailable
                    }
                }
            }
        ) { Text("Verify saved originals") }
        when (val audit = integrityReadout) {
            IntegrityReadout.NotChecked -> Text(
                "Optional. Checks the exact bytes of photos registered in the library. " +
                    "This can take time for large collections; no files are changed.",
                style = MaterialTheme.typography.bodySmall
            )
            IntegrityReadout.Checking -> Text(
                "Verifying private photo originals…",
                style = MaterialTheme.typography.bodySmall
            )
            IntegrityReadout.Unavailable -> Text(
                "The integrity check could not finish. No files were changed.",
                style = MaterialTheme.typography.bodySmall
            )
            is IntegrityReadout.Ready -> Text(
                "Verified ${audit.summary.verified} of ${audit.summary.registered} " +
                    "registered originals. ${audit.summary.problemCount} missing or " +
                    "failed verification. This does not inspect unregistered orphan " +
                    "files, repair problems or validate copies held by other apps.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        SupportSection(
            "RESTORE AN EXPORTED COLLECTION",
            "Import a Glimpse originals-only ZIP through Android Documents. " +
                "All entries, image types, byte counts and SHA-256 hashes must " +
                "validate before creating a separate new collection. Existing " +
                "collections and wallpaper settings are never replaced.",
            "This imports photographs only. It cannot recreate prior wallpaper " +
                "or shuffle settings. Large collections need enough free storage " +
                "for temporary verification before the new collection is saved."
        )
        OutlinedButton(onClick = {
            archiveOpenError = null
            try {
                restorePicker.launch(arrayOf("application/zip", "application/octet-stream"))
            } catch (_: Exception) {
                archiveOpenError = "Android could not open the archive picker."
            }
        }) { Text("Restore collection ZIP…") }
        if (archiveOpenError != null) {
            Text(archiveOpenError!!, color = MaterialTheme.colorScheme.error)
        }
        if (currentState.message.startsWith("Restored ") ||
            currentState.message.contains("archive", ignoreCase = true)) {
            Text(currentState.message, style = MaterialTheme.typography.bodyMedium)
        }
        Text("After restoring, return Home to browse the new collection. " +
            "An invalid or interrupted archive leaves existing collections unchanged.",
            style = MaterialTheme.typography.bodySmall)
        SupportSection(
            "OPTIONAL SUPPORT REPORT",
            "Generate a local-only diagnostic report. Inspect, edit, copy or " +
                "share its text; nothing uploads automatically. No photo pixels, " +
                "filenames, EXIF or collection IDs are included."
        )
        OutlinedButton(onClick = {
            val info = (inventory as? StorageReadout.Ready)?.snapshot
            diagnosticDraft = LocalDiagnosticReport.render(
                DiagnosticFacts(
                    versionCode = packageInfo?.longVersionCode ?: -1,
                    isDebug = isDebug,
                    androidApi = Build.VERSION.SDK_INT,
                    originalCount = info?.originalFileCount,
                    originalBytes = info?.originalBytes,
                    stagingCount = info?.stagingFileCount,
                    stagingBytes = info?.stagingBytes
                )
            )
            diagnosticsVisible = true
            externalError = null
        }) { Text("Generate local diagnostic report") }
        if (diagnosticsVisible) {
            OutlinedTextField(
                value = diagnosticDraft,
                onValueChange = { diagnosticDraft = it.take(8_000) },
                label = { Text("Review or edit diagnostic text") },
                minLines = 7,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "The report leaves Glimpse only if you tap Copy or Share. " +
                    "Review any additional text for private information.",
                style = MaterialTheme.typography.bodySmall
            )
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedButton(onClick = {
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText(
                            "Glimpse support report", diagnosticDraft
                        ))
                        externalError = "Report copied to Android clipboard."
                    } catch (_: Exception) {
                        externalError = "Could not copy the report."
                    }
                }) { Text("Copy report") }
                OutlinedButton(onClick = {
                    try {
                        val payload = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, diagnosticDraft)
                        }
                        context.startActivity(Intent.createChooser(
                            payload, "Share Glimpse support report"
                        ))
                        externalError = null
                    } catch (_: Exception) {
                        externalError = "No app could share this report. " +
                            "You can copy it instead."
                    }
                }) { Text("Share report") }
                androidx.compose.material3.TextButton(onClick = {
                    diagnosticsVisible = false
                    diagnosticDraft = ""
                    externalError = null
                }) { Text("Close report") }
            }
        }
        SupportSection(
            "ABOUT THIS BUILD",
            "Version: " + (packageInfo?.versionName ?: "Unavailable") +
                " · build " + (packageInfo?.longVersionCode?.toString() ?: "unknown"),
            if (isDebug) "Development/debug APK. No Play Store distribution is claimed."
            else "Installed app. Distribution source and update channel are unverified."
        )
        TextButton(
            onClick = {
                externalError = null
                try {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/gamesbyian/Glimpse/issues"))
                    )
                } catch (_: Exception) {
                    externalError = "No app could open GitHub. The address is " +
                        "github.com/gamesbyian/Glimpse/issues"
                }
            }
        ) { Text("Open Glimpse issues on GitHub") }
        if (externalError != null) {
            Text(externalError!!, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SupportSection(heading: String, vararg paragraphs: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                heading,
                style = MaterialTheme.typography.labelLarge,
                color = Color(0xFF8B5A3C),
                letterSpacing = 1.1.sp
            )
            paragraphs.forEach { paragraph ->
                Text(paragraph, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private sealed interface StorageReadout {
    data object Loading : StorageReadout
    data object Unavailable : StorageReadout
    data class Ready(val snapshot: LocalStorageInventory) : StorageReadout
}

private sealed interface IntegrityReadout {
    data object NotChecked : IntegrityReadout
    data object Checking : IntegrityReadout
    data object Unavailable : IntegrityReadout
    data class Ready(val summary: LocalOriginalIntegritySummary) : IntegrityReadout
}
