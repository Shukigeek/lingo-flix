package com.example.lingoFlix.data

import android.content.Context
import android.content.SharedPreferences
import com.example.lingoFlix.utils.LingoLog
import java.util.*

/**
 * Manages user experience points (XP) and activity streaks.
 */
class UserStatsManager(context: Context) {
    private val className = "UserStatsManager"
    private val prefs: SharedPreferences = context.getSharedPreferences("user_stats", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_XP = "total_xp"
        private const val KEY_STREAK = "current_streak"
        private const val KEY_LAST_ACTIVITY = "last_activity_date"
    }

    fun getXP(): Int {
        return try {
            prefs.getInt(KEY_XP, 0)
        } catch (e: Exception) {
            LingoLog.e(className, "Error getting XP", e)
            0
        }
    }

    fun addXP(points: Int) {
        try {
            val currentXP = getXP()
            prefs.edit().putInt(KEY_XP, currentXP + points).apply()
            LingoLog.i(className, "Added $points XP. New total: ${currentXP + points}")
        } catch (e: Exception) {
            LingoLog.e(className, "Error adding XP", e)
        }
    }

    fun getStreak(): Int {
        return try {
            prefs.getInt(KEY_STREAK, 0)
        } catch (e: Exception) {
            LingoLog.e(className, "Error getting streak", e)
            0
        }
    }

    fun markActivityToday() {
        try {
            val today = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val lastActivity = prefs.getLong(KEY_LAST_ACTIVITY, 0L)
            val diffDays = (today - lastActivity) / (1000 * 60 * 60 * 24)

            if (diffDays == 1L) {
                val newStreak = getStreak() + 1
                prefs.edit().putInt(KEY_STREAK, newStreak).apply()
                LingoLog.i(className, "Streak increased to $newStreak")
            } else if (diffDays > 1L) {
                prefs.edit().putInt(KEY_STREAK, 1).apply()
                LingoLog.i(className, "Streak reset to 1")
            }
            prefs.edit().putLong(KEY_LAST_ACTIVITY, today).apply()
        } catch (e: Exception) {
            LingoLog.e(className, "Error marking activity", e)
        }
    }
}
