package com.beaker.playsmartcards.logic

import com.beaker.playsmartcards.shared.Card
import com.beaker.playsmartcards.shared.HandType
import com.beaker.playsmartcards.shared.evaluateHand
import com.beaker.playsmartcards.shared.initializeDeckShared
import kotlin.random.Random

/**
 * Data class to represent a player (human or bot) in the poker game.
 */
data class Player(
    val name: String,
    var chips: Int,
    val hand: MutableList<Card> = mutableListOf(),
    var isFolded: Boolean = false,
    var isAllIn: Boolean = false,
    var isOut: Boolean = false,
    var betThisRound: Int = 0 // Add this line
)

/**
 * Enum for the game stage of a hand.
 */
enum class Stage { PRE_FLOP, FLOP, TURN, RIVER, SHOWDOWN, HAND_OVER }

/**
 * Data class to hold structured information about the winner of a hand.
 */
data class HandResultInfo(val winnerNames: List<String>, val winningHand: String)

data class SidePot(
    var amount: Int,
    val eligible: MutableSet<Int>  // player indices who can win this pot
)
/**
 * GameLogic encapsulates a 3-player Texas Hold'em poker game logic.
 * It manages dealing, betting rounds, hand evaluation, and win conditions.
 */
class GameLogic(
    private val startingChips: Int = 2000,
    private val smallBlindAmount: Int = 50,
    val bigBlindAmount: Int = 100
) { // The parentheses end here
    // Game state variables
    val players: Array<Player> = arrayOf(
        Player("You", startingChips),
        Player("Bot 1", startingChips),
        Player("Bot 2", startingChips)
    )
    private var lastAggressorIndex: Int = -1 // Tracks last player who raised

    var dealerIndex: Int = 0
        private set
    var stage: Stage = Stage.PRE_FLOP
        private set
    var pot: Int = 0
        private set
    var currentBet: Int = 0  // amount that players need to call in the current round
        private set

    var currentActorIndex: Int = -1
        private set

    var lastHandResult: HandResultInfo? = null
        private set


    // Deck and community cards
    private val deck: MutableList<Card> = mutableListOf()
    val communityCards: MutableList<Card> = mutableListOf()

    // Difficulty level for bot AI ("Easy", "Medium", "Hard")
    var difficulty: String = "Easy"

    // Message for status updates (to be displayed in UI)
    var message: String = ""

    // List of all pots: main pot is sidePots[0], subsequent are true side-pots
    private val sidePots = mutableListOf(SidePot(0, mutableSetOf(0,1,2)))

    // Track current bets each player has put in during this betting round
    private var currentBets: IntArray = IntArray(players.size)

    fun getAmountToCall(playerIndex: Int): Int {
        if (playerIndex < 0 || playerIndex >= players.size) {
            // Invalid player index, return 0 to avoid errors
            return 0
        }
        val player = players[playerIndex]
        // The amount to call is the current round's bet minus what this player has already put in this round.
        return (currentBet - player.betThisRound).coerceAtLeast(0)
    }


    /** Initialize a new deck and shuffle it. */
    private fun initDeck() {
        deck.clear()
        initializeDeckShared(deck)  // fill deck with 52 cards
        deck.shuffle()              // random shuffle
    }

    /** Start a new hand (round) of poker. Deals cards, posts blinds, and begins pre-flop betting. */
    fun startNewRound() {
        try {
            // 1. Check if the game is over before starting.
            if (isGameOver()) {
                message = "Game over! Thank you for playing."
                return
            }

            // 2. Reset the state for the new hand.
            stage = Stage.PRE_FLOP
            communityCards.clear()
            pot = 0
            currentBet = 0
            lastHandResult = null

            players.forEach {
                if (!it.isOut) {
                    it.hand.clear()
                    it.isFolded = false
                    it.isAllIn = false
                    it.betThisRound = 0
                }
            }

            // 3. Rotate the dealer button to the next active player.
            dealerIndex = getNextActivePlayer(dealerIndex)
            if(dealerIndex == -1) { // Failsafe if only one player is left
                isGameOver()
                return
            }

            // 4. Initialize a new shuffled deck and deal cards.
            initDeck()
            dealHoleCards()

            // 5. Post blinds and start betting round.
            postBlindsAndStartBetting()
        } catch (e: Exception) {
            message = "Error starting new round: ${e.message}"
            e.printStackTrace()
        }
    }

    /** Helper to get the next player index (clockwise) who is still active (not permanently out). */
    private fun getNextActivePlayer(fromIndex: Int): Int {
        var idx = (fromIndex + 1) % players.size
        var loopCheck = 0
        // loop until we find a player who is not out
        while (players[idx].isOut || players[idx].isFolded || players[idx].isAllIn) {
            idx = (idx + 1) % players.size
            // Failsafe to prevent infinite loops
            loopCheck++
            if(loopCheck > players.size) return -1
        }
        return idx
    }

    /** Deal two hole cards to each active player (players who still have chips). */
    private fun dealHoleCards() {
        // Dealing starts from the player to the left of the dealer (small blind) and goes clockwise.
        var idx = getNextActivePlayer(fromIndex = dealerIndex)
        for (i in 0 until players.size * 2) {
            val player = players[idx]
            if (!player.isOut) {
                if (deck.isNotEmpty()) {
                    player.hand.add(deck.removeAt(0))
                }
            }
            idx = getNextActivePlayer(fromIndex = idx)
            if (idx == -1) break // Stop if no active players left
        }
    }

    /** Post blinds: small blind and big blind contribute forced bets to the pot. */
    private fun postBlindsAndStartBetting() {
        val smallBlindIdx = getNextActivePlayer(dealerIndex)
        val bigBlindIdx   = getNextActivePlayer(smallBlindIdx)

        if (smallBlindIdx != -1) {
            // post 50
            performAction(smallBlindIdx, BotDecision(ActionType.RAISE, smallBlindAmount), 0)
        }
        if (bigBlindIdx != -1) {
            // post 100
            performAction(bigBlindIdx, BotDecision(ActionType.RAISE, bigBlindAmount), 0)
        }

        // Now everyone must at least call 100
        currentBet = bigBlindAmount
        lastAggressorIndex = bigBlindIdx

        runBettingRound(getNextActivePlayer(bigBlindIdx))
    }

    /**
     * Loop through each actor until the round completes, or the human needs to act.
     */
    private fun runBettingRound(startIndex: Int) {
        var actorIndex = startIndex

        while (true) {
            // If we’ve come full circle back to the last aggressor, the round ends.
            if (actorIndex == lastAggressorIndex) {
                afterBettingRound()
                return
            }

            val player = players[actorIndex]
            if (!player.isFolded && !player.isAllIn && !player.isOut) {
                currentActorIndex = actorIndex

                // Pause here for the human to act
                if (player.name == "You") {
                    message = "Your turn to act."
                    return
                }

                // Bot’s turn:
                val callAmount = getAmountToCall(actorIndex)
                val decision   = decideBotAction(player, callAmount)

                if (decision.type == ActionType.RAISE) {
                    lastAggressorIndex = actorIndex
                }

                performAction(actorIndex, decision, callAmount)

                if (remainingPlayersInHand() <= 1) {
                    afterBettingRound()
                    return
                }
            }

            // Move on to the next player
            actorIndex = getNextActivePlayer(actorIndex)
            if (actorIndex == -1) {
                // Everyone else was all-in
                afterBettingRound()
                return
            }
        }
    }

    /** Handle the end of a betting round: either deal next stage or proceed to showdown. */
    private fun afterBettingRound() {
        // 1) Clear per-round bets (we already added them to pot in performAction)
        players.forEach { it.betThisRound = 0 }
        currentActorIndex   = -1
        currentBet          = 0
        lastAggressorIndex  = -1

        // 2) If only one player remains, end the hand immediately
        if (remainingPlayersInHand() <= 1) {
            handleEndOfHand()
            return
        }

        // 3) Advance to next stage
        stage = when (stage) {
            Stage.PRE_FLOP -> Stage.FLOP.also  { dealCommunityCards(3) }
            Stage.FLOP     -> Stage.TURN.also  { dealCommunityCards(1) }
            Stage.TURN     -> Stage.RIVER.also { dealCommunityCards(1) }
            Stage.RIVER    -> Stage.SHOWDOWN
            else           -> stage
        }
        message = "Betting round over. Stage is now $stage."

        // 4) Showdown or next betting round
        if (stage == Stage.SHOWDOWN) {
            handleShowdown()
        } else {
            val firstToAct = getNextActivePlayer(dealerIndex)
            if (firstToAct != -1) {
                runBettingRound(firstToAct)
            } else {
                dealRemainingCommunityCards()
                handleShowdown()
            }
        }
    }




    private fun dealRemainingCommunityCards() {
        val cardsToDeal = 5 - communityCards.size
        if (cardsToDeal > 0) {
            dealCommunityCards(cardsToDeal)
        }
    }

    /** Deal the specified number of community cards from the deck to the table. */
    private fun dealCommunityCards(count: Int) {
        repeat(count) {
            if (deck.isNotEmpty()) {
                communityCards.add(deck.removeAt(0))
            }
        }
    }

    /** Describe the community cards on the table (for messages/logging). */
    private fun describeCommunityCards(): String {
        return communityCards.joinToString(", ") { it.toString() }
    }

    /** Reset the currentBets tracking and currentBet for a new betting stage. */
    private fun resetCurrentBets() {
        currentBets.fill(0)
        currentBet = 0
    }

    /** Count how many players are still in the current hand (not folded and not out). */
    private fun remainingPlayersInHand(): Int {
        return players.count { !it.isFolded && !it.isOut }
    }

    /** Decide a bot's action based on difficulty, hand strength, and randomness. */
    private fun decideBotAction(player: Player, callAmount: Int): BotDecision {
        val handStrength = evaluateHand(player.hand + communityCards)
        val randomFactor = Random.nextFloat()

        // Define bot "personality" based on difficulty. This is more systematic.
        // The Triple holds: (Aggression level, Willingness to call, Chance to bluff)
        val (aggression, callStickiness, bluffChance) = when (difficulty) {
            "Easy"   -> Triple(0.1f, 0.2f, 0.05f) // Passive, folds easily
            "Medium" -> Triple(0.3f, 0.5f, 0.15f) // Balanced, will see more flops
            "Hard"   -> Triple(0.6f, 0.8f, 0.25f) // Aggressive, sticky, and bluffs more
            else     -> Triple(0.1f, 0.2f, 0.05f) // Default to Easy
        }

        // This block handles the logic for when there is NO bet to call.
        if (callAmount == 0) {
            // The bot decides whether to Check or make an opening Bet.
            return if (handStrength.ordinal >= HandType.PAIR.ordinal && randomFactor < aggression) {
                // Bet with a pair or better, with a chance based on its aggression.
                val raiseAmount = (pot * (0.4 + aggression)).toInt().coerceAtLeast(bigBlindAmount)
                BotDecision(ActionType.RAISE, amount = raiseAmount)
            } else {
                // Check otherwise.
                BotDecision(ActionType.CHECK)
            }
        } else { // This block handles the logic for when there IS a bet to call.
            // If the call would put the bot all-in, it needs a stronger hand to continue.
            if (player.chips <= callAmount) {
                return if (handStrength.ordinal >= HandType.TWO_PAIR.ordinal) BotDecision(ActionType.CALL) else BotDecision(ActionType.FOLD)
            }

            // The bot calculates simplified pot odds to see if calling is worth the price.
            val potOdds = callAmount.toFloat() / (pot + callAmount)

            // Harder bots are "stickier" and more willing to call with marginal hands.
            val oddsThreshold = 0.6f - callStickiness

            return when {
                // Re-raise with a monster hand (Three of a Kind or better).
                handStrength.ordinal >= HandType.THREE_OF_A_KIND.ordinal && randomFactor < aggression -> {
                    // The raise amount is based on the pot size and the bot's aggression.
                    val raiseAmount = (pot * (0.6 + aggression)).toInt().coerceAtLeast(currentBet * 2)
                    BotDecision(ActionType.RAISE, amount = raiseAmount)
                }
                // Call with a decent hand if the pot odds are favorable.
                handStrength.ordinal >= HandType.PAIR.ordinal && potOdds < oddsThreshold -> {
                    BotDecision(ActionType.CALL)
                }
                // Bluffing: The bot might re-raise a small bet as a bluff.
                randomFactor < bluffChance && potOdds < 0.1f -> {
                    BotDecision(ActionType.RAISE, amount = currentBet * 2)
                }
                // If none of the above, fold the hand.
                else -> BotDecision(ActionType.FOLD)
            }
        }
    }


    /** Perform the given action for a player, updating chips, side‐pots, currentBet, etc. */
    private fun performAction(playerIndex: Int, decision: BotDecision, callAmount: Int) {
        val player = players[playerIndex]

        when (decision.type) {
            ActionType.FOLD -> {
                player.isFolded = true
                message = "${player.name} folds."
            }

            ActionType.CHECK -> {
                if (callAmount == 0) {
                    message = "${player.name} checks."
                } else {
                    player.isFolded = true
                    message = "${player.name} folds."
                }
            }

            ActionType.CALL, ActionType.RAISE -> {
                // 1) Compute how much this action adds in total
                val desiredTotal = if (decision.type == ActionType.CALL) {
                    callAmount
                } else {
                    callAmount + decision.amount
                }
                // subtract what they've already put in this round
                val amountToPutIn = (minOf(desiredTotal, player.chips + player.betThisRound)
                        - player.betThisRound)

                // 2) Deduct from their stack and update their per‐round bet
                player.chips -= amountToPutIn
                player.betThisRound += amountToPutIn

                // 3) If they’re all‐in below currentBet, split off side‐pots
                if (player.chips == 0 && player.betThisRound < currentBet) {
                    val allInAmt = player.betThisRound
                    val newPots = mutableListOf<SidePot>()

                    // for each existing pot, split off any excess over allInAmt
                    sidePots.forEach { pot ->
                        if (pot.amount > allInAmt) {
                            val excess = pot.amount - allInAmt
                            pot.amount = allInAmt
                            newPots += SidePot(excess, pot.eligible.toMutableSet())
                        }
                        // remove this all‐in player from eligibility in higher pots
                        if (pot.amount > allInAmt) {
                            pot.eligible.remove(playerIndex)
                        }
                    }
                    sidePots += newPots
                }

                // 4) Add their chips into the current (last) pot
                sidePots.last().amount += amountToPutIn

                // 5) On a raise, update the round’s currentBet and aggressor
                if (decision.type == ActionType.RAISE) {
                    currentBet = player.betThisRound
                    lastAggressorIndex = playerIndex
                }

                // 6) Mark all‐in if they emptied their stack
                if (player.chips == 0) {
                    player.isAllIn = true
                    message = "${player.name} is all-in with $$amountToPutIn."
                } else {
                    message = when (decision.type) {
                        ActionType.CALL -> "${player.name} calls $$amountToPutIn."
                        ActionType.RAISE -> "${player.name} raises to $$currentBet."
                        else -> ""
                    }
                }
            }
        }
    }

    /** Handle the player's action (called from UI for the human player). */
    fun handlePlayerAction(actionType: ActionType, raiseAmount: Int = 0) {
        // don’t allow any betting once the hand is over
        if (stage == Stage.HAND_OVER || currentActorIndex != 0) return

        val callAmount = getAmountToCall(0)
        val playerChips = players[0].chips

        // Determine the actual raise amount (only for RAISE actions)
        val actualRaise = if (actionType == ActionType.RAISE) {
            // In Texas Hold’em the minimum raise is either the size of the big blind (pre-flop)
            // or the size of the previous raise (post-flop).
            val minRaise = maxOf(bigBlindAmount, currentBet)
            // You can’t raise more than you have
            raiseAmount.coerceIn(minRaise, playerChips)
        } else {
            0
        }

        performAction(
            0,
            BotDecision(actionType, actualRaise),
            callAmount
        )

        // If you did raise, you become the new aggressor
        if (actionType == ActionType.RAISE) {
            lastAggressorIndex = 0
        }

        // Advance the action
        if (players[0].isFolded || remainingPlayersInHand() <= 1) {
            afterBettingRound()
        } else {
            val next = getNextActivePlayer(0)
            if (next != -1) runBettingRound(next) else afterBettingRound()
        }
    }


    /** Conduct the showdown: compare hands and distribute all pots (main + side) to the winner(s). */
        private fun handleShowdown() {
            // 1) Gather active players and compute their hand strengths
            val activePlayers = players.filter { !it.isFolded && !it.isOut }
            if (activePlayers.isEmpty()) {
                concludeHand()
                return
            }
            data class PlayerHandResult(val playerIndex: Int, val hand: HandType)
            val results = activePlayers.map { player ->
                PlayerHandResult(
                    playerIndex = players.indexOf(player),
                    hand = evaluateHand(player.hand + communityCards)
                )
            }

            // 2) Distribute each SidePot in turn
            for (potInfo in sidePots) {
                // find contenders eligible for this pot
                val contenders = results.filter { potInfo.eligible.contains(it.playerIndex) }
                if (contenders.isEmpty()) continue

                // find best strength among them
                val bestStrength = contenders.maxOf { it.hand.strength }
                val winners      = contenders.filter { it.hand.strength == bestStrength }

                // split this pot evenly, any remainder goes to earliest winner(s)
                val share     = potInfo.amount / winners.size
                val remainder = potInfo.amount % winners.size

                winners.forEachIndexed { idx, winner ->
                    val payout = share + if (idx < remainder) 1 else 0
                    players[winner.playerIndex].chips += payout
                }
            }

            // 3) Reset for next hand
            sidePots.clear()
            sidePots.add(SidePot(0, mutableSetOf(0, 1, 2)))
            pot   = 0    // legacy, no longer used for betting
            stage = Stage.HAND_OVER
            concludeHand()
        }


        private fun handleEndOfHand() {
        val winner = players.firstOrNull { !it.isFolded && !it.isOut } ?: return

        message = "${winner.name} wins $$pot by default!"
        winner.chips += pot

        pot           = 0
        stage         = Stage.HAND_OVER
        lastHandResult = HandResultInfo(listOf(winner.name), "Default Win")
        concludeHand()
    }

    /** Finish the hand and mark any players with zero chips as permanently out. */
    private fun concludeHand() {
        for (player in players) {
            if (player.chips <= 0 && !player.isOut) {
                player.isOut = true
                player.isAllIn = false
            }
        }
    }

    /** Check if the game is over. */
    fun isGameOver(): Boolean {
        val humanIsOut = players[0].isOut || players[0].chips <= 0
        val allBotsOut = players.slice(1..2).all { it.isOut || it.chips <= 0 }
        return humanIsOut || allBotsOut
    }
}

/** Possible action types for betting decisions. */
enum class ActionType { FOLD, CHECK, CALL, RAISE }

/** Data class to represent a bot's chosen action and any associated bet amount. */
data class BotDecision(val type: ActionType, val amount: Int = 0)