package com.beaker.playsmartcards

import android.content.Context
import android.graphics.drawable.AnimatedVectorDrawable
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.res.*
import androidx.compose.ui.unit.*
import com.beaker.playsmartcards.ui.theme.PlaySmartcardsTheme
import com.beaker.playsmartcards.shared.*
import kotlinx.coroutines.delay
import androidx.core.graphics.drawable.toBitmap
import com.beaker.playsmartcards.shared.initializeDeckShared
import com.beaker.playsmartcards.shared.HandType
import com.beaker.playsmartcards.shared.PokerCardShared
import com.beaker.playsmartcards.shared.evaluateHand
import com.beaker.playsmartcards.shared.GuidanceManager

class GuidedPlaythroughActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaySmartcardsTheme {
                GuidedPlaythroughScreen(onExit = { finish() })
            }
        }
    }
}

/**
 * Main Composable
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GuidedPlaythroughScreen(onExit: () -> Unit) {
    val context = LocalContext.current

    // Persistent stats
    val sharedPrefs = remember {
        context.getSharedPreferences("poker_stats", Context.MODE_PRIVATE)
    }
    var playerWins by remember { mutableStateOf(sharedPrefs.getInt("player_wins", 0)) }
    var botWins by remember { mutableStateOf(sharedPrefs.getInt("bot_wins", 0)) }
    var ties by remember { mutableStateOf(sharedPrefs.getInt("ties", 0)) }
    var handsPlayed by remember { mutableStateOf(0) } // tracks how many hands completed

    // Hands & Deck
    val playerHand = remember { mutableStateListOf<Card>() }
    val botHand = remember { mutableStateListOf<Card>() }
    val tableCards = remember { mutableStateListOf<Card?>(null, null, null, null, null) }
    val fullDeck = remember { mutableStateListOf<Card>() }

    // Poker State
    var stage by remember { mutableStateOf(0) }
    var pot by remember { mutableStateOf(100) }
    var totalPot by remember { mutableStateOf(0) }
    var sidePot by remember { mutableStateOf(0) } // For all-in scenario

    // Ensure both start with the exact same chip count.
    var playerChips by remember { mutableStateOf(2000) }
    var botChips by remember { mutableStateOf(2000) }

    var message by remember { mutableStateOf("Your Turn! Please bet or fold.") }
    var guidanceMessage by remember { mutableStateOf("") }

    var showSettings by remember { mutableStateOf(false) }
    var showBotCards by remember { mutableStateOf(false) }
    var showTutorial by remember { mutableStateOf(false) } // Step-by-step guide
    var showCheatSheet by remember { mutableStateOf(false) } // New cheat sheet overlay

    var playerScore by remember { mutableStateOf(0) }
    var botScore by remember { mutableStateOf(0) }

    var playerHasActed by remember { mutableStateOf(false) }
    var botHasActed by remember { mutableStateOf(false) }

    var gameOver by remember { mutableStateOf(false) }

    // Media Players
    val cardFlipSound = remember { MediaPlayer.create(context, R.raw.card_flip) }
    val cardShuffleSound = remember { MediaPlayer.create(context, R.raw.card_suffling) }
    val chipSound = remember { MediaPlayer.create(context, R.raw.chip_sound) }

    // Release resources when leaving
    DisposableEffect(Unit) {
        onDispose {
            cardFlipSound?.release()
            cardShuffleSound?.release()
            chipSound?.release()
        }
    }

    // On first load, deal initial hand
    LaunchedEffect(Unit) {
        initializeDeckShared(fullDeck)
        dealInitialHand(
            playerHand = playerHand,
            botHand = botHand,
            tableCards = tableCards,
            fullDeck = fullDeck,
            setMessage = { message = it },
            setStage = { stage = it },
            onShuffleSound = { cardShuffleSound.start() }
        )
        postBlindsAndStart(
            playerChips = playerChips,
            botChips = botChips,
            setPlayerChips = { playerChips = it },
            setBotChips = { botChips = it },
            setPot = { pot = it },
            setMessage = { message = it },
            onChipSound = { chipSound.start() }
        )
        val guidanceString = GuidanceManager.updateGuidanceEnhanced(
            playerHand = playerHand,
            botHand = botHand,
            tableCards = tableCards,
            stage = stage,
            showBotCards = showBotCards
        )
        guidanceMessage = guidanceString
    }

    // Bot acts after player
    LaunchedEffect(playerHasActed) {
        if (playerHasActed && !botHasActed) {
            delay(1000)
            botActionLogic(
                botChips = botChips,
                playerChips = playerChips,
                pot = pot,
                setBotChips = { botChips = it },
                setPlayerChips = { playerChips = it },
                setPot = { pot = it },
                setMessage = { message = it },
                setBotHasActed = { botHasActed = it },
                onBotFold = {
                    // Bot folds -> award pot to player
                    awardPotToPlayer(
                        setMessage = { message = it },
                        pot = pot,
                        playerChips = playerChips,
                        botChips = botChips,
                        setPlayerChips = { playerChips = it },
                        setBotChips = { botChips = it },
                        setPot = { pot = it }
                    )
                    // If both have chips, deal next hand
                    if (playerChips > 0 && botChips > 0) {
                        dealNextHand(
                            playerHand = playerHand,
                            botHand = botHand,
                            tableCards = tableCards,
                            fullDeck = fullDeck,
                            setMessage = { message = it },
                            setStage = { stage = it },
                            onShuffleSound = { cardShuffleSound.start() }
                        )
                    }
                }
            )
        }
    }

    // Advance stage
    LaunchedEffect(playerHasActed, botHasActed) {
        if (playerHasActed && botHasActed) {
            delay(1000)
            advanceStageWithAllIn(
                currentStage = stage,
                tableCards = tableCards,
                deck = fullDeck,
                playerHand = playerHand,
                botHand = botHand,
                playerChips = playerChips,
                botChips = botChips,
                pot = pot,
                sidePot = sidePot,
                setPlayerChips = { playerChips = it },
                setBotChips = { botChips = it },
                setPot = { pot = it },
                setSidePot = { sidePot = it },
                setTotalPot = { totalPot = it },
                setMessage = { message = it },
                setStage = { stage = it },
                setGuidance = { newStage ->
                    val updatedGuidance = GuidanceManager.updateGuidanceEnhanced(
                        playerHand = playerHand,
                        botHand = botHand,
                        tableCards = tableCards,
                        stage = newStage,
                        showBotCards = showBotCards
                    )
                    guidanceMessage = updatedGuidance
                },
                setPlayerScore = { playerScore = it },
                setBotScore = { botScore = it },
                onHandComplete = { winner ->
                    handsPlayed++
                    when (winner) {
                        "player" -> playerWins++
                        "bot" -> botWins++
                        "tie" -> ties++
                    }
                    sharedPrefs.edit()
                        .putInt("player_wins", playerWins)
                        .putInt("bot_wins", botWins)
                        .putInt("ties", ties)
                        .putInt("hands_played", handsPlayed)
                        .apply()
                },
                onCardFlipSound = { cardFlipSound.start() }
            )
            playerHasActed = false
            botHasActed = false
        }
    }

    // Check if game over
    LaunchedEffect(playerChips, botChips) {
        if (playerChips <= 0 || botChips <= 0) {
            gameOver = true
        }
    }

    // Layout
    Box(modifier = Modifier.fillMaxSize()) {

        // Main Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFF003300))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // If game is over
            if (gameOver) {
                GameOverScreen(
                    playerChips = playerChips,
                    botChips = botChips,
                    playerWins = playerWins,
                    botWins = botWins,
                    ties = ties,
                    onRestartMatch = {
                        // Reset everything
                        gameOver = false
                        playerChips = 2000
                        botChips = 2000
                        pot = 100
                        sidePot = 0
                        totalPot = 0
                        playerWins = 0
                        botWins = 0
                        ties = 0
                        handsPlayed = 0

                        fullDeck.clear()
                        initializeDeckShared(fullDeck)
                        dealInitialHand(
                            playerHand = playerHand,
                            botHand = botHand,
                            tableCards = tableCards,
                            fullDeck = fullDeck,
                            setMessage = { message = it },
                            setStage = { stage = it },
                            onShuffleSound = { cardShuffleSound.start() }
                        )
                        postBlindsAndStart(
                            playerChips = playerChips,
                            botChips = botChips,
                            setPlayerChips = { playerChips = it },
                            setBotChips = { botChips = it },
                            setPot = { pot = it },
                            setMessage = { message = it },
                            onChipSound = { chipSound.start() }
                        )
                        val newGuidance = GuidanceManager.updateGuidanceEnhanced(
                            playerHand = playerHand,
                            botHand = botHand,
                            tableCards = tableCards,
                            stage = stage,
                            showBotCards = showBotCards
                        )
                        guidanceMessage = newGuidance
                    },
                    onExit = onExit
                )
            } else {
                // Normal Gameplay
                Text("Guided Poker Playthrough", color = Color.White, fontSize = 28.sp)
                Spacer(modifier = Modifier.height(16.dp))

                StatsRow(
                    playerWins = playerWins,
                    botWins = botWins,
                    ties = ties,
                    pot = pot,
                    sidePot = sidePot,
                    totalPot = totalPot,
                    playerChips = playerChips,
                    botChips = botChips
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Player Score: $playerScore | Bot Score: $botScore", color = Color.Cyan, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Table Cards
                TableCardsDisplay(tableCards = tableCards, stage = stage)

                Spacer(modifier = Modifier.height(16.dp))

                // Bot Cards
                BotCardsDisplay(botHand = botHand, showBotCards = showBotCards)

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { showBotCards = !showBotCards }) {
                    Text(if (showBotCards) "Hide Bot Cards" else "Show Bot Cards")
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Player Cards
                PlayerCardsDisplay(playerHand = playerHand)

                Spacer(modifier = Modifier.height(16.dp))
                Text(message, color = Color.White)
                Text(guidanceMessage, color = Color.Yellow, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(16.dp))

                // Betting Buttons (stage 0..3)
                if (stage in 0..3) {
                    BettingButtonsAllIn(
                        playerChips = playerChips,
                        botChips = botChips,
                        pot = pot,
                        sidePot = sidePot,
                        setPlayerChips = { playerChips = it },
                        setBotChips = { botChips = it },
                        setPot = { pot = it },
                        setSidePot = { sidePot = it },
                        setMessage = { message = it },
                        setPlayerHasActed = { playerHasActed = it },
                        currentStage = stage,
                        onFold = {
                            // Fold -> awards pot to the other
                            awardPotToBot(
                                setMessage = { message = it },
                                pot = pot,
                                playerChips = playerChips,
                                botChips = botChips,
                                setPlayerChips = { playerChips = it },
                                setBotChips = { botChips = it },
                                setPot = { pot = it }
                            )
                            // Next hand if both have chips
                            if (playerChips > 0 && botChips > 0) {
                                dealNextHand(
                                    playerHand = playerHand,
                                    botHand = botHand,
                                    tableCards = tableCards,
                                    fullDeck = fullDeck,
                                    setMessage = { message = it },
                                    setStage = { stage = it },
                                    onShuffleSound = { cardShuffleSound.start() }
                                )
                            } else {
                                message = "Not enough chips for next hand."
                            }
                        }
                    )
                }

                // If stage == 4 and both have chips, allow dealing next hand
                if (stage == 4 && playerChips > 0 && botChips > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        // new hand
                        dealNextHand(
                            playerHand = playerHand,
                            botHand = botHand,
                            tableCards = tableCards,
                            fullDeck = fullDeck,
                            setMessage = { message = it },
                            setStage = { stage = it },
                            onShuffleSound = { cardShuffleSound.start() }
                        )
                        postBlindsAndStart(
                            playerChips = playerChips,
                            botChips = botChips,
                            setPlayerChips = { playerChips = it },
                            setBotChips = { botChips = it },
                            setPot = { pot = it },
                            setMessage = { message = it },
                            onChipSound = { chipSound.start() }
                        )

                        // Now get updated guidance from GuidanceManager
                        val newGuidance = GuidanceManager.updateGuidanceEnhanced(
                            playerHand = playerHand,
                            botHand = botHand,
                            tableCards = tableCards,
                            stage = stage,
                            showBotCards = showBotCards
                        )
                        guidanceMessage = newGuidance
                    }) {
                        Text("Deal Next Hand")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Bottom action buttons
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { showCheatSheet = true }) { Text("Cheat Sheet") }
                    Button(onClick = { showSettings = true }) { Text("Settings") }
                    Button(onClick = { showTutorial = !showTutorial }) { Text("Tutorial") }
                    Button(onClick = onExit) { Text("Exit") }
                }
            } // end of normal gameplay

            // Animated tutorial drawer
            AnimatedVisibility(
                visible = showTutorial,
                enter = slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 500)
                ),
                exit = slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 500)
                ),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(250.dp)
                    .background(Color(0xFF222222))
            ) {
                TutorialDrawer(onClose = { showTutorial = false })
            }

            // Settings popup
            if (showSettings) {
                SettingsMenuNew(
                    onClose = { showSettings = false },
                    onTutorialToggle = { showTutorial = !showTutorial }
                )
            }

            // Cheat Sheet popup
            if (showCheatSheet) {
                CheatSheetDialog(onClose = { showCheatSheet = false })
            }
        }
    }
}

/* -----------------------------------------------------------
   Composables & Logic
----------------------------------------------------------- */

