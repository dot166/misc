use std::fs::File;
use std::{env, fs, process::Command};
use std::process::Stdio;
use std::path::Path;
use std::path::PathBuf;
use serde::Deserialize;
use image::{Rgb, RgbImage};
use imageproc::drawing::draw_text_mut;
use ab_glyph::{FontArc, PxScale};
use indicatif::{ProgressBar, ProgressStyle};
use futures::stream::{self, StreamExt};
use indicatif::MultiProgress;
use uuid::Uuid;
use std::sync::{
    Arc,
    atomic::{AtomicUsize, Ordering},
};
use std::time::{Duration, SystemTime};
use sha2::{Digest, Sha256};

const CACHE_TTL: Duration = Duration::from_secs(60 * 60 * 24 * 30); // 30 days

#[derive(Debug, Deserialize)]
struct SongResponse {
    pvs: Vec<Pv>,
    #[serde(rename = "mainPicture")]
    main_picture: Picture,
    #[serde(rename = "artistString")]
    artist_string: String,
    name: String,
    id: i32,
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
    id: i32,
}

async fn get_metadata(id: &str) -> Result<SongMetadata, String> {
    let api_url = format!(
        "https://vocadb.net/api/songs/{}?fields=MainPicture,PVs",
        id
    );

    let client = reqwest::Client::builder()
        .user_agent("vocaloid-downloader/1.2.0 (https://github.com/dot166/misc)")
        .build()
        .unwrap();

    let json = get_cached_json(&api_url, &client).await?;
    let song: SongResponse =
        serde_json::from_str(&json)
        .map_err(|e| format!("JSON parse error: {}", e))?;

    let url = vocadb_id_to_url(&song)?;

    Ok(SongMetadata {
        url,
        main_picture: song.main_picture.url_original,
        artist: song.artist_string,
        name: song.name,
        id: song.id,
    })
}

fn cache_dir() -> PathBuf {
    dirs::cache_dir()
        .unwrap_or_else(|| PathBuf::from("/tmp"))
        .join("vocaloid-downloader")
}

fn prune_old_caches(dir: &Path) {
    if let Ok(entries) = fs::read_dir(dir) {
        let now = SystemTime::now();

        for entry in entries.flatten() {
            if let Ok(meta) = entry.metadata() {
                if let Ok(modified) = meta.modified() {
                    if let Ok(age) = now.duration_since(modified) {
                        if age > CACHE_TTL {
                            let _ = fs::remove_file(entry.path());
                        }
                    }
                }
            }
        }
    }
}

async fn get_cached_json(api_url: &str, client: &reqwest::Client)
    -> Result<String, String>
{
    fs::create_dir_all(cache_dir())
        .map_err(|e| format!("cache dir: {}", e))?;

    prune_old_caches(&cache_dir());

    let path = cache_path_for_url(api_url);

    if path.exists() && cache_valid(&path) {
        return fs::read_to_string(&path)
            .map_err(|e| format!("cache read: {}", e));
    }

    let resp = client
        .get(api_url)
        .send()
        .await
        .map_err(|e| format!("HTTP error: {}", e))?;

    if !resp.status().is_success() {
        return Err(format!("VocaDB returned {}", resp.status()));
    }

    let body = resp.text().await
        .map_err(|e| format!("read body: {}", e))?;

    let _ = fs::write(&path, &body);
    Ok(body)
}

fn cache_path_for_url(url: &str) -> PathBuf {
    let mut hasher = Sha256::new();
    hasher.update(url.as_bytes());
    let name = format!("{:x}.json", hasher.finalize());

    PathBuf::from(cache_dir()).join(name)
}

fn cache_valid(path: &Path) -> bool {
    if let Ok(meta) = fs::metadata(path) {
        if let Ok(modified) = meta.modified() {
            if let Ok(age) = SystemTime::now().duration_since(modified) {
                return age < CACHE_TTL;
            }
        }
    }
    false
}

