package com.example.connect.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connect.model.User
import com.example.connect.model.allEmojis
import com.example.connect.repository.FirestoreRepository
import com.example.connect.viewmodel.FeelingViewModel
import com.example.connect.viewmodel.FeelingViewModelFactory
import com.example.connect.viewmodel.HomeViewModel
import com.example.connect.viewmodel.HomeViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
private fun FloatingPlusOne(color: Color) {
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(700)
        visible = false
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(100)) + slideInVertically(tween(700)) { it / 2 },
        exit = fadeOut(tween(400)) + slideOutVertically(tween(700)) { -it },
        modifier = Modifier.offset(y = (-64).dp)
    ) {
        Text(
            text = "+1",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun FeelingScreen(
    user: User, repository: FirestoreRepository
) {
    val viewModel: FeelingViewModel = viewModel(factory = FeelingViewModelFactory(repository))
    var tapCount by remember { mutableIntStateOf(0) }
    var controlsEnabled by remember(user.uid) { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(user.uid) {
        tapCount = 0
        viewModel.loadInitial(user.uid)
    }

    LaunchedEffect(viewModel.isLoading) {
        controlsEnabled = !viewModel.isLoading
    }

    LaunchedEffect(viewModel.error) {
        viewModel.error?.let {
            scope.launch { snackbarHostState.showSnackbar(it) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
    Surface(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        color = MaterialTheme.colorScheme.background
    ) {
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Surface
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp)
            ) {
                Text(
                    text = if(viewModel.nickName == "") viewModel.userName else viewModel.nickName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row (
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val appContext = LocalContext.current.applicationContext
                    val viewHModel: HomeViewModel = viewModel(factory = HomeViewModelFactory(repository, appContext))
                    ReceivedEmojiCountsRow(
                        totalCounts = viewHModel.receivedEmojiCounts(user.uid),
                        newCounts = viewHModel.receivedEmojiCountsSinceLastVisit(user.uid),
                        onOpenFriend = { },
                        false
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val currentCount = viewModel.counts[viewModel.selectedEmoji.emoji] ?: 0
                val haptics = LocalHapticFeedback.current

                Text(
                    text = if (currentCount == 0) "Tap to send" else "×$currentCount ${viewModel.selectedEmoji.label}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(contentAlignment = Alignment.TopCenter) {
                    Button(
                        enabled = controlsEnabled,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                            viewModel.onEmojiClick(receiverId = user.uid)
                            tapCount++
                        },
                        shape = RoundedCornerShape(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.size(180.dp)
                    ) {
                        Text(text = viewModel.selectedEmoji.emoji, fontSize = 72.sp)
                    }

                    key(tapCount) {
                        if (tapCount > 0) {
                            FloatingPlusOne(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Box(modifier = Modifier.padding(bottom = 48.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    OutlinedButton(
                        enabled = controlsEnabled,
                        onClick = { viewModel.toggleDropdown() },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "${viewModel.selectedEmoji.emoji}  ${viewModel.selectedEmoji.label}",
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "▼",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        enabled = controlsEnabled,
                        onClick = { viewModel.setSelectedAsDefault(user.uid) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.tertiary,
                            contentColor = MaterialTheme.colorScheme.onTertiary
                        )
                    ) {
                        val isDefault = viewModel.currentDefaultEmoji == viewModel.selectedEmoji.emoji
                        Text(if (isDefault) "Default ✓" else "Set as default ")
                    }

                    DropdownMenu(
                        expanded = viewModel.dropdownOpen,
                        onDismissRequest = { viewModel.toggleDropdown() }
                    ) {
                        allEmojis.forEach { action ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(text = action.emoji, fontSize = 24.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = action.label,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val c = viewModel.counts[action.emoji] ?: 0
                                        if (c > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "×$c",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (viewModel.currentDefaultEmoji == action.emoji) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "default",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                },
                                onClick = { viewModel.onEmojiSelected(action) }
                            )
                        }
                    }
                }
            }
        }
    }
    } // Scaffold
}
