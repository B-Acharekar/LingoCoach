package com.mk.lingocoach.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mk.lingocoach.network.AssessmentApi
import com.mk.lingocoach.network.CompleteExerciseRequest
import com.mk.lingocoach.network.CurrentLearningPathResponse
import com.mk.lingocoach.network.Exercise
import com.mk.lingocoach.network.SublessonDetail
import com.mk.lingocoach.ui.screens.AppCache
import com.mk.lingocoach.ui.screens.normalizedLearningPath
import com.mk.lingocoach.ui.screens.withCompletedSublesson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.resume

// ─── Phase ────────────────────────────────────────────────────────────────────
enum class Phase { LOADING, CONTENT, EXERCISE, COMPLETE, ERROR }

// ─── ExerciseSource ───────────────────────────────────────────────────────────
enum class ExerciseSource { ORIGINAL, RETRY }

// ─── AnswerState ──────────────────────────────────────────────────────────────
sealed class AnswerState {
    object Unanswered : AnswerState()
    data class Correct(val answer: String) : AnswerState()
    data class Incorrect(val answer: String) : AnswerState()
}

// ─── LessonUiState ────────────────────────────────────────────────────────────
data class LessonUiState(
    val phase: Phase = Phase.LOADING,
    val sublesson: SublessonDetail? = null,
    val learningPath: CurrentLearningPathResponse? = null,
    val originalExercises: List<Exercise> = emptyList(),
    val retryQueue: ArrayDeque<Exercise> = ArrayDeque(),
    val currentExerciseSource: ExerciseSource = ExerciseSource.ORIGINAL,
    val currentOriginalIndex: Int = 0,      // pointer into originalExercises
    val correctOriginalCount: Int = 0,      // how many originals answered correctly (for progress bar)
    val answeredCount: Int = 0,             // total questions shown so far (original + retry) – for top-bar display
    val answerState: AnswerState = AnswerState.Unanswered,
    val feedback: String = "",
    val isSubmitting: Boolean = false,
    val completionSent: Boolean = false,
    val totalXpEarned: Int = 0,
    val error: String? = null,
    // ── NEW: the exercise that is currently on-screen ──────────────────────────
    // We fix it here so the UI never reads a stale/transitioning value.
    val activeExercise: Exercise? = null,
    val showCompleteButton: Boolean = false
)

