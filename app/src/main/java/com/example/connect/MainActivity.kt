package com.example.connect

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.connect.model.User
import com.example.connect.repository.FirestoreRepository
import com.example.connect.ui.FeelingScreen
import com.example.connect.ui.FindFriendsScreen
import com.example.connect.ui.HomeScreen
import com.example.connect.ui.LoginScreen
import com.example.connect.ui.SettingsScreen
import com.example.connect.ui.theme.ConnectTheme
import com.example.connect.viewmodel.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore

class MainActivity : ComponentActivity() {

    private var navigateToHome: (() -> Unit)? = null
    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore
    private val repository = FirestoreRepository(db)

    private val loginViewModel: LoginViewModel by lazy {
        LoginViewModel(repository)
    }
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnSuccessListener {
                Log.d("AUTH", "Sign in successful")
                loginViewModel.successLogin(
                    it.user?.uid ?: "",
                    it.user?.displayName ?: "",
                    it.user?.email ?: ""
                )
                navigateToHome?.invoke()
            }
        } catch (e: Exception) {
            Log.e("AUTH", "Sign in failed", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        setContent {
            val sharedPrefs = remember { getSharedPreferences("connect_preferences", MODE_PRIVATE) }
            val systemIsDarkTheme = isSystemInDarkTheme()
            var isDarkTheme by rememberSaveable {
                mutableStateOf(
                    sharedPrefs.getBoolean("dark_mode_enabled", systemIsDarkTheme)
                )
            }

            LaunchedEffect(isDarkTheme) {
                sharedPrefs.edit()
                    .putBoolean("dark_mode_enabled", isDarkTheme)
                    .apply()
            }

            ConnectTheme(darkTheme = isDarkTheme) {
                val currentUser = remember { mutableStateOf(auth.currentUser) }
                val navController = rememberNavController()
                navigateToHome = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener {
                        currentUser.value = it.currentUser
                        if (currentUser.value == null) {
                            navController.navigate("login") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }

                NavHost(
                    navController = navController,
                    startDestination = if (currentUser.value == null) "login" else "home",
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None }
                ) {
                    composable("login") {
                        LoginScreen {
                            val gso =
                                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                    .requestIdToken(getString(R.string.default_web_client_id))
                                    .requestEmail()
                                    .build()
                            val client = GoogleSignIn.getClient(this@MainActivity, gso)
                            client.signOut().addOnCompleteListener {
                                signInLauncher.launch(client.signInIntent)
                            }
                        }
                    }
                    composable("home") {
                        HomeScreen(
                            userName = currentUser.value?.displayName ?: "", repository = repository,
                            navController = navController)
                    }
                    composable("find_friends") {
                        FindFriendsScreen(repository=repository)
                    }
                    composable("feeling/{uid}") {backStackEntry ->
                        val uid = backStackEntry.arguments?.getString("uid") ?: ""
                        FeelingScreen(user = User(uid = uid), repository =repository)
                    }
                    composable("settings") {
                        SettingsScreen(
                            repository = repository,
                            navController = navController,
                            isDarkMode = isDarkTheme,
                            onDarkModeChange = { isDarkTheme = it }
                        )
                    }
                }
            }
        }
    }
}
