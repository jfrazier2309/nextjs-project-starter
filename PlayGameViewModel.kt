package com.beaker.playsmartcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beaker.playsmartcards.logic.ActionType
import com.beaker.playsmartcards.logic.GameLogic
import com.beaker.playsmartcards.logic.Player
import com.beaker.playsmartcards.logic.Stage
import com.beaker.playsmartcards.shared.Card
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SoundEvent {
    CARD_FLIP,
    CHIP,
    SHUFFLE
}

class PlayGameViewModel : ViewModel() {
    private val gameLogic = GameLogic()

    // --- NEW: expose currentBet and bigBlindAmount ---
    private val _currentBet = MutableStateFlow(gameLogic.currentBet)
    val currentBet = _currentBet.asStateFlow()

    // bigBlindAmount is constant in your logic
    val bigBlindAmount: Int = gameLogic.bigBlindAmount

    // --- existing UI state ---
    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players = _players.asStateFlow()

    private val _communityCards = MutableStateFlow<List<Card>>(emptyList())
    val communityCards = _communityCards.asStateFlow()

    private val _stage = MutableStateFlow(Stage.PRE_FLOP)
    val stage = _stage.asStateFlow()

    private val _pot = MutableStateFlow(0)
    val pot = _pot.asStateFlow()

    private val _message = MutableStateFlow("")
    val message = _message.asStateFlow()

    private val _currentActorIndex = MutableStateFlow(-1)
    val currentActorIndex = _currentActorIndex.asStateFlow()

    private val _amountToCall = MutableStateFlow(0)
    val amountToCall = _amountToCall.asStateFlow()

    // --- statistics ---
    private val _playerWins = MutableStateFlow(0)
    val playerWins = _playerWins.asStateFlow()

    private val _handsPlayed = MutableStateFlow(0)
    val handsPlayed = _handsPlayed.asStateFlow()

    // --- sound events ---
    private val _soundEvent = MutableSharedFlow<SoundEvent>()
    val soundEvent = _soundEvent.asSharedFlow()

    // --- settings ---
    private val _soundEnabled = MutableStateFlow(true)
    val soundEnabled = _soundEnabled.asStateFlow()

    private val _difficulty = MutableStateFlow("Easy")
    val difficulty = _difficulty.asStateFlow()

    init {
        startNewGame()
    }

    fun changeDifficulty() {
        val newDifficulty = when (_difficulty.value) {
            "Easy" -> "Medium"
            "Medium" -> "Hard"
            else -> "Easy"
        }
        _difficulty.value = newDifficulty
        gameLogic.difficulty = newDifficulty
    }

    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
    }

    private fun startNewGame() {
        viewModelScope.launch {
            gameLogic.startNewRound()
            if (_soundEnabled.value) {
                _soundEvent.emit(SoundEvent.SHUFFLE)
            }
            updateState()
        }
    }

    fun handlePlayerAction(actionType: ActionType, amount: Int = 0) {
        viewModelScope.launch {
            if (_soundEnabled.value && (actionType == ActionType.CALL || actionType == ActionType.RAISE)) {
                _soundEvent.emit(SoundEvent.CHIP)
            }
            gameLogic.handlePlayerAction(actionType, amount)
            updateState()
            checkHandEnd()
        }
    }

    fun startNewRound() {
        viewModelScope.launch {
            if (gameLogic.isGameOver()) return@launch
            gameLogic.startNewRound()
            if (_soundEnabled.value) {
                _soundEvent.emit(SoundEvent.SHUFFLE)
            }
            updateState()
        }
    }

    private fun updateState() {
        _players.value = gameLogic.players.toList()

        if (_soundEnabled.value && gameLogic.communityCards.size > _communityCards.value.size) {
            viewModelScope.launch { _soundEvent.emit(SoundEvent.CARD_FLIP) }
        }
        _communityCards.value    = gameLogic.communityCards.toList()
        _stage.value             = gameLogic.stage
        _pot.value               = gameLogic.pot
        _message.value           = gameLogic.message
        _currentActorIndex.value = gameLogic.currentActorIndex

        // Update our new currentBet flow
        _currentBet.value = gameLogic.currentBet

        _amountToCall.value = if (gameLogic.currentActorIndex == 0) {
            gameLogic.getAmountToCall(0)
        } else 0
    }

    private fun checkHandEnd() {
        if (gameLogic.stage == Stage.SHOWDOWN) {
            _handsPlayed.value++
            if (gameLogic.lastHandResult?.winnerNames?.contains("You") == true) {
                _playerWins.value++
            }
        }
    }
}
