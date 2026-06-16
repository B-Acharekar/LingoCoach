package com.mk.lingocoach.ui.screens

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

// ─── Data Models ─────────────────────────────────────────────────────────────

data class VocabWord(
    val word: String,
    val level: String, // "A1", "A2", "B1", "B2", "C1", "C2"
    val category: String, // Original category from file
    val mappedCategory: String, // Grouped category for UI
    val partOfSpeech: String,
    val pronunciation: String,
    val meaning: String,
    val synonyms: List<String>,
    val antonyms: List<String>,
    val examples: List<VocabExample>
)

data class VocabExample(
    val english: String,
    val translations: Map<String, String>
)

data class WordProgress(
    val word: String,
    var masteryScore: Int = 0, // 0 to 100
    var isBookmarked: Boolean = false,
    var lastReviewed: Long = 0L
)

data class CategoryStat(
    val name: String,
    val description: String,
    val totalWords: Int,
    val masteredWordsCount: Int,
    val averageMastery: Int, // 0 to 100
    val starRating: Int // 1 to 3 stars based on averageMastery
)

data class DrillQuestion(
    val word: VocabWord,
    val questionText: String, // Fill-in-the-blank sentence
    val options: List<String>,
    val correctIndex: Int
)

// ─── Json Parsing Schemas ───────────────────────────────────────────────────

private data class VocabFileContent(
    val level: String?,
    val approx_words: String?,
    val source: String?,
    val categories: Map<String, List<VocabWordJson>>?
)

private data class VocabWordJson(
    val word: String,
    val level: String?,
    val category: String?,
    val part_of_speech: String?,
    val pronunciation: String?,
    val meaning: String?,
    val synonyms: List<String>?,
    val antonyms: List<String>?,
    val examples: List<VocabExampleJson>?
)

private data class VocabExampleJson(
    val english: String,
    val translations: Map<String, String>?
)

// ─── Local Mistake Entry ─────────────────────────────────────────────────────

data class LocalMistakeEntry(
    val word: String,
    val mistakeType: String,         // "VOCAB_DRILL" | "TIMELY_DUEL" | "SPELLING"
    val userAnswer: String,
    val correctAnswer: String,
    val explanation: String,
    val timesMissed: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
)

// ─── Local Tracker Implementation ───────────────────────────────────────────

object VocabTracker {
    private const val TAG = "VocabTracker"
    private const val PROGRESS_FILE = "vocab_progress.json"
    private const val MISTAKES_FILE = "vocab_mistakes.json"
    private val gson = Gson()

    // Loaded Vocabulary
    private val allWords = mutableListOf<VocabWord>()
    
    // User Progress
    private val progressMap = mutableMapOf<String, WordProgress>()
    
    var isLoaded = false
        private set

    // Normalized Level Getter
    private fun normalizeLevel(levelStr: String?, folderName: String): String {
        val lvl = levelStr?.uppercase() ?: folderName.uppercase()
        return when {
            lvl.contains("A1") -> "A1"
            lvl.contains("A2") -> "A2"
            lvl.contains("B1") -> "B1"
            lvl.contains("B2") -> "B2"
            lvl.contains("C1") && lvl.contains("C2") -> "C1" // Default folder level
            lvl.contains("C1") -> "C1"
            lvl.contains("C2") -> "C2"
            else -> "A1"
        }
    }

    // Category Mapper for B1+ Levels
    fun getMappedCategory(originalCategory: String, level: String): String {
        val lvl = level.uppercase()
        if (lvl == "A1" || lvl == "A2") {
            return originalCategory
        }
        val catLower = originalCategory.lowercase()
        return when {
            catLower.contains("academic") || catLower.contains("rhetoric") || catLower.contains("philosophy") || catLower.contains("science") || catLower.contains("scientific") -> 
                "Academic & Analytical Language"
                
            catLower.contains("emotion") || catLower.contains("psychology") || catLower.contains("personality") -> 
                "Nuanced Emotions & Psychology"
                
            catLower.contains("business") || catLower.contains("politic") || catLower.contains("law") || catLower.contains("finance") || catLower.contains("economics") || catLower.contains("workplace") || catLower.contains("jobs") -> 
                "Business Strategy, Politics & Law"
                
            catLower.contains("literature") || catLower.contains("figurative") || catLower.contains("idiom") || catLower.contains("movie") || catLower.contains("music") || catLower.contains("art") || catLower.contains("culture") -> 
                "Advanced Descriptive Literature"
                
            else -> "Academic & Analytical Language"
        }
    }

