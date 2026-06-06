package com.example.connect.ui

import android.os.Build
import android.widget.ToggleButton
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.connect.model.User
import com.example.connect.repository.FirestoreRepository
import com.example.connect.viewmodel.SettingsViewModel
import com.example.connect.viewmodel.SettingsViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun SettingsScreen(
    repository: FirestoreRepository,
    navController: NavController,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
)  {
    val auth = Firebase.auth
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(repository))
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = "Settings",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = 24.dp)
            )
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp)
            )
            SettingsRow(
                title = "Dark Mode",
                subtitle = "Turn on/off dark mode.",
                onClick = {},
                toggleableState = true,
                isChecked = isDarkMode,
                onCheckedChange = onDarkModeChange
            )
            SettingsRow("Delete Account", "Delete your account forever.", onClick ={ showDeleteDialog=true }, toggleableState = false)
            SettingsRow("Logout","Log out of your account.", onClick = {
                showLogoutDialog=true
            }, toggleableState = false)
        }
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Account") },
                text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        viewModel.deleteAccount()
                        showDeleteDialog = false
                    }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Log Out") },
                text = { Text("Are you sure you want to log out from your account?") },
                confirmButton = {
                    androidx.compose.material3.TextButton(onClick = {
                        auth.signOut()
                        showLogoutDialog = false
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }) {
                        Text("Log Out", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Text(
            text = "Version: 1.0",
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 850.dp)
        )
        Text(
            text = "© Copyrights - RjR & AlaBe Devs, 2026",
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 870.dp)
        )
    }
}


@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit,
    toggleableState: Boolean,
    isChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    ListItem(
        headlineContent = { Text(title, color = textColor, fontWeight = FontWeight.Medium) },
        supportingContent = subtitle?.let { { Text(it) } },
        trailingContent = {

            if (toggleableState) {
                androidx.compose.material3.Switch(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
            .clickable { onClick() },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
    )
}