# ChessLock Architecture Documentation

    > **Complete architectural overview of the ChessLock app - a chess puzzle/game lockscreen for Android**

    ## 📋 **Table of Contents**

    1. [Architecture Overview](#architecture-overview)
    2. [High-Level Diagram](#high-level-architecture-diagram)
    3. [Detailed Data Flow](#detailed-data-flow-diagram)
    4. [User Interaction Scenarios](#complete-user-interaction-flow)
    5. [Component Interaction Map](#component-interaction-map)
    6. [Touch Event Flow](#touch-event-flow-critical-for-interactivity)
    7. [Key Implementation Details](#key-implementation-details)
    8. [Build & Deploy](#build--deploy)

    ---

    ## 🎯 **Architecture Overview**

    ChessLock uses a **layered architecture** with clear separation between:

    - **Flutter Layer** (UI, game logic, state management)
    - **Native Layer** (lockscreen service, overlay management)
    - **Storage Layer** (SharedPreferences for persistence)

    ### **Core Design Principles**

    ✅ **Single Source of Truth** - SharedPreferences is the persistent storage
    ✅ **Provider Pattern** - State management with ChangeNotifier
    ✅ **Native Overlay** - TYPE_APPLICATION_OVERLAY for full touch support
    ✅ **Keyguard Integration** - Respects native security, shows after unlock
    ✅ **Game State Persistence** - Save/restore active games

    ---

    ## ✅ **HOW SETTINGS SYNC BETWEEN MAIN APP AND LOCKSCREEN**

    The app uses **SharedPreferences** as the persistent storage layer that both the main app and lockscreen overlay read from.

    ---

    ## 🎯 **High-Level Architecture Diagram**

    ```text
    ┌────────────────────────────────────────────────────────────────┐
    │                       MAIN APP UI                              │
    │────────────────────────────────────────────────────────────────│
    │  User changes settings:                                        │
    │  • Full Game / Puzzle Mode                                     │
    │  • Difficulty (EASY/MEDIUM/HARD)                              │
    │  • Board Colors & Theme                                        │
    │  • Button Visibility                                           │
    └───────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
    ┌────────────────────────────────────────────────────────────────┐
    │                   PROVIDERS LAYER                              │
    │────────────────────────────────────────────────────────────────│
    │  • LockScreenProvider (game logic, difficulty)                 │
    │  • AppearanceProvider (colors, UI visibility)                  │
    │  • GameStateService (FEN, move history)                        │
    └───────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
    ┌────────────────────────────────────────────────────────────────┐
    │                  SHARED PREFERENCES                            │
    │────────────────────────────────────────────────────────────────│
    │  Persistent storage for all game & UI settings                │
    │  • difficulty, isFullGameMode                                  │
    │  • lightSquareColor, darkSquareColor, backgroundColor          │
    │  • showHintButton, showUndoButton, boardOpacity               │
    │  • game_state (FEN, moveHistory, puzzleId)                    │
    └───────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
    ┌────────────────────────────────────────────────────────────────┐
    │                 LOCKSCREEN SERVICE                             │
    │────────────────────────────────────────────────────────────────│
    │  ChessLockAccessibilityService.kt                             │
    │  • Detects native keyguard (PIN/Pattern)                      │
    │  • Dismisses keyguard if exists                               │
    │  • Shows TYPE_APPLICATION_OVERLAY                             │
    │  • Creates touch-enabled FlutterView                          │
    └───────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
    ┌────────────────────────────────────────────────────────────────┐
    │              FLUTTER VIEW OVERLAY                              │
    │────────────────────────────────────────────────────────────────│
    │  LockscreenOverlay Widget                                      │
    │  • Loads providers from SharedPreferences                      │
    │  • Renders ChessBoardWidget with custom colors                │
    │  • Handles full game / puzzle logic                           │
    │  • Touch-enabled for drag & drop moves                        │
    └───────────────────────────┬────────────────────────────────────┘
                                │
                                ▼
    ┌────────────────────────────────────────────────────────────────┐
    │                 USER INTERACTION                               │
    │────────────────────────────────────────────────────────────────│
    │  • User drags chess pieces                                     │
    │  • Makes legal moves                                           │
    │  • Checkmate detected → Device unlocks                        │
    │  • Emergency unlock available                                  │
    └────────────────────────────────────────────────────────────────┘
    ```

    ---

    ## 🏗️ **Detailed Data Flow Diagram**

        ```
        ┌─────────────────────────────────────────────────────────────┐
        │                     MAIN APP UI                             │
        │  (Settings Screen, Lock Screen, etc.)                      │
        └─────────────────────┬───────────────────────────────────────┘
                            │
                            │ User changes settings
                            ▼
        ┌─────────────────────────────────────────────────────────────┐
        │              LockScreenProvider                             │
        │  • Manages game state (puzzle/full game)                   │
        │  • Manages difficulty                                       │
        │  • Manages appearance settings                             │
        │  • Saves to SharedPreferences                              │
        └─────────────────────┬───────────────────────────────────────┘
                            │
                            │ Writes to
                            ▼
        ┌─────────────────────────────────────────────────────────────┐
        │              SharedPreferences                              │
        │  Keys:                                                      │
        │  • 'difficulty' → 'EASY'/'MEDIUM'/'HARD'                   │
        │  • 'isFullGameMode' → true/false                           │
        │  • 'lightSquareColor' → Color int                          │
        │  • 'darkSquareColor' → Color int                           │
        │  • 'backgroundColor' → Color int                           │
        │  • 'showHintButton' → true/false                           │
        │  • Game state (FEN, move history, etc.)                    │
        └─────────────────────┬───────────────────────────────────────┘
                            │
                            │ Reads from
                            ▼
        ┌─────────────────────────────────────────────────────────────┐
        │         LOCKSCREEN OVERLAY (Native Android)                 │
        │  ChessLockAccessibilityService.kt                          │
        │  • Dismisses keyguard                                       │
        │  • Shows FlutterView overlay                               │
        │  • FlutterEngine loads /lockscreen route                   │
        └─────────────────────┬───────────────────────────────────────┘
                            │
                            │ Renders
                            ▼
        ┌─────────────────────────────────────────────────────────────┐
        │         LockscreenOverlay (Flutter Widget)                  │
        │  • Consumer2<LockScreenProvider, AppearanceProvider>        │
        │  • Reads difficulty from SharedPreferences                  │
        │  • Reads game mode (puzzle vs full game)                   │
        │  • Applies appearance colors                               │
        │  • Renders ChessBoardWidget                                │
        └─────────────────────────────────────────────────────────────┘
        ```

        ---

        ## 📝 **Key Files & Their Roles**

        ### **1. LockScreenProvider** (`lib/providers/lock_screen_provider.dart`)
        **Purpose**: Central state management for chess game logic

        **Key Responsibilities**:
        - ✅ Loads settings from SharedPreferences on init
        - ✅ Manages game mode toggle (`isFullGameMode`)
        - ✅ Manages difficulty setting
        - ✅ Saves/restores game state
        - ✅ Handles puzzle solving and full game checkmate detection
        - ✅ Triggers device unlock when puzzle/game is won

        **Settings Stored**:
        ```dart
        final prefs = await SharedPreferences.getInstance();
        _difficulty = prefs.getString('difficulty') ?? 'MEDIUM';
        ```

        **Game State Persistence**:
        ```dart
        await GameStateService.saveGameState(
        fen: _chess.fen,
        isFullGameMode: _isFullGameMode,
        puzzleId: _isFullGameMode ? null : _currentPuzzle.id,
        moveHistory: _moveHistory,
        difficulty: _difficulty,
        );
        ```

        ---

        ### **2. AppearanceProvider** (`lib/providers/appearance_provider.dart`)
        **Purpose**: Manages lockscreen appearance customization

        **Settings Stored**:
        ```dart
        await prefs.setInt('lightSquareColor', _lightSquareColor.toARGB32());
        await prefs.setInt('darkSquareColor', _darkSquareColor.toARGB32());
        await prefs.setInt('backgroundColor', _backgroundColor.toARGB32());
        await prefs.setBool('showHintButton', _showHintButton);
        await prefs.setBool('showUndoButton', _showUndoButton);
        await prefs.setDouble('boardOpacity', _boardOpacity);
        ```

        **Loads on init**:
        ```dart
        AppearanceProvider() {
        _loadSettings(); // Reads from SharedPreferences
        }
        ```

        ---

        ### **3. ChessLockAccessibilityService.kt** (Native Android)
        **Purpose**: Shows Flutter overlay on lockscreen

        **Flow**:
        1. **Detects if native lock exists**:
        ```kotlin
        val isSecure = keyguardManager.isKeyguardSecure
        ```

        2. **If NO native lock → Shows overlay immediately**:
        ```kotlin
        if (!isSecure) {
            displayOverlayOnUnlockedScreen()
        }
        ```

        3. **If native lock exists → Dismisses it first**:
        ```kotlin
        KeyguardDismissActivity.launch(this)
        // Wait 120ms → displayOverlayOnUnlockedScreen()
        ```

        4. **Creates FlutterView with TYPE_APPLICATION_OVERLAY**:
        ```kotlin
        val params = WindowManager.LayoutParams(
            MATCH_PARENT, MATCH_PARENT,
            TYPE_APPLICATION_OVERLAY,
            FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS | FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )
        ```

        5. **FlutterEngine loads `/lockscreen` route**:
        ```kotlin
        flutterEngine?.navigationChannel?.setInitialRoute("/lockscreen")
        ```

        ---

        ### **4. LockscreenOverlay** (`lib/screens/lockscreen_overlay.dart`)
        **Purpose**: The actual UI shown on lockscreen

        **Reads from Providers**:
        ```dart
        Consumer2<LockScreenProvider, AppearanceProvider>(
        builder: (context, provider, appearance, child) {
            // provider.difficulty → from SharedPreferences
            // provider.isFullGameMode → from SharedPreferences
            // appearance.lightSquareColor → from SharedPreferences
            // appearance.backgroundColor → from SharedPreferences
        }
        )
        ```

        **Displays**:
        - Chess board with custom colors
        - Hint/Reset/Undo buttons (based on appearance settings)
        - Puzzle prompt or full game UI (based on `isFullGameMode`)
        - Time display
        - Emergency unlock button

        ---

        ## 🔄 **Complete Settings Sync Example**

        ### **User changes board color in main app**:

        1. **User taps color picker** in Settings Screen
        2. **AppearanceProvider updates**:
        ```dart
        void setLightSquareColor(Color color) {
        _lightSquareColor = color;
        notifyListeners(); // Updates UI immediately
        _saveSettings();   // Saves to SharedPreferences
        }
        ```

        3. **SharedPreferences updated**:
        ```dart
        await prefs.setInt('lightSquareColor', 0xFFF0D9B5);
        ```

        4. **User locks phone** → ChessLockAccessibilityService shows overlay
        5. **FlutterEngine loads `/lockscreen`** → LockscreenOverlay widget builds
        6. **AppearanceProvider constructor runs**:
        ```dart
        AppearanceProvider() {
        _loadSettings(); // Reads lightSquareColor from SharedPreferences
        }
        ```

        7. **Chess board renders with new color**:
        ```dart
        ChessBoardWidget(
        lightSquareColor: appearance.lightSquareColor, // ✅ Updated!
        )
        ```

        ---

        ### **User toggles game mode**:

        1. **User taps "Full Game Mode" in settings**
        2. **LockScreenProvider toggles**:
        ```dart
        void toggleGameMode() {
        if (_isFullGameMode) {
            _initializeNewPuzzle(); // Back to puzzle
        } else {
            startFullChessGame(); // Start full game
        }
        }
        ```

        3. **GameStateService saves**:
        ```dart
        await prefs.setString('game_state', jsonEncode({
        'isFullGameMode': true,
        'fen': 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
        'difficulty': 'MEDIUM',
        }));
        ```

        4. **User locks phone** → Overlay loads game state from SharedPreferences
        5. **LockScreenProvider restores**:
        ```dart
        final savedState = await GameStateService.loadGameState();
        _isFullGameMode = savedState['isFullGameMode']; // true
        _chess = Chess.fromFEN(savedState['fen']); // Standard starting position
        ```

        6. **UI shows full chess game board** instead of puzzle

        ---

        ## 🎯 **Key Design Principles**

        ### ✅ **Single Source of Truth**
        SharedPreferences is the single source of truth for settings.
        Both main app and lockscreen read from it.

        ### ✅ **Immediate Save**
        Every setting change immediately writes to SharedPreferences.
        No manual "Save" button needed.

        ### ✅ **Automatic Load**
        Providers load settings in their constructors.
        No manual initialization needed.

        ### ✅ **Game State Persistence**
        Active game/puzzle state is saved so users can resume if app crashes.

        ### ✅ **Two-Way Sync**
        - Main app changes → Saved to SharedPreferences → Lockscreen reads
        - Lockscreen changes → Saved to SharedPreferences → Main app reads

        ---

        ## 🔐 **Security & Permissions**

        ### **Required Permissions** (AndroidManifest.xml):
        ```xml
        <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
        <uses-permission android:name="android.permission.WAKE_LOCK" />
        <uses-permission android:name="android.permission.BIND_ACCESSIBILITY_SERVICE" />
        ```

        ### **Overlay Permission Check**:
        ```kotlin
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!Settings.canDrawOverlays(this)) {
                // Request permission
            }
        }
        ```

        ### **Accessibility Service Enabled Check**:
        ```kotlin
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        ```

        ---

        ## 📱 **Test Checklist**

        ### ✅ **Settings Sync Test**:
        1. Open main app → Change board color to red
        2. Lock phone
        3. **Expected**: Lockscreen board should be red ✅

        ### ✅ **Game Mode Test**:
        1. Open main app → Toggle "Full Game Mode" ON
        2. Lock phone
        3. **Expected**: Full chess board (not puzzle) with white to move ✅

        ### ✅ **Difficulty Test**:
        1. Open main app → Set difficulty to HARD
        2. Start full game → Lock phone
        3. **Expected**: AI makes stronger moves ✅

        ### ✅ **State Persistence Test**:
        1. Lock phone → Make 3 moves
        2. Force-close app
        3. Open app → Lock phone
        4. **Expected**: Same position restored ✅

        ---

    ---

    ## 🔄 **Complete User Interaction Flow**

    ### **Scenario 1: User Changes Board Color**

    ```text
    1. User opens Settings Screen
    ↓
    2. User taps color picker → Selects RED for light squares
    ↓
    3. AppearanceProvider.setLightSquareColor(RED)
    ↓
    4. Provider calls notifyListeners() → UI updates immediately
    ↓
    5. Provider saves to SharedPreferences:
    await prefs.setInt('lightSquareColor', 0xFFFF0000)
    ↓
    6. User locks device (power button)
    ↓
    7. ChessLockAccessibilityService detects screen off
    ↓
    8. If NO PIN: displayOverlayOnUnlockedScreen()
    If PIN exists: KeyguardDismissActivity → wait 120ms → display
    ↓
    9. FlutterEngine initializes → loads /lockscreen route
    ↓
    10. LockscreenOverlay builds
        ↓
    11. Consumer2<LockScreenProvider, AppearanceProvider> reads providers
        ↓
    12. AppearanceProvider constructor loads from SharedPreferences:
        _lightSquareColor = Color(prefs.getInt('lightSquareColor'))
        ↓
    13. ChessBoardWidget renders with RED light squares ✅
    ```

    ---

    ### **Scenario 2: User Plays Full Chess Game**

    ```text
    1. User opens Lock Screen tab → Taps "Full Game Mode"
    ↓
    2. LockScreenProvider.toggleGameMode()
    ↓
    3. Provider calls startFullChessGame():
    - Sets _isFullGameMode = true
    - Creates new Chess() with standard position
    - Saves to SharedPreferences via GameStateService
    ↓
    4. User locks device
    ↓
    5. Lockscreen overlay appears
    ↓
    6. LockScreenProvider._tryRestoreGameState() runs:
    - Reads 'isFullGameMode' = true from SharedPreferences
    - Loads FEN, move history
    ↓
    7. User drags piece (e4 square) → drops on e5
    ↓
    8. FlutterView receives PointerDown/Move/Up events
    ↓
    9. ChessBoardWidget.onMove(from: 'e2', to: 'e4')
    ↓
    10. LockScreenProvider.makeMove('e2', 'e4')
        ↓
    11. Chess engine validates move → Updates position
        ↓
    12. Saves new game state to SharedPreferences
        ↓
    13. AI engine calculates response move (difficulty: MEDIUM)
        ↓
    14. AI moves e7 → e5
        ↓
    15. User continues playing... eventually achieves checkmate
        ↓
    16. Provider detects _chess.in_checkmate
        ↓
    17. Winner = "White (You)" → _isUnlocked = true
        ↓
    18. Calls _unlockDevice('chess_game_won')
        ↓
    19. Method channel: _channel.invokeMethod('unlockDevice')
        ↓
    20. ChessLockAccessibilityService receives call
        ↓
    21. hideChessLockOverlay() → removeView() → Device unlocked ✅
    ```

    ---

    ### **Scenario 3: No Native Lock (Immediate Display)**

    ```text
    1. User has NO PIN/Pattern set on device
    ↓
    2. User locks device (power button)
    ↓
    3. ChessLockAccessibilityService.showChessLockOverlay()
    ↓
    4. Checks: keyguardManager.isKeyguardSecure = false
    ↓
    5. Logs: "🔓 No native lock - showing overlay immediately"
    ↓
    6. Calls displayOverlayOnUnlockedScreen() DIRECTLY
    ↓
    7. No KeyguardDismissActivity needed
    ↓
    8. FlutterView appears instantly with full touch enabled ✅
    ```

    ---

    ### **Scenario 4: With PIN (Post-Unlock Display)**

    ```text
    1. User HAS PIN set on device
    ↓
    2. User locks device (power button)
    ↓
    3. ChessLockAccessibilityService.showChessLockOverlay()
    ↓
    4. Checks: keyguardManager.isKeyguardSecure = true
    ↓
    5. Logs: "🔐 Native lock exists - dismissing keyguard first"
    ↓
    6. Launches KeyguardDismissActivity
    ↓
    7. Activity calls keyguardManager.requestDismissKeyguard()
    ↓
    8. Native PIN screen appears
    ↓
    9. User enters PIN correctly
    ↓
    10. onDismissSucceeded() callback fires
        ↓
    11. Waits 120ms (ensures keyguard fully dismissed)
        ↓
    12. Sends broadcast: ACTION_DISPLAY_OVERLAY
        ↓
    13. Service receives broadcast → displayOverlayOnUnlockedScreen()
        ↓
    14. Creates WindowManager.LayoutParams with:
        - TYPE_APPLICATION_OVERLAY
        - FLAG_LAYOUT_IN_SCREEN | FLAG_LAYOUT_NO_LIMITS | FLAG_KEEP_SCREEN_ON
        - NO lock screen flags (critical!)
        ↓
    15. FlutterView overlay appears with FULL touch enabled ✅
    ```

    ---

    ## 🚀 **Build & Deploy**

    ```bash
        # Clean build
        flutter clean

        # Build release APK with all fixes
        flutter build apk --release

        # Install on device
        adb install -r build/app/outputs/flutter-apk/app-release.apk

        # View logs
        adb logcat -s ChessLock:* flutter:*
        ```

        ---

    ---

    ## 🧩 **Component Interaction Map**

    ```text
    ┌─────────────────────────────────────────────────────────────────┐
    │                    FLUTTER LAYER (Dart)                         │
    ├─────────────────────────────────────────────────────────────────┤
    │                                                                 │
    │  ┌──────────────┐     ┌──────────────┐     ┌──────────────┐   │
    │  │   Settings   │────▶│ Lock Screen  │────▶│  Lockscreen  │   │
    │  │    Screen    │     │     Tab      │     │   Overlay    │   │
    │  └──────────────┘     └──────────────┘     └──────┬───────┘   │
    │         │                     │                     │           │
    │         │                     │                     │           │
    │         ▼                     ▼                     ▼           │
    │  ┌──────────────────────────────────────────────────────────┐  │
    │  │             Provider Layer (ChangeNotifier)              │  │
    │  ├──────────────────────────────────────────────────────────┤  │
    │  │  • LockScreenProvider (game logic, difficulty)           │  │
    │  │  • AppearanceProvider (colors, visibility)               │  │
    │  │  • GameStateService (FEN persistence)                    │  │
    │  └──────────────────────┬───────────────────────────────────┘  │
    │                         │                                       │
    │                         ▼                                       │
    │  ┌──────────────────────────────────────────────────────────┐  │
    │  │              SharedPreferences (Key-Value)               │  │
    │  ├──────────────────────────────────────────────────────────┤  │
    │  │  difficulty, isFullGameMode, lightSquareColor,           │  │
    │  │  darkSquareColor, backgroundColor, showHintButton,       │  │
    │  │  game_state (JSON), moveHistory, puzzleId               │  │
    │  └──────────────────────┬───────────────────────────────────┘  │
    │                         │                                       │
    └─────────────────────────┼───────────────────────────────────────┘
                            │
                            ▼ (Read/Write)
    ┌─────────────────────────────────────────────────────────────────┐
    │                   NATIVE LAYER (Kotlin)                         │
    ├─────────────────────────────────────────────────────────────────┤
    │                                                                 │
    │  ┌──────────────────────────────────────────────────────────┐  │
    │  │        ChessLockAccessibilityService.kt                  │  │
    │  ├──────────────────────────────────────────────────────────┤  │
    │  │  • Monitors screen state                                 │  │
    │  │  • Checks keyguard security                              │  │
    │  │  • Manages FlutterEngine lifecycle                       │  │
    │  │  • Creates TYPE_APPLICATION_OVERLAY                      │  │
    │  └────────────┬─────────────────────────┬───────────────────┘  │
    │               │                         │                       │
    │               ▼                         ▼                       │
    │  ┌───────────────────────┐   ┌───────────────────────┐         │
    │  │ KeyguardDismissActivity│   │    FlutterView        │         │
    │  ├───────────────────────┤   ├───────────────────────┤         │
    │  │ • Dismisses PIN/Lock  │   │ • Renders Flutter UI  │         │
    │  │ • 120ms delay         │   │ • Touch-enabled       │         │
    │  │ • Broadcasts success  │   │ • Fullscreen overlay  │         │
    │  └───────────────────────┘   └───────────────────────┘         │
    │                                                                 │
    └─────────────────────────────────────────────────────────────────┘
                            │
                            ▼
    ┌─────────────────────────────────────────────────────────────────┐
    │                  ANDROID SYSTEM LAYER                           │
    ├─────────────────────────────────────────────────────────────────┤
    │  • WindowManager (TYPE_APPLICATION_OVERLAY)                     │
    │  • KeyguardManager (isKeyguardSecure, requestDismissKeyguard)   │
    │  • PowerManager (WakeLock - keep screen on)                     │
    │  • Settings.canDrawOverlays() permission                        │
    └─────────────────────────────────────────────────────────────────┘
    ```

    ---

    ## 📊 **Touch Event Flow (Critical for Interactivity)**

    ```text
    User taps chess piece on lockscreen
            ↓
    ┌─────────────────────────────────────────────┐
    │   Android System (MotionEvent)              │
    │   • ACTION_DOWN, ACTION_MOVE, ACTION_UP     │
    └───────────────┬─────────────────────────────┘
                    ▼
    ┌─────────────────────────────────────────────┐
    │   WindowManager                             │
    │   • Checks window flags                     │
    │   • TYPE_APPLICATION_OVERLAY allows touch   │
    │   • NO FLAG_NOT_TOUCHABLE                   │
    │   • NO FLAG_NOT_FOCUSABLE                   │
    └───────────────┬─────────────────────────────┘
                    ▼
    ┌─────────────────────────────────────────────┐
    │   FlutterView (Native Android View)         │
    │   • isFocusable = true                      │
    │   • isFocusableInTouchMode = true           │
    │   • isClickable = true                      │
    │   • onTouchListener returns false           │
    └───────────────┬─────────────────────────────┘
                    ▼
    ┌─────────────────────────────────────────────┐
    │   FlutterEngine (Dart VM)                   │
    │   • Converts MotionEvent → PointerEvent     │
    │   • Sends to Flutter framework              │
    └───────────────┬─────────────────────────────┘
                    ▼
    ┌─────────────────────────────────────────────┐
    │   ChessBoardWidget (Flutter)                │
    │   • GestureDetector / Draggable             │
    │   • Detects drag start, move, end           │
    │   • Calls onMove(from, to)                  │
    └───────────────┬─────────────────────────────┘
                    ▼
    ┌─────────────────────────────────────────────┐
    │   LockScreenProvider                        │
    │   • makeMove('e2', 'e4')                    │
    │   • Chess engine validates                  │
    │   • Updates board state                     │
    │   • Checks for checkmate                    │
    │   • If won → unlockDevice()                 │
    └─────────────────────────────────────────────┘
    ```

    ---

    ## ✨ **Summary**

    Your ChessLock app uses a **proven architecture** for settings sync:

    1. ✅ **SharedPreferences** = Persistent storage
    2. ✅ **Provider pattern** = State management
    3. ✅ **Native overlay** = Lockscreen rendering
    4. ✅ **FlutterEngine** = UI framework
    5. ✅ **Game state service** = Session persistence

    ### **Key Success Factors**:

    ✅ **TYPE_APPLICATION_OVERLAY** - Allows full touch after keyguard dismissed
    ✅ **No lock screen flags on overlay** - Prevents touch blocking
    ✅ **120ms delay** - Ensures keyguard fully dismissed before showing overlay
    ✅ **Touch-enabled FlutterView** - All focusable/clickable flags set
    ✅ **SharedPreferences sync** - Settings persist across app and lockscreen
    ✅ **Provider architecture** - Clean separation of concerns

    **Everything is built correctly!** The settings sync between main app and lockscreen is working as designed. 🎉
