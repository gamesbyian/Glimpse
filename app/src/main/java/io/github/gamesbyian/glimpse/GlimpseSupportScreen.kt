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
import androidx.compose.foundation.layout.heightIn
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
import io.github.gamesbyian.glimpse.data.LibraryRelationshipAudit
import io.github.gamesbyian.glimpse.data.LocalStorageInventory
import io.github.gamesbyian.glimpse.data.PotentialOrphanInventory
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
    var orphanReadout by remember { mutableStateOf<OrphanReadout>(OrphanReadout.NotChecked) }
    var relationshipsReadout by remember {
        mutableStateOf<RelationshipReadout>(RelationshipReadout.NotChecked)
    }
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
        TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) { Text("Back to Home") }
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
                "Five-minute eligibility is checked while Glimpse is visible. " +
                "Optional background checks are off by default; when enabled, " +
                "Android runs them no more frequently than every 15 minutes " +
                "and may defer them much longer. Exact alarms, Lock and Both " +
                "targeting are not supported. Review uncertain attempts before restarting.",
            "On Android, edit your Quick Settings tiles to add Next Home photo. " +
                "This requests one change from the saved shuffle collection, " +
                "even when rotation is paused. Android may delay the request; " +
                "open Glimpse to verify the outcome. The tile cannot set Lock wallpaper.",
            "Add Glimpse Home from Android's widget picker to preview the next saved " +
                "shuffle photo when its identity can be verified. If the next " +
                "photo is not established, the widget says so instead of " +
                "inventing a preview. CHANGE HOME is an explicit request, not " +
                "a confirmed Android result; widget refresh is OS-delayed " +
                "and is not a five-minute background scheduler."
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
                "EXIF. Non-destructive crop/rotation recipes can be retained, " +
                "and separate edited PNGs can be saved to a collection or applied " +
                "to Home through the existing wallpaper command. Free-angle " +
                "straightening and source-wide automatic editing remain unavailable. " +
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
                "document destination, but it does not include wallpaper or shuffle " +
                "settings. The Restore collection ZIP action below creates a new " +
                "photo-only collection. Verify the saved destination before " +
                "uninstalling. Exports include original EXIF data and the collection title."
        )
        SupportSection(
            "LOCAL STORAGE · READ ONLY",
            storageSummary,
            "Counts are physical SHA-named files and file lengths, not the number " +
                "of photos assigned to a collection or a fresh integrity check. " +
                "The same original may belong to multiple collections.",
            "This screen never deletes files. A nonempty import fragment may hold the " +
                "only complete photo after an interrupted publication; Glimpse " +
                "preserves those bytes pending verified recovery. Never manually " +
                "delete a fragment or imported original, clear app storage, or " +
                "uninstall merely to solve an import failure."
        )
        OutlinedButton(
            modifier = Modifier.heightIn(min = 48.dp),
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
            "UNREGISTERED ORIGINALS · READ ONLY",
            "A crash after Glimpse finishes copying an original but before Room saves " +
                "its record can leave a valuable photo file outside all collections. " +
                "This check counts potential unreferenced SHA-named files. It does " +
                "not verify their bytes, recover them or assume they are disposable.",
            "Never delete these files just because they are unreferenced. " +
                "A current import or ZIP restore can also temporarily appear here."
        )
        OutlinedButton(
            modifier = Modifier.heightIn(min = 48.dp),
            enabled = orphanReadout != OrphanReadout.Checking,
            onClick = {
                orphanReadout = OrphanReadout.Checking
                auditScope.launch {
                    orphanReadout = try {
                        val snapshot = model.auditPotentialOrphanOriginals()
                        if (snapshot == null) OrphanReadout.Unavailable
                        else OrphanReadout.Ready(snapshot)
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        OrphanReadout.Unavailable
                    }
                }
            }
        ) { Text("Check for unregistered originals") }
        when (val result = orphanReadout) {
            OrphanReadout.NotChecked -> Text(
                "Optional local metadata comparison. No files are touched.",
                style = MaterialTheme.typography.bodySmall
            )
            OrphanReadout.Checking -> Text(
                "Comparing stored file names with registered photos…",
                style = MaterialTheme.typography.bodySmall
            )
            OrphanReadout.Unavailable -> Text(
                "The storage comparison could not finish. No files were changed.",
                style = MaterialTheme.typography.bodySmall
            )
            is OrphanReadout.Ready -> Text(
                "${result.snapshot.unreferencedOriginals} potentially unregistered " +
                    "original(s) among ${result.snapshot.scannedOriginals} completed " +
                    "SHA-named files, totalling " +
                    Formatter.formatShortFileSize(
                        context, result.snapshot.unreferencedBytes
                    ) +
                    ". No files were changed. This is not deletion clearance.",
                style = MaterialTheme.typography.bodySmall
            )
        }
        SupportSection(
            "LIBRARY LINKS · READ ONLY",
            "Check whether every saved original belongs to a collection, and whether " +
                "memberships, favorites and the saved shuffle record point to " +
                "existing database records. This " +
                "examines links only and does not validate image bytes.",
            "Broken links can be inherited from an interrupted older import. " +
                "Glimpse will not silently discard or repair such data."
        )
        OutlinedButton(
            modifier = Modifier.heightIn(min = 48.dp),
            enabled = relationshipsReadout != RelationshipReadout.Checking,
            onClick = {
                relationshipsReadout = RelationshipReadout.Checking
                auditScope.launch {
                    relationshipsReadout = try {
                        RelationshipReadout.Ready(model.auditLibraryRelationships())
                    } catch (cancel: CancellationException) {
                        throw cancel
                    } catch (_: Exception) {
                        RelationshipReadout.Unavailable
                    }
                }
            }
        ) { Text("Check saved library links") }
        when (val result = relationshipsReadout) {
            RelationshipReadout.NotChecked -> Text(
                "Optional and read-only. No photos or settings are changed.",
                style = MaterialTheme.typography.bodySmall
            )
            RelationshipReadout.Checking -> Text(
                "Checking library relationships…",
                style = MaterialTheme.typography.bodySmall
            )
            RelationshipReadout.Unavailable -> Text(
                "The library consistency check could not finish. Nothing was changed.",
                style = MaterialTheme.typography.bodySmall
            )
            is RelationshipReadout.Ready -> Text(
                "${result.snapshot.issueCount} potential broken library link(s). " +
                    "Photo memberships: ${result.snapshot.membershipsWithoutPhoto}; " +
                    "collection memberships: ${result.snapshot.membershipsWithoutCollection}; " +
                    "favorites: ${result.snapshot.favoritesWithoutPhoto}; " +
                    "photos without collections: ${result.snapshot.photosWithoutMembership}; " +
                    "saved rotation references: " +
                    listOf(
                        result.snapshot.rotationCollectionMissing,
                        result.snapshot.pendingPhotoMissing,
                        result.snapshot.acknowledgedPhotoMissing
                    ).count { it } + ". " +
                    "No records or originals were changed.",
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
        OutlinedButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
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
        OutlinedButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
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
                OutlinedButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
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
                OutlinedButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
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
                androidx.compose.material3.TextButton(modifier = Modifier.heightIn(min = 48.dp), onClick = {
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
            modifier = Modifier.heightIn(min = 48.dp),
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

private sealed interface OrphanReadout {
    data object NotChecked : OrphanReadout
    data object Checking : OrphanReadout
    data object Unavailable : OrphanReadout
    data class Ready(val snapshot: PotentialOrphanInventory) : OrphanReadout
}

private sealed interface RelationshipReadout {
    data object NotChecked : RelationshipReadout
    data object Checking : RelationshipReadout
    data object Unavailable : RelationshipReadout
    data class Ready(val snapshot: LibraryRelationshipAudit) : RelationshipReadout
}
