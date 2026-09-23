# Compose Hot Reload MCP — maintainer-verified how-to (v3.1 input)

Provided by the maintainer on 2026-09-23, verified by them in another Kotlin/JVM Compose Desktop
project on Windows. Where it conflicts with the researcher files in this directory, THIS file wins —
it is first-hand, the research is desk research.

## Facts checked on this machine (2026-09-23)

- `NoDefaultCurrentDirectoryInExePath=1` is set: bare `gradlew.bat` fails with "not recognized";
  `cmd /c ".\gradlew.bat ..."` works. `.mcp.json` must use the explicit `.\\gradlew.bat` path.
- JetBrains Runtime 21.0.9 is installed (`C:\Users\1\.jdks\jbr-21.0.9`, vendor JetBrains) and listed
  by `gradlew -q javaToolchains` → the foojay toolchain resolver is NOT needed (overrides the
  STACK.md/ARCHITECTURE.md recommendation). JitPack gets its JDK from `jitpack.yml`, not a resolver.
- `JAVA_TOOL_OPTIONS` is set globally; every JVM prints `Picked up JAVA_TOOL_OPTIONS: ...` to
  STDERR. stderr does not corrupt MCP stdio, but do not redirect stderr into stdout in `.mcp.json`.
- No `androidx.lifecycle` / ViewModel usage anywhere in the repo → the lifecycle 2.11.0 note below
  does not apply to this project.
- The showcase is ONE long `verticalScroll` column with all sections (`ShowcaseApp.kt:75-119`) —
  section captures need scrolling (MCP `scroll`) or a section selector.

## Maintainer's how-to (verbatim, Russian)

Цель: агент управляет запущенным приложением — получает семантическое дерево с координатами, кликает,
вводит текст, читает логи и применяет правки кода без перезапуска.

### Версии (проверено)
- Compose Multiplatform **≥ 1.12.0** — жёсткое требование. На 1.11.1 задача `hotMcpServer` есть и
  приложение подключается, но окно не регистрируется: `list_windows` = `[]`, все оконные инструменты
  отвечают «No application window is currently available».
- Плагин `org.jetbrains.compose.hot-reload` **1.2.0** указать явно (MCP-сервер появился в
  1.2.0-alpha01), чтобы версия не менялась молча вместе с плагином Compose.
- Вместе с Compose 1.12.0 поднять `org.jetbrains.androidx.lifecycle:*` до **2.11.0**. На более старой
  версии тесты ViewModel падают с `NoSuchMethodError: ViewModelStore.put`.
- JetBrains Runtime 21. Если он уже есть среди Gradle toolchains (`./gradlew -q javaToolchains`),
  плагин foojay не нужен. Java target — не выше 21.

### Подключение
1. `libs.versions.toml`:
   - в `[versions]`: `compose-hot-reload = "1.2.0"`;
   - в `[plugins]`: `compose-hot-reload = { id = "org.jetbrains.compose.hot-reload", version.ref = "compose-hot-reload" }`.
2. `build.gradle.kts`: `alias(libs.plugins.compose.hot.reload)`.
3. Проверка: `gradlew tasks --all` показывает `hotMcpServer` и `hotRun`.
4. `.mcp.json` в корне проекта:
   ```json
   { "mcpServers": { "compose-hot-reload": {
       "command": "cmd",
       "args": ["/c", ".\\gradlew.bat", "--no-daemon", "--quiet", "--console=plain", "hotMcpServer"] } } }
   ```
   - `./gradlew` на Windows не запускается.
   - Голый `gradlew.bat` не находится, если выставлена `NoDefaultCurrentDirectoryInExePath=1`.
     Поэтому путь явный: `.\gradlew.bat`.
5. Перезапустить Claude Code: сервер подключается только при старте сессии. Инструменты появятся
   под именами `mcp__compose-hot-reload__*`.

### Запуск приложения
- Отдельно, в фоне: `./gradlew hotRun --mainClass=MainKt` (для Kotlin/JVM-проекта задача `hotRun`,
  не `hotRunJvm`). Сервер сам дождётся приложения и подключится. (Here: `--mainClass=com.mordred.showcase.MainKt`.)
- Первым вызовом — `status`: должно быть `connected:true`. Затем `list_windows`: список не должен
  быть пустым.
- Приложение с защитой от второго экземпляра перед новым запуском закрывать через `CloseMainWindow()`.

### Нюансы
- **Не использовать `take_screenshot` сервера.** Он снимает область экрана под окном, а не
  содержимое окна. Если приложение закрыто другими окнами, в кадр попадёт чужое — у нас попал личный
  браузер. Снимать окно через WinAPI `PrintWindow(hwnd, hdc, 2)`: берётся содержимое самого окна,
  даже если оно перекрыто. Перед просмотром кадра проверить пару пикселей на фон приложения.
- **Свёрнутое окно выпадает из сервера**: `list_windows` = `[]`. Разворачивать без перехвата
  фокуса — `ShowWindow(h, 4)` (SW_SHOWNOACTIVATE). Держать окно на заднем плане —
  `SetWindowPos(h, HWND_BOTTOM=1, …, 0x13)`.
- `click`, `type_text`, `scroll` работают через семантику Compose: настоящий курсор не двигается,
  фокус окна не нужен, работает под другими окнами.
- `get_semantic_tree` — лучший вход для аудита вёрстки: у каждого узла есть текст, testTag и точные
  px-bounds. Ставить `testTag` на значимые элементы.
- Инструмент `reload` может ответить «Recompilation failed», хотя компиляция проходит. Проверять и
  применять правки через `./gradlew reload` — вывод честный.
- Правка, которая меняет только начальное состояние (запомненные настройки, стартовые данные), при
  reload не видна — нужен `restart`.
- ID узлов дерева меняются после рекомпозиции: перед кликом брать свежее дерево.
- Все задачи Gradle — в foreground. Фоновый процесс с рабочей папкой внутри worktree мешает потом
  удалить этот worktree.

## Consequences for v3.1 planning

- PrintWindow works on ANY toolchain (it does not need the MCP server) → the BEFORE baseline on
  CMP 1.11.1 and the AFTER captures on 1.12.0 can use the SAME capture pipeline, which removes the
  CopyFromScreen-vs-Robot calibration problem raised in PITFALLS.md / SUMMARY.md, and makes the
  baseline capture non-interfering too (no real cursor, no focus). Navigation on the old toolchain
  still has no MCP — needs a non-interfering way to reach each section (e.g. a showcase-only
  section selector, used identically before and after).
- The "does not interfere" proof keeps its original shape, with PrintWindow instead of
  `take_screenshot` and `ShowWindow(h, 4)` for the minimized case.
- A committed capture helper (PowerShell P/Invoke over `PrintWindow` / `ShowWindow` /
  `SetWindowPos`) is part of the setup, since every later visual phase reuses it.
