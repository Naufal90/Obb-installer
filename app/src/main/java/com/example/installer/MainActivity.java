package com.example.installer;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private static final String TAG = "OBB_INSTALLER";

    private static final int PICK_OBB = 100;

    private TextView status;
    private Button selectObb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "=== APPLICATION START ===");
        Log.d(TAG, "Android SDK: " + Build.VERSION.SDK_INT);
        Log.d(TAG, "Package aplikasi: " + getPackageName());

        createInterface();
        updatePermissionStatus();
    }

    private void createInterface() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("OBB Installer");
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);

        TextView description = new TextView(this);
        description.setText(
                "Installer OBB pribadi\n\n" +
                "File akan dipasang otomatis ke:\n" +
                "Android/obb/<package>/"
        );
        description.setTextSize(16);

        Button permissionButton = new Button(this);
        permissionButton.setText("Berikan Akses Penyimpanan");

        selectObb = new Button(this);
        selectObb.setText("Pilih File OBB");

        status = new TextView(this);
        status.setTextSize(16);
        status.setText("Memeriksa akses...");

        layout.addView(
                title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        layout.addView(
                description,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        layout.addView(permissionButton);
        layout.addView(selectObb);

        layout.addView(
                status,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(layout);

        permissionButton.setOnClickListener(v -> {
            Log.d(TAG, "Tombol permission ditekan");
            openStorageSettings();
        });

        selectObb.setOnClickListener(v -> {
            Log.d(TAG, "Tombol pilih OBB ditekan");
            chooseObb();
        });
    }

    private boolean hasStorageAccess() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            boolean result = Environment.isExternalStorageManager();

            Log.d(
                    TAG,
                    "MANAGE_EXTERNAL_STORAGE = " + result
            );

            return result;
        }

        Log.d(TAG, "Android <= 10, storage access dianggap tersedia");

        return true;
    }

    private void updatePermissionStatus() {

        if (hasStorageAccess()) {

            Log.d(TAG, "Storage permission: GRANTED");

            status.setText(
                    "Status: Akses penyimpanan diberikan.\n\n" +
                    "Silakan pilih file OBB."
            );

            selectObb.setEnabled(true);

        } else {

            Log.d(TAG, "Storage permission: NOT GRANTED");

            status.setText(
                    "Status: Akses penyimpanan belum diberikan.\n\n" +
                    "Tekan \"Berikan Akses Penyimpanan\"."
            );

            selectObb.setEnabled(false);
        }
    }

    private void openStorageSettings() {

        Log.d(TAG, "Membuka halaman All Files Access");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            try {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                );

                intent.setData(
                        Uri.parse("package:" + getPackageName())
                );

                startActivity(intent);

                Log.d(TAG, "Settings aplikasi berhasil dibuka");

            } catch (Exception e) {

                Log.e(
                        TAG,
                        "Gagal membuka settings aplikasi",
                        e
                );

                try {

                    Intent intent = new Intent(
                            Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    );

                    startActivity(intent);

                    Log.d(TAG, "Settings All Files dibuka");

                } catch (Exception ex) {

                    Log.e(
                            TAG,
                            "Gagal membuka semua halaman settings",
                            ex
                    );
                }
            }
        }
    }

    private void chooseObb() {

        if (!hasStorageAccess()) {

            Log.w(
                    TAG,
                    "chooseObb dibatalkan: storage permission belum diberikan"
            );

            status.setText(
                    "Akses penyimpanan belum diberikan."
            );

            return;
        }

        Log.d(TAG, "Membuka file picker");

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

        try {

            startActivityForResult(
                    intent,
                    PICK_OBB
            );

            Log.d(TAG, "File picker berhasil dibuka");

        } catch (Exception e) {

            Log.e(
                    TAG,
                    "Gagal membuka file picker",
                    e
            );

            status.setText(
                    "Gagal membuka file picker:\n" +
                    e.getMessage()
            );
        }
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
                "onActivityResult requestCode=" +
                        requestCode +
                        " resultCode=" +
                        resultCode
        );

        if (requestCode != PICK_OBB) {
            return;
        }

        if (resultCode != RESULT_OK || data == null) {

            Log.w(TAG, "Pemilihan OBB dibatalkan");

            status.setText(
                    "Pemilihan OBB dibatalkan."
            );

            return;
        }

        Uri uri = data.getData();

        if (uri == null) {

            Log.e(TAG, "URI OBB = null");

            status.setText(
                    "File OBB tidak ditemukan."
            );

            return;
        }

        Log.d(TAG, "URI OBB: " + uri);

        String fileName = getFileName(uri);

        Log.d(
                TAG,
                "Nama file OBB: " + fileName
        );

        if (fileName == null ||
                fileName.trim().isEmpty()) {

            Log.e(TAG, "Nama file tidak valid");

            status.setText(
                    "Nama file OBB tidak valid."
            );

            return;
        }

        if (!fileName.toLowerCase().endsWith(".obb")) {

            Log.w(
                    TAG,
                    "File bukan ekstensi .obb: " +
                            fileName
            );

            status.setText(
                    "File yang dipilih bukan file .obb."
            );

            return;
        }

        String packageName = extractPackageName(fileName);

        if (packageName == null) {

            Log.e(
                    TAG,
                    "Package tidak dapat ditentukan dari nama OBB"
            );

            status.setText(
                    "Tidak dapat menentukan package dari nama OBB.\n\n" +
                    "Contoh nama yang benar:\n" +
                    "main.123456.com.contoh.game.obb"
            );

            return;
        }

        Log.d(
                TAG,
                "Package terdeteksi: " +
                        packageName
        );

        File obbRoot = new File(
                Environment.getExternalStorageDirectory(),
                "Android/obb"
        );

        File packageDirectory = new File(
                obbRoot,
                packageName
        );

        File destination = new File(
                packageDirectory,
                fileName
        );

        Log.d(
                TAG,
                "OBB root: " +
                        obbRoot.getAbsolutePath()
        );

        Log.d(
                TAG,
                "Package directory: " +
                        packageDirectory.getAbsolutePath()
        );

        Log.d(
                TAG,
                "Destination: " +
                        destination.getAbsolutePath()
        );

        status.setText(
                "OBB dipilih:\n" +
                fileName +
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