/**
 * Example: A composable that plays your "chip.xml" animation
 * from /drawable.
 */
@Composable
fun ChipAnimation() {
    val context = LocalContext.current
    // Load your animated vector drawable from chip.xml
    val chipDrawable = remember {
        context.getDrawable(R.drawable.chip) as? AnimatedVectorDrawable
    }

    // Remember state so it only starts once
    LaunchedEffect(chipDrawable) {
        chipDrawable?.start() // start the animation
    }

    // Render the drawable as an Image
    Box(modifier = Modifier.size(48.dp)) {
        chipDrawable?.let {
            val bitmap = it.current.constantState?.newDrawable()?.toBitmap(width = 48, height = 48)
            if (bitmap != null) {
                Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Chip Animation")
            }
        }
    }
}

/**
 * The new "side pot" betting buttons.
 * Let's add an "All-In" button for demonstration.
 */
@Composable
fun BettingButtonsAllIn(
    playerChips: Int,
    botChips: Int,
    pot: Int,
    sidePot: Int,
    setPlayerChips: (Int) -> Unit,
    setBotChips: (Int) -> Unit,
    setPot: (Int) -> Unit,
    setSidePot: (Int) -> Unit,
    setMessage: (String) -> Unit,
    setPlayerHasActed: (Boolean) -> Unit,
    currentStage: Int,
    onFold: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Call 50
        Button(onClick = {
            val callAmount = 50
            if (playerChips >= callAmount && botChips >= callAmount) {
                setPlayerChips(playerChips - callAmount)
                setBotChips(botChips - callAmount)
                setPot(pot + callAmount * 2)
                setMessage("Both players call $$callAmount.")
                setPlayerHasActed(true)
            } else {
                setMessage("Not enough chips to call!")
            }
        }) {
            Text("Call 50")
        }

        // Raise 100
        Button(onClick = {
            val raiseAmount = 100
            if (playerChips >= raiseAmount) {
                setPlayerChips(playerChips - raiseAmount)
                var newPot = pot + raiseAmount
                // Bot must match
                if (botChips >= raiseAmount) {
                    setBotChips(botChips - raiseAmount)
                    newPot += raiseAmount
                    setMessage("You raised $$raiseAmount; Bot also calls $$raiseAmount.")
                } else {
                    // Bot can't afford full raise => partial call => side pot
                    val partial = botChips
                    setBotChips(0)
                    newPot += partial
                    val leftover = raiseAmount - partial
                    setSidePot(sidePot + leftover)
                    setMessage(
                        "You raised $$raiseAmount; Bot only had $$partial left. " +
                                "Side pot of $$leftover created."
                    )
                }
                setPot(newPot)
                setPlayerHasActed(true)
            } else {
                setMessage("Not enough chips to raise!")
            }
        }) {
            Text("Raise 100")
        }

        // All-In
        Button(onClick = {
            if (playerChips > 0) {
                val allInAmount = playerChips
                setPlayerChips(0)
                var newPot = pot + allInAmount
                // Bot must match if it can
                if (botChips >= allInAmount) {
                    setBotChips(botChips - allInAmount)
                    newPot += allInAmount
                    setMessage("You are all-in with $$allInAmount. Bot calls!")
                } else {
                    // Bot can't fully match => partial call => side pot
                    val partial = botChips
                    setBotChips(0)
                    newPot += partial
                    val leftover = allInAmount - partial
                    setSidePot(sidePot + leftover)
                    setMessage(
                        "You are all-in with $$allInAmount; Bot only had $$partial left. " +
                                "Side pot of $$leftover created."
                    )
                }
                setPot(newPot)
                setPlayerHasActed(true)
            } else {
                setMessage("You have no chips to go all-in!")
            }
        }) {
            Text("All-In")
        }

        // Fold
        Button(onClick = onFold) {
            Text("Fold")
        }
    }
}

