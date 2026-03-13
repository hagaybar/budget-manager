package com.budgetmanager.app.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.budgetmanager.app.auth.AuthState
import com.budgetmanager.app.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(24.dp))

        Text("Account", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        when (val auth = uiState.authState) {
            is AuthState.SignedIn -> {
                Text("Signed in as ${auth.name}")
                Text(auth.email, style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.signOut() }) {
                    Text("Sign Out")
                }
            }
            is AuthState.Loading -> {
                Text("Loading...")
            }
            is AuthState.SignedOut -> {
                Text("Not signed in")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Backup", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { /* TODO: Launch file picker */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isExporting
        ) {
            Text("Export Data")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = { /* TODO: Launch file picker */ },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isImporting
        ) {
            Text("Import Data")
        }

        uiState.message?.let { message ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            "BudgetManager v1.0.0",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
