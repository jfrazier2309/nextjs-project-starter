package com.beaker.playsmartcards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beaker.playsmartcards.ui.theme.PlaySmartcardsTheme

class LearnPokerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaySmartcardsTheme {
                LearnPokerScreen(
                    onBackClicked = { finish() },
                    onWalkthroughClicked = { navigateToWalkthrough() },
                    onGuidedPlaythroughClicked = { navigateToGuidedPlaythrough() },
                    onPlayGameClicked = { navigateToPlayGame() }
                )
            }
        }
    }

    private fun navigateToWalkthrough() {
        startActivity(Intent(this, WalkthroughActivity::class.java))
    }

    private fun navigateToGuidedPlaythrough() {
        startActivity(Intent(this, GuidedPlaythroughActivity::class.java))
    }

    private fun navigateToPlayGame() {
        startActivity(Intent(this, PlayGameActivity::class.java))
    }
}

@Composable
fun LearnPokerScreen(
    onBackClicked: () -> Unit,
    onWalkthroughClicked: () -> Unit,
    onGuidedPlaythroughClicked: () -> Unit,
    onPlayGameClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF004d40)) // Improved green felt background
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        // Back Button
        Button(
            onClick = onBackClicked,
            modifier = Modifier
                .align(Alignment.Start)
                .padding(bottom = 16.dp),
            shape = CircleShape
        ) {
            Text(
                text = "Back",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title Text
        Text(
            text = "Learn to Play Poker",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Buttons with improved styling
        PokerOptionButton(
            text = "Walkthrough",
            onClick = onWalkthroughClicked
        )

        PokerOptionButton(
            text = "Guided Playthrough",
            onClick = onGuidedPlaythroughClicked
        )

        PokerOptionButton(
            text = "Play Game",
            onClick = onPlayGameClicked
        )
    }
}

@Composable
fun PokerOptionButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(0.8f),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = Color(0xFF6200EE)
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}