@Composable
fun BottomButtonsNew(onSettings: () -> Unit, onTutorial: () -> Unit, onExit: () -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxWidth()
    ) {
        Button(onClick = onSettings) { Text("Settings") }
        Button(onClick = onTutorial) { Text("Tutorial") }
        Button(onClick = onExit) { Text("Exit") }
    }
}

/**
 * A simpler settings with a "Tutorial" toggle
 */
@Composable
fun SettingsMenuNew(onClose: () -> Unit, onTutorialToggle: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onClose() },
        title = { Text("Settings") },
        text = {
            Column {
                Button(onClick = onTutorialToggle) {
                    Text("Open/Close Tutorial Drawer")
                }
            }
        },
        confirmButton = { Button(onClick = onClose) { Text("Close") } }
    )
}

/**
 * Drawer content for the step-by-step tutorial
 */
@Composable
fun TutorialDrawer(onClose: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        Text("Poker Tutorial", color = Color.White, fontSize = 20.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "1. Pre-Flop: Each player is dealt 2 cards.\n" +
                    "2. Flop: Three community cards are revealed.\n" +
                    "3. Turn: Fourth card is revealed.\n" +
                    "4. River: Fifth card is revealed.\n" +
                    "5. Showdown: Best 5-card hand wins!\n\n" +
                    "Additional tips:\n" +
                    "- Raise with strong hands.\n" +
                    "- Fold if you suspect you’re beat.\n" +
                    "- Use pot odds to guide your decisions.",
            color = Color.White, fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onClose) {
            Text("Close Tutorial")
        }
    }
}

