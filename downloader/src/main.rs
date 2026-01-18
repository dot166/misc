use std::{env, fs, process::{Command, exit}};
use std::path::Path;
use serde::Deserialize;
use url::Url;
use image::{Rgb, RgbImage};
use imageproc::drawing::draw_text_mut;
use ab_glyph::{FontArc, PxScale};

#[derive(Debug, Deserialize)]
struct SongResponse {
    pvs: Vec<Pv>,
    #[serde(rename = "mainPicture")]
    main_picture: Picture,
    #[serde(rename = "artistString")]
    artist_string: String,
    name: String,
}

#[derive(Debug, Deserialize)]
struct Pv {
    service: String,
    #[serde(rename = "pvId")]
    pv_id: String,
    #[serde(rename = "pvType")]
    pv_type: String,
}

#[derive(Debug, Deserialize)]
struct Picture {
    #[serde(rename = "urlOriginal")]
    url_original: String,
}

#[derive(Debug, Clone)]
struct SongMetadata {
    url: String,
    main_picture: String,
    artist: String,
    name: String,
}

fn extract_id(input: &str) -> Option<String> {
    let url = Url::parse(input).ok()?;

    match url.domain()? {
        "www.nicovideo.jp" | "nicovideo.jp" => {
            url.path_segments()?
                .last()
                .map(|s| s.to_string())
        }
        "www.youtube.com" | "youtube.com" | "youtu.be" => {
            if url.domain()? == "youtu.be" {
                url.path_segments()?
                    .last()
                    .map(|s| s.to_string())
            } else {
                url.query_pairs()
                    .find(|(k, _)| k == "v")
                    .map(|(_, v)| v.to_string())
            }
        }
        _ => None,
    }
}

fn get_metadata_for_video(url: String, service: &String) -> Result<SongMetadata, String> {
    let api_url = format!(
        "https://vocadb.net/api/songs/byPv?pvService={}&pvId={}&fields=MainPicture,PVs",
        service,
        extract_id(&url).unwrap()
    );

    let resp = reqwest::blocking::get(&api_url)
        .map_err(|e| format!("HTTP error: {}", e))?;

    if !resp.status().is_success() {
        return Err(format!("VocaDB returned {}", resp.status()));
    }

    let song: SongResponse = resp
        .json()
        .map_err(|e| format!("JSON parse error: {}", e))?;

    Ok(SongMetadata {
        url,
        main_picture: song.main_picture.url_original,
        artist: song.artist_string,
        name: song.name,
    })
}

