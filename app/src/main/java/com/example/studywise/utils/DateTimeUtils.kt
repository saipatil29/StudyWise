package com.example.studywise.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateTimeUtils {

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayOfWeekFormatter = SimpleDateFormat("EEE", Locale.getDefault())

    fun formatDate(millis: Long): String = dateFormatter.format(Date(millis))

    fun formatTime(millis: Long): String = timeFormatter.format(Date(millis))

    fun formatDayOfWeek(millis: Long): String = dayOfWeekFormatter.format(Date(millis))

    fun formatDuration(minutes: Int): String {
        if (minutes <= 0) return "0m"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return when {
            hours > 0 && remainingMinutes > 0 -> "${hours}h ${remainingMinutes}m"
            hours > 0 -> "${hours}h"
            else -> "${remainingMinutes}m"
        }
    }

    fun getExamCountdownText(examDateMillis: Long, currentDateMillis: Long = System.currentTimeMillis()): String {
        val diffMillis = examDateMillis - currentDateMillis
        val days = TimeUnit.MILLISECONDS.toDays(diffMillis)

        return when {
            diffMillis < 0 && days < 0 -> "Exam passed"
            days == 0L -> "Exam Today 🚨"
            days == 1L -> "Exam Tomorrow ⚠️"
            else -> "Exam in $days days"
        }
    }

    fun getDaysUntil(examDateMillis: Long, currentDateMillis: Long = System.currentTimeMillis()): Long {
        val diffMillis = examDateMillis - currentDateMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis)
    }

    fun getStartOfToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfToday(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }

    fun getStartOfYesterday(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun getEndOfYesterday(): Long {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
