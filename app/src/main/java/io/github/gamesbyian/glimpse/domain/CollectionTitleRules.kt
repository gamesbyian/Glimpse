package io.github.gamesbyian.glimpse.domain

/**
 * A user-facing title, not an immutable identity. Existing collection IDs,
 * photo memberships and shuffle ownership must never be changed by renaming.
 */
internal fun checkedCollectionTitle(raw: String): String {
    val title = raw.trim()
    require(title.isNotEmpty()) { "Enter a collection name." }
    require(title.length <= 80) { "Collection names must be 80 characters or shorter." }
    require(title.none { it.isISOControl() }) {
        "Collection names cannot contain control characters."
    }
    return title
}