/**
 * A new Cheat Sheet dialog that shows recommended strong hands.
 */
@Composable
fun CheatSheetDialog(onClose: () -> Unit) {
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Poker Cheat Sheet") },
        text = {
            Column {
                Text("Strong Starting Hands:")
                Spacer(modifier = Modifier.height(4.dp))
                Text("• AA, KK, QQ, JJ")
                Text("• AKs, AQs, AJs")
                Text("• KQs")
                Text("• TT, 99")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Note: Hand strength may vary based on position and context.")
            }
        },
        confirmButton = {
            Button(onClick = onClose) {
                Text("Close")
            }
        }
    )
}

@Composable
fun GameOverScreen(
    playerChips: Int,
    botChips: Int,
    playerWins: Int,
    botWins: Int,
    ties: Int,
    onRestartMatch: () -> Unit,
    onExit: () -> Unit
) {
    Text("Game Over!", color = Color.Red, fontSize = 32.sp)
    Spacer(Modifier.height(8.dp))
    if (playerChips <= 0) {
        Text("Bot Wins!", color = Color.White, fontSize = 24.sp)
    } else {
        Text("You Win!", color = Color.White, fontSize = 24.sp)
    }
    Spacer(Modifier.height(8.dp))
    Text("Player Wins: $playerWins", color = Color.White)
    Text("Bot Wins: $botWins", color = Color.White)
    Text("Ties: $ties", color = Color.White)

    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onRestartMatch) {
        Text("Restart Entire Match")
    }
    Spacer(modifier = Modifier.height(16.dp))
    Button(onClick = onExit) {
        Text("Exit")
    }
}

