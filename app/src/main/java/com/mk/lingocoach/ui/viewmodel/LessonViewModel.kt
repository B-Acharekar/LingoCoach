package com.mk.lingocoach.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.CompleteExerciseRequest
import com.mk.lingocoach.network.CurrentLearningPathResponse
import com.mk.lingocoach.network.Exercise
import com.mk.lingocoach.network.SublessonDetail
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

// ─── Phase ───────────────────────────────────────────────────────────────────

enum class Phase { LOADING, CONTENT, EXERCISE, COMPLETE, ERROR }

// ─── ExerciseSource ──────────────────────────────────────────────────────────

enum class ExerciseSource { ORIGINAL, RETRY }

// ─── AnswerState ─────────────────────────────────────────────────────────────

sealed class AnswerState {
    object Unanswered : AnswerState()
    data class Correct(val answer: String) : AnswerState()
    data class Incorrect(val answer: String) : AnswerState()
}

// ─── LessonUiState ───────────────────────────────────────────────────────────

data class LessonUiState(
    val phase: Phase = Phase.LOADING,
    val sublesson: SublessonDetail? = null,
    val learningPath: CurrentLearningPathResponse? = null,
    val originalExercises: List<Exercise> = emptyList(),
    val retryQueue: ArrayDeque<Exercise> = ArrayDeque(),
    val currentExerciseSource: ExerciseSource = ExerciseSource.ORIGINAL,
    val currentOriginalIndex: Int = 0,
    val correctOriginalCount: Int = 0,
    val answerState: AnswerState = AnswerState.Unanswered,
    val feedback: String = "",
    val isSubmitting: Boolean = false,
    val completionSent: Boolean = false,
    val totalXpEarned: Int = 0,
    val error: String? = null
)

// ─── LessonViewModel ─────────────────────────────────────────────────────────

class LessonViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    // ─── Load sublesson + learning path concurrently ──────────────────────────

    fun loadSublesson(sublessonId: String, userId: String) {
        _uiState.update { LessonUiState(phase = Phase.LOADING) }
        viewModelScope.launch {
            try {
                val sublesson = suspendCoroutine<SublessonDetail?> { cont ->
                    AssessmentApi.getSublesson(sublessonId) { cont.resume(it) }
                }
                val learningPath = suspendCoroutine<CurrentLearningPathResponse?> { cont ->
                    AssessmentApi.getCurrentLearningPath(userId) { cont.resume(it) }
                }
                if (sublesson == null) {
                    _uiState.update { it.copy(phase = Phase.ERROR, error = "Failed to load sublesson.") }
                    return@launch
                }
                _uiState.update { current ->
                    current.copy(
                        phase = Phase.CONTENT,
                        sublesson = sublesson,
                        learningPath = learningPath,
                        originalExercises = sublesson.exercises,
                        retryQueue = ArrayDeque(),
                        currentExerciseSource = ExerciseSource.ORIGINAL,
                        currentOriginalIndex = 0,
                        correctOriginalCount = 0,
                        answerState = AnswerState.Unanswered,
                        feedback = "",
                        isSubmitting = false,
                        completionSent = false,
                        totalXpEarned = 0,
                        error = null
                    )
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "loadSublesson error", e)
                _uiState.update { it.copy(phase = Phase.ERROR, error = e.message ?: "Unknown error.") }
            }
        }
    }

    // ─── Transition from content phase to exercise phase ─────────────────────

    fun startExercises() {
        _uiState.update { current ->
            current.copy(
                phase = Phase.EXERCISE,
                currentOriginalIndex = 0,
                retryQueue = ArrayDeque(),
                correctOriginalCount = 0,
                currentExerciseSource = ExerciseSource.ORIGINAL,
                answerState = AnswerState.Unanswered,
                feedback = ""
            )
        }
    }

    // ─── Submit an answer for the active exercise ─────────────────────────────

    fun submitAnswer(answer: String, userId: String) {
        val state = _uiState.value
        val sublesson = state.sublesson ?: return
        val exercise = activeExercise(state) ?: return

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            try {
                val request = CompleteExerciseRequest(
                    user_id = userId,
                    sublesson_id = sublesson.id,
                    exercise_id = exercise.id,
                    user_answer = answer,
                    audio_transcription = ""
                )
                val response = suspendCoroutine<com.mk.lingocoach.network.CompleteExerciseResponse?> { cont ->
                    AssessmentApi.completeExercise(request) { cont.resume(it) }
                }
                if (response != null) {
                    val newAnswerState = if (response.is_correct) {
                        AnswerState.Correct(answer)
                    } else {
                        AnswerState.Incorrect(answer)
                    }
                    _uiState.update { it.copy(
                        isSubmitting = false,
                        answerState = newAnswerState,
                        feedback = response.feedback
                    ) }
                } else {
                    // Fallback: local evaluation using correct_answer
                    val isCorrect = exercise.correct_answer
                        ?.let { it.trim().equals(answer.trim(), ignoreCase = true) }
                        ?: false
                    val newAnswerState = if (isCorrect) {
                        AnswerState.Correct(answer)
                    } else {
                        AnswerState.Incorrect(answer)
                    }
                    _uiState.update { it.copy(
                        isSubmitting = false,
                        answerState = newAnswerState,
                        feedback = if (isCorrect) "Correct!" else "Incorrect. The correct answer is: ${exercise.correct_answer}"
                    ) }
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "submitAnswer error", e)
                // Fallback on exception
                val exercise2 = activeExercise(_uiState.value) ?: return@launch
                val isCorrect = exercise2.correct_answer
                    ?.let { it.trim().equals(answer.trim(), ignoreCase = true) }
                    ?: false
                val newAnswerState = if (isCorrect) {
                    AnswerState.Correct(answer)
                } else {
                    AnswerState.Incorrect(answer)
                }
                _uiState.update { it.copy(
                    isSubmitting = false,
                    answerState = newAnswerState,
                    feedback = if (isCorrect) "Correct!" else "Incorrect. The correct answer is: ${exercise2.correct_answer}"
                ) }
            }
        }
    }

    // ─── Advance the session state machine ───────────────────────────────────

    fun advance() {
        val state = _uiState.value
        val answerState = state.answerState
        if (answerState is AnswerState.Unanswered) return

        val exercise = activeExercise(state) ?: return

        when {
            // ── Correct answer from ORIGINAL list ──────────────────────────
            answerState is AnswerState.Correct && state.currentExerciseSource == ExerciseSource.ORIGINAL -> {
                val newCorrectCount = state.correctOriginalCount + 1
                val newIndex = state.currentOriginalIndex + 1
                val exhaustedOriginal = newIndex >= state.originalExercises.size

                _uiState.update { current ->
                    val newSource = if (exhaustedOriginal && current.retryQueue.isNotEmpty()) {
                        ExerciseSource.RETRY
                    } else {
                        ExerciseSource.ORIGINAL
                    }
                    current.copy(
                        correctOriginalCount = newCorrectCount,
                        currentOriginalIndex = newIndex,
                        currentExerciseSource = newSource,
                        answerState = AnswerState.Unanswered,
                        feedback = ""
                    )
                }
            }

            // ── Incorrect answer from ORIGINAL list ────────────────────────
            answerState is AnswerState.Incorrect && state.currentExerciseSource == ExerciseSource.ORIGINAL -> {
                val newRetryQueue = ArrayDeque(state.retryQueue).also { it.addLast(exercise) }
                val newIndex = state.currentOriginalIndex + 1
                val exhaustedOriginal = newIndex >= state.originalExercises.size

                _uiState.update { current ->
                    val newSource = if (exhaustedOriginal) {
                        ExerciseSource.RETRY
                    } else {
                        ExerciseSource.ORIGINAL
                    }
                    current.copy(
                        retryQueue = newRetryQueue,
                        currentOriginalIndex = newIndex,
                        currentExerciseSource = newSource,
                        answerState = AnswerState.Unanswered,
                        feedback = ""
                    )
                }
            }

            // ── Correct answer from RETRY queue ────────────────────────────
            answerState is AnswerState.Correct && state.currentExerciseSource == ExerciseSource.RETRY -> {
                val newRetryQueue = ArrayDeque(state.retryQueue).also {
                    if (it.isNotEmpty()) it.removeFirst()
                }
                _uiState.update { current ->
                    current.copy(
                        retryQueue = newRetryQueue,
                        answerState = AnswerState.Unanswered,
                        feedback = ""
                    )
                }
            }

            // ── Incorrect answer from RETRY queue ──────────────────────────
            answerState is AnswerState.Incorrect && state.currentExerciseSource == ExerciseSource.RETRY -> {
                val newRetryQueue = ArrayDeque(state.retryQueue)
                if (newRetryQueue.isNotEmpty()) {
                    val head = newRetryQueue.removeFirst()
                    newRetryQueue.addLast(head)
                }
                _uiState.update { current ->
                    current.copy(
                        retryQueue = newRetryQueue,
                        answerState = AnswerState.Unanswered,
                        feedback = ""
                    )
                }
            }
        }
    }

    // ─── Complete the sublesson (tap Complete Button) ─────────────────────────

    fun completeSublesson(userId: String, sublessonId: String) {
        val state = _uiState.value
        if (state.completionSent) return

        _uiState.update { it.copy(completionSent = true) }

        viewModelScope.launch {
            try {
                val exercises = state.originalExercises
                // Use last exercise id, or first if list is short, or empty string if none
                val exerciseId = when {
                    exercises.isNotEmpty() -> exercises.last().id
                    else -> ""
                }
                val request = CompleteExerciseRequest(
                    user_id = userId,
                    sublesson_id = sublessonId,
                    exercise_id = exerciseId,
                    user_answer = "",
                    audio_transcription = "",
                    sublesson_complete = true
                )
                val response = suspendCoroutine<com.mk.lingocoach.network.CompleteExerciseResponse?> { cont ->
                    AssessmentApi.completeExercise(request) { cont.resume(it) }
                }
                if (response != null) {
                    _uiState.update { it.copy(
                        totalXpEarned = response.xp_earned,
                        phase = Phase.COMPLETE
                    ) }
                } else {
                    // Network failure — allow retry
                    _uiState.update { it.copy(
                        completionSent = false,
                        error = "Failed to record completion. Please try again."
                    ) }
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "completeSublesson error", e)
                _uiState.update { it.copy(
                    completionSent = false,
                    error = e.message ?: "Failed to record completion."
                ) }
            }
        }
    }

    // ─── Reset all session state ──────────────────────────────────────────────

    fun reset() {
        _uiState.update { LessonUiState() }
    }

    // ─── Helper: derive the currently active exercise ─────────────────────────

    fun activeExercise(state: LessonUiState = _uiState.value): Exercise? {
        return when {
            state.currentExerciseSource == ExerciseSource.ORIGINAL &&
                    state.currentOriginalIndex < state.originalExercises.size ->
                state.originalExercises[state.currentOriginalIndex]

            state.currentExerciseSource == ExerciseSource.RETRY &&
                    state.retryQueue.isNotEmpty() ->
                state.retryQueue.first()

            else -> null
        }
    }
}
