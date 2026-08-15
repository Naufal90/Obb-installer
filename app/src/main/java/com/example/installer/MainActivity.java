package com.example.installer;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.documentfile.provider.DocumentFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String TAG = "OBB_INSTALLER";

    private static final int PICK_OBB_FILE = 1001;
    private static final int PICK_OBB_FOLDER = 1002;

    private TextView statusText;
    private TextView selectedFileText;
    private TextView packageText;
    private TextView progressText;

    private ProgressBar progressBar;

    private Button selectObbButton;
    private Button processButton;

    private Uri selectedObbUri;
    private String selectedObbName;
    private String detectedPackage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "================================");
        Log.d(TAG, "OBB INSTALLER START (SAF Mode)");
        Log.d(TAG, "================================");

        createInterface();
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
        title.setText("OBB Installer (SAF)");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        statusText = new TextView(this);
        statusText.setTextSize(17);
        statusText.setPadding(0, 30, 0, 20);
        root.addView(statusText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        selectedFileText = new TextView(this);
        selectedFileText.setTextSize(16);
        selectedFileText.setPadding(0, 20, 0, 10);
        selectedFileText.setVisibility(TextView.GONE);
        root.addView(selectedFileText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        packageText = new TextView(this);
        packageText.setTextSize(16);
        packageText.setPadding(0, 10, 0, 20);
        packageText.setVisibility(TextView.GONE);
        root.addView(packageText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        selectObbButton = new Button(this);
        selectObbButton.setText("Pilih File OBB");
        root.addView(selectObbButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        processButton = new Button(this);
        processButton.setText("Proses Install");
        processButton.setEnabled(false);
        root.addView(processButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
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
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        setContentView(root);

        statusText.setText("Selamat datang.\nSilakan pilih file OBB yang ingin dipasang.");

        selectObbButton.setOnClickListener(v -> openObbPicker());
        processButton.setOnClickListener(v -> startInstall());
    }

    // ============================================================
    // PICK OBB SOURCE FILE
    // ============================================================

    private void openObbPicker() {
        Log.d(TAG, "Opening OBB file picker");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(intent, PICK_OBB_FILE);
    }

    // ============================================================
    // ACTIVITY RESULT
    // ============================================================

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null || data.getData() == null) {
            Log.d(TAG, "Picker cancelled");
            return;
        }

        if (requestCode == PICK_OBB_FILE) {
            handleObbFileResult(data.getData());
        } else if (requestCode == PICK_OBB_FOLDER) {
            handleObbFolderResult(data.getData());
        }
    }

    private void handleObbFileResult(Uri uri) {
        selectedObbUri = uri;
        selectedObbName = getFileName(selectedObbUri);

        if (selectedObbName == null || !selectedObbName.toLowerCase(Locale.US).endsWith(".obb")) {
            statusText.setText("❌ File yang dipilih bukan OBB.");
            selectedObbUri = null;
            selectedObbName = null;
            return;
        }

        detectedPackage = extractPackageName(selectedObbName);

        if (detectedPackage == null || detectedPackage.trim().isEmpty()) {
            statusText.setText("❌ Package tidak dapat dideteksi dari nama OBB.");
            return;
        }

        selectedFileText.setVisibility(TextView.VISIBLE);
        selectedFileText.setText("📦 File OBB:\n" + selectedObbName);

        packageText.setVisibility(TextView.VISIBLE);
        packageText.setText("📁 Package:\n" + detectedPackage);

        statusText.setText("✓ OBB berhasil dipilih.\nTekan \"Proses Install\" untuk memulai.");
        processButton.setEnabled(true);
        progressBar.setVisibility(ProgressBar.GONE);
        progressText.setVisibility(TextView.GONE);
        progressBar.setProgress(0);
    }

    private void handleObbFolderResult(Uri treeUri) {
        // Simpan izin permanen untuk folder ini
        getContentResolver().takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        );

        statusText.setText("✓ Folder OBB dipilih.\nMemulai proses instalasi...");
        performInstall(treeUri);
    }

    // ============================================================
    // START INSTALL
    // ============================================================

    private void startInstall() {
        if (selectedObbUri == null || selectedObbName == null || detectedPackage == null) {
            statusText.setText("Pilih file OBB terlebih dahulu.");
            return;
        }

        // Tampilkan instruksi sebelum membuka picker folder
        new AlertDialog.Builder(this)
                .setTitle("Izin Akses Folder")
                .setMessage("Aplikasi akan membuka File Manager.\n\n" +
                        "1. Cari dan buka folder bernama 'Android'\n" +
                        "2. Buka folder bernama 'obb'\n" +
                        "3. Tekan tombol 'Select' atau 'Use this folder' di bagian bawah layar.\n\n" +
                        "Hal ini wajib dilakukan agar sistem Android mengizinkan aplikasi menulis file OBB.")
                .setPositiveButton("Buka File Manager", (dialog, which) -> launchFolderPicker())
                .setNegativeButton("Batal", null)
                .show();
    }

    private void launchFolderPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        intent.putExtra("android.content.extra.FANCY", true);
        try {
            startActivityForResult(intent, PICK_OBB_FOLDER);
        } catch (Exception e) {
            Toast.makeText(this, "File Manager tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    // ============================================================
    // PERFORM INSTALL USING SAF (DocumentFile)
    // ============================================================

    private void performInstall(Uri treeUri) {
        processButton.setEnabled(false);
        selectObbButton.setEnabled(false);
        progressBar.setVisibility(ProgressBar.VISIBLE);
        progressText.setVisibility(TextView.VISIBLE);
        progressBar.setProgress(0);
        progressText.setText("0%");

        new Thread(() -> {
            try {
                // 1. Akses folder tree yang dipilih user (misal: Android/obb/)
                DocumentFile treeDocFile = DocumentFile.fromTreeUri(this, treeUri);
                if (treeDocFile == null) {
                    throw new Exception("Tidak dapat mengakses folder OBB.");
                }

                runOnUiThread(() -> statusText.setText("Membuat folder package..."));

                // 2. Cari atau buat folder game (com.and.games505.portal_knights)
                DocumentFile gameFolder = treeDocFile.findFile(detectedPackage);
                if (gameFolder == null || !gameFolder.isDirectory()) {
                    gameFolder = treeDocFile.createDirectory(detectedPackage);
                }

                if (gameFolder == null) {
                    throw new Exception("Gagal membuat folder package game.");
                }

                // 3. Buat file OBB tujuan
                DocumentFile obbFile = gameFolder.findFile(selectedObbName);
                if (obbFile != null) {
                    obbFile.delete(); // Hapus yang lama
                }
                obbFile = gameFolder.createFile("application/octet-stream", selectedObbName);

                if (obbFile == null) {
                    throw new Exception("Gagal membuat file OBB tujuan.");
                }

                runOnUiThread(() -> statusText.setText("Menyalin OBB..."));

                // 4. Setup Streams
                InputStream input = getContentResolver().openInputStream(selectedObbUri);
                OutputStream output = getContentResolver().openOutputStream(obbFile.getUri());

                if (input == null || output == null) {
                    throw new Exception("Gagal membuka stream file.");
                }

                long fileSize = getFileSize(selectedObbUri);
                byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
                long totalCopied = 0;
                int bytesRead;

                // 5. Copy file dengan progress
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

                // 6. Sukses
                runOnUiThread(() -> {
                    progressBar.setProgress(100);
                    progressText.setText("100% - SELESAI");
                    statusText.setText("✓ OBB BERHASIL DIINSTALL\n\n" +
                            "File:\n" + selectedObbName + "\n\n" +
                            "Package:\n" + detectedPackage);
                    selectObbButton.setEnabled(true);
                    processButton.setEnabled(false);
                    Toast.makeText(this, "OBB berhasil dipindahkan.", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "INSTALL FAILED", e);
                runOnUiThread(() -> {
                    progressBar.setProgress(0);
                    progressText.setText("GAGAL");
                    statusText.setText("❌ GAGAL MENYALIN OBB\n\n" + e.getClass().getSimpleName() + "\n\n" + e.getMessage());
                    selectObbButton.setEnabled(true);
                    processButton.setEnabled(true);
                    Toast.makeText(this, "Gagal menyalin OBB.", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ============================================================
    // HELPER METHODS
    // ============================================================

    private String getFileName(Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{"_display_name"}, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get filename", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    private long getFileSize(Uri uri) {
        android.database.Cursor cursor = null;
        try {
            cursor = getContentResolver().query(uri, new String[]{"_size"}, null, null, null);
            if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
                return cursor.getLong(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get file size", e);
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

        String type = parts[0];
        if (!type.equalsIgnoreCase("main") && !type.equalsIgnoreCase("patch")) return null;

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
