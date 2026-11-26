package deez.nuts.egob.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

/** Copies user-selected documents into private app storage. */
public final class FileStorage {

    private static final long MAX_BYTES = 15L * 1024L * 1024L;

    private FileStorage() {
    }

    public static final class StoredFile {
        public final String name;
        public final String path;
        public final String mimeType;
        public final long size;

        public StoredFile(String name, String path, String mimeType, long size) {
            this.name = name;
            this.path = path;
            this.mimeType = mimeType;
            this.size = size;
        }
    }

    public static StoredFile copy(Context context, Uri uri) throws IOException {
        if (uri == null) {
            throw new IOException("Documento no seleccionado");
        }
        String name = queryName(context, uri);
        long declaredSize = querySize(context, uri);
        if (declaredSize > MAX_BYTES) {
            throw new IOException("El archivo supera el límite de 15 MB");
        }

        File directory = new File(context.getFilesDir(), "documents");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("No se pudo preparar el almacenamiento");
        }

        String safeName = sanitizeFileName(name);
        File target = new File(directory, UUID.randomUUID() + "_" + safeName);
        long copied = 0L;
        try (InputStream input = context.getContentResolver().openInputStream(uri);
             FileOutputStream output = new FileOutputStream(target)) {
            if (input == null) {
                throw new IOException("No se pudo abrir el archivo");
            }
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                copied += read;
                if (copied > MAX_BYTES) {
                    throw new IOException("El archivo supera el límite de 15 MB");
                }
                output.write(buffer, 0, read);
            }
            output.flush();
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            target.delete();
            throw e;
        }

        String mime = context.getContentResolver().getType(uri);
        return new StoredFile(name, target.getAbsolutePath(),
                mime == null ? "application/octet-stream" : mime, copied);
    }

    public static void deleteQuietly(String path) {
        if (path == null) {
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        new File(path).delete();
    }

    public static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.0f KB", bytes / 1024.0);
        }
        return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    public static String sanitizeFileName(String name) {
        String value = name == null || name.trim().isEmpty() ? "documento" : name.trim();
        value = value.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (value.length() > 80) {
            value = value.substring(value.length() - 80);
        }
        return value.isEmpty() ? "documento" : value;
    }

    private static String queryName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (index >= 0) {
                    String value = cursor.getString(index);
                    if (value != null && !value.isEmpty()) {
                        return value;
                    }
                }
            }
        }
        String fallback = uri.getLastPathSegment();
        return fallback == null ? "documento" : fallback;
    }

    private static long querySize(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (index >= 0 && !cursor.isNull(index)) {
                    return cursor.getLong(index);
                }
            }
        }
        return 0L;
    }
}
