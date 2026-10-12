package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.BudgetRepository
import com.example.ui.AuthScreen
import com.example.ui.BudgetViewModel
import com.example.ui.MainFeatureScreen
import com.example.ui.attemptAutoSignIn
import com.example.ui.theme.VaultFlowTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VaultFlowTheme {
                AppRootContent()
            }
        }
    }
}

@Composable
fun AppRootContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    // Listen to Firebase Auth state
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)

        // Attempt silent background sign-in on cold start if not yet authenticated
        if (Firebase.auth.currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = Firebase.auth.currentUser
                },
                onUnauthenticated = {
                    currentUser = null
                },
                scope = scope
            )
        }

        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    val user = currentUser
    if (user == null) {
        // Auth Gated: feature ViewModels & Firestore queries are not created until login
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        // Authenticated Session: Inject custom database ID into BudgetRepository
        val viewModel: BudgetViewModel = viewModel(
            key = user.uid,
            factory = viewModelFactory {
                initializer {
                    val dbId = context.getString(R.string.firestore_database_id)
                    val db = FirebaseFirestore.getInstance(dbId)
                    val repository = BudgetRepository(db)
                    BudgetViewModel(repository, user.uid)
                }
            }
        )

        MainFeatureScreen(
            viewModel = viewModel,
            onSignOutRequested = {
                currentUser = null
            }
        )
    }
}
