#[cfg(not(target_os = "android"))]
fn main() -> eframe::Result<()> {
    owot_rust_client::run_desktop()
}

#[cfg(target_os = "android")]
fn main() {
    // Android uses android_main() from lib.rs instead
    panic!("Android builds should use android_main(), not main()")
}
