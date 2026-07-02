package com.example.db

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GeminiRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.api.WellBeingAdvice
import com.squareup.moshi.JsonAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface AIState {
    object Idle : AIState
    object Loading : AIState
    data class Success(val advice: WellBeingAdvice) : AIState
    data class Error(val message: String) : AIState
}

class ZenViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ZenDatabase.getDatabase(application)
    private val repository = ZenRepository(database.moodDao())

    // All historic mood entries
    val entries: StateFlow<List<MoodEntry>> = repository.allEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current screen input states
    private val _selectedMood = MutableStateFlow("NEUTRAL") // RAD, HAPPY, NEUTRAL, SAD, AWFUL
    val selectedMood: StateFlow<String> = _selectedMood.asStateFlow()

    private val _journalText = MutableStateFlow("")
    val journalText: StateFlow<String> = _journalText.asStateFlow()

    // AI Status
    private val _aiState = MutableStateFlow<AIState>(AIState.Idle)
    val aiState: StateFlow<AIState> = _aiState.asStateFlow()

    fun selectMood(mood: String) {
        _selectedMood.value = mood
    }

    fun updateJournalText(text: String) {
        _journalText.value = text
    }

    fun resetInputs() {
        _journalText.value = ""
        _selectedMood.value = "NEUTRAL"
        _aiState.value = AIState.Idle
    }

    fun deleteEntry(id: Int) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun clearAllEntries() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun analyzeAndSaveEntry() {
        val moodValue = _selectedMood.value
        val journalValue = _journalText.value.trim()

        if (journalValue.isEmpty()) {
            _aiState.value = AIState.Error("Please write a few thoughts first.")
            return
        }

        viewModelScope.launch {
            _aiState.value = AIState.Loading
            
            try {
                // Call Gemini on Dispatchers.IO
                val advice = fetchAIInsights(moodValue, journalValue)
                
                // Save to Room database
                val newEntry = MoodEntry(
                    mood = moodValue,
                    journalText = journalValue,
                    timestamp = System.currentTimeMillis(),
                    aiReflection = advice.reflection,
                    aiRecommendation = advice.recommendation,
                    tag = advice.tag
                )
                
                withContext(Dispatchers.IO) {
                    repository.insert(newEntry)
                }
                
                _aiState.value = AIState.Success(advice)
            } catch (e: Exception) {
                Log.e("ZenViewModel", "Error fetching Gemini insights", e)
                
                // If API fails (e.g. no internet, bad key), generate local helpful response to ensure grace
                val fallbackAdvice = generateFallbackAdvice(moodValue, journalValue)
                val fallbackEntry = MoodEntry(
                    mood = moodValue,
                    journalText = journalValue,
                    timestamp = System.currentTimeMillis(),
                    aiReflection = fallbackAdvice.reflection,
                    aiRecommendation = fallbackAdvice.recommendation,
                    tag = fallbackAdvice.tag
                )
                
                withContext(Dispatchers.IO) {
                    repository.insert(fallbackEntry)
                }
                
                _aiState.value = AIState.Success(fallbackAdvice)
            }
        }
    }

    private suspend fun fetchAIInsights(mood: String, journalText: String): WellBeingAdvice = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("API key is not configured.")
        }

        val systemPrompt = """
            You are ZenMind, an exceptionally empathetic, insightful, and supportive mindfulness coach and psychological well-being companion. 
            Analyze the user's mood journal entry and provide gentle, comforting feedback.
            You MUST output a raw, valid JSON object with EXACTLY the following fields:
            1. "reflection": A warm, compassion-filled, and deeply personal feedback (2-3 sentences max) addressing their feelings. Do not give clichés; speak directly and kindly.
            2. "recommendation": A precise, concrete mindful action step based on their feelings. Keep it actionable, clear, and highly focused (1-2 sentences max).
            3. "tag": A single, beautiful emotional word classifying their overall vibe (e.g. Peaceful, Grateful, Overwhelmed, Exhausted, Stressed, Joyful, Sentimental, Lonely).

            Ensure your output is strictly a raw valid JSON object. Do not wrap in markdown blocks or write any introductory or trailing text.
        """.trimIndent()

        val userPrompt = "Mood: $mood\nJournal Entry: $journalText"

        val request = GeminiRequest(
            contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt))),
            generationConfig = GenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7
            )
        )

        val response = RetrofitClient.service.generateContent(apiKey, request)
        val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw IllegalStateException("Empty response from model.")

        // Clean any markdown formatting if returned (just in case)
        val cleanedJson = responseText.trim().replace("```json", "").replace("```", "").trim()

        val adapter: JsonAdapter<WellBeingAdvice> = RetrofitClient.moshiInstance.adapter(WellBeingAdvice::class.java)
        adapter.fromJson(cleanedJson) ?: throw IllegalStateException("Failed to parse JSON response.")
    }

    private fun generateFallbackAdvice(mood: String, journalText: String): WellBeingAdvice {
        val reflection = when (mood) {
            "RAD" -> "It is wonderful to hear you're feeling so great! Celebrating these bright moments helps solidify joy in our emotional memory."
            "HAPPY" -> "Your heart seems light and positive today. Keep holding onto this cheerful energy and notice how it warms your surroundings."
            "NEUTRAL" -> "A calm, centered state is a beautiful place of balance. Embracing neutral days allows us to rest and observe life clearly."
            "SAD" -> "I am so sorry you are holding heavy feelings today. Remember that sadness is like clouds passing; it is natural, and you are safe to feel it."
            "AWFUL" -> "It sounds like you're going through a deeply challenging moment right now. Please be extremely gentle with yourself; this too will pass."
            else -> "Thank you for sharing your thoughts with me. Self-reflection is the first courageous step toward mental clarity."
        }

        val recommendation = when (mood) {
            "RAD", "HAPPY" -> "Take a brief moment to write down one specific detail of today that you are grateful for, to lock in this joy."
            "NEUTRAL" -> "Do a quick 1-minute conscious check-in: feel your shoulders drop and take a slow, deep breath in."
            "SAD" -> "Wrap yourself in comfort. Place a warm hand over your heart, breathe slowly, and whisper: 'I am doing the best I can.'"
            "AWFUL" -> "Try the Zen Grounding space: look around and name 3 things you can see, 2 things you can touch, and 1 thing you can hear."
            else -> "Take three deep breaths, releasing tension with every exhale."
        }

        val tag = when (mood) {
            "RAD" -> "Radiant"
            "HAPPY" -> "Joyful"
            "NEUTRAL" -> "Balanced"
            "SAD" -> "reflective"
            "AWFUL" -> "Overwhelmed"
            else -> "Mindful"
        }

        return WellBeingAdvice(reflection, recommendation, tag)
    }
}