/**
 * Displays basic stats: wins, pot, sidePot, chips, etc.
 */
@Composable
fun StatsRow(
    playerWins: Int,
    botWins: Int,
    ties: Int,
    pot: Int,
    sidePot: Int,
    totalPot: Int,
    playerChips: Int,
    botChips: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF004400), RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Player Wins: $playerWins", color = Color.White)
            Text("Bot Wins: $botWins", color = Color.White)
            Text("Ties: $ties", color = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Main Pot: $$pot", color = Color.White)
            Text("Side Pot: $$sidePot", color = Color.White)
            Text("Total Pot: $$totalPot", color = Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Player Chips: $$playerChips", color = Color.White)
            Text("Bot Chips: $$botChips", color = Color.White)
        }
    }
}

/** Displays the table (community) cards */
@Composable
fun TableCardsDisplay(tableCards: List<Card?>, stage: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF006400), RoundedCornerShape(8.dp))
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.Center) {
            tableCards.forEachIndexed { index, card ->
                val shouldRevealCard = when (stage) {
                    0 -> false
                    1 -> index <= 2 // flop
                    2 -> index <= 3 // turn
                    3 -> index <= 4 // river
                    else -> true
                }
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .shadow(2.dp, RoundedCornerShape(4.dp))
                ) {
                    PokerCardShared(
                        card = if (shouldRevealCard) card else null,
                        cardWidth = 60.dp,
                        cardHeight = 90.dp
                    )
                }
            }
        }
    }
}

