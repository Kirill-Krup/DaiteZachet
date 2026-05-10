package com.example.daitezachet.records

import android.content.Context

object RecordsStore {
    private const val PREF = "level_records"

    fun addDeath(ctx: Context, levelNumber: Int) {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val key = "level_${levelNumber}_deaths"
        val old = p.getInt(key, 0)
        p.edit().putInt(key, old + 1).apply()
    }

    fun saveCompletion(ctx: Context, levelNumber: Int, timeMs: Long) {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val timeKey = "level_${levelNumber}_best_time"
        val doneKey = "level_${levelNumber}_completed"

        val oldBest = p.getLong(timeKey, Long.MAX_VALUE)
        val newBest = minOf(oldBest, timeMs)

        p.edit()
            .putLong(timeKey, newBest)
            .putBoolean(doneKey, true)
            .apply()
    }

    fun getLevelRecord(ctx: Context, levelNumber: Int): LevelRecord {
        val p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return LevelRecord(
            levelNumber = levelNumber,
            bestTimeMs = p.getLong("level_${levelNumber}_best_time", Long.MAX_VALUE),
            totalDeaths = p.getInt("level_${levelNumber}_deaths", 0),
            completed = p.getBoolean("level_${levelNumber}_completed", false)
        )
    }

    fun getTotalDeaths(ctx: Context, levelCount: Int): Int {
        var sum = 0
        for (i in 1..levelCount) {
            sum += getLevelRecord(ctx, i).totalDeaths
        }
        return sum
    }

    fun resetAll(ctx: Context) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}