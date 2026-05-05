package com.minami_studio.kiro

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class SaveToLocalActivity : ComponentActivity() {

    private var sourceUri: Uri? = null

    @SuppressLint("InvalidFragmentVersionForActivityResult")
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { destUri ->
        if (destUri != null && sourceUri != null) {
            try {
                contentResolver.openInputStream(sourceUri!!)?.use { input ->
                    contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        sourceUri = intent.getParcelableExtra(Intent.EXTRA_STREAM)
        if (sourceUri != null) {
            val today = java.time.LocalDate.now().toString()
            createDocumentLauncher.launch("kiro_backup_${today}.zip")
        } else {
            finish()
        }
    }
}
