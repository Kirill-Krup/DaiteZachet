package com.example.daitezachet.records

data class LevelRecord(
    val levelNumber: Int,
    val bestTimeMs: Long = Long.MAX_VALUE,
    val totalDeaths: Int = 0,
    val completed: Boolean = false
)