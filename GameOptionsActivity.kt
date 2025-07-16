package com.beaker.playsmartcards

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.beaker.playsmartcards.ui.theme.PlaySmartcardsTheme

class GameOptionsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaySmartcardsTheme {
                GameOptionsScreen(
                    onLearnPokerClicked = { navigateToLearnPoker() },
                    onLearnBlackjackClicked = { /* Not implemented yet */ },
                    onLearnTexasHoldemClicked = { /* Not implemented yet */ },
                    onLearnOmahaClicked = { /* Not implemented yet */ }
                )
            }
        }
    }

    private fun navigateToLearnPoker() {
        startActivity(Intent(this, LearnPokerActivity::class.java))
    }
}
