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
        val moduleStatus = if (updatedLessons.isEmpty()) module.status.lowercase() else updatedLessons.deriveModuleStatus()
        module.copy(status = moduleStatus, lessons = updatedLessons)
    }

    return copy(modules = updatedModules).normalizedLearningPath()
}

internal fun CurrentLearningPathResponse.normalizedLearningPath(): CurrentLearningPathResponse {
    var hasCurrent = false
    val sourceModules = modules.ensureCourseLadderModules()
    val updatedModules = sourceModules.map { module ->
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

    val normalized = copy(modules = updatedModules.recomputeMissingModuleStatuses())
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

private data class CourseModuleTemplate(
    val level: String,
    val title: String,
    val description: String,
    val aliases: List<String>
)

private val courseModuleTemplates = listOf(
    CourseModuleTemplate(
        level = "Level 1",
        title = "Foundations",
        description = "Core grammar, sentence structure, and essential vocabulary.",
        aliases = listOf("foundation", "foundations", "level 1", "a1", "a2")
    ),
    CourseModuleTemplate(
        level = "Level 2",
        title = "Daily Life Conversation",
        description = "B1-B2 conversations for everyday situations, opinions, and smoother flow.",
        aliases = listOf("daily life", "conversation", "coffee chat", "level 2", "b1", "b2")
    ),
    CourseModuleTemplate(
        level = "Level 3",
        title = "Professional and Formal",
        description = "C1 workplace, formal, persuasive, and structured communication.",
        aliases = listOf("professional", "formal", "pitch", "level 3", "c1")
    ),
    CourseModuleTemplate(
        level = "Level 4",
        title = "Master",
        description = "C2-level mastery, nuance, idioms, cultural fluency, and advanced expression.",
        aliases = listOf("master", "mastery", "cultural", "level 4", "c2")
    )
)

private fun List<CurrentModule>.ensureCourseLadderModules(): List<CurrentModule> {
    if (isEmpty()) return this

    val claimed = mutableSetOf<Int>()
    val arranged = mutableListOf<CurrentModule>()

    courseModuleTemplates.forEachIndexed { templateIndex, template ->
        val match = this
            .filterIndexed { index, _ -> index !in claimed }
            .firstOrNull { module -> module.matchesTemplate(template) }

        if (match != null) {
            claimed += this.indexOf(match)
            arranged += match.copy(
                level = template.level,
                title = template.title,
                description = match.description.ifBlank { template.description },
                order = templateIndex + 1
            )
        } else {
            arranged += placeholderModule(template, templateIndex + 1)
        }
    }

    return arranged.recomputeMissingModuleStatuses()
}

private fun CurrentModule.matchesTemplate(template: CourseModuleTemplate): Boolean {
    val haystack = listOf(level, title, description).joinToString(" ").lowercase()
    return template.aliases.any { alias -> haystack.contains(alias.lowercase()) }
}

private fun placeholderModule(template: CourseModuleTemplate, order: Int): CurrentModule =
    CurrentModule(
        id = "placeholder_level_$order",
        title = template.title,
        description = template.description,
        level = template.level,
        order = order,
        status = "locked",
        xpReward = 100 + (order - 1) * 50,
        lessons = emptyList()
    )

private fun List<CurrentModule>.recomputeMissingModuleStatuses(): List<CurrentModule> {
    val firstCurrentIndex = indexOfFirst { it.status == "current" }
    if (firstCurrentIndex >= 0) return this

    val firstIncompleteIndex = indexOfFirst { it.status != "completed" }
    if (firstIncompleteIndex == -1) return this

    return mapIndexed { index, module ->
        if (index == firstIncompleteIndex && module.id.startsWith("placeholder_level_")) {
            module.copy(status = "current")
        } else {
            module
        }
    }
}
