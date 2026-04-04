# Создание уровней в DaiteZachet

## Структура уровня

Каждый уровень — Kotlin-класс, наследующий `Level`:

```kotlin
class Level01 : Level() {
    override val number   = 1          // номер уровня
    override val hintText = "Подсказка" // текст в центре HUD

    override fun setup(engine: GameEngine) {
        // вся конфигурация уровня здесь
    }
}
```

`setup()` вызывается **при старте уровня и после каждой смерти** (после `engine.reset()`).
Размещайте объекты и задавайте хуки только внутри `setup()`.

---

## Координаты

Все вспомогательные функции принимают координаты в **долях размера комнаты** (0.0 — 1.0):

```
0.0 ────────────────────────── 1.0   (X: слева направо)
0.0
 │   игровая область
 │   (78% высоты экрана)
1.0                                   (Y: сверху вниз)
```

- `x = 0.0` — левая стена, `x = 1.0` — правая стена
- `y = 0.0` — потолок, `y = 1.0` — пол
- Фиксированные объекты: дверь (правая стена, y = 30%–97%), кнопка (x ≈ 42%, на полу)

---

## Платформы

### Горизонтальная платформа

```kotlin
engine.addPlatform(x1r, y1r, x2r, y2r)
```

| Параметр | Описание |
|---|---|
| `x1r` | Левый край (доля ширины) |
| `y1r` | Верхний край (доля высоты) |
| `x2r` | Правый край (доля ширины) |
| `y2r` | Нижний край (доля высоты) |

```kotlin
// Горизонтальная платформа — стандартная толщина ≈ 0.04
engine.addPlatform(0.10f, 0.68f, 0.50f, 0.72f)
```

### Вертикальная колонна

Та же функция, узкий X-диапазон и большой Y-диапазон:

```kotlin
// Колонна от потолка (y=0.00) до 55% высоты
engine.addPlatform(0.45f, 0.00f, 0.50f, 0.55f)

// Колонна от y=0.60 до пола (y=1.00)
engine.addPlatform(0.45f, 0.60f, 0.50f, 1.00f)

// Колонна посередине комнаты
engine.addPlatform(0.45f, 0.30f, 0.50f, 0.90f)
```

Платформы — твёрдые тела: игрок стоит на них, не проваливается сквозь.
Пока дверь закрыта, она тоже является твёрдым телом.

---

## Шипы

Любое касание шипа = мгновенная смерть. Четыре направления острия.

### Шипы на полу (острием вверх)

```kotlin
engine.addSpikesFloor(x1r, x2r)
```

Размещает ряд шипов на полу между `x1r` и `x2r`. Шипы стоят вплотную к полу,
острие направлено вверх.

```kotlin
engine.addSpikesFloor(0.10f, 0.80f)   // шипы по полу от 10% до 80%
```

### Шипы на произвольной горизонтальной поверхности

```kotlin
engine.addSpikesAt(x1r, x2r, yr, dir)
```

| `dir` | Где шипы | Острие |
|---|---|---|
| `SpikeDir.UP` | На верхней грани платформы (или пола) | Вверх |
| `SpikeDir.DOWN` | На нижней грани платформы (или потолка) | Вниз |

```kotlin
// На верхней грани платформы (yr = уровень платформы)
engine.addSpikesAt(0.20f, 0.50f, yr = 0.68f, dir = SpikeDir.UP)

// Под платформой — прыгать в неё снизу больно
engine.addSpikesAt(0.52f, 0.90f, yr = 0.42f, dir = SpikeDir.DOWN)
```

### Боковые шипы на вертикальной поверхности

```kotlin
engine.addSpikesWall(y1r, y2r, xr, dir)
```

| `dir` | Где | Острие |
|---|---|---|
| `SpikeDir.LEFT` | На правой стене / правом боку | Влево |
| `SpikeDir.RIGHT` | На левой стене / левом боку | Вправо |

```kotlin
// Шипы на правой стене (не дают подойти к двери сверху)
engine.addSpikesWall(0.10f, 0.55f, xr = 0.96f, dir = SpikeDir.LEFT)

// Шипы на правом боку колонны (xr = правый край колонны)
engine.addSpikesWall(0.40f, 0.95f, xr = 0.50f, dir = SpikeDir.RIGHT)

// Шипы на левом боку колонны (xr = левый край колонны)
engine.addSpikesWall(0.40f, 0.95f, xr = 0.45f, dir = SpikeDir.LEFT)
```

---

## Ключи

```kotlin
engine.placeKey(xr, yr)
engine.placeKey(xr, yr, id = 1, color = Color.rgb(255, 80, 80))
```

| Параметр | Описание |
|---|---|
| `xr` | Центр ключа по X |
| `yr` | Верхняя грань поверхности под ключом |
| `id` | Уникальный номер (для `openDoorWhenKey(id)`). По умолчанию 0 |
| `color` | Цвет ключа. По умолчанию золотой |

