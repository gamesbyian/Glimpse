package io.github.gamesbyian.glimpse.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CollectionRemovalSafetyTest {
    @Test fun allowsEmptyOrFullyRedundantCollectionWithoutOriginalDeletion() {
        assertEquals(CollectionRemovalSafety.SAFE,
            collectionRemovalSafety(true,0,false,false))
    }

    @Test fun refusesToStrandAnySingleMembershipOriginal() {
        assertEquals(CollectionRemovalSafety.HAS_EXCLUSIVE_ORIGINALS,
            collectionRemovalSafety(true,1,false,false))
        assertEquals(CollectionRemovalSafety.HAS_EXCLUSIVE_ORIGINALS,
            collectionRemovalSafety(true,200,false,false))
    }

    @Test fun blocksLossOfRotationSourceOrUncertainPendingOperation() {
        assertEquals(CollectionRemovalSafety.OWNS_ROTATION,
            collectionRemovalSafety(true,0,true,false))
        assertEquals(CollectionRemovalSafety.UNCERTAIN_WALLPAPER,
            collectionRemovalSafety(true,0,false,true))
    }

    @Test fun rejectsStaleCollectionIdentity() {
        assertEquals(CollectionRemovalSafety.NOT_FOUND,
            collectionRemovalSafety(false,0,false,false))
    }
}
