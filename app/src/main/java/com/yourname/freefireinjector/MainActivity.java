package com.yourname.freefireinjector;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class MainActivity extends AppCompatActivity {

    private static final int SHIZUKU_PERMISSION_REQUEST_CODE = 1001;
    private TextView statusText;

    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = (requestCode, grantResult) -> {
        if (requestCode == SHIZUKU_PERMISSION_REQUEST_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                statusText.setText("Shizuku Permission Granted!\nInjecting assets...");
                injectAssetsToFreeFire();
            } else {
                statusText.setText("Shizuku Permission Denied!");
                Toast.makeText(this, "Shizuku permission is required for injection!", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);

        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
        checkAndRequestShizukuPermission();
    }

    private void checkAndRequestShizukuPermission() {
        if (Shizuku.isPreV11()) {
            statusText.setText("Shizuku pre-v11 is not supported.");
            return;
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            statusText.setText("Shizuku Ready.\nInjecting assets...");
            injectAssetsToFreeFire();
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            statusText.setText("Please grant Shizuku permission in the Shizuku app.");
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
        } else {
            Shizuku.requestPermission(SHIZUKU_PERMISSION_REQUEST_CODE);
        }
    }

    private void injectAssetsToFreeFire() {
        try {
            File cacheDir = new File(getCacheDir(), "anshu_temp");
            if (!cacheDir.exists()) cacheDir.mkdirs();

            String[] assets = getAssets().list("anshu-on-top");
            if (assets == null) return;

            for (String filename : assets) {
                InputStream in = getAssets().open("anshu-on-top/" + filename);
                File outFile = new File(cacheDir, filename);
                OutputStream out = new FileOutputStream(outFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();

                // Use Shizuku command via reflection to bypass private access restriction in API v13
                String targetDir = "/data/data/com.dts.freefireth/files/";
                String cmd = "mkdir -p " + targetDir + " && cp " + outFile.getAbsolutePath() + " " + targetDir + filename + " && chmod 777 " + targetDir + filename;
                
                Class<?> shizukuClass = Class.forName("rikka.shizuku.Shizuku");
                Method newProcessMethod = shizukuClass.getDeclaredMethod("newProcess", String[].class, String[].class, String.class);
                newProcessMethod.setAccessible(true);
                Process process = (Process) newProcessMethod.invoke(null, new String[]{"sh", "-c", cmd}, null, null);
                if (process != null) {
                    process.waitFor();
                }
            }

            statusText.setText("Injection Successful!\nAssets copied to Free Fire.");
            Toast.makeText(this, "Successfully injected via Shizuku!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            statusText.setText("Injection Failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
    }
}