Ключ размером 30×30 px рисуется чуть выше поверхности `yr`.
При сборе: `key.isCollected = true`, `player.hasKey = true`, `collectedKeys.add(id)`.

```kotlin
// Золотой ключ на платформе y=0.42
engine.placeKey(0.72f, 0.42f)

// Красный ключ с id=1
engine.placeKey(0.22f, 0.70f, id = 1, color = Color.rgb(255, 80, 80))

// Синий ключ с id=2
engine.placeKey(0.68f, 0.42f, id = 2, color = Color.rgb(80, 140, 255))
```

---

## Дверь и условия её открытия

Дверь по умолчанию закрыта. Есть несколько способов открыть.

### Авто-открытие по ключу

```kotlin
// Открывается когда собраны ВСЕ ключи уровня
engine.openDoorWhenAllKeys()

// Открывается когда собран ЛЮБОЙ ключ
engine.openDoorWhenAnyKey()

// Открывается когда собран ключ с конкретным id
engine.openDoorWhenKey(id = 1)
```

### Открытие вручную (из хука)

```kotlin
engine.door.isOpen = true    // открыть
engine.door.isOpen = false   // закрыть
```

### Дверь открыта с самого начала

```kotlin
engine.door.isOpen = true    // вызвать в setup() до любых хуков
```

### Дверь проходима, но выглядит закрытой

По умолчанию `door.visuallyOpen = true` — при открытии рисуется зелёный контур.
Если установить `false`, дверь **всегда** рисуется как закрытая (tan + замок),
независимо от `isOpen`. Физически дверь всё равно становится проходимой.

```kotlin
engine.door.visuallyOpen = false   // вызвать в setup()
engine.openDoorWhenAllKeys()       // дверь откроется физически, но визуально — нет
```

---

## Кнопка

Кнопка расположена на полу примерно на x = 42% ширины. Положение фиксировано.
Активируется, когда игрок стоит на ней и касается её хитбокса.

### Хуки кнопки

```kotlin
engine.onButtonPressed  = { e -> /* при нажатии */ }
engine.onButtonReleased = { e -> /* при отпускании */ }
```

`e.button.pressCount` — сколько раз кнопка была нажата (полный цикл нажал→отпустил→нажал).

```kotlin
// Открыть дверь только если у игрока есть ключ
engine.onButtonPressed = { e ->
    if (e.player.hasKey) e.door.isOpen = true
}

// Кнопка-ловушка: закрыть дверь и засыпать пол шипами
engine.onButtonPressed = { e ->
    e.door.isOpen = false
    if (e.button.pressCount == 1) {
        e.addSpikesFloor(0.04f, 0.92f)
    }
}

// Дверь открывается только при первом нажатии
engine.onButtonPressed = { e ->
    if (e.button.pressCount == 1) e.door.isOpen = true
}
```

### Переместить кнопку

```kotlin
// xr — левый край кнопки, yr — нижний край (доля высоты комнаты)
engine.placeButton(xr = 0.20f, yr = 1.00f)   // у пола, ближе к левому краю
engine.placeButton(xr = 0.70f, yr = 0.69f)   // на платформе (yr = нижний край платформы)
```

По умолчанию кнопка стоит на полу у x ≈ 42%. Размер фиксирован (54×24 px).

### Скрытая кнопка

```kotlin
engine.button.hidden = true   // кнопка не рисуется и не реагирует на игрока
```

---

## Условие победы

```kotlin
engine.winCondition = { e -> /* Boolean */ }
```

Проверяется каждый тик, **только если дверь открыта**. Возвращает `true` → уровень пройден.

```kotlin
// Стандарт: стоять внутри проёма открытой двери
engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }

// Нужен ключ И дверь открыта И игрок у двери
engine.winCondition = { e ->
    e.player.hasKey && e.door.isOpen && isPlayerAtDoor(e)
}
```

`isPlayerAtDoor(e)` — защищённый метод базового класса `Level`, проверяет пересечение
игрока с проёмом двери.

---

## Логика на каждый кадр (onUpdate)

```kotlin
engine.onUpdate = { e, dt ->
    // вызывается каждый тик ДО физики
    // dt — время кадра в секундах
}
```

Используется для динамического поведения: движущихся платформ, таймеров, скриптов.

```kotlin
// Пример: открыть дверь через 3 секунды
var timer = 0f
engine.onUpdate = { e, dt ->
    timer += dt
    if (timer >= 3f) e.door.isOpen = true
}
```

---

## Пользовательский рендеринг

```kotlin
override fun draw(canvas: Canvas, engine: GameEngine, paint: Paint) {
    // рисуется поверх всего (HUD, таймеры, подсказки)
    paint.color = Color.WHITE
    paint.textSize = 52f
    paint.textAlign = Paint.Align.CENTER
    canvas.drawText("Текст", engine.room.w / 2f, 200f, paint)
}
```

---

## Доступные поля GameEngine внутри хуков

