package com.example.installer;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int PICK_OBB = 100;
    private static final int PICK_FOLDER = 101;

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

        Button selectObb = new Button(this);
        selectObb.setText("Pilih File OBB");

        Button selectFolder = new Button(this);
        selectFolder.setText("Pilih Folder Android/obb");

        status = new TextView(this);
        status.setText("Belum ada file OBB.");
        status.setTextSize(16);

        layout.addView(title,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

        layout.addView(selectObb);

        layout.addView(selectFolder);

        layout.addView(status);

        setContentView(layout);

        selectObb.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_OBB);
        });

        selectFolder.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            startActivityForResult(intent, PICK_FOLDER);
        });
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == PICK_OBB) {
            status.setText(
                    "OBB dipilih:\n" +
                    uri.toString() +
                    "\n\nSekarang pilih folder Android/obb.");
        }

        if (requestCode == PICK_FOLDER) {
            int flags =
                    data.getFlags()
                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            getContentResolver().takePersistableUriPermission(uri, flags);

            status.setText(
                    "Folder dipilih:\n" +
                    uri.toString() +
                    "\n\nAkses folder berhasil diberikan.");
        }
    }
}
