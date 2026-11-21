package com.aistudio.kbeditor.r7v4qx

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.aistudio.kbeditor.r7v4qx.media.AntiBugCrashGuard
import com.aistudio.kbeditor.r7v4qx.ui.KbEditorScreen
import com.aistudio.kbeditor.r7v4qx.ui.theme.KbEditorTheme
import com.aistudio.kbeditor.r7v4qx.ui.theme.SlateVoid

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AntiBugCrashGuard.install(applicationContext)
        requestMissingStudioPermissions()
        enableEdgeToEdge()
        setContent {
            KbEditorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SlateVoid
                ) {
                    KbEditorScreen()
                }
            }
        }
    }

    private fun requestMissingStudioPermissions() {
        val needed = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_VIDEO)
                add(Manifest.permission.READ_MEDIA_IMAGES)
                add(Manifest.permission.READ_MEDIA_AUDIO)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }.filter { perm ->
            ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }
}