```kotlin
e.player.hasKey        // Bool: есть ли хоть один ключ
e.player.isOnGround    // Bool: стоит на поверхности
e.player.isDead        // Bool: мёртв
e.player.x / .y        // позиция (пиксели)

e.collectedKeys        // Set<Int>: id собранных ключей
e.keys                 // List<Key>: все ключи уровня
e.spikes               // MutableList<Spike>: все шипы
e.platforms            // MutableList<RectF>: платформы
e.button.pressCount    // Int: счётчик нажатий
e.button.hidden        // Bool
e.door.isOpen          // Bool

e.room.w / .h          // размеры комнаты в пикселях
e.room.wallThick       // 30f

// Добавить шипы прямо из хука:
e.addSpikesFloor(0.10f, 0.90f)
e.addSpikesWall(0.10f, 0.60f, xr = 0.96f, dir = SpikeDir.LEFT)
e.addSpikesAt(0.20f, 0.60f, yr = 0.50f, dir = SpikeDir.DOWN)
e.addPlatform(0.10f, 0.50f, 0.40f, 0.54f)
```

---

## Регистрация уровней

**Файл:** `levels/LevelRegistry.kt`

```kotlin
object LevelRegistry {
    private val levels: List<Level> = listOf(
        Level01(),
        Level02(),
        Level03(),
        // ...
    )
    val count: Int get() = levels.size
    fun get(number: Int): Level = levels[number - 1]
}
```

Уровни проходятся по порядку. После последнего вызывается `onGameComplete`.

---

## Готовые паттерны

### Простой уровень: нажми кнопку

```kotlin
override fun setup(engine: GameEngine) {
    engine.onButtonPressed = { e -> e.door.isOpen = true }
    engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
}
```

### Ключ открывает дверь без кнопки

```kotlin
override fun setup(engine: GameEngine) {
    engine.addPlatform(0.52f, 0.42f, 0.92f, 0.46f)
    engine.placeKey(0.72f, 0.42f)
    engine.openDoorWhenAnyKey()
    engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
}
```

### Два ключа + автодверь + кнопка-ловушка

```kotlin
override fun setup(engine: GameEngine) {
    engine.addPlatform(0.04f, 0.70f, 0.40f, 0.74f)
    engine.addPlatform(0.44f, 0.42f, 0.88f, 0.46f)

    engine.placeKey(0.22f, 0.70f, id = 1, color = Color.rgb(255, 80, 80))
    engine.placeKey(0.68f, 0.42f, id = 2, color = Color.rgb(80, 140, 255))

    engine.addSpikesFloor(0.10f, 0.38f)
    engine.addSpikesFloor(0.46f, 0.86f)

    engine.openDoorWhenAllKeys()

    engine.onButtonPressed = { e ->
        if (e.button.pressCount == 1) e.addSpikesFloor(0.04f, 0.86f)
    }

    engine.winCondition = { e -> e.door.isOpen && isPlayerAtDoor(e) }
}
```

### Дверь открыта, кнопка закрывает её

```kotlin
override fun setup(engine: GameEngine) {
    engine.door.isOpen = true
    engine.placeKey(0.72f, 0.42f)

    engine.onButtonPressed = { e ->
        e.door.isOpen = false
        e.addSpikesFloor(0.04f, 0.92f)
    }

    engine.winCondition = { e ->
        e.player.hasKey && e.door.isOpen && isPlayerAtDoor(e)
    }
}
```

### Вертикальная колонна с шипами с одной стороны

```kotlin
override fun setup(engine: GameEngine) {
    // Колонна
    engine.addPlatform(0.44f, 0.35f, 0.49f, 1.00f)
    // Шипы справа от колонны — нельзя подходить справа
    engine.addSpikesWall(0.35f, 0.95f, xr = 0.49f, dir = SpikeDir.RIGHT)
}
```

---

## Советы по дизайну уровней

**Координаты платформ**
- Платформа на y = 0.68 достигается с пола одним прыжком (стандартная нижняя).
- Платформа на y = 0.42 достигается прыжком с нижней платформы (стандартная верхняя).
- Разница > 0.30 между платформами — прыжок может не долететь.

**Шипы**
- Шипы на полу (addSpikesFloor) оставляйте зазор у спавна (x > 0.08) и у кнопки.
- Боковые шипы y2r ≤ 0.65 не мешают подходу к двери по полу (игрок на полу ≈ y 0.90+).
- Шипы снизу платформы (DOWN) опасны только при прыжке снизу — сверху безопасно.

**Кнопка**
- По умолчанию на x ≈ 42% пола. Переместить: `engine.placeButton(xr, yr)`.
- Используйте `pressCount == 1` если хотите сработать ровно один раз.
- Скройте кнопку (`button.hidden = true`) если она не нужна на уровне.

**Ключи**
- `id = 0` (по умолчанию) подходит если ключ один.
- `openDoorWhenAllKeys()` срабатывает на все ключи уровня, независимо от id.
- `openDoorWhenKey(id)` — только на конкретный ключ, остальные игнорируются.
