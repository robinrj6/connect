package com.example.connect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.connect.model.FriendStatus
import com.example.connect.repository.FirestoreRepository
import com.example.connect.viewmodel.FindFriendsViewModel
import com.example.connect.viewmodel.FindFriendsViewModelFactory

@Composable
fun FindFriendsScreen(repository: FirestoreRepository) {
    val viewModel: FindFriendsViewModel = viewModel(factory = FindFriendsViewModelFactory(repository))

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Find Friends",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 48.dp, bottom = 16.dp)
            )

            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.onQueryChange(it) },
                label = { Text("Search by name or email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (viewModel.isSearching) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(viewModel.searchResults, key = { it.user.uid }) { user ->
                        ListItem(
                            headlineContent = { 
                                Text(
                                    user.user.name,
                                    color = MaterialTheme.colorScheme.onSurface
                                ) 
                            },
                            supportingContent = { 
                                Text(
                                    user.user.email,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            trailingContent = {
                                Button(
                                    enabled = user.status == FriendStatus.NONE,
                                    onClick = { viewModel.onClickAddFriend(user.user) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                ) {
                                    Text(
                                        text = when (user.status) {
                                            FriendStatus.NONE -> "Add"
                                            FriendStatus.FRIEND -> "Friend"
                                            FriendStatus.SENT -> "Sent"
                                            FriendStatus.PENDING -> "Pending"
                                        },
                                    )
                                }
                            }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}
