package com.beaker.playsmartcards

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beaker.playsmartcards.ui.theme.PlaySmartcardsTheme

@Composable
fun GameOptionsScreen(
    onLearnPokerClicked: () -> Unit,
    onLearnBlackjackClicked: () -> Unit,
    onLearnTexasHoldemClicked: () -> Unit,
    onLearnOmahaClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF004d40)) // Improved green felt-like background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()), // Scroll if needed
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Decorative row of chips at the top
            DecorativeChipsRow()

            Spacer(modifier = Modifier.height(24.dp))

            // Card-like box for options
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(3.dp, Color.White, RoundedCornerShape(16.dp))
                    .background(Color(0xFF003d30), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Select an Option",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    OptionButton("Learn to Play Basic Poker", onLearnPokerClicked, true)
                    OptionButton("Learn Texas Hold'em (Coming Soon)", onLearnTexasHoldemClicked, false)
                    OptionButton("Learn Omaha Poker (Coming Soon)", onLearnOmahaClicked, false)
                    OptionButton("Learn to Play Blackjack (Coming Soon)", onLearnBlackjackClicked, false)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Decorative row of chips at the bottom
            DecorativeChipsRow()
        }
    }
}

@Composable
fun DecorativeChipsRow() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(5) {
            Image(
                painter = painterResource(id = R.drawable.chip),
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp) // Larger chip size
                    .padding(6.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun OptionButton(text: String, onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(0.8f),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (enabled) Color(0xFF6200EE) else Color.Gray
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
