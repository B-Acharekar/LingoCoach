package com.mk.lingocoach.ui.screens

import com.mk.lingocoach.network.CurrentLearningPathResponse
import com.mk.lingocoach.network.CurrentLesson
import com.mk.lingocoach.network.CurrentModule
import com.mk.lingocoach.network.CurrentSublesson

internal fun CurrentLearningPathResponse.withCompletedSublesson(completedSublessonId: String): CurrentLearningPathResponse {
    if (completedSublessonId.isBlank()) return this

    val flatSublessonIds = modules
        .flatMap { module -> module.lessons }
        .flatMap { lesson -> lesson.sublessons }
        .map { it.id }
    val completedIndex = flatSublessonIds.indexOf(completedSublessonId)
    if (completedIndex == -1) return normalizedLearningPath()

    var flatIndex = 0
    val updatedModules = modules.map { module ->
        val updatedLessons = module.lessons.map { lesson ->
            val updatedSublessons = lesson.sublessons.map { sublesson ->
                val newStatus = when {
                    flatIndex <= completedIndex -> "completed"
                    flatIndex == completedIndex + 1 -> "current"
                    else -> "locked"
                }
                flatIndex += 1
                sublesson.copy(status = newStatus)
            }
            lesson.copy(status = updatedSublessons.deriveLessonStatus())
        }
        module.copy(status = updatedLessons.deriveModuleStatus(), lessons = updatedLessons)
    }

    return copy(modules = updatedModules)
}

internal fun CurrentLearningPathResponse.normalizedLearningPath(): CurrentLearningPathResponse {
    var hasCurrent = false
    val updatedModules = modules.map { module ->
        val updatedLessons = module.lessons.map { lesson ->
            val normalizedSublessons = lesson.sublessons.map { sublesson ->
                val status = sublesson.status.lowercase()
                if (status == "current") hasCurrent = true
                sublesson.copy(status = status)
            }
            lesson.copy(status = normalizedSublessons.deriveLessonStatus(), sublessons = normalizedSublessons)
        }
        module.copy(status = updatedLessons.deriveModuleStatus(), lessons = updatedLessons)
    }

    val normalized = copy(modules = updatedModules)
    return if (hasCurrent || normalized.modules.isEmpty()) normalized else normalized.unlockFirstIncomplete()
}

internal fun CurrentModule.completedLessonCount(): Int =
    lessons.count { lesson ->
        lesson.status == "completed" || lesson.sublessons.isNotEmpty() && lesson.sublessons.all { it.status == "completed" }
    }

internal fun CurrentModule.progressPercent(): Int {
    val total = lessons.size
    return if (total > 0) (completedLessonCount() * 100 / total) else 0
}

internal fun CurrentModule.currentSublesson(): CurrentSublesson? =
    lessons.asSequence()
        .flatMap { it.sublessons.asSequence() }
        .firstOrNull { it.status == "current" }
        ?: lessons.asSequence()
            .flatMap { it.sublessons.asSequence() }
            .firstOrNull { it.status != "completed" && it.status != "locked" }

private fun List<CurrentSublesson>.deriveLessonStatus(): String = when {
    isEmpty() -> "locked"
    all { it.status == "completed" } -> "completed"
    any { it.status == "current" } -> "current"
    else -> "locked"
}

private fun List<CurrentLesson>.deriveModuleStatus(): String = when {
    isEmpty() -> "locked"
    all { it.status == "completed" } -> "completed"
    any { it.status == "current" } -> "current"
    else -> "locked"
}

private fun CurrentLearningPathResponse.unlockFirstIncomplete(): CurrentLearningPathResponse {
    var unlocked = false
    val updatedModules = modules.map { module ->
        val updatedLessons = module.lessons.map { lesson ->
            val updatedSublessons = lesson.sublessons.map { sublesson ->
                if (!unlocked && sublesson.status != "completed") {
                    unlocked = true
                    sublesson.copy(status = "current")
                } else {
                    sublesson
                }
            }
            lesson.copy(status = updatedSublessons.deriveLessonStatus(), sublessons = updatedSublessons)
        }
        module.copy(status = updatedLessons.deriveModuleStatus(), lessons = updatedLessons)
    }
    return copy(modules = updatedModules)
}
