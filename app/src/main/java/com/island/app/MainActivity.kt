package com.island.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private var tick by mutableIntStateOf(0)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(Modifier.fillMaxSize()) {
                    Setup(tick) { notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }
}

@Composable
private fun Setup(tick: Int, askNotifications: () -> Unit) {
    val ctx = LocalContext.current
    val overlayOk = remember(tick) { Settings.canDrawOverlays(ctx) }
    val listenerOk = remember(tick) {
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
    }
    val notifOk = remember(tick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Island", fontSize = 32.sp)
        Text("Dynamic Island for Android: music, recording and charging power.")

        Step("Draw over other apps", overlayOk) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}")),
            )
        }
        Step("Notification access (for music)", listenerOk) {
            ctx.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        if (Build.VERSION.SDK_INT >= 33) {
            Step("Allow notifications", notifOk, askNotifications)
        }

        Button(
            onClick = { startIsland(ctx) },
            enabled = overlayOk,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Start Island") }

        OutlinedButton(
            onClick = { ctx.stopService(Intent(ctx, IslandService::class.java)) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Stop Island") }
    }
}

@Composable
private fun Step(label: String, done: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text((if (done) "\u2705  " else "\u2B1C  ") + label, modifier = Modifier.weight(1f))
        if (!done) OutlinedButton(onClick = onClick) { Text("Grant") }
    }
}

private fun startIsland(ctx: Context) {
    ContextCompat.startForegroundService(ctx, Intent(ctx, IslandService::class.java))
}
