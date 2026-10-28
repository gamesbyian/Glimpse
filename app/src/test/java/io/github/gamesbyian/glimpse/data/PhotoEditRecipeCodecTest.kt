package io.github.gamesbyian.glimpse.data

import io.github.gamesbyian.glimpse.domain.NormalizedCrop
import io.github.gamesbyian.glimpse.domain.PhotoEditGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoEditRecipeCodecTest {
    @Test fun roundTripsAllQuarterTurnsAndFractionalCrop() {
        for (quarter in 0..3) {
            val edited = PhotoEditGeometry(
                quarter, NormalizedCrop(.11, .23, .78, .92)
            )
            assertEquals(edited, PhotoEditRecipeCodec.decode(
                PhotoEditRecipeCodec.encode(edited)
            ))
        }
    }

    @Test fun malformedUnboundedOrFutureVersionRecipesFailClosed() {
        listOf<String?>(
            null, "", "2|0|0|0|1|1", "1|4|0|0|1|1",
            "1|0|NaN|0|1|1", "1|0|0|0|-1|1",
            "1|0|0|0|0|1", "1|0|0|0|1|Infinity",
            "1|0|0|0|1", "1|0|0|0|1|1|extra",
            "x".repeat(300)
        ).forEach { assertNull(PhotoEditRecipeCodec.decode(it)) }
    }

    @Test fun defaultRecipeRoundTripRemainsFullOriginal() {
        assertEquals(PhotoEditGeometry(),
            PhotoEditRecipeCodec.decode(
                PhotoEditRecipeCodec.encode(PhotoEditGeometry())
            ))
    }
}
