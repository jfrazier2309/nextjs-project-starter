package com.beaker.playsmartcards

import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.beaker.playsmartcards.logic.ActionType
import com.beaker.playsmartcards.logic.Player
import com.beaker.playsmartcards.logic.Stage
import com.beaker.playsmartcards.shared.Card
import com.beaker.playsmartcards.shared.PokerCardShared
import com.beaker.playsmartcards.ui.theme.PlaySmartcardsTheme
import kotlinx.coroutines.flow.collectLatest

class PlayGameActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaySmartcardsTheme {
                PlayGameScreen(onExit = { finish() })
            }
        }
    }
}

@Composable
fun FlippableCard(card: Card?, isFaceUp: Boolean, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (isFaceUp) 0f else 180f,
        animationSpec = tween(durationMillis = 600),
        label = "rotation"
    )
    val density = LocalDensity.current.density
    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            cameraDistance = 8 * density
        }
    ) {
        if (rotation <= 90f) {
            PokerCardShared(card = card, cardHeight = 100.dp, cardWidth = 70.dp)
        } else {
            PokerCardShared(card = null, cardHeight = 100.dp, cardWidth = 70.dp)
        }
    }
}

@Composable
fun PlayGameScreen(
    onExit: () -> Unit,
    viewModel: PlayGameViewModel = viewModel()
) {
    // existing UI state…
    val players              by viewModel.players.collectAsState()
    val communityCards       by viewModel.communityCards.collectAsState()
    val stage                by viewModel.stage.collectAsState()
    val pot                  by viewModel.pot.collectAsState()
    val message              by viewModel.message.collectAsState()
    val currentActorIndex    by viewModel.currentActorIndex.collectAsState()
    val amountToCall         by viewModel.amountToCall.collectAsState()

    // new lines:
    val currentBet           by viewModel.currentBet.collectAsState()
    val bigBlindAmount       = viewModel.bigBlindAmount

    val humanPlayer          = players.firstOrNull()

    // stats & settings state…
    val handsPlayed          by viewModel.handsPlayed.collectAsState()
    val playerWins           by viewModel.playerWins.collectAsState()
    val soundEnabled         by viewModel.soundEnabled.collectAsState()
    val difficulty           by viewModel.difficulty.collectAsState()

    var showSettings         by remember { mutableStateOf(false) }
    var showCheatSheet       by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val soundPlayers = remember {
        mapOf(
            SoundEvent.CARD_FLIP to MediaPlayer.create(context, R.raw.card_flip),
            SoundEvent.CHIP      to MediaPlayer.create(context, R.raw.chip_sound),
            SoundEvent.SHUFFLE   to MediaPlayer.create(context, R.raw.card_suffling)
        )
    }

    LaunchedEffect(Unit) {
        viewModel.soundEvent.collectLatest { event ->
            if (soundEnabled) {
                soundPlayers[event]?.start()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundPlayers.values.forEach { it.release() }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF003300))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            StatsDisplay(
                playerChips  = players.getOrNull(0)?.chips ?: 0,
                bot1Chips    = players.getOrNull(1)?.chips ?: 0,
                bot2Chips    = players.getOrNull(2)?.chips ?: 0,
                pot          = pot,
                playerWins   = playerWins,
                handsPlayed  = handsPlayed
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (players.size > 1) PlayerDisplay(player = players[1], stage = stage)
                if (players.size > 2) PlayerDisplay(player = players[2], stage = stage)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CommunityCardDisplay(communityCards = communityCards)
                Spacer(Modifier.height(8.dp))
                Text(text = message, color = Color.Yellow, modifier = Modifier.padding(8.dp))
            }

            if (humanPlayer != null) {
                PlayerDisplay(player = humanPlayer, isHuman = true, stage = stage)
            }

            // ─── Updated bottom controls ───────────────────────────────────
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 1) Next Hand button when the round is done
                if (stage == Stage.SHOWDOWN || stage == Stage.HAND_OVER) {
                    Button(onClick = { viewModel.startNewRound() }) {
                        Text("Next Hand")
                    }
                }

                // 2) Betting controls on the human’s turn
                if (currentActorIndex == 0 && humanPlayer != null) {
                    PlayerActions(
                        onFold       = { viewModel.handlePlayerAction(ActionType.FOLD) },
                        onCheck      = { viewModel.handlePlayerAction(ActionType.CHECK) },
                        onCall       = { viewModel.handlePlayerAction(ActionType.CALL) },
                        onRaise      = { amt -> viewModel.handlePlayerAction(ActionType.RAISE, amt) },
                        amountToCall = amountToCall,
                        playerChips  = humanPlayer.chips,
                        currentBet   = currentBet,
                        bigBlind     = bigBlindAmount
                    )
                }

                Spacer(Modifier.height(8.dp))

                // 3) Always show Help / Settings / Exit
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(onClick = { showCheatSheet = true }) { Text("Help") }
                    Button(onClick = { showSettings   = true }) { Text("Settings") }
                    Button(onClick = onExit)                { Text("Exit Game") }
                }
            }
            // ────────────────────────────────────────────────────────────────
        }

        AnimatedVisibility(
            visible = showCheatSheet,
            enter = slideInHorizontally(initialOffsetX = { it }),
            exit  = slideOutHorizontally(targetOffsetX = { it })
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.TopEnd)
            ) {
                CheatSheetDrawer(onClose = { showCheatSheet = false })
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            soundEnabled = soundEnabled,
            onToggleSound = { viewModel.toggleSound() },
            currentDifficulty = difficulty,
            onChangeDifficulty = { viewModel.changeDifficulty() },
            onClose = { showSettings = false }
        )
    }
}

