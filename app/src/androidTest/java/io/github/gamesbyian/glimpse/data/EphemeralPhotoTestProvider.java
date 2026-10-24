package io.github.gamesbyian.glimpse.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Test APK only: external ephemeral JPEG stream, not Photo Picker or user data.
 * Pure Java to avoid depending on Kotlin stdlib in separate androidTest APK.
 */
public final class EphemeralPhotoTestProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws java.io.FileNotFoundException {
        if (!"r".equals(mode)) throw new java.io.FileNotFoundException("Read-only");
        String leaf = uri.getLastPathSegment();
        if (leaf == null || !leaf.matches("fixture-[a-zA-Z0-9-]+\\.jpg"))
            throw new java.io.FileNotFoundException("Unexpected fixture name");
        final ParcelFileDescriptor[] pipe;
        try {
            pipe = ParcelFileDescriptor.createPipe();
        } catch (IOException error) {
            throw new java.io.FileNotFoundException("No fixture pipe: " + error);
        }
        Thread worker = new Thread(() -> {
            Bitmap bitmap = Bitmap.createBitmap(64, 88, Bitmap.Config.ARGB_8888);
            try (OutputStream output =
                     new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                bitmap.eraseColor(Color.rgb(47, 132, 183));
                bitmap.setPixel(13, 17, Color.WHITE);
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 93, output))
                    throw new IOException("JPEG fixture encoding failed");
            } catch (IOException ignored) {
                // Reader observes a failed stream and importer reports failure.
            } finally {
                bitmap.recycle();
            }
        }, "glimpse-test-jpeg-pipe");
        worker.setDaemon(true);
        worker.start();
        return pipe[0];
    }

    @Override public String getType(Uri uri) { return "image/jpeg"; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public Cursor query(Uri uri, String[] projection, String selection,
        String[] selectionArgs, String sortOrder) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues values,
        String selection, String[] args) { return 0; }
}
