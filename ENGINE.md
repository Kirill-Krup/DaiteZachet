# Архитектура движка DaiteZachet

## Обзор

Игра — 2D-платформер на Android (Kotlin). Один экран = один игровой уровень в одной
прямоугольной комнате. Пять классов составляют ядро движка:

```
GameView      — игровой цикл, рендеринг, переходы между уровнями
GameEngine    — физика, логика взаимодействий, хуки уровня
Room          — статическая геометрия комнаты
InputHandler  — обработка мультитача
Player        — состояние игрока
```

---

## Room — геометрия комнаты

**Файл:** `engine/Room.kt`

Создаётся один раз при старте уровня с размерами игровой области:

```kotlin
Room(w = screenW, h = screenH * 0.78f)
```

Содержит только **неизменяемую** геометрию:

| Поле | Описание |
|---|---|
| `wallThick = 30f` | Толщина стен/пола/потолка в пикселях |
| `ceiling` | Потолок (y = 0) |
| `floor` | Пол (y = h − 30) |
| `leftWall` | Левая стена |
| `rightWallTop` | Правая стена выше проёма двери |
| `rightWallBottom` | Правая стена ниже проёма двери |
| `doorTop = h * 0.30f` | Верхний край проёма двери |
| `doorBottom = h − wallThick` | Нижний край проёма двери |
| `doorRect` | Прямоугольник двери |
| `buttonRect` | Прямоугольник кнопки (x ≈ 42%, на полу) |
| `playerSpawnX/Y` | Точка спавна у левой стены |
| `staticSolids` | Список всех статических коллайдеров |

Платформы уровня — **динамические**, они хранятся в `GameEngine.platforms`.

---

## GameEngine — физика и логика

**Файл:** `engine/GameEngine.kt`

### Константы

```kotlin
GRAVITY    = 1600f   // px/s², вниз
JUMP_GRACE = 0.12f   // секунды льготного окна: отпускание кнопки не обрезает прыжок
```

### Состояние

```kotlin
val player: Player
val button: Button
val door:   Door
val keys:   MutableList<Key>
val spikes: MutableList<Spike>
val platforms: MutableList<RectF>
val collectedKeys: MutableSet<Int>   // id собранных ключей
var levelComplete: Boolean
```

### Входные флаги (выставляет InputHandler)

```kotlin
@Volatile var moveLeft:      Boolean
@Volatile var moveRight:     Boolean
@Volatile var jumpRequested: Boolean   // одиночный импульс при нажатии
@Volatile var jumpHeld:      Boolean   // держится ли кнопка прыжка
```

### Хуки уровня (лямбды, задаются в Level.setup)

```kotlin
var onButtonPressed:  ((GameEngine) -> Unit)?
var onButtonReleased: ((GameEngine) -> Unit)?
var onUpdate:         ((GameEngine, Float) -> Unit)?   // вызывается каждый тик
var doorCondition:    ((GameEngine) -> Boolean)?       // авто-открытие двери
var winCondition:     ((GameEngine) -> Boolean)?       // условие победы
```

### Игровой цикл (вызывается GameView каждый кадр)

```
engine.update(dt)
  ├── onUpdate(engine, dt)        ← пользовательская логика уровня
  ├── applyPhysics(dt)            ← физика игрока
  └── checkInteractions()         ← шипы / ключи / кнопка / дверь / победа
```

### Физика (applyPhysics)

Порядок операций за один тик:

1. **Горизонтальная скорость** — устанавливается из флагов ввода (±480 px/s или 0).
2. **Прыжок** — если `jumpRequested && isOnGround`: `vy = −920 px/s`, запускается таймер grace.
3. **Jump cut** — если кнопка отпущена в воздухе *после* истечения `JUMP_GRACE`:
   `vy = max(vy, −320)` (обрезает восходящую скорость).
4. **Гравитация** — `vy += 1600 * dt`, ограничена максимумом `1200 px/s`.
5. **Движение X** → разрешение горизонтальных столкновений (AABB).
6. **Движение Y** → разрешение вертикальных столкновений (AABB), обновление `isOnGround`.

### Разрешение столкновений

Используется **AABB** (axis-aligned bounding box). Горизонтальные и вертикальные
коллизии разрешаются отдельно (resolveH / resolveV) для корректного поведения
на углах. В список коллайдеров входят: статические стены комнаты + платформы +
дверь (если закрыта).

### Взаимодействия (checkInteractions)

Выполняются **в строгом порядке**:

1. **Шипы** → пересечение с любым шипом → `player.isDead = true`, выход.
2. **Ключи** → сбор при пересечении → `key.isCollected`, `collectedKeys.add(id)`, `player.hasKey = true`.
3. **Кнопка** → стоит на кнопке и на земле → `onButtonPressed` / `onButtonReleased`.
4. **doorCondition** → если возвращает `true` → `door.isOpen = true`.
5. **winCondition** → если возвращает `true` → `levelComplete = true`.

