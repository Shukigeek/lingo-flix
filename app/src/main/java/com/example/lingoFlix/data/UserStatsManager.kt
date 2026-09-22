package com.example.lingoFlix.data

import android.content.Context
import android.util.Log
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UserStatsManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_stats", Context.MODE_PRIVATE)

    fun getXP(): Int = prefs.getInt("total_xp", 0)

    fun getLevel(): Int {
        val xp = getXP()
        return (xp / 1000) + 1
    }

    fun getXPInCurrentLevel(): Int {
        return getXP() % 1000
    }

    fun addXP(points: Int) {
        val currentXP = getXP()
        val newXP = currentXP + points
        Log.d("UserStatsManager", "Adding XP: $points, New Total: $newXP")
        prefs.edit().putInt("total_xp", newXP).apply()
    }

    fun getStreak(): Int {
        updateStreak()
        val streak = prefs.getInt("streak_count", 0)
        Log.d("UserStatsManager", "Current Streak: $streak")
        return streak
    }

    fun markActivityToday() {
        val today = getTodayStartMillis()
        val lastActivity = prefs.getLong("last_activity_date", 0L)
        
        if (lastActivity < today) {
            val streak = prefs.getInt("streak_count", 0)
            val yesterday = today - TimeUnit.DAYS.toMillis(1)
            
            if (lastActivity >= yesterday) {
                prefs.edit()
                    .putInt("streak_count", streak + 1)
                    .putLong("last_activity_date", today)
                    .apply()
            } else {
                prefs.edit()
                    .putInt("streak_count", 1)
                    .putLong("last_activity_date", today)
                    .apply()
            }
        }
    }

    private fun updateStreak() {
        val today = getTodayStartMillis()
        val lastActivity = prefs.getLong("last_activity_date", 0L)
        val yesterday = today - TimeUnit.DAYS.toMillis(1)

        if (lastActivity < yesterday && lastActivity != 0L) {
            prefs.edit().putInt("streak_count", 0).apply()
        }
    }

    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
