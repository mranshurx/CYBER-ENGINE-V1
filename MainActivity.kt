package com.yourname.freefireinjector

import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var btnInject: Button
    private lateinit var tvStatus: TextView
    private lateinit var pbProgress: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnInject = findViewById(R.id.btnInject)
        tvStatus = findViewById(R.id.tvStatus)
        pbProgress = findViewById(R.id.pbProgress)

        btnInject.setOnClickListener {
            if (!checkPermissions()) {
                tvStatus.text = "Storage permission denied"
                return@setOnClickListener
            }

            pbProgress.visibility = android.view.View.VISIBLE
            tvStatus.text = "Injecting..."

            // Run in background
            Thread {
                try {
                    val files = assets.list("anshu-on-top")?.toList() ?: emptyList()
                    if (files.isEmpty()) {
                        runOnUiThread {
                            tvStatus.text = "No files found in assets/anshu-on-top"
                            pbProgress.visibility = android.view.View.GONE
                        }
                        return@Thread
                    }

                    val destDir = File(
                        Environment.getExternalStorageDirectory(),
                        "Android/data/com.dts.freefire_max/files/anshu-on-top"
                    ).apply { mkdirs() }

                    var successCount = 0
                    files.forEach { fileName ->
                        copyAssetFile("anshu-on-top/$fileName", destDir)
                        successCount++
                    }

                    runOnUiThread {
                        tvStatus.text = "Injected $successCount files successfully!"
                        pbProgress.visibility = android.view.View.GONE
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        tvStatus.text = "Error: ${e.message}"
                        pbProgress.visibility = android.view.View.GONE
                    }
                }
            }.start()
        }
    }

    private fun copyAssetFile(assetPath: String, destDir: File) {
        val destFile = File(destDir, File(assetPath).name)
        assets.open(assetPath).use { inputStream ->
            FileOutputStream(destFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
    }

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            // For API <29, just check individual permissions
            true // Simplified; you can add RuntimePermissions if needed
        }
    }
}
