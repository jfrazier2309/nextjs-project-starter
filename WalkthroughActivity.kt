package com.beaker.playsmartcards

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beaker.playsmartcards.ui.theme.PlaySmartcardsTheme
import com.beaker.playsmartcards.shared.PokerCardShared
import com.beaker.playsmartcards.shared.initializeDeckShared
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextDecoration
import com.beaker.playsmartcards.shared.Card
import com.beaker.playsmartcards.shared.evaluateHand


// --------------------
// Data Classes
// --------------------
data class QuizItem(
    val question: String,
    val answers: List<String>,
    val correctAnswerIndex: Int
)

data class WalkthroughStep(
    val title: String,
    val description: List<String>,
    val cards: List<Card>? = null,
    val imageRes: Int? = null,
    val isInteractive: Boolean = false,
    val quizItems: List<QuizItem> = emptyList(),          // For mini-quizzes
    val externalLink: String? = null                      // For additional resources
)

// --------------------
// Activity
// --------------------
class WalkthroughActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load saved step or default to 0
        val startingStep = loadWalkthroughProgress(this)

        setContent {
            PlaySmartcardsTheme {
                WalkthroughScreen(
                    initialStep = startingStep,
                    onBackClicked = { finish() },
                    onMainMenuClicked = { finish() }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Could save progress here if needed, but in this example we handle it in the composable
    }
}

// --------------------
// Preferences Helpers
// --------------------
fun loadWalkthroughProgress(context: Context): Int {
    val prefs = context.getSharedPreferences("WalkthroughPrefs", Context.MODE_PRIVATE)
    return prefs.getInt("currentStep", 0)
}

fun saveWalkthroughProgress(context: Context, step: Int) {
    val prefs = context.getSharedPreferences("WalkthroughPrefs", Context.MODE_PRIVATE)
    prefs.edit()
        .putInt("currentStep", step)
        .apply()
}

// --------------------
// Main Walkthrough Screen
// --------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkthroughScreen(
    initialStep: Int,
    onBackClicked: () -> Unit,
    onMainMenuClicked: () -> Unit
) {
    // We now start from the saved step
    var currentStep by remember { mutableStateOf(initialStep) }
    val walkthroughSteps = getWalkthroughSteps()
    val totalSteps = walkthroughSteps.size
    val step = walkthroughSteps[currentStep]

    // For saving progress
    val context = LocalContext.current

    // Persist step whenever it changes
    LaunchedEffect(currentStep) {
        saveWalkthroughProgress(context, currentStep)
    }

    // For step jump menu
    var showStepSelector by remember { mutableStateOf(false) }

    // Background gradient
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(Color(0xFF004D40), Color(0xFF006400)),
        startY = 0f,
        endY = Float.POSITIVE_INFINITY
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Walkthrough",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    Button(
                        onClick = onBackClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Back", color = Color.White)
                    }
                },
                actions = {
                    Button(
                        onClick = { showStepSelector = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Jump to Step", color = Color.White)
                    }
                    Button(
                        onClick = onMainMenuClicked,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                    ) {
                        Text("Main Menu", color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF003D33))
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(innerPadding)
        ) {
            // Make this Column scrollable:
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()) // ← Add this
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Step ${currentStep + 1} of $totalSteps",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // Step Title
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF003D33)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(16.dp),
                        maxLines = 2
                    )
                }

                // Step Description
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = Color(0x80000000),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        step.description.forEach { desc ->
                            HighlightedText(
                                text = desc,
                                highlightWords = listOf("Flop", "Turn", "River", "community cards"),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }

                // Optional Image
                step.imageRes?.let { imageRes ->
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Image(
                            painter = painterResource(id = imageRes),
                            contentDescription = "Step image",
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                // Example Cards
                step.cards?.let { cards ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Example Cards:",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            cards.forEach { card ->
                                PokerCardShared(card = card)
                            }
                        }
                    }
                }

                // Show Quizzes if any
                if (step.quizItems.isNotEmpty()) {
                    step.quizItems.forEach { quiz ->
                        QuizComposable(quiz)
                    }
                }

                // Show external link if available (example for Additional Resources)
                step.externalLink?.let { link ->
                    ExternalLinkComposable(link)
                }

                // Show interactive demo if step isInteractive
                if (step.isInteractive) {
                    // Optionally add a "Skip" button for experienced players
                    TextButton(
                        onClick = { /* Skips the entire demo, e.g., go to next step */ currentStep++ },
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("Skip Demo", color = Color.Yellow)
                    }

                    InteractiveDemo(onDemoEnd = { currentStep = 0 }) // or next step
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Expandable advanced tips (Customization & Personalization)
                var showAdvancedTips by remember { mutableStateOf(false) }
                TextButton(
                    onClick = { showAdvancedTips = !showAdvancedTips },
                    modifier = Modifier.alpha(0.9f)
                ) {
                    Text(
                        text = if (showAdvancedTips) "Hide Advanced Tips" else "Show Advanced Tips",
                        color = Color.Cyan
                    )
                }
                AnimatedVisibility(
                    visible = showAdvancedTips,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    AdvancedTipsSection()
                }

                NavigationButtons(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    onPrevious = { currentStep -= 1 },
                    onNext = { currentStep += 1 },
                    onMainMenuClicked = onMainMenuClicked
                )
            }

            // Step Selector Dialog
            if (showStepSelector) {
                StepSelectorDialog(
                    steps = walkthroughSteps,
                    onStepSelected = { index ->
                        currentStep = index
                        showStepSelector = false
                    },
                    onDismiss = { showStepSelector = false }
                )
            }
        }
    }
}

