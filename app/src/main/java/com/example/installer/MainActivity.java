package com.example.installer;

import android.Manifest;
import android.app.Activity;
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

import androidx.annotation.NonNull;

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
        createInterface();
        updatePermissionState();
    }

    private void createInterface() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("OBB Installer (Legacy)");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        statusText = new TextView(this);
        statusText.setTextSize(17);
        statusText.setPadding(0, 30, 0, 20);
        root.addView(statusText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        permissionButton = new Button(this);
        permissionButton.setText("Berikan Izin Penyimpanan");
        root.addView(permissionButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        selectedFileText = new TextView(this);
        selectedFileText.setTextSize(16);
        selectedFileText.setPadding(0, 20, 0, 10);
        selectedFileText.setVisibility(TextView.GONE);
        root.addView(selectedFileText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        packageText = new TextView(this);
        packageText.setTextSize(16);
        packageText.setPadding(0, 10, 0, 20);
        packageText.setVisibility(TextView.GONE);
        root.addView(packageText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        selectObbButton = new Button(this);
        selectObbButton.setText("Pilih File OBB");
        selectObbButton.setEnabled(false);
        root.addView(selectObbButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        processButton = new Button(this);
        processButton.setText("Proses Install");
        processButton.setEnabled(false);
        root.addView(processButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        progressBar.setVisibility(ProgressBar.GONE);

        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 50
        );
        progressParams.topMargin = 30;
        root.addView(progressBar, progressParams);

        progressText = new TextView(this);
        progressText.setTextSize(16);
        progressText.setGravity(Gravity.CENTER);
        progressText.setVisibility(TextView.GONE);
        root.addView(progressText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);

        permissionButton.setOnClickListener(v -> requestStoragePermission());
        selectObbButton.setOnClickListener(v -> openObbPicker());
        processButton.setOnClickListener(v -> startInstall());
    }

    // ============================================================
    // PERMISSION (Khusus Android 10 / targetSdk 29)
    // ============================================================

    private boolean hasStoragePermission() {
        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void updatePermissionState() {
        if (hasStoragePermission()) {
            statusText.setText("✓ Izin penyimpanan aktif.\nSilakan pilih file OBB.");
            permissionButton.setText("Izin Penyimpanan: AKTIF");
            permissionButton.setEnabled(false);
            selectObbButton.setEnabled(true);
        } else {
            statusText.setText("⚠ Izin penyimpanan belum diberikan.");
            permissionButton.setText("Berikan Izin Penyimpanan");
            permissionButton.setEnabled(true);
            selectObbButton.setEnabled(false);
            processButton.setEnabled(false);
        }
    }

    private void requestStoragePermission() {
        requestPermissions(new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        }, REQUEST_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE) {
            updatePermissionState();
        }
    }

    // ============================================================
    // PICK OBB FILE
    // ============================================================

    private void openObbPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICK_OBB);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_OBB || resultCode != RESULT_OK || data == null || data.getData() == null) return;

        selectedObbUri = data.getData();
        selectedObbName = getFileName(selectedObbUri);

        if (selectedObbName == null || !selectedObbName.toLowerCase(Locale.US).endsWith(".obb")) {
            statusText.setText("❌ File yang dipilih bukan OBB.");
            return;
        }

        detectedPackage = extractPackageName(selectedObbName);
        if (detectedPackage == null) {
            statusText.setText("❌ Package tidak terdeteksi.");
            return;
        }

        selectedFileText.setVisibility(TextView.VISIBLE);
        selectedFileText.setText("📦 File OBB:\n" + selectedObbName);
        packageText.setVisibility(TextView.VISIBLE);
        packageText.setText("📁 Package:\n" + detectedPackage);
        statusText.setText("✓ OBB siap.\nTekan \"Proses Install\".");
        processButton.setEnabled(true);
        progressBar.setVisibility(ProgressBar.GONE);
        progressText.setVisibility(ProgressBar.GONE);
        progressBar.setProgress(0);
    }

    // ============================================================
    // DIRECT INSTALL USING java.io.File (Otomatis ke Android/obb/)
    // ============================================================

    private void startInstall() {
        if (selectedObbUri == null) return;

        processButton.setEnabled(false);
        selectObbButton.setEnabled(false);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressText.setVisibility(TextView.VISIBLE);
        progressBar.setProgress(0);
        progressText.setText("0%");

        new Thread(this::performInstall).start();
    }

    private void performInstall() {
        // Karena targetSdk 29 + requestLegacyExternalStorage, ini 100% work!
        File obbRoot = new File(Environment.getExternalStorageDirectory(), "Android/obb");
        File packageDirectory = new File(obbRoot, detectedPackage);
        File destination = new File(packageDirectory, selectedObbName);

        try {
            runOnUiThread(() -> statusText.setText("Membuat folder package..."));

            if (!obbRoot.exists()) obbRoot.mkdirs();
            if (!packageDirectory.exists()) packageDirectory.mkdirs();

            if (!packageDirectory.exists()) {
                throw new Exception("Gagal membuat folder: " + packageDirectory.getAbsolutePath());
            }

            runOnUiThread(() -> statusText.setText("Menyalin OBB..."));

            InputStream input = getContentResolver().openInputStream(selectedObbUri);
            OutputStream output = new FileOutputStream(destination, false);

            long fileSize = getFileSize(selectedObbUri);
            byte[] buffer = new byte[1024 * 1024]; // 1MB
            long totalCopied = 0;
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
                totalCopied += bytesRead;

                int progress = 0;
                if (fileSize > 0) {
                    progress = (int) ((totalCopied * 100L) / fileSize);
                    if (progress > 100) progress = 100;
                }

                final int finalProgress = progress;
                final long finalBytes = totalCopied;
                runOnUiThread(() -> {
                    progressBar.setProgress(finalProgress);
                    progressText.setText(finalProgress + "%\n" + formatBytes(finalBytes) + " tersalin");
                });
            }

            output.flush();
            input.close();
            output.close();

            runOnUiThread(() -> {
                progressBar.setProgress(100);
                progressText.setText("100% - SELESAI");
                statusText.setText("✓ OBB BERHASIL DIINSTALL\n\nLokasi:\n" + destination.getAbsolutePath());
                selectObbButton.setEnabled(true);
                processButton.setEnabled(false);
                Toast.makeText(this, "OBB berhasil dipasang otomatis!", Toast.LENGTH_LONG).show();
            });

        } catch (Exception e) {
            Log.e(TAG, "INSTALL FAILED", e);
            runOnUiThread(() -> {
                progressBar.setProgress(0);
                progressText.setText("GAGAL");
                statusText.setText("❌ GAGAL MENYALIN OBB\n\n" + e.getMessage());
                selectObbButton.setEnabled(true);
                processButton.setEnabled(true);
                Toast.makeText(this, "Gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
        }
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String getFileName(Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{"_display_name"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    private long getFileSize(Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{"_size"}, null, null, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) return cursor.getLong(0);
        } catch (Exception ignored) {
        } finally {
            if (cursor != null) cursor.close();
        }
        return -1;
    }

    private String extractPackageName(String fileName) {
        if (fileName == null) return null;
        String name = fileName.trim();
        if (!name.toLowerCase(Locale.US).endsWith(".obb")) return null;
        name = name.substring(0, name.length() - 4);
        String[] parts = name.split("\\.");
        if (parts.length < 3) return null;
        if (!parts[0].equalsIgnoreCase("main") && !parts[0].equalsIgnoreCase("patch")) return null;

        StringBuilder packageBuilder = new StringBuilder();
        for (int i = 2; i < parts.length; i++) {
            if (packageBuilder.length() > 0) packageBuilder.append(".");
            packageBuilder.append(parts[i]);
        }
        String packageName = packageBuilder.toString();
        return packageName.isEmpty() ? null : packageName;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024L) return String.format(Locale.US, "%.2f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024L * 1024L) return String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}
