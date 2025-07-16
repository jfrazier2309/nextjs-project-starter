package com.beaker.playsmartcards.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beaker.playsmartcards.R

// Define suits as constants
const val SUIT_SPADES = 0
const val SUIT_HEARTS = 1
const val SUIT_DIAMONDS = 2
const val SUIT_CLUBS = 3

// Enum to represent hand rankings
enum class HandType(val strength: Int) {
    HIGH_CARD(1),
    PAIR(2),
    TWO_PAIR(3),
    THREE_OF_A_KIND(4),
    STRAIGHT(5),
    FLUSH(6),
    FULL_HOUSE(7),
    FOUR_OF_A_KIND(8),
    STRAIGHT_FLUSH(9),
    ROYAL_FLUSH(10)
}

data class Card(
    val rank: Int, // 2 to 14 (2=Two, ..., 14=Ace)
    val suit: Int, // SUIT_SPADES, SUIT_HEARTS, etc.
    val imageRes: Int // Resource ID for the card image
) {
    // Override toString for easy debugging
    override fun toString(): String {
        val rankStr = when (rank) {
            11 -> "J"
            12 -> "Q"
            13 -> "K"
            14 -> "A"
            else -> rank.toString()
        }
        val suitStr = when (suit) {
            SUIT_SPADES -> "s"
            SUIT_HEARTS -> "h"
            SUIT_DIAMONDS -> "d"
            SUIT_CLUBS -> "c"
            else -> "?"
        }
        return "$rankStr$suitStr"
    }
}


