package io.github.gamesbyian.glimpse.domain

/**
 * Advisory only: imported metadata dimensions and selected normalized crop
 * estimate how many *source* pixels remain, before bounded PNG export and
 * any unknown Android launcher wallpaper crop/zoom.
 */
internal data class CropResolutionAdvisory(
    val width: Int,
    val height: Int,
    val belowDisplay: Boolean
)

internal fun estimateCropResolution(
    geometry: PhotoEditGeometry,
    uprightWidth: Int,
    uprightHeight: Int,
    displayWidth: Int,
    displayHeight: Int
): CropResolutionAdvisory {
    require(displayWidth > 0 && displayHeight > 0)
    val crop = geometry.pixelCrop(uprightWidth, uprightHeight)
    return CropResolutionAdvisory(
        width = crop.width,
        height = crop.height,
        belowDisplay = crop.width < displayWidth || crop.height < displayHeight
    )
}
