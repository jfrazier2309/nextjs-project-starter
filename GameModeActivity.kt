package com.beaker.playsmartcards

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.beaker.playsmartcards.ui.theme.PlaySmartcardsTheme
import androidx.compose.foundation.background // For background modifier
import androidx.compose.ui.graphics.Color      // For Color class

class GameModeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaySmartcardsTheme {
                GameModeScreen()
            }
        }
    }
}

@Composable
fun GameModeScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF006400)) // Add consistent green background
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Game Mode",
            color = Color.White, // Text color to contrast with the green background
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(onClick = { /* Navigate to Walkthrough */ }) {
            Text("Start Walkthrough")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { /* Start the game logic */ }) {
            Text("Start Game")
        }
    }
}
