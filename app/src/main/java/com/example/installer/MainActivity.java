package com.example.installer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int PICK_OBB = 100;

    private TextView status;
    private Button selectObb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                "File OBB akan diarahkan ke:\n" +
                "Android/obb/<package>/"
        );
        description.setTextSize(16);

        Button permissionButton = new Button(this);
        permissionButton.setText("Berikan Akses Penyimpanan");

        selectObb = new Button(this);
        selectObb.setText("Pilih File OBB");

        status = new TextView(this);
        status.setTextSize(16);

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

        permissionButton.setOnClickListener(v -> openStorageSettings());

        selectObb.setOnClickListener(v -> chooseObb());
    }

    private boolean hasStorageAccess() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }

        return true;
    }

    private void updatePermissionStatus() {

        if (hasStorageAccess()) {

            status.setText(
                    "Status: Akses penyimpanan diberikan.\n\n" +
                    "Silakan pilih file OBB."
            );

            selectObb.setEnabled(true);

        } else {

            status.setText(
                    "Status: Akses penyimpanan belum diberikan.\n\n" +
                    "Tekan \"Berikan Akses Penyimpanan\"."
            );

            selectObb.setEnabled(false);
        }
    }

    private void openStorageSettings() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            try {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                );

                intent.setData(
                        Uri.parse("package:" + getPackageName())
                );

                startActivity(intent);

            } catch (Exception e) {

                Intent intent = new Intent(
                        Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                );

                startActivity(intent);
            }
        }
    }

    private void chooseObb() {

        if (!hasStorageAccess()) {

            status.setText(
                    "Akses penyimpanan belum diberikan."
            );

            return;
        }

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("*/*");

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivityForResult(intent, PICK_OBB);
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

        if (requestCode != PICK_OBB) {
            return;
        }

        if (resultCode != RESULT_OK || data == null) {

            status.setText(
                    "Pemilihan OBB dibatalkan."
            );

            return;
        }

        Uri uri = data.getData();

        if (uri == null) {

            status.setText(
                    "File OBB tidak ditemukan."
            );

            return;
        }

        String fileName = getFileName(uri);

        status.setText(
                "OBB dipilih:\n\n" +
                fileName +
                "\n\n" +
                "Lokasi tujuan:\n" +
                "Android/obb/<package>/\n\n" +
                "Tahap penyalinan akan dilakukan berikutnya."
        );
    }

    private String getFileName(Uri uri) {

        String path = uri.getPath();

        if (path == null) {
            return "Unknown.obb";
        }

        int separator = path.lastIndexOf('/');

        if (separator >= 0 && separator + 1 < path.length()) {
            return path.substring(separator + 1);
        }

        return path;
    }

    @Override
    protected void onResume() {

        super.onResume();

        if (status != null) {
            updatePermissionStatus();
        }
    }
}
