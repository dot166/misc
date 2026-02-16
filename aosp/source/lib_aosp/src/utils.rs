use std::env;
use std::path::{Path, PathBuf};

/// adapted from build/make/shell_utils.sh in aosp
pub fn get_top() -> Option<PathBuf> {
    let top_file = "build/make/core/envsetup.mk";

    // Check if TOP is set and the file exists
    if let Ok(top) = env::var("TOP") {
        let top_path = Path::new(&top).join(top_file);
        if top_path.exists() {
            return Some(PathBuf::from(top));
        }
    }

    // Check the current directory and navigate upwards if necessary
    let current_dir = env::current_dir().unwrap();
    let mut pwd = current_dir.clone();

    while pwd != Path::new("/") {
        let top_path = pwd.join(top_file);
        if top_path.exists() {
            return Some(pwd);
        }
        pwd = pwd.parent().unwrap_or(Path::new("/")).to_path_buf();
    }

    None
}

/// adapted from build/make/shell_utils.sh in aosp
pub fn require_top() {
    if let Some(top) = get_top() {
        println!("Root of the source tree located at: {}", top.display());
        unsafe {env::set_var("TOP", top)}
    } else {
        eprintln!("Cannot locate root of source tree. This must be run from within the Android source tree or TOP must be set.");
        std::process::exit(1);
    }
}