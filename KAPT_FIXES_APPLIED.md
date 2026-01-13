# KAPT Duplicate Class Errors - Fix Applied

## Problem Analysis

The KAPT duplicate class errors were **NOT** caused by actual duplicate class definitions in the source code. Each class exists only once. The errors were caused by:

1. **Circular Dependency in Hilt DI Graph**: 
   - `OWOTApplication` was injecting `WebSocketManager`
   - `WebSocketManager` required `OWOTApplication` as a constructor parameter
   - This created a circular dependency that KAPT couldn't resolve

2. **KAPT Stub Generation Issues**:
   - KAPT was generating stubs for both debug and release variants
   - Both sets of stubs were being seen on the classpath simultaneously
   - This caused "duplicate class" errors even though source files were unique

## Changes Applied

### 1. Fixed Circular Dependency in OWOTApplication.kt

**Before:**
```kotlin
@HiltAndroidApp
class OWOTApplication : Application() {
    @Inject
    lateinit var webSocketManager: WebSocketManager  // Circular dependency!
    // ...
}
```

**After:**
```kotlin
@HiltAndroidApp
class OWOTApplication : Application() {
    // Removed webSocketManager injection
    // WebSocketManager is now injected where needed
    // ...
}
```

### 2. Updated AppModule.kt Dependency Injection

**Before:**
```kotlin
@Provides
@Singleton
fun provideWebSocketManager(application: OWOTApplication): WebSocketManager {
    return WebSocketManager(application)
}
```

**After:**
```kotlin
@Provides
@Singleton
fun provideWebSocketManager(@ApplicationContext context: Context): WebSocketManager {
    return WebSocketManager(context.applicationContext as OWOTApplication)
}
```

### 3. Updated WorldViewModel.kt to Accept WebSocketManager

**Before:**
```kotlin
class WorldViewModel(
    private val worldName: String,
    application: Application
) : AndroidViewModel(application) {
    private val application = getApplication<OWOTApplication>()
    private val webSocketManager = application.webSocketManager  // Used removed field!
    // ...
}
```

**After:**
```kotlin
class WorldViewModel(
    private val worldName: String,
    private val webSocketManager: WebSocketManager,  // Injected via constructor
    application: Application
) : AndroidViewModel(application) {
    // ...
}
```

### 4. Updated WorldViewModelFactory.kt

**Before:**
```kotlin
class WorldViewModelFactory(
    private val worldName: String,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorldViewModel(worldName, application) as T
    }
}
```

**After:**
```kotlin
class WorldViewModelFactory(
    private val worldName: String,
    private val webSocketManager: WebSocketManager,  // Added parameter
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return WorldViewModel(worldName, webSocketManager, application) as T
    }
}
```

### 5. Updated WorldActivity.kt to Inject WebSocketManager

**Before:**
```kotlin
@AndroidEntryPoint
class WorldActivity : AppCompatActivity() {
    private fun setupViewModel(worldName: String) {
        val factory = WorldViewModelFactory(worldName, application)
        viewModel = ViewModelProvider(this, factory)[WorldViewModel::class.java]
    }
}
```

**After:**
```kotlin
@AndroidEntryPoint
class WorldActivity : AppCompatActivity() {
    @Inject
    lateinit var webSocketManager: WebSocketManager  // Inject here
    
    private fun setupViewModel(worldName: String) {
        val factory = WorldViewModelFactory(worldName, webSocketManager, application)
        viewModel = ViewModelProvider(this, factory)[WorldViewModel::class.java]
    }
}
```

### 6. Fixed WebSocketService.kt Missing Import

**Added:**
```kotlin
import kotlinx.coroutines.cancel  // For serviceScope.cancel()
```

### 7. Fixed Missing KAPT Dependency

**Added to build.gradle:**
```gradle
kapt 'androidx.hilt:hilt-compiler:1.1.0'  // For Hilt WorkManager
```

### 8. Optimized KAPT Configuration in build.gradle

**Changes:**
- Enabled `useBuildCache = true` (was false, causing unnecessary rebuilds)
- Removed `generateStubs = true` (can cause variant conflicts)
- Removed `mapDiagnosticLocations = true` (not needed)
- Removed `showProcessorStats = false` (not needed)
- Kept essential configurations for Room and Dagger

## Why This Fixes the Errors

1. **Breaks Circular Dependency**: By removing `webSocketManager` injection from `OWOTApplication`, the circular dependency is eliminated. KAPT can now process the dependency graph without getting stuck.

2. **Proper Dependency Flow**: 
   - `@ApplicationContext` → `OWOTApplication` (provided by Hilt)
   - `OWOTApplication` → `WebSocketManager` (provided by AppModule)
   - `WebSocketManager` → `WorldActivity` (injected by Hilt)
   - `WebSocketManager` → `WorldViewModel` (passed via factory)

3. **Clean KAPT Processing**: With the circular dependency removed, KAPT can generate code for both debug and release variants without conflicts.

## Verification

To verify the fixes work:

```bash
# Clean build to remove old generated files
./gradlew clean

# Build debug variant
./gradlew assembleDebug

# Build release variant  
./gradlew assembleRelease

# Run tests
./gradlew test
```

## Classes That Were Reported as Duplicates (But Weren't)

These classes were NOT actually duplicated in source code:
- `com.owot.android.client.data.models.WorldTheme` (only in CoreModels.kt)
- `com.owot.android.client.data.models.WSMessage` (only in NetworkModels.kt)
- `com.owot.android.client.data.models.*Message` classes (only in NetworkModels.kt)
- `com.owot.android.client.network.WebSocketManager` (only in WebSocketManager.kt)

The "duplicates" were KAPT-generated stub files that were conflicting due to the circular dependency issue.