    // Category Description Mapper
    fun getCategoryDescription(mappedCategory: String): String {
        return when (mappedCategory) {
            "Academic & Analytical Language" -> "Synthesize complex arguments and interpret rhetorical structures in formal contexts."
            "Nuanced Emotions & Psychology" -> "Explore the subtle shades of human feelings, personality traits, and psychological states."
            "Business Strategy, Politics & Law" -> "Master high-level terminology used in commerce, global governance, legal systems, and economics."
            "Advanced Descriptive Literature" -> "Enhance your creative expression with vivid descriptors, idioms, and figurative language."
            else -> "Curated vocabulary topics to expand your expression."
        }
    }

    // Main Init Function
    suspend fun init(context: Context) = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        try {
            allWords.clear()
            loadProgress(context)

            val assetManager = context.assets
            val rootFolder = "vocab-builder"
            val levelFolders = listOf("a1", "a2", "b1", "b2", "c1_c2")

            for (folder in levelFolders) {
                val folderPath = "$rootFolder/$folder"
                val files = assetManager.list(folderPath) ?: continue
                for (file in files) {
                    if (!file.endsWith(".json")) continue
                    val filePath = "$folderPath/$file"
                    try {
                        val jsonContent = assetManager.open(filePath).bufferedReader().use { it.readText() }
                        val parsedFile = gson.fromJson(jsonContent, VocabFileContent::class.java)
                        
                        parsedFile.categories?.forEach { (categoryName, wordList) ->
                            for (w in wordList) {
                                val resolvedLevel = normalizeLevel(w.level, folder)
                                val resolvedCategory = w.category ?: categoryName
                                val mappedCat = getMappedCategory(resolvedCategory, resolvedLevel)
                                
                                val wordObj = VocabWord(
                                    word = w.word,
                                    level = resolvedLevel,
                                    category = resolvedCategory,
                                    mappedCategory = mappedCat,
                                    partOfSpeech = w.part_of_speech ?: "noun",
                                    pronunciation = w.pronunciation ?: "",
                                    meaning = w.meaning ?: "",
                                    synonyms = w.synonyms ?: emptyList(),
                                    antonyms = w.antonyms ?: emptyList(),
                                    examples = w.examples?.map { ex ->
                                        VocabExample(
                                            english = ex.english,
                                            translations = ex.translations ?: emptyMap()
                                        )
                                    } ?: emptyList()
                                )
                                allWords.add(wordObj)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing file $filePath", e)
                    }
                }
            }
            isLoaded = true
            Log.d(TAG, "Successfully loaded ${allWords.size} words.")
        } catch (e: Exception) {
            Log.e(TAG, "Initialization failed", e)
        }
    }

    // Load user progress from local storage
    private fun loadProgress(context: Context) {
        val file = File(context.filesDir, PROGRESS_FILE)
        if (!file.exists()) return
        try {
            val json = file.readText()
            val type = object : TypeToken<Map<String, WordProgress>>() {}.type
            val savedMap = gson.fromJson<Map<String, WordProgress>>(json, type)
            if (savedMap != null) {
                progressMap.clear()
                progressMap.putAll(savedMap)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load progress", e)
        }
    }

    // Save user progress to local storage
    private fun saveProgress(context: Context) {
        try {
            val file = File(context.filesDir, PROGRESS_FILE)
            val json = gson.toJson(progressMap)
            file.writeText(json)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save progress", e)
        }
    }

    // Get level-wide progress percent (0 to 100)
    fun getLevelProgress(level: String): Int {
        val wordsInLevel = allWords.filter { it.level.uppercase() == level.uppercase() }
        if (wordsInLevel.isEmpty()) return 0
        var totalMastery = 0
        for (w in wordsInLevel) {
            totalMastery += progressMap[w.word]?.masteryScore ?: 0
        }
        return totalMastery / wordsInLevel.size
    }

    // Get overall progress percent (0 to 100)
    fun getOverallProgressPercent(): Int {
        if (allWords.isEmpty()) return 0
        var totalMastery = 0
        for (w in allWords) {
            totalMastery += progressMap[w.word]?.masteryScore ?: 0
        }
        return totalMastery / allWords.size
    }

    // Get overall words mastered vs total words count text (e.g. "12 / 1230 words")
    fun getOverallWordsCountText(): String {
        val masteredCount = allWords.count { (progressMap[it.word]?.masteryScore ?: 0) >= 80 }
        return "$masteredCount / ${allWords.size} words"
    }

    // Get total words count in a level
    fun getWordsCountInLevel(level: String): Int {
        return allWords.count { it.level.uppercase() == level.uppercase() }
    }


    // Get categories and stats for a given level
    fun getCategoryStats(level: String): List<CategoryStat> {
        val wordsInLevel = allWords.filter { it.level.uppercase() == level.uppercase() }
        val grouped = wordsInLevel.groupBy { it.mappedCategory }
        
        return grouped.map { (catName, wordList) ->
            var masteredCount = 0
            var totalMastery = 0
            for (w in wordList) {
                val mastery = progressMap[w.word]?.masteryScore ?: 0
                if (mastery >= 80) masteredCount++
                totalMastery += mastery
            }
            val avgMastery = if (wordList.isEmpty()) 0 else totalMastery / wordList.size
            val stars = when {
                avgMastery >= 75 -> 3
                avgMastery >= 35 -> 2
                else -> 1
            }
            CategoryStat(
                name = catName,
                description = getCategoryDescription(catName),
                totalWords = wordList.size,
                masteredWordsCount = masteredCount,
                averageMastery = avgMastery,
                starRating = stars
            )
        }
    }

    // Search words in selected level
    fun searchWords(query: String, level: String): List<VocabWord> {
        val wordsInLevel = allWords.filter { it.level.uppercase() == level.uppercase() }
        if (query.isBlank()) return wordsInLevel
        val q = query.lowercase()
        return wordsInLevel.filter { 
            it.word.lowercase().contains(q) || 
            it.meaning.lowercase().contains(q) || 
            it.mappedCategory.lowercase().contains(q)
        }
    }

    // Get bookmark status for a word
    fun isBookmarked(word: String): Boolean {
        return progressMap[word]?.isBookmarked ?: false
    }

    // Toggle bookmark
    fun toggleBookmark(word: String, context: Context): Boolean {
        val prog = progressMap.getOrPut(word) { WordProgress(word = word) }
        prog.isBookmarked = !prog.isBookmarked
        saveProgress(context)
        return prog.isBookmarked
    }

    // Get word's mastery score
    fun getMasteryScore(word: String): Int {
        return progressMap[word]?.masteryScore ?: 0
    }

    // Update mastery score
    fun updateWordMastery(word: String, isCorrect: Boolean, context: Context): Int {
        val prog = progressMap.getOrPut(word) { WordProgress(word = word) }
        val currentScore = prog.masteryScore
        if (isCorrect) {
            prog.masteryScore = (currentScore + 20).coerceAtMost(100)
        } else {
            prog.masteryScore = (currentScore - 10).coerceAtLeast(0)
        }
        prog.lastReviewed = System.currentTimeMillis()
        saveProgress(context)
        return prog.masteryScore
    }

    // Generate a set of multiple-choice questions for a session
    // Words with masteryScore >= 80 are excluded — they're already mastered
    fun generateDrillSession(level: String, category: String?, count: Int = 5): List<DrillQuestion> {
        var candidates = allWords.filter { it.level.uppercase() == level.uppercase() }
        if (category != null) {
            candidates = candidates.filter { it.mappedCategory.uppercase() == category.uppercase() }
        }
        // Exclude mastered words (score >= 80) — user cannot re-learn same word
        candidates = candidates.filter { (progressMap[it.word]?.masteryScore ?: 0) < 80 }

        if (candidates.isEmpty()) return emptyList()

        // Prioritize words that are not fully mastered or have been reviewed less
        val sortedCandidates = candidates.shuffled().sortedBy { progressMap[it.word]?.masteryScore ?: 0 }
        val selectedWords = sortedCandidates.take(count)

        val questions = mutableListOf<DrillQuestion>()
        for (vocabWord in selectedWords) {
            // Find an example sentence, or fall back to meaning
            val englishSentence = vocabWord.examples.firstOrNull()?.english ?: vocabWord.meaning
            
            // Generate fill-in-the-blank sentence
            // Case-insensitive regex match for the word, replacing it with "_____"
            val regex = "\\b${Regex.escape(vocabWord.word)}\\b".toRegex(RegexOption.IGNORE_CASE)
            var questionText = englishSentence.replace(regex, "_____")
            if (questionText == englishSentence) {
                // Try simple singular/plural variations or substring replacement if exact word boundaries fail
                questionText = englishSentence.replace(vocabWord.word, "_____", ignoreCase = true)
            }
            if (!questionText.contains("_____")) {
                questionText = "The word is defined as: \"${vocabWord.meaning}\". What is the word?"
            }

            // Distractors: random words from the same level
            val otherLevelWords = allWords.filter { it.level.uppercase() == level.uppercase() && it.word != vocabWord.word }
            val distractors = otherLevelWords.shuffled().take(3).map { it.word }
            
            // Build and shuffle options
            val options = (distractors + vocabWord.word).shuffled()
            val correctIdx = options.indexOf(vocabWord.word)

            questions.add(
                DrillQuestion(
                    word = vocabWord,
                    questionText = questionText,
                    options = options,
                    correctIndex = correctIdx
                )
            )
        }
        return questions
    }

    // ─── Local Mistake Vault Integration ─────────────────────────────────────

    fun addLocalMistake(word: String, mistakeType: String, userAnswer: String, correctAnswer: String, explanation: String, context: Context) {
        val existing = loadLocalMistakes(context).toMutableList()
        val idx = existing.indexOfFirst { it.word.lowercase() == word.lowercase() && it.mistakeType == mistakeType }
        if (idx >= 0) {
            val old = existing[idx]
            existing[idx] = old.copy(timesMissed = old.timesMissed + 1, createdAt = System.currentTimeMillis())
        } else {
            existing.add(
                LocalMistakeEntry(
                    word = word,
                    mistakeType = mistakeType,
                    userAnswer = userAnswer,
                    correctAnswer = correctAnswer,
                    explanation = explanation
                )
            )
        }
        saveLocalMistakes(existing, context)
    }

    fun getLocalMistakes(context: Context): List<LocalMistakeEntry> = loadLocalMistakes(context)

    private fun loadLocalMistakes(context: Context): List<LocalMistakeEntry> {
        val file = File(context.filesDir, MISTAKES_FILE)
        if (!file.exists()) return emptyList()
        return try {
            val type = object : TypeToken<List<LocalMistakeEntry>>() {}.type
            gson.fromJson(file.readText(), type) ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load local mistakes", e)
            emptyList()
        }
    }

    private fun saveLocalMistakes(list: List<LocalMistakeEntry>, context: Context) {
        try {
            File(context.filesDir, MISTAKES_FILE).writeText(gson.toJson(list))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save local mistakes", e)
        }
    }

    // ─── Backend Sync ─────────────────────────────────────────────────────────

    /**
     * Syncs local vocab progress to the backend (last-write-wins).
     * 1. POST /api/v1/vocab/sync  — push local changes
     * 2. GET  /api/v1/vocab/sync  — pull server updates, merge into progressMap
     * Saves last sync timestamp to SharedPreferences.
     */
    suspend fun syncToBackend(userId: String, context: Context) = withContext(Dispatchers.IO) {
        if (!isLoaded) return@withContext

        val prefs = context.getSharedPreferences("LingoCoachPrefs", Context.MODE_PRIVATE)
        val lastSync = prefs.getString("vocab_last_sync", null)
        val now = java.time.Instant.now().toString()

        // Build changes list from in-memory progressMap
        val changes = progressMap.values.map { p ->
            mapOf(
                "word"          to p.word,
                "mastery_score" to p.masteryScore,
                "is_bookmarked" to p.isBookmarked,
                "last_reviewed" to p.lastReviewed.toString(),
                "updated_at"    to now
            )
        }

        // Push to backend
        com.mk.lingocoach.network.AssessmentApi.syncVocab(userId, now, changes) { result ->
            Log.d(TAG, "Vocab sync push: merged=${result?.get("merged_count")}")
        }

        // Pull server updates
        com.mk.lingocoach.network.AssessmentApi.getVocabServerUpdates(userId, lastSync) { updates ->
            if (updates != null) {
                for (item in updates) {
                    val word  = item["word"] as? String ?: continue
                    val score = (item["mastery_score"] as? Double)?.toInt()
                               ?: (item["mastery_score"] as? Int) ?: continue
                    val bookmarked = item["is_bookmarked"] as? Boolean ?: false
                    val existing = progressMap.getOrPut(word) { WordProgress(word = word) }
                    existing.masteryScore  = score
                    existing.isBookmarked  = bookmarked
                }
                saveProgress(context)
                prefs.edit().putString("vocab_last_sync", now).apply()
            }
        }
    }
}
