package com.example.studywise.domain

interface StudyAssistant {
    suspend fun getSubjectAdvice(subjectName: String, prepPercentage: Int, daysUntilExam: Long): String
    suspend fun generateRevisionTip(topicName: String): String
    suspend fun analyzeStudySchedule(plannedMinutes: Int, targetMinutes: Int): String
}

class RuleBasedStudyAssistant : StudyAssistant {

    override suspend fun getSubjectAdvice(
        subjectName: String,
        prepPercentage: Int,
        daysUntilExam: Long
    ): String {
        return when {
            daysUntilExam <= 2 && prepPercentage < 50 ->
                "Focus strictly on high-weightage topics and past exam papers for $subjectName. Avoid starting entirely new conceptual chapters; consolidate what you know."
            daysUntilExam <= 7 ->
                "Use the Feynman technique and flashcards for $subjectName. Review previous quiz mistakes and complete at least one timed mock section."
            prepPercentage < 40 ->
                "Break down $subjectName into smaller 20-minute study blocks. Complete introductory module summaries before tackling complex problem-solving."
            else ->
                "You are maintaining strong progress in $subjectName. Conduct spaced repetitions every 3–4 days to sustain retention without burnout."
        }
    }

    override suspend fun generateRevisionTip(topicName: String): String {
        return "For \"$topicName\", try explaining the core mechanism without looking at your notes. Active recall produces up to 50% better long-term retention than passive re-reading."
    }

    override suspend fun analyzeStudySchedule(plannedMinutes: Int, targetMinutes: Int): String {
        return if (plannedMinutes >= targetMinutes) {
            "Your schedule meets your daily target of ${targetMinutes}m. Remember to take a 5-minute breather between intense focus blocks."
        } else {
            "You have scheduled ${plannedMinutes}m out of your ${targetMinutes}m goal. Consider adding a quick revision session in the evening."
        }
    }
}
