package deez.nuts.egob.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** Shares and exports generated documents through the system file provider. */
public final class FileShare {

    private FileShare() {
    }

    public static Uri uriFor(Context context, File file) {
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", file);
    }

    public static void share(Context context, File file, String mimeType, String title) {
        Uri uri = uriFor(context, file);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(Intent.createChooser(intent, title));
    }

    public static void open(Context context, File file, String mimeType) {
        Uri uri = uriFor(context, file);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (intent.resolveActivity(context.getPackageManager()) == null) {
            share(context, file, mimeType, "Abrir documento");
            return;
        }
        context.startActivity(intent);
    }

    /** Copies a generated file into the public Downloads collection (or app external storage). */
    public static File saveToDownloads(Context context, File source, String displayName) throws IOException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/eGob");
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (InputStream input = new FileInputStream(source);
                     OutputStream output = resolver.openOutputStream(uri)) {
                    if (output == null) {
                        throw new IOException("No se pudo abrir el destino de descarga.");
                    }
                    copy(input, output);
                }
                return new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "eGob/" + displayName);
            }
        }
        File directory = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "eGob");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IOException("No se pudo preparar la carpeta de descargas.");
        }
        File target = new File(directory, displayName);
        try (InputStream input = new FileInputStream(source);
             OutputStream output = new FileOutputStream(target)) {
            copy(input, output);
        }
        return target;
    }

    private static void copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        output.flush();
    }
}
