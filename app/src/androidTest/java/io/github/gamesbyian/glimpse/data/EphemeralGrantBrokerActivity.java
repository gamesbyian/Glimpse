package io.github.gamesbyian.glimpse.data;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/**
 * Test-only independent UID grants provider read capability to Glimpse.
 * No Kotlin library classes required inside the separate test APK process.
 */
public final class EphemeralGrantBrokerActivity extends Activity {
    public static final String ACTION_GRANT =
        "io.github.gamesbyian.glimpse.test.ACTION_GRANT_SOURCE";
    public static final String ACTION_REVOKE =
        "io.github.gamesbyian.glimpse.test.ACTION_REVOKE_SOURCE";
    public static final String EXTRA_TARGET_PACKAGE = "consumerPackage";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        try {
            Intent command = getIntent();
            Uri uri = command.getData();
            if (uri == null) throw new IllegalArgumentException("URI absent");
            String action = command.getAction();
            if (ACTION_GRANT.equals(action)) {
                String target = command.getStringExtra(EXTRA_TARGET_PACKAGE);
                if (target == null) throw new IllegalArgumentException("No consumer");
                grantUriPermission(target, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else if (ACTION_REVOKE.equals(action)) {
                revokeUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                throw new IllegalArgumentException("Unknown grant command");
            }
        } finally {
            finish();
        }
    }
}