// ─── LessonViewModel ──────────────────────────────────────────────────────────
class LessonViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LessonUiState())
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    // ── Load sublesson + learning path ────────────────────────────────────────
    fun loadSublesson(sublessonId: String, userId: String) {
        _uiState.update { LessonUiState(phase = Phase.LOADING) }
        viewModelScope.launch {
            try {
                val sublesson = suspendCoroutine<SublessonDetail?> { cont ->
                    AssessmentApi.getSublesson(sublessonId) { cont.resume(it) }
                }
                val learningPath = suspendCoroutine<CurrentLearningPathResponse?> { cont ->
                    AssessmentApi.getCurrentLearningPath(userId) { cont.resume(it) }
                }?.let { AppCache.applyLocalLearningPathProgress(it) }
                if (sublesson == null) {
                    _uiState.update { it.copy(phase = Phase.ERROR, error = "Failed to load sublesson.") }
                    return@launch
                }
                _uiState.update {
                    LessonUiState(
                        phase             = Phase.CONTENT,
                        sublesson         = sublesson,
                        learningPath      = learningPath,
                        originalExercises = sublesson.exercises
                    )
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "loadSublesson error", e)
                _uiState.update { it.copy(phase = Phase.ERROR, error = e.message ?: "Unknown error.") }
            }
        }
    }

    // ── Transition from content → exercise phase ──────────────────────────────
    fun startExercises() {
        _uiState.update { current ->
            val first = current.originalExercises.firstOrNull()
            current.copy(
                phase                 = Phase.EXERCISE,
                currentOriginalIndex  = 0,
                retryQueue            = ArrayDeque(),
                correctOriginalCount  = 0,
                answeredCount         = 0,
                currentExerciseSource = ExerciseSource.ORIGINAL,
                answerState           = AnswerState.Unanswered,
                feedback              = "",
                activeExercise        = first,    // ← set immediately, no flash
                showCompleteButton    = false
            )
        }
    }

    // ── Submit answer ─────────────────────────────────────────────────────────
    fun submitAnswer(answer: String, userId: String) {
        val state    = _uiState.value
        val sublesson = state.sublesson ?: return
        val exercise  = state.activeExercise ?: return

        _uiState.update { it.copy(isSubmitting = true) }

        viewModelScope.launch {
            try {
                val request = CompleteExerciseRequest(
                    user_id              = userId,
                    sublesson_id         = sublesson.id,
                    exercise_id          = exercise.id,
                    user_answer          = answer,
                    audio_transcription  = ""
                )
                val response = suspendCoroutine<com.mk.lingocoach.network.CompleteExerciseResponse?> { cont ->
                    AssessmentApi.completeExercise(request) { cont.resume(it) }
                }
                val isCorrect = response?.is_correct
                    ?: exercise.correct_answer?.trim().equals(answer.trim(), ignoreCase = true)

                val feedback = response?.feedback
                    ?: if (isCorrect) "Correct!" else "Incorrect. The correct answer is: ${exercise.correct_answer}"

                _uiState.update { it.copy(
                    isSubmitting = false,
                    answerState  = if (isCorrect) AnswerState.Correct(answer) else AnswerState.Incorrect(answer),
                    feedback     = feedback
                ) }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "submitAnswer error", e)
                val isCorrect = state.activeExercise?.correct_answer
                    ?.trim().equals(answer.trim(), ignoreCase = true) == true
                _uiState.update { it.copy(
                    isSubmitting = false,
                    answerState  = if (isCorrect) AnswerState.Correct(answer) else AnswerState.Incorrect(answer),
                    feedback     = if (isCorrect) "Correct!" else "Incorrect. The correct answer is: ${state.activeExercise?.correct_answer}"
                ) }
            }
        }
    }

    // ── Advance state machine ─────────────────────────────────────────────────
    // Key contract: when we move to the next question we atomically set
    // activeExercise = <next>, answerState = Unanswered, feedback = "".
    // The UI reads activeExercise — never derives it itself — so there is
    // zero window where the wrong state is shown.
    fun advance() {
        val state = _uiState.value
        if (state.answerState is AnswerState.Unanswered) return
        val exercise = state.activeExercise ?: return
        val isCorrect = state.answerState is AnswerState.Correct

        _uiState.update { current ->
            when {
                // ── Original list, correct ────────────────────────────────
                current.currentExerciseSource == ExerciseSource.ORIGINAL && isCorrect -> {
                    val newCorrect = current.correctOriginalCount + 1
                    val newIndex   = current.currentOriginalIndex + 1
                    val origDone   = newIndex >= current.originalExercises.size
                    val retryLeft  = current.retryQueue.isNotEmpty()

                    val nextSource  = if (origDone && retryLeft) ExerciseSource.RETRY else ExerciseSource.ORIGINAL
                    val nextExercise = nextExercise(current.originalExercises, current.retryQueue, nextSource, newIndex)
                    val allDone     = origDone && !retryLeft

                    current.copy(
                        correctOriginalCount  = newCorrect,
                        currentOriginalIndex  = newIndex,
                        answeredCount         = current.answeredCount + 1,
                        currentExerciseSource = nextSource,
                        answerState           = AnswerState.Unanswered,
                        feedback              = "",
                        activeExercise        = nextExercise,
                        showCompleteButton    = allDone
                    )
                }

                // ── Original list, wrong → push to retry queue ────────────
                current.currentExerciseSource == ExerciseSource.ORIGINAL && !isCorrect -> {
                    val newRetry   = ArrayDeque(current.retryQueue).also { it.addLast(exercise) }
                    val newIndex   = current.currentOriginalIndex + 1
                    val origDone   = newIndex >= current.originalExercises.size

                    val nextSource  = if (origDone) ExerciseSource.RETRY else ExerciseSource.ORIGINAL
                    val nextExercise = nextExercise(current.originalExercises, newRetry, nextSource, newIndex)
                    // If we just exhausted originals and the retry queue was empty before
                    // (this wrong answer is the only one), allDone is still false — retry handles it.
                    val allDone = false

                    current.copy(
                        retryQueue            = newRetry,
                        currentOriginalIndex  = newIndex,
                        answeredCount         = current.answeredCount + 1,
                        currentExerciseSource = nextSource,
                        answerState           = AnswerState.Unanswered,
                        feedback              = "",
                        activeExercise        = nextExercise,
                        showCompleteButton    = allDone
                    )
                }

                // ── Retry queue, correct → remove from queue ──────────────
                current.currentExerciseSource == ExerciseSource.RETRY && isCorrect -> {
                    val newRetry = ArrayDeque(current.retryQueue).also { if (it.isNotEmpty()) it.removeFirst() }
                    val origDone = current.currentOriginalIndex >= current.originalExercises.size
                    val allDone  = origDone && newRetry.isEmpty()

                    val nextSource   = if (!origDone) ExerciseSource.ORIGINAL else ExerciseSource.RETRY
                    val nextExercise = if (allDone) null
                                       else nextExercise(current.originalExercises, newRetry, nextSource, current.currentOriginalIndex)

                    current.copy(
                        retryQueue            = newRetry,
                        answeredCount         = current.answeredCount + 1,
                        currentExerciseSource = nextSource,
                        answerState           = AnswerState.Unanswered,
                        feedback              = "",
                        activeExercise        = nextExercise,
                        showCompleteButton    = allDone
                    )
                }

                // ── Retry queue, wrong → rotate to back of queue ──────────
                current.currentExerciseSource == ExerciseSource.RETRY && !isCorrect -> {
                    val newRetry = ArrayDeque(current.retryQueue)
                    if (newRetry.isNotEmpty()) { val h = newRetry.removeFirst(); newRetry.addLast(h) }

                    current.copy(
                        retryQueue         = newRetry,
                        answeredCount      = current.answeredCount + 1,
                        answerState        = AnswerState.Unanswered,
                        feedback           = "",
                        activeExercise     = newRetry.firstOrNull(),
                        showCompleteButton = false
                    )
                }

                else -> current
            }
        }
    }

    // ── Complete the sublesson ────────────────────────────────────────────────
    fun completeSublesson(userId: String, sublessonId: String) {
        if (_uiState.value.completionSent) return
        _uiState.update { it.copy(completionSent = true) }

        viewModelScope.launch {
            try {
                val exercises  = _uiState.value.originalExercises
                val exerciseId = exercises.lastOrNull()?.id ?: ""
                val request = CompleteExerciseRequest(
                    user_id              = userId,
                    sublesson_id         = sublessonId,
                    exercise_id          = exerciseId,
                    user_answer          = "",
                    audio_transcription  = "",
                    sublesson_complete   = true
                )
                val response = suspendCoroutine<com.mk.lingocoach.network.CompleteExerciseResponse?> { cont ->
                    AssessmentApi.completeExercise(request) { cont.resume(it) }
                }
                if (response != null) {
                    AppCache.rememberCompletedSublesson(sublessonId)
                    // ── FIX: invalidate the cached learning path ────────────────
                    // AppCache.learningPath is what HomeScreen and
                    // ActualLearningPathScreen read to render lesson statuses and
                    // the progress percentage. It was never being touched here,
                    // so after completing a lesson those screens kept showing the
                    // pre-completion snapshot (every lesson still "current"/
                    // "locked", hence 0%). Clearing it forces the next screen
                    // that reads AppCache to hit the network and get the fresh
                    // "completed" statuses instead of a stale cached copy.
                    val optimisticPath = (_uiState.value.learningPath ?: AppCache.learningPath)
                        ?.withCompletedSublesson(sublessonId)
                    if (optimisticPath != null) {
                        AppCache.learningPath = optimisticPath
                        AppCache.learningPathAt = System.currentTimeMillis()
                    } else {
                        AppCache.invalidateLearningPath()
                    }

                    // Also refresh our own local copy so the "next lesson" lookup
                    // in LessonScreen (which reads uiState.learningPath) reflects
                    // the just-completed lesson too, not just AppCache consumers.
                    val freshPath = suspendCoroutine<CurrentLearningPathResponse?> { cont ->
                        AssessmentApi.getCurrentLearningPath(userId) { cont.resume(it) }
                    }?.let { AppCache.applyLocalLearningPathProgress(it) }
                    if (freshPath != null) {
                        AppCache.learningPath = freshPath
                        AppCache.learningPathAt = System.currentTimeMillis()
                    }

                    _uiState.update {
                        it.copy(
                            totalXpEarned = response.xp_earned,
                            phase = Phase.COMPLETE,
                            learningPath = freshPath ?: optimisticPath ?: it.learningPath
                        )
                    }
                } else {
                    _uiState.update { it.copy(completionSent = false, error = "Failed to record completion. Please try again.") }
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "completeSublesson error", e)
                _uiState.update { it.copy(completionSent = false, error = e.message ?: "Failed to record completion.") }
            }
        }
    }

    // ── Reset ─────────────────────────────────────────────────────────────────
    fun reset() { _uiState.update { LessonUiState() } }

    // ── Helper kept for LessonScreen compatibility (reads state.activeExercise) ─
    fun activeExercise(state: LessonUiState): Exercise? = state.activeExercise

    // ── Private: pick next exercise given current pointers ────────────────────
    private fun nextExercise(
        originals  : List<Exercise>,
        retryQueue : ArrayDeque<Exercise>,
        source     : ExerciseSource,
        origIndex  : Int
    ): Exercise? = when (source) {
        ExerciseSource.ORIGINAL -> originals.getOrNull(origIndex)
        ExerciseSource.RETRY    -> retryQueue.firstOrNull()
    }
}
