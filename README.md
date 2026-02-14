# OWOT Rust Client (Android + Desktop)

This repository contains a Rust client for **Our World of Text (OWOT)** built with `egui`/`eframe` and compatible with Android.

## Features
- WebSocket networking with `tokio-tungstenite`
- Tile cache and renderer for the 16x8 OWOT tiles
- Basic cursor + optimistic write handling
- Touch drag scrolling (Android friendly)
- Unicode grapheme-safe text edits

## Requirements
- Rust 1.75+
- Android NDK (for Android builds)

## Running on Desktop
```bash
cargo run --features desktop
```

## Android Build (cargo-apk)
1. Install `cargo-apk`:
```bash
cargo install cargo-apk
```

2. Build and run:
```bash
cargo apk run --release
```

## Android Build (cargo-ndk)
1. Install `cargo-ndk`:
```bash
cargo install cargo-ndk
```

2. Build shared library:
```bash
cargo ndk -t arm64-v8a -o ./target/android build --release
```

Package the `.so` into your Android project or use an APK wrapper.

## Configuration
- The base URL is `ourworldoftext.com` (see `src/network.rs`).
- The default world is `main`. Update the `world_name` in `src/lib.rs` if needed.