### reset()

Вызывается после смерти. Очищает: игрока, кнопку, дверь, ключи, шипы, платформы,
все флаги ввода. Хуки (`onButtonPressed` и т.д.) **не сбрасываются** — `Level.setup()`
перезапишет их сам.

---

## Player — состояние персонажа

**Файл:** `engine/Player.kt`

```kotlin
WIDTH      = 44f    // px
HEIGHT     = 58f    // px
MOVE_SPEED = 480f   // px/s
JUMP_FORCE = −920f  // px/s (вверх)
```

Поля состояния: `x, y, vx, vy, isOnGround, hasKey, isDead`.
`bounds: RectF` — пересчитывается каждый раз при обращении.

---

## GameObjects — игровые объекты

**Файл:** `engine/GameObjects.kt`

### Button
```
bounds: RectF       — фиксирован (из room.buttonRect, x ≈ 42%)
isPressed: Boolean
pressCount: Int     — счётчик нажатий (инкремент при каждом новом касании)
hidden: Boolean     — если true — не рисуется и не реагирует на игрока
```

### Door
```
bounds: RectF        — правая стена, y = 30%..97% комнаты
isOpen: Boolean      — если false — является твёрдым телом (блокирует движение)
visuallyOpen: Boolean — если false — дверь рисуется как закрытая (tan + замок),
                        даже когда isOpen = true (по умолчанию true)
```

### Key
```
bounds: RectF       — 30×30 px
id: Int             — уникальный идентификатор (0 по умолчанию)
color: Int          — цвет (android.graphics.Color)
isCollected: Boolean
```

### Spike
```
bounds: RectF
dir: SpikeDir       — UP / DOWN / LEFT / RIGHT (направление острия)
width/height        — 28×24 px по умолчанию (для стеновых — 24×28)
```

---

## SpikeDir — направления шипов

```kotlin
UP    // острие вверх  → лежит на полу или верхней грани платформы
DOWN  // острие вниз   → висит с потолка или нижней грани платформы
LEFT  // острие влево  → торчит из правой стены или правого бока
RIGHT // острие вправо → торчит из левой стены или левого бока
```

---

## InputHandler — управление

**Файл:** `engine/InputHandler.kt`

Три виртуальные кнопки в нижней полосе (22% высоты экрана):

| Кнопка | X-диапазон экрана |
|---|---|
| `<` (влево) | 0% — 28% |
| `>` (вправо) | 36% — 64% |
| `^` (прыжок) | 72% — 100% |

Поддерживает **мультитач**: одновременное нажатие влево + прыжок работает корректно.
При `ACTION_DOWN` на кнопку прыжка выставляется `engine.jumpRequested = true` (однократный импульс).
`jumpHeld` обновляется каждый MotionEvent и отражает текущее состояние.

---

## GameView — цикл и рендеринг

**Файл:** `engine/GameView.kt`

### Размеры
```kotlin
gameH = screenH * 0.78f   // игровая область (78% высоты экрана)
// нижние 22% — панель управления
```

### Игровой цикл
Работает в отдельном потоке (`Thread`), цель — **60 FPS** (sleep до 16 мс/кадр).
`dt` ограничен сверху: `dt = min(rawDt, 0.05f)` — защита от физических артефактов
при паузах/низком FPS.

### Переходы

| Событие | Задержка | Действие |
|---|---|---|
| Смерть (`isDead`) | 1.0 с | `engine.reset()` + `level.setup(engine)` |
| Победа (`levelComplete`) | 1.4 с | Загрузка следующего уровня или `onGameComplete` |

### Порядок отрисовки
```
Фон → Стены/платформы → Дверь → Кнопка → Ключи → Шипы →
Игрок → HUD → Кнопка выхода → Панель управления →
level.draw() → Оверлей смерти/победы
```

### Кнопка выхода (✕)
Отрисовывается в правом верхнем углу. Обрабатывается до делегирования
тач-событий в `InputHandler` — вызывает `onExitLevel`.

---

## Жизненный цикл уровня

```
LevelRegistry.get(n)          → получить объект уровня
Room(screenW, gameH)          → создать комнату
GameEngine(room)              → создать движок
InputHandler(screenW, screenH, gameH)
level.setup(engine)           → разместить объекты, задать хуки

// каждый кадр:
engine.update(dt)
drawFrame(canvas)

// после победы:
engine.reset()
level.setup(engine)           // при рестарте
// или
setupLevel(levelNumber + 1)   // следующий уровень
```
