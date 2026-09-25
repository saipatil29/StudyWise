package com.example.studywise.domain

import com.example.studywise.data.entity.StudySession
import com.example.studywise.data.entity.Subject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class PlannedStudySlot(
    val subjectId: Long,
    val subjectName: String,
    val startTime: String,
    val durationMinutes: Int,
    val priorityScore: Int,
    val priorityLevel: PriorityLevel,
    val isRescheduled: Boolean = false,
    val reschedulingNote: String = ""
)

object AdaptiveScheduler {

    /**
     * Generates an adaptive daily study schedule allocating time to subjects
     * based on their priority scores and user daily availability.
     */
    fun generateDailySchedule(
        subjects: List<Subject>,
        subjectPriorities: Map<Long, Int>,
        availableMinutes: Int = 120,
        startHour: Int = 18, // 6:00 PM default start
        startMinute: Int = 0,
        yesterdaySessions: List<StudySession> = emptyList()
    ): List<PlannedStudySlot> {
        if (subjects.isEmpty() || availableMinutes <= 0) return emptyList()

        // 1. Check for missed study time from yesterday
        val yesterdayMinutesBySubject = yesterdaySessions.groupBy { it.subjectId }
            .mapValues { entry -> entry.value.sumOf { it.durationMinutes } }

        // Determine modified priorities accounting for missed study sessions
        val adjustedPriorities = subjects.associate { subject ->
            val basePriority = subjectPriorities[subject.id] ?: 50
            val yesterdayStudied = yesterdayMinutesBySubject[subject.id] ?: 0
            val expectedYesterday = subject.dailyGoalMinutes

            val missedMinutes = (expectedYesterday - yesterdayStudied).coerceAtLeast(0)
            val penaltyBonus = if (missedMinutes >= 20) 15 else if (missedMinutes > 0) 8 else 0
            val isRescheduled = penaltyBonus > 0

            subject.id to Triple(
                (basePriority + penaltyBonus).coerceIn(0, 100),
                isRescheduled,
                if (isRescheduled) "Adjusted: missed ${missedMinutes}m yesterday" else ""
            )
        }

        // Sort subjects by adjusted priority descending
        val sortedSubjects = subjects.sortedByDescending { adjustedPriorities[it.id]?.first ?: 0 }

        // Select top 3 or 4 subjects to study today so sessions are meaningful (>= 25 min)
        val candidateSubjects = sortedSubjects.take(3)
        val totalPrioritySum = candidateSubjects.sumOf { adjustedPriorities[it.id]?.first ?: 1 }

        if (totalPrioritySum == 0) return emptyList()

        val slots = mutableListOf<PlannedStudySlot>()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, startHour)
            set(Calendar.MINUTE, startMinute)
            set(Calendar.SECOND, 0)
        }
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

        var remainingMinutes = availableMinutes

        for (i in candidateSubjects.indices) {
            val subject = candidateSubjects[i]
            val (priorityScore, isRescheduled, note) = adjustedPriorities[subject.id]
                ?: Triple(50, false, "")

            // Distribute proportionally
            val rawDuration = if (i == candidateSubjects.lastIndex) {
                remainingMinutes
            } else {
                ((priorityScore.toDouble() / totalPrioritySum) * availableMinutes).roundToInt()
            }

            // Round to nearest 5 minutes and clamp between 20 and 60 minutes
            val duration = (rawDuration / 5 * 5).coerceIn(20, remainingMinutes.coerceAtMost(60))

            if (duration > 0) {
                val startTimeStr = timeFormat.format(cal.time)
                slots.add(
                    PlannedStudySlot(
                        subjectId = subject.id,
                        subjectName = subject.name,
                        startTime = startTimeStr,
                        durationMinutes = duration,
                        priorityScore = priorityScore,
                        priorityLevel = PriorityCalculator.getPriorityLevel(priorityScore),
                        isRescheduled = isRescheduled,
                        reschedulingNote = note
                    )
                )

                // Advance time for next slot + 10 min break
                cal.add(Calendar.MINUTE, duration + 10)
                remainingMinutes -= duration
                if (remainingMinutes < 20) break
            }
        }

        return slots
    }
}
