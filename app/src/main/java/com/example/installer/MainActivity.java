package com.example.installer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.documentfile.provider.DocumentFile;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final String TAG = "OBB_INSTALLER";

    private static final int PICK_OBB = 100;
    private static final int PICK_OBB_FOLDER = 101;

    private TextView status;
    private Button selectObb;

    private Uri selectedObbUri;
    private String selectedObbName;
    private String detectedPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== APPLICATION START ===");

        createInterface();
    }

    private void createInterface() {

        LinearLayout layout = new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                40,
                40,
                40,
                40
        );

        layout.setGravity(
                Gravity.CENTER_HORIZONTAL
        );

        TextView title = new TextView(this);

        title.setText(
                "OBB Installer"
        );

        title.setTextSize(28);

        title.setGravity(
                Gravity.CENTER
        );

        TextView description = new TextView(this);

        description.setText(
                "Pilih file OBB.\n\n" +
                "Installer akan mendeteksi package " +
                "dan membuat folder package secara otomatis."
        );

        description.setTextSize(16);

        selectObb = new Button(this);

        selectObb.setText(
                "Pilih File OBB"
        );

        status = new TextView(this);

        status.setTextSize(16);

        status.setText(
                "Belum ada file OBB."
        );

        layout.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        layout.addView(
                description
        );

        layout.addView(
                selectObb
        );

        layout.addView(
                status
        );

        setContentView(layout);

        selectObb.setOnClickListener(
                v -> chooseObb()
        );
    }

    private void chooseObb() {

        Log.d(
                TAG,
                "Membuka file picker OBB"
        );

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType(
                "*/*"
        );

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_OBB
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        Log.d(
                TAG,
                "onActivityResult: " +
                        requestCode +
                        " result=" +
                        resultCode
        );

        if (
                resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null
        ) {
            return;
        }

        Uri uri =
                data.getData();

        if (requestCode == PICK_OBB) {

            handleObbSelected(
                    uri
            );

        } else if (requestCode == PICK_OBB_FOLDER) {

            handleFolderSelected(
                    uri
            );
        }
    }

    private void handleObbSelected(
            Uri uri) {

        selectedObbUri = uri;

        selectedObbName =
                getFileName(uri);

        Log.d(
                TAG,
                "OBB URI: " +
                        uri
        );

        Log.d(
                TAG,
                "OBB name: " +
                        selectedObbName
        );

        if (
                selectedObbName == null ||
                !selectedObbName
                        .toLowerCase()
                        .endsWith(".obb")
        ) {

            status.setText(
                    "File yang dipilih bukan OBB."
            );

            return;
        }

        detectedPackage =
                extractPackageName(
                        selectedObbName
                );

        if (detectedPackage == null) {

            status.setText(
                    "Package tidak dapat " +
                    "dideteksi dari nama OBB."
            );

            return;
        }

        Log.d(
                TAG,
                "Package detected: " +
                        detectedPackage
        );

        status.setText(
                "OBB:\n" +
                        selectedObbName +
                        "\n\nPackage:\n" +
                        detectedPackage +
                        "\n\n" +
                        "Sekarang pilih folder Android/obb."
        );

        chooseObbFolder();
    }

    private void chooseObbFolder() {

        Log.d(
                TAG,
                "Membuka folder picker"
        );

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT_TREE
                );

        intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(
                intent,
                PICK_OBB_FOLDER
        );
    }

    private void handleFolderSelected(
            Uri treeUri) {

        Log.d(
                TAG,
                "Folder URI: " +
                        treeUri
        );

        int flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

        try {

            getContentResolver()
                    .takePersistableUriPermission(
                            treeUri,
                            flags
                    );

            Log.d(
                    TAG,
                    "Persistable permission berhasil"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Gagal mengambil persistable permission",
                    e
            );
        }

        DocumentFile root =
                DocumentFile.fromTreeUri(
                        this,
                        treeUri
                );

        if (root == null) {

            status.setText(
                    "Folder tidak dapat dibuka."
            );

            return;
        }

        Log.d(
                TAG,
                "Root folder berhasil dibuka"
        );

        installObb(
                root
        );
    }

    private void installObb(
            DocumentFile root) {

        if (
                selectedObbUri == null ||
                selectedObbName == null ||
                detectedPackage == null
        ) {

            status.setText(
                    "Data OBB tidak lengkap."
            );

            return;
        }

        Log.d(
                TAG,
                "Membuat folder package: " +
                        detectedPackage
        );

        DocumentFile packageFolder =
                root.findFile(
                        detectedPackage
                );

        if (
                packageFolder == null ||
                !packageFolder.isDirectory()
        ) {

            packageFolder =
                    root.createDirectory(
                            detectedPackage
                    );
        }

        if (packageFolder == null) {

            Log.e(
                    TAG,
                    "Gagal membuat folder package"
            );

            status.setText(
                    "Gagal membuat folder:\n" +
                            detectedPackage
            );

            return;
        }

        Log.d(
                TAG,
                "Folder package siap"
        );

        DocumentFile destination =
                packageFolder.findFile(
                        selectedObbName
                );

        if (destination != null) {

            Log.d(
                    TAG,
                    "File tujuan sudah ada. Menghapus."
            );

            destination.delete();
        }

        destination =
                packageFolder.createFile(
                        "application/octet-stream",
                        selectedObbName
                );

        if (destination == null) {

            Log.e(
                    TAG,
                    "Gagal membuat file tujuan"
            );

            status.setText(
                    "Gagal membuat file OBB."
            );

            return;
        }

        Log.d(
                TAG,
                "File tujuan dibuat: " +
                        destination.getUri()
        );

        status.setText(
                "Menyalin OBB...\n\n" +
                        selectedObbName
        );

        DocumentFile finalDestination =
                destination;

        new Thread(() -> {

            try {

                copyFile(
                        selectedObbUri,
                        finalDestination
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "COPY FAILED",
                        e
                );

                runOnUiThread(() ->
                        status.setText(
                                "GAGAL MENYALIN\n\n" +
                                        e.getClass()
                                                .getSimpleName() +
                                        "\n\n" +
                                        e.getMessage()
                        )
                );
            }

        }).start();
    }

    private void copyFile(
            Uri sourceUri,
            DocumentFile destination)
            throws Exception {

        Log.d(
                TAG,
                "=== COPY START ==="
        );

        InputStream input =
                getContentResolver()
                        .openInputStream(
                                sourceUri
                        );

        if (input == null) {

            throw new Exception(
                    "InputStream tidak tersedia."
            );
        }

        OutputStream output =
                getContentResolver()
                        .openOutputStream(
                                destination.getUri()
                        );

        if (output == null) {

            input.close();

            throw new Exception(
                    "OutputStream tidak tersedia."
            );
        }

        byte[] buffer =
                new byte[
                        1024 * 1024
                ];

        long total = 0;

        int count;

        while (
                (count =
                        input.read(buffer)) != -1
        ) {

            output.write(
                    buffer,
                    0,
                    count
            );

            total += count;

            final long progress =
                    total;

            runOnUiThread(() ->
                    status.setText(
                            "Menyalin OBB...\n\n" +
                                    "Tersalin: " +
                                    formatBytes(
                                            progress
                                    )
                    )
            );
        }

        output.flush();

        output.close();
        input.close();

        Log.d(
                TAG,
                "COPY FINISHED"
        );

        Log.d(
                TAG,
                "Total bytes: " +
                        total
        );

        long finalSize =
                destination.length();

        Log.d(
                TAG,
                "Destination size: " +
                        finalSize
        );

        if (
                finalSize > 0 &&
                finalSize != total
        ) {

            throw new Exception(
                    "Ukuran file tidak cocok.\n" +
                            "Source: " +
                            total +
                            "\nDestination: " +
                            finalSize
            );
        }

        Log.d(
                TAG,
                "=== COPY SUCCESS ==="
        );

        runOnUiThread(() ->
                status.setText(
                        "BERHASIL!\n\n" +
                                "File:\n" +
                                selectedObbName +
                                "\n\nPackage:\n" +
                                detectedPackage +
                                "\n\n" +
                                "Ukuran:\n" +
                                formatBytes(total)
                )
        );
    }

    private String extractPackageName(
            String fileName) {

        String name =
                fileName;

        if (
                name.toLowerCase()
                        .endsWith(".obb")
        ) {

            name =
                    name.substring(
                            0,
                            name.length() - 4
                    );
        }

        int firstDot =
                name.indexOf('.');

        if (firstDot < 0) {
            return null;
        }

        int secondDot =
                name.indexOf(
                        '.',
                        firstDot + 1
                );

        if (secondDot < 0) {
            return null;
        }

        String packageName =
                name.substring(
                        secondDot + 1
                );

        if (packageName.isEmpty()) {
            return null;
        }

        Log.d(
                TAG,
                "Extracted package: " +
                        packageName
        );

        return packageName;
    }

    private String getFileName(
            Uri uri) {

        String path =
                uri.getPath();

        if (path == null) {
            return null;
        }

        int separator =
                path.lastIndexOf('/');

        if (
                separator >= 0 &&
                separator + 1 < path.length()
        ) {

            return path.substring(
                    separator + 1
            );
        }

        return path;
    }

    private String formatBytes(
            long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (
                bytes <
                        1024L *
                        1024L *
                        1024L
        ) {

            return String.format(
                    "%.2f MB",
                    bytes /
                            (1024.0 * 1024.0)
            );
        }

        return String.format(
                "%.2f GB",
                bytes /
                        (1024.0 *
                         1024.0 *
                         1024.0)
        );
    }
} fileName +
                "\n\n" +
                "Package:\n" +
                packageName +
                "\n\n" +
                "Tujuan:\n" +
                destination.getAbsolutePath() +
                "\n\n" +
                "Menyalin..."
        );

        copyObb(
                uri,
                packageDirectory,
                destination
        );
    }

    private String extractPackageName(String fileName) {

        String name = fileName;

        if (name.toLowerCase().endsWith(".obb")) {

            name = name.substring(
                    0,
                    name.length() - 4
            );
        }

        Log.d(
                TAG,
                "Nama tanpa extension: " +
                        name
        );

        /*
         * Format OBB standar:
         *
         * main.<versionCode>.<package>.obb
         * patch.<versionCode>.<package>.obb
         *
         * Contoh:
         *
         * main.123456.com.game.example.obb
         *
         * Package:
         *
         * com.game.example
         */

        int firstDot = name.indexOf('.');

        if (firstDot < 0) {
            return null;
        }

        int secondDot = name.indexOf(
                '.',
                firstDot + 1
        );

        if (secondDot < 0) {
            return null;
        }

        String prefix =
                name.substring(
                        0,
                        firstDot
                );

        if (!prefix.equalsIgnoreCase("main") &&
                !prefix.equalsIgnoreCase("patch")) {

            Log.w(
                    TAG,
                    "Prefix OBB tidak standar: " +
                            prefix
            );
        }

        String packageName =
                name.substring(
                        secondDot + 1
                );

        if (packageName.isEmpty()) {
            return null;
        }

        /*
         * Validasi sederhana package.
         */
        if (!packageName.matches(
                "[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)+"
        )) {

            Log.e(
                    TAG,
                    "Format package mencurigakan: " +
                            packageName
            );

            return null;
        }

        return packageName;
    }

    private void copyObb(
            Uri sourceUri,
            File packageDirectory,
            File destination) {

        Log.d(TAG, "=== COPY START ===");

        new Thread(() -> {

            try {

                if (!packageDirectory.exists()) {

                    Log.d(
                            TAG,
                            "Folder package belum ada, membuat folder"
                    );

                    boolean created =
                            packageDirectory.mkdirs();

                    Log.d(
                            TAG,
                            "mkdirs result = " +
                                    created
                    );
                }

                if (!packageDirectory.exists()) {

                    throw new Exception(
                            "Gagal membuat folder:\n" +
                                    packageDirectory
                                            .getAbsolutePath()
                    );
                }

                Log.d(
                        TAG,
                        "Folder package tersedia: " +
                                packageDirectory
                                        .getAbsolutePath()
                );

                InputStream input =
                        getContentResolver()
                                .openInputStream(
                                        sourceUri
                                );

                if (input == null) {

                    throw new Exception(
                            "Tidak dapat membuka file sumber."
                    );
                }

                Log.d(
                        TAG,
                        "InputStream berhasil dibuka"
                );

                OutputStream output =
                        new FileOutputStream(
                                destination
                        );

                Log.d(
                        TAG,
                        "OutputStream berhasil dibuka"
                );

                byte[] buffer =
                        new byte[1024 * 1024];

                long total = 0;

                int bytesRead;

                while ((bytesRead =
                        input.read(buffer)) != -1) {

                    output.write(
                            buffer,
                            0,
                            bytesRead
                    );

                    total += bytesRead;

                    final long progress =
                            total;

                    runOnUiThread(() ->
                            status.setText(
                                    "Menyalin OBB...\n\n" +
                                    "Ukuran tersalin: " +
                                    formatBytes(progress)
                            )
                    );
                }

                output.flush();

                output.close();
                input.close();

                Log.d(
                        TAG,
                        "Copy selesai"
                );

                Log.d(
                        TAG,
                        "Total bytes: " +
                                total
                );

                long destinationSize =
                        destination.length();

                Log.d(
                        TAG,
                        "Destination size: " +
                                destinationSize
                );

                if (destinationSize != total) {

                    throw new Exception(
                            "Ukuran file hasil tidak cocok."
                    );
                }

                Log.d(
                        TAG,
                        "=== COPY SUCCESS ==="
                );

                runOnUiThread(() ->
                        status.setText(
                                "BERHASIL\n\n" +
                                "File:\n" +
                                destination.getName() +
                                "\n\n" +
                                "Package:\n" +
                                packageDirectory.getName() +
                                "\n\n" +
                                "Lokasi:\n" +
                                destination
                                        .getAbsolutePath() +
                                "\n\n" +
                                "Ukuran:\n" +
                                formatBytes(
                                        destinationSize
                                )
                        )
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "=== COPY FAILED ===",
                        e
                );

                runOnUiThread(() ->
                        status.setText(
                                "GAGAL MENYALIN OBB\n\n" +
                                e.getClass()
                                        .getSimpleName() +
                                "\n\n" +
                                e.getMessage()
                        )
                );
            }

        }).start();
    }

    private String formatBytes(long bytes) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {
            return String.format(
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {
            return String.format(
                    "%.2f MB",
                    bytes /
                            (1024.0 * 1024.0)
            );
        }

        return String.format(
                "%.2f GB",
                bytes /
                        (1024.0 * 1024.0 * 1024.0)
        );
    }

    private String getFileName(Uri uri) {

        String path = uri.getPath();

        if (path == null) {
            return null;
        }

        int separator =
                path.lastIndexOf('/');

        if (separator >= 0 &&
                separator + 1 < path.length()) {

            return path.substring(
                    separator + 1
            );
        }

        return path;
    }

    @Override
    protected void onResume() {

        super.onResume();

        Log.d(
                TAG,
                "onResume - checking storage access"
        );

        if (status != null) {
            updatePermissionStatus();
        }
    }
}
