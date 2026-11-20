# hh00

Клиент для Minecraft **1.21.11** (Fabric, Yarn). Меню в стиле hh00 v4 (чёрно-белое).

## Что есть

- **Меню** `HH00Screen` — открывается на Right Shift (или переназначить в настройках управления, категория hh00).
  - открывается сразу на вкладке **Settings**;
  - сайдбар с категориями Combat / Visuals / Player / Utilities / Settings;
  - аккордеоны групп, счётчики включённых;
  - переключатели с анимацией, ползунки Strength, бинды, поиск, скролл, плавные звуки;
  - шрифты Instrument Sans Regular/Bold (`assets/hh00/font/instrumentsans-regular.ttf` и `-bold.ttf`,
    провайдеры `assets/hh00/font/ui.json` и `ub.json`);
  - круглые скруглённые панели с собственным антиалиасингом, логотип hh00 рисуется вектором.
- **Модули**: `Module` + `ModuleManager` (вкл/выкл, бинды, настройки, авто-подписка на события).
  Тумблеры в меню связаны с реальными модулями: если модуль с таким именем существует — дёргается он,
  иначе состояние хранится в `HH00Screen.ENABLED`.
- **EventBus** — `@Subscribe`, приоритеты, отмена.
- **Settings** — `BooleanSetting`, `NumberSetting`, `ModeSetting`.
- **Config** — `config/hh00/config.json` (Gson).
- **Команды** — `.toggle`, `.bind`, `.help`.
- **HUD** — `Render2DEvent` (водяной знак), `Sprint`.
- **Миксин** — `KeyboardMixin` (`KeyEvent` для биндов модулей).

## Структура

```
src/main/java/dev/hh00/
├── Client.java                  # синглтон, все менеджеры
├── ClientInitializer.java       # entrypoint + keybind Right Shift
├── gui/HH00Screen.java          # меню v2
├── core/                        # Module, ModuleManager, Category, KeyBind
│   └── setting/                 # Setting, Boolean, Number, Mode
├── event/                       # EventBus + events/
├── config/ConfigManager.java
├── command/                     # Command, CommandManager, commands/
├── module/modules/
│   ├── movement/Sprint.java
│   └── render/Hud.java
├── mixin/KeyboardMixin.java
└── util/                        # ChatUtil, KeyUtil
src/main/resources/
├── fabric.mod.json
├── client.mixins.json
└── assets/hh00/
    ├── Manrope-Medium.ttf
    ├── font/ui.json
    └── lang/en_us.json
```

## Сборка

Нужен JDK 21. В папке есть wrapper (Gradle 8.14).

```bash
gradlew build
```

⚠ Путь с `(3)` ломает `gradlew.bat` (batch не любит скобки). Если не запускается — используй:

```powershell
java -classpath "gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain build
```

или перенеси проект в путь без скобок.

Готовый jar: `build/libs/hh00-1.0.0.jar`.

## Как добавить модуль

```java
public class MyModule extends Module {
    private final NumberSetting range = register(new NumberSetting("Range", 3.0, 1.0, 6.0, 0.1));

    public MyModule() {
        super("Aura", "Description", Category.COMBAT);
    }

    @Subscribe
    public void onTick(TickEvent event) {
        // ...
    }
}
```

`register(new MyModule());` в `ModuleManager#registerDefaults`.
Имя модуля в меню (`DATA` в `HH00Screen`) должно совпадать с `getName()`, тогда тумблер будет управлять модулем.