/** Bot’s cards, hidden if showBotCards == false */
@Composable
fun BotCardsDisplay(botHand: List<Card>, showBotCards: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        botHand.forEach { card ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .shadow(4.dp, RoundedCornerShape(4.dp))
            ) {
                PokerCardShared(
                    card = if (showBotCards) card else null,
                    cardWidth = 60.dp,
                    cardHeight = 90.dp
                )
            }
        }
    }
}

/** Player’s cards (always shown) */
@Composable
fun PlayerCardsDisplay(playerHand: List<Card>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        playerHand.forEach { card ->
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .shadow(4.dp, RoundedCornerShape(4.dp))
            ) {
                PokerCardShared(
                    card = card,
                    cardWidth = 80.dp,
                    cardHeight = 120.dp
                )
            }
        }
    }
}

/* -----------------------------------------------------------
   Multi-Hand Flow Functions
----------------------------------------------------------- */

/**
 * Deals the initial hand at the start of a new match.
 * Clears the table, deals 2 to player, 2 to bot.
 */
fun dealInitialHand(
    playerHand: MutableList<Card>,
    botHand: MutableList<Card>,
    tableCards: MutableList<Card?>,
    fullDeck: MutableList<Card>,
    setMessage: (String) -> Unit,
    setStage: (Int) -> Unit,
    onShuffleSound: () -> Unit
) {
    playerHand.clear()
    botHand.clear()
    for (i in tableCards.indices) tableCards[i] = null

    if (fullDeck.size < 4) {
        setMessage("Deck low, reshuffling...")
        initializeDeckShared(fullDeck)
        onShuffleSound()
    }

    val firstFour = fullDeck.take(4)
    playerHand.addAll(firstFour.subList(0, 2))
    botHand.addAll(firstFour.subList(2, 4))
    fullDeck.removeAll(firstFour)

    setStage(0)
    setMessage("Dealt initial hand. Ready for pre-flop betting.")
}

