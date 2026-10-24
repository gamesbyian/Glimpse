package io.github.gamesbyian.glimpse.data

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Runs in the separate androidTest APK UID as the URI provider's owner.
 * Instrumentation itself runs inside the TARGET app UID and cannot grant a
 * third-party provider URI, regardless of which Context object it holds.
 * This activity gives a real explicit Android URI grant to the target,
 * then revokes it on the opposite instruction. Never ships in release APK.
 */
class EphemeralGrantBrokerActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val uri = requireNotNull(intent.data) { "Test source URI missing" }
            when (intent.action) {
                ACTION_GRANT -> {
                    val target = requireNotNull(intent.getStringExtra(EXTRA_TARGET_PACKAGE))
                    grantUriPermission(target, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                ACTION_REVOKE -> {
                    revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                else -> error("Unrecognized test permission operation")
            }
        } finally {
            finish()
        }
    }

    companion object {
        const val ACTION_GRANT = "io.github.gamesbyian.glimpse.test.ACTION_GRANT_SOURCE"
        const val ACTION_REVOKE = "io.github.gamesbyian.glimpse.test.ACTION_REVOKE_SOURCE"
        const val EXTRA_TARGET_PACKAGE = "consumerPackage"
    }
}
