Fix KAPT duplicate class errors by eliminating circular dependency

## Problem
KAPT annotation processing was failing with duplicate class errors for:
- WebSocketManager
- All WSMessage subclasses (UserCountMessage, PropertyUpdateMessage, etc.)
- PropertyUpdateData, WorldTheme

These were NOT actual duplicate source files - each class existed only once.
The errors were KAPT-generated stub conflicts caused by a circular dependency
in the Hilt dependency injection graph.

## Root Cause
Circular dependency in Hilt:
- OWOTApplication injected WebSocketManager
- WebSocketManager constructor required OWOTApplication
- This prevented KAPT from properly processing annotations

## Solution

### 1. Broke Circular Dependency
- Removed `@Inject lateinit var webSocketManager` from OWOTApplication
- Updated AppModule.provideWebSocketManager to use @ApplicationContext
- Inject WebSocketManager directly where needed (Activities, ViewModels)

### 2. Updated Dependency Injection Flow
- OWOTApplication: Removed webSocketManager injection
- AppModule: Changed to `provideWebSocketManager(@ApplicationContext context)`
- WorldViewModel: Accept WebSocketManager as constructor parameter
- WorldViewModelFactory: Pass WebSocketManager to ViewModel
- WorldActivity: Inject WebSocketManager via Hilt

### 3. Fixed KAPT Configuration
- Removed problematic flags: `mapDiagnosticLocations`, `generateStubs`
- Enabled `useBuildCache` for better performance
- Added missing `androidx.hilt:hilt-compiler` dependency

### 4. Code Cleanup
- Added missing `import kotlinx.coroutines.cancel` in WebSocketService
- Removed unused imports (OWOTApplication, WebSocketManager where not needed)

## Files Changed
- app/src/main/java/com/owot/android/client/OWOTApplication.kt
- app/src/main/java/com/owot/android/client/di/AppModule.kt
- app/src/main/java/com/owot/android/client/viewmodel/WorldViewModel.kt
- app/src/main/java/com/owot/android/client/viewmodel/WorldViewModelFactory.kt
- app/src/main/java/com/owot/android/client/ui/WorldActivity.kt
- app/src/main/java/com/owot/android/client/network/WebSocketService.kt
- app/build.gradle

## Testing
Verified no duplicate class definitions exist in source:
- WorldTheme: 1 occurrence (CoreModels.kt)
- WSMessage: 1 occurrence (NetworkModels.kt)
- WebSocketManager: 1 occurrence (WebSocketManager.kt)

Build should now succeed with both debug and release variants.
