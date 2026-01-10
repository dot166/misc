use std::{env, fs, process::{Command, exit}};
use std::path::Path;

fn print_usage() {
    println!("Usage: cargo run <url_file>");
    println!("\n  url_file : File containing Niconico or YouTube URLs (one per line).");
    println!("\n  The output will be saved to ~/Downloads/yt-audio");
}

fn download_audio(url: &str, output_dir: &str) {
    println!("Downloading audio from: {}", url);

    let output = Command::new("yt-dlp")
        .args(&[
            "-x",
            "--audio-format", "mp3",
            "--audio-quality", "0",
            "-o", &format!("{}/%(title)s.%(ext)s", output_dir),
            url,
        ])
        .spawn()
        .unwrap()
        .wait_with_output();

    match output {
        Ok(output) => {
            if !output.status.success() {
                eprintln!("Error downloading {}: {}", url, String::from_utf8_lossy(&output.stderr));
            } else {
                println!("Done: {}", url);
            }
        }
        Err(e) => {
            eprintln!("Failed to execute yt-dlp: {}", e);
        }
    }
}

fn main() {
    let default_output_dir = dirs::home_dir().unwrap_or_else(|| Path::new("/").to_path_buf()).join("Downloads/yt-audio");
    let output_dir = default_output_dir.to_string_lossy().to_string();

    let mut args = env::args().skip(1);

    if let Some(url_file) = args.next() {
        if let Ok(contents) = fs::read_to_string(&url_file) {
            for line in contents.lines() {
                let url = line.trim();
                if !url.is_empty() && !url.starts_with("#") {
                    download_audio(url, &output_dir);
                }
            }
        } else {
            eprintln!("Could not read file: {}", url_file);
            exit(1);
        }
    } else {
        eprintln!("No URL file provided!");
        print_usage();
        exit(1);
    }

    println!("All done! Audio files are in: {}", output_dir);
}
