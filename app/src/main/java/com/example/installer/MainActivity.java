package com.example.installer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int PICK_OBB = 100;

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView title = new TextView(this);
        title.setText("OBB Installer");
        title.setTextSize(28);

        TextView description = new TextView(this);
        description.setText(
                "Pilih file OBB yang ingin dipasang.\n" +
                "Lokasi tujuan akan ditentukan otomatis."
        );
        description.setTextSize(16);

        Button selectObb = new Button(this);
        selectObb.setText("Pilih File OBB");

        status = new TextView(this);
        status.setText("Belum ada file OBB.");
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

        layout.addView(selectObb);

        layout.addView(
                status,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(layout);

        selectObb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, PICK_OBB);
        });
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != PICK_OBB ||
                resultCode != RESULT_OK ||
                data == null ||
                data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        String fileName = uri.getLastPathSegment();

        if (fileName == null) {
            fileName = "OBB";
        }

        status.setText(
                "OBB dipilih:\n\n" +
                fileName +
                "\n\nMenentukan lokasi Android/obb..."
        );

        /*
         * Tahap berikutnya:
         *
         * 1. Baca nama file OBB.
         * 2. Tentukan package game.
         * 3. Tentukan Android/obb/<package>.
         * 4. Minta akses Android yang diperlukan.
         * 5. Buat folder package.
         * 6. Salin OBB.
         */
    }
}