/**
 * Deals the next hand, preserving chips & stats.
 */
fun dealNextHand(
    playerHand: MutableList<Card>,
    botHand: MutableList<Card>,
    tableCards: MutableList<Card?>,
    fullDeck: MutableList<Card>,
    setMessage: (String) -> Unit,
    setStage: (Int) -> Unit,
    onShuffleSound: () -> Unit
) {
    playerHand.clear()
    botHand.clear()
    for (i in tableCards.indices) tableCards[i] = null

    if (fullDeck.size < 4) {
        setMessage("Deck low, reshuffling for next hand...")
        initializeDeckShared(fullDeck)
        onShuffleSound()
    }

    val nextFour = fullDeck.take(4)
    playerHand.addAll(nextFour.subList(0, 2))
    botHand.addAll(nextFour.subList(2, 4))
    fullDeck.removeAll(nextFour)

    setStage(0)
    setMessage("Dealt next hand. Pre-flop betting round!")
}

/**
 * Blind posting for each new hand.
 */
fun postBlindsAndStart(
    playerChips: Int,
    botChips: Int,
    setPlayerChips: (Int) -> Unit,
    setBotChips: (Int) -> Unit,
    setPot: (Int) -> Unit,
    setMessage: (String) -> Unit,
    onChipSound: () -> Unit
) {
    val blind = 50
    if (playerChips >= blind && botChips >= blind) {
        setPlayerChips(playerChips - blind)
        setBotChips(botChips - blind)
        setPot(blind * 2)
        setMessage("Each posted $$blind as blind. Begin betting!")
        onChipSound()
    } else {
        setMessage("Not enough chips to post blinds! Consider reshuffling or restarting.")
    }
}

/* -----------------------------------------------------------
   Side Pots & All-In Logic
----------------------------------------------------------- */

/**
 * Extends normal "advanceStage" with checks for all-in / side pot.
 * If either is all-in, we might skip certain betting rounds or
 * finalize the pot.
 */
fun advanceStageWithAllIn(
    currentStage: Int,
    tableCards: MutableList<Card?>,
    deck: MutableList<Card>,
    playerHand: MutableList<Card>,
    botHand: MutableList<Card>,
    playerChips: Int,
    botChips: Int,
    pot: Int,
    sidePot: Int,
    setPlayerChips: (Int) -> Unit,
    setBotChips: (Int) -> Unit,
    setPot: (Int) -> Unit,
    setSidePot: (Int) -> Unit,
    setTotalPot: (Int) -> Unit,
    setMessage: (String) -> Unit,
    setStage: (Int) -> Unit,
    setGuidance: (Int) -> Unit,
    setPlayerScore: (Int) -> Unit,
    setBotScore: (Int) -> Unit,
    onHandComplete: (String) -> Unit,
    onCardFlipSound: () -> Unit
) {
    val bothHaveChips = (playerChips > 0 && botChips > 0)
    val nextStage = if (!bothHaveChips && currentStage < 3) {
        // Skip directly to showdown if one player is all-in
        3
    } else {
        currentStage
    }

    when (nextStage) {
        0 -> {
            if (deck.size < 3) {
                setMessage("Deck out of cards!")
                return
            }
            tableCards[0] = deck.removeAt(0)
            tableCards[1] = deck.removeAt(0)
            tableCards[2] = deck.removeAt(0)
            onCardFlipSound()
            setMessage("Flop revealed! Place your bets.")
            setStage(1)
            setGuidance(1)
        }
        1 -> {
            if (deck.size < 1) {
                setMessage("No more cards left!")
                return
            }
            tableCards[3] = deck.removeAt(0)
            onCardFlipSound()
            setMessage("Turn revealed! Place your bets.")
            setStage(2)
            setGuidance(2)
        }
        2 -> {
            if (deck.size < 1) {
                setMessage("No more cards left!")
                return
            }
            tableCards[4] = deck.removeAt(0)
            onCardFlipSound()
            setMessage("River revealed! Final betting round.")
            setStage(3)
            setGuidance(3)
        }
        3 -> {
            // Showdown
            setMessage("Showdown! Checking final hands.")
            val community = tableCards.filterNotNull()
            val playerBest = evaluateHand(playerHand + community)
            val botBest = evaluateHand(botHand + community)

            val playerHandDesc = describeHandRank(playerHand + community)
            val botHandDesc = describeHandRank(botHand + community)

            val winner = when {
                playerBest > botBest -> "player"
                botBest > playerBest -> "bot"
                else -> "tie"
            }

            val total = pot + sidePot
            setTotalPot(total)

            when (winner) {
                "player" -> {
                    setPlayerChips(playerChips + total)
                    setMessage("You won $$total with $playerHandDesc! Bot had $botHandDesc.")
                }
                "bot" -> {
                    setBotChips(botChips + total)
                    setMessage("Bot won $$total with $botHandDesc! You had $playerHandDesc.")
                }
                "tie" -> {
                    val split = total / 2
                    setPlayerChips(playerChips + split)
                    setBotChips(botChips + split)
                    setMessage("It's a tie! You both split $$total.")
                }
            }
            setPot(0)
            setSidePot(0)
            onHandComplete(winner)
            setStage(4)
            setGuidance(4)
        }
        4 -> {
            // Already done
        }
        else -> {
            // no-op
        }
    }
}

