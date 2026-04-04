package com.example.daitezachet.levels

object LevelRegistry {
    private val levels: List<Level> = listOf(
        Level01(), // 1
        Level02(), // 2
        Level03(), // 3
        Level01(), // 4
        Level02(), // 5
        Level03(), // 6
        Level01(), // 7
        Level02(), // 8
        Level03(), // 9
        Level01(), // 10
        Level02(), // 11
        Level03(), // 12
        Level01(), // 13
        Level02(), // 14
        Level03()  // 15
    )

    val count: Int get() = levels.size

    /** Returns the level for the given 1-based number. */
    fun get(number: Int): Level = levels[number - 1]
}
