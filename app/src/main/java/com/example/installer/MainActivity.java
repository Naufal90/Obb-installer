package com.example.installer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 40, 40, 40);

        TextView title = new TextView(this);
        title.setText("OBB Installer");
        title.setTextSize(28);

        Button permissionButton = new Button(this);
        permissionButton.setText("Berikan Akses Penyimpanan");

        status = new TextView(this);
        status.setTextSize(18);

        layout.addView(title);
        layout.addView(permissionButton);
        layout.addView(status);

        setContentView(layout);

        updateStatus();

        permissionButton.setOnClickListener(v -> openStoragePermission());
    }

    private void updateStatus() {
        if (Environment.isExternalStorageManager()) {
            status.setText("Status: Akses penyimpanan sudah diberikan.");
        } else {
            status.setText("Status: Akses penyimpanan belum diberikan.");
        }
    }

    private void openStoragePermission() {
        try {
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
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

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }
}