/* -----------------------------------------------------------
   Bot Logic
----------------------------------------------------------- */

fun botActionLogic(
    botChips: Int,
    playerChips: Int,
    pot: Int,
    setBotChips: (Int) -> Unit,
    setPlayerChips: (Int) -> Unit,
    setPot: (Int) -> Unit,
    setMessage: (String) -> Unit,
    setBotHasActed: (Boolean) -> Unit,
    onBotFold: () -> Unit
) {
    // A naive bot logic that calls if possible; improve for more realistic poker behavior.
    val neededToCall = 50
    if (botChips >= neededToCall) {
        setBotChips(botChips - neededToCall)
        setPot(pot + neededToCall)
        setMessage("Bot calls $$neededToCall.")
        setBotHasActed(true)
    } else {
        // Fold if unable to call
        onBotFold()
        setBotHasActed(false)
    }
}

fun awardPotToPlayer(
    setMessage: (String) -> Unit,
    pot: Int,
    playerChips: Int,
    botChips: Int,
    setPlayerChips: (Int) -> Unit,
    setBotChips: (Int) -> Unit,
    setPot: (Int) -> Unit
) {
    setPlayerChips(playerChips + pot)
    setMessage("You win $$pot by default (bot folded)!")
    setPot(0)
}

fun awardPotToBot(
    setMessage: (String) -> Unit,
    pot: Int,
    playerChips: Int,
    botChips: Int,
    setPlayerChips: (Int) -> Unit,
    setBotChips: (Int) -> Unit,
    setPot: (Int) -> Unit
) {
    setBotChips(botChips + pot)
    setMessage("Bot wins $$pot by default (you folded)!")
    setPot(0)
}

/*
   Evaluate hand => human description
*/
fun describeHandRank(cards: List<Card>): String {
    val handType = evaluateHand(cards)
    return when (handType) {
        HandType.ROYAL_FLUSH -> "a Royal Flush"
        HandType.STRAIGHT_FLUSH -> "a Straight Flush"
        HandType.FOUR_OF_A_KIND -> "Four of a Kind"
        HandType.FULL_HOUSE -> "a Full House"
        HandType.FLUSH -> "a Flush"
        HandType.STRAIGHT -> "a Straight"
        HandType.THREE_OF_A_KIND -> "Three of a Kind"
        HandType.TWO_PAIR -> "Two Pair"
        HandType.PAIR -> "a Pair"
        HandType.HIGH_CARD -> "a High Card"
        else -> "an Unknown Hand Type"
    }
}