fn vocadb_id_to_url(song: &SongResponse) -> Result<String, String> {
    // Prefer original NicoNico, then original YouTube
    for pv in &song.pvs {
        if pv.pv_type == "Original" && pv.service == "NicoNicoDouga" && !is_blocked_pv(pv.pv_id.clone()) {
            return Ok(format!(
                "https://www.nicovideo.jp/watch/{}",
                pv.pv_id
            ));
        }
    }

    for pv in &song.pvs {
        if pv.pv_type == "Original" && pv.service == "Youtube" && !is_blocked_pv(pv.pv_id.clone()) {
            return Ok(format!(
                "https://www.youtube.com/watch?v={}",
                pv.pv_id
            ));
        }
    }

    println!("No original PVs found, falling back to other");

    // Prefer original NicoNico, then original YouTube
    for pv in &song.pvs {
        if pv.pv_type == "Other" && pv.service == "NicoNicoDouga" && !is_blocked_pv(pv.pv_id.clone()) {
            return Ok(format!(
                "https://www.nicovideo.jp/watch/{}",
                pv.pv_id
            ));
        }
    }

    for pv in &song.pvs {
        if pv.pv_type == "Other" && pv.service == "Youtube" && !is_blocked_pv(pv.pv_id.clone()) {
            return Ok(format!(
                "https://www.youtube.com/watch?v={}",
                pv.pv_id
            ));
        }
    }

    Err("No NicoNico or YouTube PV found".into())
}

fn is_blocked_pv(id: String) -> bool {
    if id == "sm12276096" {
        return true;
    } else {
        return false;
    }
}


fn print_usage() {
    println!("Usage: cargo run <url_file>");
    println!("  url_file : File containing Niconico or YouTube URLs or VocaDB IDs (one per line).");
    println!("  The output will be saved to ~/Downloads/vocaloid");
    println!("  This uses VocaDB for metadata, even if an ID is not used");
}

