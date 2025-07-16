package com.beaker.playsmartcards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beaker.playsmartcards.ui.theme.PlaySmartcardsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaySmartcardsTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    StartGameScreen(onStartClicked = { navigateToGameOptions() })
                }
            }
        }
    }

    private fun navigateToGameOptions() {
        startActivity(Intent(this, GameOptionsActivity::class.java))
    }
}

@Composable
fun StartGameScreen(onStartClicked: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF004d40)), // Improved color palette
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Add a logo or image at the top
        Image(
            painter = painterResource(id = R.drawable.poker_table), // Replace with your actual drawable
            contentDescription = "App Logo",
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .padding(bottom = 24.dp)
        )

        Text(
            text = "Welcome to Play Smart Cards!",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(16.dp)
        )

        Text(
            text = "Learn casino-style card games and master your skills!",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onStartClicked,
            modifier = Modifier
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Start Game",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Optional footer text
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Have fun and good luck!",
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
