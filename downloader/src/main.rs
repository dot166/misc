use std::{env, fs, process::{Command, exit}};
use std::path::Path;
use serde::Deserialize;

#[derive(Debug, Deserialize)]
struct SongResponse {
    pvs: Vec<Pv>,
}

#[derive(Debug, Deserialize)]
struct Pv {
    service: String,
    pvId: String,
    pvType: String,
}

fn vocadb_id_to_url(id: &str) -> Result<String, String> {
    let api_url = format!(
        "https://vocadb.net/api/songs/{}?fields=PVs",
        id
    );

    let resp = reqwest::blocking::get(&api_url)
        .map_err(|e| format!("HTTP error: {}", e))?;

    if !resp.status().is_success() {
        return Err(format!("VocaDB returned {}", resp.status()));
    }

    let song: SongResponse = resp
        .json()
        .map_err(|e| format!("JSON parse error: {}", e))?;

    // Prefer original NicoNico, then original YouTube
    for pv in &song.pvs {
        if pv.pvType == "Original" && pv.service == "NicoNicoDouga" {
            return Ok(format!(
                "https://www.nicovideo.jp/watch/{}",
                pv.pvId
            ));
        }
    }

    for pv in &song.pvs {
        if pv.pvType == "Original" && pv.service == "Youtube" {
            return Ok(format!(
                "https://www.youtube.com/watch?v={}",
                pv.pvId
            ));
        }
    }

    Err("No original NicoNico or YouTube PV found".into())
}


fn print_usage() {
    println!("Usage: cargo run <url_file>");
    println!("\n  url_file : File containing Niconico or YouTube URLs or VocaDB IDs (one per line).");
    println!("\n  The output will be saved to ~/Downloads/vocaloid");
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
    let default_output_dir = dirs::home_dir().unwrap_or_else(|| Path::new("/").to_path_buf()).join("Downloads/vocaloid");
    let output_dir = default_output_dir.to_string_lossy().to_string();

    let mut args = env::args().skip(1);

    if let Some(url_file) = args.next() {
        if let Ok(contents) = fs::read_to_string(&url_file) {
            for line in contents.lines() {
                let input = line.trim();
                if input.is_empty() || input.starts_with("#") {
                    continue;
                }

                let url = if input.chars().all(|c| c.is_ascii_digit()) {
                   match vocadb_id_to_url(input) {
                        Ok(url) => {
                            println!("Resolved VocaDB {} → {}", input, url);
                            url
                        }
                        Err(e) => {
                            eprintln!("Failed to resolve VocaDB {}: {}", input, e);
                            continue;
                        }
                    }
                } else {
                    input.to_string()
                };

                download_audio(&url, &output_dir);
            }
        } else {
            eprintln!("Could not read file: {}", url_file);
            exit(1);
        }
    } else {
        eprintln!("No file provided!");
        print_usage();
        exit(1);
    }

    println!("All done! Audio files are in: {}", output_dir);
}