// --------------------
// Step Selector Dialog
// --------------------
@Composable
fun StepSelectorDialog(
    steps: List<WalkthroughStep>,
    onStepSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        text = {
            Column {
                Text("Jump to a specific step:")
                Spacer(modifier = Modifier.height(8.dp))
                steps.forEachIndexed { index, step ->
                    TextButton(onClick = { onStepSelected(index) }) {
                        Text(text = "Step ${index + 1}: ${step.title}")
                    }
                }
            }
        }
    )
}

// --------------------
// Quizzes & Engagement
// --------------------
@Composable
fun QuizComposable(quizItem: QuizItem) {
    var selectedAnswer by remember { mutableStateOf(-1) }
    var submitted by remember { mutableStateOf(false) }

    Text(
        text = quizItem.question,
        color = Color.Yellow,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )

    quizItem.answers.forEachIndexed { idx, answer ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable {
                    if (!submitted) selectedAnswer = idx
                }
                .padding(vertical = 4.dp)
        ) {
            RadioButton(
                selected = (selectedAnswer == idx),
                onClick = { if (!submitted) selectedAnswer = idx },
                colors = RadioButtonDefaults.colors(
                    selectedColor = Color.Cyan,
                    unselectedColor = Color.White
                )
            )
            Text(
                text = answer,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    Button(
        onClick = { submitted = true },
        enabled = !submitted && selectedAnswer >= 0,
        modifier = Modifier.padding(vertical = 8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF003D33))
    ) {
        Text("Submit", color = Color.White)
    }

    if (submitted) {
        val isCorrect = (selectedAnswer == quizItem.correctAnswerIndex)
        val resultText = if (isCorrect) "Correct!" else "Incorrect. Try reviewing the step above."
        val resultColor = if (isCorrect) Color.Green else Color.Red

        Text(
            text = resultText,
            color = resultColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

// --------------------
// Highlighted Text
// Example of "tooltips/overlays" approach
// --------------------
@Composable
fun HighlightedText(
    text: String,
    highlightWords: List<String>,
    modifier: Modifier = Modifier
) {
    // Simple logic: highlight certain keywords, no actual tooltip for brevity
    // Could be expanded with a real tooltip overlay
    val annotatedString = buildAnnotatedString {
        append(text)
        highlightWords.forEach { keyword ->
            val startIndex = text.indexOf(keyword, ignoreCase = true)
            if (startIndex >= 0) {
                // Add a style span
                addStyle(
                    style = SpanStyle(
                        color = Color.Yellow,
                        fontWeight = FontWeight.Bold
                    ),
                    start = startIndex,
                    end = startIndex + keyword.length
                )
            }
        }
    }
    Text(annotatedString, color = Color.White, modifier = modifier)
}

// --------------------
// External Link Composable
// --------------------
@Composable
fun ExternalLinkComposable(url: String) {
    Text(
        text = "More info: $url",
        color = Color.Magenta,
        fontSize = 14.sp,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .padding(top = 8.dp)
            .clickable {
                // Open the link in a browser.
                // Use e.g. an Intent in an Android context, or a custom approach
                // This is a placeholder
            }
    )
}

// --------------------
// Advanced Tips Section
// --------------------
@Composable
fun AdvancedTipsSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .background(Color(0x66000000), shape = RoundedCornerShape(8.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Advanced Tips",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "1. Position Play: Being last to act gives more control and information.",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "2. Pot Odds: Compare the current pot size to the cost of a potential call.",
            color = Color.White,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "3. Hand Ranges: Think in terms of ranges rather than single potential hands.",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

// --------------------
// Navigation Buttons
// --------------------
@Composable
fun NavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onMainMenuClicked: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        Button(
            onClick = { if (currentStep > 0) onPrevious() },
            enabled = currentStep > 0,
            modifier = Modifier.padding(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF003D33),
                contentColor = Color.White
            )
        ) {
            Text("Previous")
        }

        Button(
            onClick = onMainMenuClicked,
            modifier = Modifier.padding(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF003D33),
                contentColor = Color.White
            )
        ) {
            Text("Main Menu")
        }

        Button(
            onClick = { if (currentStep < totalSteps - 1) onNext() },
            enabled = currentStep < totalSteps - 1,
            modifier = Modifier.padding(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF003D33),
                contentColor = Color.White
            )
        ) {
            Text("Next")
        }
    }
}

// --------------------
// Interactive Demo
// (With improved MediaPlayer handling & skip option)
// --------------------
@Composable
fun InteractiveDemo(onDemoEnd: () -> Unit) {
    var demoStep by remember { mutableStateOf(0) }
    val demoMessages = listOf(
        "Dealing your cards...",
        "Flop is dealt: three community cards appear.",
        "Turn is dealt: one more community card is revealed.",
        "River is dealt: final community card revealed.",
        "Showdown! Determining the winner..."
    )

    // Deal logic
    val playerHand = remember { mutableStateListOf<Card>() }
    val botHand = remember { mutableStateListOf<Card>() }
    val communityCards = remember { mutableStateListOf<Card?>(null, null, null, null, null) }
    val deck = remember { mutableStateListOf<Card>() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Improved MediaPlayer usage
    val cardFlipPlayer = remember {
        MediaPlayer.create(context, R.raw.card_flip).apply {
            // Set volume or looping if needed
            // e.g. setVolume(0.5f, 0.5f)
        }
    }

    DisposableEffect(Unit) {
        // Initialize deck & deal
        initializeDeckShared(deck)
        playerHand.addAll(deck.take(2))
        deck.removeAll(playerHand)
        botHand.addAll(deck.take(2))
        deck.removeAll(botHand)

        onDispose {
            // Release MediaPlayer resources
            if (cardFlipPlayer.isPlaying) {
                cardFlipPlayer.stop()
            }
            cardFlipPlayer.release()
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
    ) {
        Text(
            text = demoMessages[demoStep],
            color = Color.Yellow,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val visibleCards: List<Card?> = when (demoStep) {
            0 -> playerHand.map { it }.map<Card, Card?> { it }
            1 -> {
                dealCommunityCards(communityCards, deck, 3)
                communityCards.take(3)
            }
            2 -> {
                dealCommunityCards(communityCards, deck, 4)
                communityCards.take(4)
            }
            3 -> {
                dealCommunityCards(communityCards, deck, 5)
                communityCards
            }
            4 -> communityCards
            else -> emptyList()
        }

        Row(
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            visibleCards.forEach { card ->
                if (card != null) {
                    PokerCardShared(
                        card = card,
                        modifier = Modifier.clickable {
                            // Optional: handle card clicks for more detail
                        }
                    )
                }
            }
        }

        // Play card flip sound after each new card is dealt
        LaunchedEffect(demoStep) {
            if (demoStep > 0) {
                cardFlipPlayer.start()
            }
        }

        if (demoStep == 4) {
            val finalCommunityCards = communityCards.filterNotNull()
            val playerScore = evaluateHand(playerHand + finalCommunityCards)
            val botScore = evaluateHand(botHand + finalCommunityCards)

            val result = when {
                playerScore > botScore -> "You win! Your hand is stronger."
                botScore > playerScore -> "Bot wins! Better luck next time."
                else -> "It's a tie! Both hands are equally strong."
            }

            Text(
                text = result,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(8.dp)
            )
        }

        Button(
            onClick = {
                if (demoStep < demoMessages.size - 1) {
                    demoStep++
                } else {
                    // Reset everything and exit the demo
                    deck.clear()
                    playerHand.clear()
                    botHand.clear()
                    communityCards.clear()
                    onDemoEnd()
                }
            },
            modifier = Modifier.padding(8.dp)
        ) {
            Text(if (demoStep < demoMessages.size - 1) "Continue" else "End Demo")
        }
    }
}

// Deals the specified number of community cards
fun dealCommunityCards(
    communityCards: MutableList<Card?>,
    deck: MutableList<Card>,
    numberToReveal: Int
) {
    for (i in 0 until numberToReveal) {
        if (communityCards[i] == null && deck.isNotEmpty()) {
            communityCards[i] = deck.removeAt(0)
        }
    }
}

// --------------------
// Steps with Quizzes, External Links, etc.
// --------------------
fun getWalkthroughSteps(): List<WalkthroughStep> {
    val flushHearts = getSpecificCards(listOf(
        R.drawable.five_heart, R.drawable.six_heart, R.drawable.seven_heart,
        R.drawable.eight_heart, R.drawable.nine_heart
    ))

    val twoPair = getSpecificCards(listOf(
        R.drawable.two_clover, R.drawable.two_heart,
        R.drawable.four_diamond, R.drawable.four_clover, R.drawable.six_spade
    ))

    val royalFlushHearts = getSpecificCards(listOf(
        R.drawable.ten_heart, R.drawable.jack_heart, R.drawable.queen_heart,
        R.drawable.king_heart, R.drawable.ace_heart
    ))

    return listOf(
        WalkthroughStep(
            title = "Step 1: Understand the Goal",
            description = listOf(
                "The main objective is to form the best poker hand or strategically outplay opponents.",
                "Success often comes from understanding the cards, your odds, and reading other players."
            ),
            externalLink = "https://en.wikipedia.org/wiki/Texas_hold_%27em"
        ),
        WalkthroughStep(
            title = "Step 2: Learn the Betting Rounds",
            description = listOf(
                "Pre-Flop: Players are dealt hole cards and place initial bets.",
                "Flop: Three community cards are revealed, followed by another round of betting.",
                "Turn: A fourth community card appears, and players can again bet or fold.",
                "River: The fifth community card is dealt, and the final bets are placed before the showdown."
            ),
            quizItems = listOf(
                QuizItem(
                    question = "Which betting round comes after the Flop?",
                    answers = listOf("Pre-Flop", "Turn", "River"),
                    correctAnswerIndex = 1
                )
            )
        ),
        WalkthroughStep(
            title = "Step 3: Common Poker Hands",
            description = listOf(
                "High Card: No combination, just your highest card matters.",
                "Pair: Two cards of the same rank.",
                "Two Pair: Two different pairs, which add strength to your hand."
            ),
            cards = twoPair
        ),
        WalkthroughStep(
            title = "Step 4: Strong Hands",
            description = listOf(
                "Straight: Five cards in sequential order (e.g. 5-6-7-8-9).",
                "Flush: Five cards of the same suit, regardless of order.",
                "These hands greatly increase your winning potential."
            ),
            cards = flushHearts
        ),
        WalkthroughStep(
            title = "Step 5: The Best Poker Hands",
            description = listOf(
                "Full House: Three of a kind + a pair (e.g. 10-10-10-J-J).",
                "Royal Flush: A, K, Q, J, 10 of the same suit – the ultimate hand.",
                "Mastering these top-tier hands can turn the tables in critical moments."
            ),
            cards = royalFlushHearts
        ),
        WalkthroughStep(
            title = "Step 6: Tips for Bluffing",
            description = listOf(
                "Bluffing: Convince opponents you have a stronger hand than you do.",
                "Observe betting patterns, timing, and reactions to detect or execute bluffs.",
                "A well-timed bluff can win a hand even with weaker cards."
            )
        ),
        WalkthroughStep(
            title = "Step 7: Putting It All Together – Basic Gameplay Flow",
            description = listOf(
                "Decide your seat and buy-in.",
                "Get your hole cards and assess the situation.",
                "Place your bet, call, or fold based on your strategy.",
                "Flop, Turn, and River: watch community cards appear and adjust your tactics.",
                "Showdown if multiple players remain – best 5-card hand wins!"
            )
        ),
        WalkthroughStep(
            title = "Step 8: Interactive Demo",
            description = listOf(
                "Try a live demo: dealing cards, community cards, and final showdown.",
                "Practice reading the board and making strategic decisions."
            ),
            isInteractive = true
        )
    )
}

// --------------------
// Example of Mapping Resources to Cards
// --------------------
fun getSpecificCards(drawableIds: List<Int>): List<Card> {
    return drawableIds.map { resId -> mapResourceToCard(resId) }
}

fun mapResourceToCard(resId: Int): Card {
    val (rank, suit) = determineRankSuitFromResource(resId)
    return Card(rank, suit, resId)
}

fun determineRankSuitFromResource(resId: Int): Pair<Int, Int> {
    // Real logic must parse your resource naming scheme
    // Here is just a placeholder:
    return Pair(14, 0) // (Ace of Spades) as example
}