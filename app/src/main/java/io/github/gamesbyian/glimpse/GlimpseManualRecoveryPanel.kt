package io.github.gamesbyian.glimpse

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import io.github.gamesbyian.glimpse.data.BoundedPhotoDecode
import io.github.gamesbyian.glimpse.data.GlimpseDatabase
import io.github.gamesbyian.glimpse.data.ManualMissingTargetStageResume
import io.github.gamesbyian.glimpse.data.MissingTargetStageCandidate
import io.github.gamesbyian.glimpse.data.MissingTargetStagePreflight
import io.github.gamesbyian.glimpse.data.PrivatePublicationReceiptJournal
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Explicit, one-photo-at-a-time consent candidate. No startup scan, no
 * background scheduler, no automatic mutation and no destructive recovery.
 *
 * All original bytes and receipt names remain in transient app memory. Only
 * count and safe collection title are displayed. A preview is compulsory
 * before consent, and the backend re-verifies the selected receipt/bytes.
 *
 * Do not ship until owner UX, API29/modern Android accessibility and complete
 * fail-closed outcome acceptance. This is a stacked draft on recovery #185.
 */
@Composable
internal fun GlimpseManualRecoveryPanel(context: Context) {
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var scan by remember { mutableStateOf<MissingTargetStagePreflight?>(null) }
    var scanned by remember { mutableStateOf(false) }
    var index by remember { mutableIntStateOf(0) }
    var loaded by remember { mutableStateOf<RecoveryPhotoPreview?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // Bitmap wraps app-private bytes. Never persist, export or log the photo.
    DisposableEffect(loaded) {
        onDispose { loaded?.bitmap?.recycle() }
    }

    fun select(candidates: List<MissingTargetStageCandidate>, selected: Int) {
        busy = true
        scope.launch {
            try {
                val candidate = candidates[selected]
                val preview = withContext(Dispatchers.IO) {
                    val task = currentCoroutineContext()
                    task.ensureActive()
                    val bitmap = BoundedPhotoDecode.decode(candidate.staged, maxEdge = 256)
                    val collection = GlimpseDatabase.open(context).dao()
                        .findCollection(candidate.collectionId)?.title
                    if (bitmap == null || collection.isNullOrBlank()) {
                        bitmap?.recycle()
                        null
                    } else RecoveryPhotoPreview(bitmap, collection)
                }
                loaded = preview
                index = selected
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                loaded = null
                message = "Could not preview this retained photo safely."
            } finally {
                busy = false
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "REVIEW INTERRUPTED IMPORTS",
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                "Optional. Check for intact photos that an interrupted import left " +
                    "outside the library. Glimpse will never replace or remove an " +
                    "existing photo to recover one. No file changes until you " +
                    "preview one photo and explicitly confirm it.",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedButton(
                modifier = Modifier.heightIn(min = 48.dp),
                enabled = !busy,
                onClick = {
                    busy = true
                    scan = null
                    loaded = null
                    message = null
                    scope.launch {
                        try {
                            val outcome = withContext(Dispatchers.IO) {
                                val task = currentCoroutineContext()
                                PrivatePublicationReceiptJournal(
                                    File(context.filesDir, "originals")
                                ).inspectMissingTargetStages(
                                    checkpoint = { task.ensureActive() }
                                )
                            }
                            scan = outcome
                            scanned = true
                            if (outcome == null) {
                                message = "The check could not finish safely. No files changed."
                                busy = false
                            } else if (outcome.candidates.isEmpty()) {
                                message = "No fully verified, safely missing-target " +
                                    "photo candidates were found. Other interruptions " +
                                    "may still require manual attention."
                                busy = false
                            } else {
                                select(outcome.candidates, 0)
                            }
                        } catch (cancel: CancellationException) {
                            throw cancel
                        } catch (_: Exception) {
                            scanned = true
                            message = "Unable to inspect interrupted imports."
                            busy = false
                        }
                    }
                }
            ) { Text(if (busy) "Checking…" else "Review retained photos") }

            val current = scan?.candidates?.getOrNull(index)
            if (scanned && scan != null) {
                Text(
                    "${scan!!.possibleCount} eligible source(s) in a bounded scan. " +
                        "${scan!!.omittedReceipts} receipt(s) omitted.",
                    style = MaterialTheme.typography.bodySmall
                )
                if (scan!!.hashBudgetExhausted) {
                    Text(
                        "Some files were not hashed within the safety limit.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (current != null) {
                Text(
                    "Retained photo ${index + 1} of ${scan!!.candidates.size}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (loaded?.bitmap != null) {
                    Image(
                        bitmap = loaded!!.bitmap.asImageBitmap(),
                        contentDescription = "Preview of one intact retained photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp)
                    )
                    Text(
                        "Destination collection: ${loaded!!.collectionTitle}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        if (busy) "Checking the selected photo preview…"
                        else "Photo preview unavailable. Recovery cannot be confirmed.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = !busy && index > 0,
                    onClick = { select(scan!!.candidates, index - 1) }
                ) { Text("Previous retained photo") }
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = !busy && index < scan!!.candidates.lastIndex,
                    onClick = { select(scan!!.candidates, index + 1) }
                ) { Text("Next retained photo") }
                OutlinedButton(
                    modifier = Modifier.heightIn(min = 48.dp),
                    enabled = !busy && loaded?.bitmap != null,
                    onClick = { confirm = true }
                ) { Text("Review restoring this photo") }
            }
            if (message != null) {
                Text(message!!, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "An exact existing original, partial digest, unreadable file or " +
                    "unknown owner is never an automatic recovery candidate. " +
                    "Retained staging files and receipts are never discarded.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    val selected = scan?.candidates?.getOrNull(index)
    if (confirm && selected != null && loaded?.bitmap != null) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Restore this one photo?") },
            text = {
                Text(
                    "Try to add the previewed photo to ${loaded!!.collectionTitle}. " +
                        "Glimpse will first verify the exact retained bytes and " +
                        "that the destination filename is still absent. It will " +
                        "not delete, overwrite or quarantine any file. If an " +
                        "interruption occurs, the result may need further review."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        confirm = false
                        busy = true
                        val receiptName = selected.receipt.file.name
                        scope.launch {
                            try {
                                val result = ManualMissingTargetStageResume(
                                    File(context.filesDir, "originals"),
                                    GlimpseDatabase.open(context).dao()
                                ).resumeOnlyAfterExplicitConfirmation(
                                    userConfirmed = true,
                                    selectedReceiptName = receiptName
                                )
                                message = when {
                                    result.attached == 1 ->
                                        "One photo was added to its collection. " +
                                        "Its staging copy and receipt were preserved."
                                    result.duplicates > 0 ->
                                        "That photo was already in the selected collection. " +
                                        "No original was replaced."
                                    else ->
                                        "This photo could not be safely restored. " +
                                        "The staging copy and receipt were preserved; " +
                                        "an unregistered destination may need further review."
                                }
                            } catch (cancel: CancellationException) {
                                throw cancel
                            } catch (_: Exception) {
                                message = "Recovery could not complete safely. " +
                                    "Review local evidence before attempting anything else."
                            } finally {
                                // A preflight is not valid after any attempted
                                // publication. Require a fresh explicit scan.
                                scan = null
                                loaded = null
                                busy = false
                            }
                        }
                    }
                ) { Text("Restore this photo") }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text("Cancel") }
            }
        )
    }
}

private data class RecoveryPhotoPreview(
    val bitmap: Bitmap,
    val collectionTitle: String
)
