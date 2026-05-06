package com.example.daitezachet.levels

object LevelRegistry {
    private val levels: List<Level> = listOf(
        Level01(), // 1 Андриана легкий 1
        Level02(), // 3 Ани легкий 2
        Level03(), //легкий Антона 3
        Level04(), // Стаса ниже среднего 4
        Level05(), // 4 Викин ниже среднего 5
        Level06(), // Денис Б
        Level08(), // 5 средне Полины 8
        Level09(), // Денис А. 
        Level10(), // Насти сложновато 10
        Level12(), // 6 Даши сложновато 12
        Level13(), // 13 Кирилла
        Level14(), //Дениса К 14
        Level15(), // сложный Миши 15
    )


    val count: Int get() = levels.size

    /** Returns the level for the given 1-based number. */
    fun get(number: Int): Level = levels[number - 1]
}