fn generate_fallback_cover(path: &PathBuf) -> Result<(), String> {
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


async fn download_cover(url: &str, path: &PathBuf) -> Result<(), String> {
    if url.starts_with("data:") || url.is_empty() {
        return generate_fallback_cover(path);
    }

    match reqwest::get(url).await {
        Ok(resp) => {
            let bytes = resp.bytes().await.map_err(|e| e.to_string())?;
            fs::write(path, &bytes).map_err(|e| e.to_string())?;
            Ok(())
        }
        Err(_) => {
            generate_fallback_cover(path)
        }
    }
}

fn download_audio(
    meta: SongMetadata,
    output_dir: &str,
    work_dir: &Path,
) -> Result<(), String> {
    let song_path = work_dir.join("song.mp3");
    let cover_path = work_dir.join("cover.jpg");
    let log_path = work_dir.join("worker.log");

    let log_file = File::create(&log_path)
        .map_err(|e| format!("Failed to create log file: {}", e))?;

    let status = Command::new("yt-dlp")
        .args(&[
            "-x",
            "--audio-format", "mp3",
            "--audio-quality", "0",
            "-o",
            song_path.to_str().unwrap(),
            &meta.url,
        ])
        .stdout(Stdio::from(log_file.try_clone().unwrap()))
        .stderr(Stdio::from(log_file.try_clone().unwrap()))
        .status()
        .map_err(|e| format!("Failed to run yt-dlp: {}", e))?;

    if !status.success() {
        let log: String;
        if env::var("CI").is_ok() {
            log = format!("logs: {}", fs::read_to_string(log_path).unwrap())
        } else {
            log = format!("(see {})", log_path.display().to_string())
        }
        return Err(format!(
            "yt-dlp failed for {} {}",
            meta.url,
            log
        ));
    }

    let status2 = Command::new("ffmpeg")
        .args(&[
            "-y",
            "-i", song_path.to_str().unwrap(),
            "-i", cover_path.to_str().unwrap(),
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
            "-metadata", &format!("comment=Metadata from VocaDB (https://vocadb.net), ID: {}", meta.id),
            &format!(
                "{}/{}.mp3",
                output_dir,
                meta.id,
            ),
        ])
        .stdout(Stdio::from(log_file.try_clone().unwrap()))
        .stderr(Stdio::from(log_file))
        .status()
        .map_err(|e| format!("Failed to run ffmpeg: {}", e))?;

    if !status2.success() {
        let log: String;
        if env::var("CI").is_ok() {
            log = format!("logs: {}", fs::read_to_string(log_path).unwrap())
        } else {
            log = format!("(see {})", log_path.display().to_string())
        }
        return Err(format!(
            "ffmpeg failed for {} {}",
            meta.name,
            log
        ));
    }

    Ok(())
}

async fn process_song(
    input: String,
    output_dir: String,
    bar: ProgressBar,
) -> Result<(), String> {
    let job_dir: PathBuf = std::env::temp_dir()
        .join(format!("vocaloid_job_{}", Uuid::new_v4()));

    fs::create_dir_all(&job_dir)
        .map_err(|e| format!("tmp dir: {}", e))?;

    bar.set_message("Resolving metadata");

    let meta = get_metadata(&input).await?;

    bar.set_message("Downloading cover");

    let cover_path = job_dir.join("cover.jpg");
    download_cover(&meta.main_picture, &cover_path)
        .await
        .map_err(|e| format!("cover: {}", e))?;

    bar.set_message("Downloading audio");

    // Run blocking yt-dlp + ffmpeg off the async runtime
    let out_dir = output_dir.clone();
    let name = meta.name.clone();
    let work_dir = job_dir.clone();

    let result = tokio::task::spawn_blocking(move || {
        download_audio(meta, &out_dir, &work_dir)
    })
    .await;

    match result {
        Ok(Ok(())) => {
            std::fs::remove_dir_all(&job_dir).ok();
            bar.finish_with_message(format!("Done: {}", name));
            Ok(())
        }
        Ok(Err(e)) => {
            Err(e)
        }
        Err(_) => {
            Err("Worker panicked".into())
        }
    }
}

#[tokio::main]
async fn main() {
    let default_output_dir = dirs::home_dir()
        .unwrap()
        .join("Downloads/vocaloid")
        .to_string_lossy()
        .to_string();

    fs::create_dir_all(&default_output_dir).unwrap();

    let args: Vec<String> = env::args().collect();
    if args.len() < 2 {
        print_usage();
        return;
    }

    let contents = fs::read_to_string(&args[1]).unwrap();

    let jobs: Vec<String> = contents
        .lines()
        .map(str::trim)
        .filter(|l| !l.is_empty() && l.chars().all(|c| c.is_ascii_digit()))
        .map(String::from)
        .collect();

    let mp = MultiProgress::new();
    let style = ProgressStyle::with_template("{spinner} {msg}")
        .unwrap()
        .tick_strings(&["⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"]);

    let max_parallel = num_cpus::get().min(8); // like a polite build system

    let failures = Arc::new(AtomicUsize::new(0));

    println!("Metadata provided by VocaDB (https://vocadb.net)");

    stream::iter(jobs)
        .for_each_concurrent(max_parallel, |job| {
            let bar = mp.add(ProgressBar::new_spinner());
            bar.set_style(style.clone());
            bar.enable_steady_tick(std::time::Duration::from_millis(100));

            let out = default_output_dir.clone();
            let failures = failures.clone();
            let err_bar = bar.clone();
            let err_job = job.clone();
            async move {
                if let Err(e) = process_song(job, out, bar).await {
                    if env::var("CI").is_ok() {
                        eprintln!("worker {} failure: {}", err_job, e);
                    } else {
                        err_bar.finish_with_message(format!("{} Failed: {}", err_job, e));
                    }
                    failures.fetch_add(1, Ordering::Relaxed);
                }
            }
        })
        .await;

    let count = failures.load(Ordering::Relaxed);
    if count > 0 {
        eprintln!("{} job(s) failed", count);
        std::process::exit(1);
    }

    println!("All done! Audio files are in: {}", default_output_dir);
}
