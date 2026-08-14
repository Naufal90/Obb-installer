package com.example.installer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final String TAG = "OBB_INSTALLER";

    private static final int PICK_OBB = 100;
    private static final int PICK_OBB_FOLDER = 101;

    private Uri obbUri;
    private String obbName;
    private String packageName;

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== APPLICATION START ===");

        createUI();
    }

    private void createUI() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("OBB Installer");
        title.setTextSize(28);

        TextView info = new TextView(this);
        info.setText(
                "Pilih file OBB.\n" +
                "Installer akan mendeteksi package secara otomatis."
        );
        info.setTextSize(16);

        Button selectButton = new Button(this);
        selectButton.setText("Pilih File OBB");

        status = new TextView(this);
        status.setTextSize(16);
        status.setText("Siap.");

        layout.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        layout.addView(info);
        layout.addView(selectButton);
        layout.addView(status);

        setContentView(layout);

        selectButton.setOnClickListener(v -> selectObb());
    }

    private void selectObb() {

        Log.d(TAG, "Membuka file picker OBB");

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("*/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);

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
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        Log.d(
                TAG,
                "onActivityResult request=" +
                        requestCode +
                        " result=" +
                        resultCode
        );

        if (
                resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null
        ) {
            Log.d(TAG, "User membatalkan picker");
            return;
        }

        Uri uri = data.getData();

        if (requestCode == PICK_OBB) {

            handleObb(uri);

        } else if (requestCode == PICK_OBB_FOLDER) {

            handleObbFolder(uri);
        }
    }

    private void handleObb(Uri uri) {

        obbUri = uri;

        Log.d(
                TAG,
                "OBB URI = " + uri
        );

        obbName = getFileName(uri);

        Log.d(
                TAG,
                "OBB NAME = " + obbName
        );

        if (
                obbName == null ||
                !obbName.toLowerCase().endsWith(".obb")
        ) {

            status.setText(
                    "File yang dipilih bukan file OBB."
            );

            Log.e(
                    TAG,
                    "File bukan OBB"
            );

            return;
        }

        packageName =
                extractPackageName(obbName);

        if (packageName == null) {

            status.setText(
                    "Package tidak dapat dideteksi."
            );

            Log.e(
                    TAG,
                    "Gagal mendeteksi package"
            );

            return;
        }

        Log.d(
                TAG,
                "PACKAGE = " + packageName
        );

        status.setText(
                "OBB:\n" +
                        obbName +
                        "\n\nPackage:\n" +
                        packageName +
                        "\n\nPilih folder Android/obb."
        );

        selectObbFolder();
    }

    private void selectObbFolder() {

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

    private void handleObbFolder(Uri treeUri) {

        Log.d(
                TAG,
                "OBB ROOT URI = " + treeUri
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
                    "Persistable permission gagal",
                    e
            );
        }

        status.setText(
                "Menyiapkan folder package..."
        );

        installObb(
                treeUri
        );
    }

    private void installObb(Uri rootUri) {

        Log.d(
                TAG,
                "=== INSTALL START ==="
        );

        Log.d(
                TAG,
                "Root = " + rootUri
        );

        Log.d(
                TAG,
                "Package = " + packageName
        );

        try {

            Uri packageFolder =
                    findOrCreateFolder(
                            rootUri,
                            packageName
                    );

            if (packageFolder == null) {

                throw new Exception(
                        "Tidak dapat membuat folder package."
                );
            }

            Log.d(
                    TAG,
                    "Package folder = " +
                            packageFolder
            );

            copyObb(
                    packageFolder
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "INSTALL FAILED",
                    e
            );

            status.setText(
                    "GAGAL\n\n" +
                            e.getClass()
                                    .getSimpleName() +
                            "\n\n" +
                            e.getMessage()
            );
        }
    }

    private Uri findOrCreateFolder(
            Uri parentUri,
            String folderName
    ) throws Exception {

        Log.d(
                TAG,
                "Mencari folder: " +
                        folderName
        );

        Uri childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                        parentUri,
                        DocumentsContract.getTreeDocumentId(
                                parentUri
                        )
                );

        android.database.Cursor cursor =
                getContentResolver().query(
                        childrenUri,
                        new String[]{
                                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                                DocumentsContract.Document.COLUMN_MIME_TYPE
                        },
                        null,
                        null,
                        null
                );

        if (cursor != null) {

            try {

                while (cursor.moveToNext()) {

                    String documentId =
                            cursor.getString(0);

                    String name =
                            cursor.getString(1);

                    String mime =
                            cursor.getString(2);

                    if (
                            folderName.equals(name) &&
                            DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)
                    ) {

                        Log.d(
                                TAG,
                                "Folder ditemukan"
                        );

                        return DocumentsContract.buildDocumentUriUsingTree(
                                parentUri,
                                documentId
                        );
                    }
                }

            } finally {

                cursor.close();
            }
        }

        Log.d(
                TAG,
                "Folder belum ada, membuat..."
        );

        Uri newFolder =
                DocumentsContract.createDocument(
                        getContentResolver(),
                        parentUri,
                        DocumentsContract.Document.MIME_TYPE_DIR,
                        folderName
                );

        if (newFolder == null) {

            throw new Exception(
                    "createDocument() mengembalikan null."
            );
        }

        Log.d(
                TAG,
                "Folder berhasil dibuat"
        );

        return newFolder;
    }

    private void copyObb(
            Uri packageFolder
    ) throws Exception {

        Log.d(
                TAG,
                "=== COPY OBB START ==="
        );

        status.setText(
                "Menyalin OBB...\n\n" +
                        obbName
        );

        Uri destination =
                DocumentsContract.createDocument(
                        getContentResolver(),
                        packageFolder,
                        "application/octet-stream",
                        obbName
                );

        if (destination == null) {

            throw new Exception(
                    "Tidak dapat membuat file OBB tujuan."
            );
        }

        Log.d(
                TAG,
                "Destination = " +
                        destination
        );

        InputStream input =
                getContentResolver()
                        .openInputStream(
                                obbUri
                        );

        if (input == null) {

            throw new Exception(
                    "Tidak dapat membuka OBB sumber."
            );
        }

        OutputStream output =
                getContentResolver()
                        .openOutputStream(
                                destination
                        );

        if (output == null) {

            input.close();

            throw new Exception(
                    "Tidak dapat membuka OBB tujuan."
            );
        }

        byte[] buffer =
                new byte[1024 * 1024];

        long total = 0;

        int count;

        try {

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
                                        formatBytes(
                                                progress
                                        )
                        )
                );
            }

            output.flush();

        } finally {

            try {
                input.close();
            } catch (Exception ignored) {
            }

            try {
                output.close();
            } catch (Exception ignored) {
            }
        }

        Log.d(
                TAG,
                "COPY FINISHED"
        );

        Log.d(
                TAG,
                "Bytes copied = " +
                        total
        );

        status.setText(
                "BERHASIL!\n\n" +
                        "File:\n" +
                        obbName +
                        "\n\nPackage:\n" +
                        packageName +
                        "\n\nUkuran:\n" +
                        formatBytes(total)
        );

        Log.d(
                TAG,
                "=== INSTALL SUCCESS ==="
        );
    }

    private String extractPackageName(
            String fileName
    ) {

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

        String[] parts =
                name.split("\\.");

        if (parts.length < 3) {
            return null;
        }

        /*
         * Contoh:
         *
         * main.547389636.com.and.games505.portal_knights.obb
         *
         * parts:
         *
         * main
         * 547389636
         * com
         * and
         * games505
         * portal_knights
         *
         * Package dimulai dari parts[2].
         */

        if (
                !parts[0].equalsIgnoreCase("main") &&
                !parts[0].equalsIgnoreCase("patch")
        ) {

            return null;
        }

        StringBuilder packageName =
                new StringBuilder();

        for (
                int i = 2;
                i < parts.length;
                i++
        ) {

            if (packageName.length() > 0) {
                packageName.append(".");
            }

            packageName.append(
                    parts[i]
            );
        }

        String result =
                packageName.toString();

        if (result.isEmpty()) {
            return null;
        }

        Log.d(
                TAG,
                "Detected package = " +
                        result
        );

        return result;
    }

    private String getFileName(
            Uri uri
    ) {

        android.database.Cursor cursor =
                getContentResolver().query(
                        uri,
                        new String[]{
                                "_display_name"
                        },
                        null,
                        null,
                        null
                );

        if (cursor != null) {

            try {

                if (cursor.moveToFirst()) {

                    return cursor.getString(0);
                }

            } finally {

                cursor.close();
            }
        }

        return null;
    }

    private String formatBytes(
            long bytes
    ) {

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
}
