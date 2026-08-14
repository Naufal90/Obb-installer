package com.example.installer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String TAG = "OBB_INSTALLER";

    private static final int PICK_OBB = 100;
    private static final int PICK_OBB_FOLDER = 101;

    private TextView status;
    private TextView selectedFileText;
    private TextView packageText;
    private TextView progressText;

    private ProgressBar progressBar;

    private Button selectObbButton;
    private Button processButton;

    private Uri obbUri;
    private String obbName;
    private String packageName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== OBB INSTALLER START ===");

        createUI();

        checkStoragePermission();
    }

    private void createUI() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("OBB Installer");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setTextSize(17);
        status.setPadding(0, 30, 0, 20);

        selectedFileText = new TextView(this);
        selectedFileText.setTextSize(16);
        selectedFileText.setVisibility(TextView.GONE);

        packageText = new TextView(this);
        packageText.setTextSize(16);
        packageText.setVisibility(TextView.GONE);

        progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(ProgressBar.GONE);

        progressText = new TextView(this);
        progressText.setTextSize(16);
        progressText.setGravity(Gravity.CENTER);
        progressText.setVisibility(TextView.GONE);

        selectObbButton = new Button(this);
        selectObbButton.setText("Pilih File OBB");
        selectObbButton.setEnabled(false);

        processButton = new Button(this);
        processButton.setText("Proses Install");
        processButton.setEnabled(false);

        layout.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        layout.addView(status);

        layout.addView(selectedFileText);

        layout.addView(packageText);

        layout.addView(
                progressBar,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        50
                )
        );

        layout.addView(progressText);

        layout.addView(selectObbButton);

        layout.addView(processButton);

        setContentView(layout);

        selectObbButton.setOnClickListener(
                v -> selectObb()
        );

        processButton.setOnClickListener(
                v -> startProcess()
        );
    }

    // =========================================================
    // PERMISSION
    // =========================================================

    private boolean hasStoragePermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }

        return true;
    }

    private void checkStoragePermission() {

        Log.d(TAG, "Checking storage permission");

        if (hasStoragePermission()) {

            Log.d(TAG, "Storage permission: GRANTED");

            showPermissionGranted();

        } else {

            Log.d(TAG, "Storage permission: NOT GRANTED");

            showPermissionDialog();
        }
    }

    private void showPermissionDialog() {

        status.setText(
                "Akses penyimpanan diperlukan."
        );

        selectObbButton.setEnabled(false);

        new AlertDialog.Builder(this)
                .setTitle("Izin Penyimpanan Diperlukan")
                .setMessage(
                        "OBB Installer membutuhkan akses " +
                        "penyimpanan untuk mengelola file OBB."
                )
                .setCancelable(false)
                .setNegativeButton(
                        "Nanti",
                        null
                )
                .setPositiveButton(
                        "Izinkan",
                        (dialog, which) ->
                                openStorageSettings()
                )
                .show();
    }

    private void openStorageSettings() {

        Log.d(
                TAG,
                "Opening All Files Access settings"
        );

        try {

            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            );

            intent.setData(
                    Uri.parse(
                            "package:" + getPackageName()
                    )
            );

            startActivity(intent);

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "App-specific settings failed",
                    e
            );

            try {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                );

                startActivity(intent);

            } catch (Exception ex) {

                Log.e(
                        TAG,
                        "Unable to open storage settings",
                        ex
                );
            }
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (status == null) {
            return;
        }

        if (hasStoragePermission()) {

            showPermissionGranted();

        } else {

            status.setText(
                    "Akses penyimpanan belum diberikan."
            );

            selectObbButton.setEnabled(false);
        }
    }

    private void showPermissionGranted() {

        Log.d(
                TAG,
                "Storage permission granted"
        );

        status.setText(
                "✓ Akses diberikan.\n" +
                "Silakan pilih file OBB."
        );

        selectObbButton.setEnabled(true);
    }

    // =========================================================
    // PILIH OBB
    // =========================================================

    private void selectObb() {

        Log.d(
                TAG,
                "Opening OBB picker"
        );

        Intent intent = new Intent(
                Intent.ACTION_OPEN_DOCUMENT
        );

        intent.setType("*/*");

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

            Log.d(
                    TAG,
                    "Picker cancelled"
            );

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

            selectedFileText.setVisibility(
                    TextView.GONE
            );

            packageText.setVisibility(
                    TextView.GONE
            );

            processButton.setEnabled(false);

            status.setText(
                    "File yang dipilih bukan file OBB."
            );

            Log.e(
                    TAG,
                    "Selected file is not OBB"
            );

            return;
        }

        packageName =
                extractPackageName(obbName);

        if (packageName == null) {

            processButton.setEnabled(false);

            status.setText(
                    "Package tidak dapat dideteksi."
            );

            Log.e(
                    TAG,
                    "Unable to detect package"
            );

            return;
        }

        // Tampilkan file yang dipilih
        selectedFileText.setVisibility(
                TextView.VISIBLE
        );

        selectedFileText.setText(
                "📦 File OBB:\n" +
                obbName
        );

        // Tampilkan package
        packageText.setVisibility(
                TextView.VISIBLE
        );

        packageText.setText(
                "📁 Package:\n" +
                packageName
        );

        status.setText(
                "File OBB berhasil dipilih.\n" +
                "Tekan \"Proses Install\" untuk melanjutkan."
        );

        processButton.setEnabled(true);

        progressBar.setVisibility(
                ProgressBar.GONE
        );

        progressText.setVisibility(
                TextView.GONE
        );

        Log.d(
                TAG,
                "OBB selected successfully"
        );

        Log.d(
                TAG,
                "Waiting for user to press Process"
        );
    }

    // =========================================================
    // PROSES INSTALL
    // =========================================================

    private void startProcess() {

        if (
                obbUri == null ||
                obbName == null ||
                packageName == null
        ) {

            status.setText(
                    "File OBB belum dipilih."
            );

            return;
        }

        Log.d(
                TAG,
                "=== USER PRESSED PROCESS ==="
        );

        processButton.setEnabled(false);
        selectObbButton.setEnabled(false);

        progressBar.setVisibility(
                ProgressBar.VISIBLE
        );

        progressText.setVisibility(
                TextView.VISIBLE
        );

        progressBar.setProgress(0);

        progressText.setText(
                "0%"
        );

        status.setText(
                "Pilih folder Android/obb..."
        );

        selectObbFolder();
    }

    // =========================================================
    // PILIH ANDROID/OBB
    // =========================================================

    private void selectObbFolder() {

        Log.d(
                TAG,
                "Opening Android/obb folder picker"
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

    private void handleObbFolder(
            Uri rootUri
    ) {

        Log.d(
                TAG,
                "Selected OBB root = " +
                rootUri
        );

        int flags =
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

        try {

            getContentResolver()
                    .takePersistableUriPermission(
                            rootUri,
                            flags
                    );

            Log.d(
                    TAG,
                    "Persistable permission granted"
            );

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Persistable permission failed",
                    e
            );
        }

        status.setText(
                "Mempersiapkan folder package..."
        );

        installObb(rootUri);
    }

    // =========================================================
    // INSTALL
    // =========================================================

    private void installObb(
            Uri rootUri
    ) {

        Log.d(
                TAG,
                "=== INSTALL START ==="
        );

        Log.d(
                TAG,
                "Package = " +
                packageName
        );

        new Thread(() -> {

            try {

                runOnUiThread(() ->
                        status.setText(
                                "Mencari folder package..."
                        )
                );

                Uri packageFolder =
                        findOrCreateFolder(
                                rootUri,
                                packageName
                        );

                if (packageFolder == null) {

                    throw new Exception(
                            "Folder package gagal dibuat."
                    );
                }

                Log.d(
                        TAG,
                        "Package folder = " +
                        packageFolder
                );

                runOnUiThread(() ->
                        status.setText(
                                "Folder package siap.\n" +
                                "Menyalin OBB..."
                        )
                );

                copyObb(packageFolder);

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "INSTALL FAILED",
                        e
                );

                runOnUiThread(() -> {

                    progressBar.setProgress(0);

                    progressText.setText(
                            "Gagal"
                    );

                    status.setText(
                            "❌ GAGAL MENYALIN OBB\n\n" +
                            e.getClass()
                                    .getSimpleName() +
                            "\n\n" +
                            e.getMessage()
                    );

                    processButton.setEnabled(true);
                    selectObbButton.setEnabled(true);
                });
            }

        }).start();
    }

    // =========================================================
    // CREATE PACKAGE FOLDER
    // =========================================================

    private Uri findOrCreateFolder(
            Uri parentUri,
            String folderName
    ) throws Exception {

        Log.d(
                TAG,
                "Searching package folder: " +
                folderName
        );

        String parentDocumentId =
                DocumentsContract
                        .getTreeDocumentId(
                                parentUri
                        );

        Uri childrenUri =
                DocumentsContract
                        .buildChildDocumentsUriUsingTree(
                                parentUri,
                                parentDocumentId
                        );

        android.database.Cursor cursor =
                getContentResolver().query(
                        childrenUri,
                        new String[]{
                                DocumentsContract.Document
                                        .COLUMN_DOCUMENT_ID,

                                DocumentsContract.Document
                                        .COLUMN_DISPLAY_NAME,

                                DocumentsContract.Document
                                        .COLUMN_MIME_TYPE
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
                            DocumentsContract.Document
                                    .MIME_TYPE_DIR
                                    .equals(mime)
                    ) {

                        Log.d(
                                TAG,
                                "Existing package folder found"
                        );

                        return DocumentsContract
                                .buildDocumentUriUsingTree(
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
                "Creating package folder: " +
                folderName
        );

        Uri folder =
                DocumentsContract.createDocument(
                        getContentResolver(),
                        parentUri,
                        DocumentsContract.Document
                                .MIME_TYPE_DIR,
                        folderName
                );

        if (folder == null) {

            throw new Exception(
                    "Android menolak pembuatan folder package."
            );
        }

        Log.d(
                TAG,
                "Package folder created"
        );

        return folder;
    }

    // =========================================================
    // COPY OBB + PROGRESS
    // =========================================================

    private void copyObb(
            Uri packageFolder
    ) throws Exception {

        Log.d(
                TAG,
                "=== COPY START ==="
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
                    "Android menolak pembuatan file OBB."
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

        long fileSize =
                getFileSize(obbUri);

        Log.d(
                TAG,
                "Source size = " +
                fileSize
        );

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

                int progress = 0;

                if (fileSize > 0) {

                    progress =
                            (int)
                            ((total * 100L)
                                    / fileSize);

                    if (progress > 100) {
                        progress = 100;
                    }
                }

                final int finalProgress =
                        progress;

                final long finalTotal =
                        total;

                runOnUiThread(() -> {

                    progressBar.setProgress(
                            finalProgress
                    );

                    progressText.setText(
                            finalProgress +
                            "%  (" +
                            formatBytes(
                                    finalTotal
                            ) +
                            ")"
                    );

                    status.setText(
                            "Menyalin OBB..."
                    );
                });

                Log.d(
                        TAG,
                        "COPY " +
                        progress +
                        "% - " +
                        total +
                        " bytes"
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
                "Total copied = " +
                total
        );

        runOnUiThread(() -> {

            progressBar.setProgress(100);

            progressText.setText(
                    "100% - Selesai"
            );

            status.setText(
                    "✓ INSTALL BERHASIL\n\n" +
                    "File:\n" +
                    obbName +
                    "\n\n" +
                    "Package:\n" +
                    packageName +
                    "\n\n" +
                    "Lokasi:\n" +
                    "Android/obb/" +
                    packageName
            );

            processButton.setEnabled(false);
            selectObbButton.setEnabled(true);
        });

        Log.d(
                TAG,
                "=== INSTALL SUCCESS ==="
        );
    }

    // =========================================================
    // GET FILE SIZE
    // =========================================================

    private long getFileSize(
            Uri uri
    ) {

        android.database.Cursor cursor =
                getContentResolver().query(
                        uri,
                        new String[]{
                                DocumentsContract.Document
                                        .COLUMN_SIZE
                        },
                        null,
                        null,
                        null
                );

        if (cursor != null) {

            try {

                if (cursor.moveToFirst()) {

                    if (!cursor.isNull(0)) {

                        return cursor.getLong(0);
                    }
                }

            } finally {

                cursor.close();
            }
        }

        return -1;
    }

    // =========================================================
    // PACKAGE DETECTION
    // =========================================================

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

        if (
                !parts[0].equalsIgnoreCase("main") &&
                !parts[0].equalsIgnoreCase("patch")
        ) {

            return null;
        }

        StringBuilder result =
                new StringBuilder();

        for (
                int i = 2;
                i < parts.length;
                i++
        ) {

            if (result.length() > 0) {
                result.append(".");
            }

            result.append(parts[i]);
        }

        String resultString =
                result.toString();

        Log.d(
                TAG,
                "Detected package = " +
                resultString
        );

        return resultString;
    }

    // =========================================================
    // FILE NAME
    // =========================================================

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

    // =========================================================
    // FORMAT SIZE
    // =========================================================

    private String formatBytes(
            long bytes
    ) {

        if (bytes < 1024) {
            return bytes + " B";
        }

        if (bytes < 1024 * 1024) {

            return String.format(
                    Locale.US,
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (
                bytes <
                1024L * 1024L * 1024L
        ) {

            return String.format(
                    Locale.US,
                    "%.2f MB",
                    bytes /
                    (1024.0 * 1024.0)
            );
        }

        return String.format(
                Locale.US,
                "%.2f GB",
                bytes /
                (1024.0 * 1024.0 * 1024.0)
        );
    }
}