fn get_metadata_for_vocadb_id(id: &str) -> Result<SongMetadata, String> {
    let api_url = format!(
        "https://vocadb.net/api/songs/{}?fields=MainPicture,PVs",
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

    let url = match vocadb_id_to_url(&song) {
        Ok(url) => {
            println!("Resolved VocaDB {} → {}", id, url);
            url
        }
        Err(e) => {
            return Err(format!("Failed to resolve VocaDB {}: {}", id, e));
        }
    };

    Ok(SongMetadata {
        url,
        main_picture: song.main_picture.url_original,
        artist: song.artist_string,
        name: song.name,
    })
}

fn vocadb_id_to_url(song: &SongResponse) -> Result<String, String> {
    // Prefer original NicoNico, then original YouTube
    for pv in &song.pvs {
        if pv.pv_type == "Original" && pv.service == "NicoNicoDouga" {
            return Ok(format!(
                "https://www.nicovideo.jp/watch/{}",
                pv.pv_id
            ));
        }
    }

    for pv in &song.pvs {
        if pv.pv_type == "Original" && pv.service == "Youtube" {
            return Ok(format!(
                "https://www.youtube.com/watch?v={}",
                pv.pv_id
            ));
        }
    }

    Err("No original NicoNico or YouTube PV found".into())
}


fn print_usage() {
    println!("Usage: cargo run <url_file>");
    println!("  url_file : File containing Niconico or YouTube URLs or VocaDB IDs (one per line).");
    println!("  The output will be saved to ~/Downloads/vocaloid");
    println!("  This uses VocaDB for metadata, even if an ID is not used");
}

fn generate_fallback_cover(path: &str) -> Result<(), String> {
    let mut img = RgbImage::from_pixel(300, 300, Rgb([255, 0, 255])); // HOT PINK

    let font_data = fs::read("Comic_Sans_MS_Bold.ttf")
        .map_err(|e| format!("Font load error: {}", e))?;

    let font = FontArc::try_from_vec(font_data)
        .map_err(|_| "Failed to parse font".to_string())?;

    let scale = PxScale::from(32.0);

    draw_text_mut(
        &mut img,
        Rgb([0, 0, 0]),
        20,
        130,
        scale,
        &font,
        "CHECK METADATA :3",
    );

    img.save(path)
        .map_err(|e| format!("Image save error: {}", e))?;

    Ok(())
}


fn download_cover(url: &str, path: &str) -> Result<(), String> {
    if url.starts_with("data:") || url.is_empty() {
        return generate_fallback_cover(path);
    }

    match reqwest::blocking::get(url) {
        Ok(resp) => {
            let bytes = resp.bytes().map_err(|e| e.to_string())?;
            fs::write(path, &bytes).map_err(|e| e.to_string())?;
            Ok(())
        }
        Err(_) => {
            generate_fallback_cover(path)
        }
    }
}

fn safe_filename(s: &str) -> String {
    s.chars()
        .map(|c| match c {
            '/' | '\\' | ':' | '*' | '?' | '"' | '<' | '>' | '|' => '_',
            _ => c,
        })
        .collect()
}

fn download_audio(meta: SongMetadata, output_dir: &str) {
    println!("Downloading audio from: {}", meta.url);

    let output = Command::new("yt-dlp")
        .args(&[
            "-x",
            "--audio-format", "mp3",
            "--audio-quality", "0",
            "-o", "song.mp3",
            &meta.url,
        ])
        .spawn()
        .unwrap()
        .wait_with_output();

    match output {
        Ok(output) => {
            if !output.status.success() {
                panic!("Error downloading {}: please check above logs", meta.url);
            } else {
                println!("Done: {}", meta.url);
            }
        }
        Err(e) => {
            panic!("Failed to execute yt-dlp: {}", e);
        }
    }

    let cover_path = "cover.jpg";
    download_cover(&meta.main_picture, cover_path).unwrap();

    let artist = safe_filename(&meta.artist);
    let title = safe_filename(&meta.name);

    let output2 = Command::new("ffmpeg")
        .args(&[
            "-y",
            "-i", "song.mp3",
            "-i", "cover.jpg",
            "-map", "0:a",
            "-map", "1:v",
            "-c", "copy",
            "-id3v2_version", "3",
            "-metadata", &format!("title={}", meta.name),
            "-metadata", &format!("artist={}", meta.artist),
            "-metadata", &format!("album={}", meta.name),
            "-metadata", "genre=VOCALOID",
            "-metadata:s:v", "title=Album cover",
            "-metadata:s:v", "comment=Cover (front)",
            &format!("{}/{} - {}.mp3", output_dir, artist, title),
        ])
        .spawn()
        .unwrap()
        .wait_with_output();

    match output2 {
        Ok(output) => {
            if !output.status.success() {
                panic!("Error setting metadata using ffmpeg: please check above logs");
            } else {
                println!("Done setting metadata using ffmpeg");
            }
        }
        Err(e) => {
            panic!("Failed to execute ffmpeg: {}", e);
        }
    }

    fs::remove_file("song.mp3").unwrap();
    fs::remove_file("cover.jpg").unwrap();
}

fn main() {
    let default_output_dir = dirs::home_dir().unwrap_or_else(|| Path::new("/").to_path_buf()).join("Downloads/vocaloid");
    let output_dir = default_output_dir.to_string_lossy().to_string();
    fs::create_dir_all(&output_dir).unwrap();

    let mut args = env::args().skip(1);

    if let Some(url_file) = args.next() {
        if let Ok(contents) = fs::read_to_string(&url_file) {
            for line in contents.lines() {
                let input = line.trim();
                if input.is_empty() || input.starts_with("#") {
                    continue;
                }

                if input.chars().all(|c| c.is_ascii_digit()) {
                    let meta = get_metadata_for_vocadb_id(input).unwrap();
                    download_audio(meta, &output_dir);
                } else {
                    let service: String;
                    if input.contains("nicovideo.jp") {
                        service = "NicoNicoDouga".to_string();
                    } else if input.contains("youtube") || input.contains("youtu.be") {
                        service = "Youtube".to_string();
                    } else {
                        panic!("Unsupported service: {}", input)
                    }
                    let meta = match get_metadata_for_video(input.to_string(), &service) {
                        Ok(meta) => {
                            println!("Resolved VocaDB {} → {}", input, meta.url);
                            meta
                        }
                        Err(e) => {
                            eprintln!("Failed to resolve VocaDB {}: {}", input, e);
                            SongMetadata {
                                url: input.to_string(),
                                main_picture: "".to_string(),
                                artist: service.clone(),
                                name: format!("CHECK METADATA - {}", extract_id(input).unwrap()),
                            }
                        }
                    };
                    download_audio(meta, &output_dir);
                };
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
