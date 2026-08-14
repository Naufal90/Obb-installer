package com.example.installer;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String TAG = "OBB_INSTALLER";

    private static final int PICK_OBB = 1001;
    private static final int REQUEST_STORAGE = 1002;

    private TextView statusText;
    private TextView selectedFileText;
    private TextView packageText;
    private TextView progressText;

    private ProgressBar progressBar;

    private Button permissionButton;
    private Button selectObbButton;
    private Button processButton;

    private Uri selectedObbUri;
    private String selectedObbName;
    private String detectedPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "================================");
        Log.d(TAG, "OBB INSTALLER START");
        Log.d(TAG, "Android SDK: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Device: " + Build.MANUFACTURER + " " + Build.MODEL);
        Log.d(TAG, "================================");

        createInterface();

        updatePermissionState();
    }

    // ============================================================
    // UI
    // ============================================================

    private void createInterface() {

        LinearLayout root = new LinearLayout(this);

        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);

        title.setText("OBB Installer");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        root.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        statusText = new TextView(this);

        statusText.setTextSize(17);
        statusText.setPadding(0, 30, 0, 20);

        root.addView(
                statusText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        permissionButton = new Button(this);

        permissionButton.setText(
                "Berikan Akses Semua File"
        );

        root.addView(
                permissionButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        selectedFileText = new TextView(this);

        selectedFileText.setTextSize(16);
        selectedFileText.setPadding(0, 20, 0, 10);
        selectedFileText.setVisibility(TextView.GONE);

        root.addView(
                selectedFileText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        packageText = new TextView(this);

        packageText.setTextSize(16);
        packageText.setPadding(0, 10, 0, 20);
        packageText.setVisibility(TextView.GONE);

        root.addView(
                packageText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        selectObbButton = new Button(this);

        selectObbButton.setText(
                "Pilih File OBB"
        );

        root.addView(
                selectObbButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        processButton = new Button(this);

        processButton.setText(
                "Proses Install"
        );

        processButton.setEnabled(false);

        root.addView(
                processButton,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        progressBar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(ProgressBar.GONE);

        LinearLayout.LayoutParams progressParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        50
                );

        progressParams.topMargin = 30;

        root.addView(
                progressBar,
                progressParams
        );

        progressText = new TextView(this);

        progressText.setTextSize(16);
        progressText.setGravity(Gravity.CENTER);
        progressText.setVisibility(TextView.GONE);

        root.addView(
                progressText,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(root);

        permissionButton.setOnClickListener(
                v -> requestStoragePermission()
        );

        selectObbButton.setOnClickListener(
                v -> openObbPicker()
        );

        processButton.setOnClickListener(
                v -> startInstall()
        );
    }

    // ============================================================
    // STORAGE PERMISSION
    // ============================================================

    private boolean hasStoragePermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            boolean granted =
                    Environment.isExternalStorageManager();

            Log.d(
                    TAG,
                    "MANAGE_EXTERNAL_STORAGE = " + granted
            );

            return granted;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            boolean granted =
                    checkSelfPermission(
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED;

            Log.d(
                    TAG,
                    "WRITE_EXTERNAL_STORAGE = " + granted
            );

            return granted;
        }

        return true;
    }

    private void updatePermissionState() {

        if (hasStoragePermission()) {

            Log.d(
                    TAG,
                    "Storage permission GRANTED"
            );

            statusText.setText(
                    "✓ Akses penyimpanan sudah diberikan.\n" +
                    "Pilih file OBB yang ingin dipasang."
            );

            permissionButton.setText(
                    "Akses Penyimpanan: AKTIF"
            );

            permissionButton.setEnabled(false);

            selectObbButton.setEnabled(true);

        } else {

            Log.d(
                    TAG,
                    "Storage permission NOT GRANTED"
            );

            statusText.setText(
                    "⚠ Akses penyimpanan belum diberikan."
            );

            permissionButton.setText(
                    "Berikan Akses Semua File"
            );

            permissionButton.setEnabled(true);

            selectObbButton.setEnabled(false);
            processButton.setEnabled(false);
        }
    }

    private void requestStoragePermission() {

        Log.d(
                TAG,
                "Requesting storage permission"
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            try {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                );

                intent.setData(
                        Uri.parse(
                                "package:" +
                                getPackageName()
                        )
                );

                startActivity(intent);

                Log.d(
                        TAG,
                        "Opened app-specific All Files Access settings"
                );

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Failed to open app-specific settings",
                        e
                );

                try {

                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    );

                    startActivity(intent);

                    Log.d(
                            TAG,
                            "Opened global All Files Access settings"
                    );

                } catch (Exception ex) {

                    Log.e(
                            TAG,
                            "Failed to open All Files Access settings",
                            ex
                    );

                    Toast.makeText(
                            this,
                            "Tidak dapat membuka pengaturan izin.",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    REQUEST_STORAGE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == REQUEST_STORAGE) {

            Log.d(
                    TAG,
                    "Legacy storage permission result"
            );

            updatePermissionState();
        }
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (permissionButton != null) {
            updatePermissionState();
        }
    }

    // ============================================================
    // PICK OBB FILE
    // ============================================================

    private void openObbPicker() {

        if (!hasStoragePermission()) {

            Log.d(
                    TAG,
                    "Cannot pick OBB: permission missing"
            );

            requestStoragePermission();

            return;
        }

        Log.d(
                TAG,
                "Opening OBB file picker"
        );

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

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

    // ============================================================
    // PICK RESULT
    // ============================================================

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
                requestCode != PICK_OBB ||
                resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null
        ) {

            Log.d(
                    TAG,
                    "OBB picker cancelled"
            );

            return;
        }

        selectedObbUri =
                data.getData();

        Log.d(
                TAG,
                "Selected URI = " +
                selectedObbUri
        );

        selectedObbName =
                getFileName(selectedObbUri);

        Log.d(
                TAG,
                "Selected filename = " +
                selectedObbName
        );

        if (
                selectedObbName == null ||
                !selectedObbName
                        .toLowerCase(Locale.US)
                        .endsWith(".obb")
        ) {

            Log.e(
                    TAG,
                    "Selected file is not OBB"
            );

            selectedObbUri = null;
            selectedObbName = null;
            detectedPackage = null;

            selectedFileText.setVisibility(
                    TextView.GONE
            );

            packageText.setVisibility(
                    TextView.GONE
            );

            processButton.setEnabled(false);

            statusText.setText(
                    "❌ File yang dipilih bukan OBB."
            );

            return;
        }

        detectedPackage =
                extractPackageName(
                        selectedObbName
                );

        Log.d(
                TAG,
                "Detected package = " +
                detectedPackage
        );

        if (
                detectedPackage == null ||
                detectedPackage.trim().isEmpty()
        ) {

            Log.e(
                    TAG,
                    "Package detection failed"
            );

            processButton.setEnabled(false);

            statusText.setText(
                    "❌ Package tidak dapat dideteksi dari nama OBB."
            );

            return;
        }

        selectedFileText.setVisibility(
                TextView.VISIBLE
        );

        selectedFileText.setText(
                "📦 File OBB:\n" +
                selectedObbName
        );

        packageText.setVisibility(
                TextView.VISIBLE
        );

        packageText.setText(
                "📁 Package:\n" +
                detectedPackage +
                "\n\n" +
                "📍 Tujuan otomatis:\n" +
                "/storage/emulated/0/Android/obb/" +
                detectedPackage
        );

        statusText.setText(
                "✓ OBB berhasil dipilih.\n" +
                "Tekan \"Proses Install\" untuk memulai."
        );

        processButton.setEnabled(true);

        progressBar.setVisibility(
                ProgressBar.GONE
        );

        progressText.setVisibility(
                TextView.GONE
        );

        progressBar.setProgress(0);

        Log.d(
                TAG,
                "OBB selection complete"
        );
    }

    // ============================================================
    // START INSTALL
    // ============================================================

    private void startInstall() {

        if (
                selectedObbUri == null ||
                selectedObbName == null ||
                detectedPackage == null
        ) {

            Log.e(
                    TAG,
                    "Install requested but OBB data missing"
            );

            statusText.setText(
                    "Pilih file OBB terlebih dahulu."
            );

            return;
        }

        if (!hasStoragePermission()) {

            Log.e(
                    TAG,
                    "Install aborted: storage permission missing"
            );

            statusText.setText(
                    "Akses penyimpanan belum diberikan."
            );

            requestStoragePermission();

            return;
        }

        Log.d(TAG, "================================");
        Log.d(TAG, "INSTALL START");
        Log.d(TAG, "OBB = " + selectedObbName);
        Log.d(TAG, "PACKAGE = " + detectedPackage);
        Log.d(TAG, "================================");

        processButton.setEnabled(false);
        selectObbButton.setEnabled(false);
        permissionButton.setEnabled(false);

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

        statusText.setText(
                "Mempersiapkan instalasi..."
        );

        new Thread(
                this::performInstall
        ).start();
    }

    // ============================================================
    // DIRECT FILE INSTALL
    // ============================================================

    private void performInstall() {

        File obbRoot =
                new File(
                        Environment.getExternalStorageDirectory(),
                        "Android/obb"
                );

        File packageDirectory =
                new File(
                        obbRoot,
                        detectedPackage
                );

        File destination =
                new File(
                        packageDirectory,
                        selectedObbName
                );

        Log.d(
                TAG,
                "External storage = " +
                Environment.getExternalStorageDirectory()
        );

        Log.d(
                TAG,
                "OBB root = " +
                obbRoot.getAbsolutePath()
        );

        Log.d(
                TAG,
                "Package directory = " +
                packageDirectory.getAbsolutePath()
        );

        Log.d(
                TAG,
                "Destination = " +
                destination.getAbsolutePath()
        );

        try {

            runOnUiThread(() ->
                    statusText.setText(
                            "Membuat folder package..."
                    )
            );

            // ====================================================
            // CHECK OBB ROOT
            // ====================================================

            if (!obbRoot.exists()) {

                Log.d(
                        TAG,
                        "OBB root does not exist. Creating..."
                );

                boolean created =
                        obbRoot.mkdirs();

                Log.d(
                        TAG,
                        "OBB root mkdirs = " +
                        created
                );
            }

            if (!obbRoot.exists()) {

                throw new Exception(
                        "Folder Android/obb tidak dapat diakses."
                );
            }

            // ====================================================
            // CREATE PACKAGE DIRECTORY
            // ====================================================

            if (!packageDirectory.exists()) {

                Log.d(
                        TAG,
                        "Creating package directory..."
                );

                boolean created =
                        packageDirectory.mkdirs();

                Log.d(
                        TAG,
                        "Package mkdirs = " +
                        created
                );
            } else {

                Log.d(
                        TAG,
                        "Package directory already exists"
                );
            }

            if (
                    !packageDirectory.exists() ||
                    !packageDirectory.isDirectory()
            ) {

                throw new Exception(
                        "Folder package tidak dapat dibuat:\n" +
                        packageDirectory
                                .getAbsolutePath()
                );
            }

            runOnUiThread(() ->
                    statusText.setText(
                            "Folder package siap.\n" +
                            "Menyalin OBB..."
                    )
            );

            // ====================================================
            // SOURCE STREAM
            // ====================================================

            InputStream input =
                    getContentResolver()
                            .openInputStream(
                                    selectedObbUri
                            );

            if (input == null) {

                throw new Exception(
                        "Tidak dapat membuka file OBB sumber."
                );
            }

            // ====================================================
            // DESTINATION STREAM
            // ====================================================

            OutputStream output =
                    new FileOutputStream(
                            destination,
                            false
                    );

            long fileSize =
                    getFileSize(selectedObbUri);

            Log.d(
                    TAG,
                    "Source file size = " +
                    fileSize
            );

            byte[] buffer =
                    new byte[1024 * 1024];

            long totalCopied = 0;

            int bytesRead;

            try {

                while (
                        (bytesRead =
                                input.read(buffer)) != -1
                ) {

                    output.write(
                            buffer,
                            0,
                            bytesRead
                    );

                    totalCopied += bytesRead;

                    int progress = 0;

                    if (fileSize > 0) {

                        progress =
                                (int)
                                (
                                        totalCopied * 100L
                                        / fileSize
                                );

                        if (progress > 100) {
                            progress = 100;
                        }
                    }

                    final int finalProgress =
                            progress;

                    final long finalBytes =
                            totalCopied;

                    runOnUiThread(() -> {

                        progressBar.setProgress(
                                finalProgress
                        );

                        progressText.setText(
                                finalProgress +
                                "%\n" +
                                formatBytes(
                                        finalBytes
                                ) +
                                " tersalin"
                        );

                        statusText.setText(
                                "Menyalin OBB..."
                        );
                    });
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
                    "Copy finished."
            );

            Log.d(
                    TAG,
                    "Total copied = " +
                    totalCopied
            );

            // ====================================================
            // VERIFY
            // ====================================================

            if (!destination.exists()) {

                throw new Exception(
                        "File tujuan tidak ditemukan setelah proses copy."
                );
            }

            long destinationSize =
                    destination.length();

            Log.d(
                    TAG,
                    "Destination size = " +
                    destinationSize
            );

            if (
                    fileSize > 0 &&
                    destinationSize != fileSize
            ) {

                Log.e(
                        TAG,
                        "SIZE MISMATCH"
                );

                throw new Exception(
                        "Ukuran file tidak cocok.\n" +
                        "Sumber: " +
                        fileSize +
                        "\nTujuan: " +
                        destinationSize
                );
            }

            // ====================================================
            // SUCCESS
            // ====================================================

            Log.d(
                    TAG,
                    "================================"
            );

            Log.d(
                    TAG,
                    "INSTALL SUCCESS"
            );

            Log.d(
                    TAG,
                    "Destination = " +
                    destination.getAbsolutePath()
            );

            Log.d(
                    TAG,
                    "================================"
            );

            runOnUiThread(() -> {

                progressBar.setProgress(100);

                progressText.setText(
                        "100% - SELESAI"
                );

                statusText.setText(
                        "✓ OBB BERHASIL DIINSTALL\n\n" +
                        "File:\n" +
                        selectedObbName +
                        "\n\n" +
                        "Package:\n" +
                        detectedPackage +
                        "\n\n" +
                        "Lokasi:\n" +
                        destination.getAbsolutePath()
                );

                selectObbButton.setEnabled(true);
                processButton.setEnabled(false);
                permissionButton.setEnabled(false);

                Toast.makeText(
                        MainActivity.this,
                        "OBB berhasil dipindahkan.",
                        Toast.LENGTH_LONG
                ).show();
            });

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "================================",
                    e
            );

            Log.e(
                    TAG,
                    "INSTALL FAILED"
            );

            Log.e(
                    TAG,
                    "Exception = " +
                    e.getClass().getName()
            );

            Log.e(
                    TAG,
                    "Message = " +
                    e.getMessage()
            );

            runOnUiThread(() -> {

                progressBar.setProgress(0);

                progressText.setText(
                        "GAGAL"
                );

                statusText.setText(
                        "❌ GAGAL MENYALIN OBB\n\n" +
                        e.getClass()
                                .getSimpleName() +
                        "\n\n" +
                        String.valueOf(
                                e.getMessage()
                        )
                );

                selectObbButton.setEnabled(true);
                processButton.setEnabled(true);
                permissionButton.setEnabled(
                        !hasStoragePermission()
                );

                Toast.makeText(
                        MainActivity.this,
                        "Gagal menyalin OBB.",
                        Toast.LENGTH_LONG
                ).show();
            });
        }
    }

    // ============================================================
    // GET FILE NAME FROM SELECTED URI
    // ============================================================

    private String getFileName(Uri uri) {

        android.database.Cursor cursor = null;

        try {

            cursor =
                    getContentResolver().query(
                            uri,
                            new String[]{
                                    "_display_name"
                            },
                            null,
                            null,
                            null
                    );

            if (
                    cursor != null &&
                    cursor.moveToFirst()
            ) {

                return cursor.getString(0);
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to get filename",
                    e
            );

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return null;
    }

    // ============================================================
    // GET FILE SIZE
    // ============================================================

    private long getFileSize(Uri uri) {

        android.database.Cursor cursor = null;

        try {

            cursor =
                    getContentResolver().query(
                            uri,
                            new String[]{
                                    "_size"
                            },
                            null,
                            null,
                            null
                    );

            if (
                    cursor != null &&
                    cursor.moveToFirst()
            ) {

                if (!cursor.isNull(0)) {

                    return cursor.getLong(0);
                }
            }

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Failed to get file size",
                    e
            );

        } finally {

            if (cursor != null) {
                cursor.close();
            }
        }

        return -1;
    }

    // ============================================================
    // DETECT PACKAGE FROM OBB NAME
    // ============================================================

    private String extractPackageName(
            String fileName
    ) {

        if (fileName == null) {
            return null;
        }

        String name =
                fileName.trim();

        if (
                !name
                        .toLowerCase(Locale.US)
                        .endsWith(".obb")
        ) {

            return null;
        }

        name =
                name.substring(
                        0,
                        name.length() - 4
                );

        Log.d(
                TAG,
                "Parsing OBB name: " +
                name
        );

        /*
         * Format OBB:
         *
         * main.<version>.<package>.obb
         *
         * atau:
         *
         * patch.<version>.<package>.obb
         *
         * Contoh:
         *
         * main.547389636.com.and.games505.portal_knights.obb
         *
         * Hasil:
         *
         * com.and.games505.portal_knights
         */

        String[] parts =
                name.split("\\.");

        if (parts.length < 3) {

            Log.e(
                    TAG,
                    "Invalid OBB filename"
            );

            return null;
        }

        String type =
                parts[0];

        if (
                !type.equalsIgnoreCase("main") &&
                !type.equalsIgnoreCase("patch")
        ) {

            Log.e(
                    TAG,
                    "OBB type is not main/patch: " +
                    type
            );

            return null;
        }

        // parts[1] = version code

        StringBuilder packageBuilder =
                new StringBuilder();

        for (
                int i = 2;
                i < parts.length;
                i++
        ) {

            if (packageBuilder.length() > 0) {
                packageBuilder.append(".");
            }

            packageBuilder.append(
                    parts[i]
            );
        }

        String packageName =
                packageBuilder.toString();

        if (packageName.isEmpty()) {
            return null;
        }

        Log.d(
                TAG,
                "Package detected = " +
                packageName
        );

        return packageName;
    }

    // ============================================================
    // FORMAT BYTES
    // ============================================================

    private String formatBytes(long bytes) {

        if (bytes < 1024) {

            return bytes + " B";
        }

        if (bytes < 1024L * 1024L) {

            return String.format(
                    Locale.US,
                    "%.2f KB",
                    bytes / 1024.0
            );
        }

        if (bytes < 1024L * 1024L * 1024L) {

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
                        (1024.0 *
                                1024.0 *
                                1024.0)
        );
    }
}
