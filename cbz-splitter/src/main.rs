use anyhow::{Context, Result};
use regex::Regex;
use std::{
    collections::BTreeMap,
    env,
    fs::File,
    io::{Read, Write},
    path::{Path, PathBuf},
};
use zip::{write::SimpleFileOptions, ZipArchive, ZipWriter};

#[derive(Debug)]
struct Entry {
    name: String,
    data: Vec<u8>,
}

fn main() -> Result<()> {
    let dir = env::args()
        .nth(1)
        .context("Usage: cbz-splitter <manga directory>")?;

    let dir = Path::new(&dir);

    if !dir.is_dir() {
        anyhow::bail!("{} is not a directory", dir.display());
    }

    let manga_name = dir
        .file_name()
        .context("Couldn't determine directory name")?
        .to_string_lossy()
        .to_string();

    let mut files: Vec<_> = std::fs::read_dir(dir)?
        .filter_map(|e| e.ok())
        .map(|e| e.path())
        .filter(|p| {
            p.extension()
                .is_some_and(|ext| ext.eq_ignore_ascii_case("cbz"))
        })
        .collect();

    // Process volumes in filename order.
    files.sort();

    for file in files {
        println!("Processing {}", file.display());
        split_cbz(&file, &manga_name)?;
    }

    Ok(())
}

fn split_cbz(path: &Path, manga_name: &str) -> Result<()> {
    let file = File::open(path)?;
    let mut archive = ZipArchive::new(file)?;

    // Matches:
    // c001
    // c001#1
    // c010.5
    // c010.5#2
    let chapter_re = Regex::new(r"- c([0-9]+(?:\.[0-9]+)?(?:#[0-9]+)?) ").unwrap();

    let mut chapters: BTreeMap<String, Vec<Entry>> = BTreeMap::new();

    for i in 0..archive.len() {
        let mut file = archive.by_index(i)?;

        if file.is_dir() {
            continue;
        }

        let name = file.name().to_string();

        let chapter = match chapter_re.captures(&name) {
            Some(c) => c[1].to_string().trim_start_matches('0').to_string(),
            None => {
                eprintln!("Skipping (no chapter found): {}", name);
                continue;
            }
        };

        let mut data = Vec::new();
        file.read_to_end(&mut data)?;

        chapters
            .entry(chapter)
            .or_default()
            .push(Entry { name, data });
    }

    for (chapter, files) in chapters {
        std::fs::create_dir_all(Path::new(&manga_name))?;
        let out_name = format!("{manga_name}/{manga_name} - Chapter {chapter}.cbz");
        println!("Writing {}", out_name);

        let out = File::create(PathBuf::from(out_name))?;
        let mut zip = ZipWriter::new(out);

        let options = SimpleFileOptions::default()
            .compression_method(zip::CompressionMethod::Deflated);

        for entry in files {
            zip.start_file(entry.name, options)?;
            zip.write_all(&entry.data)?;
        }

        zip.finish()?;
    }

    Ok(())
}