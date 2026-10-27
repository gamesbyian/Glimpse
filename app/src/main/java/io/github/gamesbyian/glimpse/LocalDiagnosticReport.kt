package io.github.gamesbyian.glimpse

/**
 * Allowlisted public-runtime and numeric local-storage facts only.
 * Do not add filenames, URIs, IDs, arbitrary exception text or user photos.
 */
internal data class DiagnosticFacts(
    val versionCode: Long,
    val isDebug: Boolean,
    val androidApi: Int,
    val originalCount: Int?,
    val originalBytes: Long?,
    val stagingCount: Int?,
    val stagingBytes: Long?
)

internal object LocalDiagnosticReport {
    fun render(facts: DiagnosticFacts): String {
        val channel = if (facts.isDebug) "Debug/development" else "Distribution unverified"
        val storage = if (facts.originalCount == null || facts.originalBytes == null ||
            facts.stagingCount == null || facts.stagingBytes == null
        ) "Unavailable (no files modified)" else
            facts.originalCount.toString() + " originals / " + facts.originalBytes + " bytes; " +
                facts.stagingCount + " staging fragments / " + facts.stagingBytes + " bytes"
        return listOf(
            "Glimpse support report (local, user-reviewed)",
            "",
            "App version code: " + facts.versionCode,
            "Build channel: " + channel,
            "Android API level: " + facts.androidApi,
            "Private image storage: " + storage,
            "",
            "Notes:",
            "[Describe the issue here before sharing.]",
            "",
            "This report contains numerical storage totals and public build",
            "information only. No image pixels, EXIF, URI, filename,",
            "collection name or collection ID is added automatically.",
            "Exporting is optional; review and edit the text first."
        ).joinToString("\n")
    }
}