@Composable
fun PokerCardShared(
    card: Card?,
    modifier: Modifier = Modifier,
    cardWidth: Dp = 60.dp,
    cardHeight: Dp = 90.dp
) {
    val imageId = card?.imageRes ?: R.drawable.back_of_playing_card_red
    Card(
        modifier = modifier
            .size(width = cardWidth, height = cardHeight)
            .padding(2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Image(
            painter = painterResource(id = imageId),
            contentDescription = card?.toString() ?: "Card Back",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

private val fullDeckResources = listOf(
    Triple(14, SUIT_SPADES, R.drawable.ace_spade),
    Triple(2, SUIT_SPADES, R.drawable.two_spade),
    Triple(3, SUIT_SPADES, R.drawable.three_spade),
    Triple(4, SUIT_SPADES, R.drawable.four_spade),
    Triple(5, SUIT_SPADES, R.drawable.five_spade),
    Triple(6, SUIT_SPADES, R.drawable.six_spade),
    Triple(7, SUIT_SPADES, R.drawable.seven_spade),
    Triple(8, SUIT_SPADES, R.drawable.eight_spade),
    Triple(9, SUIT_SPADES, R.drawable.nine_spade),
    Triple(10, SUIT_SPADES, R.drawable.ten_spade),
    Triple(11, SUIT_SPADES, R.drawable.jack_spade),
    Triple(12, SUIT_SPADES, R.drawable.queen_spade),
    Triple(13, SUIT_SPADES, R.drawable.king_spade),
    Triple(14, SUIT_HEARTS, R.drawable.ace_heart),
    Triple(2, SUIT_HEARTS, R.drawable.two_heart),
    Triple(3, SUIT_HEARTS, R.drawable.three_heart),
    Triple(4, SUIT_HEARTS, R.drawable.four_heart),
    Triple(5, SUIT_HEARTS, R.drawable.five_heart),
    Triple(6, SUIT_HEARTS, R.drawable.six_heart),
    Triple(7, SUIT_HEARTS, R.drawable.seven_heart),
    Triple(8, SUIT_HEARTS, R.drawable.eight_heart),
    Triple(9, SUIT_HEARTS, R.drawable.nine_heart),
    Triple(10, SUIT_HEARTS, R.drawable.ten_heart),
    Triple(11, SUIT_HEARTS, R.drawable.jack_heart),
    Triple(12, SUIT_HEARTS, R.drawable.queen_heart),
    Triple(13, SUIT_HEARTS, R.drawable.king_heart),
    Triple(14, SUIT_DIAMONDS, R.drawable.ace_diamond),
    Triple(2, SUIT_DIAMONDS, R.drawable.two_diamond),
    Triple(3, SUIT_DIAMONDS, R.drawable.three_diamond),
    Triple(4, SUIT_DIAMONDS, R.drawable.four_diamond),
    Triple(5, SUIT_DIAMONDS, R.drawable.five_diamond),
    Triple(6, SUIT_DIAMONDS, R.drawable.six_diamond),
    Triple(7, SUIT_DIAMONDS, R.drawable.seven_diamond),
    Triple(8, SUIT_DIAMONDS, R.drawable.eight_diamond),
    Triple(9, SUIT_DIAMONDS, R.drawable.nine_diamond),
    Triple(10, SUIT_DIAMONDS, R.drawable.ten_diamond),
    Triple(11, SUIT_DIAMONDS, R.drawable.jack_diamond),
    Triple(12, SUIT_DIAMONDS, R.drawable.queen_diamond),
    Triple(13, SUIT_DIAMONDS, R.drawable.king_diamond),
    Triple(14, SUIT_CLUBS, R.drawable.ace_clover),
    Triple(2, SUIT_CLUBS, R.drawable.two_clover),
    Triple(3, SUIT_CLUBS, R.drawable.three_clover),
    Triple(4, SUIT_CLUBS, R.drawable.four_clover),
    Triple(5, SUIT_CLUBS, R.drawable.five_clover),
    Triple(6, SUIT_CLUBS, R.drawable.six_clover),
    Triple(7, SUIT_CLUBS, R.drawable.seven_clover),
    Triple(8, SUIT_CLUBS, R.drawable.eight_clover),
    Triple(9, SUIT_CLUBS, R.drawable.nine_clover),
    Triple(10, SUIT_CLUBS, R.drawable.ten_clover),
    Triple(11, SUIT_CLUBS, R.drawable.jack_clover),
    Triple(12, SUIT_CLUBS, R.drawable.queen_clover),
    Triple(13, SUIT_CLUBS, R.drawable.king_clover)
)

fun initializeDeckShared(deck: MutableList<Card>) {
    deck.clear()
    val allCards = fullDeckResources.map { (rank, suit, res) ->
        Card(rank, suit, res)
    }
    deck.addAll(allCards.shuffled())
}

private fun <T> List<T>.getCombinations(k: Int): Sequence<List<T>> {
    if (k < 0 || k > size) {
        return emptySequence()
    }
    var indices = IntArray(k) { it }
    return sequence {
        while (true) {
            yield(indices.map { this@getCombinations[it] })
            var i = k - 1
            while (i >= 0 && indices[i] == i + size - k) {
                i--
            }
            if (i < 0) {
                break
            }
            indices[i]++
            for (j in i + 1 until k) {
                indices[j] = indices[j - 1] + 1
            }
        }
    }
}

fun evaluateHand(cards: List<Card>): HandType {
    if (cards.size < 5) return evaluatePartialHand(cards)
    return cards.getCombinations(5)
        .map { combo -> evaluateFiveCardHand(combo) }
        .maxByOrNull { it.strength } ?: HandType.HIGH_CARD
}

private fun evaluatePartialHand(cards: List<Card>): HandType {
    // Simplified evaluation for hands with fewer than 5 cards
    val rankCounts = cards.groupingBy { it.rank }.eachCount()
    val isFlush = if (cards.isNotEmpty()) cards.map { it.suit }.distinct().size == 1 else false

    return when {
        rankCounts.containsValue(4) -> HandType.FOUR_OF_A_KIND
        rankCounts.containsValue(3) -> HandType.THREE_OF_A_KIND
        rankCounts.values.count { it == 2 } >= 1 -> HandType.PAIR
        isFlush -> HandType.FLUSH
        else -> HandType.HIGH_CARD
    }
}

private fun evaluateFiveCardHand(cards: List<Card>): HandType {
    val ranks = cards.map { it.rank }
    val suits = cards.map { it.suit }
    val rankCounts = ranks.groupingBy { it }.eachCount()
    val isFlush = suits.distinct().size == 1
    val isStraight = checkIfStraight(ranks)

    return when {
        isFlush && isStraight -> if (ranks.contains(14) && ranks.contains(13)) HandType.ROYAL_FLUSH else HandType.STRAIGHT_FLUSH
        rankCounts.containsValue(4) -> HandType.FOUR_OF_A_KIND
        rankCounts.containsValue(3) && rankCounts.containsValue(2) -> HandType.FULL_HOUSE
        isFlush -> HandType.FLUSH
        isStraight -> HandType.STRAIGHT
        rankCounts.containsValue(3) -> HandType.THREE_OF_A_KIND
        rankCounts.values.count { it == 2 } == 2 -> HandType.TWO_PAIR
        rankCounts.containsValue(2) -> HandType.PAIR
        else -> HandType.HIGH_CARD
    }
}


fun checkIfStraight(ranks: List<Int>): Boolean {
    val distinctRanks = ranks.distinct().sorted()
    if (distinctRanks.size < 5) return false

    // Check for standard straight
    for (i in 0..distinctRanks.size - 5) {
        val window = distinctRanks.subList(i, i + 5)
        var isConsecutive = true
        for (j in 0..3) {
            if (window[j] + 1 != window[j + 1]) {
                isConsecutive = false
                break
            }
        }
        if (isConsecutive) return true
    }

    // Check for Ace-low straight (A, 2, 3, 4, 5)
    val aceLow = distinctRanks.containsAll(listOf(2, 3, 4, 5, 14))
    return aceLow
}

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
    }
}

fun updateGuidanceEnhanced(
    playerHand: List<Card>,
    botHand: List<Card>,
    tableCards: List<Card?>,
    stage: Int,
    showBotCards: Boolean
): String {
    val visibleTable = tableCards.filterNotNull()
    val playerEval = evaluateHand(playerHand + visibleTable)
    val playerDesc = describeHandRank(playerHand + visibleTable)

    val botDesc = if (showBotCards) {
        val botEval = evaluateHand(botHand + visibleTable)
        "${describeHandRank(botHand + visibleTable)} (Strength: $botEval)"
    } else {
        "(Hidden Hand)"
    }

    val stageMsg = when (stage) {
        0 -> "Pre-Flop"
        1 -> "Flop"
        2 -> "Turn"
        3 -> "River"
        4 -> "Showdown"
        else -> "Unknown"
    }

    val advice = when {
        playerEval.strength >= HandType.TWO_PAIR.strength -> "You have a strong hand. Consider raising."
        playerEval.strength == HandType.PAIR.strength -> "A pair can be decent—calling might be safe."
        else -> "Your hand might be weak. Consider folding if big raises come."
    }

    return "Stage: $stageMsg\nYour Best Hand: $playerDesc\nBot’s Hand: $botDesc\nAdvice: $advice"
}