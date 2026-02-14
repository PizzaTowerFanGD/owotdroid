mod app;
mod network;
mod types;

use app::OwotApp;

#[cfg(target_os = "android")]
#[no_mangle]
pub fn android_main(_app: android_activity::AndroidApp) {
    android_logger::init_once(android_logger::Config::default());
    let options = eframe::NativeOptions::default();
    let world_name = "main".to_string();
    eframe::run_native(
        "OWOT Rust Client",
        options,
        Box::new(|_| Box::new(OwotApp::new(world_name))),
    )
    .expect("eframe run");
}

#[cfg(not(target_os = "android"))]
pub fn run_desktop() -> eframe::Result<()> {
    let _ = env_logger::builder().is_test(false).try_init();
    let options = eframe::NativeOptions::default();
    let world_name = "main".to_string();
    eframe::run_native(
        "OWOT Rust Client",
        options,
        Box::new(|_| Box::new(OwotApp::new(world_name))),
    )
}
