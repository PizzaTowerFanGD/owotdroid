# OWOT Android Client

A native Android client for Our World of Text (OWOT) - a collaborative infinite text world platform.

## Overview

This Android application provides a complete native implementation of an OWOT client, allowing users to:
- Connect to OWOT worlds via WebSocket
- View and edit infinite text canvases
- Chat with other users in real-time
- Navigate worlds with touch gestures and zoom
- Access world statistics and properties
- Configure user preferences

## Features

### Core Functionality
- **Infinite Canvas Rendering**: Efficiently renders tiles of text using Android's Canvas API
- **WebSocket Communication**: Full implementation of OWOT protocol over WebSockets
- **Real-time Collaboration**: See other users' cursors and edits in real-time
- **Touch Gestures**: Pan, zoom, and tap interactions optimized for mobile
- **Chat System**: Page and global chat with support for user roles and formatting
- **Text Editing**: Direct character placement with color and background support

### Technical Features
- **MVVM Architecture**: Clean separation of concerns using Android ViewModels
- **Room Database**: Local storage for tiles, chat history, and preferences
- **WebSocket Management**: Robust connection handling with auto-reconnect
- **Background Service**: Keeps connections alive when app is backgrounded
- **Performance Optimization**: Tile caching and selective rendering
- **Material Design**: Modern UI following Material Design principles

## Architecture

### Data Layer
- **Models**: Complete OWOT protocol data structures
- **Room Database**: Local storage with migration support
- **Network**: WebSocket manager with OkHttp3

### Business Logic Layer
- **ViewModels**: Manage UI state and coordinate between data and presentation
- **Repository Pattern**: Abstract data access and caching
- **Coroutines**: Asynchronous operations with proper lifecycle management

### Presentation Layer
- **Activities**: Main navigation and world interaction
- **SurfaceView**: Custom rendering engine for the infinite canvas
- **Adapters**: RecyclerView adapters for lists and chat
- **Material UI**: Modern Android UI components

## Key Components

### WebSocketManager
Handles all network communication with OWOT servers:
- Connection management with auto-reconnect
- Message queuing and batching
- Protocol implementation for all OWOT message types
- Background operation support

### OWOTRenderer
High-performance rendering engine:
- Tile-based rendering with caching
- Text rendering with font and color support
- Cursor and selection visualization
- Optimized for Android's Canvas API

### OWOTSurfaceView
Custom Android view for user interaction:
- Touch gesture detection
- Camera control (pan, zoom)
- Coordinate transformation
- Optimized rendering loop

### Data Models
Complete OWOT protocol implementation:
- Tiles with properties and decorations
- Chat messages with user roles
- World properties and statistics
- User permissions and preferences

## Installation

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9+

### Building
1. Clone the repository
2. Open in Android Studio
3. Sync project with Gradle files
4. Build and run on device or emulator

### Dependencies
- AndroidX libraries
- OkHttp3 for networking
- Gson for JSON serialization
- Room for database
- Hilt for dependency injection
- Material Components

## Usage

### Connecting to Worlds
1. Launch the app
2. Enter world name on main screen
3. Tap "Connect" to establish WebSocket connection
4. World canvas will load automatically

### Navigation
- **Pan**: Drag with one finger
- **Zoom**: Pinch with two fingers or use FABs
- **Center**: Tap "Center" button to reset view
- **Tap**: Tap to position cursor

### Editing
- Tap on a character position to move cursor
- Use virtual keyboard to type
- Characters appear immediately (optimistic updates)
- Edits are batched and sent to server

### Chat
- Chat input at bottom of screen
- Tap send button or press Enter
- Messages appear in chat area
- Support for commands (e.g., /nick)

## Configuration

### User Preferences
- Nickname customization
- Text and background colors
- Font size settings
- Grid and cursor visibility
- Chat preferences

### Network Settings
- Connection timeouts
- Auto-reconnect behavior
- Background connection handling

## Protocol Implementation

The client implements the complete OWOT protocol:

### Message Types
- **fetch**: Request tile data
- **write**: Send character edits
- **chat**: Send and receive chat messages
- **cursor**: Update cursor positions
- **protect**: Change protection settings
- **link**: Create links between coordinates

### Data Structures
- **Tiles**: 16x8 character grids with properties
- **Cells**: Individual character positions with colors and links
- **Users**: Nicknames and permission levels
- **Worlds**: Properties, themes, and statistics

## Performance

### Rendering Optimization
- Tile-based rendering with caching
- Selective re-rendering of dirty tiles
- Off-screen canvas for pre-rendering
- Frame rate throttling

### Memory Management
- Efficient tile caching with LRU eviction
- Background cleanup of unused resources
- Bitmap recycling for memory efficiency

### Network Optimization
- Message batching to reduce overhead
- Selective tile fetching based on viewport
- Background synchronization

## Testing

### Unit Tests
- Model validation and serialization
- Business logic verification
- Network layer testing

### Integration Tests
- WebSocket communication
- Database operations
- UI interaction flows

### Manual Testing
- Real OWOT server connectivity
- Performance under load
- Battery usage optimization

## Contributing

### Development Setup
1. Fork the repository
2. Create feature branch
3. Follow existing code patterns
4. Add tests for new functionality
5. Submit pull request

### Code Style
- Kotlin coding conventions
- Material Design principles
- Consistent naming and structure
- Comprehensive documentation

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Our World of Text community for the original platform
- Android development community for libraries and tools
- Contributors and testers who helped improve the client

## Support

For issues, questions, or contributions:
- GitHub Issues for bug reports
- GitHub Discussions for questions
- Pull requests for contributions

## Roadmap

### Planned Features
- [ ] Link creation and editing UI
- [ ] Advanced selection tools
- [ ] World search and discovery
- [ ] Offline mode with sync
- [ ] Theming and customization
- [ ] Accessibility improvements

### Technical Improvements
- [ ] Unit test coverage expansion
- [ ] Performance profiling and optimization
- [ ] Memory usage optimization
- [ ] Battery life improvements
- [ ] Security hardening

---

**Note**: This is a third-party client implementation. OWOT is a trademark of its respective owners.