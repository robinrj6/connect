package com.example.connect.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.connect.R
import com.example.connect.model.UserWithStatus
import com.example.connect.repository.FirestoreRepository
import com.example.connect.ui.theme.ErrorRed
import com.example.connect.ui.theme.SuccessGreen
import com.example.connect.viewmodel.HomeViewModel
import com.example.connect.viewmodel.HomeViewModelFactory

@Composable
fun HomeScreen(
    userName: String, repository: FirestoreRepository, navController: NavController
) {
    val appContext = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository, appContext))

    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                viewModel.persistCurrentEmojiSnapshotAsBaseline()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.persistCurrentEmojiSnapshotAsBaseline()
        }
    }

    fun openFeeling(uid: String) {
            navController.navigate("feeling/$uid") {
                launchSingleTop = true
        }
    }
    
    if (viewModel.showNotifications) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissNotifications() },
            title = { Text("Friend Requests") },
            text = {
                if (viewModel.pendingRequests.isEmpty()) {
                    Text("No pending requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn {
                        items(viewModel.pendingRequests, key = { it.user.uid }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(user.user.name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Row {
                                    Button(
                                        onClick = { viewModel.onAcceptRequest(user.user) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SuccessGreen
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text("✓", color = Color.White)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { viewModel.onRejectRequest(user.user) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ErrorRed
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text("✕", color = Color.White)
                                    }
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onDismissNotifications() }) {
                    Text("Close")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 48.dp, start = 24.dp, end = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = userName.split(" ")
                    .take(3)
                    .joinToString("") { it.first().uppercase() },
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.onClickNotification() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_action_name),
                        modifier = Modifier.size(16.dp),
                        contentDescription = "Notifications", // Essential for accessibility
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { navController.navigate("find_friends") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.add_friend),
                        modifier = Modifier.size(16.dp),
                        contentDescription = "Add Friend", // Essential for accessibility
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { navController.navigate("settings") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.settings_cog),
                        modifier = Modifier.size(16.dp),
                        contentDescription = "Settings", // Essential for accessibility
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        val friends = viewModel.friends
        val haptics = LocalHapticFeedback.current

        if (friends.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No one's here yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            var contextUser by remember { mutableStateOf<UserWithStatus?>(null) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            )  {
                items(friends, key = { it.user.uid }) { user ->
                    var showMenu by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .combinedClickable(
                                    onClick = {
                                        openFeeling(user.user.uid)
                                    },
                                    onLongClick = {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        contextUser = user
                                        showMenu = true
                                    },
                                    onLongClickLabel = "Show actions"
                                )
                                .fillMaxWidth()
                                .height(190.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                if (user.fav == true) {
                                    Text("⭐", fontSize = 12.sp, modifier = Modifier.offset(
                                        x = (64).dp
                                    ))
                                }
                                Text(
                                    text = user.user.name.split(" ")
                                        .take(3)
                                        .joinToString("") { it.first().uppercase() },
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = user.nickname?.takeIf { it.isNotBlank() } ?: user.user.name,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                ReceivedEmojiCountsRow(
                                    totalCounts = viewModel.receivedEmojiCounts(user.user.uid),
                                    newCounts = viewModel.receivedEmojiCountsSinceLastVisit(user.user.uid),
                                    onOpenFriend = { openFeeling(user.user.uid)},
                                    true
                                )
                            }
                        }
                        FriendsActionsSheet(
                            viewModel = viewModel,
                            user = user,
                            showMenu = showMenu,
                            onDismiss = { showMenu = false }
                        )

                    }
                }
            }
        }
    }
    } // Surface
}

@Composable
fun FriendsActionsSheet(viewModel: HomeViewModel, user: UserWithStatus, showMenu: Boolean, onDismiss: () -> Unit) {
    var showNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember(user.user.uid, user.nickname) {
        mutableStateOf(user.nickname.orEmpty())
    }

    if (showNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showNicknameDialog = false },
            title = { Text("Add Nickname") },
            text = {
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { nicknameInput = it },
                    singleLine = true,
                    label = { Text("Nickname") },
                    placeholder = { Text("Enter nickname") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setNickname(user, nicknameInput)
                    showNicknameDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNicknameDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = onDismiss,
        offset = DpOffset(140.dp, -180.dp)
    ) {
        DropdownMenuItem(
            text = { Text("Add Nickname") },
            onClick = {
                showNicknameDialog = true
                onDismiss()
            }
        )
        DropdownMenuItem(
            text = {
                // Use explicit boolean check
                val isFav = user.fav == true
                Text(if (isFav) "Unfavorite" else "Favorite")
            },
            onClick = {
                viewModel.favFriend(user, user.fav != true)
                onDismiss()}
        )
        DropdownMenuItem(
            text = { Text("Unfriend", color = MaterialTheme.colorScheme.error) },
            onClick = {
                viewModel.unfriend(user.user)
                onDismiss()
            }
        )
    }
}