@Composable
fun StatsDisplay(playerChips: Int, bot1Chips: Int, bot2Chips: Int, pot: Int, playerWins: Int, handsPlayed: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x33000000), RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text("Player Wins: $playerWins", color = Color.White)
                Text("Hands Played: $handsPlayed", color = Color.White)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pot: $$pot", color = Color.White, fontSize = 20.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Your Chips: $$playerChips", color = Color.White)
                Text("Bot 1 Chips: $$bot1Chips", color = Color.White)
                Text("Bot 2 Chips: $$bot2Chips", color = Color.White)
            }
        }
    }
}

@Composable
fun PlayerDisplay(player: Player, isHuman: Boolean = false, stage: Stage) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Row {
            val showCards = isHuman || stage == Stage.SHOWDOWN
            if (player.hand.size >= 2) {
                FlippableCard(card = player.hand[0], isFaceUp = showCards)
                Spacer(modifier = Modifier.width(4.dp))
                FlippableCard(card = player.hand[1], isFaceUp = showCards)
            } else {
                PokerCardShared(card = null)
                Spacer(modifier = Modifier.width(4.dp))
                PokerCardShared(card = null)
            }
        }
    }
}

@Composable
fun CommunityCardDisplay(communityCards: List<Card>) {
    Row(
        modifier = Modifier.padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 5) {
            val card = communityCards.getOrNull(i)
            FlippableCard(card = card, isFaceUp = card != null)
        }
    }
}

@Composable
fun PlayerActions(
    onFold: () -> Unit,
    onCheck: () -> Unit,
    onCall: () -> Unit,
    onRaise: (Int) -> Unit,
    amountToCall: Int,
    playerChips: Int,
    currentBet: Int,
    bigBlind: Int
) {
    // Calculate minimum and maximum legal raise
    val minRaise = maxOf(bigBlind, currentBet)
    val raiseLower = amountToCall + minRaise
    val raiseUpper = playerChips

    // Initialize and clamp the raise amount
    var raiseAmount by remember {
        mutableIntStateOf(raiseLower.coerceAtMost(raiseUpper))
    }
    // ensure it stays in bounds
    raiseAmount = raiseAmount.coerceIn(raiseLower, raiseUpper)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (playerChips > amountToCall) {
            Text(text = "Raise Amount: $$raiseAmount", color = Color.White)
            Slider(
                value = raiseAmount.toFloat(),
                onValueChange = { newValue ->
                    // snap to multiples of 10 and clamp
                    val snapped = (newValue.toInt() / 10) * 10
                    raiseAmount = snapped.coerceIn(raiseLower, raiseUpper)
                },
                valueRange = raiseLower.toFloat()..raiseUpper.toFloat(),
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = onFold) {
                Text("Fold")
            }
            if (amountToCall == 0) {
                Button(onClick = onCheck) {
                    Text("Check")
                }
            } else {
                Button(
                    onClick = onCall,
                    enabled = playerChips >= amountToCall
                ) {
                    Text("Call ($$amountToCall)")
                }
            }
            Button(
                onClick = { onRaise(raiseAmount) },
                enabled = playerChips > amountToCall
            ) {
                Text("Raise")
            }
        }
    }
}


@Composable
fun SettingsDialog(soundEnabled: Boolean, onToggleSound: () -> Unit, currentDifficulty: String, onChangeDifficulty: () -> Unit, onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Settings") },
        text = {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sound Enabled")
                    Switch(checked = soundEnabled, onCheckedChange = { onToggleSound() })
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onChangeDifficulty) {
                    Text("Difficulty: $currentDifficulty")
                }
            }
        },
        confirmButton = {
            Button(onClick = onClose) { Text("Close") }
        }
    )
}

@Composable
fun CheatSheetDrawer(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(280.dp)
            .background(Color(0xE6222222))
            .padding(16.dp)
    ) {
        Text("Poker Hand Rankings", color = Color.White, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Royal Flush (A-K-Q-J-10, same suit)", color = Color.Yellow)
        Text("Straight Flush (5 cards in sequence, same suit)", color = Color.White)
        Text("Four of a Kind", color = Color.White)
        Text("Full House (3 of a kind + a pair)", color = Color.White)
        Text("Flush (5 cards, same suit)", color = Color.White)
        Text("Straight (5 cards in sequence)", color = Color.White)
        Text("Three of a Kind", color = Color.White)
        Text("Two Pair", color = Color.White)
        Text("One Pair", color = Color.White)
        Text("High Card", color = Color.White)
        Spacer(modifier = Modifier.weight(1f))
        Button(onClick = onClose) {
            Text("Close")
        }
    }
}