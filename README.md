# 🏆 ChessLock - Professional Chess-Based Lockscreen App

## 📱 **Project Overview**

ChessLock is a sophisticated Android lockscreen application that replaces traditional PINs/passwords with chess puzzles. Built for Android 9+ (API 28+), it combines security with chess education in a beautiful, professional interface.

## ✨ **Key Features**

### 🔐 **Security Features**

- **Multi-layer Authentication**: Chess puzzle → Biometric → System lockscreen
- **Device Administrator Integration**: Secure screen locking capabilities
- **Emergency Unlock System**: 5-tap activation + biometric authentication
- **Security Logging**: Tracks unlock attempts and emergency usage

### ♟️ **Chess Engine**

- **Real Chess Puzzles**: Tactical puzzles from beginner to advanced
- **Multiple Difficulty Levels**: Easy, Medium, Hard with different complexity
- **Legal Move Validation**: Full chess rules implementation
- **Hint System**: Context-aware hints when players get stuck
- **Undo Functionality**: Allows taking back moves

### 🎨 **Professional Design**

- **Modern Material Design 3**: Beautiful, consistent UI components
- **Chess Theme**: Elegant dark theme with gold chess accents
- **Smooth Animations**: Piece movements, selections, and transitions
- **Live Clock**: Real-time display on lockscreen
- **Responsive Layout**: Adapts to different screen sizes

### 🛠️ **Setup & Configuration**

- **Guided Setup Wizard**: Step-by-step permission configuration
- **Status Indicators**: Visual feedback on setup completion
- **Permission Management**: Overlay and device admin permissions
- **Settings Panel**: Customizable difficulty and preferences

## 📋 **Technical Specifications**

### **Build Configuration**

- **Target SDK**: Android 36 (latest)
- **Minimum SDK**: Android 28 (Android 9+)
- **Gradle**: 8.13
- **Kotlin**: 2.0.21
- **Architecture**: MVVM with modern Android components

### **Key Dependencies**

- **Material Design 3**: Modern UI components
- **AndroidX Biometric**: Fingerprint/face authentication
- **Coroutines**: Asynchronous operations
- **Lifecycle Components**: Modern Android architecture

### **Permissions Required**

- `SYSTEM_ALERT_WINDOW`: For overlay lockscreen
- `DISABLE_KEYGUARD`: For screen lock control
- `WAKE_LOCK`: For screen management
- `BIND_DEVICE_ADMIN`: For device administrator functions
- `USE_BIOMETRIC`: For emergency authentication

## 🚀 **Installation & Setup**

### **1. Build the App**

```bash
cd chesslock2
./gradlew clean assembleDebug
```

### **2. Install on Device**

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### **3. Setup Process**

1. Open ChessLock app
2. Tap "Complete Setup"
3. Grant overlay permission when prompted
4. Grant device administrator permission
5. Test with "Test Chess Lock"

## 🎯 **Usage Instructions**

### **Normal Operation**

1. Tap "Test Chess Lock" to activate
2. Device locks and shows chess puzzle
3. Solve the puzzle to unlock
4. Use hint button if stuck
5. Undo moves if needed

### **Emergency Unlock**

1. Tap emergency button 5 times rapidly
2. Confirm emergency unlock
3. Authenticate with biometric (fingerprint/face)
4. Device unlocks (logged for security)

## 🔧 **File Structure**

### **Core Classes**

- `MainActivity.kt`: Main app entry point and setup
- `OverlayLockActivity.kt`: Lockscreen interface
- `ChessBoardView.kt`: Interactive chess board component
- `PuzzleManager.kt`: Chess puzzle data and management
- `LockAdminReceiver.kt`: Device administrator functions
- `FenParser.kt`: Chess notation parsing

### **Resources**

- `res/layout/`: UI layouts for activities
- `res/drawable/`: Chess pieces and UI graphics
- `res/values/colors.xml`: Professional color scheme
- `res/values/strings.xml`: Localized text resources

## 🛡️ **Security Considerations**

### **Privacy Protection**

- No network permissions required
- All data stored locally
- No user data collection
- Open source transparency

### **Security Measures**

- Emergency unlock logging
- Biometric authentication requirement
- Admin privilege validation
- Secure preference storage

## 🎮 **Chess Puzzle System**

### **Puzzle Categories**

- **Easy**: Basic checkmate in 1-2 moves
- **Medium**: Tactical combinations and forks
- **Hard**: Complex sacrifices and endgames

### **Features**

- FEN notation support
- Legal move validation
- Hint system with explanations
- Progress tracking

## 🔮 **Future Enhancements**

- Online puzzle synchronization
- Achievement system
- Custom puzzle import
- Multiplayer challenges
- Advanced statistics
- Theme customization

## 📞 **Support & Contributions**

This is a professional-grade chess lockscreen application built with modern Android best practices. The codebase is well-documented, modular, and ready for further enhancement or customization.

---

## 🏆 Built for chess lovers who want security with style! ♛
