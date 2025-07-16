package com.beaker.playsmartcards.shared

import com.beaker.playsmartcards.shared.Card
import com.beaker.playsmartcards.shared.HandType
import com.beaker.playsmartcards.shared.evaluateHand

object GuidanceManager {

    /**
     * Returns a guidance string based on:
     *  - Player’s hand
     *  - Bot’s hand (hidden unless showBotCards == true)
     *  - Community table cards
     *  - The current stage (0=Pre-Flop, etc.)
     */
    fun updateGuidanceEnhanced(
        playerHand: List<Card>,
        botHand: List<Card>,
        tableCards: List<Card?>,
        stage: Int,
        showBotCards: Boolean
    ): String {
        val visibleTable = tableCards.filterNotNull()

        // Evaluate player’s best 5-card combination
        val playerEval = evaluateHand(playerHand + visibleTable)
        val playerDesc = describeHandRank(playerHand + visibleTable)

        // Evaluate bot’s hand only if we’re showing them
        val botDesc = if (showBotCards) {
            val botEval = evaluateHand(botHand + visibleTable)
            "${describeHandRank(botHand + visibleTable)} (Strength: $botEval)"
        } else {
            "(Hidden Hand)"
        }

        // Map stage index to a label
        val stageMsg = when (stage) {
            0 -> "Pre-Flop"
            1 -> "Flop"
            2 -> "Turn"
            3 -> "River"
            4 -> "Showdown"
            else -> "Unknown"
        }

        // Very simple “advice” based on playerEval
        val advice = when {
            playerEval.ordinal < HandType.PAIR.ordinal ->
                "You likely have a strong hand. Consider raising."
            playerEval.ordinal == HandType.PAIR.ordinal ->
                "A pair can be decent—calling might be safe."
            else ->
                "Your hand might be weak. Consider folding if big raises come."
        }

        return buildString {
            appendLine("Stage: $stageMsg")
            appendLine("Your Best Hand: $playerDesc")
            appendLine("Bot’s Hand: $botDesc")
            appendLine("Advice: $advice")
        }
    }

    /**
     * Converts a hand type into a user-friendly string, e.g.:
     * HandType.FULL_HOUSE -> "a Full House"
     */
    private fun describeHandRank(cards: List<Card>): String {
        val handType = evaluateHand(cards)
        return when (handType) {
            HandType.ROYAL_FLUSH     -> "a Royal Flush"
            HandType.STRAIGHT_FLUSH  -> "a Straight Flush"
            HandType.FOUR_OF_A_KIND  -> "Four of a Kind"
            HandType.FULL_HOUSE      -> "a Full House"
            HandType.FLUSH           -> "a Flush"
            HandType.STRAIGHT        -> "a Straight"
            HandType.THREE_OF_A_KIND -> "Three of a Kind"
            HandType.TWO_PAIR        -> "Two Pair"
            HandType.PAIR            -> "a Pair"
            HandType.HIGH_CARD       -> "a High Card"
        }
    }
}
