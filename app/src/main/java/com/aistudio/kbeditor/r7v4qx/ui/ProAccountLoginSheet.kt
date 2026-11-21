package com.aistudio.kbeditor.r7v4qx.ui

import android.accounts.AccountManager
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.kbeditor.r7v4qx.storage.ProAccountState
import com.aistudio.kbeditor.r7v4qx.ui.theme.*

@Composable
fun ProAccountLoginDialog(
    accountState: ProAccountState,
    onLogin: (String, String) -> Unit,
    onLogout: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(accountState.userName) }
    var email by remember { mutableStateOf(accountState.userEmail) }
    val gmailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val pickedEmail = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME).orEmpty().trim()
            if (pickedEmail.isNotBlank()) {
                val derivedName = name.trim().ifBlank {
                    pickedEmail.substringBefore("@").replaceFirstChar { it.uppercase() }
                }
                email = pickedEmail
                name = derivedName
                onLogin(derivedName, pickedEmail)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SlateBase,
        titleContentColor = TextPrimary,
        textContentColor = TextPrimary,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Stars, contentDescription = null, tint = WarmAmber)
                Text(
                    text = if (accountState.isLoggedIn) "KB Studio Pro Plan" else "Pro Login • 50 Coins Daily",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SlateElevated, RoundedCornerShape(10.dp))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (accountState.isLoggedIn) {
                                "Active Balance: ${accountState.dailyCoins} / 50 Coins Today"
                            } else {
                                "Sign in to unlock 50 Free Pro Coins every day"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = WarmAmber,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Includes Gemini 2.5 Flash Auto-Edit, AI Color Grade & Google Flow AI Launcher.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                if (accountState.isLoggedIn) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ElectricCyan)
                        Column {
                            Text(accountState.userName, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                            Text(accountState.userEmail, style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = {
                            val intent = AccountManager.newChooseAccountIntent(
                                null, null, arrayOf("com.google"), null, null, null, null
                            )
                            gmailPickerLauncher.launch(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, tint = ElectricCyan)
                        Spacer(Modifier.width(8.dp))
                        Text("Continue with Gmail", color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                    val tfColors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = SlateBorder,
                        focusedLabelColor = ElectricCyan,
                        unfocusedLabelColor = TextMuted,
                        cursorColor = ElectricCyan
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email / Channel ID") },
                        singleLine = true,
                        colors = tfColors,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (accountState.isLoggedIn) {
                Button(onClick = onDismiss) {
                    Text("Continue Editing")
                }
            } else {
                Button(onClick = { onLogin(name, email) }) {
                    Text("Login & Claim 50 Coins")
                }
            }
        },
        dismissButton = {
            if (accountState.isLoggedIn) {
                TextButton(onClick = onLogout) {
                    Text("Sign Out", color = WarmAmber)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Later", color = TextMuted)
                }
            }
        }
    )
}
